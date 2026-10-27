import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const source = (name) => readFile(new URL(`../src/${name}`, import.meta.url), 'utf8');

test('审核中心接入注册资料订单发票真实待审列表', async () => {
  const page = await source('pages/reviews.js');
  for (const endpoint of ['/review/registrations', '/review/profile-changes', '/review/orders', '/review/invoices']) {
    assert.ok(page.includes(endpoint), endpoint);
  }
  assert.ok(page.includes('moneyYuan'));
  assert.ok(!page.includes('payableCents'));
});

test('用户医院页面提供真实角色维护和停用启用', async () => {
  const page = await source('pages/organizations.js');
  assert.ok(page.includes('/roles'));
  assert.ok(page.includes('/status'));
  for (const role of ['DOCTOR', 'HOSPITAL_ADMIN', 'LAB_STAFF', 'COURIER', 'ADMIN']) assert.ok(page.includes(role), role);
  assert.ok(page.includes('rolePayload'));
  assert.ok(page.includes('userStatusPayload'));
});

test('财务发票复用管理审核接口且订单全管理明确待接入', async () => {
  const finance = await source('pages/finance.js');
  assert.ok(finance.includes("'/review/invoices'"));
  assert.ok(finance.includes('/reject'));
  assert.ok(finance.includes('/issue'));
  assert.ok(finance.includes('moneyYuan'));
  for (const page of ['orders', 'logistics', 'reports']) {
    const text = await source(`pages/${page}.js`);
    assert.ok(text.includes("renderState('pending')"), `${page} 应明确待接入`);
  }
});

test('管理端所有分页请求使用零基页码', async () => {
  const files = ['components/coupons.js', 'pages/reviews.js', 'pages/organizations.js', 'pages/orders.js', 'pages/logistics.js', 'pages/laboratory.js', 'pages/reports.js', 'pages/finance.js', 'pages/operations.js', 'pages/messages.js', 'pages/system.js'];
  for (const file of files) {
    const text = await source(file);
    assert.ok(!/page:\s*1\b/.test(text), `${file} 仍有 1 基页码`);
  }
});
