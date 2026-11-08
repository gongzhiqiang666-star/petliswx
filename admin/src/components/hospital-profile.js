import {field} from './ui.js';
import {formData} from '../pages/shared.js';
export async function openHospitalProfile(context,id,onSaved){
 const base='/admin/legacy/hospital-profiles',creating=id==null;
 const [capabilities,row]=await Promise.all([context.api.request(base+'/capabilities'),creating?Promise.resolve({CityId:0,PayType:0}):context.api.request(`${base}/${id}`)]);
 if((creating?capabilities.hospitalProfileCreates:capabilities.hospitalProfileWrites)!==true)throw Error('医院档案维护尚未开启');
 const key=crypto.randomUUID();let submitted=null;
 const fields=[['name','医院名称','HospitalName'],['englishName','医院英文名','HospitalName_en'],['invoice','发票抬头','Invoice'],['cityId','地区 ID','CityId'],['place','所在地区','Place'],['address','详细地址','Adress'],['tel','电话','Tel'],['fax','传真','Fax']];
 context.dialog({title:creating?'新增医院':'编辑医院档案',submitLabel:'保存档案',body:`<div class="form-grid">${fields.map(([name,label,column])=>field({name,label,value:row[column]??'',required:name==='name'||creating&&name==='englishName',type:name==='cityId'?'number':'text',min:name==='cityId'?0:undefined})).join('')}${field({name:'payType',label:'结算方式',value:row.PayType,options:[{value:0,label:'次结'},{value:1,label:'月结'}]})}${creating?field({name:'active',label:'状态',value:0,options:[{value:0,label:'禁用'},{value:1,label:'正常'}]})+field({name:'loginName',label:'原医院登录名'})+field({name:'loginPassword',label:'原医院登录密码',type:'password'}):''}</div>`,async onSubmit(form){
  const data=formData(form),{loginName,loginPassword,active,...profile}=data,command={...profile,cityId:Number(data.cityId),payType:Number(data.payType),expectedToken:row.editToken,requestKey:key},payload=creating?{profile:command,loginName,loginPassword,active:Number(active)}:command,canonical=JSON.stringify(payload);
  if(submitted&&submitted!==canonical)throw Error('上次保存结果未确认，请用原内容重试或重新打开核对');submitted=canonical;
  try{await context.api.request(creating?base:`${base}/${id}`,{method:creating?'POST':'PUT',data:payload});}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))submitted=null;throw error;}
  if(form.isConnected){context.toast('医院档案已保存');await onSaved?.();}
 }});
}
