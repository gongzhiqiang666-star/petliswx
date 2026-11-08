export async function downloadHospitalReport(api,hospitalId,detailId){
 if(!Number.isSafeInteger(hospitalId)||hospitalId<1||!Number.isSafeInteger(detailId)||detailId<1||detailId>4294967295)throw Error('报告或医院编号无效');
 const file=await api.download(`/orders/hospital-history/reports/${detailId}/pdf?hospitalId=${hospitalId}`);
 if(file.blob?.type?.split(';')[0].toLowerCase()!=='application/pdf'||await file.blob.slice(0,5).text()!=='%PDF-')throw Error('服务器未返回有效PDF，原报告未被替换');return file;
}
export function previewHospitalReport(context,file){const url=URL.createObjectURL(file.blob);context.dialog({title:'历史检测报告',wide:true,body:'<iframe data-hospital-report-frame title="历史检测报告PDF" style="width:100%;height:65vh;border:0"></iframe>',onReady(target){target.querySelector('[data-hospital-report-frame]').src=url},onClose(){URL.revokeObjectURL(url)}})}
export function saveHospitalReport(file){const url=URL.createObjectURL(file.blob),link=document.createElement('a');link.href=url;link.download=file.filename||'检测报告.pdf';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
