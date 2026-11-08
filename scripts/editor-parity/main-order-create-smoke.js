import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:1'});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const creates=[],errors=[];let draft=null;
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(()=>{localStorage.setItem('petlis.admin.token','synthetic-token');localStorage.setItem('petlis.admin.user',JSON.stringify({username:'主单创建验收',roles:['ADMIN']}));});
 await context.route('**/*',route=>{const request=route.request(),url=new URL(request.url()),path=url.pathname;if(url.origin!==origin)return route.abort();if(!path.startsWith('/api/'))return route.continue();let data={};
 if(path==='/api/admin/legacy/main-orders/capabilities')data={mainOrders:true,mainOrderWrites:true,mainOrderCreates:true};
 else if(path==='/api/admin/legacy/main-orders'&&request.method()==='POST'){creates.push(request.postDataJSON());draft={ListDiagnosisProjectId:43,UserNO:'55550-',AnimalName:'',MasterName:'',InfoHospitalId:0,SampleStatus:2,editToken:'a'.repeat(64)};data=43;}
 else if(path==='/api/admin/legacy/main-orders/43'&&request.method()==='PUT'){Object.assign(draft,request.postDataJSON().fields);data=43;}
 else if(path==='/api/admin/legacy/main-orders/43')data=draft;
 else if(path==='/api/admin/legacy/main-orders')data={items:draft?[{orderId:43,orderNo:draft.UserNO,hospitalName:'未指定医院',animalName:draft.AnimalName,status:2,amount:'0.00'}]:[],total:draft?1:0};
 return route.fulfill({json:{code:200,data}});
 });
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/orders');await page.locator('[data-main-order-new]').click();assert.equal(await page.locator('[name="autoLogistics"]').inputValue(),'false');await page.locator('[name="autoLogistics"]').selectOption('true');await page.getByRole('button',{name:'创建并填写资料',exact:true}).click();await page.getByRole('dialog',{name:'编辑送检主单',exact:true}).waitFor();
 assert.equal(creates.length,1);assert.equal(creates[0].autoLogistics,true);assert.ok(creates[0].requestKey.length>=16);assert.equal(await page.locator('[name="UserNO"]').inputValue(),'55550-');assert.equal(await page.locator('[name="InfoHospitalId"]').inputValue(),'0');
 await page.locator('[name="AnimalName"]').fill('新建动物');await page.getByRole('button',{name:'保存主单',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));await page.locator('[data-main-order-edit="43"]').waitFor();assert.ok((await page.locator('#order-list').innerText()).includes('新建动物'));assert.ok((await page.locator('#order-list').innerText()).includes('未指定医院'));assert.deepEqual(errors,[]);
 console.log(JSON.stringify({syntheticApi:true,actualPcCreateForm:true,freightOptionSubmitted:true,createThenEditor:true,zeroHospitalDraftVisible:true,saveRefresh:true,pageErrors:0,realBackendAcceptance:false}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
