export function customerViewCommand(kind,value){
 const reference=String(value??'').trim();if(!['CID','HOSPITAL'].includes(kind)||!reference)throw Error('请填写有效编号或旧链接参数');
 if(/^https?:\/\//i.test(reference)){
  const url=new URL(reference);if(/detailmonth/i.test(url.pathname))throw Error('月详情链接尚未接通，请使用医院或明细链接参数');
  const cid=url.searchParams.get('CID'),hospital=url.searchParams.get('HospID')??url.searchParams.get('hospid');
  if(cid)return {kind:'CID',token:cid};if(hospital)return {kind:'HOSPITAL',token:hospital};throw Error('链接缺少CID或HospID参数');
 }
 if(reference.includes(':'))throw Error('旧链接参数无效');
 if(/^[0-9]+$/.test(reference)){const id=Number(reference);if(!Number.isSafeInteger(id)||id<1||id>4294967295)throw Error('编号参数无效');return {kind,id};}
 return {kind,token:reference};
}
