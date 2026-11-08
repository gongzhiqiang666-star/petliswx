import {pageHeader,renderTable,renderState} from '../components/ui.js';
import {escapeHtml} from '../utils.js';
import {paymentLogBase as base,paymentLogFields,openPaymentLogEditor} from '../components/legacy-payment-logs.js';
export async function render(container,context){
 const caps=await context.api.request(base+'/capabilities');let page=0,sequence=0;
 container.innerHTML=pageHeader('网银交易流水','保留原支付交易信息，支持追溯与维护',caps.paymentLogWrites?'<button class="button" data-paylog-new>新增交易流水</button>':'')+'<section class="panel"><p class="muted">这里维护原交易记录，不执行收款、退款或核销。相同交易号可能对应多条记录，不自动合并或删除。</p><div class="toolbar"><label>平台流水号、商户订单号、账户 ID 或标题 <input type="search" data-paylog-search></label><button class="button secondary" data-paylog-query>查询</button></div><div class="payment-log-list" data-paylog-list></div></section>';
 const list=container.querySelector('[data-paylog-list]');
 async function load(){const current=++sequence;list.innerHTML=renderState('loading');try{const d=await context.api.request(base,{query:{q:container.querySelector('[data-paylog-search]').value,page,size:20}});if(current!==sequence||!container.isConnected)return;
  list.innerHTML=renderTable({rows:d.items??[],columns:[{label:'编号',key:'PayLogId'},...paymentLogFields.map(([key,label])=>({key,label}))],actions:r=>`${caps.paymentLogWrites?`<button class="button text" data-paylog-edit="${r.PayLogId}">编辑</button>`:''}${caps.paymentLogDeletes?`<button class="button text" data-paylog-delete="${r.PayLogId}">删除</button>`:''}`})+`<div class="toolbar"><span>共 ${Number(d.total)} 条 · 第 ${page+1} 页</span><button class="button secondary" data-paylog-prev ${page===0?'disabled':''}>上一页</button><button class="button secondary" data-paylog-next ${(page+1)*20>=Number(d.total)?'disabled':''}>下一页</button></div>`;
 }catch(error){if(current===sequence&&container.isConnected)list.innerHTML=renderState('error',error);}}
 container.addEventListener('click',async event=>{const button=event.target.closest('[data-paylog-new],[data-paylog-edit],[data-paylog-delete],[data-paylog-query],[data-paylog-prev],[data-paylog-next],[data-action=retry]');if(!button||button.disabled)return;button.disabled=true;try{
  if(button.hasAttribute('data-paylog-new')||button.hasAttribute('data-paylog-edit'))await openPaymentLogEditor(context,button.dataset.paylogEdit?Number(button.dataset.paylogEdit):null,load);
  else if(button.hasAttribute('data-paylog-delete')){const id=Number(button.dataset.paylogDelete),row=await context.api.request(`${base}/${id}`),requestKey=crypto.randomUUID();context.dialog({title:'删除交易流水',submitLabel:'确认删除流水',body:`<p>确认永久删除第 ${id} 条流水「${escapeHtml(row.TradeNoAli)}」？删除记录不会撤销支付、退回资金或改变订单状态。</p>`,async onSubmit(){await context.api.request(`${base}/${id}/delete`,{method:'POST',data:{requestKey,expectedToken:row.editToken}});context.toast('流水已删除');await load();}});}
  else{if(button.hasAttribute('data-paylog-prev'))page--;else if(button.hasAttribute('data-paylog-next'))page++;else page=0;await load();}
 }catch(error){context.toast(error.message,'error');}finally{button.disabled=false;}});await load();
}
