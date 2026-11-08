import test from 'node:test';
import assert from 'node:assert/strict';
import {invoiceSnapshot,invoiceDetails} from '../src/pages/finance.js';
test('发票核对读取申请快照并兼容对象及旧字段命名',()=>{
 const profile={title:'申请时医院',taxNumber:'TEST123456789012',email:'invoice@example.test',address:'申请地址',phone:'000',bankName:'测试行',bankAccount:'123',orderIds:[12,13]};
 assert.deepEqual(invoiceSnapshot({profileSnapshot:JSON.stringify(profile)}),profile);
 assert.deepEqual(invoiceSnapshot({profile_snapshot:profile}),profile);
 const html=invoiceDetails({id:7,amount:'88.00',profileSnapshot:JSON.stringify(profile)});
 for(const expected of ['申请时医院','TEST123456789012','invoice@example.test','申请地址','测试行','12、13','¥88.00'])assert.ok(html.includes(expected));
});
test('损坏或缺失的历史抬头不会使财务页面崩溃或显示未定义',()=>{
 for(const profileSnapshot of [null,'invalid','[]','null','42']){
  assert.deepEqual(invoiceSnapshot({profileSnapshot}),{});
  const html=invoiceDetails({id:7,amount:'88.00',profileSnapshot});assert.ok(html.includes('—'));assert.ok(!html.includes('undefined'));
 }
});
test('数据库原样快照的税号及银行字段完整展示',()=>{
 const html=invoiceDetails({id:7,amount:'88',profile_snapshot:JSON.stringify({title:'医院',tax_number:'TEST123456789012',bank_name:'原银行',bank_account:'987654'})});
 for(const text of ['TEST123456789012','原银行','987654'])assert.ok(html.includes(text));
});
test('发票快照中的抬头和订单内容按纯文本展示防止注入',()=>{
 const html=invoiceDetails({id:7,amount:'88',profileSnapshot:{title:'<img src=x onerror=alert(1)>',email:'<script>x</script>',orderIds:['<svg onload=x>']}});
 assert.ok(!html.includes('<img'));assert.ok(!html.includes('<script'));assert.ok(!html.includes('<svg'));assert.ok(html.includes('&lt;img'));
});
