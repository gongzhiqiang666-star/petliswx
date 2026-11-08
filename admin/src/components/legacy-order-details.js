import {renderTable} from './ui.js';
import {escapeHtml} from '../utils.js';
import {openLegacyDetailCreate} from './legacy-detail-form.js';
const base='/admin/legacy/order-details/orders/';
export function detailBodyHtml(row){
 return `<dl><dt>邮件标题</dt><dd>${escapeHtml(row.MailTitle??'')}</dd><dt>备注</dt><dd>${escapeHtml(row.DetailRemark??'')}</dd></dl><p class="muted">以下为已保存正文。外部图片在本机预览中不加载，图片地址仍保留在原文中。</p><h3>报告正文</h3><iframe class="legacy-body-preview" data-detail-preview="report" title="已保存报告正文" sandbox referrerpolicy="no-referrer"></iframe><h3>邮件正文</h3><iframe class="legacy-body-preview" data-detail-preview="mail" title="已保存邮件正文" sandbox referrerpolicy="no-referrer"></iframe><details><summary>邮件正文源文</summary><pre>${escapeHtml(row.MailReport??'')}</pre></details><details><summary>报告正文源文</summary><pre>${escapeHtml(row.ReportResult??'')}</pre></details>`;
}
export function mountDetailBodyPreviews(target,row){
 for(const [kind,key] of [['report','ReportResult'],['mail','MailReport']]){
  const frame=target.querySelector(`[data-detail-preview="${kind}"]`);
  frame.srcdoc=`<!doctype html><html><head><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; img-src data:; form-action 'none'; base-uri 'none'"></head><body>${String(row[key]??'')}</body></html>`;
 }
}
export async function openLegacyOrderDetails(context,orderId){
 let page=0,busy=false;
 const fetchPage=p=>context.api.request(base+orderId,{query:{page:p,size:20}});
 const table=data=>renderTable({rows:data.items??[],emptyText:'暂无检测明细',columns:[{label:'明细编号',key:'GenterNO'},{label:'检测项目',key:'TestName'},{label:'检测编号',key:'TestNO'},{label:'原价（元）',key:'OrginalPrice'},{label:'折扣',key:'OrginalDiscount'},{label:'实际价格（元）',key:'TestPrice'},{label:'发送标志',key:'IsSendMail'},{label:'作废／费用豁免标志',key:'IsCheckPass'},{label:'支付状态值',key:'PayStatus2'},{label:'检测状态值',key:'DiagnosisStatus'},{label:'备注',key:'DetailRemark'}],actions:r=>`${capabilities.orderDetailUpdates===true?`<button type="button" class="button text" data-detail-edit="${Number(r.DiagnosisDetailId)}">编辑</button>`:''}<button type="button" class="button text" data-detail-view="${Number(r.DiagnosisDetailId)}">查看正文</button>`});
 const [data,capabilities]=await Promise.all([fetchPage(0),context.api.request('/admin/legacy/order-details/capabilities')]);
 context.dialog({title:'送检检测明细',wide:true,body:`${capabilities.orderDetailWrites===true?'<div class="toolbar"><button type="button" class="button" data-detail-create>新增检测明细</button></div>':''}<div data-details-list>${table(data)}</div><div class="toolbar"><span data-details-page>共 ${Number(data.total)} 条 · 第 1 页</span><button type="button" class="button secondary" data-detail-prev disabled>上一页</button><button type="button" class="button secondary" data-detail-next ${Number(data.total)<=20?'disabled':''}>下一页</button></div>`,onReady(target){
  target.addEventListener('click',async event=>{
   const button=event.target.closest('[data-detail-create],[data-detail-edit],[data-detail-view],[data-detail-prev],[data-detail-next]');if(!button||button.disabled||busy)return;busy=true;
   try{if(button.hasAttribute('data-detail-create')){
    await openLegacyDetailCreate(context,orderId,async()=>openLegacyOrderDetails(context,orderId));
   }else if(button.hasAttribute('data-detail-edit')){
    await openLegacyDetailCreate(context,orderId,async()=>openLegacyOrderDetails(context,orderId),Number(button.dataset.detailEdit));
   }else if(button.hasAttribute('data-detail-view')){
    const row=await context.api.request(base+orderId+'/'+Number(button.dataset.detailView));context.dialog({title:'检测明细正文',wide:true,body:detailBodyHtml(row),onReady:target=>mountDetailBodyPreviews(target,row)});
   }else{const next=page+(button.hasAttribute('data-detail-next')?1:-1);const result=await fetchPage(next);page=next;target.querySelector('[data-details-list]').innerHTML=table(result);target.querySelector('[data-details-page]').textContent=`共 ${Number(result.total)} 条 · 第 ${page+1} 页`;target.querySelector('[data-detail-prev]').disabled=page===0;target.querySelector('[data-detail-next]').disabled=(page+1)*20>=Number(result.total);}
   }catch(error){context.toast(error.message,'error');}finally{busy=false;button.disabled=false;const previous=target.querySelector('[data-detail-prev]');if(previous)previous.disabled=page===0;}
  });
 }});
}
