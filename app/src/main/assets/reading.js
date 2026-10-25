(function(root){
  'use strict';
  const profiles=['Güleser','İlker','Elis','Lila'];
  function today(){const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`}
  function shift(date,n){const d=new Date(date+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+n);return d.toISOString().slice(0,10)}
  function validDate(s){return /^\d{4}-\d{2}-\d{2}$/.test(s||'')&&!Number.isNaN(Date.parse(s+'T12:00:00Z'))&&new Date(s+'T12:00:00Z').toISOString().slice(0,10)===s}
  function validGoal(n){return Number.isInteger(n)&&n>=1&&n<=1000}
  function rows(input){return (Array.isArray(input)?input:[]).filter(x=>x&&validDate(x.date)&&profiles.includes(x.profile)&&Number.isInteger(x.start)&&Number.isInteger(x.end)&&x.start>=0&&x.end>=0&&x.start<=100000&&x.end<=100000&&(x.goal===0||validGoal(x.goal))&&!Number.isNaN(Date.parse(x.updatedAt)))}
  function days(state,profile){
    const unique=new Map(),out={};
    for(const [i,b] of (state.books||[]).entries())for(const row of rows(b.readingDays)){
      if(row.profile!==profile)continue;const key=(b.id||i)+'/'+row.date,old=unique.get(key);
      if(!old)unique.set(key,{...row});else{const newer=Date.parse(row.updatedAt)>=Date.parse(old.updatedAt)?row:old;unique.set(key,{...newer,goal:newer.goal||row.goal||old.goal})}
    }
    for(const x of unique.values()){
      const d=out[x.date]||(out[x.date]={pages:0,goal:0,updatedAt:''});d.pages+=Math.max(0,x.end-x.start);
      if(x.goal&&(!d.updatedAt||Date.parse(x.updatedAt)>=Date.parse(d.updatedAt))){d.goal=x.goal;d.updatedAt=x.updatedAt}
    }
    return out;
  }
  function goal(state,profile,history=days(state,profile),date=today()){const n=state.dailyGoals?.[profile];if(validGoal(n))return n;const keys=Object.keys(history).filter(k=>k<=date&&history[k].goal>0).sort();return keys.length?history[keys.at(-1)].goal:0}
  function summary(state,profile,date=today()){
    const history=days(state,profile),target=goal(state,profile,history,date),current={pages:history[date]?.pages||0,goal:target};history[date]=current;
    const completed=Object.keys(history).filter(k=>k<=date&&history[k].goal>0&&history[k].pages>=history[k].goal).sort(),set=new Set(completed);
    let run=0,best=0,last='';for(const key of completed){run=last&&shift(last,1)===key?run+1:1;best=Math.max(best,run);last=key}
    let streak=0,cursor=set.has(date)?date:shift(date,-1);while(set.has(cursor)){streak++;cursor=shift(cursor,-1)}
    return {date,target,pages:current.pages,complete:target>0&&current.pages>=target,remaining:Math.max(0,target-current.pages),streak,best,history};
  }
  function record(old,next,date=today(),target=0,updatedAt=new Date().toISOString()){
    const list=rows(old?.readingDays).map(x=>({...x}));if(!old||old.profile!==next.profile||Number(old.currentPage||0)===Number(next.currentPage||0))return list;
    const i=list.findIndex(x=>x.date===date&&x.profile===next.profile),prior=list[i];
    const row={date,profile:next.profile,start:prior?prior.start:Number(old.currentPage||0),end:Number(next.currentPage||0),goal:target||prior?.goal||0,updatedAt};
    if(i<0)list.push(row);else list[i]=row;return list.sort((a,b)=>(a.date+'/'+a.profile).localeCompare(b.date+'/'+b.profile));
  }
  function setGoal(state,profile,target,date=today()){
    if(!validGoal(target))throw Error('Günlük hedef 1–1000 sayfa olabilir.');state.dailyGoals||={};state.dailyGoals[profile]=target;
    for(const b of state.books||[]){let changed=false;b.readingDays=rows(b.readingDays).map(x=>{if(x.date===date&&x.profile===profile){changed=true;return {...x,goal:target,updatedAt:new Date().toISOString()}}return x});if(changed)b.syncStatus='pending'}
  }
  function rebase(old,next,date=today(),updatedAt=new Date().toISOString()){
    const list=rows(next.readingDays).map(x=>({...x}));if(!old||old.profile!==next.profile||Number(old.currentPage||0)===Number(next.currentPage||0))return list;
    return list.map(x=>{if(x.date!==date||x.profile!==next.profile||x.end===Number(next.currentPage||0))return x;const end=Number(next.currentPage||0),pages=Math.max(0,x.end-x.start);return {...x,start:Math.max(0,end-pages),end,updatedAt}});
  }
  function month(date,offset=0){const d=new Date(date.slice(0,7)+'-01T12:00:00Z');d.setUTCMonth(d.getUTCMonth()+offset);return d.toISOString().slice(0,7)}
  const api={today,shift,validDate,validGoal,days,goal,summary,record,setGoal,rebase,month};root.Reading=api;if(typeof module!=='undefined')module.exports=api;
})(typeof window==='undefined'?globalThis:window);
