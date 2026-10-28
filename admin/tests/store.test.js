import test from 'node:test';
import assert from 'node:assert/strict';
import { createStore, STORAGE_KEYS } from '../src/store.js';

function memoryStorage(initial = {}) {
  const data = new Map(Object.entries(initial));
  return {
    getItem: (key) => data.get(key) ?? null,
    setItem: (key, value) => data.set(key, String(value)),
    removeItem: (key) => data.delete(key),
    data
  };
}

test('store 恢复持久化 Token 和用户', () => {
  const user = { id: 1, displayName: '管理员', roles: ['ADMIN'] };
  const storage = memoryStorage({
    [STORAGE_KEYS.token]: 'saved-token',
    [STORAGE_KEYS.user]: JSON.stringify(user)
  });
  const store = createStore({ storage });
  assert.equal(store.getState().token, 'saved-token');
  assert.deepEqual(store.getState().user, user);
  assert.equal(store.hasRole('ADMIN'), true);
});

test('store 登录成功后持久化，退出后彻底清理', async () => {
  const storage = memoryStorage();
  const user = { id: 1, roles: ['ADMIN'] };
  const api = { request: async () => ({ token: 'fresh', user }) };
  const store = createStore({ storage, api });
  await store.login('admin', 'Admin123!');
  assert.equal(store.getState().token, 'fresh');
  assert.equal(storage.getItem(STORAGE_KEYS.token), 'fresh');
  assert.deepEqual(JSON.parse(storage.getItem(STORAGE_KEYS.user)), user);
  store.logout();
  assert.equal(store.getState().token, null);
  assert.equal(store.getState().user, null);
  assert.equal(storage.getItem(STORAGE_KEYS.token), null);
  assert.equal(storage.getItem(STORAGE_KEYS.user), null);
});

test('store 损坏的用户缓存不会阻止启动', () => {
  const storage = memoryStorage({ [STORAGE_KEYS.token]: 'token', [STORAGE_KEYS.user]: '{bad' });
  const store = createStore({ storage });
  assert.equal(store.getState().token, 'token');
  assert.equal(store.getState().user, null);
});

test('store 登录失败不写入旧状态', async () => {
  const storage = memoryStorage();
  const store = createStore({ storage, api: { request: async () => { throw new Error('密码错误'); } } });
  await assert.rejects(store.login('admin', 'bad'), /密码错误/);
  assert.equal(store.getState().token, null);
  assert.equal(storage.getItem(STORAGE_KEYS.token), null);
});

test('store 订阅者收到状态变化', async () => {
  const store = createStore({ storage: memoryStorage(), api: { request: async () => ({ token: 't', user: { roles: [] } }) } });
  let calls = 0;
  const unsubscribe = store.subscribe(() => calls++);
  await store.login('a', 'b');
  store.logout();
  unsubscribe();
  store.logout();
  assert.equal(calls, 2);
});
