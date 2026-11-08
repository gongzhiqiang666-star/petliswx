import fs from 'node:fs';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const f=JSON.parse(fs.readFileSync(0,'utf8')),server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const journal=[],errors=[];
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(({token})=>{if(window!==window.top)return;localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'变量绑定验收',roles:['ADMIN']}));},{token:f.token});
 await context.route('**/*',async route=>{const request=route.request(),url=new URL(request.url());if(url.origin!==origin)return route.abort();if(url.pathname==='/api/admin/legacy/template-bindings'&&request.method()==='GET'){url.searchParams.set('q',f.prefix);return route.continue({url:url.href});}
  if(/^\/api\/admin\/legacy\/template-bindings(?:\/\d+(?:\/delete)?)?$/.test(url.pathname)&&['POST','PUT'].includes(request.method())){const response=await route.fetch(),body=await response.json();if(body.code===200){journal.push({id:Number(body.data),requestKey:request.postDataJSON().requestKey});fs.writeFileSync(f.journal,JSON.stringify(journal),{mode:0o600});}return route.fulfill({response});}return route.continue();
 });
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto(origin+'/#/legacy-template-bindings');await page.locator('[data-binding-new]').click();
 await page.locator('[name=bindTable]').fill('ListDiagnosisProject');await page.locator('[name=columnName]').fill('AnimalName');await page.locator('input[name=description]').fill(f.prefix);await page.getByRole('button',{name:'保存绑定',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 const id=journal[0].id;await page.locator(`[data-binding-edit="${id}"]`).click();assert.equal(await page.locator('[name=columnName]').inputValue(),'AnimalName');await page.locator('[name=columnName]').fill('MasterName');await page.getByRole('button',{name:'保存绑定',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 await page.locator(`[data-binding-edit="${id}"]`).click();assert.equal(await page.locator('[name=columnName]').inputValue(),'MasterName');assert.equal(await page.locator('input[name=description]').inputValue(),f.prefix);await page.keyboard.press('Escape');
 await page.locator(`[data-binding-delete="${id}"]`).click();await page.getByRole('button',{name:'确认删除绑定',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));assert.equal(await page.locator(`[data-binding-edit="${id}"]`).count(),0);assert.deepEqual(errors,[]);
 console.log(JSON.stringify({actualPcBindings:true,realJava:true,createEditReopen:true,deleteConfirmed:true,pageErrors:0,discoveryQueryRestricted:true,id}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
