"""Analyses the history zips in data/games for the top officers who played in them.

For every slot played by a top officer (Officer nodes in Neo4j) it writes
data/analysis/game<gameid>-slot<slot>.json with
- economy: per turn planets, clans, mines, factories, defense, money, minerals, ships
- tech: tech level changes of the own starbases
- builds: every ship that appeared in the player's fleet, with its equipment
- contacts / messages: the player's diplomacy and messages with the other players
- milestones: a few key figures of the opening
The Vaadin view "Spielanalyse" (/analysis) shows these files.

Usage: python -I tools/game_analysis.py [--force] [--slot N] [zip ...]
  without zips all archives in data/games are analysed; existing results are kept
  unless --force is given. --slot analyses that slot instead of the top officers.
"""
import argparse
import html
import json
import re
import sys
import zipfile
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from topofficer_changes import RACES, neo4j_password, query  # noqa: E402

TURN_FILE = re.compile(r"player(\d+)-turn(\d+)\.trn")
# message types of planets.nu (see "messagetype" in the turn files)
PLAYER_MESSAGE_TYPES = {0, 17}
DIPLOMACY_TYPE = 18
OPENING_TURNS = (10, 20, 30, 40)


def load_turn(archive, slot, turn):
    raw = archive.read("player%d-turn%d.trn" % (slot, turn))
    try:
        data = json.loads(raw)
    except ValueError:
        # turn 0 of old games has empty lists written as "scores": ] instead of []
        data = json.loads(re.sub(rb'(":\s*)\]', rb"\1[]", raw))
    return data.get("rst", data)


def slot_names(archive, slot_turns):
    """real usernames per slot; late turns show eliminated players as "dead"."""
    names = {}
    for turn in sorted(slot_turns[1]):
        for player in load_turn(archive, 1, turn)["players"]:
            if player["username"] not in ("dead", "open", "") and player["id"] not in names:
                names[player["id"]] = player["username"]
        if turn > 30 and names:
            break
    return names


def message_text(body):
    text = re.sub(r"<br\s*/?>", "\n", body or "", flags=re.IGNORECASE)
    return html.unescape(re.sub(r"<[^>]+>", "", text)).strip()


