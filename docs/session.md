# Sitzungsübergabe

Stand: 2026-11-16, Branch main

## Stand
- Übergabe-Workflow eingerichtet: CLAUDE.md, docs/operations.md, ADRs 0001–0004, /savesession, Hooks in .claude/settings.json.
- Hooks validate-session.mjs und load-session.mjs von Hand getestet; alles auf Branch session-workflow committet und gepusht.
- Auf Spiekeroog laufen Aletheia, Urania, Artemis und Kalliope per Cron, siehe docs/operations.md.
## Entscheidungen
- Sitzungen kurz halten: vor Themenwechsel /savesession, dann neue Sitzung; Grundlage docs/decisions/0001-record-architecture-decisions.md.
- docs/ wird im Repo versioniert, kein Backup-Spiegel.
- Kein Jira, kein Blockieren von /compact; nach /compact gibt load-session nur einen Hinweis.
## Stolperfallen
- Untracked Experimente (queries.cypher, result.json, reports/, src/main/frontend/neo4j*, planetsMenues.js) nie mitcommitten.
- Vaadin/Vite-Build-Dateien und .eclipse/ stehen jetzt in .gitignore.
- Hooks greifen erst in einer neu gestarteten Sitzung, nicht in der laufenden.
## Offene Punkte
- PR session-workflow → main anlegen und mergen (Frank).
- Workflow danach auf die Repos knownet und quiz übertragen.
- unsicher: ob Uranias alte Cloud-Routine nur deaktiviert oder gelöscht ist.
## Nächster Schritt
- Neue Sitzung im Repo starten und prüfen, dass der SessionStart-Hook diese Übergabe anzeigt.
