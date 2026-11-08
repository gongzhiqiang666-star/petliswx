import test from 'node:test';
import assert from 'node:assert/strict';
import { render } from '../src/pages/dashboard.js';
for (const staleFailure of [false,true]) {
 test(`注册审核按提交日期展示，旧${staleFailure?'失败':'成功'}响应不能覆盖新日期`,async()=>{
  const elements=new Map(),requests=[];
  const element=key=>{if(!elements.has(key))elements.set(key,{innerHTML:'',value:'7',handlers:{},addEventListener(name,fn){this.handlers[name]=fn}});return elements.get(key)};
  const container={innerHTML:'',querySelector:element};
  const api={request:(path,options)=>new Promise((resolve,reject)=>requests.push({path,options,resolve,reject}))};
  const initial=render(container,{api,toast(){}});
  assert.match(container.innerHTML,/当前审核状态，非历史审核操作次数/);
  const first=requests.find(r=>r.path==='/admin/dashboard/registrations');
  const original=globalThis.FormData;
  globalThis.FormData=class {entries(){return [['from','2026-11-01'],['to','2026-11-07']][Symbol.iterator]()}};
  try {element('#dashboard-filter').handlers.submit({preventDefault(){},currentTarget:{}})} finally {globalThis.FormData=original}
  const latest=requests.filter(r=>r.path==='/admin/dashboard/registrations').at(-1);
  assert.deepEqual(latest.options.query,{from:'2026-11-01',to:'2026-11-08'});
  latest.resolve({submittedCount:17,pendingCount:3,approvedCount:12,rejectedCount:2,totalPending:24});
  await Promise.resolve();
  assert.match(element('#registration-summary').innerHTML,/>17</);
  assert.match(element('#registration-summary').innerHTML,/>24</);
  if(staleFailure)first.reject(new Error('过期错误'));
  else first.resolve({submittedCount:999,totalPending:999});
  for(const request of requests){if(request!==first && request!==latest) request.resolve(request.path.includes('client-events')?[]:{})}
  await initial;
  assert.doesNotMatch(element('#registration-summary').innerHTML,/999|过期错误/);
 });
}
