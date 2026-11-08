/** Own synthetic order, actual Chrome -> Java API; discovery list alone is synthetic. */
import fs from 'node:fs';import assert from 'node:assert/strict';import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';import {chromium} from 'playwright-core';
const f=JSON.parse(fs.readFileSync(0,'utf8')),errors=[],external=[],writes=[];let browser,drop=true;
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});
await new Promise(r=>server.listen(0,'127.0.0.1',r));
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block',viewport:{width:1400,height:1000}}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(({token})=>{localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'合成主单备注验收',roles:['ADMIN']}));},{token:f.token});
 await context.route('**/*',async route=>{const request=route.request(),url=new URL(request.url());if(url.origin!==origin){external.push(url.origin);return route.abort();}
  if(url.pathname==='/api/admin/orders'||url.pathname==='/api/admin/legacy/main-orders')return route.fulfill({json:{code:200,data:{items:[{orderId:f.order,orderNo:f.prefix,hospitalName:'合成医院',animalName:'合成动物',amount:'125.50',status:2}],total:1}}});
  if(url.pathname===`/api/admin/legacy/main-orders/${f.order}`&&request.method()==='PUT'){
   writes.push(request.postDataJSON());if(drop&&writes.length===2){drop=false;const response=await route.fetch();assert.equal(response.status(),200);assert.equal((await response.json()).code,200);return route.abort();}
  }
  return route.continue();
 });
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto(origin+'/#/orders');
 const open=async()=>{await page.locator(`[data-main-order-edit="${f.order}"]`).click();await page.locator('[data-main-remark-editor] [contenteditable=true]').waitFor();};
 const save=()=>page.getByRole('button',{name:'保存主单',exact:true}).click();const closed=()=>page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 await open();assert.equal(await page.locator('[data-main-remark-editor] table').count(),1);assert.equal(await page.locator('[data-main-remark-editor] img[src]').count(),1);
 await page.locator('[name="AnimalName"]').fill('合成备注验收动物');await save();await closed();assert.equal(writes.length,1);assert.equal(writes[0].fields.Remark,undefined);
 await open();await page.locator('[data-main-remark-editor] td p').first().click();await page.keyboard.press('End');await page.keyboard.type(' 补充复检说明');await save();await page.locator('#dialog-error').filter({hasText:/网络|失败/}).waitFor();assert.equal(writes.length,2);await save();await closed();assert.equal(writes.length,3);assert.deepEqual(writes[1],writes[2]);
 await open();assert.ok((await page.locator('[data-main-remark-editor]').innerText()).includes('补充复检说明'));assert.equal(await page.locator('[data-main-remark-editor] td[colspan="2"]').count(),1);assert.equal(await page.locator('[data-main-remark-editor] img[src]').getAttribute('src'),f.image);
 if(f.screenshot)await page.locator('[data-main-remark-editor]').screenshot({path:f.screenshot});
 await save();await page.getByText('没有需要保存的修改',{exact:true}).waitFor();assert.equal(writes.length,3);assert.deepEqual(external,[]);assert.deepEqual(errors,[]);
 const unauthorized=await context.request.get(origin+`/api/admin/legacy/main-orders/${f.order}`);assert.equal(unauthorized.status(),401);
 console.log(JSON.stringify({actualChrome:true,actualJava:true,discoveryOnlySynthetic:true,untouchedRemarkOmitted:true,richRemarkEditedSavedReopened:true,responseLostAfterCommitSameRequest:true,noChangeNoWrite:true,anonymousRejected:true,externalRequests:0,pageErrors:0,requestKey:writes[1].requestKey,html:writes[1].fields.Remark}));
}finally{await browser?.close();await new Promise(r=>server.close(r));}
