import {pageHeader,renderTable,renderState,field} from '../components/ui.js';
import {escapeHtml} from '../utils.js';
const base='/admin/legacy/templates';
export async function openTemplate(context,kind,id,onSaved){
 const [caps,row,editorModule,pdfCaps]=await Promise.all([context.api.request(base+'/capabilities'),id==null?Promise.resolve({title:'',html:'',projectId:0}):context.api.request(`${base}/${kind}/${id}`),import('../../assets/legacy-editor.js'),context.api.request('/admin/legacy/html-pdf/capabilities').catch(()=>({htmlPdf:false}))]);
 if(caps.templateWrites!==true)throw Error('模板维护尚未开启');
 let editor,editorError,submitted=null;const requestKey=crypto.randomUUID();
 context.dialog({title:id==null?'新增业务模板':'编辑业务模板',wide:true,submitLabel:'保存模板',body:`<p class="muted">保存模板不会修改已有检测明细或发送邮件。报告正文的变量、表格和图片地址会随模板保存。</p>${field({name:'title',label:'模板名称',value:row.title,required:true})}${kind==='report'?field({name:'projectId',label:'所属检测项目',value:row.projectId,type:'number'}):''}<h3>模板正文</h3>${pdfCaps.htmlPdf===true?'<div class="toolbar"><button type="button" class="button secondary" data-template-pdf>下载当前正文PDF预览</button></div>':''}<div data-template-editor></div><p data-template-error role="alert"></p><details><summary>当前正文源文（只读）</summary><textarea readonly rows="8" aria-label="当前正文源文">${escapeHtml(row.html??'')}</textarea></details>`,
  onReady(form){try{editor=editorModule.mountEditor(form.querySelector('[data-template-editor]'),String(row.html??''));}catch(error){editorError=error;form.querySelector('[data-template-error]').textContent=`无法安全编辑此正文：${error.message}。已保留源文，请先核对内容。`;}
   form.querySelector('[data-template-pdf]')?.addEventListener('click',async event=>{const button=event.currentTarget;button.disabled=true;try{if(editorError||!editor)throw Error('正文尚不能安全预览');const file=await context.api.download('/admin/legacy/html-pdf/preview',{method:'POST',data:{html:editor.getHTML()}});if(form.isConnected){const url=URL.createObjectURL(file.blob),link=document.createElement('a');link.href=url;link.download=file.filename;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}}catch(error){if(form.isConnected)form.querySelector('[data-template-error]').textContent=error.message;}finally{button.disabled=false;}});
   if(kind==='report')mountProjects(form,context.api);
  },onClose(){editor?.destroy();},async onSubmit(form){
   if(editorError||!editor)throw Error('正文尚不能安全编辑，请核对源文后处理');
   const projectId=kind==='report'?Number(form.elements.namedItem('projectId').value):0;
   if(!Number.isSafeInteger(projectId)||projectId<0)throw Error('请选择有效的检测项目');
   const command={title:form.elements.namedItem('title').value,html:editor.getHTML(),projectId,requestKey,expectedToken:row.editToken??null},canonical=JSON.stringify(command);
   if(submitted&&submitted!==canonical)throw Error('上次保存结果未确认，请先用原内容重试或重新打开核对');submitted=canonical;
   try{await context.api.request(`${base}/${kind}${id==null?'':'/'+id}`,{method:id==null?'POST':'PUT',data:command});}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))submitted=null;throw error;}
   if(form.isConnected){context.toast('模板已保存');await onSaved?.();}
  }});
}
function mountProjects(form,api){
 const input=form.elements.namedItem('projectId'),select=document.createElement('select');select.name='projectId';select.id=input.id;
 select.add(new Option('未关联检测项目','0'));const value=String(input.value||0);if(value!=='0')select.add(new Option(`原检测项目 #${value}`,value));select.value=value;input.replaceWith(select);
 const toolbar=document.createElement('div');toolbar.className='toolbar';const search=document.createElement('input');search.type='search';search.placeholder='项目名称或编号';search.setAttribute('aria-label','检测项目搜索');
 const button=document.createElement('button');button.type='button';button.className='button secondary';button.textContent='搜索项目';const more=document.createElement('button');more.type='button';more.className='button text';more.textContent='更多项目';more.hidden=true;const feedback=document.createElement('small');feedback.setAttribute('role','status');toolbar.append(search,button,more);select.after(toolbar,feedback);
 let page=0,query='',busy=false;
 async function load(append=false){if(busy)return;busy=true;button.disabled=true;more.disabled=true;try{const next=append?page+1:0,q=append?query:search.value,data=await api.request(base+'/projects',{query:{q,page:next,size:100}});if(!form.isConnected)return;
  if(!append){const old=select.value,label=select.selectedOptions[0]?.textContent;select.replaceChildren(new Option('未关联检测项目','0'));if(old!=='0')select.add(new Option(label??`#${old}`,old));select.value=old;}
  for(const item of data.items??[]){const old=[...select.options].find(o=>o.value===String(item.id));if(old)old.textContent=item.name;else select.add(new Option(item.name,String(item.id)));}page=next;query=q;more.hidden=(page+1)*100>=Number(data.total);feedback.textContent='';
 }catch(error){feedback.textContent=error.message;}finally{busy=false;button.disabled=false;more.disabled=false;}}
 button.addEventListener('click',()=>load());more.addEventListener('click',()=>load(true));search.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();load();}});load();
}
export async function render(container,context){
 const caps=await context.api.request(base+'/capabilities');let kind='report',page=0,version=0;
 container.innerHTML=pageHeader('业务模板','维护检测报告与邮件模板；保存后供后续明细选择使用',caps.templateWrites?'<button type="button" class="button" data-template-new>新增模板</button>':'')+'<section class="panel"><div class="toolbar"><label>模板类型 <select data-template-kind><option value="report">报告模板</option><option value="mail">邮件模板</option></select></label><label>模板名称 <input type="search" data-template-search></label><button type="button" class="button secondary" data-template-search-button>查询</button></div><div data-template-list></div></section>';
 const target=container.querySelector('[data-template-list]');
 async function load(){const current=++version;target.innerHTML=renderState('loading');try{const data=await context.api.request(`${base}/${kind}`,{query:{q:container.querySelector('[data-template-search]').value,page,size:20}});if(current!==version||!container.isConnected)return;
  target.innerHTML=renderTable({rows:data.items??[],columns:[{label:'模板编号',key:'id'},{label:'模板名称',key:'title'},...(kind==='report'?[{label:'所属项目编号',key:'projectId'}]:[])],actions:r=>`<button type="button" class="button text" data-template-view="${r.id}">查看正文</button>${caps.templateWrites?`<button type="button" class="button text" data-template-edit="${r.id}">编辑</button>`:''}${caps.templateDeletes?`<button type="button" class="button text" data-template-delete="${r.id}">删除</button>`:''}`})+`<div class="toolbar"><span>共 ${Number(data.total)} 条 · 第 ${page+1} 页</span><button type="button" class="button secondary" data-template-prev ${page===0?'disabled':''}>上一页</button><button type="button" class="button secondary" data-template-next ${(page+1)*20>=Number(data.total)?'disabled':''}>下一页</button></div>`;
 }catch(error){if(current===version)target.innerHTML=renderState('error',error);}}
 container.querySelector('[data-template-kind]').addEventListener('change',event=>{kind=event.target.value;page=0;load();});
 container.addEventListener('click',async event=>{const b=event.target.closest('[data-template-new],[data-template-edit],[data-template-view],[data-template-delete],[data-template-search-button],[data-template-prev],[data-template-next],[data-action=retry]');if(!b||b.disabled)return;b.disabled=true;try{
  if(b.hasAttribute('data-template-new')||b.hasAttribute('data-template-edit'))await openTemplate(context,kind,b.dataset.templateEdit?Number(b.dataset.templateEdit):null,load);
  else if(b.hasAttribute('data-template-view')){const row=await context.api.request(`${base}/${kind}/${Number(b.dataset.templateView)}`);context.dialog({title:'模板正文预览',wide:true,body:`<h3>${escapeHtml(row.title)}</h3><p class="muted">正文预览不替换业务变量，不生成PDF；外部图片不加载，原地址保留。</p><iframe class="legacy-body-preview" data-template-preview title="模板正文" sandbox referrerpolicy="no-referrer"></iframe><details><summary>正文源文</summary><pre>${escapeHtml(row.html??'')}</pre></details>`,onReady(target){target.querySelector('[data-template-preview]').srcdoc=`<!doctype html><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; img-src data:; form-action 'none'; base-uri 'none'"><body>${String(row.html??'')}</body>`;}});}
  else if(b.hasAttribute('data-template-delete')){const selectedKind=kind,id=Number(b.dataset.templateDelete),row=await context.api.request(`${base}/${selectedKind}/${id}`),requestKey=crypto.randomUUID();context.dialog({title:'删除业务模板',submitLabel:'确认删除模板',body:`<p>确认删除「${escapeHtml(row.title)}」？后续不能再选择此模板，已有检测明细正文保持原样。</p>`,async onSubmit(){await context.api.request(`${base}/${selectedKind}/${id}/delete`,{method:'POST',data:{requestKey,expectedToken:row.editToken}});context.toast('模板已删除');await load();}});}
  else{if(b.hasAttribute('data-template-prev'))page--;else if(b.hasAttribute('data-template-next'))page++;else page=0;await load();}
 }catch(error){context.toast(error.message,'error');}finally{b.disabled=false;}});await load();
}
