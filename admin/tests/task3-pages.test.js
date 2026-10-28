import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { canLabAction, canLogisticsAction, canReportAction } from '../src/permissions.js';
import { isRouteAllowed } from '../src/router.js';

const source = (name) => readFile(new URL(`../src/pages/${name}.js`, import.meta.url), 'utf8');

test('物流页面接入真实运单并按严格角色显示操作', async () => {
  const page = await source('logistics');
  assert.ok(page.includes("api.request('/logistics'"));
  assert.ok(page.includes('/assign'));
  assert.ok(page.includes('/exception'));
  assert.ok(page.includes('escapeHtml(row.trackingNo'));
  assert.ok(!page.includes("renderState('pending')"));
  const admin = { roles: ['ADMIN'] };
  const courier = { roles: ['COURIER'] };
  const lab = { roles: ['LAB_STAFF'] };
  assert.equal(canLogisticsAction('create', admin), true);
  assert.equal(canLogisticsAction('assign', admin), true);
  assert.equal(canLogisticsAction('pickup', admin), false);
  assert.equal(canLogisticsAction('receive', admin), false);
  assert.equal(canLogisticsAction('pickup', courier), true);
  assert.equal(canLogisticsAction('receive', lab), true);
});

test('实验与报告页面接真实工作流且按角色显示动作', async () => {
  const labPage = await source('laboratory');
  const reportPage = await source('reports');
  assert.ok(labPage.includes("api.request('/lab/tasks'"));
  assert.ok(reportPage.includes("api.request('/report-workflows'"));
  assert.ok(reportPage.includes('/review'));
  assert.ok(reportPage.includes('/publish'));
  assert.ok(!reportPage.includes("renderState('pending')"));
  const admin = { roles: ['ADMIN'] };
  const lab = { roles: ['LAB_STAFF'] };
  const doctor = { roles: ['DOCTOR'] };
  for (const action of ['assign', 'results', 'return', 'retest']) {
    assert.equal(canLabAction(action, lab), true, action);
    assert.equal(canLabAction(action, doctor), false, action);
  }
  for (const action of ['create', 'review', 'publish']) {
    assert.equal(canReportAction(action, admin), true, action);
    assert.equal(canReportAction(action, lab), true, action);
    assert.equal(canReportAction(action, doctor), false, action);
  }
  assert.equal(canReportAction('download', doctor), true);
  assert.equal(isRouteAllowed('logistics', doctor), true);
  assert.equal(isRouteAllowed('reports', doctor), true);
  assert.equal(isRouteAllowed('laboratory', doctor), false);
});
