'use strict';
const catalog = require('../data/catalog-tr.json');
const { trendyol } = require('./trendyol');
const norm = s => String(s || '').toLocaleLowerCase('tr').replace(/ı/g, 'i').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, ' ').trim();

// A series title must not verify a different sequel (Hizmetçi != Hizmetçi İzliyor).
const sameTitle = (a,b) => !!norm(a) && norm(a) === norm(b);
const sameAuthor = (a,b) => !norm(a) || (!!norm(b) && norm(a) === norm(b));
const matches = (hit, book) => sameTitle(hit.title,book.title) && sameAuthor(book.author,hit.author);

async function getText(url) {
  const r = await fetch(url, { signal:AbortSignal.timeout(6000), headers:{'User-Agent':'Kitapligim-sunucu/1.1 (personal family reading tracker)'} });
  if (!r.ok) throw new Error(String(r.status));
  const reader = r.body.getReader(), chunks = []; let size = 0;
  try {
    while (true) {
      const {value,done} = await reader.read(); if (done) break;
      size += value.byteLength;
      if (size > 3_000_000) throw new Error('Yanıt çok büyük.');
      chunks.push(Buffer.from(value));
    }
    return Buffer.concat(chunks).toString('utf8');
  } finally { await reader.cancel().catch(()=>{}); }
}
const getJson = async url => JSON.parse(await getText(url));

function createEnricher(deps = {}) {
  const json = deps.getJson || getJson, html = deps.getText || getText, local = deps.catalog || catalog;
  async function googleBooks(book) {
    // Plain text also works when field operators return zero results. Don't repeat 429/quota errors.
    const q = `${book.title} ${book.author || ''}`.trim();
    const base = `https://www.googleapis.com/books/v1/volumes?q=${encodeURIComponent(q)}&maxResults=16&printType=books`;
    for (const suffix of ['&langRestrict=tr','']) {
      const j = await json(base+suffix);
      for (const item of j.items || []) {
        const v = item.volumeInfo || {}, hit = {title:v.title,author:(v.authors||[]).join(', ')};
        if (!matches(hit,book)) continue;
        // An unfiltered match proves the work exists, but not that its ISBN/pages belong to the Turkish edition.
        if (!suffix && v.language !== 'tr' && v.language !== 'tur') return {...hit,isbn:'',totalPages:0,cover:'',source:'Google Books · eser kaydı'};
        const isbn = (v.industryIdentifiers||[]).find(i=>i.type==='ISBN_13')?.identifier || '';
        return {...hit,isbn,totalPages:v.pageCount||0,cover:(v.imageLinks?.thumbnail||'').replace(/^http:/,'https:'),source:'Google Books'};
      }
    }
    return null;
  }
  async function openLibrary(book) {
    const fields = 'title,author_name,editions,editions.title,editions.language,editions.isbn,editions.cover_i,editions.number_of_pages';
    const j = await json(`https://openlibrary.org/search.json?title=${encodeURIComponent(book.title)}&author=${encodeURIComponent(book.author||'')}&lang=tr&limit=5&fields=${fields}`);
    for (const work of j.docs || []) {
      const author = (work.author_name||[]).join(', ');
      for (const e of work.editions?.docs || []) {
        if (!(e.language||[]).some(l=>l==='tur'||l==='tr') || !matches({title:e.title,author},book)) continue;
        return {title:e.title,author,isbn:e.isbn?.[0]||'',totalPages:e.number_of_pages||0,cover:e.cover_i?`https://covers.openlibrary.org/b/id/${e.cover_i}-M.jpg`:'',source:'Open Library'};
      }
      // A work can contain ISBNs from many languages. Never attach one arbitrarily to a Turkish title.
      if (matches({title:work.title,author},book)) return {title:work.title,author,isbn:'',totalPages:0,cover:'',source:'Open Library · eser kaydı'};
    }
    return null;
  }
  async function shop(book) {
    const query = `${book.title} ${book.author||''}`.trim();
    const cards = trendyol(await html('https://www.trendyol.com/sr?wc=91&q='+encodeURIComponent(query)));
    for (const card of cards) {
      // Sellers often include the author in the display title; the publisher/brand is never the author.
      const expected = norm(`${book.title} ${book.author||''}`);
      if (norm(card.title) === expected || (!book.author && sameTitle(card.title,book.title))) return {...card,title:book.title,author:book.author||''};
    }
    return null;
  }
  async function enrichOne(book) {
    let hit = local.find(b=>matches(b,book));
    if (!hit) for (const lookup of [googleBooks,shop,openLibrary]) {
      try { hit = await lookup(book); if (hit) break; } catch {}
    }
    if (!hit) return {...book,isbn:'',totalPages:0,cover:'',source:'Danışman',verified:false};
    let cover = hit.cover || '';
    if (cover.startsWith('/') && hit.sourceUrl) { try {cover = new URL(cover,hit.sourceUrl).href;} catch {cover='';} }
    return {...book,author:book.author||hit.author,isbn:hit.isbn||'',totalPages:hit.totalPages||0,cover,source:hit.source,verified:true,
      ...(hit.sourceUrl?{sourceUrl:hit.sourceUrl}:{}),...(hit.buyUrl?{buyUrl:hit.buyUrl,store:hit.store||''}:{})};
  }
  return {enrichOne,enrich:books=>Promise.all(books.map(enrichOne))};
}
const { enrich, enrichOne } = createEnricher();
module.exports = { enrich, enrichOne, createEnricher, norm, sameTitle };
