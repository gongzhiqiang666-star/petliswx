/** Actual admin form, synthetic API. No real credentials or database operations. */
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:1'});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const writes=[],errors=[];
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(()=>{localStorage.setItem('petlis.admin.token','synthetic-token');localStorage.setItem('petlis.admin.user',JSON.stringify({username:'医院新增验收',roles:['ADMIN']}));});
 await context.route('**/*',route=>{const request=route.request(),url=new URL(request.url());if(url.origin!==origin)return route.abort();if(!url.pathname.startsWith('/api/'))return route.continue();let data={};
 if(url.pathname==='/api/admin/legacy/hospital-profiles/capabilities')data={hospitalProfiles:true,hospitalProfileWrites:true,hospitalProfileCreates:true};
 else if(url.pathname==='/api/admin/legacy/hospital-profiles'&&request.method()==='POST'){writes.push(request.postDataJSON());data=100;}
 else if(['/api/admin/users','/api/admin/hospitals'].includes(url.pathname))data={items:[],total:0};
 return route.fulfill({json:{code:200,data}});
 });
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/organizations');await page.locator('[data-hospital-new]').click();
 assert.equal(await page.locator('[name="active"]').inputValue(),'0');assert.equal(await page.locator('[name="payType"]').inputValue(),'0');assert.equal(await page.locator('[name="loginPassword"]').getAttribute('type'),'password');assert.equal(await page.locator('[name="amount"]').count(),0);
 await page.locator('[name="name"]').fill('合成医院');await page.getByRole('button',{name:'保存档案',exact:true}).click();assert.equal(writes.length,0);
 await page.locator('[name="englishName"]').fill('Synthetic Hospital');await page.locator('[name="loginName"]').fill('synthetic-login');await page.locator('[name="loginPassword"]').fill('synthetic-password');await page.getByRole('button',{name:'保存档案',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 assert.equal(writes.length,1);const payload=writes[0];assert.equal(payload.active,0);assert.equal(payload.profile.payType,0);assert.equal(payload.profile.name,'合成医院');assert.equal(payload.profile.englishName,'Synthetic Hospital');assert.equal(payload.loginName,'synthetic-login');assert.equal(payload.loginPassword,'synthetic-password');assert.equal(payload.profile.loginPassword,undefined);assert.equal(payload.profile.expectedToken,undefined);assert.ok(payload.profile.requestKey.length>=16);
 await page.locator('[data-hospital-new]').click();assert.equal(await page.locator('[name="loginPassword"]').inputValue(),'');assert.equal(await page.locator('[name="name"]').inputValue(),'');await page.keyboard.press('Escape');assert.deepEqual(errors,[]);
 console.log(JSON.stringify({syntheticApi:true,actualBrowserCreateForm:true,originalDisabledCashDefaults:true,englishRequired:true,passwordMaskedAndNotRetained:true,createPayloadVerified:true,pageErrors:0,realBackendAcceptance:false}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
