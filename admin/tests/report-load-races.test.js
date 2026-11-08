import test from 'node:test';
import assert from 'node:assert/strict';
import { render } from '../src/pages/reports.js';

function harness() {
  const elements = new Map();
  const element = key => {
    if (!elements.has(key)) elements.set(key, { innerHTML: '', style: {}, handlers: {}, addEventListener(event, fn) { this.handlers[event] = fn; } });
    return elements.get(key);
  };
  const container = { innerHTML: '', querySelector: element, addEventListener() {} };
  const requests = [];
  const context = { store: { getState: () => ({ user: { roles: ['ADMIN'] } }) },
    api: { request: (path, options) => new Promise((resolve, reject) => requests.push({ path, options, resolve, reject })) } };
  return { container, context, requests, element };
}
const templatePage = { items: [{ id: 1, template_name: '当前模板', template_code: 'CBC', test_name: '血常规', version: 2, status: 'ACTIVE' }], total: 1, page: 0, size: 20 };

for (const staleFailure of [false, true]) {
  test(`切换到模板管理后旧报告${staleFailure ? '失败' : '成功'}响应不能覆盖当前列表`, async () => {
    const h = harness();
    const initial = render(h.container, h.context);
    h.element('#template-create').handlers.click();
    assert.equal(h.requests[0].path, '/report-workflows');
    assert.equal(h.requests[1].path, '/report-workflows/templates');
    h.requests[1].resolve(templatePage);
    await Promise.resolve();
    assert.match(h.element('#report-list').innerHTML, /当前模板/);
    if (staleFailure) h.requests[0].reject(new Error('过期报告请求失败'));
    else h.requests[0].resolve({ items: [{ id: 999, status: 'PUBLISHED' }], total: 1 });
    await initial;
    assert.match(h.element('#report-list').innerHTML, /当前模板/);
    assert.doesNotMatch(h.element('#report-list').innerHTML, /过期报告请求失败|#999/);
  });
}
