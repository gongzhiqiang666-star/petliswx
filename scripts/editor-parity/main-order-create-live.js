import assert from 'node:assert/strict';
import fs from 'node:fs';
const fixture=JSON.parse(fs.readFileSync(0,'utf8'));
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const creates=[],errors=[];let draft=null;
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 let orderId=null;
 await context.addInitScript(({token})=>{localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'真实主单创建验收',roles:['ADMIN']}));},{token:fixture.token});
 await context.route('**/*',async route=>{const request=route.request(),url=new URL(request.url());if(url.origin!==origin)return route.abort();
  if(url.pathname==='/api/admin/legacy/main-orders'&&request.method()==='POST'){
   const response=await route.fetch(),body=await response.json();if(body.code===200){orderId=body.data;creates.push(request.postDataJSON());fs.writeFileSync(fixture.journal,JSON.stringify({orderId,requestKey:request.postDataJSON().requestKey}),{mode:0o600});}return route.fulfill({response});
  }
  if(url.pathname==='/api/admin/legacy/main-orders'&&request.method()==='GET'){
   const response=await route.fetch(),body=await response.json();if(body.code===200){body.data.items=orderId?body.data.items.filter(row=>Number(row.orderId)===Number(orderId)):[];body.data.total=body.data.items.length;}return route.fulfill({response,json:body});
  }
  return route.continue();
 });

 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/orders');await page.locator('[data-main-order-new]').click();assert.equal(await page.locator('[name="autoLogistics"]').inputValue(),'false');await page.locator('[name="autoLogistics"]').selectOption('true');await page.getByRole('button',{name:'创建并填写资料',exact:true}).click();await page.getByRole('dialog',{name:'编辑送检主单',exact:true}).waitFor();
 assert.equal(creates.length,1);assert.equal(creates[0].autoLogistics,true);assert.ok(creates[0].requestKey.length>=16);assert.equal(await page.locator('[name="UserNO"]').inputValue(),'55550-');assert.equal(await page.locator('[name="InfoHospitalId"]').inputValue(),'0');
 await page.locator('[name="AnimalName"]').fill('新建动物');await page.locator('[name="UserNO"]').fill(fixture.prefix);await page.getByRole('button',{name:'保存主单',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));await page.locator(`[data-main-order-edit="${orderId}"]`).waitFor();assert.ok((await page.locator('#order-list').innerText()).includes('新建动物'));assert.ok((await page.locator('#order-list').innerText()).includes('未指定医院'));assert.deepEqual(errors,[]);
 console.log(JSON.stringify({realJavaCreate:true,orderId,actualPcCreateForm:true,freightOptionSubmitted:true,createThenEditor:true,zeroHospitalDraftVisible:true,saveRefresh:true,pageErrors:0,realBackendAcceptance:true}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
