import fs from 'node:fs';import assert from 'node:assert/strict';import {fileURLToPath} from 'node:url';import {createStaticServer} from '../../admin/server.js';import {chromium} from 'playwright-core';
const f=JSON.parse(fs.readFileSync(0,'utf8')),server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;const errors=[];
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(({token})=>{if(window!==window.top)return;localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'网银核价验收',roles:['ADMIN']}));},{token:f.token});await context.route('**/*',r=>new URL(r.request().url()).origin===origin?r.continue():r.abort());const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto(origin+'/#/legacy-bank-payments');await page.locator('[name=orderNo]').fill(f.prefix);await page.getByRole('button',{name:'核对待付项目'}).click();await page.waitForFunction(()=>document.querySelector('[data-bank-result]')?.textContent.includes('应付 12.50 元'));assert.equal(await page.locator('[data-bank-result] tbody tr').count(),1);
 let preparationProof={};
 if(f.prepare){
  const writes=[];let dropped=false,firstOrder;
  await context.route('**/api/admin/legacy/bank-payments/requests',async route=>{
   if(route.request().method()!=='POST')return route.continue();writes.push(route.request().postDataJSON());
   if(!dropped){dropped=true;const response=await route.fetch();assert.equal(response.status(),200);const body=await response.json();assert.equal(body.code,200);firstOrder=body.data.orderId;return route.abort('failed');}
   return route.continue();
  });
  await page.getByRole('button',{name:'保存支付单',exact:true}).click();await page.getByRole('button',{name:'确认保存支付单',exact:true}).click();await page.locator('#dialog-error').filter({hasText:/网络|fetch|请求|失败/i}).waitFor();
  await page.getByRole('button',{name:'关闭',exact:true}).click();await page.getByRole('button',{name:'保存支付单',exact:true}).click();await page.getByRole('button',{name:'确认保存支付单',exact:true}).click();await page.getByRole('dialog').waitFor({state:'hidden'});
  assert.equal(writes.length,2);assert.deepEqual(writes[0],writes[1]);assert.equal(writes[1].hospital,Number(f.hospital));
  await page.locator('[data-bank-requests]').getByText(firstOrder,{exact:true}).waitFor();assert.match(await page.locator('[data-bank-requests]').innerText(),/已保存，尚未发送/);
  await page.reload();await page.locator('[data-bank-requests]').getByText(firstOrder,{exact:true}).waitFor();
  if(f.dispatch){
   await page.locator('[data-bank-requests]').getByRole('button',{name:'发送银行',exact:true}).first().click();await page.getByRole('button',{name:'确认发送银行',exact:true}).click();await page.getByRole('dialog').waitFor({state:'hidden'});await page.waitForFunction(()=>document.querySelector('[data-bank-requests]')?.textContent.includes('结果待核对'));
   assert.equal(await page.locator('[data-bank-requests]').getByRole('button',{name:'发送银行',exact:true}).count(),0);
   const duplicate=await page.evaluate(async key=>{const response=await fetch('/api/admin/legacy/bank-payments/requests/'+encodeURIComponent(key)+'/send',{method:'POST',headers:{Authorization:'Bearer '+localStorage.getItem('petlis.admin.token')}});return (await response.json()).code;},writes[0].requestKey);assert.equal(duplicate,409);
   if(f.dispatchRollback){const failed=page.waitForResponse(response=>response.request().method()==='POST'&&response.url().endsWith('/query'));await page.locator('[data-bank-requests]').getByRole('button',{name:'查询原交易',exact:true}).first().click();assert.equal((await (await failed).json()).code,409);await page.getByRole('button',{name:'刷新记录',exact:true}).click();await page.waitForFunction(()=>document.querySelector('[data-bank-requests]')?.textContent.includes('结果待核对'));}
   if(f.notify){
    const uri=new URL(f.noticeUrl);assert.equal(uri.hostname,'127.0.0.1');const packet=await (await fetch(uri)).text();
    const proof=await page.evaluate(async({packet,key})=>{
     const path='/api/payment/legacy-bank/notify',send=async body=>{const r=await fetch(path,{method:'POST',headers:{'Content-Type':'text/json;charset=utf-8'},body});return {status:r.status,body:await r.text()};};
     const tampered=await send(packet.replace('1250','1251')),oversized=await send('x'.repeat(1024*1024+1));
     const [notification,query]=await Promise.all([send(packet),fetch('/api/admin/legacy/bank-payments/requests/'+encodeURIComponent(key)+'/query',{method:'POST',headers:{Authorization:'Bearer '+localStorage.getItem('petlis.admin.token')}}).then(r=>r.json())]);
     const duplicate=await send(packet);return {tampered,oversized,notification,duplicate,query};
    },{packet,key:writes[0].requestKey});
    assert.equal(proof.tampered.status,400);assert.equal(proof.oversized.status,413);assert.deepEqual(proof.notification,{status:200,body:'success'});assert.deepEqual(proof.duplicate,{status:200,body:'success'});assert.equal(proof.query.code,200);await page.getByRole('button',{name:'刷新记录',exact:true}).click();
   }else await page.locator('[data-bank-requests]').getByRole('button',{name:'查询原交易',exact:true}).first().click();await page.waitForFunction(()=>document.querySelector('[data-bank-requests]')?.textContent.includes('已核对付款成功'));
   const repeated=await page.evaluate(async key=>{const response=await fetch('/api/admin/legacy/bank-payments/requests/'+encodeURIComponent(key)+'/query',{method:'POST',headers:{Authorization:'Bearer '+localStorage.getItem('petlis.admin.token')}});return (await response.json()).data.status;},writes[0].requestKey);assert.equal(repeated,'SUCCEEDED');
   await page.reload();await page.waitForFunction(()=>document.querySelector('[data-bank-requests]')?.textContent.includes('已核对付款成功'));
  }
  if(f.refund){
   const row=page.locator('[data-bank-requests] tbody tr').filter({hasText:firstOrder});await row.getByRole('button',{name:'申请退款',exact:true}).click();const dialog=page.getByRole('dialog');await dialog.locator('[name=amount]').fill('12.50');await dialog.locator('[name=reason]').fill(f.prefix);
   const created=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/api/admin/legacy/bank-refunds'));await dialog.getByRole('button',{name:'确认保存退款申请',exact:true}).click();const response=await (await created).json();assert.equal(response.code,200);const refund=response.data;await page.getByRole('dialog').waitFor({state:'hidden'});
   const refundRow=page.locator('[data-refund-list] tbody tr').filter({hasText:refund.refundOrderId});await refundRow.getByRole('button',{name:'发送退款',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'确认发送退款',exact:true}).click();await page.getByRole('dialog').waitFor({state:'hidden'});await page.waitForFunction(()=>document.querySelector('[data-refund-list]')?.textContent.includes('结果待核对'));
   assert.equal(await refundRow.getByRole('button',{name:'发送退款',exact:true}).count(),0);const retry=await page.evaluate(async key=>{const r=await fetch('/api/admin/legacy/bank-refunds/'+key+'/send',{method:'POST',headers:{Authorization:'Bearer '+localStorage.getItem('petlis.admin.token')}});return (await r.json()).code;},refund.requestKey);assert.equal(retry,409);
   await refundRow.getByRole('button',{name:'查询退款',exact:true}).click();await page.waitForFunction(()=>document.querySelector('[data-refund-list]')?.textContent.includes('已收到银行返回'));await refundRow.getByRole('button',{name:'查看银行返回',exact:true}).click();assert.match(await page.getByRole('dialog').innerText(),/OWNED_RESPONSE/);assert.match(await page.getByRole('dialog').innerText(),/<bank reply, display as text>/);await page.getByRole('dialog').getByRole('button',{name:'关闭',exact:true}).first().click();
   await page.reload();await page.locator('[data-refund-list]').getByText(refund.refundOrderId,{exact:true}).waitFor();assert.match(await page.locator('[data-refund-list]').innerText(),/已收到银行返回/);
  }
  preparationProof={actualPcPreparation:true,lostResponseRecovered:true,reopenedDialogUsesOriginalRequest:true,receiptSurvivesReload:true,requestKeys:writes.map(w=>w.requestKey)};
 }
 await page.locator('[name=mode]').selectOption('MONTH');await page.locator('[name=hospital]').fill(String(f.hospital));await page.locator('[name=month]').fill('2024-02');await page.getByRole('button',{name:'核对待付项目'}).click();await page.waitForFunction(()=>document.querySelector('[data-bank-result]')?.textContent.includes('应付 7.25 元'));assert.equal(await page.locator('[data-bank-result] tbody tr').count(),1);if(f.screenshot)await page.screenshot({path:f.screenshot,fullPage:true});assert.deepEqual(errors,[]);console.log(JSON.stringify({actualPcBankQuote:true,unreceivedFreightRuleVerified:true,monthlyHospitalUnpaidNonExemptRuleVerified:true,noRealBankDispatch: true,localDispatchVerified:Boolean(f.dispatch),anonymousNotificationAndQueryRace:Boolean(f.notify),pageErrors:0,...preparationProof}));
}finally{await browser?.close();await new Promise(r=>server.close(r));}
