import fs from 'node:fs';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const fixture=JSON.parse(fs.readFileSync(0,'utf8'));
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const journal=[],errors=[];
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(({token})=>{if(window!==window.top)return;localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'模板真实验收',roles:['ADMIN']}));},{token:fixture.token});
 await context.route('**/*',async route=>{const request=route.request(),url=new URL(request.url());if(url.origin!==origin)return route.abort();
  if(/^\/api\/admin\/legacy\/templates\/(report|mail)$/.test(url.pathname)&&request.method()==='GET'){url.searchParams.set('q',fixture.prefix);return route.continue({url:url.href});}
  if(/^\/api\/admin\/legacy\/templates\/(report|mail)(?:\/\d+(?:\/delete)?)?$/.test(url.pathname)&&['POST','PUT'].includes(request.method())){
   const response=await route.fetch(),body=await response.json();if(body.code===200){journal.push({kind:url.pathname.split('/')[5],id:Number(body.data),requestKey:request.postDataJSON().requestKey,method:request.method(),path:url.pathname});fs.writeFileSync(fixture.journal,JSON.stringify(journal),{mode:0o600});}return route.fulfill({response});
  }return route.continue();
 });
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));await page.goto(origin+'/#/legacy-templates');await page.locator('[data-template-new]').click();
 await page.locator('[name=title]').fill(fixture.prefix+'_report');await page.locator('[data-template-editor] .tiptap').fill('报告 {{标本号}} 中文 English α');await page.getByRole('button',{name:'保存模板',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 const report=journal.find(item=>item.kind==='report').id;await page.locator(`[data-template-edit="${report}"]`).click();assert.match(await page.locator('[data-template-editor] .tiptap').innerText(),/报告.*中文 English α/);
 await page.locator('[aria-label="检测项目搜索"]').fill(fixture.projectNo);await page.getByRole('button',{name:'搜索项目',exact:true}).click();await page.locator('[name=projectId] option[value="246"]').waitFor({state:'attached'});await page.locator('[name=projectId]').selectOption('246');
 await page.locator('[name=title]').fill(fixture.prefix+'_updated');await page.locator('[data-template-editor] .tiptap').fill('报告修订 {{标本号}} 中文 English α');await page.getByRole('button',{name:'保存模板',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 await page.locator(`[data-template-edit="${report}"]`).click();assert.equal(await page.locator('[name=projectId]').inputValue(),'246');assert.match(await page.locator('[data-template-editor] .tiptap').innerText(),/报告修订/);await page.keyboard.press('Escape');
 await page.locator(`[data-template-view="${report}"]`).click();assert.equal(await page.locator('[data-template-preview]').getAttribute('sandbox'),'');assert.match(await page.frameLocator('[data-template-preview]').locator('body').innerText(),/报告修订/);await page.keyboard.press('Escape');
 await page.locator('[data-template-kind]').selectOption('mail');await page.locator('[data-template-new]').click();await page.locator('[name=title]').fill(fixture.prefix+'_mail');await page.getByRole('button',{name:'保存模板',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));
 const mail=journal.find(item=>item.kind==='mail').id;await page.locator(`[data-template-edit="${mail}"]`).click();assert.equal((await page.locator('[data-template-editor] .tiptap').innerText()).trim(),'');await page.keyboard.press('Escape');
 await page.locator(`[data-template-delete="${mail}"]`).click();await page.getByRole('button',{name:'确认删除模板',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#active-dialog-form'));assert.equal(await page.locator(`[data-template-edit="${mail}"]`).count(),0);
 assert.deepEqual(errors,[]);console.log(JSON.stringify({actualPcTemplateMaintenance:true,realJava:true,reportCreateEditReopen:true,projectBinding:true,sandboxedPreview:true,mailEmptyBody:true,mailDelete:true,pageErrors:0,report,mail,discoveryQueryRestricted:true}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
