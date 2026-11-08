/** Real PC Tiptap editor + synthetic API, no real patients or DB writes. */
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:1'});
await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
const image='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jGJkAAAAASUVORK5CYII=';
const original=`<p style="text-align:center"><strong>合成送检备注</strong></p><table border="1"><tbody><tr><td colspan="2"><p>保留表格内容</p></td></tr><tr><td><p>样本</p></td><td><p>已冷藏</p></td></tr></tbody></table><p><img src="${image}" width="12" height="12" alt="合成示意图"></p>`;
const row={ListDiagnosisProjectId:42,UserNO:'TEST-42',AnimalName:'合成动物',Remark:original,editToken:'a'.repeat(64)};
const writes=[],errors=[],external=[];let loseResponse=false;
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(()=>{localStorage.setItem('petlis.admin.token','synthetic-token');localStorage.setItem('petlis.admin.user',JSON.stringify({username:'合成备注验收',roles:['ADMIN']}));});
 await context.route('**/*',async route=>{const request=route.request(),url=new URL(request.url()),path=url.pathname;if(url.origin!==origin){external.push(request.url());return route.abort();}if(!path.startsWith('/api/'))return route.continue();let data={};
  if(path==='/api/admin/orders'||path==='/api/admin/legacy/main-orders')data={items:[{orderId:42,orderNo:row.UserNO,hospitalName:'合成医院',animalName:row.AnimalName,amount:'0.00',status:2}],total:1};
  else if(path==='/api/admin/legacy/main-orders/capabilities')data={mainOrders:true,mainOrderWrites:true};
  else if(path.includes('/choices/'))data={items:[],total:0};
  else if(path==='/api/admin/legacy/main-orders/42'&&request.method()==='PUT'){const command=request.postDataJSON();writes.push(command);Object.assign(row,command.fields);if(loseResponse){loseResponse=false;return route.abort();}data=42;}
  else if(path==='/api/admin/legacy/main-orders/42')data=row;
  return route.fulfill({json:{code:200,data}});
 });
 const page=await context.newPage();page.setDefaultTimeout(6000);page.on('pageerror',e=>errors.push(e.message));await page.goto(origin+'/#/orders');
 const open=async()=>{await page.locator('[data-main-order-edit="42"]').click();await page.locator('[data-main-remark-editor] [contenteditable="true"]').waitFor();};
 const save=()=>page.getByRole('button',{name:'保存主单',exact:true}).click();const closed=()=>page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 await open();assert.equal(await page.locator('textarea[name="Remark"]').count(),0);assert.equal(await page.locator('[data-main-remark-editor] table').count(),1);assert.equal(await page.locator('[data-main-remark-editor] td[colspan="2"]').count(),1);assert.equal(await page.locator('[data-main-remark-editor] img[src]').getAttribute('src'),image);
 await page.locator('[name="AnimalName"]').fill('修改合成动物');await save();await closed();assert.equal(writes.length,1);assert.equal(writes[0].fields.Remark,undefined);assert.equal(row.Remark,original);
 await open();const cell=page.locator('[data-main-remark-editor] td').first();await cell.locator('p').click();await page.keyboard.press('End');await page.keyboard.type(' 补充说明');loseResponse=true;await save();await page.locator('#dialog-error').filter({hasText:/网络|失败/}).waitFor();assert.equal(writes.length,2);const changed=writes[1].fields.Remark;assert.ok(changed.includes('补充说明'));assert.ok(changed.includes(image));assert.ok(changed.includes('colspan="2"'));assert.ok(changed.includes('已冷藏'));
 await save();await closed();assert.equal(writes.length,3);assert.deepEqual(writes[1],writes[2]);
 await open();assert.ok((await page.locator('[data-main-remark-editor]').innerText()).includes('补充说明'));await save();await page.getByText('没有需要保存的修改',{exact:true}).waitFor();assert.equal(writes.length,3);await page.keyboard.press('Escape');
 row.Remark='<p>原有内容</p><iframe src="https://never-fetch.invalid"></iframe>';await page.locator('[data-main-order-edit="42"]').click();await page.locator('[data-main-remark-feedback]').filter({hasText:/保留/}).waitFor();assert.equal(await page.locator('iframe').count(),0);await page.locator('[name="AnimalName"]').fill('仍可修改其他资料');await save();await closed();assert.equal(writes.at(-1).fields.Remark,undefined);assert.equal(row.Remark,'<p>原有内容</p><iframe src="https://never-fetch.invalid"></iframe>');assert.deepEqual(external,[]);assert.deepEqual(errors,[]);
 console.log(JSON.stringify({syntheticApi:true,actualPcEditor:true,untouchedHtmlPreserved:true,tableImageAndEditedTextPreserved:true,lostResponseRetrySameRequest:true,reopenAndNoChange:true,unsupportedHtmlCannotOverwrite:true,otherFieldsStillEditable:true,externalRequests:0,pageErrors:0,realBackendAcceptance:false}));
}finally{await browser?.close();await new Promise(r=>server.close(r));}
