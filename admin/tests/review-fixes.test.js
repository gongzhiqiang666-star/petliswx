import test from 'node:test';
import assert from 'node:assert/strict';
import { actionData, decodeActionData, escapeHtml } from '../src/utils.js';
import { bindRetry } from '../src/components/retry.js';
import { couponTemplatePayload } from '../src/adapters.js';
import { directoryOptions, itemOptions } from '../src/options.js';

test('系统配置 action data 编码后可经 DOM 属性持久化且阻断 XSS', () => {
  const original = { key: 'theme"><img src=x onerror=alert(1)>', value: '<script>alert(1)</script>', description: 'A&B"\'' };
  const encoded = actionData(original);
  const html = `<button data-config="${encoded}">编辑</button>`;
  assert.ok(!html.includes('<img'));
  assert.ok(!html.includes('<script>'));
  const attributes = new Map();
  const element = { setAttribute: (name, value) => attributes.set(name, String(value)), getAttribute: (name) => attributes.get(name) };
  element.setAttribute('data-config', html.match(/data-config="([^"]*)"/)[1]);
  assert.deepEqual(decodeActionData(element.getAttribute('data-config')), original);
  assert.ok(escapeHtml(encoded).includes('&'));
});

test('统一重试 DOM 委托只响应内部 retry 按钮', () => {
  let listener;
  let retries = 0;
  const container = { addEventListener: (type, callback) => { assert.equal(type, 'click'); listener = callback; } };
  bindRetry(container, () => retries++);
  listener({ target: { closest: () => null } });
  assert.equal(retries, 0);
  listener({ target: { closest: (selector) => selector === '[data-action="retry"]' ? {} : null } });
  assert.equal(retries, 1);
});

test('优惠券模板表单精确转换分与 Instant', () => {
  assert.deepEqual(couponTemplatePayload({ code: ' SAVE ', name: ' 满减 ', couponType: 'FIXED', value: '10.05', minSpend: '50.00', validFrom: '2026-01-01T00:00:00Z', validTo: '2027-01-01T00:00:00Z', totalQuantity: '20', status: 'ACTIVE' }), {
    code: 'SAVE', name: '满减', couponType: 'FIXED', valueCents: 1005, minSpendCents: 5000,
    validFrom: '2026-01-01T00:00:00.000Z', validTo: '2027-01-01T00:00:00.000Z', totalQuantity: 20, status: 'ACTIVE'
  });
});

test('人员与项目下拉显示名称和真实 ID', () => {
  assert.deepEqual(directoryOptions([{ id: 3, displayName: '配送甲', username: 'courier3' }]), [{ value: 3, label: '配送甲（courier3 · ID 3）' }]);
  assert.deepEqual(itemOptions([{ id: 8, name: '血常规', code: 'CBC' }]), [{ value: 8, label: '血常规（CBC · ID 8）' }]);
});
