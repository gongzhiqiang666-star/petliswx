import {field} from './ui.js';
import {formData} from '../pages/shared.js';

export function dictionaryCommand(schema,values,requestKey,expectedToken=null){
 const name=String(values.name??''),englishName=String(values.englishName??'');
 if(!name.trim()||!englishName.trim())throw Error('中英文名称均不能为空');
 if(Array.from(name).length>schema.maxNameLength||Array.from(englishName).length>schema.maxNameLength)throw Error(`名称最多${schema.maxNameLength}个字符`);
 const active=schema.hasStatus===false?null:Number(values.active??schema.createDefaultIsActive??0);if(schema.hasStatus!==false&&![0,1].includes(active))throw Error('请选择有效状态');
 return {name,englishName,active,requestKey,expectedToken};
}
export async function openDictionaryEditor(context,schema,row,onSaved){
 const id=row?.[schema.columns[0].key];
 const record=id?await context.api.request('/admin/legacy/dictionaries/'+encodeURIComponent(schema.kind)+'/'+id):null;
 const requestKey=globalThis.crypto.randomUUID(),expectedToken=record?.editToken??null;
 const body=`<div class="form-grid">${field({name:'name',label:schema.columns[1].label,value:record?.[schema.columns[1].key]??'',required:true})}${field({name:'englishName',label:schema.columns[2].label,value:record?.[schema.columns[2].key]??'',required:true})}${schema.hasStatus===false?'':field({name:'active',label:'状态',value:record?.IsActive??schema.createDefaultIsActive,options:[{value:'1',label:'正常'},{value:'0',label:'禁用'}]})}</div><button class="button secondary" type="reset">重置</button>`;
 context.dialog({title:(record?'编辑':'添加')+schema.label,body,submitLabel:'确定',onReady(form){form.addEventListener('reset',()=>{const error=form.querySelector('#dialog-error');if(error)error.textContent=''})},async onSubmit(form){
  const data=dictionaryCommand(schema,formData(form),requestKey,expectedToken);
  await context.api.request('/admin/legacy/dictionaries/'+encodeURIComponent(schema.kind)+(id?'/'+id:''),{method:id?'PUT':'POST',data});
  context.toast('保存成功');await onSaved();
 }});
}
