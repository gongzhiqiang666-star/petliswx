export function dictionaryQuery(values,page=0){
 const q={name:values.name||'',englishName:values.englishName||'',sort:values.sort,order:values.order||'DESC',page,size:20};
 if(values.active!==undefined&&values.active!=='')q.active=Number(values.active);
 for(const key of ['createdFrom','createdTo','updatedFrom','updatedTo'])if(values[key]){
  const millis=new Date(values[key]).getTime();if(!Number.isFinite(millis))throw Error('请填写有效日期');q[key]=Math.floor(millis/1000);
 }
 for(const prefix of ['created','updated'])if(q[prefix+'From']!==undefined&&q[prefix+'To']!==undefined&&q[prefix+'From']>q[prefix+'To'])throw Error('开始时间不能晚于结束时间');
 return q;
}
export function legacyTimestamp(seconds){
 if(!seconds)return '无';const d=new Date(Number(seconds)*1000);if(!Number.isFinite(d.getTime()))return '无';
 const parts=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hourCycle:'h23'}).formatToParts(d);
 const p=Object.fromEntries(parts.map(x=>[x.type,x.value]));return `${p.year}-${p.month}-${p.day} ${p.hour}:${p.minute}:${p.second}`;
}
export function createDictionaryReader(api,onResult,onError){
 let sequence=0;
 return async(kind,query)=>{const version=++sequence;try{const result=await api.request('/admin/legacy/dictionaries/'+encodeURIComponent(kind),{query});if(version===sequence)onResult(result)}catch(error){if(version===sequence)onError(error)}};
}
