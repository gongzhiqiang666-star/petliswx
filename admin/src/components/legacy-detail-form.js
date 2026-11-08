import {field} from './ui.js';

const base='/admin/legacy/order-details';
const money=value=>{
 const text=String(value??'');
 if(!text)return null;
 if(!/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(text))throw Error('请输入有效的金额，不支持科学计数法');
 return text;
};
export function createDetailCommand(values,bodies,requestKey,editing=false){
 const testTypeId=Number(values.testTypeId);
 if(!Number.isSafeInteger(testTypeId)||testTypeId<1)throw Error('请选择检测项目');
 const detailRemark=String(values.detailRemark??'');
 if(!editing&&testTypeId===1225&&(!detailRemark||detailRemark==='0'))throw Error('此项目备注不能为空，请填写快递单号');
 return {testTypeId,originalPrice:money(values.originalPrice),originalDiscount:String(values.originalDiscount??'10'),testPrice:money(values.testPrice),detailRemark,modelId:Number(values.modelId||0),mailTitle:String(values.mailTitle??''),mailReport:bodies.mail,reportResult:bodies.report,isSendMail:0,isCheckPass:0,payStatus:0,diagnosisStatus:0,requestKey};
}
/** An interrupted POST may have committed. Retry only the identical request and key. */
export function createDetailSession(api,orderId,requestKey,detailId=null){
 let pending=null,result=null,inflight=null;
 return {async save(command){
  const data={...command,requestKey},canonical=JSON.stringify(data);
  if(pending!==null&&pending!==canonical)throw Error('上次提交结果尚需核对，请先用原内容重试；确认是否已新增后再修改');
  if(result!==null)return result;
  if(inflight)return inflight;
  pending=canonical;
  inflight=api.request(`${base}/orders/${orderId}${detailId===null?'':'/'+detailId}`,{method:detailId===null?'POST':'PUT',data}).then(id=>{result=id;return id;}).catch(error=>{
   // Only a definite validation/authorization refusal is safe to edit and resubmit.
   if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))pending=null;
   throw error;
  }).finally(()=>{inflight=null;});
  return inflight;
 }};
}

