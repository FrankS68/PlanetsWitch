"""Analyses the last finished games of one planets.nu player via the public API.

Writes data/analysis/player-<username>.json with, per game, the outcome (win,
survived, eliminated, resigned), the player's score curve per turn (planets, ships,
starbases, military score, planet rank against the leader), alliances and the
deaths of the other slots. An "assessment" (list of paragraphs written by Aletheia) is
kept when the file is rewritten. The Vaadin view "Spieleranalyse" (/playeranalysis) shows it.
Needs no Neo4j and no archives, only api.planets.nu.

Usage: python -I tools/player_analysis.py <username> [--games 10]
"""
import argparse
import gzip
import json
import re
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

RACES = ["", "Solar Federation", "Lizard Alliance", "Empire of the Birds", "Hordes of Fury",
         "Privateer Bands", "Cyborg", "Crystal Confederation", "Evil Empire",
         "Robotic Imperium", "Rebel Confederation", "Missing Colonies of Man", "Horwasp Plague"]
MILESTONE_TURNS = (10, 20, 30, 50, 80)
# event types of game/loadinfo
JOINED, RESIGNED, ALLIANCE_FORMED, GAME_WON, SLOT_DEAD, ALLIANCE_BROKEN, DROPPED = 3, 8, 4, 6, 7, 9, 10
OUTPUT_DIR = Path(__file__).resolve().parent.parent / "data" / "analysis"


def api(path, **parameters):
    url = "http://api.planets.nu/%s?%s" % (path, urllib.parse.urlencode(parameters))
    request = urllib.request.Request(url, headers={"Accept-Encoding": "gzip"})
    with urllib.request.urlopen(request, timeout=120) as response:
        raw = response.read()
        if response.headers.get("Content-Encoding") == "gzip":
            raw = gzip.decompress(raw)
    return json.loads(raw)


def slot_usernames(events):
    """username per slot as joined during the game (later turns show eliminated slots as "dead")"""
    names = {}
    for event in sorted(events, key=lambda event: (event["turn"], event["id"])):
        match = re.match(r"(.+) has joined the game in slot (\d+)", event["description"])
        if event["eventtype"] == JOINED and match:
            names[int(match.group(2))] = match.group(1)
    return names


def winners(events):
    for event in events:
        if event["eventtype"] == GAME_WON:
            return [name.replace("+", " ") for name in re.findall(r"@(\S+)", event["description"])]
    return []


