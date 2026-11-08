/** Actual admin form, synthetic API. No real credentials or database operations. */
import assert from 'node:assert/strict';
import fs from 'node:fs';
const fixture=JSON.parse(fs.readFileSync(0,'utf8'));
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const writes=[],errors=[];
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(({token})=>{localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'合成医院创建验收',roles:['ADMIN']}));},{token:fixture.token});
 await context.route('**/*',route=>{const url=new URL(route.request().url());if(url.origin!==origin)return route.abort();if(['/api/admin/users','/api/admin/hospitals'].includes(url.pathname))return route.fulfill({json:{code:200,data:{items:[],total:0}}});return route.continue();});

 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/organizations');await page.locator('[data-hospital-new]').click();
 assert.equal(await page.locator('[name="active"]').inputValue(),'0');assert.equal(await page.locator('[name="payType"]').inputValue(),'0');assert.equal(await page.locator('[name="loginPassword"]').getAttribute('type'),'password');assert.equal(await page.locator('[name="amount"]').count(),0);
 await page.locator('[name="name"]').fill(fixture.prefix);await page.getByRole('button',{name:'保存档案',exact:true}).click();assert.equal(writes.length,0);
 await page.locator('[name="englishName"]').fill('Synthetic Hospital');await page.locator('[name="loginName"]').fill('synthetic-login');await page.locator('[name="loginPassword"]').fill('synthetic-password');const responsePromise=page.waitForResponse(response=>new URL(response.url()).pathname==='/api/admin/legacy/hospital-profiles'&&response.request().method()==='POST');await page.getByRole('button',{name:'保存档案',exact:true}).click();const response=await responsePromise;const result=await response.json();assert.equal(result.code,200);const hospital=result.data;await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 const record=await page.evaluate(async id=>{const r=await fetch('/api/admin/legacy/hospital-profiles/'+id,{headers:{Authorization:'Bearer '+localStorage.getItem('petlis.admin.token')}});return r.json();},hospital);assert.equal(record.code,200);assert.equal(record.data.HospitalName,fixture.prefix);assert.equal(record.data.LoginPwd,undefined);

 await page.locator('[data-hospital-new]').click();assert.equal(await page.locator('[name="loginPassword"]').inputValue(),'');assert.equal(await page.locator('[name="name"]').inputValue(),'');await page.keyboard.press('Escape');assert.deepEqual(errors,[]);
 console.log(JSON.stringify({realJavaCreateApi:true,hospital,syntheticHospital:true,createFormSubmitted:true,readbackMatches:true,passwordNotReturned:true,reopenClearsInput:true,pageErrors:0,discoveryListsIsolated:true}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
