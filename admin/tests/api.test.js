import test from 'node:test';
import assert from 'node:assert/strict';
import { ApiError, createApi } from '../src/api.js';

const response = (status, body, headers = {}) => ({
  ok: status >= 200 && status < 300,
  status,
  headers: { get: (name) => headers[name.toLowerCase()] ?? null },
  json: async () => body,
  text: async () => typeof body === 'string' ? body : JSON.stringify(body)
});

test('request 解包成功数据并携带 Bearer Token', async () => {
  let captured;
  const api = createApi({
    baseUrl: '/api',
    getToken: () => 'jwt-token',
    fetchImpl: async (url, options) => {
      captured = { url, options };
      return response(200, { code: 200, message: 'ok', data: { items: [1] } });
    }
  });
  assert.deepEqual(await api.request('/orders', { query: { page: 2, status: 'TESTING' } }), { items: [1] });
  assert.equal(captured.url, '/api/orders?page=2&status=TESTING');
  assert.equal(captured.options.headers.Authorization, 'Bearer jwt-token');
  assert.equal(captured.options.headers['Content-Type'], 'application/json');
});

test('request 登录接口不附加过期 Token，POST data 转 JSON', async () => {
  let captured;
  const api = createApi({
    getToken: () => 'expired',
    fetchImpl: async (url, options) => {
      captured = { url, options };
      return response(200, { code: 200, data: { token: 'new' } });
    }
  });
  await api.request('/auth/login', { method: 'POST', data: { username: 'admin', password: 'secret' }, auth: false });
  assert.equal(captured.options.body, '{"username":"admin","password":"secret"}');
  assert.equal(captured.options.headers.Authorization, undefined);
});

test('request 将业务错误规范化并保留 details', async () => {
  const api = createApi({ fetchImpl: async () => response(422, { code: 42230, message: '结果不完整', details: { taskId: 8 } }) });
  await assert.rejects(api.request('/reports', { method: 'POST', data: {} }), (error) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 422);
    assert.equal(error.code, 42230);
    assert.equal(error.message, '结果不完整');
    assert.deepEqual(error.details, { taskId: 8 });
    return true;
  });
});

test('request 对 HTTP 200 内非零业务码同样抛错', async () => {
  const api = createApi({ fetchImpl: async () => response(200, { code: 40001, message: '参数错误' }) });
  await assert.rejects(api.request('/x'), /参数错误/);
});

test('401 清理登录并给出友好错误', async () => {
  let unauthorized = 0;
  const api = createApi({
    getToken: () => 'old',
    onUnauthorized: () => unauthorized++,
    fetchImpl: async () => response(401, { code: 40100, message: 'Token expired' })
  });
  await assert.rejects(api.request('/users/me'), (error) => error.status === 401 && error.message === '登录已过期，请重新登录');
  assert.equal(unauthorized, 1);
});

test('403 与无效响应均有可读错误', async () => {
  const forbidden = createApi({ fetchImpl: async () => response(403, { message: 'Access denied' }) });
  await assert.rejects(forbidden.request('/admin/dashboard'), (error) => error.status === 403 && error.message === '没有权限执行此操作');
  const broken = createApi({ fetchImpl: async () => ({ ok: true, status: 200, headers: { get: () => 'text/html' }, text: async () => '<html>' }) });
  await assert.rejects(broken.request('/x'), /服务返回了无法识别的响应/);
});

test('request 网络错误不伪造成功', async () => {
  const api = createApi({ fetchImpl: async () => { throw new TypeError('offline'); } });
  await assert.rejects(api.request('/orders'), (error) => error.status === 0 && /网络连接失败/.test(error.message));
});

test('FormData 上传不手工设置 Content-Type', async () => {
  let captured;
  const form = new FormData();
  form.append('file', new Blob(['pdf']), 'a.pdf');
  const api = createApi({ fetchImpl: async (url, options) => {
    captured = options;
    return response(200, { code: 200, data: { id: 1 } });
  } });
  await api.request('/invoices/1/file', { method: 'POST', data: form });
  assert.equal(captured.body, form);
  assert.equal(captured.headers['Content-Type'], undefined);
});

test('download 使用鉴权并返回 Blob 与文件名', async () => {
  let captured;
  const blob = new Blob(['pdf']);
  const api = createApi({
    getToken: () => 'jwt',
    fetchImpl: async (url, options) => {
      captured = { url, options };
      return { ok: true, status: 200, headers: { get: (name) => name === 'Content-Disposition' ? 'attachment; filename="report.pdf"' : null }, blob: async () => blob };
    }
  });
  const result = await api.download('/reports/1/download');
  assert.equal(captured.options.headers.Authorization, 'Bearer jwt');
  assert.equal(result.blob, blob);
  assert.equal(result.filename, 'report.pdf');
});
