import {escapeHtml} from '../utils.js';
export async function executeBankRequest(context,row,query,afterChange){
 async function submit(){const receipt=await context.api.request(`/admin/legacy/bank-payments/requests/${encodeURIComponent(row.requestKey)}/${query?'query':'send'}`,{method:'POST'});context.toast(receipt.status==='SUCCEEDED'?'原交易已核对为付款成功':receipt.status==='CASHIER_READY'?'收银台已受理，尚未确认付款':'原请求状态已更新，请核对记录');await afterChange(receipt);}
 if(query)return submit();
 context.dialog({title:'发送原支付单',submitLabel:'确认发送银行',body:`<p>支付单 ${escapeHtml(row.orderId)}，金额 ${escapeHtml(row.amount)} 元。</p><p>确认将这张已保存的支付单发送银行。结果不明时只查询原交易，不重新发送或创建另一张单。</p>`,onSubmit:submit});
}
export function bankCashierLink(row){
 if(row.status!=='CASHIER_READY'||typeof row.cashierUrl!=='string'||row.cashierUrl.length>2000)return '';
 try{const url=new URL(row.cashierUrl);if(url.username||url.password||url.hash||!(url.protocol==='https:'||url.protocol==='http:'&&url.hostname==='127.0.0.1'&&url.port))return '';return `<a class="button text" href="${escapeHtml(url.href)}" target="_blank" rel="noopener noreferrer">打开收银台</a>`;}catch{return '';}
}
