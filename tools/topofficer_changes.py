"""Compares the latest top officer snapshot in Neo4j with earlier days.

Prints JSON for the topofficer analyst agent: per race the current top 10 with
rank and rating changes against the previous snapshot day and the day about a
week before, plus new entries and drop-outs.

Usage: python -I tools/topofficer_changes.py [path/to/planets-credentials.properties]
"""
import base64
import json
import sys
import urllib.request
from datetime import date, timedelta
from pathlib import Path

RACES = ["", "Solar Federation", "Lizard Alliance", "Empire of the Birds", "Hordes of Fury",
         "Privateer Bands", "Cyborg", "Crystal Confederation", "Evil Empire",
         "Robotic Imperium", "Rebel Confederation", "Missing Colonies of Man", "Horwasp Plague"]


def neo4j_password(properties_file):
    for line in Path(properties_file).read_text(encoding="utf-8").splitlines():
        if line.startswith("spring.neo4j.authentication.password="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("no neo4j password in %s" % properties_file)


def query(password, statement, **parameters):
    body = json.dumps({"statements": [{"statement": statement, "parameters": parameters}]}).encode()
    request = urllib.request.Request("http://localhost:7474/db/neo4j/tx/commit", data=body, headers={
        "Content-Type": "application/json",
        "Authorization": "Basic " + base64.b64encode(("neo4j:" + password).encode()).decode()})
    result = json.load(urllib.request.urlopen(request, timeout=30))
    if result["errors"]:
        raise SystemExit(json.dumps(result["errors"]))
    columns = result["results"][0]["columns"]
    return [dict(zip(columns, row["row"])) for row in result["results"][0]["data"]]


def snapshot(password, day):
    rows = query(password, """
        MATCH (s:OfficerSnapshot) WHERE toString(s.day) = $day
        RETURN s.officerId AS officerId, s.username AS username, s.accountid AS accountid,
               s.raceid AS raceid, s.rank AS rank, s.rating AS rating, s.levelname AS levelname,
               s.experience AS experience, s.tonssunk AS tonssunk""", day=day)
    return {row["officerId"]: row for row in rows}


def compare(current, earlier):
    races = {}
    for officer in sorted(current.values(), key=lambda o: (o["raceid"], o["rank"])):
        before = earlier.get(officer["officerId"]) if earlier else None
        entry = dict(officer)
        if earlier is not None:
            entry["rankBefore"] = before["rank"] if before else None
            entry["ratingChange"] = officer["rating"] - before["rating"] if before else None
            entry["levelBefore"] = before["levelname"] if before else None
        races.setdefault(officer["raceid"], {"race": RACES[officer["raceid"]], "top": [], "droppedOut": []})["top"].append(entry)
    if earlier:
        for officer in earlier.values():
            if officer["officerId"] not in current:
                races.setdefault(officer["raceid"], {"race": RACES[officer["raceid"]], "top": [], "droppedOut": []})["droppedOut"].append(
                    {"username": officer["username"], "rankBefore": officer["rank"], "rating": officer["rating"]})
    return races


def main():
    properties_file = sys.argv[1] if len(sys.argv) > 1 else "planets-credentials.properties"
    password = neo4j_password(properties_file)
    days = [row["day"] for row in query(password,
            "MATCH (s:OfficerSnapshot) RETURN DISTINCT toString(s.day) AS day ORDER BY day")]
    if not days:
        print(json.dumps({"error": "no snapshots yet"}))
        return
    latest = days[-1]
    previous = days[-2] if len(days) > 1 else None
    week_target = (date.fromisoformat(latest) - timedelta(days=7)).isoformat()
    week = max((d for d in days[:-1] if d <= week_target), default=None)
    current = snapshot(password, latest)
    print(json.dumps({
        "latestDay": latest,
        "latestIsToday": latest == date.today().isoformat(),
        "snapshotDays": len(days),
        "previousDay": previous,
        "weekDay": week,
        "vsPrevious": compare(current, snapshot(password, previous) if previous else None),
        "vsWeek": compare(current, snapshot(password, week)) if week else None,
    }, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
