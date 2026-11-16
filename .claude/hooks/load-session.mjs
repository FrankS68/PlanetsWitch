#!/usr/bin/env node
// SessionStart hook (startup|clear|compact): prints docs/session.md, current branch and
// `git status --short` to stdout so a fresh session starts with the handover. Always exit 0.
import { readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { execFileSync } from 'node:child_process';

let input = {};
try {
  input = JSON.parse(readFileSync(0, 'utf8') || '{}');
} catch {
  input = {};
}

const projectDir = process.env.CLAUDE_PROJECT_DIR || input.cwd || process.cwd();
const out = [];

function git(args) {
  try {
    return execFileSync('git', args, { cwd: projectDir, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trimEnd();
  } catch {
    return null;
  }
}

try {
  const sessionFile = resolve(projectDir, 'docs/session.md');
  out.push('# Übergabe aus docs/session.md');
  if (existsSync(sessionFile)) {
    out.push(readFileSync(sessionFile, 'utf8').trimEnd());
  } else {
    out.push('(docs/session.md fehlt – am Ende der Sitzung /savesession ausführen.)');
  }

  const branch = git(['branch', '--show-current']);
  const status = git(['status', '--short']);
  out.push('', '# Git');
  out.push(`Branch: ${branch || 'unbekannt'}`);
  if (status === null) out.push('git status --short: nicht verfügbar');
  else out.push(status ? `git status --short:\n${status}` : 'git status --short: sauber');

  if (input.source === 'compact') {
    out.push('', 'Hinweis: Der Kontext wurde gerade komprimiert. Für ein neues Thema besser /savesession und eine neue Sitzung starten.');
  } else {
    out.push('', 'Weiter bei „Nächster Schritt". Vor einem Themenwechsel /savesession und neue Sitzung.');
  }
} catch (e) {
  out.push(`(load-session: ${e.message})`);
}

process.stdout.write(out.join('\n') + '\n');
process.exit(0);
