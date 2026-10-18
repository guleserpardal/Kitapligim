'use strict';
const { spawn } = require('child_process');

/**
 * Runs one isolated, tool-less Claude Code turn. --safe-mode skips CLAUDE.md, hooks, MCP and plugins,
 * so the family's chat never mixes with the PC owner's own Claude sessions.
 */
function runClaude(prompt, cfg) {
  return new Promise((resolve, reject) => {
    const args = [...cfg.claudePrefixArgs, '-p', '--safe-mode', '--tools', '', '--no-session-persistence', '--output-format', 'text', '--model', cfg.claudeModel];
    const child = spawn(cfg.claudeBin, args, { stdio: ['pipe', 'pipe', 'pipe'], windowsHide: true });
    let out = '', err = '', done = false;
    const finish = (fn, v) => { if (done) return; done = true; clearTimeout(timer); fn(v); };
    const timer = setTimeout(() => { child.kill(); finish(reject, Object.assign(new Error('Claude zamanında yanıt vermedi.'), { code: 'timeout' })); }, cfg.claudeTimeoutMs);
    child.stdout.on('data', (d) => { out += d; if (out.length > 200000) child.kill(); });
    child.stderr.on('data', (d) => { err += d; if (err.length > 20000) err = err.slice(-20000); });
    child.on('error', (e) => finish(reject, Object.assign(new Error(e.code === 'ENOENT' ? 'Claude Code bu PC\'de bulunamadı (CLAUDE_BIN).' : e.message), { code: 'spawn' })));
    child.on('close', (code) => {
      if (code === 0 && out.trim()) return finish(resolve, out);
      finish(reject, Object.assign(new Error('Claude yanıt üretemedi.'), { code: 'exit', detail: (err || out).slice(-500) }));
    });
    child.stdin.on('error', () => {});
    child.stdin.end(prompt);
  });
}

module.exports = { runClaude };
