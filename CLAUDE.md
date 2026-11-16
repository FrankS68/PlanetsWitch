# PlanetsWitch

Analyse-Tool für das Online-Strategiespiel planets.nu: Spring Boot 4 + Vaadin + Neo4j.
Lädt Spiele, Spieler, Leaderboard und Turn-Archive über die öffentliche API von planets.nu
in einen Graphen und zeigt Auswertungen in Vaadin-Views (Topofficer-Chart, Graph, Spiel- und
Spieleranalysen). Python-Hilfsskripte liegen in `tools/`, ihre Ergebnisse in `data/analysis/`.
Drei Agentinnen arbeiten mit: **Aletheia** (Topofficer-Bericht, Analysen, `.claude/agents/aletheia.md`),
**Urania** (Topofficer-Stand, Antworten auf Fragen) und **Artemis** (spielt selbst, Account „lomize").
Sie laufen per Cron auf dem Raspberry Pi Spiekeroog, siehe `docs/operations.md`.

## Start here

1. Zu Beginn `docs/session.md` lesen und bei **„Nächster Schritt"** weitermachen
   (der SessionStart-Hook zeigt die Datei automatisch an).
2. Getroffene Entscheidungen stehen in `docs/decisions/` (ADRs). Nicht neu verhandeln,
   sondern bei Bedarf ein neues ADR schreiben, das ein altes ablöst.
3. Vor einem Themenwechsel: `/savesession` ausführen und eine **neue Sitzung** starten,
   statt die alte weiterzuführen. Lange Sitzungen kosten bei jeder Runde den ganzen Kontext.
4. Dauerhafte Fakten (Umgebung, Server, Vorlieben) liegen im Auto-Memory
   (`~/.claude/projects/C--Users-frank-git-planetswitch/memory/`, Index `MEMORY.md`).
   Hier nur verweisen, nicht kopieren.

## Build und Test

- Kein `java` im PATH: JDK-Pfad und Befehle siehe Memory `planetswitch-dev-environment`.
- Kompilieren (offline, ohne Frontend):
  `./mvnw -q -o compile -Dvaadin.skip=true` (vorher `JAVA_HOME` setzen).
- App starten: `./mvnw -o spring-boot:run` – Port 8081 ist Franks Testinstanz (läuft oft schon);
  für eigene Tests eine zweite Instanz auf 8091 starten und danach beenden.
- Die Testinstanz nutzt die echte Neo4j-Datenbank: Testdaten danach wieder entfernen.
- Docker-Betrieb (auch arm64): `docker compose up -d --build`, siehe `README.md`.

## Regeln, die beißen

Details stehen im Auto-Memory; hier nur die Kurzfassung mit Verweis.

- **Git** (`git-workflow`): Remote heißt `PlanetsWitch`, nicht `origin`. Commit und Push nur auf
  Franks ausdrückliche Anfrage. Ein Branch pro Feature, PR gegen `main`, Frank mergt selbst.
  Untracked Experimente (`queries.cypher`, `result.json`, `src/main/frontend/neo4j*.js`) nie mitcommitten.
- **Rassen** (`race-names-in-output`): in allem Lesbaren immer „Name (ID)",
  z. B. „Robotic Imperium (9)", nie die nackte ID.
- **Analyseergebnisse** (`analysis-results-in-vaadin`): Python ist für Analysen erlaubt,
  Ergebnisse landen aber immer in der Vaadin-App (JSON in `data/analysis/` + View).
- **Platz** (`aletheia-topofficer-agent`, ADR 0003): Platz = Position in der
  Leaderboard-Liste der API, nie nach Rating umsortieren.
- **Archive** (`game-archive-download-policy`): nur Siege der letzten 30 Tage, 10 GB Obergrenze.
- **Sprache**: mit Frank Deutsch; Commit-Messages und Code-Kommentare Englisch.
- **Secrets**: `planets-credentials.properties`, `.env` und `~/.config/agenten/*.env` nie lesen,
  ausgeben oder committen.

## Wo was liegt

- `src/main/java/...` – Spring-Boot-App, Vaadin-Views, Neo4j-Repositories
- `tools/` – Python-Skripte (`topofficer_changes.py`, `game_analysis.py`, `player_analysis.py`)
- `data/` – Spielarchive und Analyse-JSON (lokal veraltet, Master ist Spiekeroog)
- `reports/` – Aletheias Berichte
- `.claude/agents/` – Agentinnen-Prompts; `.claude/commands/` – Slash-Commands
- `.claude/hooks/` – Hooks für den Übergabe-Workflow (`docs/session.md`)
- `docs/` – `session.md` (Übergabe), `operations.md` (Betrieb), `decisions/` (ADRs)
