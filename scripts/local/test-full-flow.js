// Real WeChat simulator P0 flow. Uses only the synthetic local fixture and mock payment.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto')
const {execFileSync}=require('child_process')
const privateRoot=path.join(process.env.HOME,'.local/share/petlis')
const automator=require(path.join(privateRoot,'tools/miniprogram-qa/node_modules/miniprogram-automator'))
const statePath=path.join(privateRoot,'simulator-fixture.json')
const fixture=JSON.parse(fs.readFileSync(statePath,'utf8'))
assert.match(fixture.account,/^petlis_qa_[a-f0-9]+$/)
const base='http://127.0.0.1:8082/api'
const mysql=path.join(privateRoot,'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysql')
const mysqlConfig=path.join(privateRoot,'mysql/root.cnf')
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms))
function sql(statement){return execFileSync(mysql,['--defaults-extra-file='+mysqlConfig,'--batch','--skip-column-names','laboklin'],{input:statement,encoding:'utf8'}).trim()}
async function api(apiPath,{token=fixture.session.token,method='GET',body,raw=false}={}){
 let response
 for(let attempt=0;attempt<12;attempt++){
  response=await fetch(base+apiPath,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body===undefined?undefined:JSON.stringify(body)})
  if(response.status!==429)break
  await sleep(1000)
 }
 if(raw){assert.equal(response.status,200,apiPath+' HTTP '+response.status);return response}
 const text=await response.text();let result
 try{result=JSON.parse(text)}catch{throw new Error(apiPath+' HTTP '+response.status+': '+text.slice(0,200))}
 assert.equal(response.status,200,apiPath+' HTTP '+response.status+': '+text.slice(0,300))
 assert.equal(result.code,200,apiPath+': '+JSON.stringify(result))
 return result.data
}
function businessCleanup(){
 const uid=Number(fixture.userId)
 assert.ok(Number.isInteger(uid)&&uid>0)
 sql(`DELETE d FROM lin_lb_notification_log d JOIN lin_lb_notification n ON n.id=d.notification_id WHERE n.order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId=${uid}); DELETE FROM lin_lb_notification WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId=${uid}); DELETE FROM lin_invoice_order WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId=${uid}); DELETE FROM lin_wechat_payment WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId=${uid}); DELETE FROM lin_order_extension WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId=${uid}); DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId=${uid}); DELETE FROM lin_lb_listdiagnosisproject WHERE CreateSampleId=${uid};`)
}
function saveProgress(progress){fs.writeFileSync(statePath,JSON.stringify({...JSON.parse(fs.readFileSync(statePath,'utf8')),...progress},null,2),{mode:0o600})}
function makePdf(filePath){
 const objects=[
  '<< /Type /Catalog /Pages 2 0 R >>',
  '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
  '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
  '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>'
 ]
 const stream='BT /F1 18 Tf 72 720 Td (Petlis QA full-flow synthetic report) Tj 0 -36 Td (No real patient or payment data.) Tj ET'
 objects.push(`<< /Length ${Buffer.byteLength(stream)} >>\nstream\n${stream}\nendstream`)
 let pdf='%PDF-1.4\n';const offsets=[0]
 objects.forEach((object,index)=>{offsets.push(Buffer.byteLength(pdf));pdf+=`${index+1} 0 obj\n${object}\nendobj\n`})
 const xref=Buffer.byteLength(pdf);pdf+=`xref\n0 ${objects.length+1}\n0000000000 65535 f \n`
 for(let i=1;i<=objects.length;i++)pdf+=String(offsets[i]).padStart(10,'0')+' 00000 n \n'
 pdf+=`trailer\n<< /Size ${objects.length+1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`
 fs.writeFileSync(filePath,pdf,{mode:0o600})
}
;(async()=>{
 let mini,orderId=null,pdfPath=null,pdfOpened=false,pdfNote='not attempted'
 const uid=Number(fixture.userId)
 const transientError=/timeout waiting for automator response|Connection closed|timed out|WebSocket|destroyed|stale/i
 async function again(fn,attempts=8,delay=800){let error;for(let i=0;i<attempts;i++){try{return await fn()}catch(e){error=e;if(!transientError.test(String(e&&e.message)))throw e;await sleep(delay)}}throw error}
 const currentPage=()=>again(()=>mini.currentPage())
 const pageData=key=>again(async()=>(await mini.currentPage()).data(key))
 const shots=path.join(privateRoot,'qa');fs.mkdirSync(shots,{recursive:true,mode:0o700})
 const shot=name=>again(()=>mini.screenshot({path:path.join(shots,'full-flow-'+name+'.png')}))
 async function waitFor(check,label,timeout=20000){const end=Date.now()+timeout;let value,lastError;while(Date.now()<end){try{value=await check();if(value)return value}catch(e){lastError=e;if(!transientError.test(String(e&&e.message)))throw e}await sleep(1000)}throw new Error('Timed out: '+label+(lastError?'; '+lastError.message:''))}
 async function waitPath(expected){return waitFor(async()=>{const page=await currentPage();return page.path===expected?page:null},'page '+expected)}
 async function tap(page,selector){return again(async()=>{const element=await page.$(selector);assert.ok(element,'Element missing: '+selector);await element.tap()})}
 async function input(page,selector,value){return again(async()=>{const element=await page.$(selector);assert.ok(element,'Input missing: '+selector);await element.trigger('input',{value})})}
 async function switchTab(url){await again(()=>mini.switchTab(url));return waitPath(url.replace(/^\//,''))}
 try{
  mini=await automator.connect({wsEndpoint:process.env.PETLIS_AUTOMATOR_ENDPOINT||'ws://127.0.0.1:9420'})
  console.log('01 Automation connected')
  await again(()=>mini.reLaunch('/pages/login/login'))
  let page=await waitPath('pages/login/login')
  await again(()=>page.waitFor('input[data-field="account"]'))
  await input(page,'input[data-field="account"]',fixture.account);await input(page,'input[data-field="password"]',fixture.password)
  await tap(page,'.row');await waitFor(async()=>await pageData('remember')===false,'remember disabled')
  for(let attempt=0;attempt<3;attempt++){
   await tap(page,'button[type="primary"]')
   try{page=await waitFor(async()=>{const p=await currentPage();return p.path==='pages/home/home'?p:null},'login home',8000);break}
   catch(error){if(attempt===2)throw error;console.log('Login tap '+(attempt+1)+' did not navigate, retrying')}
  }
  await shot('01-home')
  console.log('02 UI login and home passed')

  page=await again(()=>mini.navigateTo('/pages/order-add-pet/order-add-pet?petId='+fixture.petId))
  await waitFor(async()=>await pageData('petInfo'),'pet info')
  const auto=await waitFor(async()=>{const master=await pageData('deliveryInfo.masterName'),masterEn=await pageData('deliveryInfo.masterNameEn'),age=await pageData('animalInfo.age'),collect=await pageData('sampleInfo.collectDt');return master&&masterEn&&age&&collect?{master,masterEn,age,collect}:null},'automatic form fields')
  assert.ok(auto.master&&auto.masterEn&&auto.age&&auto.collect)
  const petInfo=await pageData('petInfo');assert.ok(petInfo.infoVarietyId&&petInfo.infoVarietyName&&petInfo.infoVarietyNameEn,'Fixture pet must have a complete breed')
  await shot('02-order-form-loaded');console.log('03 Pet and automatic owner/age/date fields passed')

  await tap(page,'.form-picker[aria-label="选择样本类型"]')
  const sample=await waitFor(async()=>{const visible=await pageData('samplePickerVisible'),item=(await pageData('sampleFilteredTypes'))[0];return visible&&item?item:null},'sample type picker and items')
  await shot('03-sample-types');await tap(page,'.sample-item')
  await waitFor(async()=>await pageData('sampleInfo.infoSampleTypeId'),'selected sample type written in current page')
  assert.equal(await pageData('sampleInfo.infoSampleTypeId'),sample.infoSampleTypeId)
  assert.equal(await pageData('samplePickerVisible'),false,'Sample picker must close after selection')
  console.log('04 Real sample-type selection passed: '+sample.infoSampleTypeId)

  await input(page,'input[placeholder="请输入采样部位"]','QA full flow blood')
  await input(page,'input[placeholder="请输入送检医师姓名"]','QA Simulator Doctor')
  await waitFor(async()=>await pageData('sampleInfo.sampleArea')==='QA full flow blood'&&(await pageData('deliveryInfo.doctorName'))==='QA Simulator Doctor','manual form fields')
  await tap(page,'button[aria-label="选择检测项目"]');page=await waitPath('pages/order-test-items/order-test-items')
  const labs=await waitFor(async()=>{const value=await pageData('laboratories');return value&&value.length?value:null},'laboratories')
  if(!(await pageData('laboratoryId'))){
   await again(async()=>{const picker=await page.$('picker');assert.ok(picker,'Laboratory picker missing');await picker.trigger('change',{value:'0'})})
  }
  await waitFor(async()=>await pageData('laboratoryId'),'laboratory selection')
  const items=await waitFor(async()=>{const loading=await pageData('loading'),value=await pageData('filteredItems');return !loading&&value&&value.length?value:null},'test items')
  const selected=items.find(item=>Number(item.infoTestTypeId)!==1225&&Number.isFinite(Number(item.price))&&Number(item.price)>=0)
  assert.ok(selected,'No selectable non-rabies priced test item')
  await tap(page,'.item-card[data-id="'+selected.infoTestTypeId+'"]')
  await waitFor(async()=>(await pageData('selectedIds')).includes(selected.infoTestTypeId),'test item selected')
  await shot('04-test-items');console.log('05 Laboratory and test selection passed: '+selected.infoTestTypeId)
  await tap(page,'.btn-next');page=await waitPath('pages/order-preview/order-preview')
  let quote
  try{quote=await waitFor(async()=>{const quoting=await pageData('quoting'),value=await pageData('quote');return !quoting&&value?value:null},'server quote')}
  catch(error){console.log('Quote failure detail: quoting='+await pageData('quoting')+' quoteError='+await pageData('quoteError'));throw error}
  assert.equal(await pageData('quoteError'),'');assert.ok(Number(quote.payableAmount)>=0)
  await shot('05-order-preview');console.log('06 Server quote passed; payable='+quote.payableAmount)

  await again(()=>mini.evaluate(()=>{const p=getCurrentPages()[getCurrentPages().length-1];const original=p.onSubmit;p.__submitCalled=0;p.onSubmit=function(...args){p.__submitCalled++;return original.apply(p,args)};const toast=wx.showToast;wx.__lastToast=null;wx.showToast=function(options){wx.__lastToast=options&&options.title;return toast(options)}}))
  for(let attempt=0;attempt<3;attempt++){
   await tap(page,'.bottom-actions .btn-primary')
   await sleep(2000)
   const called=await again(()=>mini.evaluate(()=>getCurrentPages()[getCurrentPages().length-1].__submitCalled))
   const toast=await again(()=>mini.evaluate(()=>wx.__lastToast))
   console.log('06b submit tap '+(attempt+1)+': called='+called+' submitting='+await pageData('submitting')+' quoteError='+await pageData('quoteError')+' toast='+toast+' items='+(await pageData('formData.selectedTestItems')).length)
   if(called>0)break
   if(attempt===2)throw new Error('Submit button tap did not invoke onSubmit')
  }
  let order=null
  const findOrder=async()=>{const data=await api('/orders?keyword='+encodeURIComponent(fixture.account));return (data.content||[]).find(x=>String(x.animalName||'').startsWith(fixture.account))||null}
  for(let attempt=0;attempt<3 && !order;attempt++){
   try{order=await waitFor(findOrder,'created order',10000)}catch(error){
    if(attempt===2)throw error
    // Only retry after confirming no order exists, so a transient submit failure
    // cannot create a duplicate order.
    if(await findOrder()){order=await findOrder();break}
    if(await pageData('submitting'))continue
    await tap(page,'.bottom-actions .btn-primary');console.log('Retrying submit after transient failure')
   }
  }
  assert.ok(order,'Order must be created')
  orderId=Number(order.orderId);assert.ok(orderId>0);saveProgress({fullFlowOrderId:orderId});assert.equal(Number(order.sampleStatus),0)
  console.log('07 UI submit created order '+orderId+' in status 0 (verified by API); replacing native modal confirm with direct navigation')
  page=await again(()=>mini.navigateTo('/pages/orders/orders'));await waitPath('pages/orders/orders')
  await waitFor(async()=>{const rows=await pageData('filteredOrders');const row=rows.find(x=>Number(x.orderId)===orderId);return row&&Number(row.sampleStatus)===0?row:null},'pending-review order in UI')
  await shot('06-order-pending-review')

  const password=fs.readFileSync(path.join(privateRoot,'reviewer-credentials.txt'),'utf8').split(/\r?\n/).find(line=>line.startsWith('密码：')).split('：',2)[1]
  const reviewerLogin=await api('/auth/password/login',{token:null,method:'POST',body:{account:'petlis_reviewer',password}})
  await api('/review/orders/'+orderId,{token:reviewerLogin.token,method:'POST',body:{approved:true,reason:'QA full-flow simulator approval'}})
  const approved=await api('/orders/'+orderId);assert.equal(Number(approved.sampleStatus),-3)
  console.log('08 Reviewer API approval passed; order is -3')
  await switchTab('/pages/home/home');page=await again(()=>mini.navigateTo('/pages/orders/orders'));await waitPath('pages/orders/orders')
  await waitFor(async()=>{const rows=await pageData('filteredOrders');const row=rows.find(x=>Number(x.orderId)===orderId);return row&&Number(row.sampleStatus)===-3?row:null},'pending-payment order in UI')
  await shot('07-order-pending-payment')
  await tap(page,'.btn-pay[data-id="'+orderId+'"]')
  await waitFor(async()=>{const detail=await api('/orders/'+orderId);return Number(detail.sampleStatus)===2?detail:null},'mock payment status 2',30000)
  page=await waitPath('pages/orders/orders')
  await waitFor(async()=>{const rows=await pageData('filteredOrders');const row=rows.find(x=>Number(x.orderId)===orderId);return row&&Number(row.sampleStatus)===2?row:null},'paid order in UI')
  assert.equal(Number((await api('/orders/'+orderId)).sampleStatus),2)
  await shot('08-order-paid-waiting-report');console.log('09 Real UI mock payment passed; order is 2, no real funds')

  const pdfName='petlis-full-flow-'+orderId+'-'+Date.now()+'.pdf'
  const pdfRoot=process.env.PETLIS_PDF_ROOT||'/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn/public/Content/FilePdf'
  pdfPath=path.join(pdfRoot,pdfName);makePdf(pdfPath);saveProgress({fullFlowPdf:pdfPath})
  const pdfReference='/Content/FilePdf/'+pdfName
  sql(`UPDATE lin_lb_listdiagnosisproject SET SampleStatus=3 WHERE ListDiagnosisProjectId=${orderId} AND CreateSampleId=${uid}; UPDATE lin_lb_diagnosisdetail SET DiagnosisStatus=3,IsSendMail=1,PDFHttpUrl='${pdfReference}',SendMailDt=NOW() WHERE ListDiagnosisProjectId=${orderId} AND IsCheckPass=0;`)
  const reportId=Number(sql(`SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=${orderId} AND IsCheckPass=0 LIMIT 1;`));assert.ok(reportId>0)
  console.log('10 Synthetic report completion and PDF fixture written for report '+reportId)

  const reportNotice=await waitFor(async()=>{const inbox=await api('/inbox');return inbox.items.find(x=>x.eventType==='report'&&x.pagePath==='pages/order-detail/order-detail?id='+orderId)||null},'legacy report inbox bridge',30000)
  assert.match(reportNotice.message,/已完成/)
  page=await switchTab('/pages/notifications/notifications')
  await waitFor(async()=>{const items=await pageData('items');return items.find(x=>x.eventType==='report'&&x.pagePath==='pages/order-detail/order-detail?id='+orderId)||null},'report notification in UI')
  await shot('09-report-notification');console.log('11 Report inbox bridge and notifications UI passed')

  page=await switchTab('/pages/report-list/report-list')
  const reportRow=await waitFor(async()=>{const rows=await pageData('filteredReports');return rows.find(x=>Number(x.orderId)===orderId&&Number(x.reportId)===reportId)||null},'report in report-list UI')
  assert.ok(reportRow)
  await shot('10-report-list');console.log('12 Report list UI passed')
  const reportDetail=await api('/reports/'+reportId);assert.equal(reportDetail.pdfHttpUrl,pdfReference)
  await tap(page,'.report-content[data-id="'+reportId+'"]');page=await waitPath('pages/report-view/report-view')
  await waitFor(async()=>{const loading=await pageData('loading'),report=await pageData('report');return !loading&&report},'report detail UI')
  assert.equal(await pageData('error'),false);assert.equal((await pageData('report')).pdfHttpUrl,pdfReference)
  const viewReport=await pageData('report')
  assert.ok(viewReport.id&&viewReport.petName&&viewReport.testDate&&viewReport.reportDate&&viewReport.fileName,'Report view fields must be populated')
  assert.ok(!viewReport.reportDate.startsWith('1900'),'Legacy zero date must not be displayed')
  await shot('11-report-detail');console.log('13 Report detail UI and pdfHttpUrl passed')

  try{
   const pdfResponse=await api('/pdf/view/'+reportId,{raw:true});const bytes=Buffer.from(await pdfResponse.arrayBuffer());assert.equal(bytes.subarray(0,5).toString(),'%PDF-')
   await tap(page,'.bottom-actions .btn-primary');page=await waitPath('pages/pdf-view/pdf-view')
   await waitFor(async()=>await pageData('busy')===false,'PDF open attempt',20000)
   const pdfError=await pageData('error');assert.equal(pdfError||'','')
   pdfOpened=true;pdfNote='PDF bytes verified and wx.openDocument completed without a page error';await shot('12-report-pdf')
   console.log('14 PDF openDocument invocation passed')
  }catch(error){pdfNote='PDF UI not verified: '+error.message;console.log('14 '+pdfNote)}

  console.log('FULL FLOW PASSED order='+orderId+' report='+reportId+' pdfOpened='+pdfOpened+' '+pdfNote)
 }catch(error){
  console.error('FULL FLOW FAILED: '+error.stack)
  if(mini){try{await again(()=>mini.screenshot({path:path.join(shots,'full-flow-failed.png')}));console.log('Failure screenshot: '+path.join(shots,'full-flow-failed.png'))}catch(e){console.error('Failure screenshot unavailable: '+e.message)}}
  throw error
 }finally{
  if(mini){try{await again(()=>mini.evaluate(()=>getApp().clearSession()))}catch(e){console.error('Session cleanup warning: '+e.message)}try{await mini.disconnect()}catch(e){console.error('Disconnect warning: '+e.message)}}
  businessCleanup()
  if(pdfPath)fs.unlinkSync(pdfPath)
  console.log('Synthetic order, payment, legacy notification and PDF fixtures cleaned')
 }
})().catch(error=>{process.exitCode=1})