export async function openLegacyDetailCreate(context,orderId,onSaved,detailId=null){
 const editing=detailId!==null;
 const [capabilities,defaults,editorModule]=await Promise.all([
  context.api.request(base+'/capabilities'),context.api.request(`${base}/orders/${orderId}/${editing?detailId:'form-defaults'}`),
  import('../../assets/legacy-editor.js')
 ]);
 if(capabilities.orderDetails!==true||(editing?capabilities.orderDetailUpdates:capabilities.orderDetailWrites)!==true)throw Error('检测明细维护尚未开启');
 const session=createDetailSession(context.api,orderId,globalThis.crypto.randomUUID(),detailId);
 let editors={},loading=0,selectionVersion=0,requestFailed=false;
 const fieldHtml=(name,label,value='',extra={})=>field({name,label,value,...extra});
 const selectHtml=(name,label)=>fieldHtml(name,label,'',{options:[{value:'',label:'请选择'}]});
 context.dialog({title:editing?'编辑检测明细':'新增检测明细',wide:true,submitLabel:'保存明细',body:`
  <p class="muted">${editing?'保存沿用原 PHP 修改规则：非空发送日期会更新主单报告日期并将主单置为已完成；保存本身不会发送邮件。':'填写检测项目和价格，确认报告及邮件正文后保存。本次新增不会发送邮件或发布报告。'}</p>
  <div class="toolbar">${fieldHtml('testSearch','搜索检测项目')}<button type="button" class="button secondary" data-detail-search>搜索</button><button type="button" class="button secondary" data-detail-tests-more hidden>更多项目</button></div>
  <div class="form-grid">${selectHtml('testTypeId','检测项目')}${selectHtml('priceId','价格档位')}${fieldHtml('originalPrice','原价（元）',defaults.OrginalPrice??'')}${fieldHtml('originalDiscount','折扣',defaults.OrginalDiscount??'10')}${fieldHtml('testPrice','实际价格（元）',defaults.TestPrice??'')}
  <div class="form-field"><span>核价</span><button type="button" class="button secondary" data-detail-quote>按原规则计算实价</button></div>
  ${selectHtml('modelId','报告模板')}${selectHtml('mailTemplateId','邮件模板')}${fieldHtml('mailTitle','邮件标题',defaults.MailTitle??'',{wide:true})}${fieldHtml('detailRemark','备注',defaults.DetailRemark??'',{type:'textarea',wide:true})}
  ${editing?fieldHtml('isSendMail','是否已发送',defaults.IsSendMail??0,{options:[{value:0,label:'未发送'},{value:1,label:'已通过其他方式发送'}]})+fieldHtml('isCheckPass','作废／费用豁免',defaults.IsCheckPass??0,{options:[{value:0,label:'正常计费'},{value:1,label:'作废，不计入账单'}]})+fieldHtml('sendMailDt','发送报告日期',String(defaults.SendMailDt??'').startsWith('1900-01-01')?'':defaults.SendMailDt??'',{placeholder:'YYYY-MM-DD；留空表示尚未发送'}):''}</div>
  <div class="toolbar"><button type="button" class="button secondary" data-detail-load-report>载入报告模板</button><button type="button" class="button secondary" data-detail-load-mail>载入邮件模板</button><button type="button" class="button secondary" data-detail-mail-more hidden>更多邮件模板</button><button type="button" class="button secondary" data-detail-model-more hidden>更多报告模板</button></div>
  <p data-detail-feedback role="status"></p>
  <h3>报告正文</h3><div data-report-editor></div><h3>邮件正文</h3><div data-mail-editor></div>
  <details><summary>保存与审核说明</summary><p>新增明细保留原业务默认标志。发送、审核、付款及检测状态不会因编辑正文而自动设为完成。</p></details>`,
  onReady(form){
   const input=name=>form.elements.namedItem(name),feedback=form.querySelector('[data-detail-feedback]');
   const active=()=>form.isConnected;
   const options=(name,items,valueKey,label,append=false)=>{
    const select=input(name);if(!append)select.replaceChildren(new Option('请选择',''));
    for(const item of items)select.add(new Option(label(item),item[valueKey]));
   };
   const run=async operation=>{loading++;feedback.textContent='正在加载…';try{await operation();if(active()){feedback.textContent='';requestFailed=false;}}catch(error){if(active()){feedback.textContent=error.message;requestFailed=true;}}finally{loading--;}};
   try{
    editors.report=editorModule.mountEditor(form.querySelector('[data-report-editor]'),String(defaults.ReportResult??''));
    editors.mail=editorModule.mountEditor(form.querySelector('[data-mail-editor]'),String(defaults.MailReport??''));
   }catch(error){requestFailed=true;feedback.textContent=error.message;}
   let testsPage=0,testsQuery='',mailPage=0,modelPage=0;
   const loadTests=async(append=false)=>{
    const page=append?testsPage+1:0,q=append?testsQuery:input('testSearch').value;
    const data=await context.api.request(base+'/tests',{query:{q,page,size:100}});if(!active())return;
    const selected=input('testTypeId').value;
    options('testTypeId',data.items,'InfoTestTypeId',r=>`${r.TestNO??''} ${r.TestName??''}`,append);testsPage=page;testsQuery=q;
    if(editing&&!append&&!selected){const value=String(defaults.InfoTestTypeId);if(![...input('testTypeId').options].some(option=>option.value===value))input('testTypeId').add(new Option(`原检测项目 #${value}（保留）`,value));input('testTypeId').value=value;}
    form.querySelector('[data-detail-tests-more]').hidden=(page+1)*100>=data.total;
   };
   const loadMail=async(append=false)=>{const page=append?mailPage+1:0;const data=await context.api.request(base+'/mail-templates',{query:{page,size:100}});if(!active())return;options('mailTemplateId',data.items,'id',r=>r.model_type,append);mailPage=page;if(!append)input('mailTemplateId').value=String(defaults.DictMailDiagnosisReportId??'');form.querySelector('[data-detail-mail-more]').hidden=(page+1)*100>=data.total;};
   const loadModels=async(append=false)=>{
    const testId=input('testTypeId').value,version=selectionVersion,page=append?modelPage+1:0;
    const data=await context.api.request(`${base}/tests/${testId}/templates`,{query:{page,size:100}});if(!active()||version!==selectionVersion)return;
    options('modelId',data.items,'InfoTestTypeModelId',r=>r.ModelName,append);modelPage=page;form.querySelector('[data-detail-model-more]').hidden=(page+1)*100>=data.total;
    if(editing&&version===0&&!append){const value=String(defaults.InfoTestTypeModelId??0);if(value!=='0'&&![...input('modelId').options].some(option=>option.value===value))input('modelId').add(new Option(`原报告模板 #${value}（保留）`,value));input('modelId').value=value;}
   };
   form.addEventListener('change',event=>{
    if(event.target===input('testTypeId')){
     const testId=input('testTypeId').value,version=++selectionVersion;
     options('priceId',[],'IID',()=> '');options('modelId',[],'InfoTestTypeModelId',()=> '');input('originalPrice').value='';input('testPrice').value='';
     if(!testId)return;
     run(async()=>{
      // Prices are business choices, including inactive historical tiers, not floating-point numbers.
      const prices=[];let page=0,total=1;
      while(prices.length<total){const result=await context.api.request(`${base}/tests/${testId}/prices`,{query:{page:page++,size:100}});if(!active()||version!==selectionVersion)return;prices.push(...result.items);total=result.total;if(!result.items.length)break;}
      const title=await context.api.request(`${base}/orders/${orderId}/test-mail-title`,{query:{testId}});if(!active()||version!==selectionVersion)return;
      options('priceId',prices,'IID',r=>`${r.price} 元${r.IsActive===0?'（停用）':''}`);
      input('priceId').onchange=()=>{const price=prices.find(row=>String(row.IID)===input('priceId').value);if(price){input('originalPrice').value=price.price;input('testPrice').value=price.price;}};
      if(prices.length){input('priceId').value=String(prices[0].IID);input('priceId').onchange();}
      input('mailTitle').value=title.title;await loadModels();
     });
    }
   });
   form.addEventListener('click',event=>{
    const button=event.target.closest('[data-detail-search],[data-detail-tests-more],[data-detail-mail-more],[data-detail-model-more],[data-detail-quote],[data-detail-load-report],[data-detail-load-mail]');if(!button||loading)return;
    run(async()=>{
     if(button.hasAttribute('data-detail-search'))return loadTests();
     if(button.hasAttribute('data-detail-tests-more'))return loadTests(true);
     if(button.hasAttribute('data-detail-mail-more'))return loadMail(true);
     if(button.hasAttribute('data-detail-model-more'))return loadModels(true);
     if(button.hasAttribute('data-detail-quote')){const result=await context.api.request(`${base}/orders/${orderId}/quote`,{method:'POST',data:{price:money(input('originalPrice').value),discount:input('originalDiscount').value}});if(active())input('testPrice').value=result.price;return;}
     const kind=button.hasAttribute('data-detail-load-report')?'report':'mail',id=input(kind==='report'?'modelId':'mailTemplateId').value;
     if(!id)throw Error('请先选择模板');
     if(editors[kind].isDirty()&&!globalThis.confirm('载入模板会替换当前正文，是否继续？'))return;
     const version=selectionVersion,before=editors[kind].getHTML();
     const result=await context.api.request(`${base}/orders/${orderId}/render/${kind}/${id}`,{query:{detailId:detailId??0}});
     if(active()&&version===selectionVersion){if(editors[kind].getHTML()!==before)throw Error('加载期间正文已修改，已保留当前内容，请重新载入模板');editors[kind].setHTML(result.html);}
    });
   });
   run(async()=>{await loadTests();await loadMail();if(editing){
    await loadModels();const prices=[];let page=0,total=1;
    while(prices.length<total){const result=await context.api.request(`${base}/tests/${defaults.InfoTestTypeId}/prices`,{query:{page:page++,size:100}});if(!active()||selectionVersion!==0)return;prices.push(...result.items);total=result.total;if(!result.items.length)break;}
    options('priceId',prices,'IID',r=>`${r.price} 元${r.IsActive===0?'（停用）':''}`);
    input('priceId').onchange=()=>{const price=prices.find(row=>String(row.IID)===input('priceId').value);if(price)input('originalPrice').value=price.price;};
   }});
  },
  onClose(){Object.values(editors).forEach(editor=>editor.destroy());},
  async onSubmit(form){
   if(loading)throw Error('请等待加载完成');
   if(requestFailed||!editors.mail||!editors.report)throw Error('页面尚未完整加载，请处理上方提示后再保存');
   const values=Object.fromEntries(new FormData(form));
   let command=createDetailCommand(values,{mail:editors.mail.getHTML(),report:editors.report.getHTML()},undefined,editing);
   if(editing){const {payStatus,diagnosisStatus,...rest}=command;command={...rest,isSendMail:Number(values.isSendMail),isCheckPass:Number(values.isCheckPass),sendMailDt:String(values.sendMailDt??''),expectedToken:defaults.editToken};}
   const id=await session.save(command);if(form.isConnected){context.toast('检测明细已保存');await onSaved?.(id);}
  }
 });
}
