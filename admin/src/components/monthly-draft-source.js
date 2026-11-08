const base='/admin/legacy/monthly-reports';
function timestamp(value){
 const text=String(value??'');if(!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(text))throw Error('请填写收样起止时间');
 return text.length===16?text+':00':text;
}
export function monthlyDraftSource(hospital,month,range){
 if(!Number.isSafeInteger(hospital)||hospital<1||hospital>2147483647)throw Error('请填写有效医院编号');
 if(range){const from=timestamp(range.from),to=timestamp(range.to);if(from>to)throw Error('起始收样时间不能晚于结束时间');return {path:`${base}/${hospital}/range-draft`,query:{from,to}};}
 if(typeof month!=='string'||!/^\d{4}-\d{2}$/.test(month))throw Error('请选择有效账单月份');
 return {path:`${base}/${hospital}/${month}/draft`,query:{}};
}
