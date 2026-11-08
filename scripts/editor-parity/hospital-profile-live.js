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
 await context.addInitScript(({token})=>{localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'档案合成验收',roles:['ADMIN']}));},{token:f.token});
 await context.route('**/*',route=>{const url=new URL(route.request().url());if(url.origin!==origin)return route.abort();if(url.pathname==='/api/admin/hospitals')return route.fulfill({json:{code:200,data:{items:[{id:f.hospital,name:f.prefix,status:'ACTIVE'}],total:1}}});if(url.pathname==='/api/admin/users')return route.fulfill({json:{code:200,data:{items:[],total:0}}});return route.continue();});
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/organizations');await page.locator('[data-tab="hospitals"]').click();await page.locator(`[data-hospital-profile="${f.hospital}"]`).click();
 await page.locator('[name="name"]').fill(f.prefix+'_edited');await page.locator('[name="englishName"]').fill('Synthetic Hospital');await page.locator('[name="invoice"]').fill('合成开票抬头');await page.locator('[name="cityId"]').selectOption({index:1});const selectedCity=Number(await page.locator('[name="cityId"]').inputValue());assert.ok(selectedCity>0);await page.locator('[name="place"]').fill('合成地区');await page.locator('[name="address"]').fill('合成地址');await page.locator('[name="tel"]').fill('test-phone');await page.locator('[name="fax"]').fill('test-fax');await page.locator('[name="payType"]').selectOption('1');await page.getByRole('button',{name:'保存档案',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 await page.locator(`[data-hospital-profile="${f.hospital}"]`).click();assert.equal(await page.locator('[name="name"]').inputValue(),f.prefix+'_edited');assert.equal(await page.locator('[name="invoice"]').inputValue(),'合成开票抬头');assert.equal(await page.locator('[name="payType"]').inputValue(),'1');assert.equal(await page.locator('[name="englishName"]').inputValue(),'Synthetic Hospital');assert.equal(Number(await page.locator('[name="cityId"]').inputValue()),selectedCity);assert.equal(await page.locator('[name="amount"],[name="LoginPwd"]').count(),0);await page.keyboard.press('Escape');assert.deepEqual(errors,[]);
 console.log(JSON.stringify({realJavaProfileApi:true,syntheticHospital:true,hospital:f.hospital,editSaveReopen:true,selectedCity,pageErrors:0,discoveryListIsolated:true}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
