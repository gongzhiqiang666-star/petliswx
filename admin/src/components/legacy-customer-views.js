export function customerViewCommand(kind,value,month='',userNo=''){
 const reference=String(value??'').trim();if(!['CID','HOSPITAL','MONTH'].includes(kind)||!reference)throw Error('请填写有效编号或旧链接参数');
 if(/^https?:\/\//i.test(reference)){
  const url=new URL(reference),cid=url.searchParams.get('CID'),hospital=url.searchParams.get('HospID')??url.searchParams.get('hospid');
  if(/detailmonth/i.test(url.pathname)){if(!hospital||!url.searchParams.get('paymonth'))throw Error('月份链接缺少医院或月份参数');return {kind:'MONTH',token:hospital,month:url.searchParams.get('paymonth'),userNo:url.searchParams.get('userno')??''};}
  if(cid)return {kind:'CID',token:cid};if(hospital)return kind==='MONTH'?{kind:'MONTH',token:hospital,month,userNo}:{kind:'HOSPITAL',token:hospital};throw Error('链接缺少CID或HospID参数');
 }
 if(reference.includes(':'))throw Error('旧链接参数无效');
 let command;
 if(/^[0-9]+$/.test(reference)){const id=Number(reference);if(!Number.isSafeInteger(id)||id<1||id>4294967295)throw Error('编号参数无效');command={kind,id};}else command={kind,token:reference};
 return kind==='MONTH'?{...command,month,userNo}:command;
}
