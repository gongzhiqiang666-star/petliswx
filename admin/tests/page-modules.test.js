import test from 'node:test';
import assert from 'node:assert/strict';

const modules = [
  'dashboard', 'reviews', 'organizations', 'orders', 'logistics', 'laboratory',
  'reports', 'catalog', 'finance', 'operations', 'messages', 'system'
];

for (const name of modules) {
  test(`${name} 页面暴露统一 render 入口`, async () => {
    const module = await import(`../src/pages/${name}.js`);
    assert.equal(typeof module.render, 'function');
  });
}
