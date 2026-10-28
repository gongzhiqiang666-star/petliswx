import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const catalog = fs.readFileSync(new URL('../src/pages/catalog.js', import.meta.url), 'utf8');

test('合同列表提供真实停用启用操作', () => {
  assert.match(catalog, /data-contract-status/);
  assert.match(catalog, /\/status`, \{ method: 'PATCH'/);
  assert.match(catalog, /active:/);
});

test('项目页面提供新建编辑删除并复用既有目录接口', () => {
  assert.match(catalog, /id="item-new"/);
  assert.match(catalog, /data-item-edit/);
  assert.match(catalog, /data-item-delete/);
  assert.match(catalog, /method: 'POST'/);
  assert.match(catalog, /item \? 'PUT' : 'POST'/);
  assert.match(catalog, /method: 'DELETE'/);
});
