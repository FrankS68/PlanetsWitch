# 0002 Agents run on Spiekeroog via cron

- **Status:** accepted
- **Date:** 2026-11-14

## Context

Aletheia lief als geplante Aufgabe in der Claude-Desktop-App auf Franks Windows-Rechner,
Urania und Artemis als Cloud-Routinen auf claude.ai. Der Windows-Rechner ist nicht immer an,
und die Cloud-Routinen erreichen weder die Neo4j noch die App im Heimnetz. Die Datenbank ist
seit 2026-11-14 auf den Raspberry Pi Spiekeroog umgezogen.

## Decision

Alle Agentinnen laufen auf Spiekeroog als Benutzer `claudine` mit Claude Code headless,
gestartet per Crontab über `~/agenten/run-agent.sh` (Modell, Gate, Colony-Tools und
Umgebung pro Agentin, siehe `docs/operations.md`). Die Windows-Aufgabe und die
Cloud-Routinen sind deaktiviert, nicht gelöscht.

## Consequences

- Läufe sind unabhängig vom Windows-Rechner; Zugriff auf Neo4j/App direkt im LAN.
- Kosten steuerbar über `<name>.model` und `<name>.gate`.
- Der Pi ist ein Single Point of Failure; Logs liegen nur lokal in `~/agenten/logs`.
- Prompt-Änderungen müssen auf dem Pi landen (Repo-Checkout bzw. `~/agenten/<name>-prompt.md`).

## Rejected alternatives

- Cloud-Routinen auf claude.ai: kein Zugriff auf Heimnetz-Neo4j ohne Freigabe nach außen.
- Geplante Aufgabe auf Windows: Rechner nicht durchgehend an, Datenbank liegt nicht mehr dort.
