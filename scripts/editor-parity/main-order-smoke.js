/** Actual PC order editor with synthetic API; no real orders are touched. */
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:1'});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;let writable=true;const writes=[],errors=[];
const row={ListDiagnosisProjectId:42,UserNO:'TEST-42',AnimalName:'原宠物',MasterName:'原主人',CollectDt:'2024-05-06 00:00:00',TestFee:'125.50',IsPayMoney:1,editToken:'a'.repeat(64)};
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(()=>{localStorage.setItem('petlis.admin.token','synthetic-token');localStorage.setItem('petlis.admin.user',JSON.stringify({username:'主单页面验收',roles:['ADMIN']}));});
 await context.route('**/*',route=>{const request=route.request(),url=new URL(request.url()),path=url.pathname;if(url.origin!==origin)return route.abort();if(!path.startsWith('/api/'))return route.continue();let data={};
 if(path==='/api/admin/orders')data={items:[{orderId:42,orderNo:row.UserNO,hospitalName:'合成医院',animalName:row.AnimalName,amount:'125.50',status:2}],total:1};
 else if(path==='/api/admin/legacy/main-orders/capabilities')data={mainOrders:true,mainOrderWrites:writable};
 else if(path==='/api/admin/legacy/main-orders/42'&&request.method()==='PUT'){const c=request.postDataJSON();writes.push(c);for(const [key,value] of Object.entries(c.fields))if(!(key==='CollectDt'&&value===''))row[key]=value;data=42;}
 else if(path==='/api/admin/legacy/main-orders/42')data=row;
 return route.fulfill({json:{code:200,data}});
 });
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/orders');await page.locator('[data-main-order-edit="42"]').click();assert.equal(await page.locator('[name="AnimalName"]').inputValue(),'原宠物');assert.equal(await page.locator('[name="TestFee"],[name="IsPayMoney"]').count(),0);
 await page.locator('[name="AnimalName"]').fill('修改宠物');await page.locator('[name="MasterName"]').fill('修改主人');await page.locator('[name="CollectDt"]').fill('');await page.getByRole('button',{name:'保存主单',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 assert.equal(writes.length,1);assert.deepEqual(writes[0].fields,{AnimalName:'修改宠物',MasterName:'修改主人',CollectDt:''});assert.equal(writes[0].expectedToken,'a'.repeat(64));assert.ok(writes[0].requestKey.length>=16);
 await page.locator('[data-main-order-edit="42"]').click();assert.equal(await page.locator('[name="AnimalName"]').inputValue(),'修改宠物');assert.equal(await page.locator('[name="CollectDt"]').inputValue(),'2024-05-06 00:00:00');await page.getByRole('button',{name:'保存主单',exact:true}).click();await page.getByText('没有需要保存的修改',{exact:true}).waitFor();assert.equal(writes.length,1);await page.keyboard.press('Escape');
 writable=false;await page.reload();await page.getByRole('heading',{name:'送检管理',exact:true}).waitFor();assert.equal(await page.locator('[data-main-order-edit]').count(),0);assert.deepEqual(errors,[]);
 console.log(JSON.stringify({syntheticApi:true,actualPcForm:true,editSaveReopen:true,onlyChangedFieldsSubmitted:true,noFinancialInputs:true,emptyDatePreservesOriginal:true,noChangeDoesNotWrite:true,writeFlagEnforced:true,pageErrors:0,realBackendAcceptance:false}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
