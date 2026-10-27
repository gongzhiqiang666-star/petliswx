import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveStaticPath, contentType } from '../server.js';

test('静态服务器限制在 admin 目录并支持 SPA 回退', () => {
  const root = '/tmp/petlis-admin';
  assert.equal(resolveStaticPath(root, '/src/api.js'), '/tmp/petlis-admin/src/api.js');
  assert.equal(resolveStaticPath(root, '/orders/42'), '/tmp/petlis-admin/index.html');
  assert.equal(resolveStaticPath(root, '/../../etc/passwd'), null);
  assert.equal(resolveStaticPath(root, '/%2e%2e/%2e%2e/etc/passwd'), null);
});

test('静态服务器提供 ES Module 和样式 MIME', () => {
  assert.equal(contentType('/x/app.js'), 'text/javascript; charset=utf-8');
  assert.equal(contentType('/x/styles.css'), 'text/css; charset=utf-8');
  assert.equal(contentType('/index.html'), 'text/html; charset=utf-8');
});
