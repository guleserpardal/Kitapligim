'use strict';
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const http=require('node:http');
const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve(__dirname,'../app/src/main/assets');
const server=http.createServer((req,res)=>{
  const filename=path.resolve(root,'.'+decodeURIComponent(req.url==='/'?'/index.html':req.url));
  if(!filename.startsWith(root+path.sep)){res.writeHead(403).end();return;}
  fs.readFile(filename,(err,data)=>{if(err){res.writeHead(404).end();return;}res.setHeader('Content-Type',({'.html':'text/html','.js':'application/javascript','.css':'text/css','.json':'application/json','.svg':'image/svg+xml'})[path.extname(filename)]||'application/octet-stream');res.end(data);});
});
let browser;
(async()=>{
  await new Promise(r=>server.listen(0,'127.0.0.1',r));
  browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE||undefined});
  const context=await browser.newContext({viewport:{width:390,height:844}});
  await context.addInitScript(()=>{
    const state={books:[],profile:'Güleser',goals:{},connected:false,assistant:{configured:true}};
    window.testSearch={opened:[],pending:[]};
    window.Android={request(id,action,raw){
      const p=JSON.parse(raw);let value={},error=null;
      if(action==='state')value=state;
      if(action==='search'){
        if(p.q==='ilk'){window.testSearch.pending.push(id);return;}
        if(p.q==='kesinti')error='Kataloglara şu an ulaşılamıyor.';
        value=p.q==='son'? [{title:'Son Kitap',author:'Örnek Yazar',source:'Trendyol · kitap ilanı'}]:[];
      }
      if(action==='openLink')window.testSearch.opened.push(p.url);
      if(action==='offers'||action==='shopPrices')value=[];
      setTimeout(()=>window.nativeResult(id,value,error),0);
    }};
    localStorage.setItem('kitapligim-chat-Güleser',JSON.stringify([{role:'assistant',content:'Öneri',books:[{title:'Aranan Kitap',author:'Örnek Yazar',verified:false}]}]));
  });
  const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.locator('.home-library').waitFor();
  await page.getByRole('button',{name:'Ara',exact:true}).click();
  async function search(q){await page.locator('#book-search input').fill(q);await page.locator('#book-search').evaluate(f=>f.requestSubmit());}
  await search('Aranan & Kitap');await page.getByText('Kataloglarda eşleşme bulunamadı',{exact:true}).waitFor();
  await page.getByRole('button',{name:'Trendyol’da ara',exact:true}).click();
  const target=new URL(await page.evaluate(()=>window.testSearch.opened.at(-1)));
  assert.equal(target.origin,'https://www.trendyol.com');assert.equal(target.searchParams.get('q'),'Aranan & Kitap');assert.equal(target.searchParams.get('wc'),'91');
  await page.locator('[data-action="manual-search"]').last().click();
  assert.equal(await page.locator('#edit-form [name=title]').inputValue(),'Aranan & Kitap');
  await page.getByRole('button',{name:'Kapat',exact:true}).click();await page.getByRole('button',{name:'Vazgeç',exact:true}).click();
  await search('ISBN: 978-625-6411-30-2');await page.getByText('Kataloglarda eşleşme bulunamadı',{exact:true}).waitFor();
  await page.locator('[data-action="manual-search"]').last().click();assert.equal(await page.locator('#edit-form [name=isbn]').inputValue(),'9786256411302');assert.equal(await page.locator('#edit-form [name=title]').inputValue(),'');
  await page.getByRole('button',{name:'Kapat',exact:true}).click();await page.getByRole('button',{name:'Vazgeç',exact:true}).click();
  await search('kesinti');await page.getByText('Kataloglara şu an ulaşılamıyor.',{exact:true}).waitFor();assert.equal(await page.getByRole('button',{name:'Trendyol’da ara',exact:true}).count(),1);
  await search('ilk');await search('son');await page.getByRole('heading',{name:'Son Kitap',exact:true}).waitFor();
  await page.evaluate(()=>window.nativeResult(window.testSearch.pending[0],[{title:'Eski Sonuç',source:'Katalog'}],null));
  assert.equal(await page.getByRole('heading',{name:'Son Kitap',exact:true}).count(),1);assert.equal(await page.getByRole('heading',{name:'Eski Sonuç',exact:true}).count(),0);
  await page.getByRole('tab',{name:'Danışman',exact:true}).click();await page.getByText('Bu kaynaklarda doğrulanamadı',{exact:true}).waitFor();await page.getByRole('button',{name:'Trendyol’da ara',exact:true}).click();
  assert.equal(new URL(await page.evaluate(()=>window.testSearch.opened.at(-1))).searchParams.get('q'),'Aranan Kitap Örnek Yazar');
  for(const width of [320,390,430]){await page.setViewportSize({width,height:844});assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));}
  assert.deepEqual(errors,[]);console.log('Search UI checks passed: empty/outage fallback, encoded shop links, manual title/ISBN, stale response isolation, advisor fallback and narrow widths.');
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(async()=>{if(browser)await browser.close();await new Promise(r=>server.close(r));});
