import {renderDetail,renderTable} from './ui.js';
export function historyQuery(hospitalId,filters={},page=0){
 const query={hospitalId,userNo:String(filters.userNo||'').trim(),status:Number(filters.status??-1),page,size:20},from=filters.from||'',to=filters.to||'';
 if(Boolean(from)!==Boolean(to)||from>to)throw Error('请选择完整且顺序正确的起止日期');
 if(from){for(const day of [from,to]){let valid=false;try{valid=/^\d{4}-\d{2}-\d{2}$/.test(day)&&new Date(day+'T00:00:00Z').toISOString().slice(0,10)===day}catch{}if(!valid)throw Error('日期无效')}query.from=from+'T00:00:00';query.to=to+'T23:59:59'}return query;
}
export async function openHospitalCancellation(context,hospitalId,row,onSaved,attempt={}){
 if(row.canCancel!==true||!row.cancelToken)throw Error('该送检单当前不可取消，请刷新核对');
 const signature=JSON.stringify({hospitalId,id:row.orderId,token:row.cancelToken});if(attempt.signature&&attempt.signature!==signature)throw Error('上次取消结果未确认，请刷新记录核对');
 attempt.signature=signature;attempt.key=attempt.key||crypto.randomUUID();
 context.dialog({title:'确认取消送检',submitLabel:'确认取消送检',body:`${renderDetail([{label:'样本编号',value:row.userNo||'未编号'},{label:'动物姓名',value:row.animalName},{label:'当前状态',value:row.statusLabel||'待审核'}])}<p>${row.paid?'该单已有付款标记。':''}取消后保留检测明细和资金记录，不自动退款。</p>`,async onSubmit(form){
  const result=await context.api.request('/orders/hospital-history/'+row.orderId+'/cancel',{method:'POST',data:{hospitalId,expectedToken:row.cancelToken,requestKey:attempt.key}});if(Number(result?.orderId)!==Number(row.orderId)||Number(result?.sampleStatus)!==4)throw Error('取消结果待核对，请按原内容重试');if(form.isConnected){context.toast('送检已取消');await onSaved?.()}
 }});
}
export function openHospitalHistoryDetails(context,row){context.dialog({title:'送检历史详情',wide:true,body:renderDetail([{label:'样本编号',value:row.userNo||'未编号'},{label:'动物姓名',value:row.animalName},{label:'畜主姓名',value:row.ownerName},{label:'送检医师',value:row.doctorName},{label:'创建时间',value:row.submittedAt},{label:'状态',value:row.statusLabel},{label:'付款标记',value:row.paid?'已付款':'未付款'},...(row.rejectionReason?[{label:'审核意见',value:row.rejectionReason,wide:true}]:[])])+renderTable({columns:[{key:'testName',label:'检测项目'},{key:'testNo',label:'项目编号'},{key:'reportState',label:'报告状态'}],rows:row.items||[],emptyText:'没有已匹配的检测明细'})+((row.items||[]).some(i=>i.canViewLegacyReport===true||i.reportState==='已出报告')?'<p class="muted">历史报告文件访问尚未就绪，请联系实验室获取报告。</p>':'')})}
