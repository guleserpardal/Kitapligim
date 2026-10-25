'use strict';
const {chromium}=require('playwright'),assert=require('node:assert/strict'),http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../app/src/main/assets'),out=process.env.UI_OUTPUT_DIR||path.resolve(__dirname,'../test-results');fs.mkdirSync(out,{recursive:true});
const date='2026-10-25',profile='Güleser',record=(date,pages)=>({date,profile,start:0,end:pages,goal:20,updatedAt:date+'T10:00:00Z'});
const state={profile,goals:{[profile]:15},dailyGoals:{[profile]:20},books:[
 {id:'first',title:'Huşu Ağacı',author:'Funda Uçuk Er',status:'reading',profile,currentPage:87,totalPages:208,notes:'',rating:0,updatedAt:'2026-10-24T12:00:00Z',readingDays:[record('2026-10-22',8),record('2026-10-23',20),record('2026-10-24',25)]},
 {id:'second',title:'İkinci kitabım',author:'Örnek yazar',status:'reading',profile,currentPage:10,totalPages:300,notes:'',rating:0,updatedAt:'2026-10-23T12:00:00Z'}],connected:false,assistant:{configured:false}};
const server=http.createServer((req,res)=>{const n=decodeURIComponent(new URL(req.url,'http://localhost').pathname),p=path.join(root,n==='/'?'index.html':n);if(!p.startsWith(root+path.sep)){res.writeHead(403).end();return}fs.readFile(p,(e,b)=>{if(e){res.writeHead(404).end();return}res.setHeader('Content-Type',({'.html':'text/html','.js':'text/javascript','.css':'text/css','.json':'application/json','.svg':'image/svg+xml'})[path.extname(p)]||'application/octet-stream');res.end(b)})});
let browser;
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE||undefined});
 const context=await browser.newContext({viewport:{width:390,height:844},timezoneId:'Europe/Istanbul'});
 await context.addInitScript(s=>{if(!localStorage.getItem('kitapligim-preview'))localStorage.setItem('kitapligim-preview',JSON.stringify(s))},state);
 const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));await page.clock.install({time:new Date('2026-10-25T10:00:00+03:00')});await page.goto('http://127.0.0.1:'+server.address().port);
 await page.locator('.daily-home').waitFor();assert.equal(await page.locator('.daily-home-score strong').innerText(),'0');assert((await page.locator('.chain-mini').innerText()).includes('2 günlük'));
 async function update(id,pages){await page.evaluate(id=>openDetail(id),id);await page.locator('#page-input').fill(String(pages));await page.getByRole('button',{name:'Değişiklikleri kaydet',exact:true}).click();await page.locator('[data-action="back"]').click()}
 await update('first',97);assert.equal(await page.locator('.daily-home-score strong').innerText(),'10');
 await page.evaluate(()=>openDetail('first'));await page.locator('#notes-input').fill('Sadece not değişti.');await page.getByRole('button',{name:'Değişiklikleri kaydet',exact:true}).click();await page.locator('[data-action="back"]').click();assert.equal(await page.locator('.daily-home-score strong').innerText(),'10');
 await update('first',92);assert.equal(await page.locator('.daily-home-score strong').innerText(),'5');
 await update('second',25);assert.equal(await page.locator('.daily-home-score strong').innerText(),'20');assert((await page.locator('.chain-mini').innerText()).includes('3 günlük'));
 await page.reload();await page.locator('.daily-home.achieved').waitFor();assert.equal(await page.locator('.daily-home-score strong').innerText(),'20');await page.screenshot({path:path.join(out,'daily-home.png')});
 await page.getByRole('button',{name:'Günlük okuma hedefi ve takvim',exact:true}).click();await page.locator('.daily-panel').waitFor();assert.equal(await page.locator('.streak-badge strong').innerText(),'3');
 assert((await page.locator('[data-date="2026-10-25"]').getAttribute('class')).includes('met'));assert((await page.locator('[data-date="2026-10-22"]').getAttribute('class')).includes('partial'));assert.equal(await page.locator('[data-date="2026-10-26"]').getAttribute('disabled'),'');
 await page.locator('[data-date="2026-10-23"]').click();assert((await page.locator('.reading-day-detail').innerText()).includes('20 sayfa / 20 sayfa'));
 await page.getByRole('button',{name:'Önceki ay',exact:true}).click();assert((await page.locator('.calendar-heading h3').innerText()).includes('Eylül'));await page.getByRole('button',{name:'Sonraki ay',exact:true}).click();assert.equal(await page.getByRole('button',{name:'Sonraki ay',exact:true}).getAttribute('disabled'),'');
 await page.locator('[data-action="daily-goal"]').click();await page.locator('#daily-goal-form [name="dailyGoal"]').fill('30');await page.getByRole('button',{name:'Hedefimi kaydet',exact:true}).click();assert.equal(await page.locator('.streak-badge strong').innerText(),'2');
 const past=await page.evaluate(()=>Reading.summary(S,S.profile,today()).history['2026-10-24']);assert.equal(past.goal,20);
 await page.locator('[data-action="daily-goal"]').click();await page.locator('[data-action="daily-preset"][data-pages="20"]').click();await page.getByRole('button',{name:'Hedefimi kaydet',exact:true}).click();
 for(const width of [320,390,430]){await page.setViewportSize({width,height:844});assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Daily calendar overflow at '+width);assert.equal(await page.locator('.calendar-weekdays span').count(),7)}
 await page.setViewportSize({width:390,height:844});await page.clock.runFor(4500);await page.screenshot({path:path.join(out,'daily-calendar.png')});
 await page.getByRole('button',{name:'Lila',exact:true}).click();assert.equal(await page.locator('.streak-badge strong').innerText(),'0');assert.equal(await page.locator('.daily-large-number>strong').innerText(),'0');await page.locator('[data-action="daily-goal"]').click();await page.locator('[data-pages="10"]').click();await page.getByRole('button',{name:'Hedefimi kaydet',exact:true}).click();assert((await page.locator('.daily-goal-edit').innerText()).includes('10 sayfa'));
 await page.getByRole('button',{name:'Güleser',exact:true}).click();assert.equal(await page.locator('.daily-large-number>strong').innerText(),'20');
 await page.clock.setSystemTime(new Date('2026-10-26T10:00:00+03:00'));await page.evaluate(()=>refreshReadingDay());assert.equal(await page.locator('.daily-large-number>strong').innerText(),'0');assert.equal(await page.locator('.streak-badge strong').innerText(),'3');
 assert.deepEqual(errors,[]);console.log('Daily UI checks passed: page deltas, correction, no double counting, multi-book total, persistence, calendar, goal changes, profile isolation, midnight and narrow screens.');
})().catch(e=>{console.error(e);process.exitCode=1}).finally(async()=>{if(browser)await browser.close();await new Promise(r=>server.close(r))});
