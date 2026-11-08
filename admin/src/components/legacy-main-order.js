import {mountMainOrderChoices} from './legacy-main-order-choices.js';
import {field} from './ui.js';
import {formData} from '../pages/shared.js';
const groups=[['送检资料',[['UserNO','申请单号'],['CreateDataTypeIId','客户数据组'],['InfoHospitalId','医院'],['CityId','地区 ID'],['DoctorName','医生'],['MarketingName','市场人员'],['SampleStatus','原状态值']]],['动物与主人',[['AnimalName','动物名称'],['AnimalName_en','动物英文名'],['InfoAnimalId','物种'],['AnimalTypeId','品种选择'],['AnimalType','品种'],['AnimalType_en','品种英文名'],['DictSexId','性别'],['Age','年龄'],['Birthday','出生日期'],['ChipNO','芯片号'],['MasterName','主人姓名'],['MasterName_en','主人英文名']]],['标本与报告',[['InfoSampleTypeId','样本类型'],['SampleArea','采样部位'],['CollectDt','采样日期'],['ReceiveSampleDt','收样日期'],['SendSampleDt','寄样日期'],['SendReportDt','报告日期'],['ReportNO','报告编号'],['DiagnosticResults','诊断结果'],['Remark','备注']]]];
const dates=new Set(['Birthday','CollectDt','ReceiveSampleDt','SendSampleDt','SendReportDt']);
export function mainOrderValues(row){return Object.fromEntries(groups.flatMap(([,fields])=>fields.map(([name])=>{let value=String(row[name]??'');if(dates.has(name))value=value.startsWith('1900-01-01')?'':value.replace('T',' ');return [name,value];})));}
export async function openLegacyMainOrder(context,id,onSaved){
 const base='/admin/legacy/main-orders';const [capabilities,row,editorModule]=await Promise.all([context.api.request(base+'/capabilities'),context.api.request(`${base}/${id}`),import('../../assets/legacy-editor.js')]);
 if(capabilities.mainOrderWrites!==true)throw Error('原主单维护尚未开启');
 const before=mainOrderValues(row),requestKey=crypto.randomUUID();let submitted=null,remarkEditor=null;
 context.dialog({title:'编辑送检主单',wide:true,submitLabel:'保存主单',body:`<p class="muted">沿用原主单业务字段。日期留空会保留原值；费用、付款和检测明细在对应业务中维护。</p>${groups.map(([label,fields])=>`<h3>${label}</h3><div class="form-grid">${fields.map(([name,label])=>name==='Remark'?'<section class="form-field wide"><h3>备注</h3><div data-main-remark-editor></div><p data-main-remark-feedback role="status"></p></section>':field({name,label,value:before[name],type:name==='DiagnosticResults'?'textarea':'text',wide:name==='DiagnosticResults'})).join('')}</div>`).join('')}`,onReady(form){mountMainOrderChoices(form,context.api);try{remarkEditor=editorModule.mountEditor(form.querySelector('[data-main-remark-editor]'),before.Remark);}catch(error){form.querySelector('[data-main-remark-feedback]').textContent='当前备注含暂不能安全编辑的内容，已保留原样；可继续修改其他资料。'+error.message;}},onClose(){remarkEditor?.destroy();},async onSubmit(form){
  if(Number(form.dataset.mainNumberPending||0)>0)throw Error('请等待数据组单号计算完成');if(form.dataset.mainNumberError)throw Error('数据组单号尚未成功计算，请重新选择数据组后再保存');
  const values=formData(form);if(remarkEditor?.isDirty())values.Remark=remarkEditor.getHTML();const fields=Object.fromEntries(Object.entries(values).filter(([name,value])=>value!==before[name]));if(!Object.keys(fields).length)throw Error('没有需要保存的修改');
  const command={fields,expectedToken:row.editToken,requestKey},canonical=JSON.stringify(command);if(submitted&&submitted!==canonical)throw Error('上次结果未确认，请先用原内容重试或重新打开核对');submitted=canonical;
  try{await context.api.request(`${base}/${id}`,{method:'PUT',data:command});}catch(error){if([400,401,403,404,422].includes(error.status)||error.status===200&&[400,401,403,404,422].includes(Number(error.code)))submitted=null;throw error;}
  if(form.isConnected){context.toast('主单已保存');await onSaved?.();}
 }});
}
