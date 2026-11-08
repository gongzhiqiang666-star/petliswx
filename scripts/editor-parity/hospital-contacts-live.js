import fs from 'node:fs';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const fixture=JSON.parse(fs.readFileSync(0,'utf8'));
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const errors=[];
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
 const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(({token})=>{localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'合成联系人验收',roles:['ADMIN']}));},{token:fixture.token});
 await context.route('**/*',route=>{
  const url=new URL(route.request().url());if(url.origin!==origin)return route.abort();
  // Isolate discovery to our fixture, but all contact APIs use the real Java service.
  if(url.pathname==='/api/admin/hospitals')return route.fulfill({json:{code:200,data:{items:[{id:fixture.hospital,name:fixture.prefix,status:'ACTIVE'}],total:1}}});
  if(url.pathname==='/api/admin/users')return route.fulfill({json:{code:200,data:{items:[],total:0}}});
  return route.continue();
 });
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));
 await page.goto(origin+'/#/organizations');await page.locator('[data-tab="hospitals"]').click();await page.locator(`[data-hospital-contacts="${fixture.hospital}"]`).click();await page.locator('[data-contact-new]').click();
 await page.locator('[name="name"]').fill(fixture.prefix);await page.locator('[name="mail"]').fill('report@example.test');await page.locator('[name="financeContact"]').selectOption('1');await page.getByRole('button',{name:'保存联系人',exact:true}).click();await page.locator('[data-contact-edit]').waitFor();
 const id=Number(await page.locator('[data-contact-edit]').getAttribute('data-contact-edit'));
 await page.locator('[data-contact-edit]').click();assert.equal(await page.locator('[name="mail"]').inputValue(),'report@example.test');await page.locator('[name="mail"]').fill('finance@example.test');await page.getByRole('button',{name:'保存联系人',exact:true}).click();await page.getByText('finance@example.test',{exact:true}).waitFor();
 const result=await page.evaluate(async({hospital,id})=>{const response=await fetch(`/api/admin/legacy/hospital-contacts/${hospital}/${id}/recipient`,{headers:{Authorization:'Bearer '+localStorage.getItem('petlis.admin.token')}});return response.json();},{hospital:fixture.hospital,id});assert.equal(result.code,200);assert.equal(result.data.mail,'finance@example.test');
 const confirmation=page.waitForEvent('dialog');await page.locator('[data-contact-delete]').click();await (await confirmation).accept();await page.locator('[data-contact-list] .empty-state').waitFor();assert.deepEqual(errors,[]);
 console.log(JSON.stringify({realContactJavaApi:true,syntheticHospital:true,hospital:fixture.hospital,contactId:id,createEditReopen:true,recipientRead:true,deleteAndRefresh:true,pageErrors:0,discoveryListIsolated:true}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
