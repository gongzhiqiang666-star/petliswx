import { formData } from './shared.js';
import { formatDate, idempotencyKey, moneyYuan, pageItems, value, cell } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { canFinanceAction } from '../permissions.js';
import { hospitalOptions } from '../options.js';

export async function render(container, context) {
  const { api, store } = context;
  const user = store.getState().user;
  let tab = 'bills';
  let filters = { page: 0, size: 20 };
  container.innerHTML = `${pageHeader('财务处理', '月结账单双方对账、统一 mock 结算、发票与现结 mock 退款')}
    <div class="tabs"><button class="tab active" data-tab="bills">月结账单</button><button class="tab" data-tab="invoices">发票处理</button><button class="tab" data-tab="refund">现结退款</button></div><section class="panel"><div id="finance-tools"></div><div id="finance-list"></div></section>`;
  const list = container.querySelector('#finance-list');
  const tools = container.querySelector('#finance-tools');
  async function load() {
    list.innerHTML = renderState('loading');
    if (tab === 'refund') {
      tools.innerHTML = '';
      list.innerHTML = canFinanceAction('refund', user) ? `<div class="narrow-form"><h2>现结 mock 退款</h2><p class="form-hint">仅处理已支付现结订单，不调用真实支付。已申请或开具发票的订单会被服务端拒绝。</p><form id="refund-form" class="form-grid">${field({ name: 'orderId', label: '送检单 ID', type: 'number', required: true, min: 1 })}${field({ name: 'reason', label: '退款原因', required: true, wide: true })}<button class="button danger" type="submit">确认 mock 退款</button></form></div>` : renderState('forbidden');
      list.querySelector('#refund-form')?.addEventListener('submit', async (event) => {
        event.preventDefault(); const data = formData(event.currentTarget);
        if (!globalThis.confirm(`确认对送检单 #${data.orderId} 执行 mock 退款？`)) return;
        try { await api.request(`/admin/refunds/${data.orderId}`, { method: 'POST', data: { reason: data.reason.trim(), requestKey: idempotencyKey() } }); context.toast('mock 退款已处理'); event.target.reset(); }
        catch (error) { context.toast(error.message, 'error'); }
      });
      return;
    }
    tools.innerHTML = tab === 'bills' && canFinanceAction('generate', user) ? '<button class="button" id="bill-generate">生成月结账单</button>' : '';
    tools.querySelector('#bill-generate')?.addEventListener('click', openGenerate);
    try {
      const data = await api.request(tab === 'bills' ? '/admin/bills' : '/review/invoices', { query: filters });
      if (tab === 'bills') list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无月结账单', columns: [
        { label: '账单号', render: (row) => cell(row, 'billNo') }, { label: '医院', render: (row) => cell(row, 'hospitalId') },
        { label: '账期', render: (row) => `${value(row, 'periodStart')} ~ ${value(row, 'periodEnd')}` },
        { label: '应付（元）', render: (row) => moneyYuan(value(row, 'dueAmount', 0)) }, { label: '未结（元）', render: (row) => moneyYuan(value(row, 'outstandingAmount', 0)) },
        { label: '到期日', render: (row) => cell(row, 'dueDate') }, { label: '逾期天数', render: (row) => cell(row, 'overdueDays') }, { label: '状态', render: (row) => badge(value(row, 'status', '')) }
      ], actions: (row) => `<button class="button text" data-bill="${row.id}">明细</button>${canFinanceAction('reconcile', user) ? `<button class="button text" data-reconcile="${row.id}">对账</button>` : ''}${canFinanceAction('settle', user) ? `<button class="button small" data-settle="${row.id}">mock 结算</button>` : ''}` }) + pagination(data);
      else list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无待审发票申请', columns: [
        { label: '申请编号', render: (row) => cell(row, 'id') }, { label: '用户 ID', render: (row) => cell(row, 'userId') },
        { label: '金额（元）', render: (row) => moneyYuan(row.amount) }, { label: '申请时间', render: (row) => formatDate(value(row, 'createdAt', null)) }
      ], actions: (row) => canFinanceAction('issue', user) ? `<button class="button small" data-issue="${row.id}">开具</button><button class="button danger text" data-reject="${row.id}">拒绝</button>` : '' }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  async function openGenerate() {
    try {
      const [hospitals, laboratories] = await Promise.all([api.request('/admin/hospitals', { query: { status: 'ACTIVE' } }), api.request('/admin/catalog/laboratories')]);
      context.dialog({ title: '生成医院月结账单', submitLabel: '生成账单', body: `<div class="form-grid">${field({ name: 'hospitalId', label: '医院', options: hospitalOptions(pageItems(hospitals)), required: true })}${field({ name: 'laboratoryId', label: '实验室', options: pageItems(laboratories).map((lab) => ({ value: lab.id, label: lab.name })), required: true })}${field({ name: 'month', label: '账期月份', type: 'month', required: true })}</div>`, async onSubmit(form) {
        const data = formData(form); await api.request('/admin/bills', { method: 'POST', data: { hospitalId: Number(data.hospitalId), laboratoryId: Number(data.laboratoryId), month: data.month } }); context.toast('月结账单已生成'); load();
      } });
    } catch (error) { context.toast(error.message, 'error'); }
  }
  container.addEventListener('click', async (event) => {
    const tabButton = event.target.closest('[data-tab]'); if (tabButton) { tab = tabButton.dataset.tab; filters = { page: 0, size: 20 }; container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton)); load(); }
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const bill = event.target.closest('[data-bill]'); if (bill) showBill(bill.dataset.bill);
    const reconcile = event.target.closest('[data-reconcile]'); if (reconcile) openReconcile(reconcile.dataset.reconcile);
    const settle = event.target.closest('[data-settle]'); if (settle && globalThis.confirm(`确认对账单 #${settle.dataset.settle} 统一 mock 结算？`)) {
      try { await api.request(`/admin/bills/${settle.dataset.settle}/settlement`, { method: 'POST', data: { reference: `mock-${idempotencyKey()}` } }); context.toast('账单已 mock 结算'); load(); } catch (error) { context.toast(error.message, 'error'); }
    }
    const reject = event.target.closest('[data-reject]'); if (reject) context.dialog({ title: '拒绝发票申请', submitLabel: '确认拒绝', body: field({ name: 'reason', label: '拒绝原因', type: 'textarea', required: true, wide: true }), async onSubmit(form) {
      await api.request(`/invoices/${reject.dataset.reject}/reject`, { method: 'POST', data: { reason: formData(form).reason.trim() } }); context.toast('发票申请已拒绝'); load();
    } });
    const issue = event.target.closest('[data-issue]'); if (issue) context.dialog({ title: '开具电子发票', submitLabel: '确认开具', body: field({ name: 'invoiceUrl', label: '电子发票 HTTPS 地址', required: true, wide: true }), async onSubmit(form) {
      await api.request(`/invoices/${issue.dataset.issue}/issue`, { method: 'POST', data: { invoiceUrl: formData(form).invoiceUrl.trim() } }); context.toast('发票已开具'); load();
    } });
  });
  async function showBill(id) {
    try {
      const [bill, entries] = await Promise.all([api.request(`/admin/bills/${id}`), api.request(`/admin/bills/${id}/items`, { query: { size: 100 } })]);
      context.dialog({ title: `账单 #${id}`, wide: true, body: `${renderDetail([
        { label: '账单号', value: value(bill, 'billNo') }, { label: '总金额（元）', value: moneyYuan(value(bill, 'totalAmount', 0)) },
        { label: '优惠（元）', value: moneyYuan(value(bill, 'discountAmount', 0)) }, { label: '未结（元）', value: moneyYuan(value(bill, 'outstandingAmount', 0)) },
        { label: '状态', value: value(bill, 'status') }, { label: '对账时间', value: formatDate(value(bill, 'reconciledAt', null)) }
      ])}<h3>账单明细</h3>${renderTable({ rows: pageItems(entries), columns: [{ label: '送检单', render: (row) => cell(row, 'orderNo') }, { label: '订单 ID', render: (row) => cell(row, 'orderId') }, { label: '项目', render: (row) => cell(row, 'description') }, { label: '最终金额（元）', render: (row) => moneyYuan(value(row, 'finalAmount', 0)) }] })}` });
    } catch (error) { context.toast(error.message, 'error'); }
  }
  function openReconcile(id) {
    context.dialog({ title: '账单对账', submitLabel: '提交对账结果', body: `<div class="form-grid">${field({ name: 'action', label: '处理结果', options: [{ value: 'CONFIRM', label: '确认无误' }, { value: 'DISPUTE', label: '提出异议' }] })}${field({ name: 'reason', label: '异议原因（提出异议必填）', type: 'textarea', wide: true })}</div>`, async onSubmit(form) {
      await api.request(`/admin/bills/${id}/reconciliation`, { method: 'POST', data: formData(form) }); context.toast('对账结果已保存'); load();
    } });
  }
  await load();
}
