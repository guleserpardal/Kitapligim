'use strict';
const test=require('node:test'),assert=require('node:assert/strict'),R=require('../app/src/main/assets/reading.js');
const p='Güleser',day='2026-10-25',row=(date,pages,goal=20,profile=p)=>({date,profile,start:0,end:pages,goal,updatedAt:date+'T12:00:00Z'}),state=(rows,extra={})=>({dailyGoals:{[p]:20},books:[{id:'one',readingDays:rows}],...extra});
test('87 to 97 counts ten; same position and repeated saves do not add',()=>{
 let old={profile:p,currentPage:87},next={profile:p,currentPage:97};let rows=R.record(old,next,day,20);assert.equal(R.summary(state(rows),p,day).pages,10);
 rows=R.record({...next,readingDays:rows},next,day,20);assert.equal(rows.length,1);assert.equal(R.summary(state(rows),p,day).pages,10);
});
test('initial imports and profile transfer are baselines',()=>{assert.equal(R.record(null,{profile:p,currentPage:250},day,20).length,0);assert.equal(R.record({profile:p,currentPage:87},{profile:'Elis',currentPage:97},day,20).length,0)});
test('correction reduces today; crossing the baseline is counted once',()=>{
 let a={profile:p,currentPage:87},b={profile:p,currentPage:117},rows=R.record(a,b,day,20);
 rows=R.record({...b,readingDays:rows},{...b,currentPage:92},day,20);assert.equal(R.summary(state(rows),p,day).pages,5);
 rows=R.record({...b,currentPage:92,readingDays:rows},{...b,currentPage:80},day,20);assert.equal(R.summary(state(rows),p,day).pages,0);
});
test('two books add up, duplicate snapshots do not and deletion keeps history',()=>{
 const s=state([row(day,8)]);s.books.push({id:'two',deleted:true,readingDays:[row(day,12)]});s.books.push(s.books[0]);assert.equal(R.summary(s,p,day).pages,20);assert(R.summary(s,p,day).complete);
});
test('profiles have independent totals',()=>{const s=state([row(day,20),row(day,40,20,'Elis')]);assert.equal(R.summary(s,p,day).pages,20);assert.equal(R.summary(s,'Elis',day).pages,40)});
test('yesterday streak lives while today is pending',()=>{const s=state([row('2026-10-22',20),row('2026-10-23',25),row('2026-10-24',20),row(day,5)]);const d=R.summary(s,p,day);assert.equal(d.streak,3);assert.equal(d.best,3);assert.equal(d.remaining,15)});
test('completing today extends chain and missing yesterday breaks it',()=>{assert.equal(R.summary(state([row('2026-10-24',20),row(day,20)]),p,day).streak,2);assert.equal(R.summary(state([row('2026-10-23',20),row(day,20)]),p,day).streak,1);assert.equal(R.summary(state([row('2026-10-23',20)]),p,day).streak,0)});
test('calendar math survives leap day, year change and daylight savings',()=>{assert.equal(R.shift('2024-02-28',1),'2024-02-29');assert.equal(R.shift('2026-12-31',1),'2027-01-01');assert.equal(R.shift('2026-03-29',1),'2026-03-30');assert.equal(R.month('2026-01-31',-1),'2025-12')});
test('future and malformed records cannot create a chain',()=>{assert.equal(R.summary(state([row('2026-10-26',500),row('not-a-date',500)]),p,day).best,0)});
test('historical goals survive a change today',()=>{const s=state([row('2026-10-24',20),row(day,10)]);R.setGoal(s,p,10,day);const d=R.summary(s,p,day);assert.equal(d.history['2026-10-24'].goal,20);assert.equal(d.streak,2)});
test('no configured goal never fabricates a completed day',()=>{const d=R.summary({books:[]},p,day);assert.equal(d.target,0);assert.equal(d.streak,0);assert.equal(d.complete,false)});
test('invalid goals are rejected',()=>{for(const n of [0,-1,2.5,1001,NaN])assert.throws(()=>R.setGoal({books:[]},p,n,day))});
test('a shelf/status jump rebases instead of leaking into the next page update',()=>{const old={profile:p,currentPage:97,readingDays:R.record({profile:p,currentPage:87},{profile:p,currentPage:97},day,20)},next={profile:p,currentPage:208};next.readingDays=R.rebase(old,{...next,readingDays:old.readingDays},day);assert.equal(R.summary(state(next.readingDays),p,day).pages,10);const corrected=R.record(next,{profile:p,currentPage:200},day,20);assert.equal(R.summary(state(corrected),p,day).pages,2)});
