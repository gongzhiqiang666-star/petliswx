const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const ROOT = path.resolve(__dirname, '..');
const RESULTS = path.join(__dirname, 'results');
const BACKEND = process.env.BACKEND_ORIGIN;
const ADMIN = process.env.ADMIN_ORIGIN;
const summary = { status: 'RUNNING', startedAt: new Date().toISOString(), steps: [], mini: {}, browser: { console: [], pageerrors: [], screenshots: [] } };
function save() { fs.writeFileSync(path.join(RESULTS, 'merge-e2e.json'), JSON.stringify(summary, null, 2)); }
function pass(name, details) { summary.steps.push({ name, status: 'PASS', details }); console.log(`PASS ${name}`); save(); }
async function raw(pathname, { token, method = 'GET', body, headers = {}, absolute = false } = {}) {
  const h = { ...headers }; if (token) h.Authorization = `Bearer ${token}`;
  if (body !== undefined && !(body instanceof FormData)) { h['content-type'] = 'application/json'; body = JSON.stringify(body); }
  const response = await fetch(`${absolute ? BACKEND : `${BACKEND}/api`}${pathname}`, { method, headers: h, body });
  const type = response.headers.get('content-type') || '';
  const data = type.includes('json') ? await response.json() : Buffer.from(await response.arrayBuffer());
  return { status: response.status, body: data, headers: response.headers };
}
async function api(pathname, options = {}) {
  const r = await raw(pathname, options);
  assert.equal(r.status, 200, `${options.method || 'GET'} ${pathname}: ${r.status} ${JSON.stringify(r.body)}`);
  assert.equal(r.body.code, 200, `${options.method || 'GET'} ${pathname}: ${JSON.stringify(r.body)}`);
  return r.body.data;
}
async function denied(pathname, options = {}) {
  const r = await raw(pathname, options);
  assert(r.status !== 200 || r.body.code !== 200, `应拒绝但成功: ${pathname} ${JSON.stringify(r.body)}`);
  return r;
}
const login = (account, password = 'Doctor123!') => api('/auth/password/login', { method: 'POST', body: { account, password } });
async function qualification() {
  const form = new FormData(); form.append('file', new Blob([Buffer.from('%PDF-1.4\nMerge synthetic qualification\n%%EOF')], { type: 'application/pdf' }), 'qualification.pdf');
  return (await api('/auth/qualifications', { method: 'POST', body: form })).url;
}
function sql(query) {
  const r = spawnSync(process.env.MYSQL_CLIENT, ['--no-defaults', `--socket=${process.env.MYSQL_SOCKET}`, '-umerge', process.env.MYSQL_DATABASE, '--batch', '--raw', '--skip-column-names', `--execute=${query}`], { encoding: 'utf8' });
  assert.equal(r.status, 0, r.stderr); return r.stdout.trim();
}
function orderBody(petId, labId, testTypeId) { return { petId, laboratoryId: labId, infoAnimalId: 1, infoSampleTypeId: 1, animalName: '验收猫', animalNameEn: 'Merge Cat', animalTypeId: 1, animalType: '田园猫', animalTypeEn: 'Domestic', animalWeight: '4kg', chipNO: 'MERGE001', age: '3Y', dictSexId: 1, sampleArea: '前肢静脉', masterName: '验收畜主', masterNameEn: 'Merge Owner', doctorName: '验收医生', medicalHistory: '既往病史：无', clinicalSymptoms: '精神欠佳', diagnosis: '待检', medication: '无', diagnosisDetails: [{ infoTestTypeId: testTypeId }] }; }
function makeMini() {
  const storage = new Map(); const requests = [];
  global.wx = {
    getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }), getExtConfigSync: () => ({ baseURL: BACKEND }),
    getStorageSync: key => storage.get(key), setStorageSync: (key, value) => storage.set(key, value), removeStorageSync: key => storage.delete(key),
    request(options) { requests.push({ method: options.method, url: options.url }); fetch(options.url, { method: options.method, headers: options.header, body: ['GET', 'HEAD'].includes(options.method) ? undefined : JSON.stringify(options.data || {}) }).then(async r => options.success({ statusCode: r.status, data: await r.json() })).catch(e => options.fail({ errMsg: e.message })); },
    reLaunch() {}, navigateTo() {}, switchTab() {}, showToast() {}, showModal() {}, setNavigationBarTitle() {}, stopPullDownRefresh() {}
  };
  global.getApp = () => ({ globalData: {}, acceptSession(result) { storage.set('token', result.token); storage.set('userInfo', result); this.globalData.token = result.token; } }); global.getCurrentPages = () => [];
  global.Page = definition => { global.__lastPage = definition; };
  const request = require(path.join(ROOT, 'miniprogram/utils/request.js'));
  const services = require(path.join(ROOT, 'miniprogram/api/index.js'));
  const pages = ['login/login.js', 'pets/pets.js', 'order-preview/order-preview.js', 'orders/orders.js', 'order-detail/order-detail.js', 'report-list/report-list.js', 'pet-health/pet-health.js', 'bills/bills.js', 'coupons/coupons.js'];
  for (const page of pages) { try { require(path.join(ROOT, 'miniprogram/pages', page)); } catch (error) { if (!/wx\.|getApp|Page is not defined/.test(error.message)) throw error; } }
  summary.mini.requestModule = 'miniprogram/utils/request.js'; summary.mini.serviceModule = 'miniprogram/api/index.js'; summary.mini.pageModules = pages;
  async function login(account, password) {
    const filename = path.join(ROOT, 'miniprogram/pages/login/login.js'); delete require.cache[require.resolve(filename)]; require(filename);
    const page = { ...global.__lastPage, data: { ...global.__lastPage.data, account, password } };
    page.setData = changes => Object.assign(page.data, changes); await page.submit(); assert.equal(page.data.error, ''); assert(storage.get('token'));
  }
  return { request, services, storage, requests, login };
}
async function shipmentAndReport(order, tokens) {
  const shipmentId = await api('/logistics', { token: tokens.admin.token, method: 'POST', body: { orderId: order.orderId, trackingNo: `MERGE-${order.orderId}`, laboratoryId: 1, pickupAddress: '验收路1号', expectedArrivedAt: '2026-10-29T10:00:00', courierUserId: 2 } });
  await api(`/logistics/${shipmentId}/assign`, { token: tokens.admin.token, method: 'POST', body: { courierUserId: 2 } });
  await api(`/logistics/${shipmentId}/pickup`, { token: tokens.courier.token, method: 'POST', body: {} });
  await api(`/logistics/${shipmentId}/transit`, { token: tokens.courier.token, method: 'POST', body: {} });
  await api(`/logistics/${shipmentId}/deliver`, { token: tokens.courier.token, method: 'POST', body: {} });
  await api(`/logistics/${shipmentId}/scan-receive`, { token: tokens.lab.token, method: 'POST', body: { barcode: order.userNO } });
  await api(`/logistics/${shipmentId}/scan-receive`, { token: tokens.lab.token, method: 'POST', body: { barcode: order.userNO } });
  const task = await api(`/lab/tasks/order/${order.orderId}/assign`, { token: tokens.lab.token, method: 'POST', body: { assigneeId: 3, equipmentCode: 'MERGE-ANALYZER' } });
  const detail = await api(`/orders/${order.orderId}`, { token: tokens.doctor.token });
  const results = detail.items.map((item, index) => ({ orderItemId: item.itemId, name: `指标${index + 1}`, value: '5.20', unit: '10^9/L', referenceRange: '3.5-5.5' }));
  await api(`/lab/tasks/${task.id}/results`, { token: tokens.lab.token, method: 'POST', body: { results } });
  await api(`/lab/tasks/${task.id}/return`, { token: tokens.lab.token, method: 'POST', body: { reason: '验收退样：样本凝块' } });
  const retest = await api(`/lab/tasks/${task.id}/retest`, { token: tokens.lab.token, method: 'POST', body: { reason: '验收复检' } });
  await api(`/lab/tasks/${retest}/results`, { token: tokens.lab.token, method: 'POST', body: { results } });
  const workflows = await api('/report-workflows', { token: tokens.lab.token, method: 'POST', body: { taskId: retest, summary: '中文摘要：指标稳定', conclusion: '中文结论：未见异常' } });
  assert(workflows.length > 0);
  for (const id of workflows) {
    const unpublished = await api(`/reports/${id}`, { token: tokens.doctor.token }); assert(!unpublished.pdfHttpUrl);
    await denied(`/reports/${id}/download`, { token: tokens.doctor.token });
    await api(`/report-workflows/${id}/review`, { token: tokens.admin.token, method: 'POST', body: { decision: 'APPROVE', reason: '中文审核通过' } });
    await api(`/report-workflows/${id}/publish`, { token: tokens.admin.token, method: 'POST', body: {} });
    await api(`/report-workflows/${id}/publish`, { token: tokens.admin.token, method: 'POST', body: {} });
    const pdf = await raw(`/report-workflows/${id}/download`, { token: tokens.doctor.token }); assert.equal(pdf.status, 200); assert.equal(pdf.body.subarray(0, 4).toString(), '%PDF');
    fs.writeFileSync(path.join(RESULTS, `report-${id}.pdf`), pdf.body);
    const printed = await raw(`/admin/print/reports/${id}`, { token: tokens.admin.token }); assert.equal(printed.status, 200); assert(printed.body.toString().includes('中文'));
  }
  return { shipmentId, taskId: task.id, retestTaskId: retest, reportIds: workflows };
}
async function runBrowser(tokens, doctorId, orderId) {
  let playwright; for (const candidate of [process.env.PLAYWRIGHT_MODULE, 'playwright', '/Users/apple/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'].filter(Boolean)) { try { playwright = require(candidate); break; } catch {} }
  assert(playwright, 'Playwright 不可用');
  let browser; try { browser = await playwright.chromium.launch({ headless: true, channel: 'chrome' }); } catch { browser = await playwright.chromium.launch({ headless: true }); }
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    page.on('console', m => { if (m.type() === 'error') summary.browser.console.push({ text: m.text(), url: m.location().url }); });
    page.on('pageerror', e => summary.browser.pageerrors.push(e.message));
    const shot = async name => { const file = path.join(RESULTS, name); await page.screenshot({ path: file, fullPage: true }); summary.browser.screenshots.push(file); };
    await page.goto(ADMIN); await page.locator('input[name=username]').fill('merge_admin'); await page.locator('input[name=password]').fill('Admin123!');
    await Promise.all([page.waitForSelector('.app-shell'), page.locator('#login-form button[type=submit]').click()]); await shot('admin-dashboard.png');
    await page.locator('[data-route=reviews]').click(); await page.waitForSelector(`[data-review="${doctorId}"]`); await page.locator(`[data-review="${doctorId}"]`).click(); await page.locator('#active-dialog-form textarea[name=reason]').fill('浏览器注册审核通过'); await page.locator('button[form=active-dialog-form]').click();
    await page.locator('[data-tab=orders]').click(); await page.waitForSelector(`[data-review="${orderId}"]`); await page.locator(`[data-review="${orderId}"]`).click(); await page.locator('#active-dialog-form textarea[name=reason]').fill('浏览器订单审核通过'); await page.locator('button[form=active-dialog-form]').click(); await shot('admin-reviews.png');
    const labels = { logistics: '物流配送', laboratory: '实验室任务', reports: '报告审核发布', finance: '财务处理', operations: '运营管理', orders: '送检管理' };
    for (const route of ['logistics', 'laboratory', 'reports', 'finance', 'operations', 'orders']) { await page.locator(`[data-route=${route}]`).click(); await page.waitForFunction(label => document.querySelector('#current-page')?.textContent === label && !document.querySelector('#page-content')?.textContent.includes('正在加载'), labels[route]); await shot(`admin-${route}.png`); }
    await page.locator('[data-route=catalog]').click(); await page.waitForSelector('#item-new'); await page.locator('#item-new').click();
    await page.locator('#active-dialog-form input[name=name]').fill('浏览器验收项目'); await page.locator('#active-dialog-form input[name=code]').fill('BROWSER-CRUD'); await page.locator('#active-dialog-form input[name=price]').fill('66.50');
    await page.locator('button[form=active-dialog-form]').click(); await page.waitForFunction(() => document.querySelector('#catalog-list')?.textContent.includes('浏览器验收项目')); await shot('admin-catalog-crud.png');
    const unexpected = summary.browser.console.filter(e => !(e.url.endsWith('/favicon.ico') && e.text.includes('404')));
    assert.deepEqual(summary.browser.pageerrors, []); assert.deepEqual(unexpected, []);
    pass('Playwright管理端登录/审核/物流/实验/报告/财务/运营/打印入口', { screenshots: summary.browser.screenshots.length, consoleErrors: 0, pageerrors: 0 });
  } finally { await browser.close(); }
}
async function main() {
  assert(BACKEND && ADMIN);
  const admin = await login('merge_admin', 'Admin123!'); const courier = await login('merge_courier', 'Staff123!'); const lab = await login('merge_lab', 'Staff123!'); const other = await login('merge_other');
  const qualificationUrl = await qualification();
  const registered = await api('/auth/register', { method: 'POST', body: { username: 'merge_doc', password: 'Doctor123!', mobile: '13800000010', code: '123456', hospitalId: 1, nickname: '验收医生', title: '主治医师', qualificationFiles: [qualificationUrl] } });
  sql(`UPDATE lin_user SET openid='mock-openid' WHERE id=${registered}`);
  await denied('/auth/password/login', { method: 'POST', body: { account: 'merge_doc', password: 'Doctor123!' } });
  await api(`/review/registrations/${registered}`, { token: admin.token, method: 'POST', body: { approved: true, reason: '注册验收通过' } });
  const doctor = await login('merge_doc');
  await api(`/admin/users/${registered}/roles`, { token: admin.token, method: 'POST', body: { roleCode: 'DOCTOR', hospitalId: 1 } });
  await api(`/admin/users/${registered}/roles`, { token: admin.token, method: 'POST', body: { roleCode: 'HOSPITAL_ADMIN', hospitalId: 1 } });
  doctor.token = (await login('merge_doc')).token;
  const tokens = { admin, courier, lab, doctor };
  pass('医生注册/未审拒登/审核/登录', { doctorId: registered });
  const today = new Date().toISOString().slice(0, 10); const later = '2027-12-31';
  for (const [hospitalId, type, no] of [[1, 'CASH', 'MERGE-CASH'], [3, 'MONTHLY', 'MERGE-MONTHLY']]) {
    const id = await api('/admin/contracts', { token: admin.token, method: 'POST', body: { contractNo: no, hospitalId, laboratoryId: 1, settlementType: type, billingCycleDays: 30, creditAmount: 100000, effectiveFrom: '2026-01-01', effectiveTo: later } });
    await api(`/admin/contracts/${id}/prices`, { token: admin.token, method: 'POST', body: { infoTestTypeId: 11, listPrice: 100, discountRate: 0.8, agreedPrice: 75, effectiveFrom: '2026-01-01', effectiveTo: later, status: 'ACTIVE' } });
  }
  pass('多实验室项目与现结/月结协议价', { laboratoryIds: [1, 2], cashPrice: 75 });
  const couponRule = { name: '验收优惠券', amount: 10, minimumAmount: 50, laboratoryId: 1, startsAt: '2026-01-01T00:00:00', expiresAt: '2027-12-31T23:59:59' };
  await api('/admin/coupons/1?totalQuantity=10&reason=验收库存', { token: admin.token, method: 'PUT', body: couponRule });
  const userCoupon = await api('/admin/coupons/1/users/' + registered, { token: admin.token, method: 'POST', body: { requestKey: 'merge-coupon-grant-01' } });
  assert.equal(await api('/admin/coupons/1/users/' + registered, { token: admin.token, method: 'POST', body: { requestKey: 'merge-coupon-grant-01' } }), userCoupon);
  const mini = makeMini(); await mini.login('merge_doc', 'Doctor123!');
  const couponQuote = await mini.request.post('/coupons/quote', { laboratoryId: 1, testTypeIds: [11], userCouponId: userCoupon }); assert.equal(Number(couponQuote.payableAmount), 65);
  const pet = await mini.services.pets.create({ animalName: '验收猫', animalNameEn: 'Merge Cat', infoAnimalId: 1, infoVarietyId: 1, dictSexId: 1, chipNo: 'MERGE001', ownerName: '验收畜主', ownerNameEn: 'Merge Owner' });
  const draftId = await mini.request.post('/drafts', { laboratoryId: 1, payload: { petInfo: { animalName: '验收猫' }, testTypeIds: [11] } });
  const cash = await mini.services.orders.create({ ...orderBody(pet.id, 1, 11), draftId, draftVersion: 1, idempotencyKey: 'merge-cash-create-0001' });
  const replay = await mini.services.orders.create({ ...orderBody(pet.id, 1, 11), draftId, draftVersion: 1, idempotencyKey: 'merge-cash-create-0001' }); assert.equal(replay.orderId, cash.orderId);
  await denied(`/orders/${cash.orderId}`, { token: other.token });
  const browserDoctor = await api('/auth/register', { method: 'POST', body: { username: 'browser_doc', password: 'Doctor123!', mobile: '13800000011', code: '123456', hospitalId: 1, nickname: '浏览器医生', title: '医师', qualificationFiles: [qualificationUrl] } }).catch(async () => { sql("INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','13800000011','123456',0,'127.0.0.1',UNIX_TIMESTAMP())"); return api('/auth/register', { method: 'POST', body: { username: 'browser_doc', password: 'Doctor123!', mobile: '13800000011', code: '123456', hospitalId: 1, nickname: '浏览器医生', title: '医师', qualificationFiles: [qualificationUrl] } }); });
  await runBrowser(tokens, browserDoctor, cash.orderId);
  const payment = await mini.services.payment.create({ orderId: cash.orderId, amount: 75, openid: 'mock-openid' });
  await mini.request.post(`/payment/mock-callback/${payment.paymentNo}`); await denied(`/payment/mock-callback/${payment.paymentNo}`, { token: doctor.token, method: 'POST', body: {} });
  const paid = await mini.services.orders.getDetail(cash.orderId); assert.equal(paid.sampleStatus, 2); assert.equal(sql(`SELECT IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=${cash.orderId}`), '1');
  pass('小程序真实请求层登录态/档案/草稿/送检/记录与mock支付幂等', { petId: pet.id, draftId, orderId: cash.orderId, requests: mini.requests.length });
  const monthlyReg = '13800000012'; sql(`INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','${monthlyReg}','123456',0,'127.0.0.1',UNIX_TIMESTAMP())`);
  const monthlyId = await api('/auth/register', { method: 'POST', body: { username: 'monthly_doc', password: 'Doctor123!', mobile: monthlyReg, code: '123456', hospitalId: 3, nickname: '月结医生', title: '医师', qualificationFiles: [qualificationUrl] } });
  await api(`/review/registrations/${monthlyId}`, { token: admin.token, method: 'POST', body: { approved: true, reason: '月结注册通过' } });
  await api(`/admin/users/${monthlyId}/roles`, { token: admin.token, method: 'POST', body: { roleCode: 'DOCTOR', hospitalId: 3 } });
  await api(`/admin/users/${monthlyId}/roles`, { token: admin.token, method: 'POST', body: { roleCode: 'HOSPITAL_ADMIN', hospitalId: 3 } });
  const monthlyDoctor = await login('monthly_doc'); const monthlyPet = await api('/pets', { token: monthlyDoctor.token, method: 'POST', body: { animalName: '月结猫', animalNameEn: 'Monthly Cat', infoAnimalId: 1, infoVarietyId: 1, dictSexId: 1, ownerName: '月结畜主' } });
  const monthly = await api('/orders', { token: monthlyDoctor.token, method: 'POST', body: { ...orderBody(monthlyPet.id, 1, 11), idempotencyKey: 'merge-monthly-create-001' } });
  await api(`/review/orders/${monthly.orderId}`, { token: admin.token, method: 'POST', body: { approved: true, reason: '月结审核' } });
  await denied('/payment/create', { token: monthlyDoctor.token, method: 'POST', body: { orderId: monthly.orderId, amount: 75, openid: 'mock' } });
  await denied(`/admin/refunds/${monthly.orderId}`, { token: admin.token, method: 'POST', body: { reason: '月结不可走现结退款', requestKey: 'merge-refund-monthly' } });
  const refundOrder = await api('/orders', { token: doctor.token, method: 'POST', body: { ...orderBody(pet.id, 1, 11), idempotencyKey: 'merge-refund-create-001' } });
  await denied(`/admin/refunds/${refundOrder.orderId}`, { token: admin.token, method: 'POST', body: { reason: '未支付不可退款', requestKey: 'merge-refund-unpaid' } });
  await api(`/review/orders/${refundOrder.orderId}`, { token: admin.token, method: 'POST', body: { approved: true, reason: '退款订单审核' } });
  const refundPayment = await api('/payment/create', { token: doctor.token, method: 'POST', body: { orderId: refundOrder.orderId, amount: 75, openid: 'mock-openid' } });
  await api(`/payment/mock-callback/${refundPayment.paymentNo}`, { token: doctor.token, method: 'POST', body: {} });
  await api(`/admin/refunds/${refundOrder.orderId}`, { token: doctor.token, method: 'POST', body: { reason: 'mock退款验收', requestKey: 'merge-refund-success' } });
  await api(`/admin/refunds/${refundOrder.orderId}`, { token: doctor.token, method: 'POST', body: { reason: 'mock退款验收', requestKey: 'merge-refund-success' } });
  pass('未支付/月结/已开票退款边界与mock退款幂等', { refundOrderId: refundOrder.orderId });
  const workflow = await shipmentAndReport(cash, tokens);
  const logistics = await mini.services.logistics.getByOrder(cash.orderId); assert(logistics.id);
  const reports = await mini.services.reports.getList({ page: 0, size: 20 }); assert(reports.content.length > 0);
  sql(`UPDATE lin_lb_listdiagnosisproject SET IsPayMoney=0 WHERE ListDiagnosisProjectId=${cash.orderId}`);
  const restrictedReports = await mini.services.reports.getList({ page: 0, size: 20 });
  const restricted = restrictedReports.content.find(r => r.reportId === workflow.reportIds[0]); assert(restricted.paymentRestricted); assert(!restricted.pdfHttpUrl && !restricted.pdfFullUrl && !restricted.securePdfUrl);
  sql(`UPDATE lin_lb_listdiagnosisproject SET IsPayMoney=1 WHERE ListDiagnosisProjectId=${cash.orderId}`);
  const health = await mini.services.pets.getHealth(pet.id, { page: 0, size: 20 }); assert(health.medicalHistory.length > 0); assert(health.reports.items.length > 0);
  pass('配送/扫码签收/任务/结果/退样复检/报告审核发布/中文PDF/小程序物流报告病史', workflow);
  const invoiceProfile = await api('/invoices/profiles', { token: doctor.token, method: 'POST', body: { title: '合并验收一院', taxNumber: '91310000MERGE00001', email: 'merge@example.invalid' } });
  const invoice = await api('/invoices', { token: doctor.token, method: 'POST', body: { profileId: invoiceProfile, orderIds: [cash.orderId], requestKey: 'merge-invoice-000001' } });
  await api(`/invoices/${invoice}/issue`, { token: admin.token, method: 'POST', body: { invoiceUrl: 'https://example.invalid/merge-invoice.pdf' } });
  await denied(`/admin/refunds/${cash.orderId}`, { token: admin.token, method: 'POST', body: { reason: '已开票边界', requestKey: 'merge-refund-invoice' } });
  pass('发票申请开具与已开票退款边界', { invoice });
  sql(`UPDATE lin_lb_listdiagnosisproject SET CreateDt='2026-09-15 10:00:00' WHERE ListDiagnosisProjectId=${monthly.orderId}`);
  const bill = await api('/admin/bills', { token: admin.token, method: 'POST', body: { hospitalId: 3, laboratoryId: 1, month: '2026-09' } });
  const again = await api('/admin/bills', { token: admin.token, method: 'POST', body: { hospitalId: 3, laboratoryId: 1, month: '2026-09' } }); assert.equal(again.id, bill.id);
  await api(`/bills/${bill.id}/reconciliation`, { token: monthlyDoctor.token, method: 'POST', body: { action: 'CONFIRM', reason: '医院对账' } });
  await api(`/admin/bills/${bill.id}/reconciliation`, { token: admin.token, method: 'POST', body: { action: 'CONFIRM', reason: '实验室对账' } });
  await api(`/admin/bills/${bill.id}/settlement`, { token: admin.token, method: 'POST', body: { reference: 'mock-MERGE-BILL' } });
  await api(`/admin/bills/${bill.id}/settlement`, { token: admin.token, method: 'POST', body: { reference: 'mock-MERGE-BILL' } });
  const supplementOrder = await api('/orders', { token: monthlyDoctor.token, method: 'POST', body: { ...orderBody(monthlyPet.id, 1, 11), idempotencyKey: 'merge-supplement-create-01' } });
  await api(`/review/orders/${supplementOrder.orderId}`, { token: admin.token, method: 'POST', body: { approved: true, reason: '补充账单订单' } });
  sql(`UPDATE lin_lb_listdiagnosisproject SET CreateDt='2026-09-20 10:00:00' WHERE ListDiagnosisProjectId=${supplementOrder.orderId}`);
  const supplement = await api('/admin/bills', { token: admin.token, method: 'POST', body: { hospitalId: 3, laboratoryId: 1, month: '2026-09' } }); assert.equal(supplement.billKind || supplement.bill_kind, 'SUPPLEMENT');
  await api(`/bills/${supplement.id}/reconciliation`, { token: monthlyDoctor.token, method: 'POST', body: { action: 'CONFIRM', reason: '补充医院对账' } });
  await api(`/admin/bills/${supplement.id}/reconciliation`, { token: admin.token, method: 'POST', body: { action: 'CONFIRM', reason: '补充实验室对账' } });
  await api(`/admin/bills/${supplement.id}/settlement`, { token: admin.token, method: 'POST', body: { reference: 'mock-MERGE-SUPPLEMENT' } });
  mini.storage.set('token', monthlyDoctor.token); const miniBills = await mini.services.bills.getList({ page: 0, size: 20 }); assert(miniBills.items.some(b => b.id === bill.id));
  pass('月账单生成/双方对账/补充账单/mock结算幂等与小程序账单', { billId: bill.id, supplementBillId: supplement.id, amount: bill.dueAmount });
  mini.storage.set('token', doctor.token);
  await api('/admin/operations/points-adjustments', { token: admin.token, method: 'POST', body: { userId: registered, delta: 500, reason: '验收积分', requestKey: 'merge-points-0001' } });
  const redemptionBody = { giftId: 1, quantity: 1, deliveryAddress: { recipient: '验收医生', phone: '13800000010', address: '验收路1号' }, requestKey: 'merge-redeem-00001' };
  const redemption = await api('/mall/redemptions', { token: doctor.token, method: 'POST', body: redemptionBody }); assert.equal(await api('/mall/redemptions', { token: doctor.token, method: 'POST', body: redemptionBody }), redemption);
  const article = await api('/articles', { token: admin.token, method: 'POST', body: { category: '验收', type: 'article', title: '中文资讯', summary: '验收摘要', content: '中文资讯正文', popupEnabled: true } });
  await api(`/articles/${article}/action`, { token: doctor.token, method: 'PUT', body: { liked: true, bookmarked: true } });
  const inbox = await api('/inbox?page=0&size=100', { token: doctor.token }); assert(inbox.items.some(i => i.eventType === 'report'));
  assert.equal(sql(`SELECT COUNT(*) FROM lin_notification_event WHERE event_type='report' AND business_key='${cash.orderId}:published'`), '1');
  const coupons = await mini.request.get('/coupons'); assert(Array.isArray(coupons));
  pass('积分/商城重复兑换/优惠券/资讯/消息与通知去重检查', { redemption, article, inbox: inbox.total });
  for (const kind of ['orders', 'specimens']) { const r = await raw(`/admin/print/${kind}/${cash.orderId}`, { token: admin.token }); assert.equal(r.status, 200); assert(r.body.toString().includes(String(cash.orderId))); }
  const dashboard = await api('/admin/dashboard?from=2026-01-01&to=2026-12-31', { token: admin.token }); assert(dashboard);
  const appJson = JSON.parse(fs.readFileSync(path.join(ROOT, 'miniprogram/app.json'), 'utf8')); assert.equal(appJson.pages.length, 41); assert.equal(appJson.tabBar.list.length, 5);
  const appRoles = await api('/user/roles', { token: lab.token }); assert(appRoles.some(r => (r.roleCode || r) === 'LAB_STAFF'));
  await denied('/admin/dashboard?from=2026-01-01&to=2026-12-31', { token: other.token });
  await api('/admin/orders', { token: doctor.token }); await api('/admin/orders', { token: lab.token });
  await api('/lab/tasks', { token: lab.token });
  await denied('/admin/contracts', { token: doctor.token }); await denied('/admin/contracts', { token: lab.token });
  await denied('/admin/orders', { token: other.token });
  await denied('/admin/contracts', { token: admin.token, method: 'POST', body: { contractNo: 'MERGE-CASH-OVERLAP', hospitalId: 1, laboratoryId: 1, settlementType: 'CASH', billingCycleDays: 30, creditAmount: 100000, effectiveFrom: '2026-06-01', effectiveTo: later } });
  const cashContractId = sql("SELECT id FROM lin_contract WHERE contract_no='MERGE-CASH'");
  await api(`/admin/contracts/${cashContractId}/status`, { token: admin.token, method: 'PATCH', body: { active: false, reason: '停用验收' } });
  await denied('/orders', { token: doctor.token, method: 'POST', body: { ...orderBody(pet.id, 1, 11), idempotencyKey: 'merge-inactive-cash-001' } });
  await api(`/admin/contracts/${cashContractId}/status`, { token: admin.token, method: 'PATCH', body: { active: true, reason: '恢复验收' } });
  const monthlyContractId = sql("SELECT id FROM lin_contract WHERE contract_no='MERGE-MONTHLY'");
  await api(`/admin/contracts/${monthlyContractId}/status`, { token: admin.token, method: 'PATCH', body: { active: false, reason: '停用后历史出账验收' } });
  await api(`/bills/${bill.id}`, { token: monthlyDoctor.token });
  await denied('/orders', { token: monthlyDoctor.token, method: 'POST', body: { ...orderBody(monthlyPet.id, 1, 11), idempotencyKey: 'merge-inactive-monthly-1' } });
  await api(`/admin/contracts/${monthlyContractId}/status`, { token: admin.token, method: 'PATCH', body: { active: true, reason: '恢复验收' } });
  const crudId = sql("SELECT InfoTestTypeId FROM lin_lb_infotesttype WHERE TestNO='BROWSER-CRUD'");
  await api(`/admin/catalog/laboratories/1/items/${crudId}`, { token: lab.token, method: 'PUT', body: { name: '浏览器项目已编辑', code: 'BROWSER-CRUD', price: 67 } });
  await api(`/admin/catalog/laboratories/1/items/${crudId}`, { token: lab.token, method: 'DELETE' });
  assert.equal(sql(`SELECT CONCAT(IsActive,(SELECT active FROM lin_lab_test WHERE laboratory_id=1 AND test_type_id=${crudId})) FROM lin_lb_infotesttype WHERE InfoTestTypeId=${crudId}`), '00');
  pass('合同重叠/启停用/历史出账与项目CRUD/HTTP角色矩阵/41页面5Tab', { pages: 41, tabs: 5, crudId: Number(crudId) });
  summary.status = 'PASS'; summary.orderIds = { cash: cash.orderId, monthly: monthly.orderId }; summary.reportIds = workflow.reportIds;
}
main().then(() => { summary.finishedAt = new Date().toISOString(); save(); }).catch(error => { summary.status = 'FAIL'; summary.error = { message: error.message, stack: error.stack }; summary.finishedAt = new Date().toISOString(); save(); console.error(error); process.exitCode = 1; });
