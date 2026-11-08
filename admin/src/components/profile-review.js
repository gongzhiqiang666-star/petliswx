import {escapeHtml} from '../utils.js';
import {renderTable} from './ui.js';
function object(value){try{const parsed=typeof value==='string'?JSON.parse(value):value;return parsed && typeof parsed==='object'&&!Array.isArray(parsed)?parsed:{}}catch{return {}}}
export function profileReviewSummary(row){const r=object(row.requestedData);return [r.nickname,r.title,row.requestedHospitalName||r.hospitalId].filter(x=>x!==undefined&&x!==null&&x!=='').map(escapeHtml).join(' · ');}
export function profileReviewDetails(row){
 const before=object(row.previousData),after=object(row.requestedData),cross=Number(before.hospitalId)!==Number(after.hospitalId);
 const rows=[{name:'医生姓名',before:before.nickname,after:after.nickname},{name:'职称',before:before.title,after:after.title},{name:'所属医院',before:before.hospitalName||before.hospitalId,after:row.requestedHospitalName||after.hospitalId},{name:'头像',before:before.avatar?'已有头像':'未设置',after:after.avatar===before.avatar?'保持原头像':after.avatar?'申请新头像':'未设置'}];
 const files=Array.isArray(after.qualificationFiles)?after.qualificationFiles:[];
 return (cross?'<p class="form-hint">跨医院申请通过后，普通医生的医院权限将同步变更。含管理或多机构权限的账号需先核对角色，避免误转移权限。</p>':'')+renderTable({rows,columns:[{label:'资料项',key:'name'},{label:'修改前',key:'before'},{label:'申请修改为',key:'after'}]})+'<h3>申请资质文件</h3>'+files.map((url,index)=>/^\/files\/qualifications\/[a-f0-9]{32}\.(pdf|png|jpg)$/.test(url)?`<button type="button" class="button secondary" data-profile-qualification="${escapeHtml(url)}">下载资质 ${index+1}</button>`:'<span class="form-hint">无效资质文件地址，请核对</span>').join('');
}
export function bindProfileFiles(form,api,toast){form.querySelectorAll('[data-profile-qualification]').forEach(button=>button.addEventListener('click',async()=>{button.disabled=true;try{const result=await api.download(button.dataset.profileQualification),url=URL.createObjectURL(result.blob),link=document.createElement('a');link.href=url;link.download='资质文件.'+button.dataset.profileQualification.split('.').pop();link.click();URL.revokeObjectURL(url);}catch(e){toast(e.message,'error');}finally{button.disabled=false;}}));}
