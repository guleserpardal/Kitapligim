'use strict';

// Only the public book-category search cards. HTML is read as text; scripts are never executed.
function decode(s) {
  return s.replace(/&#(x[0-9a-f]+|[0-9]+);|&(amp|lt|gt|quot|apos|nbsp);/gi, (all, num, named) => {
    if (named) return ({amp:'&',lt:'<',gt:'>',quot:'"',apos:"'",nbsp:' '})[named.toLowerCase()];
    const n = num[0].toLowerCase() === 'x' ? parseInt(num.slice(1),16) : Number(num);
    return n > 0 && n <= 0x10ffff ? String.fromCodePoint(n) : '';
  });
}
const text = s => decode(s.replace(/<!--[\s\S]*?-->/g, '').replace(/<[^>]*>/g, '')).replace(/\s+/g, ' ').trim();
function attr(tag, name) {
  const m = tag.match(new RegExp('(?:^|\\s)'+name+'\\s*=\\s*(["\'])(.*?)\\1', 'i'));
  return m ? decode(m[2]) : '';
}
function trendyol(html) {
  const books = [], seen = new Set();
  for (const match of String(html).matchAll(/<a\b([^>]*)>([\s\S]*?)<\/a\s*>/gi)) {
    if (attr(match[1], 'data-testid') !== 'product-card') continue;
    const name = match[2].match(/<span\b[^>]*class=["'][^"']*\bproduct-name\b[^"']*["'][^>]*>([\s\S]*?)<\/span\s*>/i);
    if (!name) continue;
    let url;
    try { url = new URL(attr(match[1], 'href'), 'https://www.trendyol.com/'); } catch { continue; }
    if (url.origin !== 'https://www.trendyol.com' || !/-p-[0-9]+$/.test(url.pathname)) continue;
    const title = text(name[1]);
    if (!title || seen.has(title.toLocaleLowerCase('tr'))) continue;
    seen.add(title.toLocaleLowerCase('tr'));
    const img = [...match[2].matchAll(/<img\b([^>]*)>/gi)].find(m => attr(m[1], 'data-testid') === 'image-img');
    const cover = img ? attr(img[1], 'src') : '';
    books.push({ title, author:'', isbn:'', totalPages:0, cover:cover.startsWith('https://')?cover:'', source:'Trendyol · kitap ilanı', sourceUrl:url.href, buyUrl:url.href, store:'Trendyol' });
    if (books.length >= 16) break;
  }
  return books;
}
module.exports = { trendyol };
