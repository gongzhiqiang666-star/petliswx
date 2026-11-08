import test from 'node:test';
import assert from 'node:assert/strict';
import {openPaymentLogEditor,paymentLogFields} from '../src/components/legacy-payment-logs.js';
test('流水编辑保留空字符串、负数精度、原时间和所有业务列，未知状态不替换',async()=>{
 let modal;const writes=[];const row={PayLogId:7,editToken:'a'.repeat(64),TotalFee:'-12.35',CreateDt:'2020-02-29 13:14:15',TradeStatus:'CUSTOM_STATE',TradeNoAli:'<tag>',TradeNoUser:'000123'};
 for(const [name] of paymentLogFields)row[name]??='';
 const context={api:{request:async(url,options)=>{if(!options)return row;writes.push({url,...options});return 7}},dialog:d=>modal=d,toast(){}};
 await openPaymentLogEditor(context,7,()=>{});assert.match(modal.body,/&lt;tag&gt;/);assert.ok(!modal.body.includes('<tag>'));const form={elements:Object.fromEntries(paymentLogFields.map(([name])=>[name,{value:row[name]}])),isConnected:true};await modal.onSubmit(form);
 assert.equal(writes[0].method,'PUT');assert.equal(writes[0].data.fields.TotalFee,'-12.35');assert.equal(writes[0].data.fields.BuyMail,'');assert.equal(writes[0].data.fields.TradeNoUser,'000123');assert.equal(writes[0].data.fields.TradeStatus,'CUSTOM_STATE');assert.equal(writes[0].data.expectedToken,row.editToken);
});
test('结果不明重试沿用请求号和原输入，修改输入须先核对',async()=>{
 let modal,fail=true;const writes=[];const context={api:{request:async(url,options)=>{writes.push({url,...options});if(fail)throw Error('network');return 1}},dialog:d=>modal=d,toast(){}};
 await openPaymentLogEditor(context,null,()=>{});const form={elements:Object.fromEntries(paymentLogFields.map(([name])=>[name,{value:name==='TotalFee'?'0':name==='CreateDt'?'2020-02-29 13:14:15':''}])),isConnected:true};await assert.rejects(modal.onSubmit(form),/network/);form.elements.TotalFee.value='2';await assert.rejects(modal.onSubmit(form),/核对/);form.elements.TotalFee.value='0';fail=false;await modal.onSubmit(form);assert.equal(writes[0].data.requestKey,writes[1].data.requestKey);
});
