import {formData} from '../pages/shared.js';
import {cell,formatDate,idempotencyKey,pageItems,value} from '../utils.js';
import {field,pagination,renderDetail,renderState,renderTable} from './ui.js';
const states={draft:'草稿',published:'已发布',archived:'已下架'};
const types={article:'图文',live:'直播',activity:'活动'};
export function articleData(data){return {category:data.category,type:data.type,title:data.title,summary:data.summary||'',content:data.content||'',coverUrl:data.coverUrl||'',liveAt:data.liveAt||null,liveUrl:data.liveUrl||null,popupEnabled:data.popupEnabled==='true'};}
export function createArticlePublisher(api){const key=idempotencyKey();return data=>api.request('/articles',{method:'POST',headers:{'Idempotency-Key':key},data:articleData(data)});}
export function articleFields(article={},publishing=false){
 const current=(key,fallback='')=>value(article,key,fallback);
 return `<div class="form-grid">${field({name:'category',label:'分类',required:true,value:current('category')})}${field({name:'type',label:'类型',value:current('type','article'),options:Object.entries(types).map(([value,label])=>({value,label}))})}${field({name:'title',label:'标题',required:true,value:current('title')})}${field({name:'coverUrl',label:'封面 HTTPS 地址（选填）',value:current('coverUrl')})}${field({name:'liveAt',label:'直播时间',type:'datetime-local',value:String(current('liveAt')).replace(' ','T').slice(0,16)})}${field({name:'liveUrl',label:'直播 HTTPS 地址',value:current('liveUrl')})}${field({name:'summary',label:'摘要（最多1000字）',type:'textarea',wide:true,value:current('summary')})}${field({name:'content',label:'正文',type:'textarea',required:publishing,wide:true,value:current('content')})}${field({name:'popupEnabled',label:'弹窗展示',value:Number(current('popupEnabled',0))===1?'true':'false',options:[{value:'false',label:'否'},{value:'true',label:'是'}]})}</div>`;
}
export async function renderArticleAdmin(container,context){
 const {api}=context;let status='',page=0,sequence=0;
 container.innerHTML='<div id="article-admin"><div class="toolbar"><button class="button" id="article-draft-new">新建草稿</button><button class="button secondary" id="article-new">直接发布资讯</button><label>状态 <select data-article-filter><option value="">全部</option><option value="draft">草稿</option><option value="published">已发布</option><option value="archived">已下架</option></select></label></div><p class="form-hint">草稿和下架内容不在小程序展示；编辑不重复通知，下架保留内容及操作记录。</p><div id="article-list"></div></div>';
 const root=container.querySelector('#article-admin'),list=root.querySelector('#article-list');
 async function load(){const run=++sequence;list.innerHTML=renderState('loading');try{
  const result=await api.request('/admin/articles',{query:{status,page,size:20}});if(run!==sequence)return;
  list.innerHTML=renderTable({rows:pageItems(result),emptyText:'暂无对应状态的资讯',columns:[{label:'标题',render:r=>cell(r,'title')},{label:'分类',render:r=>cell(r,'category')},{label:'类型',render:r=>types[r.type]||cell(r,'type')},{label:'状态',render:r=>states[r.status]||cell(r,'status')},{label:'发布时间',render:r=>formatDate(value(r,'publishedAt',null))},{label:'更新时间',render:r=>formatDate(value(r,'updatedAt',null))}],actions:r=>`<button class="button text" data-article-edit="${r.id}">编辑</button>${r.status==='draft'?`<button class="button small" data-article-status="${r.id}" data-status="published">发布</button>`:''}${r.status==='published'||r.status==='draft'?`<button class="button danger text" data-article-status="${r.id}" data-status="archived">${r.status==='published'?'下架':'归档'}</button>`:''}${r.status==='archived'?`<button class="button text" data-article-status="${r.id}" data-status="draft">恢复草稿</button>`:''}`})+pagination(result);
 }catch(e){if(run===sequence)list.innerHTML=renderState(e.status===403?'forbidden':'error',e);}}
 function create(publish){const key=idempotencyKey(),send=publish?createArticlePublisher(api):data=>api.request('/admin/articles',{method:'POST',headers:{'Idempotency-Key':key},data:articleData(data)});
  context.dialog({title:publish?'发布资讯':'新建资讯草稿',wide:true,submitLabel:publish?'确认发布':'保存草稿',body:articleFields({},publish),async onSubmit(form){await send(formData(form));context.toast(publish?'资讯已发布':'草稿已保存');await load();}});
 }
 root.addEventListener('change',e=>{if(e.target.matches('[data-article-filter]')){status=e.target.value;page=0;load();}});
 root.addEventListener('click',async e=>{
  if(e.target.closest('#article-new')){create(true);return;}if(e.target.closest('#article-draft-new')){create(false);return;}
  const paging=e.target.closest('[data-page]');if(paging&&!paging.disabled){page=Number(paging.dataset.page);load();return;}
  if(e.target.closest('[data-action=retry]')){load();return;}
  const edit=e.target.closest('[data-article-edit]'),change=e.target.closest('[data-article-status]');if(!edit&&!change)return;
  try{
   const id=Number((edit||change).dataset[edit?'articleEdit':'articleStatus']),article=await api.request('/admin/articles/'+id),key=idempotencyKey();
   if(edit)context.dialog({title:'编辑资讯',wide:true,submitLabel:'保存修改',body:articleFields(article,article.status==='published')+field({name:'reason',label:'修改原因',required:true,type:'textarea',wide:true}),async onSubmit(form){const data=formData(form);await api.request('/admin/articles/'+id,{method:'PUT',data:{article:articleData(data),version:article.version,reason:data.reason,requestKey:key}});context.toast('资讯已更新');await load();}});
   else {const target=change.dataset.status;context.dialog({title:target==='published'?'确认发布资讯':target==='draft'?'恢复为草稿':'下架或归档资讯',submitLabel:'确认操作',body:renderDetail([{label:'标题',value:article.title},{label:'当前状态',value:states[article.status]},{label:'目标状态',value:states[target]}])+field({name:'reason',label:'操作原因',required:true,type:'textarea',wide:true}),async onSubmit(form){await api.request('/admin/articles/'+id+'/status',{method:'POST',data:{status:target,version:article.version,reason:formData(form).reason,requestKey:key}});context.toast('状态已更新');await load();}});}
  }catch(e){context.toast(e.message,'error');}
 });
 await load();
}
