import test from 'node:test';import assert from 'node:assert/strict';
import {openBankPreparation} from '../src/components/legacy-bank-preparation.js';
const quote={description:'订单:<B1>',displayAmount:'12.50',amount:'12.50',count:1,items:[{InfoHospitalId:7}]};
test('支付准备保留原核价和请求键，重开弹窗后网络重试不重建请求',async()=>{
 let modal,fail=true;const sent=[],attempt={};const context={dialog:d=>modal=d,toast(){},api:{request:async(url,opt)=>{sent.push(opt.data);if(fail)throw Error('network');return {orderId:'TEST1',status:'READY'};}}};
 await openBankPreparation(context,{mode:'ORDER',orderNo:'B1'},quote,()=>{},attempt);assert.match(modal.body,/&lt;B1&gt;/);assert.match(modal.body,/不会发起扣款/);
 await assert.rejects(modal.onSubmit({elements:{},isConnected:true}),/network/);
 await openBankPreparation(context,{mode:'ORDER',orderNo:'B1'},quote,()=>{},attempt);fail=false;await modal.onSubmit({elements:{},isConnected:true});
 assert.equal(sent[0].requestKey,sent[1].requestKey);assert.equal(sent[1].hospital,7);assert.equal(sent[1].displayedAmount,'12.50');assert.equal(sent[1].orderNo,'B1');
});
test('同号涉及多医院时须明确选择且重试不能换付款医院',async()=>{
 let modal;const attempt={},context={dialog:d=>modal=d,toast(){},api:{request:async()=>{throw Error('network');}}};
 const multiple={...quote,items:[{InfoHospitalId:7},{InfoHospitalId:8}]};await openBankPreparation(context,{mode:'ORDER',orderNo:'B1'},multiple,()=>{},attempt);
 await assert.rejects(modal.onSubmit({elements:{hospital:{value:''}}}),/医院/);
 await assert.rejects(modal.onSubmit({elements:{hospital:{value:'7'}}}),/network/);
 await assert.rejects(modal.onSubmit({elements:{hospital:{value:'8'}}}),/核对/);
});
