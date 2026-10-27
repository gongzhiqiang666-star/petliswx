import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { canFinanceAction, canRefund, canReportAction } from '../src/permissions.js';

const source = (name) => readFile(new URL(`../src/${name}`, import.meta.url), 'utf8');

test('看板使用元金额真实聚合与日期筛选导出', async () => {
  const page = await source('pages/dashboard.js');
  assert.ok(page.includes("api.request('/admin/dashboard'"));
  assert.ok(page.includes('/admin/export/orders.csv'));
  assert.ok(page.includes('moneyYuan(data.revenue)'));
  assert.ok(!page.includes('revenueCents'));
});

test('财务页使用月结双方对账统一mock结算与专用mock退款', async () => {
  const page = await source('pages/finance.js');
  for (const endpoint of ['/admin/bills', '/reconciliation', '/settlement', '/admin/refunds/']) assert.ok(page.includes(endpoint), endpoint);
  assert.ok(page.includes('mock-'));
  assert.ok(!page.includes('真实冲减钱包'));
  assert.ok(!page.includes('实际扣款'));
  assert.equal(canFinanceAction('settle', { roles: ['HOSPITAL_ADMIN'] }), false);
  assert.equal(canRefund({ roles: ['DOCTOR'] }, { settlementType: 'CURRENT', status: 'PENDING_PICKUP', paidAt: 'now' }), false);
});

test('签约运营系统页全部调用统一后端真实接口', async () => {
  const expectations = {
    'pages/catalog.js': ['/admin/catalog/laboratories', '/admin/catalog/items', '/admin/contracts', '/prices'],
    'pages/operations.js': ['/admin/operations/configs', '/admin/operations/points-adjustments', '/mall/gifts', '/mall/redemptions', '/cancel'],
    'pages/system.js': ['/admin/operations/configs', '/admin/operations/audits'],
    'components/coupons.js': ['/admin/coupons/templates', '/users/', 'requestKey']
  };
  for (const [file, endpoints] of Object.entries(expectations)) {
    const page = await source(file);
    for (const endpoint of endpoints) assert.ok(page.includes(endpoint), `${file}: ${endpoint}`);
    assert.ok(!page.includes('待接入'), file);
  }
});

test('优惠券编辑加载并展示真实库存而不使用默认数量覆盖', async () => {
  const page = await source('components/coupons.js');
  assert.ok(page.includes('data-total-quantity'));
  assert.ok(page.includes('data-issued-quantity'));
  assert.ok(page.includes('current.totalQuantity'));
  assert.ok(page.includes('current.issuedQuantity'));
  assert.ok(page.includes('已发 / 总量'));
});

test('订单与报告提供真实鉴权打印入口且没有待接入', async () => {
  const orders = await source('pages/orders.js');
  const reports = await source('pages/reports.js');
  assert.ok(orders.includes('/admin/print/orders/'));
  assert.ok(orders.includes('/admin/print/specimens/'));
  assert.ok(reports.includes('/admin/print/reports/'));
  assert.ok(!orders.includes("renderState('pending')"));
  assert.equal(canReportAction('print', { roles: ['DOCTOR'] }), false);
  assert.equal(canReportAction('print', { roles: ['HOSPITAL_ADMIN'] }), true);
});
