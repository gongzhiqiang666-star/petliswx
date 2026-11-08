import {escapeHtml} from '../utils.js';
import {field,renderDetail} from './ui.js';
const base='/admin/legacy/order-adoption';
const read=(form,name)=>form.elements.namedItem?form.elements.namedItem(name):form.elements[name];
/** The attempt belongs to this page's administrator session, not a global account cache. */
export async function openLegacyAdoption(context,id,onSaved,attempt={}){
 const data=await context.api.request(base+'/'+id);
 const summary=renderDetail([{label:'样本号',value:data.orderNo||'尚未填写'},{label:'医院',value:data.hospitalName},{label:'动物',value:data.animalName},{label:'原送检医师',value:data.doctorName||'未记录'},{label:'原单总额',value:data.storedAmount===''?'未记录':data.storedAmount+' 元'},{label:'原明细合计',value:data.detailAmount+' 元'}]);
 const problems=(data.problems||[]).length?`<ul>${data.problems.map(problem=>`<li>${escapeHtml(problem)}</li>`).join('')}</ul>`:'';
 const bound=data.alreadyBound?`<p>当前已接入：医生 ${escapeHtml(data.boundDoctorName||'待核对')}，实验室 ${escapeHtml(data.boundLaboratoryName||'待核对')}。</p>`:'';
 if(!data.ready||!data.writable){context.dialog({title:'核对旧单接入审核',wide:true,body:summary+bound+problems+'<p class="muted">先处理上述事项，再重新打开核对；需要补编号时，使用列表中的“编辑主单”。</p>'});return;}
 const doctorOptions=[{value:'',label:'请选择本院医生账号'},...data.doctors.map(row=>({value:String(row.id),label:row.name}))];
 const labOptions=[{value:'',label:'请选择承接实验室'},...data.laboratories.map(row=>({value:String(row.id),label:row.name}))];
 attempt.requestKey??=crypto.randomUUID();
 context.dialog({title:'确认旧单接入审核',wide:true,submitLabel:'确认接入待审核',body:summary+
  (data.fillsZeroAmount?'<p class="form-hint">原单尚未归属医生且总额为零。确认后按原明细合计填入总额，检测项目单价不变。</p>':'<p class="muted">保留原金额与单号，不重新定价。</p>')+
  `<div class="form-grid">${field({name:'doctorId',label:'接收订单的医生账号',required:true,value:attempt.command?.doctorId||data.assignedDoctorId||'',options:doctorOptions})}${field({name:'laboratoryId',label:'承接实验室',required:true,value:attempt.command?.laboratoryId||'',options:labOptions})}</div><label class="form-hint"><input type="checkbox" name="confirmed" required style="width:auto"> 我已核对原送检医师、所选账号、实验室及 ${escapeHtml(data.detailAmount)} 元明细合计。</label>`+
  (attempt.command&&!attempt.receipt?'<p class="muted">上次结果未确认，请保持原选择重试，或关闭后重新核对当前单据。</p>':''),
  async onSubmit(form){
   if(read(form,'confirmed')?.checked!==true)throw Error('请先确认医师、账号、实验室和原金额');
   const doctorId=Number(read(form,'doctorId')?.value),laboratoryId=Number(read(form,'laboratoryId')?.value);
   if(!data.doctors.some(row=>Number(row.id)===doctorId)||!data.laboratories.some(row=>Number(row.id)===laboratoryId))throw Error('请选择本院医生和可用实验室');
   if(Number(data.assignedDoctorId)>0&&doctorId!==Number(data.assignedDoctorId))throw Error('原医生已归属，不能在接入时更换');
   attempt.requestKey??=crypto.randomUUID();
   const command={doctorId,laboratoryId,confirmedAmount:data.detailAmount,expectedToken:data.editToken,requestKey:attempt.requestKey,confirmed:true};
   if(attempt.command&&JSON.stringify(attempt.command)!==JSON.stringify(command))throw Error('上次结果未确认，请先使用原选择重试或关闭后重新核对');
   attempt.command??=command;
   let result;try{result=await context.api.request(base+'/'+id,{method:'POST',data:attempt.command});}catch(error){const code=Number(error.code||error.status);if([400,401,403,404,409,422].includes(code)){attempt.command=null;attempt.requestKey=null;}throw error;}
   if(Number(result?.orderId)!==Number(id)||result?.managed!==true)throw Error('接入结果待核对，请重新打开查看当前绑定');
   attempt.receipt=result;context.toast('已接入待审核，尚未执行审核');await onSaved?.();
  }
 });
}
