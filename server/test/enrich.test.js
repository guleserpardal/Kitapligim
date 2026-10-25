'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {createEnricher,sameTitle} = require('../src/enrich');
const {trendyol} = require('../src/trendyol');
const catalog = require('../data/catalog-tr.json');
const fixture = fs.readFileSync(path.join(__dirname,'fixtures/trendyol-books.html'),'utf8');
const down = async () => { throw new Error('unavailable'); };
const book = {title:'Aranan Kitap',author:'Örnek Yazar',why:'Bir öneri.'};

test('public shop cards decode text, deduplicate sellers and reject foreign URLs',()=>{
  const list=trendyol(fixture);assert.equal(list.length,3);assert.equal(list[0].title,'Aranan Kitap / Örnek Yazar');
  assert.equal(list[0].author,'');assert.equal(list[0].isbn,'');assert.equal(list[0].totalPages,0);
  assert.equal(list[0].buyUrl,'https://www.trendyol.com/yayinevi/aranan-kitap-p-100?merchantId=1&x=2');
});
test('publisher catalogue verifies Turkish/ASCII names offline with no requests',async()=>{
  const e=createEnricher({getJson:()=>assert.fail('network'),getText:()=>assert.fail('network')});
  const b=await e.enrichOne({title:'HIZMETCI',author:'Freida McFadden'});assert.equal(b.verified,true);assert.equal(b.isbn,'9786256411302');
  const relative=await e.enrichOne({title:'Kilitli Kapı',author:'Freida McFadden'});assert.match(relative.cover,/^https:\/\/www\.olimposdukkan\.com\//);
});
test('server snapshot matches the Android catalogue when run from the repository',()=>{
  const filename=path.join(__dirname,'../../app/src/main/assets/catalog-tr.json');
  if(fs.existsSync(filename))assert.deepEqual(catalog,JSON.parse(fs.readFileSync(filename,'utf8')));
});
test('same title is exact; a sequel and different author never verify the original',async()=>{
  assert.equal(sameTitle('Hizmetçi','HİZMETÇİ'),true);assert.equal(sameTitle('Hizmetçi','Hizmetçi İzliyor'),false);
  const e=createEnricher({getJson:down,getText:down});
  assert.equal((await e.enrichOne({title:'Hizmetçi',author:'Başka Yazar'})).verified,false);
});
test('Google uses plain query and retries an empty language filter',async()=>{
  const urls=[],e=createEnricher({catalog:[],getText:down,getJson:async url=>{
    urls.push(url);assert.equal(new URL(url).searchParams.get('q'),'Aranan Kitap Örnek Yazar');
    return url.includes('langRestrict')?{items:[]}:{items:[{volumeInfo:{title:book.title,authors:[book.author],language:'tr',pageCount:208,industryIdentifiers:[{type:'ISBN_13',identifier:'9786256411302'}]}}]};
  }});
  const b=await e.enrichOne(book);assert.equal(b.verified,true);assert.equal(b.totalPages,208);assert.equal(urls.length,2);
});
test('unfiltered non-Turkish Google hits do not copy a foreign edition ISBN',async()=>{
  const e=createEnricher({catalog:[],getText:down,getJson:async url=>url.includes('langRestrict')?{items:[]}:{items:[{volumeInfo:{title:book.title,authors:[book.author],language:'en',pageCount:999,industryIdentifiers:[{type:'ISBN_13',identifier:'wrong'}]}}]}});
  const b=await e.enrichOne(book);assert.equal(b.verified,true);assert.equal(b.isbn,'');assert.equal(b.totalPages,0);
});
test('429 is not retried and Trendyol can verify an explicitly displayed title and author',async()=>{
  let requests=0;
  const e=createEnricher({catalog:[],getJson:async()=>{requests++;throw new Error('429');},getText:async url=>{
    assert.equal(new URL(url).searchParams.get('wc'),'91');return fixture;
  }});
  const b=await e.enrichOne(book);assert.equal(b.verified,true);assert.equal(b.source,'Trendyol · kitap ilanı');assert.equal(b.isbn,'');assert.equal(requests,1);
});
test('a publisher brand is not treated as an author; a sequel is not a title match',async()=>{
  const e=createEnricher({catalog:[],getJson:down,getText:async()=>fixture});
  for(const b of [{title:'Aranan Kitap',author:'Örnek Yayınları'},{title:'Aranan',author:'Örnek Yazar'}])assert.equal((await e.enrichOne(b)).verified,false);
});
test('Open Library uses a matching Turkish edition rather than the work ISBN',async()=>{
  const e=createEnricher({catalog:[],getText:down,getJson:async url=>{
    if(!url.includes('openlibrary'))throw new Error('429');
    return {docs:[{title:'Original title',author_name:[book.author],isbn:['wrong'],editions:{docs:[{title:book.title,language:['tur'],isbn:['correct'],number_of_pages:208}]}}]};
  }});
  const b=await e.enrichOne(book);assert.equal(b.isbn,'correct');assert.equal(b.totalPages,208);assert.equal(b.verified,true);
});
test('work-only matches keep unknown edition details empty',async()=>{
  const e=createEnricher({catalog:[],getText:down,getJson:async url=>{
    if(!url.includes('openlibrary'))throw new Error('429');return {docs:[{title:book.title,author_name:[book.author],isbn:['wrong'],number_of_pages_median:999}]};
  }});
  const b=await e.enrichOne(book);assert.equal(b.verified,true);assert.equal(b.isbn,'');assert.equal(b.totalPages,0);
});
test('an outage preserves the suggestion without inventing metadata',async()=>{
  const b=await createEnricher({catalog:[],getJson:down,getText:down}).enrichOne(book);
  assert.equal(b.verified,false);assert.equal(b.title,book.title);assert.equal(b.why,book.why);assert.equal(b.isbn,'');
});
