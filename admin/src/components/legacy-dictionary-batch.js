import {renderTable} from './ui.js';
import {escapeHtml} from '../utils.js';

export async function openDictionaryBatch(context,schema,rows,operation,onSaved){
 const deleting=operation==='delete';
 if(deleting?schema.canDelete!==true:schema.canWrite!==true)throw Error('当前未开放此操作');
 if(!deleting&&(schema.hasStatus===false||![0,1].includes(operation)))throw Error('状态无效');
 if(!Array.isArray(rows)||!rows.length||rows.length>1000)throw Error('请先选择1至1000条记录');
 const pk=schema.columns[0].key,ids=rows.map(row=>Number(row[pk]));
 if(new Set(ids).size!==ids.length||ids.some(id=>!Number.isInteger(id)||id<1||id>(schema.kind==='variety'?Number.MAX_SAFE_INTEGER:4294967295)))throw Error('记录选择无效，请刷新重试');
 const records=await Promise.all(ids.map(id=>context.api.request('/admin/legacy/dictionaries/'+encodeURIComponent(schema.kind)+'/'+id)));
 const targets=records.map((row,index)=>{if(Number(row[pk])!==ids[index]||!row.editToken)throw Error('记录快照无效，请刷新重试');return {id:ids[index],editToken:row.editToken}});
 const requestKey=globalThis.crypto.randomUUID();
 const summary=deleting?'将永久删除以下记录，不能在此页面恢复。':`将以下记录的状态设为“${operation===1?'正常':'禁用'}”。`;
 const body=`<p>${escapeHtml(summary)}</p>`+renderTable({rows:records,columns:[...schema.columns.slice(0,3).map(c=>({label:c.label,key:c.key})),...(schema.hasStatus===false?[]:[{label:'当前状态',render:row=>escapeHtml(row.IsActive===1?'正常':row.IsActive===0?'禁用':row.IsActive)}])]});
 context.dialog({title:deleting?'确认删除'+schema.label:'批量修改'+schema.label,body,wide:true,submitLabel:deleting?'确认删除':'确定',async onSubmit(){
  const result=await context.api.request('/admin/legacy/dictionaries/'+encodeURIComponent(schema.kind)+(deleting?'/delete':'/status'),{method:'POST',data:{targets,active:deleting?null:operation,requestKey}});
  context.toast(`已处理 ${Number(result.count)} 条记录`);await onSaved();
 }});
}
