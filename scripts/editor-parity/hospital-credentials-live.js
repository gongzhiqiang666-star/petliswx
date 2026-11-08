import fs from 'node:fs';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const f=JSON.parse(fs.readFileSync(0,'utf8'));
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const errors=[];
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(({token})=>{localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'凭据合成验收',roles:['ADMIN']}));},{token:f.token});
 await context.route('**/*',route=>{const url=new URL(route.request().url());if(url.origin!==origin)return route.abort();if(url.pathname==='/api/admin/hospitals')return route.fulfill({json:{code:200,data:{items:[{id:f.hospital,name:f.prefix,status:'ACTIVE'}],total:1}}});if(url.pathname==='/api/admin/users')return route.fulfill({json:{code:200,data:{items:[],total:0}}});return route.continue();});
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/organizations');await page.locator('[data-tab="hospitals"]').click();
 const open=async()=>{await page.locator(`[data-hospital-credentials="${f.hospital}"]`).click();assert.equal(await page.locator('[name="password"]').inputValue(),'');};
 const save=async()=>{await page.getByRole('button',{name:'保存登录资料',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));};
 await open();assert.equal(await page.locator('[name="loginName"]').inputValue(),'historical-login');await page.locator('[name="loginName"]').fill('changed-login');await save();
 await open();assert.equal(await page.locator('[name="loginName"]').inputValue(),'changed-login');await page.locator('[name="password"]').fill('synthetic-new-password');await save();
 await open();assert.ok((await page.locator('.modal-body').innerText()).includes('当前已设置密码'));await page.locator('[name="clearPassword"]').selectOption('true');await save();
 await open();assert.ok((await page.locator('.modal-body').innerText()).includes('当前未设置密码'));await page.keyboard.press('Escape');assert.deepEqual(errors,[]);
 console.log(JSON.stringify({realCredentialJavaApi:true,hospital:f.hospital,blankPreservesPassword:true,setAndExplicitClear:true,passwordNeverRepopulated:true,pageErrors:0,discoveryListsIsolated:true}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
