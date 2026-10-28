import test from 'node:test';
import assert from 'node:assert/strict';
import { renderTable, renderState, renderDetail, modal, pagination } from '../src/components/ui.js';

test('列表渲染真实行、操作和 XSS escaping', () => {
  const html = renderTable({
    columns: [{ label: '单号', key: 'orderNo' }, { label: '金额', render: (row) => `¥${row.amount}` }],
    rows: [{ id: 1, orderNo: '<script>alert(1)</script>', amount: '1.00' }],
    actions: (row) => `<button data-id="${row.id}">查看</button>`,
    emptyText: '暂无送检单'
  });
  assert.ok(html.includes('&lt;script&gt;'));
  assert.ok(!html.includes('<script>'));
  assert.ok(html.includes('data-id="1"'));
  assert.ok(html.includes('<table'));
});

test('空、加载、错误与 403 状态有明确反馈', () => {
  assert.ok(renderTable({ columns: [{ label: '名称', key: 'name' }], rows: [] }).includes('暂无数据'));
  assert.ok(renderState('loading').includes('正在加载'));
  assert.ok(renderState('error', { message: '失败' }).includes('失败'));
  assert.ok(renderState('forbidden').includes('没有权限'));
});

test('详情和弹窗默认转义服务端内容', () => {
  const detail = renderDetail([{ label: '备注', value: '<b>bad</b>' }]);
  assert.ok(detail.includes('&lt;b&gt;bad&lt;/b&gt;'));
  assert.ok(!detail.includes('<b>bad</b>'));
  const dialog = modal({ title: '确认', body: '<p>内容</p>', footer: '<button>确定</button>' });
  assert.ok(dialog.includes('role="dialog"'));
  assert.ok(dialog.includes('<p>内容</p>'));
});

test('零基分页显示一基页码并禁用边界按钮', () => {
  const first = pagination({ page: 0, size: 20, total: 50 });
  assert.ok(first.includes('共 50 条'));
  assert.ok(first.includes('1 / 3'));
  assert.ok(first.includes('disabled'));
  const last = pagination({ page: 2, size: 20, total: 50 });
  assert.ok(last.includes('3 / 3'));
});
