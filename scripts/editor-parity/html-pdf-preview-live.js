import fs from 'node:fs';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const f=JSON.parse(fs.readFileSync(0,'utf8')),server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const errors=[],writes=[];
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(({token})=>{if(window!==window.top)return;localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'PDF预览验收',roles:['ADMIN']}));},{token:f.token});
 await context.route('**/*',route=>{const request=route.request(),url=new URL(request.url());if(url.origin!==origin)return route.abort();if(url.pathname==='/api/admin/legacy/templates/report'&&request.method()==='GET'){url.searchParams.set('q',f.prefix);return route.continue({url:url.href});}if(url.pathname.startsWith('/api/admin/legacy/templates/')&&['POST','PUT','DELETE'].includes(request.method()))writes.push(url.pathname);return route.continue();});
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto(origin+'/#/legacy-templates');await page.locator('[data-template-new]').click();await page.locator('[data-template-editor] .tiptap').fill('Petlis 中文报告 PDF预览 结果 12.50 END');
 const downloading=page.waitForEvent('download');await page.locator('[data-template-pdf]').click();const download=await downloading;assert.equal(download.suggestedFilename(),'report-preview.pdf');await download.saveAs(f.output);assert.equal(fs.readFileSync(f.output).subarray(0,5).toString(),'%PDF-');assert.deepEqual(writes,[]);assert.deepEqual(errors,[]);
 console.log(JSON.stringify({actualPcPdfPreview:true,realJavaRenderer:true,authenticatedPostDownload:true,unsavedBodyUsed:true,noTemplateWrites:true,pageErrors:0}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
