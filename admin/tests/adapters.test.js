import test from 'node:test';
import assert from 'node:assert/strict';
import {
  reviewPayload, orderReviewPayload, pickupPayload, resultPayload, reportPayload,
  contractPayload, billPayload, invoiceProcessPayload, pointAdjustmentPayload,
  giftPayload, articlePayload, messagePayload, configPayload, reviewRequest
} from '../src/adapters.js';

test('审核中心按资源使用后端真实方法和请求体', () => {
  assert.deepEqual(reviewRequest('hospitals', 3, { approved: 'true', reason: 'ok' }), {
    path: '/admin/hospitals/3/review', method: 'PATCH', data: { approved: true, reason: 'ok' }
  });
  assert.deepEqual(reviewRequest('users', 4, { approved: 'false', reason: 'bad' }), {
    path: '/review/registrations/4', method: 'POST', data: { approved: false, reason: 'bad' }
  });
  assert.deepEqual(reviewRequest('orders', 5, { approved: 'true', reason: '' }), {
    path: '/review/orders/5', method: 'POST', data: { approved: true, reason: '' }
  });
});

test('医院医生审核适配布尔 approved，后续送检工作流保留 decision', () => {
  assert.deepEqual(reviewPayload({ approved: true, reason: '' }), { approved: true, reason: '' });
  assert.deepEqual(reviewPayload({ approved: false, reason: '资质不全' }), { approved: false, reason: '资质不全' });
  assert.deepEqual(orderReviewPayload({ decision: 'APPROVE', reason: '' }), { decision: 'APPROVE', reason: '' });
});

test('物流分配保留 courierId 数值和运单字段', () => {
  assert.deepEqual(pickupPayload({ trackingNo: ' SF01 ', carrier: '顺丰', courierId: '8' }), { trackingNo: 'SF01', carrier: '顺丰', courierId: 8 });
  assert.deepEqual(pickupPayload({ trackingNo: '', carrier: '', courierId: '' }), { trackingNo: null, carrier: null, courierId: null });
});

test('检测结果适配后端 ResultCommand 并转换异常布尔', () => {
  assert.deepEqual(resultPayload([{ orderItemId: '3', name: ' ALT ', value: '42', unit: 'U/L', referenceRange: '0-40', abnormalFlag: 'true' }]), {
    results: [{ orderItemId: 3, name: 'ALT', value: '42', unit: 'U/L', referenceRange: '0-40', abnormalFlag: true }]
  });
  assert.equal(resultPayload([{ orderItemId: 1, name: 'n', value: 'v', unit: 'u', referenceRange: 'r', abnormalFlag: '' }]).results[0].abnormalFlag, null);
});

test('报告生成审核和物流异常理由字段正确', () => {
  assert.deepEqual(reportPayload({ summary: ' 总结 ', conclusion: ' 结论 ' }), { summary: '总结', conclusion: '结论' });
  assert.deepEqual(orderReviewPayload({ decision: 'REJECT', reason: ' 退回 ' }), { decision: 'REJECT', reason: '退回' });
});

test('签约金额从元转分且折扣为整数基点', () => {
  assert.deepEqual(contractPayload({ hospitalId: '2', labId: '3', name: '月结合同', settlementType: 'MONTHLY', discount: '95', monthlyCreditLimit: '1000.50', effectiveFrom: '2026-01-01', effectiveTo: '' }), {
    hospitalId: 2, labId: 3, name: '月结合同', settlementType: 'MONTHLY', discountBps: 9500,
    monthlyCreditLimitCents: 100050, effectiveFrom: '2026-01-01', effectiveTo: null
  });
  assert.throws(() => contractPayload({ hospitalId: 1, labId: 1, name: 'x', settlementType: 'CURRENT', discount: '101', monthlyCreditLimit: '0', effectiveFrom: '2026-01-01' }), /0 到 100/);
});

test('账单与发票处理字段与后端一致', () => {
  assert.deepEqual(billPayload({ hospitalId: '2', billingMonth: '2026-10', dueDate: '2026-11-15' }), { hospitalId: 2, billingMonth: '2026-10', dueDate: '2026-11-15' });
  assert.deepEqual(invoiceProcessPayload({ action: 'REJECT', reason: ' 信息错误 ' }), { action: 'REJECT', reason: '信息错误' });
});

test('积分调整生成幂等键且金额不经过浮点转换', () => {
  const payload = pointAdjustmentPayload({ userId: '9', points: '-25', reason: ' 纠错 ' }, 'fixed-key');
  assert.deepEqual(payload, { data: { userId: 9, points: -25, reason: '纠错' }, idempotencyKey: 'fixed-key' });
  assert.throws(() => pointAdjustmentPayload({ userId: 9, points: 0, reason: 'x' }), /不能为 0/);
});

test('礼品、资讯、消息和配置表单保持真实接口字段', () => {
  assert.deepEqual(giftPayload({ name: ' 玩具 ', pointsCost: '100', stock: '5', description: 'd', imageUrl: '', status: 'ACTIVE' }), { name: '玩具', pointsCost: 100, stock: 5, description: 'd', imageUrl: null, status: 'ACTIVE' });
  const article = articlePayload({ categoryId: '2', title: ' 标题 ', slug: 'slug', summary: '', content: '正文', coverUrl: '', contentType: 'RICH_TEXT', liveStartAt: '', liveEndAt: '', popupEnabled: true, status: 'PUBLISHED' });
  assert.equal(article.categoryId, 2);
  assert.equal(article.popupEnabled, true);
  assert.equal(article.liveStartAt, null);
  assert.deepEqual(messagePayload({ userId: '4', title: '通知', content: '内容', channel: 'IN_APP', messageType: 'BUSINESS' }), { userId: 4, title: '通知', content: '内容', channel: 'IN_APP', messageType: 'BUSINESS' });
  assert.deepEqual(configPayload({ value: ' 10 ', description: ' 说明 ' }), { value: '10', description: '说明' });
});
