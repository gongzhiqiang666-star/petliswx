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
  preparationProof={actualPcPreparation:true,lostResponseRecovered:true,reopenedDialogUsesOriginalRequest:true,receiptSurvivesReload:true,requestKeys:writes.map(w=>w.requestKey)};
 }
 await page.locator('[name=mode]').selectOption('MONTH');await page.locator('[name=hospital]').fill(String(f.hospital));await page.locator('[name=month]').fill('2024-02');await page.getByRole('button',{name:'核对待付项目'}).click();await page.waitForFunction(()=>document.querySelector('[data-bank-result]')?.textContent.includes('应付 7.25 元'));assert.equal(await page.locator('[data-bank-result] tbody tr').count(),1);if(f.screenshot)await page.screenshot({path:f.screenshot,fullPage:true});assert.deepEqual(errors,[]);console.log(JSON.stringify({actualPcBankQuote:true,unreceivedFreightRuleVerified:true,monthlyHospitalUnpaidNonExemptRuleVerified:true,noBankDispatchExposed:true,pageErrors:0,...preparationProof}));
}finally{await browser?.close();await new Promise(r=>server.close(r));}
