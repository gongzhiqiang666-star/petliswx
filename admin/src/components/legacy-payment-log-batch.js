import {field,renderTable} from './ui.js';
import {paymentLogBase as base,paymentLogFields} from './legacy-payment-logs.js';
export async function openPaymentLogBatch(context,ids,operation,onSaved){
 if(!Array.isArray(ids)||!ids.length||ids.length>100||new Set(ids).size!==ids.length||!ids.every(id=>Number.isInteger(id)&&id>0)||!['UPDATE','DELETE'].includes(operation))throw Error('请勾选1至100条不同的流水');
 const rows=await Promise.all(ids.map(id=>context.api.request(`${base}/${id}`))),targets=rows.map(r=>({id:r.PayLogId,expectedToken:r.editToken})),requestKey=crypto.randomUUID(),deleting=operation==='DELETE';let submitted=null;
 const table=(name)=>renderTable({rows:rows.map(r=>({...r,fieldValue:name?r[name]:r.TradeStatus})),columns:[{key:'PayLogId',label:'编号'},{key:'TradeNoAli',label:'平台流水号'},{key:'TotalFee',label:'交易金额（元）'},{key:'fieldValue',label:name?paymentLogFields.find(([key])=>key===name)?.[1]??'当前值':'当前状态'}]});
 context.dialog({title:deleting?'批量删除交易流水':'批量修改交易流水',wide:true,submitLabel:deleting?'确认批量删除':'确认批量修改',body:`<p>${deleting?`将永久删除以下 ${rows.length} 条交易流水。删除不撤销支付、不退回资金，也不改变订单状态。`:`将修改以下 ${rows.length} 条流水的同一个字段，其他字段保持原值。不执行收款、退款或财务核销。`}</p>${deleting?'':`<div class="form-grid">${field({name:'batchField',label:'要修改的字段',required:true,options:[{value:'',label:'请选择字段'},...paymentLogFields.map(([value,label])=>({value,label}))]})}${field({name:'batchValue',label:'统一新值（留空表示清空文本）'})}</div><p class="form-hint">金额最多四位小数并按原精度保留两位；交易时间使用“2020-02-29 13:14:15”。</p>`}<div data-paylog-batch-preview>${table()}</div>`,onReady(form){if(!deleting)form.elements.batchField.addEventListener('change',()=>{form.querySelector('[data-paylog-batch-preview]').innerHTML=table(form.elements.batchField.value);});},async onSubmit(form){
  const fields=deleting?{}:{[form.elements.batchField.value]:form.elements.batchValue.value};if(!deleting&&!paymentLogFields.some(([name])=>name===form.elements.batchField.value))throw Error('请选择需要修改的字段');
  const command={operation,targets,fields,requestKey},canonical=JSON.stringify(command);if(submitted&&submitted!==canonical)throw Error('上次批量结果未确认，请先使用原内容重试或关闭后核对');submitted=canonical;
  try{await context.api.request(base+'/batch',{method:'POST',data:command});}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))submitted=null;throw error;}
  if(form.isConnected){context.toast(deleting?`已删除 ${rows.length} 条流水`:`已修改 ${rows.length} 条流水`);await onSaved();}
 }});
}
