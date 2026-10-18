'use strict';
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const http=require('node:http');
const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve(__dirname,'../app/src/main/assets');
const output=process.env.UI_OUTPUT_DIR||path.resolve(__dirname,'../test-results');
fs.mkdirSync(output,{recursive:true});
const books=[];
function book(id,title,status,profile='Güleser',extra={}){return {id,title,status,profile,author:'Örnek Yazar',currentPage:14,totalPages:208,rating:0,notes:'',highlights:[],cover:'',updatedAt:'2026-10-18T10:00:00Z',...extra}}
books.push(book('reading','Huşu Ağacı','reading','Güleser',{author:'Funda Uçuk Er',currentPage:87,cover:'https://covers.test/reading.svg'}));
for(let i=0;i<8;i++)books.push(book('want-'+i,i===0?'Şeker Portakalı':'Sıradaki kitap '+i,'want','Güleser',{currentPage:0,cover:i<2?'https://covers.test/want-'+i+'.svg':''}));
for(let i=0;i<3;i++)books.push(book('done-'+i,'Bitmiş kitap '+i,'done','Güleser',{currentPage:208,finishedAt:new Date().getFullYear()+'-10-01',rating:4}));
books.push(book('pause-1','Ara verilen roman','paused'),book('pause-2','Diğer yarım kitap','paused'));
books.push(book('ilker','İlker’in özel kitabı','reading','İlker'));
const state={version:1,books,profile:'Güleser',goals:{'Güleser':15},connected:false,assistant:{configured:false}};
const server=http.createServer((req,res)=>{
  const name=decodeURIComponent(new URL(req.url,'http://localhost').pathname),file=path.join(root,name==='/'?'index.html':name);
  if(!file.startsWith(root+path.sep)){res.writeHead(403).end();return}
  fs.readFile(file,(err,data)=>{if(err){res.writeHead(404).end();return}res.setHeader('Content-Type',({'.html':'text/html','.js':'text/javascript','.css':'text/css','.json':'application/json','.svg':'image/svg+xml'})[path.extname(file)]||'application/octet-stream');res.end(data)});
});
let browser;
async function openMenu(page){await page.locator('#quick-add-toggle').click();await page.locator('#quick-add-menu').waitFor()}
(async()=>{
  await new Promise(r=>server.listen(0,'127.0.0.1',r));const url='http://127.0.0.1:'+server.address().port;
  browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE||undefined});const context=await browser.newContext({viewport:{width:390,height:844}});
  await context.addInitScript(s=>localStorage.setItem('kitapligim-preview',JSON.stringify(s)),state);
  await context.route('https://covers.test/**',r=>r.fulfill({contentType:'image/svg+xml',body:`<svg xmlns="http://www.w3.org/2000/svg" width="260" height="390"><rect width="260" height="390" fill="${r.request().url().includes('reading')?'#325d58':'#ddc595'}"/><rect x="12" y="12" width="236" height="366" rx="3" fill="none" stroke="#d2be8a"/><path d="M40 310Q150 230 210 110M72 275l-12-45m62-6 40-10m-15-37-2-43" fill="none" stroke="#b4bd88" stroke-width="3"/><text x="130" y="62" text-anchor="middle" fill="#fff9e7" font-family="serif" font-size="29">${r.request().url().includes('reading')?'Huşu Ağacı':'Kitap köşesi'}</text><text x="130" y="90" text-anchor="middle" fill="#fff9e7" font-family="serif" font-size="16">Örnek kapak</text></svg>`}));
  const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));await page.goto(url);
  await page.locator('.home-library').waitFor();assert.equal(await page.locator('[data-home-shelf]').count(),4);
  assert.equal(await page.locator('.home-library .filters,.home-library .chip').count(),0);
  assert.equal(await page.locator('.reading-feature h3').innerText(),'Huşu Ağacı');
  assert.equal(await page.getByText('İlker’in özel kitabı',{exact:true}).count(),0);
  for(const width of [320,390,430,768]){
    await page.setViewportSize({width,height:844});
    assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Horizontal page overflow at '+width);
    const fab=await page.locator('#quick-add-toggle').boundingBox(),nav=await page.locator('.bottom-nav').boundingBox();assert(fab.y+fab.height<=nav.y,'Add button overlaps navigation');
  }
  await page.setViewportSize({width:390,height:844});await page.screenshot({path:path.join(output,'home-390.png'),fullPage:true});
  await page.locator('[data-home-shelf="want"] [data-action="view-shelf"]').click();await page.locator('.shelf-page').waitFor();
  assert.equal(await page.locator('.book-row').count(),8,'Full shelf must include books beyond home preview');
  await page.getByRole('textbox',{name:'Bu rafta ara'}).fill('kitap 7');await page.locator('#library-search').evaluate(f=>f.requestSubmit());
  assert.equal(await page.locator('.book-row').count(),1);await page.locator('.book-row').click();await page.locator('#notes-input').fill('Kaydedilen okuma notu');await page.getByRole('button',{name:'Değişiklikleri kaydet',exact:true}).click();
  await page.locator('[data-action="back"]').click();await page.locator('.shelf-page').waitFor();assert.equal(await page.getByRole('textbox',{name:'Bu rafta ara'}).inputValue(),'kitap 7');
  assert.equal(await page.evaluate(()=>JSON.parse(localStorage.getItem('kitapligim-preview')).books.find(b=>b.id==='want-7').notes),'Kaydedilen okuma notu');
  await page.getByRole('button',{name:'Kitaplığıma dön'}).click();await page.locator('[data-home-shelf="paused"] [data-action="view-shelf"]').click();assert.equal(await page.locator('.book-row').count(),2);
  await page.locator('.book-row').first().click();await page.locator('#page-input').fill('15');await page.getByRole('button',{name:'Değişiklikleri kaydet',exact:true}).click();
  assert.equal(await page.evaluate(()=>JSON.parse(localStorage.getItem('kitapligim-preview')).books.find(b=>b.id==='pause-1').status),'paused');
  await page.locator('[data-action="back"]').click();await page.getByRole('button',{name:'Kitaplığıma dön'}).click();
  await page.getByRole('button',{name:'İlker',exact:true}).click();assert.equal(await page.locator('.reading-feature h3').innerText(),'İlker’in özel kitabı');assert.equal(await page.getByText('Huşu Ağacı',{exact:true}).count(),0);
  await page.getByRole('button',{name:'Güleser',exact:true}).click();
  await openMenu(page);assert.equal(await page.locator('#quick-add-menu button').count(),5);await page.screenshot({path:path.join(output,'quick-add-390.png')});
  await page.keyboard.press('Escape');assert.equal(await page.locator('#quick-add-menu').count(),0);assert.equal(await page.locator('#quick-add-toggle').getAttribute('aria-expanded'),'false');
  await openMenu(page);assert(await page.evaluate(()=>window.onNativeBack()));assert.equal(await page.locator('#quick-add-menu').count(),0);
  await openMenu(page);await page.getByRole('button',{name:'Danışmana sor',exact:true}).click();await page.getByRole('button',{name:'PC sunucusunu bağla',exact:true}).waitFor();await page.getByRole('button',{name:'Kitaplığım',exact:true}).click();
  for(const mode of ['cover','isbn']){await openMenu(page);await page.locator('#quick-add-menu [data-mode="'+mode+'"]').click();await page.getByRole('dialog',{name:'Kitabının fotoğrafını ekle'}).waitFor();await page.getByRole('button',{name:'Kapat',exact:true}).click()}
  await openMenu(page);await page.getByRole('button',{name:'Kitap ara',exact:true}).click();await page.locator('#book-search input').waitFor();await page.getByRole('button',{name:'Kitaplığım',exact:true}).click();
  await openMenu(page);await page.getByRole('button',{name:'Elle ekle',exact:true}).click();await page.locator('#edit-form [name="title"]').fill('Yeni deneme kitabı');await page.locator('[data-action="save-edit"]').click();await page.locator('.home-library').waitFor();
  assert(await page.evaluate(()=>JSON.parse(localStorage.getItem('kitapligim-preview')).books.some(b=>b.title==='Yeni deneme kitabı'&&b.profile==='Güleser')));
  await page.setViewportSize({width:320,height:568});await openMenu(page);const menu=await page.locator('#quick-add-menu').boundingBox();assert(menu.y>=0&&menu.x>=0&&menu.x+menu.width<=320);await page.screenshot({path:path.join(output,'small-screen-menu.png')});await page.keyboard.press('Escape');
  // Empty libraries and untrusted book titles must stay safe and usable.
  await page.evaluate(()=>window.receiveState({books:[],profile:'Lila',goals:{},connected:false,assistant:{configured:false}}));assert.equal(await page.locator('[data-home-shelf]').count(),4);assert.equal(await page.locator('.library-book,.reading-feature').count(),0);await page.getByRole('button',{name:'Yıllık okuma hedefi belirle'}).waitFor();
  await page.evaluate(()=>window.receiveState({books:[{id:'unsafe',title:'<img src=x onerror=alert(1)>',author:'<script>bad</script>',profile:'Lila',status:'paused'}],profile:'Lila',goals:{}}));assert.equal(await page.locator('img[src="x"],script:not([src])').count(),0);assert.equal(await page.locator('[data-home-shelf="paused"] .library-book-title').innerText(),'<img src=x onerror=alert(1)>');
  assert.deepEqual(errors,[]);console.log('Home UI checks passed: four shelves, complete lists, search, profile isolation, saved notes, paused progress, five add actions, back/Escape, small screens and empty/unsafe data.');
})().catch(e=>{console.error(e);process.exitCode=1}).finally(async()=>{if(browser)await browser.close();await new Promise(r=>server.close(r))});
