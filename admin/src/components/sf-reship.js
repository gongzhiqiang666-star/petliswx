import {field} from './ui.js';
import {formData} from '../pages/shared.js';
import {escapeHtml} from '../utils.js';
const names=['contact','mobile','province','city','county','address'];
export function reshipEntry(options){if(!options?.enabled)return '';return options.canReship?'<button type="button" class="button secondary" data-sf-reship>重新寄件</button>':`<p class="form-hint">${escapeHtml(options.reason||'当前暂不能再次寄件')}</p>`;}
export function reshipPayload(values,previous){
 const reason=String(values.reason||'').trim();if(!reason || reason.length>500)throw Error('请填写1至500字再次寄件原因');
 const read=prefix=>{const result=Object.fromEntries(names.map(key=>[key,String(values[prefix+'_'+key]||'').trim()]));if(names.some(key=>!result[key])||!/^1[3-9][0-9]{9}$/.test(result.mobile))throw Error('请填写完整寄收件资料及有效手机号');return result};
 return {previousCreateRequestId:previous,reason,sender:read('sender'),recipient:read('recipient')};
}
export function openSfReship(context,orderId,options,onSaved){
 if(!options?.enabled || !options.canReship || !Number.isSafeInteger(Number(options.previousCreateRequestId)) || Number(options.previousCreateRequestId)<=0)throw Error('当前不可再次寄件，请刷新请求状态');
 const previous=Number(options.previousCreateRequestId),next=Number(options.nextAttemptNo);
 const contacts=(prefix,title)=>`<h3 class="wide">${title}</h3>`+names.map((name,index)=>field({name:prefix+'_'+name,label:['联系人','手机号','省','市','区县','详细地址'][index],required:true,wide:name==='address',type:name==='mobile'?'tel':'text'})).join('');
 context.dialog({title:`送检单 #${orderId} · 第${next}次寄件`,wide:true,submitLabel:'提交沙箱再次寄件',body:`<p class="form-hint">前次运单和面单历史会保留，原送检单与金额不变。请重新核对寄收件资料；沙箱不会预约真实取件。结果未知时请先查询本单结果。</p><div class="form-grid">${field({name:'reason',label:'再次寄件原因（最多500字）',required:true,type:'textarea',wide:true})}${contacts('sender','寄件信息')}${contacts('recipient','收件实验室')}</div><div class="toolbar"><button type="button" class="button secondary" data-sf-reship-check>查询本单结果</button><button type="button" class="button secondary" data-sf-reship-reconcile hidden>查单并恢复本地运单</button></div><p class="form-hint" data-sf-reship-state role="status"></p>`,onReady(form){
  const check=form.querySelector('[data-sf-reship-check]'),recover=form.querySelector('[data-sf-reship-reconcile]'),state=form.querySelector('[data-sf-reship-state]');
  const show=result=>{const status=result.cancellationState==='SUCCEEDED'?'已确认取消':({RESERVED:'请求已记录',SENDING:'结果待确认',UNKNOWN:'结果待确认',SUCCEEDED:'寄件已确认',REJECTED:'承运商未受理'})[result.state]||result.state;state.textContent=`第${result.attemptNo||1}次寄件：${status}${result.trackingNo?' · '+result.trackingNo:''}`;recover.hidden=!(Number(result.attemptNo)===next&&!result.shipmentId&&['SENDING','UNKNOWN','SUCCEEDED'].includes(result.state));recover.style.display=recover.hidden?'none':'';};
  recover.style.display='none';check.addEventListener('click',async()=>{check.disabled=true;try{show(await context.api.request(`/logistics/sf/orders/${orderId}`))}catch(error){state.textContent=error.message}finally{check.disabled=false}});
  recover.addEventListener('click',async()=>{recover.disabled=true;try{show(await context.api.request(`/logistics/sf/orders/${orderId}/reconcile`,{method:'POST'}));context.toast('已查询原请求，未重新下单');await onSaved()}catch(error){state.textContent=error.message}finally{recover.disabled=false}});
 },async onSubmit(form){const data=reshipPayload(formData(form),previous),result=await context.api.request(`/logistics/sf/orders/${orderId}/reship`,{method:'POST',data});context.toast(result.shipmentId?'再次寄件已确认，原历史保留':'请求已记录，请查询原请求结果');await onSaved();}});
}
