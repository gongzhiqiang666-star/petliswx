import test from 'node:test';
import assert from 'node:assert/strict';
import { centsToYuan, yuanToCents, escapeHtml, formatDate, pageItems, value, cell } from '../src/utils.js';

test('业务表格文本适配器转义服务端字符串并兼容 snake_case', () => {
  assert.equal(cell({ display_name: '<img src=x>' }, 'displayName'), '&lt;img src=x&gt;');
  assert.equal(cell(null, 'name'), '—');
});

test('分转元准确保留两位并处理负数', () => {
  assert.equal(centsToYuan(0), '0.00');
  assert.equal(centsToYuan(5), '0.05');
  assert.equal(centsToYuan(123456), '1234.56');
  assert.equal(centsToYuan(-1), '-0.01');
  assert.equal(centsToYuan(null), '—');
});

test('元转分拒绝精度损失和非数字', () => {
  assert.equal(yuanToCents('0.29'), 29);
  assert.equal(yuanToCents('12.3'), 1230);
  assert.equal(yuanToCents('-0.01'), -1);
  assert.equal(yuanToCents(8), 800);
  assert.throws(() => yuanToCents('0.001'), /最多两位小数/);
  assert.throws(() => yuanToCents('abc'), /有效金额/);
  assert.throws(() => yuanToCents(''), /有效金额/);
});

test('escapeHtml 阻断标签、属性和脚本注入', () => {
  const escaped = escapeHtml('<img src=x onerror="alert(1)">&\'');
  assert.equal(escaped, '&lt;img src=x onerror=&quot;alert(1)&quot;&gt;&amp;&#39;');
  assert.ok(!escaped.includes('<img'));
});

test('formatDate 对空值与无效值安全', () => {
  assert.equal(formatDate(null), '—');
  assert.equal(formatDate('bad'), '—');
  assert.match(formatDate('2026-01-02T03:04:05Z', 'UTC'), /2026/);
});

test('分页数据同时支持 items 和数组并兼容 camel/snake 字段', () => {
  assert.deepEqual(pageItems({ items: [1, 2] }), [1, 2]);
  assert.deepEqual(pageItems([3]), [3]);
  assert.deepEqual(pageItems(null), []);
  assert.equal(value({ order_no: 'O1' }, 'orderNo'), 'O1');
  assert.equal(value({ orderNo: 'O2' }, 'orderNo'), 'O2');
});
