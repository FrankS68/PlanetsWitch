---
description: Write docs/session.md as a handover to tomorrow's session (five fixed sections)
model: sonnet
---

Schreibe `docs/session.md` neu – als Übergabe an „die Sitzung von morgen", die nichts von
diesem Gespräch weiß und nur diese Datei, `CLAUDE.md` und `docs/` sieht. Sprache: Deutsch.

## Quellen (nur diese)

1. Der bisherige Gesprächsverlauf dieser Sitzung.
2. `git status --short`, `git branch --show-current` und `git log --oneline -10`;
   für nicht committete Änderungen `git diff --stat`.
3. Die bestehende `docs/session.md` (was davon noch gilt, bleibt; Erledigtes fliegt raus).
4. `docs/decisions/` (ADRs) und `docs/operations.md` – nur verlinken, nicht abschreiben.

## Format (wird vom Hook `validate-session.mjs` geprüft)

```
# Sitzungsübergabe

Stand: JJJJ-MM-TT, Branch <branch>

## Stand
- …
## Entscheidungen
- …
## Stolperfallen
- …
## Offene Punkte
- …
## Nächster Schritt
- …
```

- Genau diese fünf Abschnitte in dieser Reihenfolge, keine weiteren `##`-Überschriften.
- Vor `## Stand` nur Titel und die Zeile „Stand: …".
- Jeder Eintrag ist eine Zeile `- …` mit höchstens 25 Wörtern; keine Unterpunkte, kein Fließtext.
- Höchstens 5 Einträge je Abschnitt, **genau 1** unter „Nächster Schritt"; ein leerer
  Abschnitt bekommt `- keine`.
- Die ganze Datei höchstens 60 Zeilen.

## Inhalt

- **Stand:** was jetzt funktioniert bzw. erledigt ist, inkl. Branch und ob committet/gepusht.
- **Entscheidungen:** in dieser Sitzung getroffen; Dauerhaftes als ADR in `docs/decisions/`
  anlegen (Format siehe `0001-…`) oder ins Auto-Memory schreiben und hier nur verlinken.
- **Stolperfallen:** was Zeit gekostet hat und wieder passieren kann (Befehl, Pfad, Fehler).
- **Offene Punkte:** angefangen, aber nicht fertig oder nicht geprüft.
- **Nächster Schritt:** die eine konkrete Aufgabe, mit der die nächste Sitzung beginnt.

## Regeln

- Nichts erfinden. Was nicht belegt ist, mit „unsicher:" markieren.
- Keine Secrets, Passwörter, Tokens oder API-Keys, auch nicht teilweise.
- Rassen immer als „Name (ID)".
- Nichts committen oder pushen.
- Zum Schluss: Datei schreiben, Meldungen des Hooks beheben, dann in 2–3 Sätzen sagen,
  was drinsteht, und empfehlen, für das nächste Thema eine neue Sitzung zu starten.
