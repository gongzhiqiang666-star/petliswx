import {formData} from '../pages/shared.js';
import {escapeHtml,formatDate,idempotencyKey,pageItems} from '../utils.js';
import {field,renderTable} from './ui.js';
const labels={LICENSE:'执照资料',COOPERATION:'合作资料',OTHER:'其他资料'};
export function hospitalDocumentTable(rows){return renderTable({rows,emptyText:'暂无医院资料文件',columns:[{label:'文件',render:r=>`<strong>${escapeHtml(r.originalName)}</strong><small class="cell-sub">#${escapeHtml(r.id)} · ${Math.ceil(Number(r.fileSize)/1024)} KB</small>`},{label:'资料类型',render:r=>escapeHtml(labels[r.purpose]||r.purpose||'其他')},{label:'版本',render:r=>`${Number(r.replaced)?'历史版本':'当前版本'}${r.previousFileId&&r.previousFileId!=='null'?' · 替换 #'+escapeHtml(r.previousFileId):''}`},{label:'上传原因',render:r=>escapeHtml(r.reason)},{label:'上传时间',render:r=>formatDate(r.createdAt)}],actions:r=>`<button type="button" class="button text" data-hospital-document-download="${r.id}">下载</button>`});}
export async function openHospitalDocuments(context,hospitalId){
 const {api}=context,endpoint='/admin/hospitals/'+hospitalId+'/documents',requestKey=idempotencyKey();let page=0;
 const initial=await api.request(endpoint,{query:{page,size:20}});let rows=pageItems(initial),total=Number(initial.total);
 const choices=()=>[{value:'',label:'新增独立资料'},...rows.filter(r=>!Number(r.replaced)).map(r=>({value:r.id,label:'#'+r.id+' '+r.originalName}))];
 const history=()=>hospitalDocumentTable(rows)+(rows.length<total?'<button type="button" class="button secondary" id="hospital-doc-more">加载更多历史</button>':'');
 context.dialog({title:'医院资料文件',wide:true,submitLabel:'上传资料',body:`<p class="form-hint">资料仅平台管理员可查看。替换会保留旧版本及原因，不删除原文件。</p><div id="hospital-doc-history">${history()}</div><div class="form-grid"><label class="form-field wide"><span>资料文件（PDF / PNG / JPEG，最大10MB）</span><input type="file" name="documentFile" accept="application/pdf,image/png,image/jpeg,.pdf,.png,.jpg,.jpeg"></label>${field({name:'purpose',label:'资料类型',required:true,options:Object.entries(labels).map(([value,label])=>({value,label}))})}${field({name:'previousFileId',label:'新增或替换',options:choices()})}${field({name:'reason',label:'上传或替换原因（最多500字）',required:true,type:'textarea',wide:true})}</div>`,onReady(form){
  form.querySelector('[name=previousFileId]').addEventListener('change',e=>{const row=rows.find(x=>String(x.id)===e.target.value);if(row)form.querySelector('[name=purpose]').value=row.purpose;});
  form.addEventListener('click',async e=>{
   const download=e.target.closest('[data-hospital-document-download]'),more=e.target.closest('#hospital-doc-more');if(!download&&!more)return;const button=download||more;button.disabled=true;
   try{
    if(download){const file=await api.download('/platform-files/'+download.dataset.hospitalDocumentDownload+'/download'),url=URL.createObjectURL(file.blob),link=document.createElement('a');link.href=url;link.download=file.filename;link.click();URL.revokeObjectURL(url);}
    else {const result=await api.request(endpoint,{query:{page:page+1,size:20}});page++;rows=rows.concat(pageItems(result));total=Number(result.total);form.querySelector('#hospital-doc-history').innerHTML=history();const select=form.querySelector('[name=previousFileId]'),current=select.value;select.innerHTML=choices().map(x=>`<option value="${escapeHtml(x.value)}">${escapeHtml(x.label)}</option>`).join('');select.value=current;}
   }catch(error){context.toast(error.message,'error');}finally{button.disabled=false;}
  });
 },async onSubmit(form){
  const file=form.querySelector('[name=documentFile]').files[0];if(!file)throw Error('请选择医院资料文件');if(file.size>10*1024*1024)throw Error('资料文件不能超过10MB');const data=formData(form),body=new FormData();body.append('file',file);body.append('purpose',data.purpose);body.append('reason',data.reason.trim());body.append('requestKey',requestKey);if(data.previousFileId)body.append('previousFileId',data.previousFileId);await api.request(endpoint,{method:'POST',data:body});context.toast('医院资料已保存，旧版本保留');
 }});
}
