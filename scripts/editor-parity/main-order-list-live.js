import fs from 'node:fs';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const fixture=JSON.parse(fs.readFileSync(0,'utf8'));
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:18082'});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;const errors=[],lists=[];
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
 const context=await browser.newContext({serviceWorkers:'block'}),origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(({token})=>{localStorage.setItem('petlis.admin.token',token);localStorage.setItem('petlis.admin.user',JSON.stringify({username:'数据组隔离验收',roles:['ADMIN']}));},{token:fixture.token});
 // No API responses are replaced; other origins cannot be reached.
 await context.route('**/*',route=>new URL(route.request().url()).origin===origin?route.continue():route.abort());
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));
 page.on('response',async response=>{if(new URL(response.url()).pathname==='/api/admin/legacy/main-orders'&&response.request().method()==='GET')lists.push(await response.json());});
 await page.goto(origin+'/#/orders');await page.locator(`[data-print-order="${fixture.allowed}"]`).waitFor();
 assert.equal(await page.locator('[data-print-order]').count(),1);
 for(const id of fixture.excluded)assert.equal(await page.locator(`[data-print-order="${id}"]`).count(),0);
 assert.ok(lists.length>0);for(const body of lists){assert.equal(body.code,200);assert.equal(Number(body.data.total),1);assert.deepEqual(body.data.items.map(row=>Number(row.orderId)),[fixture.allowed]);}
 assert.deepEqual(errors,[]);
 console.log(JSON.stringify({actualPcList:true,realJavaList:true,noApiMocks:true,originalDataGroupFiltered:true,negativeStateExcluded:true,pageErrors:0}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
