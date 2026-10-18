'use strict';
const http = require('http');
const crypto = require('crypto');
const { load } = require('./config');
const { buildPrompt, parseReply } = require('./prompt');
const { runClaude } = require('./claude');
const { enrich } = require('./enrich');

const MAX_BODY = 300 * 1024;
const MAX_PARALLEL = 2;

function createServer(cfg, deps = {}) {
  const run = deps.runClaude || runClaude;
  const enrichBooks = deps.enrich || enrich;
  let active = 0;

  const send = (res, status, body) => {
    const data = JSON.stringify(body);
    res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Content-Length': Buffer.byteLength(data), 'Cache-Control': 'no-store' });
    res.end(data);
  };
  const tokenOk = (req) => {
    const given = Buffer.from(String(req.headers['x-token'] || ''));
    const want = Buffer.from(cfg.appToken);
    return want.length > 0 && given.length === want.length && crypto.timingSafeEqual(given, want);
  };
  const readBody = (req) => new Promise((resolve, reject) => {
    let size = 0; const chunks = [];
    req.on('data', (c) => { size += c.length; if (size > MAX_BODY) { reject(Object.assign(new Error('İstek çok büyük.'), { status: 413 })); req.destroy(); } else chunks.push(c); });
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });

  return http.createServer(async (req, res) => {
    const url = new URL(req.url, 'http://x');
    try {
      if (req.method === 'GET' && url.pathname === '/api/health') return send(res, 200, { ok: true, app: 'kitapligim', tokenSet: cfg.appToken.length > 0 });
      if (!tokenOk(req)) return send(res, 401, { error: 'Geçersiz anahtar.' });
      if (req.method === 'GET' && url.pathname === '/api/ping') return send(res, 200, { ok: true });
      if (req.method === 'POST' && url.pathname === '/api/chat') {
        let body;
        try { body = JSON.parse(await readBody(req)); } catch (e) { return send(res, e.status || 400, { error: e.status ? e.message : 'İstek okunamadı.' }); }
        let prompt;
        try { prompt = buildPrompt(body); } catch (e) { return send(res, 400, { error: e.message }); }
        if (active >= MAX_PARALLEL) return send(res, 429, { error: 'Danışman şu an meşgul, birkaç saniye sonra tekrar dene.' });
        active++;
        const started = Date.now();
        try {
          const parsed = parseReply(await run(prompt, cfg));
          const books = cfg.enrich && parsed.books.length ? await enrichBooks(parsed.books) : parsed.books.map((b) => ({ ...b, isbn: '', totalPages: 0, cover: '', source: 'Danışman', verified: false }));
          console.log(`${new Date().toISOString()} chat ok ${Date.now() - started}ms books=${books.length}`);
          return send(res, 200, { reply: parsed.reply, books });
        } catch (e) {
          console.error(`${new Date().toISOString()} chat error ${e.code || ''} ${e.message} ${e.detail || ''}`);
          return send(res, e.code === 'timeout' ? 504 : 502, { error: e.code === 'spawn' || e.code === 'timeout' ? e.message : 'Danışman şu an yanıt veremiyor. PC\'deki Claude oturumunu kontrol et.' });
        } finally { active--; }
      }
      send(res, 404, { error: 'Bulunamadı.' });
    } catch (e) {
      console.error(`${new Date().toISOString()} server error ${e.message}`);
      if (!res.headersSent) send(res, 500, { error: 'Sunucu hatası.' });
    }
  });
}

if (require.main === module) {
  const cfg = load();
  if (!cfg.appToken) { console.error('APP_TOKEN boş. server/.env dosyasına uzun bir anahtar yaz (bkz. .env.example).'); process.exit(1); }
  createServer(cfg).listen(cfg.port, cfg.host, () => console.log(`Kitaplığım sunucusu http://${cfg.host}:${cfg.port} adresinde dinliyor.`));
}

module.exports = { createServer };
