import {renderDetail,renderTable} from './ui.js';
export async function openHospitalDraftPreview(context,hospitalId,id,onSaved){
 const base='/orders/hospital-history/drafts/'+id,row=await context.api.request(base+'/preview',{query:{hospitalId}}),requestKey=crypto.randomUUID();
 const fields=[['MasterName','畜主姓名'],['AnimalName','动物姓名'],['Age','动物年龄'],['DoctorName','送检医师'],['CollectDt','采样日期'],['SampleArea','采样部位']].map(([key,label])=>({label,value:row.fields[key]||'未记录'}));fields.push({label:'动物性别',value:row.sexName||'未记录'});
 if(row.previewMode==='PENDING')fields.push({label:'当前品种',value:row.idVarietyName||'未记录'},{label:'原预览品种',value:row.legacyVarietyName||'未记录'});else fields.push({label:'动物品种',value:row.varietyName||'未记录'});
 context.dialog({title:'预览送检资料',wide:true,...(row.writable===true?{submitLabel:'确认提交送检'}:{}),body:`${renderDetail(fields)}${row.writable!==true?'<p class="muted">当前仅可预览；提交参数未开启或品种预览规则待确认。</p>':''}<h3>检测项目</h3>${renderTable({columns:[{key:'testName',label:'项目名称'},{key:'testNo',label:'项目编号'},{key:'price',label:'检测金额（元）'}],rows:row.items,emptyText:'尚未选择检测项目'})}<h3>送检备注</h3><iframe data-draft-preview-remark title="送检备注预览" sandbox referrerpolicy="no-referrer" style="width:100%;min-height:180px;border:1px solid #dce8e4"></iframe>`,
  onReady(target){target.querySelector('[data-draft-preview-remark]').srcdoc='<!doctype html><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src &#39;none&#39;; style-src &#39;unsafe-inline&#39;; img-src data:; form-action &#39;none&#39;; base-uri &#39;none&#39;"><body>'+String(row.fields.Remark??'')+'</body>'},
  ...(row.writable===true?{async onSubmit(form){const result=await context.api.request(base+'/submit',{method:'POST',data:{hospitalId,expectedToken:row.editToken,requestKey}});if(Number(result?.orderId)!==id||Number(result?.sampleStatus)!==0)throw Error('提交结果待核对，请按原内容重试');if(form.isConnected){context.toast('送检已提交，等待审核');await onSaved?.()}}}:{})
 });
}
