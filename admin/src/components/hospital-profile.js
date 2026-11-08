import {field} from './ui.js';
import {formData} from '../pages/shared.js';
export function hospitalCityOptions(cities,selected){
 const rows=Array.isArray(cities)?cities:[],byId=new Map(rows.map(row=>[Number(row.cityId),row]));
 const options=[{value:0,label:'未选择地区'}];
 for(const row of rows){const names=[],seen=new Set();let current=row;while(current&&!seen.has(Number(current.cityId))){seen.add(Number(current.cityId));names.unshift(String(current.cityName??''));current=byId.get(Number(current.belongCityCityId));}options.push({value:row.cityId,label:names.join(' / ')});}
 const id=Number(selected||0);if(id>0&&!byId.has(id))options.push({value:id,label:`原地区 #${id}（保留历史选择）`});return options;
}
export async function openHospitalProfile(context,id,onSaved){
 const base='/admin/legacy/hospital-profiles',creating=id==null;
 const [capabilities,row,cities]=await Promise.all([context.api.request(base+'/capabilities'),creating?Promise.resolve({CityId:0,PayType:0}):context.api.request(`${base}/${id}`),context.api.request('/dictionary/cities')]);
 if((creating?capabilities.hospitalProfileCreates:capabilities.hospitalProfileWrites)!==true)throw Error('医院档案维护尚未开启');
 const key=crypto.randomUUID();let submitted=null;
 const fields=[['name','医院名称','HospitalName'],['englishName','医院英文名','HospitalName_en'],['invoice','发票抬头','Invoice'],['cityId','地区','CityId'],['place','所在地区','Place'],['address','详细地址','Adress'],['tel','电话','Tel'],['fax','传真','Fax']];
 context.dialog({title:creating?'新增医院':'编辑医院档案',submitLabel:'保存档案',body:`<div class="form-grid">${fields.map(([name,label,column])=>field({name,label,value:row[column]??'',required:name==='name'||creating&&name==='englishName',type:'text',options:name==='cityId'?hospitalCityOptions(cities,row.CityId):undefined})).join('')}${field({name:'payType',label:'结算方式',value:row.PayType,options:[{value:0,label:'次结'},{value:1,label:'月结'}]})}${field({name:'active',label:'状态',value:creating?0:row.IsActive,options:[{value:0,label:'禁用'},{value:1,label:'正常'}]})}${creating?field({name:'loginName',label:'原医院登录名'})+field({name:'loginPassword',label:'原医院登录密码',type:'password'}):''}</div>`,async onSubmit(form){
  const data=formData(form),{loginName,loginPassword,active,...profile}=data,command={...profile,cityId:Number(data.cityId),payType:Number(data.payType),expectedToken:row.editToken,requestKey:key,...(creating?{}:{active:Number(active)})},payload=creating?{profile:command,loginName,loginPassword,active:Number(active)}:command,canonical=JSON.stringify(payload);
  if(submitted&&submitted!==canonical)throw Error('上次保存结果未确认，请用原内容重试或重新打开核对');submitted=canonical;
  try{await context.api.request(creating?base:`${base}/${id}`,{method:creating?'POST':'PUT',data:payload});}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))submitted=null;throw error;}
  if(form.isConnected){context.toast('医院档案已保存');await onSaved?.();}
 }});
}

export async function openHospitalCredentials(context,id){
 const base='/admin/legacy/hospital-profiles';
 const [capabilities,row]=await Promise.all([context.api.request(base+'/capabilities'),context.api.request(`${base}/${id}/credentials`)]);
 if(capabilities.hospitalCredentialWrites!==true)throw Error('原医院登录资料维护尚未开启');
 const requestKey=crypto.randomUUID();let pending=null;
 context.dialog({title:'原医院登录资料',submitLabel:'保存登录资料',body:`<p>维护原医院登录资料，不会创建会员账号或授予新权限。${row.passwordConfigured?'当前已设置密码。':'当前未设置密码。'}</p><div class="form-grid">${field({name:'loginName',label:'原登录名',value:row.loginName??''})}${field({name:'password',label:'新密码（留空保留）',type:'password'})}${field({name:'clearPassword',label:'清空原密码',value:'false',options:[{value:'false',label:'否，保留或修改'},{value:'true',label:'是，清空密码'}]})}</div>`,async onSubmit(form){
  const values=formData(form),command={loginName:values.loginName,password:values.password||null,clearPassword:values.clearPassword==='true',requestKey,expectedToken:row.editToken};
  if(command.clearPassword&&command.password)throw Error('不能同时填写新密码和清空密码');
  const canonical=JSON.stringify(command);if(pending&&pending!==canonical)throw Error('上次结果未确认，请用原内容重试或重新打开核对');pending=canonical;
  try{await context.api.request(`${base}/${id}/credentials`,{method:'PUT',data:command});}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))pending=null;throw error;}
  context.toast('原医院登录资料已保存');
 }});
}
