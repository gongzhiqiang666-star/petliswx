import test from 'node:test';
import assert from 'node:assert/strict';
import { canOrderReview, canRefund, canLogisticsAction, canFinanceAction, canSendMessage } from '../src/permissions.js';

const user = (roles) => ({ roles });
const refundable = { settlementType: 'CURRENT', status: 'PENDING_PICKUP', paidAt: '2026-01-01', refundedAt: null };

test('退款入口仅对合法角色和状态显示', () => {
  assert.equal(canRefund(user(['ADMIN']), refundable), true);
  assert.equal(canRefund(user(['HOSPITAL_ADMIN']), refundable), true);
  assert.equal(canRefund(user(['DOCTOR']), refundable), true);
  assert.equal(canRefund(user(['LAB_STAFF']), refundable), false);
  assert.equal(canRefund(user(['COURIER']), refundable), false);
  assert.equal(canRefund(user(['ADMIN']), { ...refundable, status: 'TESTING' }), false);
  assert.equal(canRefund(user(['ADMIN']), { ...refundable, settlementType: 'MONTHLY' }), false);
  assert.equal(canRefund(user(['ADMIN']), { ...refundable, paidAt: null }), false);
  assert.equal(canRefund(user(['ADMIN']), { ...refundable, refundedAt: '2026-01-02' }), false);
});

test('审核物流签收和财务处理入口与后端角色一致', () => {
  assert.equal(canOrderReview(user(['ADMIN'])), true);
  assert.equal(canOrderReview(user(['HOSPITAL_ADMIN'])), false);
  assert.equal(canLogisticsAction('pickup', user(['ADMIN'])), false);
  assert.equal(canLogisticsAction('pickup', user(['COURIER'])), true);
  assert.equal(canLogisticsAction('pickup', user(['LAB_STAFF'])), false);
  assert.equal(canLogisticsAction('exception', user(['COURIER'])), true);
  assert.equal(canLogisticsAction('exception', user(['LAB_STAFF'])), false);
  assert.equal(canLogisticsAction('receive', user(['LAB_STAFF'])), true);
  assert.equal(canLogisticsAction('receive', user(['COURIER'])), false);
  assert.equal(canFinanceAction('generate', user(['ADMIN'])), true);
  assert.equal(canFinanceAction('generate', user(['HOSPITAL_ADMIN'])), false);
  assert.equal(canFinanceAction('settle', user(['HOSPITAL_ADMIN'])), true);
  assert.equal(canFinanceAction('issue', user(['HOSPITAL_ADMIN'])), false);
  assert.equal(canFinanceAction('reject', user(['HOSPITAL_ADMIN'])), false);
  assert.equal(canFinanceAction('download', user(['HOSPITAL_ADMIN'])), true);
});

test('仅管理员显示消息发送和平台审核管理入口', () => {
  assert.equal(canSendMessage(user(['ADMIN'])), true);
  assert.equal(canSendMessage(user(['DOCTOR'])), false);
  assert.equal(canSendMessage(user(['LAB_STAFF'])), false);
});
