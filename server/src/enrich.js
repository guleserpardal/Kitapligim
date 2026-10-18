'use strict';

const norm = (s) => String(s || '').toLocaleLowerCase('tr').replace(/ı/g, 'i').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, ' ').trim();

function sameTitle(a, b) {
  const x = norm(a), y = norm(b);
  return !!x && !!y && (x === y || x.includes(y) || y.includes(x));
}

async function getJson(url) {
  const r = await fetch(url, { signal: AbortSignal.timeout(6000), headers: { 'User-Agent': 'Kitapligim-sunucu/1.0' } });
  if (!r.ok) throw new Error(String(r.status));
  return r.json();
}

async function googleBooks(book) {
  const q = `intitle:${book.title}` + (book.author ? `+inauthor:${book.author}` : '');
  const j = await getJson(`https://www.googleapis.com/books/v1/volumes?q=${encodeURIComponent(q).replace(/%2B/g, '+')}&langRestrict=tr&maxResults=5&printType=books`);
  for (const item of j.items || []) {
    const v = item.volumeInfo || {};
    if (!sameTitle(v.title, book.title)) continue;
    const isbn = (v.industryIdentifiers || []).find((i) => i.type === 'ISBN_13')?.identifier || '';
    const cover = (v.imageLinks?.thumbnail || '').replace(/^http:/, 'https:');
    return { title: v.title, author: (v.authors || []).join(', '), isbn, totalPages: v.pageCount || 0, cover, source: 'Google Books' };
  }
  return null;
}

async function openLibrary(book) {
  const j = await getJson(`https://openlibrary.org/search.json?title=${encodeURIComponent(book.title)}&author=${encodeURIComponent(book.author || '')}&limit=5&fields=title,author_name,cover_i,isbn,number_of_pages_median`);
  for (const d of j.docs || []) {
    if (!sameTitle(d.title, book.title)) continue;
    return { title: book.title, author: (d.author_name || []).join(', '), isbn: d.isbn?.[0] || '', totalPages: d.number_of_pages_median || 0, cover: d.cover_i ? `https://covers.openlibrary.org/b/id/${d.cover_i}-M.jpg` : '', source: 'Open Library' };
  }
  return null;
}

/** Best effort: a book that no catalogue knows is marked unverified so the app can warn the user. */
async function enrichOne(book) {
  for (const lookup of [googleBooks, openLibrary]) {
    try {
      const hit = await lookup(book);
      if (hit) return { ...book, author: book.author || hit.author, isbn: hit.isbn, totalPages: hit.totalPages, cover: hit.cover, source: hit.source, verified: true };
    } catch {}
  }
  return { ...book, isbn: '', totalPages: 0, cover: '', source: 'Danışman', verified: false };
}

const enrich = (books) => Promise.all(books.map(enrichOne));

module.exports = { enrich, enrichOne, norm, sameTitle };
