import {escapeHtml} from '../utils.js';
import {field} from './ui.js';
/** attempt belongs to an applied quote, surviving dialog close/reopen and uncertain responses. */
export async function openBankPreparation(context,query,quote,afterSave,attempt){
 const hospitals=[...new Set(quote.items.map(row=>Number(row.InfoHospitalId)))];
 if(!hospitals.length)throw Error('没有可核对的付款项目，请重新核价');
 attempt.requestKey??=crypto.randomUUID();
 const fixed=query.mode==='MONTH'?Number(query.hospital):hospitals.length===1?hospitals[0]:null;
 context.dialog({title:'保存网银支付单',submitLabel:'确认保存支付单',body:`<p>${escapeHtml(quote.description)}</p><p>核对金额 <strong>${escapeHtml(quote.displayAmount??quote.amount)} 元</strong>，共 ${Number(quote.count)} 个项目。</p><p>本次只保存支付单与项目分摊，不会发起扣款。保存成功不代表银行受理或付款成功。</p>${fixed===null?field({name:'hospital',label:'付款医院',required:true,options:[{value:'',label:'请选择付款医院'},...hospitals.map(id=>({value:String(id),label:`医院 ${id}`}))]}):`<p>付款医院编号：${fixed}</p>`}`,async onSubmit(form){
  const hospital=fixed??Number(form.elements.hospital.value);
  if(!Number.isSafeInteger(hospital)||hospital<1||!hospitals.includes(hospital))throw Error('请选择本次项目所属的付款医院');
  const canonical={...query,hospital,displayedAmount:String(quote.displayAmount??quote.amount),requestKey:attempt.requestKey};
  if(attempt.command&&JSON.stringify(attempt.command)!==JSON.stringify(canonical))throw Error('原请求结果尚需核对，请勿更换付款医院后重试');
  attempt.command??=canonical;
  const receipt=await context.api.request('/admin/legacy/bank-payments/requests',{method:'POST',data:attempt.command});attempt.receipt=receipt;
  context.toast('支付单已保存，尚未发送银行');await afterSave(receipt);
 }});
}
