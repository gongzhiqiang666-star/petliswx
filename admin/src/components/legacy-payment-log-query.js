import {field} from './ui.js';
import {paymentLogFields} from './legacy-payment-logs.js';
export function paymentLogQuery(values={},page=0){
 const q={q:values.q??'',page,size:20,sort:values.sort||'PayLogId',order:values.order||'DESC'};
 for(const name of ['PayLogId','BuyMail','BuyerId','TradeNoAli','TradeNoUser','TradeStatus','Subject','TradeBody','Exterface'])if(values[name])q[name]=values[name];
 for(const name of ['amountMin','amountMax']){const value=String(values[name]??'').trim();if(!value)continue;if(!/^[+-]?\d{1,18}(\.\d{1,8})?$/.test(value))throw Error('金额条件须为十进制数');q[name]=value==='0'?'0.00':value;}
 for(const name of ['from','to']){let value=values[name];if(!value)continue;value=value.replace('T',' ');if(value.length===16)value+=':00';if(!/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(value))throw Error('请填写有效交易时间');q[name]=value;}
 if(q.from&&q.to&&q.from>q.to)throw Error('开始时间不能晚于结束时间');return q;
}
export function paymentLogFilterForm(){return `<details><summary>更多查询条件与排序</summary><div class="form-grid" data-paylog-filters>${field({name:'PayLogId',label:'流水编号'})}${paymentLogFields.filter(([k])=>!['TotalFee','CreateDt'].includes(k)).map(([name,label])=>field({name,label})).join('')}${field({name:'amountMin',label:'最小金额（含）'})}${field({name:'amountMax',label:'最大金额（含）'})}${field({name:'from',label:'开始时间（含）',type:'datetime-local',step:'1'})}${field({name:'to',label:'结束时间（含）',type:'datetime-local',step:'1'})}${field({name:'sort',label:'排序字段',options:[{value:'PayLogId',label:'流水编号'},...paymentLogFields.map(([value,label])=>({value,label}))]})}${field({name:'order',label:'排序方向',options:[{value:'DESC',label:'降序'},{value:'ASC',label:'升序'}]})}</div></details>`;}
