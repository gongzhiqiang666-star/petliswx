import {field,renderTable} from './ui.js';
import {escapeHtml} from '../utils.js';
import {paymentLogBase,paymentLogFields} from './legacy-payment-logs.js';
const base=paymentLogBase+'/import';
export function createPaymentLogImportSession(api){
 let preview=null,attempted=false,sequence=0,key=null;
 return {
  invalidate(){if(attempted)throw Error('上次导入结果未确认，请先重试或关闭后核对记录');sequence++;preview=null;key=null;},
  async prepare(data){if(attempted)throw Error('上次导入结果未确认，请先重试或关闭后核对记录');const current=++sequence;preview=null;const value=await api.request(base+'/preview',{method:'POST',data});if(current!==sequence)return null;preview=structuredClone(value);key=crypto.randomUUID();return structuredClone(preview);},
  async commit(){if(!preview)throw Error('请先预览导入文件并核对内容');attempted=true;return api.request(base,{method:'POST',data:{rows:structuredClone(preview.rows),expiresAt:preview.expiresAt,previewToken:preview.previewToken,requestKey:key}});}
 };
}
export function renderPaymentLogImportPreview(preview,page=0){
 const rows=preview.rows.slice(page*20,(page+1)*20).map(r=>({...r.fields,rowNumber:r.rowNumber,operation:r.id==null?'新增':'更新',PayLogId:r.id??'自动编号'}));
 return `<p><strong>新增 ${Number(preview.created)} 条 · 更新 ${Number(preview.updated)} 条</strong> · 编码 ${escapeHtml(preview.encoding)} · 预览有效期30分钟</p>${preview.warnings?.length?`<details class="form-hint"><summary>${preview.warnings.length} 条导入提示（点击查看）</summary><ul>${preview.warnings.map(w=>`<li>${escapeHtml(w)}</li>`).join('')}</ul></details>`:''}<div class="payment-log-list">${renderTable({rows,columns:[{key:'rowNumber',label:'文件行'},{key:'operation',label:'操作'},{key:'PayLogId',label:'编号'},...paymentLogFields.map(([key,label])=>({key,label}))]})}</div><div class="toolbar"><span>共 ${preview.rows.length} 行 · 第 ${page+1} 页</span><button type="button" class="button secondary" data-import-prev ${page===0?'disabled':''}>上一页</button><button type="button" class="button secondary" data-import-next ${(page+1)*20>=preview.rows.length?'disabled':''}>下一页</button></div>`;
}
export function openPaymentLogImport(context,onSaved){
 const session=createPaymentLogImportSession(context.api);let disposed=false,preview=null,page=0;
 context.dialog({title:'导入交易流水',wide:true,submitLabel:'确认整批导入',body:`<p>先预览，再整批保存。仅维护流水，不收款、不退款、不核销；相同交易号不会自动去重。新增文件不含 PayLogId 列；更新文件必须填写已有 PayLogId，未提供的列保持原值。</p><div class="form-grid"><label class="form-field wide"><span>CSV / XLS / XLSX（最多2MB、1000行，读取首个工作表）</span><input name="importFile" type="file" accept=".csv,.xls,.xlsx" required></label>${field({name:'headerType',label:'表头类型',options:[{value:'name',label:'字段名（推荐，支持主键更新）'},{value:'comment',label:'原PHP注释（原时间列标题为空）'}]})}${field({name:'encoding',label:'CSV编码',options:[{value:'AUTO',label:'自动识别（预览时请核对中文）'},{value:'UTF-8',label:'UTF-8'},{value:'GBK',label:'GBK'},{value:'BIG5',label:'Big5'},{value:'ISO-8859-1',label:'Latin1'}]})}</div><p class="form-hint">日期请用文本“2020-02-29 13:14:15”；金额按原字段保留两位。Excel公式原样读取，不执行计算；超过15位或带前导零的编号请设为文本。</p><div class="toolbar"><button type="button" class="button secondary" data-import-template>下载字段名模板</button><button type="button" class="button" data-import-preview>预览文件</button></div><div data-import-feedback role="status"></div><div data-import-results></div>`,onClose(){disposed=true;},onReady(form){
  const result=form.querySelector('[data-import-results]'),feedback=form.querySelector('[data-import-feedback]'),inputs=['importFile','headerType','encoding'].map(n=>form.elements[n]);
  const show=()=>result.innerHTML=preview?renderPaymentLogImportPreview(preview,page):'';
  for(const input of inputs)input.addEventListener('change',()=>{try{session.invalidate();preview=null;show();feedback.textContent='文件或选项已更改，请重新预览';}catch(error){feedback.textContent=error.message;}});
  form.addEventListener('click',async event=>{const button=event.target.closest('[data-import-preview],[data-import-template],[data-import-prev],[data-import-next]');if(!button||button.disabled)return;button.disabled=true;try{
   if(button.hasAttribute('data-import-template')){const file=await context.api.download(base+'/template');const url=URL.createObjectURL(file.blob),link=document.createElement('a');link.href=url;link.download=file.filename;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
   else if(button.hasAttribute('data-import-preview')){const file=form.elements.importFile.files[0];if(!file||file.size>2*1024*1024)throw Error('请选择不超过2MB的CSV、XLS或XLSX文件');const data=new FormData();data.append('file',file);data.append('encoding',form.elements.encoding.value);data.append('headerType',form.elements.headerType.value);preview=null;show();feedback.textContent='正在预览…';const loaded=await session.prepare(data);if(disposed||!loaded)return;preview=loaded;page=0;show();feedback.textContent='请核对各页记录后点击“确认整批导入”';}
   else{page+=button.hasAttribute('data-import-prev')?-1:1;show();}
  }catch(error){if(!disposed)feedback.textContent=error.message;}finally{button.disabled=false;}});
 },async onSubmit(form){
  if(!preview)throw Error('请先预览导入文件并核对内容');for(const n of ['importFile','headerType','encoding'])form.elements[n].disabled=true;const button=form.querySelector('[data-import-preview]');button.disabled=true;
  const result=await session.commit();if(form.isConnected){context.toast(`已导入：新增 ${result.created} 条，更新 ${result.updated} 条`);await onSaved();}
 }});
}
