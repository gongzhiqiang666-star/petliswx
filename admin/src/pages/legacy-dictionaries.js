import {escapeHtml,pageItems} from '../utils.js';
import {field,pageHeader,renderTable,renderState} from '../components/ui.js';
import {formData} from './shared.js';
import {dictionaryQuery,legacyTimestamp,createDictionaryReader} from '../components/legacy-dictionaries.js';
import {openDictionaryEditor} from '../components/legacy-dictionary-editor.js';
import {openDictionaryBatch} from '../components/legacy-dictionary-batch.js';

export async function render(container,context){
 const schemas=await context.api.request('/admin/legacy/dictionaries');
 if(!Array.isArray(schemas)||!schemas.length)throw Error('基础字典配置不可用');
 let current=schemas[0],page=0,total=0,visibleRows=new Map();
 const selected=new Map();
 container.innerHTML=pageHeader('基础数据管理','物种、性别、采样类型与品种字典',schemas.some(s=>s.canWrite)?'<button class="button" data-dictionary-new>添加</button>':'')+`<div class="tabs">${schemas.map(s=>`<button class="tab ${s===current?'active':''}" data-kind="${escapeHtml(s.kind)}">${escapeHtml(s.label)}</button>`).join('')}</div><section class="panel"><div id="dictionary-tools"></div><div id="dictionary-list"></div></section>`;
 const tools=container.querySelector('#dictionary-tools'),list=container.querySelector('#dictionary-list');
 function updateSelection(){
  const count=container.querySelector('[data-selected-count]');if(count)count.textContent=`已选择 ${selected.size} 条`;
  container.querySelectorAll('[data-batch-status],[data-batch-delete]').forEach(button=>button.disabled=!selected.size);
 }
 function form(){
  tools.innerHTML=`<form id="dictionary-filter"><div class="form-grid">${field({name:'name',label:current.columns[1].label})}${field({name:'englishName',label:current.columns[2].label})}${current.hasStatus===false?'':field({name:'active',label:'状态',options:[{value:'',label:'全部'},{value:'1',label:'正常'},{value:'0',label:'禁用'}]})}${field({name:'sort',label:'排序字段',options:current.columns.map(c=>({value:c.key,label:c.label}))})}${field({name:'order',label:'排序方向',options:[{value:'DESC',label:'降序'},{value:'ASC',label:'升序'}]})}${['createdFrom','createdTo','updatedFrom','updatedTo'].map((name,i)=>field({name,label:[`${current.columns.find(c=>c.key==='createtime').label}起`,`${current.columns.find(c=>c.key==='createtime').label}止`,'更新时间起','更新时间止'][i],type:'datetime-local'})).join('')}</div><button class="button" type="submit">查询</button></form>`;
  if(current.canWrite)tools.insertAdjacentHTML('beforeend',`<div class="toolbar"><span data-selected-count>已选择 0 条</span>${current.hasStatus===false?'':`<button class="button secondary" data-batch-status="1" disabled>批量设为正常</button><button class="button secondary" data-batch-status="0" disabled>批量设为禁用</button>`}${current.canDelete?'<button class="button danger" data-batch-delete disabled>删除所选</button>':''}</div>`);
 }
 const reader=createDictionaryReader(context.api,data=>{
  total=Number(data.total)||0;const rows=pageItems(data);visibleRows=new Map(rows.map(row=>[Number(row[current.columns[0].key]),row]));
  const columns=current.columns.map(c=>({label:c.label,render:row=>escapeHtml(c.key==='IsActive'?(current.statusLabels[String(row[c.key])]??row[c.key]):['createtime','updatetime'].includes(c.key)?legacyTimestamp(row[c.key]):row[c.key])}));
  if(current.canWrite)columns.unshift({label:'选择',render:row=>`<input type="checkbox" data-dictionary-select="${Number(row[current.columns[0].key])}" aria-label="选择记录 ${Number(row[current.columns[0].key])}">`});
  list.innerHTML=renderTable({rows,columns,actions:current.canWrite?(row=>`<button class="button text" data-dictionary-edit="${Number(row[current.columns[0].key])}">编辑</button>${current.canDelete?`<button class="button danger text" data-dictionary-delete="${Number(row[current.columns[0].key])}">删除</button>`:''}`):undefined})+`<div class="toolbar"><span>共 ${total} 条 · 第 ${page+1} 页</span><button class="button secondary" data-page="prev" ${page===0?'disabled':''}>上一页</button><button class="button secondary" data-page="next" ${(page+1)*20>=total?'disabled':''}>下一页</button></div>`;
 },error=>{list.innerHTML=renderState(error.status===403?'forbidden':'error',error)});
 async function load(){try{const query=dictionaryQuery(formData(container.querySelector('#dictionary-filter')),page);selected.clear();updateSelection();list.innerHTML=renderState('loading');await reader(current.kind,query)}catch(error){context.toast(error.message,'error')}}
 container.addEventListener('submit',event=>{if(event.target.id==='dictionary-filter'){event.preventDefault();page=0;load()}});
 container.addEventListener('change',event=>{
  const box=event.target.closest('[data-dictionary-select]');if(!box)return;
  const id=Number(box.dataset.dictionarySelect),row=visibleRows.get(id);if(!row)return;
  if(box.checked)selected.set(id,row);else selected.delete(id);updateSelection();
 });
 container.addEventListener('click',event=>{
  const add=event.target.closest('[data-dictionary-new]'),edit=event.target.closest('[data-dictionary-edit]');
  if(add||edit){const schema=current,button=add||edit;button.disabled=true;openDictionaryEditor(context,schema,edit?{[schema.columns[0].key]:Number(edit.dataset.dictionaryEdit)}:null,load).catch(error=>context.toast(error.message,'error')).finally(()=>{button.disabled=false});return}
  const batch=event.target.closest('[data-batch-status],[data-batch-delete]'),single=event.target.closest('[data-dictionary-delete]');
  if(batch||single){const button=batch||single;if(button.disabled)return;const deleting=!!single||button.hasAttribute('data-batch-delete');const rows=single?[visibleRows.get(Number(single.dataset.dictionaryDelete))]:[...selected.values()];button.disabled=true;openDictionaryBatch(context,current,rows,deleting?'delete':Number(button.dataset.batchStatus),load).catch(error=>context.toast(error.message,'error')).finally(()=>{button.disabled=false;updateSelection()});return}
  const tab=event.target.closest('[data-kind]');if(tab){current=schemas.find(s=>s.kind===tab.dataset.kind);page=0;container.querySelectorAll('[data-kind]').forEach(b=>b.classList.toggle('active',b===tab));form();load();return}
  const button=event.target.closest('[data-page]');if(button&&!button.disabled){page+=button.dataset.page==='next'?1:-1;load();return}
  if(event.target.closest('[data-action="retry"]'))load();
 });
 form();await load();
}
