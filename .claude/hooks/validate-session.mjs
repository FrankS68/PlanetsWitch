#!/usr/bin/env node
// PostToolUse hook (Write|Edit): validates docs/session.md against the handover format.
// Exit 0 = ok or not our file, exit 2 = violations (reported on stderr, fed back to Claude).
import { readFileSync, existsSync } from 'node:fs';
import { resolve, isAbsolute } from 'node:path';

const SECTIONS = ['Stand', 'Entscheidungen', 'Stolperfallen', 'Offene Punkte', 'Nächster Schritt'];
const MAX_LINES = 60;
const MAX_ENTRIES = 5;
const MAX_WORDS = 25;
const MAX_PREAMBLE = 4; // non-empty lines before the first section (title + "Stand:" line)

function readStdin() {
  try {
    return readFileSync(0, 'utf8');
  } catch {
    return '';
  }
}

let input = {};
try {
  input = JSON.parse(readStdin() || '{}');
} catch {
  process.exit(0); // not a hook payload we understand; never block on that
}

const projectDir = process.env.CLAUDE_PROJECT_DIR || input.cwd || process.cwd();
let filePath = input?.tool_input?.file_path || input?.tool_input?.path || '';
if (!filePath) process.exit(0);
if (!isAbsolute(filePath)) filePath = resolve(projectDir, filePath);

const normalized = filePath.replace(/\\/g, '/').toLowerCase();
const target = resolve(projectDir, 'docs/session.md').replace(/\\/g, '/').toLowerCase();
if (normalized !== target) process.exit(0);
if (!existsSync(filePath)) process.exit(0);

function validate(text) {
  const errors = [];
  const lines = text.replace(/\r\n/g, '\n').replace(/\n+$/, '').split('\n');
  if (lines.length > MAX_LINES) errors.push(`Datei hat ${lines.length} Zeilen, erlaubt sind ${MAX_LINES}.`);

  const found = [];
  let current = null;
  const entries = {};
  let preamble = 0;

  lines.forEach((raw, i) => {
    const line = raw.trimEnd();
    const n = i + 1;
    if (line.trim() === '') return;
    const h2 = line.match(/^##\s+(.+?)\s*$/);
    if (h2) {
      current = h2[1];
      found.push(current);
      if (!SECTIONS.includes(current)) errors.push(`Zeile ${n}: unbekannter Abschnitt "## ${current}".`);
      entries[current] = entries[current] || 0;
      return;
    }
    if (current === null) {
      preamble++;
      if (line.startsWith('- ')) errors.push(`Zeile ${n}: Eintrag vor dem ersten Abschnitt.`);
      return;
    }
    if (!line.startsWith('- ')) {
      errors.push(`Zeile ${n}: im Abschnitt "${current}" sind nur Einträge "- …" erlaubt.`);
      return;
    }
    entries[current]++;
    const words = line.slice(2).trim().split(/\s+/).filter(Boolean).length;
    if (words > MAX_WORDS) errors.push(`Zeile ${n}: ${words} Wörter, erlaubt sind ${MAX_WORDS}.`);
  });

  if (preamble > MAX_PREAMBLE) errors.push(`Vor "## Stand" stehen ${preamble} Zeilen, erlaubt sind ${MAX_PREAMBLE}.`);
  if (found.join('|') !== SECTIONS.join('|')) {
    errors.push(`Abschnitte müssen genau sein: ${SECTIONS.map((s) => '## ' + s).join(', ')} (gefunden: ${found.map((s) => '## ' + s).join(', ') || 'keine'}).`);
  }
  for (const s of SECTIONS) {
    const c = entries[s] ?? 0;
    if (s === 'Nächster Schritt') {
      if (found.includes(s) && c !== 1) errors.push(`"## ${s}" braucht genau 1 Eintrag, hat ${c}.`);
    } else {
      if (c > MAX_ENTRIES) errors.push(`"## ${s}" hat ${c} Einträge, erlaubt sind ${MAX_ENTRIES}.`);
      if (found.includes(s) && c === 0) errors.push(`"## ${s}" ist leer; mindestens "- keine" schreiben.`);
    }
  }
  return errors;
}

const errors = validate(readFileSync(filePath, 'utf8'));
if (errors.length) {
  process.stderr.write(`docs/session.md verletzt das Übergabeformat (siehe .claude/commands/savesession.md):\n- ${errors.join('\n- ')}\n`);
  process.exit(2);
}
process.exit(0);
