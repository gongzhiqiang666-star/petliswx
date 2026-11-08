import {pageHeader,renderTable,renderState,pagination} from '../components/ui.js';
import {escapeHtml} from '../utils.js';
import {openHospitalDraftRemark} from '../components/hospital-draft-remark.js';
const base='/orders/hospital-history';
export async function render(container,context){
 const caps=await context.api.request(base+'/capabilities');if(caps.legacyHospitalDrafts!==true){container.innerHTML=renderState('pending');return}
 let hospital=null,page=0,hospitalPage=0,total=0,sequence=0,choiceSequence=0,query='',busy=false;
 container.innerHTML=pageHeader('本院送检草稿','查看未提交的医院草稿，维护送检备注，随后在小程序继续送检')+'<section class="panel"><div class="toolbar"><label>所属医院 <select data-draft-hospital aria-label="所属医院"></select></label><input type="search" data-draft-hospital-search aria-label="搜索医院" placeholder="搜索可访问医院"><button type="button" class="button secondary" data-draft-hospital-find>搜索医院</button><button type="button" class="button text" data-draft-hospital-more hidden>更多医院</button><button type="button" class="button secondary" data-draft-refresh>刷新草稿</button></div><p data-draft-feedback role="status"></p><div data-draft-list></div><div data-draft-pagination></div></section>';
 const select=container.querySelector('[data-draft-hospital]'),list=container.querySelector('[data-draft-list]'),pager=container.querySelector('[data-draft-pagination]'),feedback=container.querySelector('[data-draft-feedback]'),more=container.querySelector('[data-draft-hospital-more]');
 async function load(next=0){if(!hospital)return;const run=++sequence,target=hospital;busy=true;list.innerHTML=renderState('loading');pager.innerHTML='';
  try{const data=await context.api.request(base+'/drafts',{query:{hospitalId:target,page:next,size:20}});if(run!==sequence||!container.isConnected)return;page=next;total=Number(data.total);list.innerHTML=renderTable({columns:[{key:'animalName',label:'动物姓名'},{key:'ownerName',label:'畜主姓名'},{key:'doctorName',label:'送检医师'},{key:'createdAt',label:'创建时间'}],rows:data.items,emptyText:'本院暂无未提交草稿',actions:row=>`<button type="button" class="button text" data-hospital-draft-remark="${Number(row.orderId)}">${caps.legacyHospitalDraftWrites===true?'编辑备注':'查看备注'}</button>`});pager.innerHTML=pagination({page,size:20,total})}
  catch(error){if(run===sequence&&container.isConnected)list.innerHTML=renderState(error.status===403?'forbidden':'error',error)}finally{if(run===sequence)busy=false}
 }
 async function hospitals(append=false){const run=++choiceSequence,next=append?hospitalPage+1:0,q=append?query:container.querySelector('[data-draft-hospital-search]').value;more.disabled=true;
  try{const data=await context.api.request(base+'/hospitals',{query:{q,page:next,size:20}});if(run!==choiceSequence||!container.isConnected)return;const selected=select.value,label=select.selectedOptions[0]?.textContent;
   if(!append){select.innerHTML=selected?`<option value="${escapeHtml(selected)}">${escapeHtml(label)}</option>`:''}
   for(const row of data.items){const value=String(row.id),old=[...select.options].find(o=>o.value===value);if(old)old.textContent=row.name;else select.add(new Option(row.name,value))}
   hospitalPage=next;query=q;more.hidden=(next+1)*20>=Number(data.total);feedback.textContent='';
   if(!hospital){const preferred=String(context.store.getState().user?.hospitalId??'');if([...select.options].some(o=>o.value===preferred))select.value=preferred;hospital=Number(select.value)||null;if(hospital)await load();else list.innerHTML=renderTable({columns:[],rows:[],emptyText:'尚未关联可访问的医院'})}
  }catch(error){if(run===choiceSequence)feedback.textContent=error.message}finally{if(run===choiceSequence)more.disabled=false}
 }
 select.addEventListener('change',()=>{hospital=Number(select.value)||null;load()});container.querySelector('[data-draft-hospital-find]').addEventListener('click',()=>hospitals());more.addEventListener('click',()=>hospitals(true));container.querySelector('[data-draft-refresh]').addEventListener('click',()=>load());
 pager.addEventListener('click',event=>{const b=event.target.closest('[data-page]');if(b&&!busy)load(Number(b.dataset.page))});
 list.addEventListener('click',async event=>{const b=event.target.closest('[data-hospital-draft-remark]');if(!b||busy)return;b.disabled=true;try{await openHospitalDraftRemark(context,hospital,Number(b.dataset.hospitalDraftRemark),()=>load(page))}catch(error){context.toast(error.message,'error')}finally{b.disabled=false}});
 await hospitals();
}
