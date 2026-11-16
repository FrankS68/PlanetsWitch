# 0003 Rank ("Platz") is the position in the API list

- **Status:** accepted
- **Date:** 2026-11-14

## Context

Das Leaderboard von `api.planets.nu/account/leaderboard` liefert `topofficers` als Liste
pro Rasse (Index = Rassen-ID 1–12) mit je 10 Offizieren. Die Liste ist nicht nach Rating
sortiert. App, Aletheia und Urania hatten den „Platz" unterschiedlich bestimmt.

## Decision

„Platz" ist die Position eines Offiziers in der Liste seiner Rasse, so wie die API sie
liefert (1 = erster Eintrag). Nie nach Rating umsortieren. Gilt für App, Berichte und alle
Agentinnen.

## Consequences

- Alle zählen gleich; Berichte und Chart sind vergleichbar.
- Ein höherer Platz bedeutet nicht zwingend ein höheres Rating; Berichte müssen das nicht erklären.

## Rejected alternatives

- Platz nach Rating innerhalb der Rasse: weicht von der Darstellung auf planets.nu ab.
