'use strict';
const fs = require('fs');
const path = require('path');

function parseEnv(text) {
  const out = {};
  for (const line of text.split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (!m || line.trim().startsWith('#')) continue;
    out[m[1]] = m[2].replace(/^(['"])(.*)\1$/, '$2');
  }
  return out;
}

function load(envFile = path.join(__dirname, '..', '.env'), env = process.env) {
  let file = {};
  try { file = parseEnv(fs.readFileSync(envFile, 'utf8')); } catch {}
  const get = (k, d) => (env[k] !== undefined && env[k] !== '' ? env[k] : file[k] !== undefined && file[k] !== '' ? file[k] : d);
  return {
    appToken: get('APP_TOKEN', ''),
    port: Number(get('PORT', '8790')),
    host: get('HOST', '0.0.0.0'),
    claudeBin: get('CLAUDE_BIN', 'claude'),
    claudePrefixArgs: get('CLAUDE_PREFIX_ARGS', '').split(' ').filter(Boolean),
    claudeModel: get('CLAUDE_MODEL', 'sonnet'),
    claudeTimeoutMs: Number(get('CLAUDE_TIMEOUT_MS', '120000')),
    enrich: get('ENRICH', '1') === '1',
  };
}

module.exports = { load, parseEnv };
