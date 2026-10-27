import test from 'node:test';
import assert from 'node:assert/strict';
import { createApi } from '../src/api.js';
import { createStore } from '../src/store.js';
import { reviewRequest } from '../src/adapters.js';
import { isModulePending, moneyYuan, pageItems } from '../src/utils.js';
import { renderState } from '../src/components/ui.js';

const response = (body, status = 200) => ({ ok: status >= 200 && status < 300, status, json: async () => body });

test('管理端仅接受后端 code=200 成功契约', async () => {
  const api = createApi({ fetchImpl: async () => response({ code: 200, data: { page: 0, items: [] } }) });
  assert.deepEqual(await api.request('/admin/users'), { page: 0, items: [] });
  const legacyZero = createApi({ fetchImpl: async () => response({ code: 0, data: {} }) });
  await assert.rejects(legacyZero.request('/auth/login'), /业务处理失败/);
});

test('统一密码登录使用 account 并采用服务端 ADMIN 用户与角色', async () => {
  const calls = [];
  const user = { id: 7, username: 'root', displayName: '管理员', roles: ['ADMIN'] };
  const api = { request: async (...args) => { calls.push(args); return { token: 'real-jwt', roles: ['ADMIN'], user }; } };
  const values = new Map();
  const storage = { getItem: (key) => values.get(key), setItem: (key, value) => values.set(key, value), removeItem: (key) => values.delete(key) };
  const store = createStore({ api, storage });

  assert.equal(await store.login('root', 'Secret123'), user);
  assert.equal(calls[0][0], '/auth/password/login');
  assert.deepEqual(calls[0][1].data, { account: 'root', password: 'Secret123' });
  assert.equal(store.getState().token, 'real-jwt');
  assert.equal(store.getState().user.roles[0], 'ADMIN');
  assert.equal(store.hasRole('ADMIN'), true);
});

test('审核适配既有 review API 和布尔审核体', () => {
  assert.deepEqual(reviewRequest('users', 7, { approved: 'true', reason: '通过' }), {
    path: '/review/registrations/7', method: 'POST', data: { approved: true, reason: '通过' }
  });
  assert.deepEqual(reviewRequest('orders', 9, { approved: 'false', reason: '驳回' }), {
    path: '/review/orders/9', method: 'POST', data: { approved: false, reason: '驳回' }
  });
});

test('分页兼容 items 与 Spring content 且金额元不再乘除一百', () => {
  assert.deepEqual(pageItems({ content: [{ id: 1 }] }), [{ id: 1 }]);
  assert.equal(moneyYuan('12.30'), '¥12.30');
  assert.equal(moneyYuan(0), '¥0.00');
});

test('未接入模块显示明确待接入而不是伪造数据', () => {
  assert.equal(isModulePending({ status: 404 }), true);
  assert.equal(isModulePending({ code: 404 }), true);
  assert.equal(isModulePending({ status: 500 }), false);
  assert.match(renderState('error', { status: 404 }), /模块待接入/);
});
