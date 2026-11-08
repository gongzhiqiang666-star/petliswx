import {field,renderTable} from './ui.js';
import {formData} from '../pages/shared.js';
export const testCategories=[{value:0,label:'内部'},{value:1,label:'细胞学&组织病理'},{value:2,label:'过敏原'},{value:3,label:'微生物'},{value:4,label:'综合(其他项目)'}];
const states=[{value:1,label:'正常'},{value:0,label:'禁用'}];
export function catalogItemCommand(values,requestKey,expectedToken=null){
 const name=String(values.name??''),code=String(values.code??''),type=Number(values.type??0),active=Number(values.active??1);
 if(!name.trim()||!code.trim()||Array.from(name).length>250||Array.from(code).length>250)throw Error('检测项目名和检测编号不能为空且最多250字');
 if(!testCategories.some(v=>v.value===type)||![0,1].includes(active))throw Error('所属大类或状态无效');return {name,code,type,active,requestKey,expectedToken};
}
export function catalogPriceCommand(values,requestKey){const price=String(values.price??'').trim(),active=Number(values.active??1);if(!/^-?\d{1,16}(\.\d{1,2})?$/.test(price)||![0,1].includes(active))throw Error('价格最多两位小数，状态须有效');return {price,active,requestKey};}
export function catalogPriceEditCommand(values,requestKey,expectedToken){const price=String(values.price??'').trim(),active=values.active===undefined?null:Number(values.active);if(!/^-?\d{1,16}(\.\d{1,4})?$/.test(price)||(active!==null&&![0,1].includes(active)))throw Error('编辑价格最多四位小数，状态须有效');return {price,active,requestKey,expectedToken};}
export async function openCatalogItem(context,id,onSaved){
 const row=id?await context.api.request('/admin/legacy/test-catalog/items/'+id):null,requestKey=globalThis.crypto.randomUUID();
 context.dialog({title:row?'编辑检测项目':'添加检测项目',body:`<div class="form-grid">${field({name:'name',label:'检测项目名',required:true,value:row?.TestName??''})}${field({name:'code',label:'检测编号',required:true,value:row?.TestNO??''})}${field({name:'type',label:'所属大类',options:testCategories,value:row?.type??0})}${field({name:'active',label:'状态',options:states,value:row?.IsActive??1})}</div><button class="button secondary" type="reset">重置</button>`,submitLabel:'确定',async onSubmit(form){await context.api.request('/admin/legacy/test-catalog/items'+(id?'/'+id:''),{method:id?'PUT':'POST',data:catalogItemCommand(formData(form),requestKey,row?.editToken??null)});context.toast('项目已保存');await onSaved();}});
}
export async function openCatalogPrices(context,id,canWrite,onSaved,canDelete=false){
 const data=await context.api.request('/admin/legacy/test-catalog/items/'+id+'/prices',{query:{page:0,size:100}}),requestKey=globalThis.crypto.randomUUID();
 const table=rows=>renderTable({rows,columns:[{label:'Id',key:'IID'},{label:'价格',key:'price'},{label:'状态',render:r=>r.IsActive===1?'正常':'禁用'}],actions:canWrite?(r=>`<button type="button" class="button text" data-price-edit="${Number(r.IID)}">编辑</button>${canDelete?`<button type="button" class="button danger text" data-price-delete="${Number(r.IID)}">删除</button>`:''}`):undefined});
 const options={title:'维护多段价格',wide:true,body:`<div data-catalog-prices>${table(data.items??[])}</div><div class="toolbar"><span data-catalog-price-page>共 ${Number(data.total)} 条 · 第 1 页</span><button type="button" class="button secondary" data-price-prev disabled>上一页</button><button type="button" class="button secondary" data-price-next ${Number(data.total)<=100?'disabled':''}>下一页</button></div>${canWrite?`<div class="form-grid">${field({name:'price',label:'价格（元）',type:'number',step:'0.01',required:true})}${field({name:'active',label:'状态',value:1,options:states})}</div>`:''}`,onReady(target){let page=0,busy=false;target.addEventListener('click',async e=>{const b=e.target.closest('[data-price-prev],[data-price-next],[data-price-edit],[data-price-delete]');if(!b||b.disabled||busy)return;if(b.hasAttribute('data-price-edit')||b.hasAttribute('data-price-delete')){busy=true;try{await openCatalogPriceChange(context,id,Number(b.dataset.priceEdit||b.dataset.priceDelete),b.hasAttribute('data-price-delete'),onSaved)}catch(error){context.toast(error.message,'error')}finally{busy=false}return;}busy=true;const next=page+(b.hasAttribute('data-price-next')?1:-1);try{const r=await context.api.request('/admin/legacy/test-catalog/items/'+id+'/prices',{query:{page:next,size:100}});page=next;target.querySelector('[data-catalog-prices]').innerHTML=table(r.items??[]);target.querySelector('[data-catalog-price-page]').textContent=`共 ${Number(r.total)} 条 · 第 ${page+1} 页`;target.querySelector('[data-price-prev]').disabled=page===0;target.querySelector('[data-price-next]').disabled=(page+1)*100>=Number(r.total);}catch(error){context.toast(error.message,'error')}finally{busy=false}})}};
 if(canWrite){options.submitLabel='添加价格';options.onSubmit=async form=>{await context.api.request('/admin/legacy/test-catalog/items/'+id+'/prices',{method:'POST',data:catalogPriceCommand(formData(form),requestKey)});context.toast('价格已添加');await onSaved();};}context.dialog(options);
}

export async function openCatalogPriceChange(context,testId,priceId,deleting,onSaved){
 const base='/admin/legacy/test-catalog/items/'+testId+'/prices/'+priceId;
 const row=await context.api.request(base),requestKey=globalThis.crypto.randomUUID();
 const body=deleting?'<p>将永久删除这条价格记录，不能在此页面恢复。</p>'+renderTable({rows:[row],columns:[{label:'Id',key:'IID'},{label:'价格',key:'price'},{label:'状态',render:r=>r.IsActive===1?'正常':'禁用'}]}):`<div class="form-grid">${field({name:'price',label:'价格（元）',type:'number',step:'0.0001',required:true,value:row.price})}${field({name:'active',label:'状态',options:states,value:row.IsActive})}</div>`;
 context.dialog({title:deleting?'确认删除价格':'编辑价格',body,submitLabel:deleting?'确认删除':'保存价格',async onSubmit(form){
  const data=deleting?{requestKey,expectedToken:row.editToken}:catalogPriceEditCommand(formData(form),requestKey,row.editToken);
  await context.api.request(base+(deleting?'/delete':''),{method:deleting?'POST':'PUT',data});context.toast(deleting?'价格已删除':'价格已保存');await onSaved();
 }});
}
