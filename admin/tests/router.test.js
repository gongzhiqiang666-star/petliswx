import test from 'node:test';
import assert from 'node:assert/strict';
import { routes, resolveRoute, isRouteAllowed } from '../src/router.js';

test('路由包含所有管理职责', () => {
  const ids = routes.map((route) => route.id);
  for (const id of ['dashboard', 'reviews', 'organizations', 'orders', 'logistics', 'laboratory', 'reports', 'catalog', 'finance', 'operations', 'messages', 'system']) {
    assert.ok(ids.includes(id), `缺少 ${id}`);
  }
});

test('路由解析 hash、详情参数和未知路径', () => {
  assert.equal(resolveRoute('#/orders').id, 'orders');
  assert.equal(resolveRoute('#/orders/42').params.id, '42');
  assert.equal(resolveRoute('#/reports/7').params.id, '7');
  assert.equal(resolveRoute('#/does-not-exist').id, 'dashboard');
  assert.equal(resolveRoute('').id, 'dashboard');
});

test('非管理员仅可进入授权页面', () => {
  const lab = { roles: ['LAB_STAFF'] };
  assert.equal(isRouteAllowed('laboratory', lab), true);
  assert.equal(isRouteAllowed('reports', lab), true);
  assert.equal(isRouteAllowed('system', lab), false);
  assert.equal(isRouteAllowed('dashboard', { roles: ['ADMIN'] }), true);
});

test('legacy reviewer 仅能进入审核中心且不能获得管理员页面', () => {
  const reviewer = { roles: ['REVIEWER'] };
  assert.equal(isRouteAllowed('reviews', reviewer), true);
  assert.equal(isRouteAllowed('organizations', reviewer), false);
  assert.equal(isRouteAllowed('dashboard', reviewer), false);
});

test('迁移字典入口同时受管理员角色和服务端能力开关控制',()=>{
 const admin={roles:['ADMIN']};assert.equal(isRouteAllowed('legacyDictionaries',admin),false);assert.equal(isRouteAllowed('legacyDictionaries',admin,{dictionaries:false}),false);assert.equal(isRouteAllowed('legacyDictionaries',admin,{dictionaries:true}),true);assert.equal(isRouteAllowed('legacyDictionaries',{roles:['DOCTOR']},{dictionaries:true}),false)
});

test('原网银核价仅管理员且业务开关开启可见',()=>{assert.equal(isRouteAllowed('legacyBankPayments',{roles:['ADMIN']},{bankPayments:true}),true);assert.equal(isRouteAllowed('legacyBankPayments',{roles:['ADMIN']},{}),false);assert.equal(isRouteAllowed('legacyBankPayments',{roles:['DOCTOR']},{bankPayments:true}),false);});
