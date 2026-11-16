# 0001 Record architecture decisions

- **Status:** accepted
- **Date:** 2026-11-16

## Context

Entscheidungen zu planetswitch und den Agentinnen wurden bisher nur im Gesprächsverlauf und im
Auto-Memory festgehalten. Neue Sitzungen müssen sie mühsam wiederfinden oder verhandeln sie neu.
Lange Sitzungen, die alles im Kontext behalten, kosten zu viel vom Wochenlimit.

## Decision

Wichtige, schwer umkehrbare Entscheidungen werden als ADR in `docs/decisions/` abgelegt:
eine Datei pro Entscheidung, `NNNN-kurzer-titel.md`, fortlaufend nummeriert.
Format: Titel, Status (proposed / accepted / superseded by NNNN), Date, Context, Decision,
Consequences, Rejected alternatives. Ein ADR wird nicht umgeschrieben; eine Änderung bekommt
ein neues ADR, das alte erhält den Status „superseded by NNNN".
`docs/session.md` verlinkt ADRs nur, statt sie zu wiederholen.

## Consequences

- Neue Sitzungen finden Entscheidungen mit wenig Kontext.
- Kleiner Mehraufwand beim Entscheiden.
- Fakten ohne Entscheidungscharakter (Ports, Pfade, Vorlieben) bleiben im Auto-Memory.

## Rejected alternatives

- Alles im Auto-Memory: nicht versioniert, nicht im Repo sichtbar, mischt Fakten und Entscheidungen.
- Ticketsystem (Jira o. Ä.): gibt es nicht; die Logbücher der Agentinnen liegen in Claude-Docs.