def analyse_slot(archive, game_id, slot, turns, names, races_by_slot, last_players):
    me_name = names.get(slot, "?")
    economy, tech, builds, messages = [], [], [], []
    contacts = {}
    known_ships, last_tech, seen_messages = set(), {}, set()
    game_name = None

    def contact(other):
        return contacts.setdefault(other, {
            "slot": other, "username": names.get(other, "?"), "race": races_by_slot.get(other, ""),
            "firstMessageTurn": None, "sent": 0, "received": 0,
            "ambassadorSentTurn": None, "ambassadorReceivedTurn": None})

    for turn in turns:
        if turn == 0:
            continue
        data = load_turn(archive, slot, turn)
        game_name = game_name or data["game"]["name"]
        hulls = {h["id"]: h for h in data["hulls"]}
        engines = {e["id"]: e["name"] for e in data["engines"]}
        beams = {b["id"]: b["name"] for b in data["beams"]}
        torpedos = {t["id"]: t["name"] for t in data["torpedos"]}
        planets = [p for p in data["planets"] if p["ownerid"] == slot]
        planet_ids = {p["id"] for p in planets}
        starbases = [b for b in data["starbases"] if b["planetid"] in planet_ids]
        starbase_positions = {(p["x"], p["y"]) for p in planets if any(b["planetid"] == p["id"] for b in starbases)}
        ships = [s for s in data["ships"] if s["ownerid"] == slot]

        def total(key):
            return sum(max(p.get(key) or 0, 0) for p in planets)

        warships = [s for s in ships if s["beams"] or s["torps"] or s["bays"] or hulls.get(s["hullid"], {}).get("beams")]
        economy.append({
            "turn": turn, "planets": len(planets), "clans": total("clans"), "mines": total("mines"),
            "factories": total("factories"), "defense": total("defense"),
            "megacredits": total("megacredits"), "supplies": total("supplies"),
            "neutronium": total("neutronium"), "duranium": total("duranium"),
            "tritanium": total("tritanium"), "molybdenum": total("molybdenum"),
            "colonistTaxAvg": round(sum(p["colonisttaxrate"] for p in planets) / len(planets), 1) if planets else 0,
            "ships": len(ships), "warships": len(warships), "freighters": len(ships) - len(warships),
            "starbases": len(starbases)})

        for base in starbases:
            levels = (base["hulltechlevel"], base["enginetechlevel"], base["beamtechlevel"], base["torptechlevel"])
            if last_tech.get(base["id"]) != levels:
                tech.append({"turn": turn, "starbaseId": base["id"], "planetId": base["planetid"],
                             "hull": levels[0], "engine": levels[1], "beam": levels[2], "torpedo": levels[3]})
                last_tech[base["id"]] = levels

        for ship in ships:
            if ship["id"] in known_ships:
                continue
            known_ships.add(ship["id"])
            hull = hulls.get(ship["hullid"], {})
            builds.append({
                "turn": turn, "shipId": ship["id"], "name": ship["name"], "hull": hull.get("name", "?"),
                "engine": engines.get(ship["engineid"], ""),
                "beams": ship["beams"], "beam": beams.get(ship["beamid"], "") if ship["beams"] else "",
                "launchers": ship["torps"], "torpedo": torpedos.get(ship["torpedoid"], "") if ship["torps"] else "",
                "bays": ship["bays"], "warship": ship in warships,
                # ships appearing away from a starbase were captured or given, not built
                "origin": "start" if turn == 1 else ("gebaut" if (ship["x"], ship["y"]) in starbase_positions else "übernommen")})

        for message in data.get("mymessages", []):
            if message["id"] in seen_messages:
                continue
            seen_messages.add(message["id"])
            mtype = message.get("messagetype")
            if mtype == DIPLOMACY_TYPE:
                # ownerid receives the ambassador of target
                if message["ownerid"] == slot:
                    entry = contact(message["target"])
                    entry["ambassadorReceivedTurn"] = entry["ambassadorReceivedTurn"] or message["turn"]
                else:
                    entry = contact(message["ownerid"])
                    entry["ambassadorSentTurn"] = entry["ambassadorSentTurn"] or message["turn"]
                continue
            if mtype not in PLAYER_MESSAGE_TYPES:
                continue
            outgoing = me_name.lower() in message.get("headline", "").lower()
            other = message["target"] if message["ownerid"] == slot else message["ownerid"]
            if other == slot:
                continue
            entry = contact(other)
            entry["sent" if outgoing else "received"] += 1
            entry["firstMessageTurn"] = entry["firstMessageTurn"] or message["turn"]
            messages.append({"turn": message["turn"], "direction": "out" if outgoing else "in",
                             "slot": other, "username": names.get(other, "?"),
                             "text": message_text(message.get("body"))})

    # one message to several players is stored once per recipient
    grouped = {}
    for message in messages:
        key = (message["turn"], message["direction"], message["text"])
        if key in grouped:
            grouped[key]["recipients"].append(message["username"])
        else:
            grouped[key] = dict(message, recipients=[message["username"]])
    messages = sorted(grouped.values(), key=lambda m: (m["turn"], m["direction"]))

    first_warship = next((b for b in builds if b["warship"] and b["origin"] == "gebaut"), None)
    by_turn = {e["turn"]: e for e in economy}
    milestones = {
        "firstWarshipTurn": first_warship["turn"] if first_warship else None,
        "firstWarship": first_warship and "%s (%s)" % (first_warship["hull"], first_warship["engine"]),
        "freightersBeforeFirstWarship": sum(1 for b in builds if b["origin"] == "gebaut" and not b["warship"]
                                            and (not first_warship or b["turn"] <= first_warship["turn"])),
        "firstOwnMessageTurn": next((m["turn"] for m in messages if m["direction"] == "out"), None),
        "contactsWritten": sum(1 for c in contacts.values() if c["sent"]),
        "opening": {str(t): by_turn[t] for t in OPENING_TURNS if t in by_turn},
    }
    last = last_players.get(slot, {})
    return {
        "gameId": game_id, "gameName": game_name, "slot": slot, "username": me_name,
        "race": races_by_slot.get(slot, ""), "turns": max(turns),
        "finishRank": last.get("finishrank"), "generated": datetime.now().isoformat(timespec="seconds"),
        "milestones": milestones, "economy": economy, "tech": tech, "builds": builds,
        "contacts": sorted(contacts.values(), key=lambda c: c["slot"]), "messages": messages,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("zips", nargs="*")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--slot", type=int)
    parser.add_argument("--credentials", default="planets-credentials.properties")
    parser.add_argument("--out", default="data/analysis")
    args = parser.parse_args()

    officers = set()
    if args.slot is None:
        password = neo4j_password(args.credentials)
        officers = {row["name"] for row in query(password, "MATCH (o:Officer) RETURN DISTINCT toLower(o.username) AS name")}
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    zips = [Path(z) for z in args.zips] or sorted(Path("data/games").glob("game*.zip"))
    written = []
    for path in zips:
        game_id = int(re.search(r"(\d+)", path.stem).group(1))
        with zipfile.ZipFile(path) as archive:
            slot_turns = {}
            for name in archive.namelist():
                match = TURN_FILE.fullmatch(name)
                if match:
                    slot_turns.setdefault(int(match.group(1)), set()).add(int(match.group(2)))
            names = slot_names(archive, slot_turns)
            last = load_turn(archive, 1, max(slot_turns[1]))
            last_players = {p["id"]: p for p in last["players"]}
            races = {r["id"]: r["name"] for r in last["races"]}
            races_by_slot = {}
            for turn in sorted(slot_turns[1])[:5]:
                for player in load_turn(archive, 1, turn)["players"]:
                    if player["raceid"]:
                        races_by_slot.setdefault(player["id"], races.get(player["raceid"], RACES[player["raceid"]] if player["raceid"] < len(RACES) else ""))
            slots = [args.slot] if args.slot else [s for s, n in names.items() if n.lower() in officers]
            for slot in slots:
                target = out_dir / ("game%d-slot%d.json" % (game_id, slot))
                if target.exists() and not args.force:
                    continue
                result = analyse_slot(archive, game_id, slot, sorted(slot_turns[slot]), names, races_by_slot, last_players)
                target.write_text(json.dumps(result, ensure_ascii=False, indent=1), encoding="utf-8")
                written.append({"file": str(target), "game": game_id, "slot": slot, "username": result["username"]})
    print(json.dumps({"written": written, "officerSlotsFound": len(written)}, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
