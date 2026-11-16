# Betrieb auf Spiekeroog

Spiekeroog ist ein Raspberry Pi 5 im Heimnetz (`spiekeroog.fritz.box`, Debian 12, arm64,
Zeitzone Europe/Berlin). Claude meldet sich per `ssh spiekeroog` als Benutzer `claudine` an
(kein sudo, Mitglied der Gruppe docker). Franks eigenes Konto heißt `frank`.
Details und Historie: Memory `spiekeroog-server`. Stand dieser Datei: 2026-11-16.

## planetswitch-Stack

- Verzeichnis `~/planetswitch` (git-Checkout, Remote `PlanetsWitch`, Branch `main`).
- `docker compose` mit der `docker-compose.yml` aus dem Repo; Container `planetswitch-app-1`
  (App, Port 8081) und `planetswitch-neo4j-1` (Neo4j, HTTP 7475, Bolt 7688).
- `.env` neben der Compose-Datei enthält Passwort und Ports – Inhalt nie ausgeben.
- Die Neo4j auf Spiekeroog ist die Master-Datenbank; lokales `data/` ist veraltet.
- Image wird auf dem Pi gebaut (~5 min): `docker compose up -d --build`.
- Code aktualisieren: `git fetch && git merge --ff-only PlanetsWitch/main`.
- Python-Tools laufen im App-Container:
  `docker compose exec -T app python3 -I tools/<skript>.py` (`./tools` ist read-only gemountet).
- Auf demselben Pi läuft auch knownet (Ports 8082/7474/7687) – nicht verwechseln.

## Agentinnen

Claude Code läuft headless als `claudine`. Jeder Lauf geht über
`~/agenten/run-agent.sh <name> <arbeitsverzeichnis> [promptdatei]`:

- Prompt aus `~/agenten/<name>-prompt.md`; Sperre per `flock`, Laufzeitlimit 3 h.
- `~/agenten/<name>.model` (optional): Modell, eine Zeile. Derzeit alle auf Sonnet.
- `~/agenten/<name>.gate` (optional, ausführbar): Exit ≠ 0 überspringt den Lauf ohne
  Claude-Aufruf, Eintrag in `logs/<name>-skipped.log`.
- `~/agenten/<name>.colony-tools` (optional): erlaubte Tools für The Colony
  (MCP-Server aus `~/agenten/colony-mcp.json`); ohne Datei keine Colony-Tools.
- Umgebungsdateien `~/.config/agenten/<name>.env` (chmod 600, API-Keys):
  `aletheia.env`, `artemis.env`, `kalliope.env`. Nur die Namen sind hier dokumentiert.
- Logs: `~/agenten/logs/<name>-JJJJ-MM-TT_HHMM.log`, 30 Tage aufbewahrt.

Crontab von `claudine` (Europe/Berlin):

| Zeit | Agentin | Arbeitsverzeichnis | Zweck |
|---|---|---|---|
| 07:00 | Kalliope | `~/agenten/kalliope` | Quellen für knownet (anderes Projekt) |
| 08:30, 11:30, 18:30, 19:30 | Artemis | `~/agenten/artemis` | Fragerunde (08:30/18:30), Spielrunde (11:30/19:30); Gate begrenzt Abendläufe |
| 09:00, 19:00 | Urania | `~/agenten/urania` | Topofficer-Stand, Antworten auf Fragen |
| 09:30 | Aletheia | `~/planetswitch` | Topofficer-Bericht, Analysen, Antworten |

Ablauf und Rollen: ADR 0002 (Cron auf Spiekeroog) und ADR 0004 (Fragen sind Informationsanfragen).
unsicher: ob Uranias alte Cloud-Routine nur deaktiviert oder gelöscht ist.

## Logbücher

Die Agentinnen haben kein Gedächtnis zwischen Läufen; ihr Stand liegt in Claude-Docs
(Connector `mcp__claude_ai_Claude_Docs__*`):

- „Logbuch Aletheia & Urania" – Anweisungen, Topofficer-Stand, Abschnitt „Fragen von Lomize", Log-Tabelle.
- „Artemis-Logbuch" – Pläne und Notizen von Artemis.
- Kalliope hat ein eigenes Logbuch (knownet).

IDs und Links der Docs stehen im Memory `aletheia-topofficer-agent`.

## Typische Prüfungen (nur lesend)

```bash
ssh spiekeroog 'crontab -l'
ssh spiekeroog 'cd ~/planetswitch && docker compose ps'
ssh spiekeroog 'ls -t ~/agenten/logs | head'
```
