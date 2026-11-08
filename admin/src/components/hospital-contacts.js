import {field,renderTable} from './ui.js';
import {formData} from '../pages/shared.js';
const base='/admin/legacy/hospital-contacts';
export async function openHospitalContacts(context,hospital){
 const capabilities=await context.api.request(base+'/capabilities');
 let page=0;const deletionRequests=new Map();
 const load=p=>context.api.request(`${base}/${hospital}`,{query:{page:p,size:20}});
 const table=data=>renderTable({rows:data.items,columns:[{key:'Name',label:'姓名'},{key:'Tel',label:'电话'},{key:'Mail',label:'邮箱'},{key:'Fax',label:'传真'},{key:'isFinanceContact',label:'财务专用（1=是）'},{key:'ContactRemark',label:'备注'}],actions:capabilities.hospitalContactWrites===true?row=>`<button type="button" class="button text" data-contact-edit="${Number(row.InfoHospitalContactId)}">编辑</button>${capabilities.hospitalContactDeletes===true?`<button type="button" class="button danger text" data-contact-delete="${Number(row.InfoHospitalContactId)}">删除</button>`:''}`:undefined});
 const initial=await load(0);
 context.dialog({title:'医院联系人',wide:true,body:`${capabilities.hospitalContactWrites===true?'<button type="button" class="button" data-contact-new>新增联系人</button>':''}<div data-contact-list>${table(initial)}</div><div class="toolbar"><span data-contact-page>共 ${Number(initial.total)} 条</span><button type="button" class="button secondary" data-contact-prev disabled>上一页</button><button type="button" class="button secondary" data-contact-next ${initial.total<=20?'disabled':''}>下一页</button></div>`,onReady(target){let busy=false;target.addEventListener('click',async event=>{
  const button=event.target.closest('[data-contact-delete],[data-contact-edit],[data-contact-new],[data-contact-prev],[data-contact-next]');if(!button||busy||button.disabled)return;busy=true;
  try{
   if(button.hasAttribute('data-contact-delete')){
    const id=Number(button.dataset.contactDelete);let command=deletionRequests.get(id);
    if(!command){const row=await context.api.request(`${base}/${hospital}/${id}`);if(!globalThis.confirm('确认删除该联系人？删除后将无法再从名单中选择此人。'))return;command={requestKey:crypto.randomUUID(),expectedToken:row.editToken};deletionRequests.set(id,command);}
    try{await context.api.request(`${base}/${hospital}/${id}`,{method:'DELETE',data:command});}catch(error){if(error.status===409)deletionRequests.delete(id);throw error;}
    deletionRequests.delete(id);context.toast('联系人已删除');await openHospitalContacts(context,hospital);
   }else if(button.hasAttribute('data-contact-new')||button.hasAttribute('data-contact-edit')){
    const id=button.dataset.contactEdit;const row=id?await context.api.request(`${base}/${hospital}/${id}`):{};
    const key=crypto.randomUUID();let submitted=null;
    context.dialog({title:id?'编辑医院联系人':'新增医院联系人',body:`<div class="form-grid">${field({name:'name',label:'姓名',value:row.Name??''})}${field({name:'tel',label:'电话',value:row.Tel??''})}${field({name:'mail',label:'邮箱',value:row.Mail??'',required:true})}${field({name:'fax',label:'传真',value:row.Fax??''})}${field({name:'financeContact',label:'财务专用',value:row.isFinanceContact??0,options:[{value:0,label:'否'},{value:1,label:'是'}]})}${field({name:'remark',label:'备注',value:row.ContactRemark??'',type:'textarea',wide:true})}</div>`,submitLabel:'保存联系人',async onSubmit(form){
     const values=formData(form),command={...values,financeContact:Number(values.financeContact),requestKey:key,expectedToken:row.editToken??null};const canonical=JSON.stringify(command);
     if(submitted&&submitted!==canonical)throw Error('上次保存结果未确认，请先用原内容重试或重新打开核对');submitted=canonical;
     try{await context.api.request(`${base}/${hospital}${id?'/'+id:''}`,{method:id?'PUT':'POST',data:command});}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))submitted=null;throw error;}
     if(form.isConnected){context.toast('联系人已保存');await openHospitalContacts(context,hospital);}
    }});
   }else{const next=page+(button.hasAttribute('data-contact-next')?1:-1),data=await load(next);page=next;target.querySelector('[data-contact-list]').innerHTML=table(data);target.querySelector('[data-contact-page]').textContent=`共 ${data.total} 条 · 第 ${page+1} 页`;target.querySelector('[data-contact-prev]').disabled=page===0;target.querySelector('[data-contact-next]').disabled=(page+1)*20>=data.total;}
  }catch(error){context.toast(error.message,'error');}finally{busy=false;}
 });}});
}
