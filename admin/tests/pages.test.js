import test from 'node:test';
import assert from 'node:assert/strict';
import { pageMeta, buildQuery, actionFeedback } from '../src/pages/shared.js';

test('核心送检闭环页面都声明真实 API 与操作', () => {
  for (const id of ['orders', 'logistics', 'laboratory', 'reports', 'finance']) {
    assert.ok(pageMeta[id], id);
    assert.ok(pageMeta[id].endpoint.startsWith('/'));
    assert.ok(pageMeta[id].actions.length > 0, `${id} 不应只有静态菜单`);
  }
});

test('筛选查询忽略空值并始终使用零页起始', () => {
  assert.equal(buildQuery({ status: '', month: '2026-10', page: 0, size: 20 }), '?month=2026-10&page=0&size=20');
});

test('操作反馈保留成功和服务端错误', () => {
  assert.equal(actionFeedback(null, '已发布').message, '已发布');
  assert.equal(actionFeedback(null, '已发布').type, 'success');
  assert.equal(actionFeedback({ status: 403 }, '已发布').type, 'forbidden');
  assert.equal(actionFeedback(new Error('状态不允许'), '已发布').message, '状态不允许');
});
