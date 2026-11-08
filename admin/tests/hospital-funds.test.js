import test from 'node:test';import assert from 'node:assert/strict';
import {hospitalFundsCommand,openHospitalFunds} from '../src/components/hospital-funds.js';
test('医院金额保留字符串精度，备注及原表范围受控',()=>{
 assert.equal(hospitalFundsCommand({amount:'-12.50',remark:'扣款'},'request_1234567890').amount,'-12.50');
 for(const amount of ['0','0.00','-0.00','1.009','1e3','100000000','NaN'])assert.throws(()=>hospitalFundsCommand({amount,remark:'备注'},'key'));
 assert.throws(()=>hospitalFundsCommand({amount:'1',remark:' '},'key'));
});
test('资金读页面区分医院资金，不提供流水编辑删除',async()=>{
 let dialog;await openHospitalFunds({api:{request:async p=>p.endsWith('entries')?{items:[],total:0}:{HospitalName:'医院A',amount:'12.50'}},dialog:d=>dialog=d},1,false);
 assert.match(dialog.body,/医院资金池/);assert.ok(!dialog.onSubmit);assert.ok(!dialog.body.includes('name="amount"'));assert.ok(!dialog.body.includes('data-delete'));
});
test('失败重试保留同一资金请求号，金额不转浮点',async()=>{
 const Previous=globalThis.FormData;globalThis.FormData=class{constructor(form){this.form=form}entries(){return Object.entries(this.form.values)}};
 try{let modal;const writes=[];const context={toast(){},dialog:d=>modal=d,api:{request:async(p,o)=>{if(o?.method==='POST'){writes.push(o.data);if(writes.length===1)throw Error('timeout');return 1;}return p.endsWith('entries')?{items:[],total:0}:{HospitalName:'医院A',amount:'1.00'}}}};
 await openHospitalFunds(context,1,true);const form={values:{amount:'0.01',remark:'加款'}};await assert.rejects(modal.onSubmit(form));await modal.onSubmit(form);assert.equal(writes[0].requestKey,writes[1].requestKey);assert.equal(writes[1].amount,'0.01');
 }finally{globalThis.FormData=Previous}
});
