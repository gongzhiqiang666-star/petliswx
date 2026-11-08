const mappings={CreateDataTypeIId:'dataGroup',InfoHospitalId:'hospital',InfoAnimalId:'species',AnimalTypeId:'variety',DictSexId:'sex',InfoSampleTypeId:'sampleType'};
/** Search/pagination and selected-ID hydration; never invent dependent dictionary relations. */
export async function mountMainOrderChoices(form,api){
 const controls=[];
 for(const [name,kind] of Object.entries(mappings)){
  const input=form.elements.namedItem(name);if(!input)continue;
  const select=document.createElement('select');select.name=input.name;select.id=input.id;
  const selected=String(input.value||'0');select.add(new Option(selected==='0'?'未选择':`原选择 #${selected}`,selected));input.replaceWith(select);
  const toolbar=document.createElement('div');toolbar.className='toolbar';
  const search=document.createElement('input');search.type='search';search.placeholder='输入名称搜索';search.setAttribute('aria-label',`${kind}名称搜索`);
  const button=document.createElement('button');button.type='button';button.className='button secondary';button.textContent='搜索';
  const more=document.createElement('button');more.type='button';more.className='button text';more.textContent='更多';more.hidden=true;
  const status=document.createElement('small');status.setAttribute('role','status');toolbar.append(search,button,more);select.after(toolbar,status);
  let page=0,query='',version=0,busy=false;const records=new Map();
  const fill=items=>{for(const row of items??[]){records.set(String(row.id),row);const existing=[...select.options].find(option=>option.value===String(row.id));if(existing)existing.textContent=row.name??`#${row.id}`;else select.add(new Option(row.name??`#${row.id}`,String(row.id)));}};
  const applyVariety=()=>{if(kind!=='variety'||select.value==='0')return;const row=records.get(select.value);if(!row)return;for(const [target,key] of [['AnimalType','Variety_CN'],['AnimalType_en','Variety_EN']]){const field=form.elements.namedItem(target);if(field&&row[key]!=null)field.value=String(row[key]);}};
  select.addEventListener('change',applyVariety);
  let numberVersion=0;
  if(kind==='dataGroup')select.addEventListener('change',async()=>{
   const group=select.value,version=++numberVersion,number=form.elements.namedItem('UserNO'),before=number?.value;
   if(group==='0'){delete form.dataset.mainNumberError;return;}
   form.dataset.mainNumberPending=String(Number(form.dataset.mainNumberPending||0)+1);delete form.dataset.mainNumberError;
   try{const result=await api.request(`/admin/legacy/main-orders/group-number/${encodeURIComponent(group)}`);if(!form.isConnected||version!==numberVersion||select.value!==group)return;
    if(number&&number.value===before)number.value=result.userno;
    status.textContent=`${result.groupName}；${result.prefix?'固定前缀：'+result.prefix:'无固定前缀'}；起始编号：${result.startNo}；当前最新编号：${result.latest}`;
   }catch(error){if(version===numberVersion){status.textContent=error.message;form.dataset.mainNumberError=error.message;}}finally{form.dataset.mainNumberPending=String(Math.max(0,Number(form.dataset.mainNumberPending||0)-1));}
  });
  const hint=document.createElement('small');hint.setAttribute('role','status');if(kind==='hospital')status.after(hint);
  let hospitalVersion=0;
  const loadHospital=async()=>{if(kind!=='hospital')return;const selected=select.value,version=++hospitalVersion;try{const result=await api.request(`/admin/legacy/main-orders/hospital-settlement/${encodeURIComponent(selected)}`);if(!form.isConnected||version!==hospitalVersion||select.value!==selected)return;hint.replaceChildren(document.createTextNode(String(result.message??'')));
   if(result.linkConfigured===true&&/^https?:\/\//i.test(result.url??'')){const anchor=document.createElement('a');anchor.href=result.url;anchor.target='_blank';anchor.rel='noopener noreferrer';anchor.textContent='查看汇总付款链接';hint.append(' ',anchor);}else if(result.found&&result.linkMessage)hint.append(' · 汇总链接尚未配置');
  }catch(error){if(version===hospitalVersion)hint.textContent=error.message;}};
  if(kind==='hospital'){select.addEventListener('change',loadHospital);loadHospital();}
  const initialText=kind==='variety' ?[form.elements.namedItem('AnimalType')?.value,form.elements.namedItem('AnimalType_en')?.value]:null;
  const load=async append=>{const current=++version;busy=true;button.disabled=true;more.disabled=true;status.textContent='加载中…';try{
   const next=append?page+1:0,q=append?query:search.value;
   const result=await api.request(`/admin/legacy/main-orders/choices/${kind}`,{query:{q,page:next,size:50}});if(current!==version||!form.isConnected)return;
   if(!append){const value=select.value,label=select.selectedOptions[0]?.textContent??`#${value}`;select.replaceChildren(new Option('未选择','0'));if(value!=='0')select.add(new Option(label,value));select.value=value;}
   fill(result.items);page=next;query=q;more.hidden=(page+1)*50>=Number(result.total);status.textContent='';
  }catch(error){if(current===version)status.textContent=error.message;}finally{if(current===version){busy=false;button.disabled=false;more.disabled=false;}}};
  button.addEventListener('click',()=>{if(!busy)load(false);});more.addEventListener('click',()=>{if(!busy)load(true);});search.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();if(!busy)load(false);}});
  controls.push({select,kind,fill,selected,applyVariety,initialText});
 }
 await Promise.all(controls.map(async control=>{
  try{const result=await api.request(`/admin/legacy/main-orders/choices/${control.kind}`,{query:control.selected==='0'?{page:0,size:50}:{selectedId:control.selected,page:0,size:1}});if(form.isConnected){control.fill(result.items);if(control.kind==='variety'&&control.select.value===control.selected&&form.elements.namedItem('AnimalType')?.value===control.initialText[0]&&form.elements.namedItem('AnimalType_en')?.value===control.initialText[1])control.applyVariety();}}catch{ /* Preserve the original ID; search can retry without clearing the value. */ }
 }));
}
