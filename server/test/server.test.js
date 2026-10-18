'use strict';
const test = require('node:test');
const assert = require('node:assert');
const path = require('path');
const { createServer } = require('../src/server');
const { buildPrompt, parseReply } = require('../src/prompt');
const { parseEnv } = require('../src/config');
const { sameTitle } = require('../src/enrich');

const cfg = { appToken: 'gizli-anahtar', port: 0, host: '127.0.0.1', claudeBin: process.execPath, claudePrefixArgs: [path.join(__dirname, 'fake-claude.js')], claudeModel: 'sonnet', claudeTimeoutMs: 5000, enrich: false };

async function withServer(fn, c = cfg, deps) {
  const server = createServer(c, deps);
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  const base = `http://127.0.0.1:${server.address().port}`;
  try { await fn(base); } finally { server.close(); }
}
const chat = (base, body, token = cfg.appToken) => fetch(base + '/api/chat', { method: 'POST', headers: { 'X-Token': token, 'Content-Type': 'application/json' }, body: JSON.stringify(body) });

test('health anahtarsız açık, sohbet anahtarsız 401', () => withServer(async (base) => {
  assert.equal((await fetch(base + '/api/health')).status, 200);
  assert.equal((await chat(base, { messages: [] }, 'yanlis')).status, 401);
  assert.equal((await fetch(base + '/api/chat', { method: 'POST' })).status, 401);
}));

test('APP_TOKEN boşsa hiçbir anahtar kabul edilmez', () => withServer(async (base) => {
  assert.equal((await chat(base, { messages: [] }, '')).status, 401);
}, { ...cfg, appToken: '' }));

test('sohbet: izole modda Claude çağrılır ve kitaplar döner', () => withServer(async (base) => {
  const r = await chat(base, { profile: 'Elis', library: [{ title: 'Küçük Prens', status: 'done', rating: 5 }], messages: [{ role: 'user', content: 'Gerilim istiyorum' }] });
  assert.equal(r.status, 200);
  const j = await r.json();
  assert.equal(j.reply, 'ok [safe]');
  assert.equal(j.books[0].title, 'Hizmetçi');
  assert.equal(j.books[0].verified, false);
}));

test('sohbet: son mesaj kullanıcıdan değilse 400', () => withServer(async (base) => {
  assert.equal((await chat(base, { messages: [{ role: 'assistant', content: 'merhaba' }] })).status, 400);
  assert.equal((await chat(base, { messages: [] })).status, 400);
}));

test('sohbet: Claude çökerse 502 ve ayrıntı sızmaz', () => withServer(async (base) => {
  const r = await chat(base, { messages: [{ role: 'user', content: 'HATA_ÜRET' }] });
  assert.equal(r.status, 502);
  assert.ok(!JSON.stringify(await r.json()).includes('boom'));
}));

test('sohbet: aynı anda ikiden fazla istek 429', () => {
  let release; const gate = new Promise((r) => (release = r));
  return withServer(async (base) => {
    const body = { messages: [{ role: 'user', content: 'a' }] };
    const first = [chat(base, body), chat(base, body)];
    await new Promise((r) => setTimeout(r, 50));
    assert.equal((await chat(base, body)).status, 429);
    release();
    assert.deepEqual((await Promise.all(first)).map((r) => r.status), [200, 200]);
  }, cfg, { runClaude: async () => { await gate; return '{"reply":"x","books":[]}'; } });
});

test('parseReply: kod bloğu, bozuk JSON ve düz metin', () => {
  assert.equal(parseReply('```json\n{"reply":"a","books":[{"title":"T","author":"A","why":"W"}]}\n```').books.length, 1);
  assert.equal(parseReply('Merhaba {bozuk').reply, 'Merhaba {bozuk');
  assert.equal(parseReply('').books.length, 0);
  assert.equal(parseReply(JSON.stringify({ reply: 'x', books: Array.from({ length: 9 }, (_, i) => ({ title: 'T' + i })) })).books.length, 5);
});

test('buildPrompt: kitaplık ve geçmiş sınırlanır', () => {
  const p = buildPrompt({ profile: 'Güleser', library: [{ title: 'Hizmetçi', author: 'F', status: 'done', rating: 4 }], messages: Array.from({ length: 30 }, (_, i) => ({ role: i % 2 ? 'assistant' : 'user', content: 'm' + i })).concat([{ role: 'user', content: 'son' }]) });
  assert.match(p, /Hizmetçi — F \(okudu, 4\/5 puan\)/);
  assert.match(p, /son/);
  assert.ok(!p.includes('m0\n'));
});

test('parseEnv ve sameTitle', () => {
  assert.deepEqual(parseEnv('# yorum\nAPP_TOKEN="abc"\nPORT=1\n'), { APP_TOKEN: 'abc', PORT: '1' });
  assert.ok(sameTitle('Hizmetçi', 'HİZMETÇİ'));
  assert.ok(!sameTitle('Hizmetçi', 'Sır'));
});
