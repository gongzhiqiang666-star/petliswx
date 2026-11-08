import test from 'node:test';
import assert from 'node:assert/strict';
import { openLegacyOrderDetails, detailBodyHtml } from '../src/components/legacy-order-details.js';
test('detail body preserves exact HTML as escaped source without executing it',()=>{
 const html='<img src="https://example.invalid/pixel" onerror="alert(1)"><table><tr><td>原文 &amp;</td></tr></table>';
 const view=detailBodyHtml({MailTitle:'<标题>',MailReport:html,ReportResult:html});
 assert.ok(view.includes('&lt;img'));assert.ok(view.includes('&amp;amp;'));assert.ok(!view.includes('<img'));assert.ok(view.includes('&lt;标题&gt;'));
});
test('order detail list uses scoped endpoint and retains monetary text',async()=>{
 let options;const calls=[];
 const context={api:{async request(path,options){calls.push([path,options]);return {items:[{DiagnosisDetailId:17,TestName:'<项目>',TestPrice:'1.00',OrginalPrice:'2.00',OrginalDiscount:'5.00'}],total:1}}},dialog(value){options=value}};
 await openLegacyOrderDetails(context,42);
 assert.equal(calls[0][0],'/admin/legacy/order-details/orders/42');assert.equal(calls[0][1].query.size,20);
 assert.ok(options.body.includes('&lt;项目&gt;'));assert.ok(options.body.includes('1.00'));assert.ok(options.body.includes('data-detail-view="17"'));assert.equal(options.onSubmit,undefined);
});
