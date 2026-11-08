import {field,renderTable} from './ui.js';
import {escapeHtml} from '../utils.js';
import {formData} from '../pages/shared.js';
import {legacyTimestamp} from './legacy-dictionaries.js';

export function hospitalFundsCommand(values,requestKey){
 const amount=String(values.amount??'').trim(),remark=String(values.remark??'');
 if(!/^-?\d{1,8}(\.\d{1,2})?$/.test(amount)||/^[-0.]+$/.test(amount))throw Error('请填写非零金额，最多两位小数');
 if(!remark.trim()||Array.from(remark).length>255)throw Error('请填写255字以内的备注');
 return {amount,remark,requestKey};
}
export async function openHospitalFunds(context,hospitalId,canWrite=false){
 const base='/admin/legacy/hospital-funds/'+hospitalId;
 const [hospital,data]=await Promise.all([context.api.request(base),context.api.request(base+'/entries',{query:{page:0,size:20}})]);
 const requestKey=globalThis.crypto.randomUUID();
 const table=rows=>renderTable({rows,emptyText:'暂无医院资金流水',columns:[{key:'id',label:'Id'},{key:'HospitalName',label:'医院'},{key:'amount',label:'金额（元）'},{key:'admin_id',label:'管理员ID'},{key:'remark',label:'备注'},{label:'创建时间',render:r=>escapeHtml(legacyTimestamp(r.createtime))}]});
 const body=`<p><strong>医院资金池</strong> · ${escapeHtml(hospital.HospitalName)}</p><p>可用余额：<strong>${escapeHtml(hospital.amount??'待核实')} 元</strong></p><p class="muted">医院账户与医生个人钱包分别记账。流水记录保留，加减款请填写备注。</p><div data-fund-ledger>${table(data.items??[])}</div><div class="toolbar"><span data-fund-page>共 ${Number(data.total)} 条 · 第 1 页</span><button type="button" class="button secondary" data-fund-prev disabled>上一页</button><button type="button" class="button secondary" data-fund-next ${Number(data.total)<=20?'disabled':''}>下一页</button></div>${canWrite?`<div class="form-grid">${field({name:'amount',label:'金额（负数为扣）',type:'number',step:'0.01',required:true})}${field({name:'remark',label:'备注',required:true})}</div>`:''}`;
 const options={title:'医院资金 · '+hospital.HospitalName,body,wide:true,onReady(target){
  let page=0,busy=false;target.addEventListener('click',async event=>{
   const button=event.target.closest('[data-fund-prev],[data-fund-next]');if(!button||button.disabled||busy)return;
   busy=true;const next=page+(button.hasAttribute('data-fund-next')?1:-1);
   try{const result=await context.api.request(base+'/entries',{query:{page:next,size:20}});page=next;target.querySelector('[data-fund-ledger]').innerHTML=table(result.items??[]);target.querySelector('[data-fund-page]').textContent=`共 ${Number(result.total)} 条 · 第 ${page+1} 页`;target.querySelector('[data-fund-prev]').disabled=page===0;target.querySelector('[data-fund-next]').disabled=(page+1)*20>=Number(result.total);}catch(e){context.toast(e.message,'error')}finally{busy=false}
  });
 }};
 if(canWrite){options.submitLabel='确认记账';options.onSubmit=async form=>{await context.api.request(base+'/entries',{method:'POST',data:hospitalFundsCommand(formData(form),requestKey)});context.toast('医院资金已记账');};}
 context.dialog(options);
}
