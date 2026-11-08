import {saveHospitalReport} from './hospital-report.js';
export async function exportVisibleOrderDetails(api,status){
 let query='';if(status!==''&&status!=null){const number=Number(status);if(!Number.isInteger(number)||number<-2147483648||number>2147483647)throw Error('订单状态无效');query='?status='+encodeURIComponent(String(number));}
 const file=await api.download('/admin/legacy/order-export'+query);
 if(file.blob?.type?.split(';')[0].toLowerCase()!=='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'||await file.blob.slice(0,2).text()!=='PK')throw Error('未取得有效Excel文件，请重试');return file;
}
export async function downloadVisibleOrderDetails(context,status){const file=await exportVisibleOrderDetails(context.api,status);saveHospitalReport(file);context.toast('明细已导出；范围、总数和单次上限见“导出说明”工作表');}
