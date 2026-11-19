# 0005 Aletheia takes over Urania

- **Status:** accepted
- **Date:** 2026-11-19

## Context

Urania (09:00/19:00) pflegte über die öffentliche API den Topofficer-Stand, verfolgte die Spiele
der Spitzenreiter und beantwortete API-Fragen von Artemis; Aletheia (09:30) hatte Graph,
Spielarchive und Analysen. Beide schrieben ins selbe Logbuch, Artemis musste Fragen auf zwei
Adressatinnen verteilen. Aletheia läuft ohnehin auf Spiekeroog, hat `topofficer_changes.py` mit
denselben Leaderboard-Daten und kann die öffentliche API genauso nutzen.

## Decision

Aletheia übernimmt alle Aufgaben von Urania; Urania läuft nicht mehr. Aletheia läuft um 09:30
(Hauptlauf, `.claude/agents/aletheia.md` inkl. Schritt 2b Topofficer-Stand) und um 19:00 kurz
(`~/agenten/aletheia-abend-prompt.md`: nur Fragen, kleine Aufgaben, Kommentare), damit Artemis'
Fragen aus der 18:30-Fragerunde bis 19:30 beantwortet sind. Artemis fragt nur noch „Aletheia“.
Das Logbuch behält den Titel „Logbuch Aletheia & Urania“, alte Urania-Einträge bleiben stehen.

## Consequences

- Eine Agentin weniger, keine Doppelarbeit beim Leaderboard; Fragen haben eine Adressatin.
- Aletheias Hauptlauf wird länger (mehr Tokens pro Lauf, Risiko am 5-Stunden-Limit des Pi).
- Der Abendlauf teilt sich die Sperre `/tmp/agent-aletheia.lock` mit dem Hauptlauf.

## Rejected alternatives

- Nur morgens laufen: Abendfragen von Artemis blieben bis zum nächsten Morgen offen.
- Urania behalten: zwei Agentinnen für überlappende Daten und ein geteilter Fragenkanal.