def analyse_game(entry, username):
    game_id = entry["gameid"]
    info = api("game/loadinfo", gameid=game_id)
    scores = api("game/loadscores", gameid=game_id)["scores"]
    events = info["events"]
    account_id = entry["accountid"]
    slot = next((score["ownerid"] for score in scores if score["accountid"] == account_id), None)
    names = slot_usernames(events)
    slot_races = {player["id"]: RACES[player["raceid"]] for player in info["players"]}

    by_turn = {}
    for score in scores:
        by_turn.setdefault(score["turn"], {})[score["ownerid"]] = score
    curve = []
    for turn in sorted(by_turn):
        own = by_turn[turn].get(slot)
        if own is None:
            continue
        others = [score for owner, score in by_turn[turn].items() if owner != slot]
        leader = max(others, key=lambda score: score["planets"], default=None)
        curve.append({
            "turn": turn,
            "planets": own["planets"],
            "capitalShips": own["capitalships"],
            "freighters": own["freighters"],
            "starbases": own["starbases"],
            "military": own["militaryscore"],
            "planetRank": 1 + sum(1 for score in others if score["planets"] > own["planets"]),
            "militaryRank": 1 + sum(1 for score in others if score["militaryscore"] > own["militaryscore"]),
            "leaderPlanets": leader["planets"] if leader else 0,
            "leader": names.get(leader["ownerid"], slot_races.get(leader["ownerid"], "")) if leader else "",
        })
    death_turn = next((event["turn"] for event in events
                       if event["eventtype"] == SLOT_DEAD and event["playerid"] == slot), None)
    turn_left = entry["turnleft"] or None
    # the slot keeps getting scores after the player is gone (leftover planets, a predecessor before
    # a late join), so only the turns the player actually played count
    last_alive = min(turn for turn in (turn_left, death_turn, curve[-1]["turn"] if curve else 0) if turn)
    played = [point for point in curve if entry["turnjoined"] <= point["turn"] <= last_alive]
    at_end = played[-1] if played else None
    won = winners(events)
    if username.lower() in (name.lower() for name in won):
        partners = [name for name in won if name.lower() != username.lower()]
        outcome = "Sieg" + (" (mit %s)" % ", ".join(partners) if partners else "")
        category = "win"
    elif turn_left is not None:
        # leaving removes the ships at once, the remaining planets stay in the slot's score
        outcome = "aufgegeben in Zug %d (noch %d Planeten, %d Kriegsschiffe)" % (
            turn_left, at_end["planets"] if at_end else 0, at_end["capitalShips"] if at_end else 0)
        category = "left"
    elif death_turn is not None:
        outcome = "ausgeschieden in Zug %d" % death_turn
        category = "dead"
    else:
        outcome = "überlebt, Platz %d" % entry["finishrank"]
        category = "survived"

    alliances = []
    for event in sorted(events, key=lambda event: event["turn"]):
        if event["eventtype"] in (ALLIANCE_FORMED, ALLIANCE_BROKEN) and "(%s)" % username in event["description"]:
            partner = [name for name in re.findall(r"\(([^)]+)\)", event["description"]) if name != username]
            alliances.append({"turn": event["turn"], "partner": partner[0] if partner else "",
                              "formed": event["eventtype"] == ALLIANCE_FORMED})
    deaths = [{"turn": event["turn"], "slot": event["playerid"], "race": slot_races.get(event["playerid"], ""),
               "username": names.get(event["playerid"], "")}
              for event in sorted(events, key=lambda event: event["turn"]) if event["eventtype"] == SLOT_DEAD]

    peak = max(played, key=lambda point: point["planets"], default=None)
    milestones = {str(turn): point for turn in MILESTONE_TURNS for point in played if point["turn"] == turn}
    game = entry["game"]
    return {
        "gameId": game_id,
        "gameName": game["name"],
        "gameType": game["shortdescription"],
        "slot": slot,
        "race": RACES[entry["raceid"]],
        "dateEnded": game["dateended"][:10],
        "turns": game["turn"],
        "players": game["slots"],
        "finishRank": entry["finishrank"],
        "outcome": outcome,
        "category": category,
        "winners": won,
        "turnJoined": entry["turnjoined"],
        "turnLeft": turn_left,
        "deathTurn": death_turn,
        "lastAliveTurn": last_alive,
        "peakPlanets": peak["planets"] if peak else 0,
        "peakPlanetsTurn": peak["turn"] if peak else None,
        # in the first turns everybody has the homeworld only, so the rank counts from turn 10
        "bestPlanetRank": min((point["planetRank"] for point in played if point["turn"] >= 10), default=None),
        "turnsAsPlanetLeader": sum(1 for point in played if point["turn"] >= 10 and point["planetRank"] == 1),
        "finalPlanets": at_end["planets"] if at_end else 0,
        "finalPlanetRank": at_end["planetRank"] if at_end else None,
        "militaryPercent": entry["military"],
        "turnsMissed": next((player["turnsmissedtotal"] for player in info["players"] if player["id"] == slot), None),
        "milestones": milestones,
        "alliances": alliances,
        "deaths": deaths,
        "curve": curve,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("username")
    parser.add_argument("--games", type=int, default=10)
    args = parser.parse_args()
    profile = api("account/loadprofile", username=args.username)
    if not profile.get("success"):
        raise SystemExit("unknown player %s" % args.username)
    account = profile["account"]
    history = api("account/history", accountid=account["id"])["history"]
    finished = sorted((entry for entry in history if entry["game"]["status"] == 3),
                      key=lambda entry: entry["game"]["dateended"], reverse=True)[:args.games]
    games = [analyse_game(entry, account["username"]) for entry in finished]
    result = {
        "username": account["username"],
        "accountId": account["id"],
        "title": account.get("_title"),
        "totalTurns": account["turns"],
        "turnsMissed": account["turnsmissed"],
        "generated": datetime.now().isoformat(timespec="seconds"),
        "games": games,
    }
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    target = OUTPUT_DIR / ("player-%s.json" % re.sub(r"[^a-z0-9_-]+", "_", account["username"].lower()))
    if target.exists():
        # the written assessment (paragraphs by Aletheia) survives a refresh of the numbers
        result["assessment"] = json.loads(target.read_text(encoding="utf-8")).get("assessment", [])
    target.write_text(json.dumps(result, ensure_ascii=False, indent=1), encoding="utf-8")
    print(json.dumps({"written": str(target), "games": [
        {key: game[key] for key in ("gameId", "gameName", "race", "outcome", "peakPlanets", "peakPlanetsTurn")}
        for game in games]}, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
