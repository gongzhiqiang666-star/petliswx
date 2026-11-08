import {field} from './ui.js';import {formData} from '../pages/shared.js';
const base='/orders/hospital-history';
const fields=[['MasterName','畜主姓名'],['AnimalName','动物姓名'],['Age','动物年龄'],['DoctorName','送检医师'],['SampleArea','采样部位'],['CollectDt','采样日期'],['AnimalTypeId','动物品种'],['DictSexId','动物性别'],['InfoSampleTypeId','采样类型']];
const kinds={AnimalTypeId:'variety',DictSexId:'sex',InfoSampleTypeId:'sampleType'};
export function draftEditableValues(row){return Object.fromEntries(fields.map(([name])=>[name,name==='CollectDt'&&String(row.fields[name]??'').startsWith('1900-01-01')?'':String(row.fields[name]??'')]))}
export function changedDraftFields(before,values){return Object.fromEntries(fields.filter(([key])=>values[key]!==undefined&&String(values[key])!==before[key]).map(([key])=>[key,String(values[key])]))}
export async function openHospitalDraftInfo(context,hospitalId,id,onSaved){
 const creating=id==null;const caps=await context.api.request(base+'/capabilities');
 const row=creating?{fields:{Age:' - 岁 - 月',AnimalTypeId:0,DictSexId:0,InfoSampleTypeId:0},labels:{},writable:caps.legacyHospitalDraftCreates===true}:await context.api.request(`${base}/drafts/${id}`,{query:{hospitalId}});
 if(creating&&row.writable!==true)throw Error('医院草稿新建尚未开启');
 const before=draftEditableValues(row),requestKey=crypto.randomUUID();let submitted=null;
 context.dialog({title:creating?'新建医院送检草稿':'医院草稿资料',wide:true,...(row.writable?{submitLabel:creating?'创建草稿':'保存资料'}:{}),body:`<p class="muted">第一步：填写送检资料。保存后选择检测项目；备注在草稿列表单独维护。</p><div class="form-grid">${fields.filter(([name])=>!creating||name!=='SampleArea').map(([name,label])=>field({name,label,value:before[name],type:name==='CollectDt'?'date':'text',required:creating&&['MasterName','AnimalName','Age','DoctorName'].includes(name)})).join('')}</div>`,
  onReady(form){if(!row.writable){for(const input of form.querySelectorAll('input'))input.disabled=true;return}for(const[name,kind]of Object.entries(kinds))mountChoice(form,context.api,hospitalId,name,kind,row.labels[name]);},
  ...(row.writable?{async onSubmit(form){const values=formData(form),changed=changedDraftFields(before,values);let data;
   if(creating){data={hospitalId,fields:Object.fromEntries(Object.entries(values).filter(([key,value])=>fields.some(([k])=>key===k)&&value!=='')),requestKey}}
   else{if(!Object.keys(changed).length)throw Error('资料未修改');data={hospitalId,fields:changed,expectedToken:row.editToken,requestKey}}
   const canonical=JSON.stringify(data);if(submitted&&submitted!==canonical)throw Error('上次保存结果未确认，请先用原内容重试');submitted=canonical;
   let result;try{result=await context.api.request(base+'/drafts'+(creating?'':'/'+id),{method:creating?'POST':'PUT',data})}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))submitted=null;throw error}
   if(!Number.isSafeInteger(Number(result))||Number(result)<1||!creating&&Number(result)!==id)throw Error('保存结果待核对，请按原内容重试');if(form.isConnected){context.toast(creating?'草稿已创建，请选择检测项目':'资料已保存');await onSaved?.(Number(result))}
  }}:{})
 });
}
function mountChoice(form,api,hospitalId,name,kind,label){
 const input=form.elements.namedItem(name),select=document.createElement('select'),selected=String(input.value||0);select.name=name;select.id=input.id;select.add(new Option(label||(selected==='0'?'未选择':`原选择 #${selected}`),selected));input.replaceWith(select);
 const bar=document.createElement('div');bar.className='toolbar';const search=document.createElement('input');search.type='search';search.placeholder='输入名称搜索';search.setAttribute('aria-label',name+'名称搜索');const find=document.createElement('button'),more=document.createElement('button'),status=document.createElement('small');for(const b of [find,more]){b.type='button';b.className='button secondary'}find.textContent='搜索';more.textContent='更多';more.hidden=true;bar.append(search,find,more,status);select.after(bar);let page=0,query='',sequence=0;
 async function load(append=false){const run=++sequence,next=append?page+1:0,q=append?query:search.value;find.disabled=more.disabled=true;try{const data=await api.request(base+'/draft-choices/'+kind,{query:{hospitalId,q,page:next,size:50}});if(run!==sequence||!form.isConnected)return;const current=select.value,text=select.selectedOptions[0]?.textContent;if(!append){select.replaceChildren(new Option(text||'未选择',current))}for(const r of data.items){const found=[...select.options].find(o=>o.value===String(r.id));if(found)found.textContent=r.name;else select.add(new Option(r.name,String(r.id)))}select.value=current;page=next;query=q;more.hidden=(page+1)*50>=Number(data.total);status.textContent=''}catch(error){if(run===sequence)status.textContent=error.message}finally{if(run===sequence)find.disabled=more.disabled=false}}
 find.addEventListener('click',()=>load());more.addEventListener('click',()=>load(true));load();
}
