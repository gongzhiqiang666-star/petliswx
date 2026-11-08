import {renderDetail} from './ui.js';
const base='/orders/hospital-history';
export async function openHospitalDraftRemark(context,hospitalId,id,onSaved){
 const row=await context.api.request(`${base}/drafts/${id}`,{query:{hospitalId}}),writable=row.writable===true;
 const editorModule=writable?await import('../../assets/legacy-editor.js'):null;
 const original=String(row.fields.Remark??''),requestKey=crypto.randomUUID();let editor=null,submitted=null;
 context.dialog({title:writable?'编辑医院草稿备注':'查看医院草稿备注',wide:true,...(writable?{submitLabel:'保存备注'}:{}),body:`${renderDetail([{label:'动物姓名',value:row.fields.AnimalName},{label:'畜主姓名',value:row.fields.MasterName},{label:'送检医师',value:row.fields.DoctorName}])}<h3>送检备注</h3><p class="muted">保存后可在小程序继续选择项目和预览送检。</p><div data-hospital-remark-editor></div><p data-hospital-remark-feedback role="status"></p><iframe title="原送检备注预览" data-hospital-remark-preview sandbox referrerpolicy="no-referrer" style="width:100%;min-height:280px;border:1px solid #dce8e4" hidden></iframe>`,
  onReady(form){
   const preview=()=>{const frame=form.querySelector('[data-hospital-remark-preview]');frame.hidden=false;frame.srcdoc='<!doctype html><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src &#39;none&#39;; style-src &#39;unsafe-inline&#39;; img-src data:; form-action &#39;none&#39;; base-uri &#39;none&#39;"><body>'+original+'</body>'};
   if(!writable){preview();return}
   try{editor=editorModule.mountEditor(form.querySelector('[data-hospital-remark-editor]'),original)}catch(error){form.querySelector('[data-hospital-remark-feedback]').textContent='当前备注含暂不能安全编辑的内容，已保留原样。';preview();form.closest('[data-modal-panel]').querySelector('button[type=submit]').disabled=true;}
  },onClose(){editor?.destroy()},...(writable?{async onSubmit(form){
   if(!editor)throw Error('当前备注暂不能安全编辑，原文已保留');const html=editor.getHTML();if(!editor.isDirty()||html===original)throw Error('备注未修改');
   const command={hospitalId,fields:{Remark:html},expectedToken:row.editToken,requestKey},canonical=JSON.stringify(command);if(submitted&&submitted!==canonical)throw Error('上次保存结果未确认，请先按原内容重试');submitted=canonical;
   let result;try{result=await context.api.request(`${base}/drafts/${id}`,{method:'PUT',data:command})}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))submitted=null;throw error}
   if(Number(result)!==id)throw Error('保存结果待核对，请按原内容重试');if(form.isConnected){context.toast('备注已保存');await onSaved?.()}
  }}:{})
 });
}
