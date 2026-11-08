import {field} from './ui.js';
export const paymentLogFields=[['BuyMail','卖家账户'],['BuyerId','卖家支付宝 ID'],['TradeNoAli','平台流水号（原标签：报告编号）'],['TradeNoUser','商户订单号（原标签：样本编号）'],['TotalFee','交易金额（元）'],['TradeStatus','交易状态'],['Subject','标题'],['TradeBody','内容'],['Exterface','扩展'],['CreateDt','交易时间']];
export const paymentLogBase='/admin/legacy/payment-logs';
function nowShanghai(){return new Intl.DateTimeFormat('sv-SE',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false}).format(new Date());}
export async function openPaymentLogEditor(context,id,onSaved){
 const row=id==null?{TotalFee:'0',CreateDt:nowShanghai()}:await context.api.request(`${paymentLogBase}/${id}`),requestKey=crypto.randomUUID();let submitted=null;
 context.dialog({title:id==null?'新增交易流水':'编辑交易流水',submitLabel:'保存流水',body:paymentLogFields.map(([name,label])=>field({name,label,value:row[name]??'',required:name==='TotalFee'||name==='CreateDt'})).join('')+'<p class="muted">保存仅维护交易记录，不扣款、不退款、不核销。金额保留正负号，最多输入四位小数，按原数据库精度保存为两位；空文本及原交易状态原样保留。</p>',async onSubmit(form){
  const fields=Object.fromEntries(paymentLogFields.map(([name])=>[name,form.elements[name].value]));const command={fields,requestKey,expectedToken:row.editToken??null},canonical=JSON.stringify(command);if(submitted&&submitted!==canonical)throw Error('上次保存结果未确认，请先用原内容重试或重新打开核对');submitted=canonical;
  try{await context.api.request(id==null?paymentLogBase:`${paymentLogBase}/${id}`,{method:id==null?'POST':'PUT',data:command});}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))submitted=null;throw error;}
  if(form.isConnected){context.toast('交易流水已保存');await onSaved();}
 }});
}
