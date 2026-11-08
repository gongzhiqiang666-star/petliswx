/** Real admin DOM, synthetic API only; no database or external services. */
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:1'});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
let browser,writable=true,fail=true;const rows=[],writes=[],errors=[];const base='/api/admin/legacy/hospital-contacts';
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
 const context=await browser.newContext({serviceWorkers:'block'});const origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(()=>{localStorage.setItem('petlis.admin.token','synthetic-test-token');localStorage.setItem('petlis.admin.user',JSON.stringify({username:'联系人页面验收',roles:['ADMIN']}));});
 await context.route('**/*',async route=>{
  const request=route.request(),url=new URL(request.url()),path=url.pathname;if(url.origin!==origin)return route.abort();if(!path.startsWith('/api/'))return route.continue();let data={};
  if(path===base+'/capabilities')data={hospitalContacts:true,hospitalContactWrites:writable,hospitalContactDeletes:writable};
  else if(path==='/api/admin/users')data={items:[],total:0};
  else if(path==='/api/admin/hospitals')data={items:[{id:42,name:'合成医院',status:'ACTIVE'}],total:1};
  else if(path===base+'/42'&&request.method()==='POST'){
   const c=request.postDataJSON();writes.push(c);if(fail){fail=false;return route.fulfill({status:503,json:{code:503,message:'合成连接失败'}});}
   rows.push({InfoHospitalContactId:9,InfoHospitalId:42,Name:c.name,Tel:c.tel,Mail:c.mail,Fax:c.fax,ContactRemark:c.remark,isFinanceContact:c.financeContact,editToken:'a'.repeat(64)});data=9;
  }else if(path===base+'/42/9'&&request.method()==='PUT'){
   const c=request.postDataJSON();writes.push(c);assert.equal(c.expectedToken,'a'.repeat(64));Object.assign(rows[0],{Name:c.name,Mail:c.mail,isFinanceContact:c.financeContact});data=9;
  }else if(path===base+'/42')data={items:rows,total:rows.length};
  else if(path===base+'/42/9'&&request.method()==='DELETE'){writes.push(request.postDataJSON());rows.splice(0);data=9;}
  else if(path===base+'/42/9')data=rows[0];
  return route.fulfill({json:{code:200,data}});
 });
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/organizations');await page.locator('[data-tab="hospitals"]').click();await page.locator('[data-hospital-contacts="42"]').click();await page.locator('[data-contact-new]').click();
 await page.locator('[name="name"]').fill('<联系人>');await page.locator('[name="mail"]').fill('report@example.test');await page.locator('[name="financeContact"]').selectOption('1');await page.getByRole('button',{name:'保存联系人',exact:true}).click();
 await page.getByText('合成连接失败',{exact:true}).waitFor();
 await page.locator('[name="mail"]').fill('changed@example.test');await page.getByRole('button',{name:'保存联系人',exact:true}).click();await page.getByText('上次保存结果未确认，请先用原内容重试或重新打开核对',{exact:true}).waitFor();assert.equal(writes.length,1);
 await page.locator('[name="mail"]').fill('report@example.test');await page.getByRole('button',{name:'保存联系人',exact:true}).click();await page.locator('[data-contact-edit="9"]').waitFor();assert.deepEqual(writes[0],writes[1]);assert.ok((await page.locator('[data-contact-list]').innerText()).includes('<联系人>'));assert.equal(await page.locator('[data-contact-list]').locator('联系人').count(),0);
 await page.locator('[data-contact-edit="9"]').click();assert.equal(await page.locator('[name="mail"]').inputValue(),'report@example.test');await page.locator('[name="mail"]').fill('finance@example.test');await page.getByRole('button',{name:'保存联系人',exact:true}).click();await page.locator('[data-contact-edit="9"]').waitFor();assert.ok((await page.locator('[data-contact-list]').innerText()).includes('finance@example.test'));
 const cancelled=page.waitForEvent('dialog');await page.locator('[data-contact-delete="9"]').click();await (await cancelled).dismiss();await page.evaluate(()=>Promise.resolve());assert.equal(rows.length,1);
 const confirmed=page.waitForEvent('dialog');await page.locator('[data-contact-delete="9"]').click();await (await confirmed).accept();await page.locator('[data-contact-list] .empty-state').waitFor();assert.equal(rows.length,0);
 await page.keyboard.press('Escape');writable=false;await page.locator('[data-hospital-contacts="42"]').click();assert.equal(await page.locator('[data-contact-new],[data-contact-edit]').count(),0);assert.deepEqual(errors,[]);
 console.log(JSON.stringify({syntheticApi:true,actualBrowserForm:true,createEditReopen:true,uncertainRetrySameKeyAndPayload:true,changedRetryBlocked:true,escapedContactName:true,writeFlagEnforced:true,deleteConfirmationAndRefresh:true,pageErrors:0,realBackendAcceptance:false}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
