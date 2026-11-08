import test from 'node:test';
import assert from 'node:assert/strict';
import {createDetailCommand,createDetailSession} from '../src/components/legacy-detail-form.js';

test('detail command keeps exact money strings, HTML and PHP empty-remark rule',()=>{
 const values={testTypeId:'12',originalPrice:'123456789012.34',originalDiscount:'其他折扣',testPrice:'0.00',detailRemark:'  ',modelId:'7',mailTitle:'标题'};
 const command=createDetailCommand(values,{mail:'<p>原文</p>',report:'<table><tr><td>结果</td></tr></table>'},'request-key-123456');
 assert.equal(command.originalPrice,'123456789012.34');assert.equal(command.testPrice,'0.00');assert.equal(command.originalDiscount,'其他折扣');assert.equal(command.reportResult,'<table><tr><td>结果</td></tr></table>');assert.equal(command.isSendMail,0);assert.equal(command.isCheckPass,0);
 assert.throws(()=>createDetailCommand({...values,testTypeId:'1225',detailRemark:'0'},{mail:'',report:''},'request-key-123456'),/备注/);
 assert.equal(createDetailCommand({...values,testTypeId:'1225'},{mail:'',report:''},'request-key-123456').detailRemark,'  ');
 assert.throws(()=>createDetailCommand({...values,testPrice:'1e3'},{mail:'',report:''},'request-key-123456'),/金额/);
});
test('uncertain create retry retains key and payload; editing after uncertain submission needs explicit reconciliation',async()=>{
 const sent=[];let fail=true;
 const api={async request(path,options){sent.push([path,options.data]);if(fail)throw Error('连接中断');return 91;}};
 const session=createDetailSession(api,42,'request-key-123456');const command={testTypeId:1,testPrice:'10.00'};
 await assert.rejects(()=>session.save(command),/连接中断/);fail=false;
 await assert.rejects(()=>session.save({...command,testPrice:'11.00'}),/核对/);
 assert.equal(await session.save(command),91);assert.equal(sent.length,2);assert.deepEqual(sent[0],sent[1]);
 assert.equal(sent[0][0],'/admin/legacy/order-details/orders/42');
});
test('successful create cannot submit a second order from the same session',async()=>{
 let writes=0;const session=createDetailSession({request:async()=>{writes++;return 7;}},42,'request-key-123456');
 assert.equal(await session.save({testTypeId:1}),7);assert.equal(await session.save({testTypeId:1}),7);assert.equal(writes,1);
});
