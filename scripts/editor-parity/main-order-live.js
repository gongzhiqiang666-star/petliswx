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
 await context.addInitScript(({token})=>{localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'合成主单验收',roles:['ADMIN']}));},{token:f.token});
 await context.route('**/*',route=>{const url=new URL(route.request().url());if(url.origin!==origin)return route.abort();if(url.pathname==='/api/admin/orders')return route.fulfill({json:{code:200,data:{items:[{orderId:f.order,orderNo:f.prefix,hospitalName:'合成医院',animalName:'合成宠物',amount:'125.50',status:2}],total:1}}});return route.continue();});
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/orders');await page.locator(`[data-main-order-edit="${f.order}"]`).click();await page.locator('[name="AnimalName"]').fill('主单修改宠物');await page.locator('[name="MasterName"]').fill('主单修改主人');await page.locator('[name="CollectDt"]').fill('');assert.equal(await page.locator('[name="TestFee"],[name="IsPayMoney"]').count(),0);
 await page.getByRole('button',{name:'保存主单',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));await page.locator(`[data-main-order-edit="${f.order}"]`).click();assert.equal(await page.locator('[name="AnimalName"]').inputValue(),'主单修改宠物');assert.equal(await page.locator('[name="MasterName"]').inputValue(),'主单修改主人');assert.equal(await page.locator('[name="CollectDt"]').inputValue(),'2024-05-06 00:00:00');await page.keyboard.press('Escape');assert.deepEqual(errors,[]);
 console.log(JSON.stringify({realJavaMainOrderApi:true,syntheticOrder:true,order:f.order,editSaveReopen:true,emptyDatePreserved:true,noFinancialInputs:true,pageErrors:0,discoveryListIsolated:true}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
