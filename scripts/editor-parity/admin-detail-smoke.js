/** Full admin UI with synthetic API responses. Never connects to MySQL or real services. */
import assert from 'node:assert/strict';
import {mkdir} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {createStaticServer} from '../../admin/server.js';
import {chromium} from 'playwright-core';
const server=createStaticServer({root:fileURLToPath(new URL('../../admin/',import.meta.url)),apiOrigin:'http://127.0.0.1:1'});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
let browser;
const writes=[],errors=[];
const base='/api/admin/legacy/order-details',report='<p style="color:rgb(18, 52, 86)">报告原文</p><table style="width:400px"><colgroup><col width="100"><col width="300"></colgroup><tbody><tr><td>项目</td><td>结果</td></tr></tbody></table>';
const saved=[];let enabled=true;
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
 const context=await browser.newContext({serviceWorkers:'block',viewport:{width:1440,height:1050}});
 const origin=`http://127.0.0.1:${server.address().port}`;
 await context.addInitScript(()=>{if(window!==window.top)return;localStorage.setItem('petlis.admin.token','synthetic-ui-test');localStorage.setItem('petlis.admin.user',JSON.stringify({userId:1,username:'界面验收',roles:['ADMIN']}));});
 await context.route('**/*',async route=>{
  const request=route.request(),url=new URL(request.url());
  if(url.origin!==origin)return route.abort();
  if(!url.pathname.startsWith('/api/'))return route.continue();
  const path=url.pathname;
  let data;
  if(path===base+'/capabilities')data={orderDetails:true,orderDetailWrites:enabled,orderDetailUpdates:enabled};
  else if(path==='/api/admin/legacy/capabilities'||path==='/api/admin/legacy/test-catalog/capabilities')data={};
  else if(path==='/api/admin/orders')data={items:[{orderId:42,orderNo:'UI-TEST-42',animalName:'测试宠物',hospitalName:'合成测试医院',amount:'200.00',status:2}],total:1};
  else if(path===base+'/orders/42/form-defaults')data={MailTitle:'测试邮件标题',MailReport:'<p>邮件原文</p>',ReportResult:report,OrginalDiscount:'10',DictMailDiagnosisReportId:1};
  else if(path===base+'/tests')data={items:[{InfoTestTypeId:11,TestNO:'T11',TestName:'检测项目甲'},{InfoTestTypeId:1225,TestNO:'T1225',TestName:'狂犬检测'}],total:2};
  else if(path.endsWith('/prices'))data={items:[{IID:71,price:'200.00',IsActive:1},{IID:72,price:'100.00',IsActive:0}],total:2};
  else if(path.endsWith('/templates'))data={items:[{InfoTestTypeModelId:81,ModelName:'报告模板甲'}],total:1};
  else if(path===base+'/mail-templates')data={items:[{id:1,model_type:'报告邮件'}],total:1};
  else if(path.endsWith('/test-mail-title'))data={title:'测试邮件标题检测项目甲'};
  else if(path.endsWith('/quote')){assert.deepEqual(request.postDataJSON(),{price:'200.00',discount:'9'});data={price:'180.00'};}
  else if(path.includes('/render/report/'))data={html:'<p>模板报告正文</p>'};
  else if(path.includes('/render/mail/'))data={html:'<p>模板邮件正文</p>'};
  else if(path===base+'/orders/42'&&request.method()==='POST'){
   const command=request.postDataJSON();writes.push(command);
   saved.push({DiagnosisDetailId:91,GenterNO:'42-91',InfoTestTypeId:11,InfoTestTypeModelId:81,OrginalPrice:'200.00',OrginalDiscount:'9',IsSendMail:0,IsCheckPass:0,SendMailDt:'1900-01-01 00:00:00',editToken:'a'.repeat(64),TestName:'检测项目甲',TestPrice:command.testPrice,MailTitle:command.mailTitle,MailReport:command.mailReport,ReportResult:command.reportResult});data=91;
  }else if(path===base+'/orders/42/91'&&request.method()==='PUT'){
   const command=request.postDataJSON();writes.push(command);assert.equal(command.expectedToken,'a'.repeat(64));assert.equal(command.isCheckPass,1);assert.equal(command.sendMailDt,'');
   Object.assign(saved[0],{TestPrice:command.testPrice,ReportResult:command.reportResult,MailReport:command.mailReport,IsCheckPass:command.isCheckPass});data=91;
  }else if(path===base+'/orders/42')data={items:saved,total:saved.length};
  else if(path===base+'/orders/42/91')data=saved[0];
  else throw Error('Unexpected test API '+request.method()+' '+path);
  return route.fulfill({json:{code:200,data}});
 });
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));
 await page.goto(origin+'/#/orders');
 await page.locator('[data-order-details="42"]').click();await page.locator('[data-detail-create]').click();
 await page.locator('[name="testTypeId"] option[value="11"]').waitFor({state:'attached'});
 await page.locator('[name="testTypeId"]').selectOption('11');
 await page.waitForFunction(()=>document.querySelector('[name="originalPrice"]')?.value==='200.00');
 await page.locator('[name="originalDiscount"]').fill('9');await page.locator('[data-detail-quote]').click();
 await page.waitForFunction(()=>document.querySelector('[name="testPrice"]')?.value==='180.00');
 assert.equal(await page.locator('[data-report-editor] .tiptap p').first().evaluate(element=>getComputedStyle(element).color),'rgb(18, 52, 86)');
 const reportEditor=page.locator('[data-report-editor] .tiptap');
 await reportEditor.locator('p').first().click();await page.keyboard.press('End');await page.keyboard.type('新增结果');
 await page.locator('[data-report-editor]').getByRole('button',{name:'撤销',exact:true}).click();
 assert.ok(!(await reportEditor.innerText()).includes('新增结果'));
 await page.locator('[data-report-editor]').getByRole('button',{name:'重做',exact:true}).click();
 assert.ok((await reportEditor.innerText()).includes('新增结果'));
 await page.locator('[name="modelId"]').selectOption('81');
 page.once('dialog',dialog=>dialog.dismiss());await page.locator('[data-detail-load-report]').click();
 await page.waitForFunction(()=>document.querySelector('[data-detail-feedback]')?.textContent==='');
 assert.ok((await reportEditor.innerText()).includes('新增结果'));
 // Verify native formatting in our toolbar, without copying legacy toolbar appearance.
 await reportEditor.locator('p').first().click();await page.keyboard.press('End');
 await page.locator('[data-report-editor]').getByRole('button',{name:'加粗',exact:true}).click();await page.keyboard.type('加粗结果');
 assert.ok((await reportEditor.innerHTML()).includes('<strong'));
 await page.locator('[data-detail-load-mail]').click();
 await page.waitForFunction(()=>document.querySelector('[data-mail-editor] .tiptap')?.textContent.includes('模板邮件正文'));
 // Explicit private path; never save patient or production data in tracked files.
 const screenshot=process.env.PETLIS_UI_SCREENSHOT;
 if(screenshot){await mkdir(new URL('.', 'file://'+screenshot),{recursive:true});await page.screenshot({path:screenshot,fullPage:true});}
 await page.getByRole('button',{name:'保存明细',exact:true}).click();
 await page.locator('[data-detail-view="91"]').waitFor();assert.equal(writes.length,1);
 assert.equal(writes[0].testPrice,'180.00');assert.equal(writes[0].mailReport,'<p>模板邮件正文</p>');assert.ok(writes[0].reportResult.includes('新增结果'));assert.ok(writes[0].reportResult.includes('加粗结果'));assert.ok(writes[0].reportResult.includes('colgroup'));
 await page.locator('[data-detail-view="91"]').click();
 await page.getByRole('dialog',{name:'检测明细正文',exact:true}).waitFor();
 await page.frameLocator('[data-detail-preview="report"]').getByText('报告原文新增结果加粗结果',{exact:false}).waitFor();
 await page.getByRole('button',{name:'关闭',exact:true}).last().click();
 await page.locator('[data-order-details="42"]').click();await page.locator('[data-detail-edit="91"]').click();
 await page.waitForFunction(()=>document.querySelector('[data-detail-feedback]')?.textContent==='');
 assert.equal(await page.locator('[name="testPrice"]').inputValue(),'180.00');
 assert.equal(await page.locator('[name="testTypeId"]').inputValue(),'11');assert.equal(await page.locator('[name="modelId"]').inputValue(),'81');
 await page.locator('[name="isCheckPass"]').selectOption('1');
 await page.locator('[data-report-editor] .tiptap p').first().click();await page.keyboard.press('End');await page.keyboard.type('修改后结果');
 await page.getByRole('button',{name:'保存明细',exact:true}).click();await page.locator('[data-detail-view="91"]').waitFor();assert.equal(writes.length,2);assert.ok(saved[0].ReportResult.includes('修改后结果'));
 await page.getByRole('button',{name:'关闭',exact:true}).last().click();
 // Cancel and reopen editors: no stale instance, handler or draft survives.
 await page.locator('[data-order-details="42"]').click();await page.locator('[data-detail-create]').click();
 await page.locator('[data-report-editor] .tiptap').waitFor();await page.keyboard.press('Escape');
 await page.locator('[data-order-details="42"]').click();await page.locator('[data-detail-create]').click();await page.locator('[data-report-editor] .tiptap').waitFor();
 assert.ok(!(await page.locator('[data-report-editor] .tiptap').innerText()).includes('新增结果'));
 await page.keyboard.press('Escape');enabled=false;await page.locator('[data-order-details="42"]').click();assert.equal(await page.locator('[data-detail-create]').count(),0);
 assert.deepEqual(errors,[]);
 console.log(JSON.stringify({syntheticApi:true,databaseWrites:0,pageErrors:errors.length,actualUiCreateSaveReopen:true,actualUiUpdate:true,quoteMoneyString:true,templateReplaceCancel:true,nativeEditUndoRedo:true,toolbarFormatting:true,importedTableRetained:true,inlineReportStyle:true,cancelReopen:true,writeFlagHidesAction:true,backendAcceptance:false}));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
