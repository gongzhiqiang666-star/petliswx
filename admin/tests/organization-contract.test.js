import test from 'node:test';
import assert from 'node:assert/strict';
import { reviewRequest, rolePayload, userStatusPayload } from '../src/adapters.js';

test('角色维护提交真实角色和医院实验室归属', () => {
  assert.deepEqual(rolePayload({ roleCode: 'HOSPITAL_ADMIN', hospitalId: '12', laboratoryId: '' }), {
    roleCode: 'HOSPITAL_ADMIN', hospitalId: 12, laboratoryId: null
  });
  assert.deepEqual(rolePayload({ roleCode: 'LAB_STAFF', hospitalId: '', laboratoryId: '31' }), {
    roleCode: 'LAB_STAFF', hospitalId: null, laboratoryId: 31
  });
});

test('用户停用提交布尔状态和真实原因', () => {
  assert.deepEqual(userStatusPayload({ active: 'false', reason: ' 离职 ' }), { active: false, reason: '离职' });
});

test('资料审核复用既有接口，发票通过使用真实 URL', () => {
  assert.deepEqual(reviewRequest('profiles', 5, { approved: 'false', reason: '资料不符' }), {
    path: '/review/profile-changes/5', method: 'POST', data: { approved: false, reason: '资料不符' }
  });
  assert.deepEqual(reviewRequest('invoices', 8, { approved: 'true', invoiceUrl: 'https://example.com/invoice.pdf' }), {
    path: '/invoices/8/issue', method: 'POST', data: { invoiceUrl: 'https://example.com/invoice.pdf' }
  });
  assert.deepEqual(reviewRequest('invoices', 8, { approved: 'false', reason: '信息错误' }), {
    path: '/invoices/8/reject', method: 'POST', data: { reason: '信息错误' }
  });
});
