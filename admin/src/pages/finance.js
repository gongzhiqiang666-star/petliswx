import { billPayload } from '../adapters.js';
import { formData } from './shared.js';
import { formatDate, idempotencyKey, money, moneyYuan, pageItems, value, cell } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { canFinanceAction } from '../permissions.js';
import { hospitalOptions } from '../options.js';

export async function render(container, context) {
  const { api } = context;
  let tab = 'bills';
  let filters = { page: 0, size: 20, status: '' };
  container.innerHTML = `${pageHeader('财务处理', '月结账单对账结算、发票开具与订单退款')}
    <div class="tabs"><button class="tab active" data-tab="bills">月结账单</button><button class="tab" data-tab="invoices">发票处理</button><button class="tab" data-tab="refund">订单退款</button></div>
    <section class="panel"><div id="finance-tools"></div><div id="finance-list"></div></section>`;
  const list = container.querySelector('#finance-list');
  const tools = container.querySelector('#finance-tools');
  async function load() {
    list.innerHTML = renderState('loading');
    if (tab === 'refund') {
      tools.innerHTML = '';
      list.innerHTML = `<div class="narrow-form"><h2>订单退款</h2><p class="form-hint">仅已支付订单可退款。操作会真实冲减钱包并回退相关积分，请核对订单后执行。</p><form id="refund-form" class="form-grid">${field({ name: 'orderId', label: '送检单 ID', type: 'number', required: true, min: 1 })}<button class="button danger" type="submit">确认退款</button></form></div>`;
      list.querySelector('#refund-form').addEventListener('submit', async (event) => {
        event.preventDefault(); const id = formData(event.currentTarget).orderId;
        if (!globalThis.confirm(`确认对送检单 #${id} 执行退款？此操作不可由前端撤销。`)) return;
        try { await api.request(`/orders/${id}/refund`, { method: 'POST', headers: { 'Idempotency-Key': idempotencyKey() } }); context.toast('订单退款已处理'); event.target.reset(); }
        catch (error) { context.toast(error.message, 'error'); }
      });
      return;
    }
    tools.innerHTML = tab === 'bills'
      ? canFinanceAction('generate', context.store.getState().user) ? '<button class="button" id="bill-generate">生成月结账单</button>' : '<span class="form-hint">查看本院账单并进行对账与结算。</span>'
      : canFinanceAction('issue', context.store.getState().user) ? '<span class="form-hint">填写真实电子发票 HTTPS 地址后开具；拒绝需填写原因。</span>' : '<span class="form-hint">查看待处理发票申请。</span>';
    tools.querySelector('#bill-generate')?.addEventListener('click', openGenerate);
    try {
      const data = await api.request(tab === 'bills' ? '/bills' : '/review/invoices', { query: filters });
      list.innerHTML = tab === 'bills' ? renderTable({ rows: pageItems(data), emptyText: '暂无月结账单', columns: [
        { label: '账单号', render: (row) => cell(row, 'billNo') }, { label: '医院', render: (row) => cell(row, 'hospitalId') },
        { label: '账期', render: (row) => `${value(row, 'periodStart')} ~ ${value(row, 'periodEnd')}` },
        { label: '账单金额', render: (row) => money(value(row, 'totalCents', 0)) }, { label: '未结金额', render: (row) => money(value(row, 'outstandingCents', 0)) },
        { label: '到期日', render: (row) => cell(row, 'dueDate') }, { label: '状态', render: (row) => badge(value(row, 'status', '')) }
      ], actions: (row) => `<button class="button text" data-bill="${value(row, 'id')}">明细</button><button class="button text" data-reconcile="${value(row, 'id')}">对账</button><button class="button small" data-settle="${value(row, 'id')}">结算</button>` }) + pagination(data)
        : renderTable({ rows: pageItems(data), emptyText: '暂无待审发票申请', columns: [
          { label: '申请编号', render: (row) => cell(row, 'id') }, { label: '用户 ID', render: (row) => cell(row, 'userId') },
          { label: '金额（元）', render: (row) => moneyYuan(row.amount) }, { label: '状态', render: () => badge('PENDING') },
          { label: '申请时间', render: (row) => formatDate(value(row, 'createdAt', null)) }
        ], actions: (row) => canFinanceAction('issue', context.store.getState().user) ? `<button class="button small" data-issue="${value(row, 'id')}">开具</button><button class="button danger text" data-reject="${value(row, 'id')}">拒绝</button>` : '' }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  async function openGenerate() {
    try {
      const hospitals = pageItems(await api.request('/admin/hospitals', { query: { status: 'ACTIVE' } }));
      context.dialog({ title: '生成医院月结账单', submitLabel: '生成账单', body: `<div class="form-grid">${field({ name: 'hospitalId', label: '医院', options: hospitalOptions(hospitals), required: true })}${field({ name: 'billingMonth', label: '账期月份', type: 'month', required: true })}${field({ name: 'dueDate', label: '付款截止日', type: 'date', required: true })}</div>`, async onSubmit(form) {
        await api.request('/bills/generate', { method: 'POST', data: billPayload(formData(form)) }); context.toast('月结账单已生成'); load();
      } });
    } catch (error) { context.toast(error.message, 'error'); }
  }
  container.addEventListener('click', async (event) => {
    const tabButton = event.target.closest('[data-tab]'); if (tabButton) { tab = tabButton.dataset.tab; filters = { page: 0, size: 20, status: '' }; container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton)); load(); }
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const bill = event.target.closest('[data-bill]'); if (bill) showBill(bill.dataset.bill);
    const reconcile = event.target.closest('[data-reconcile]'); if (reconcile) openReconcile(reconcile.dataset.reconcile);
    const settle = event.target.closest('[data-settle]'); if (settle && globalThis.confirm(`确认结算账单 #${settle.dataset.settle}？将从当前账号钱包实际扣款。`)) {
      try { await api.request(`/bills/${settle.dataset.settle}/settle`, { method: 'POST', headers: { 'Idempotency-Key': idempotencyKey() } }); context.toast('账单已结算'); load(); } catch (error) { context.toast(error.message, 'error'); }
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
      const [bill, entries] = await Promise.all([api.request(`/bills/${id}`), api.request(`/bills/${id}/items`, { query: { size: 100 } })]);
      context.dialog({ title: `账单 #${id}`, wide: true, body: `${renderDetail([
        { label: '账单号', value: value(bill, 'billNo') }, { label: '医院 ID', value: value(bill, 'hospitalId') },
        { label: '总金额', value: money(value(bill, 'totalCents', 0)) }, { label: '未结金额', value: money(value(bill, 'outstandingCents', 0)) },
        { label: '状态', value: value(bill, 'status') }, { label: '对账时间', value: formatDate(value(bill, 'reconciledAt', null)) },
        { label: '异议原因', value: value(bill, 'disputeReason'), wide: true }
      ])}<h3>账单明细</h3>${renderTable({ rows: pageItems(entries), columns: [{ label: '送检单', render: (row) => cell(row, 'orderNo') }, { label: '订单 ID', render: (row) => cell(row, 'orderId') }, { label: '金额', render: (row) => money(value(row, 'amountCents', 0)) }] })}` });
    } catch (error) { context.toast(error.message, 'error'); }
  }
  function openReconcile(id) {
    context.dialog({ title: '账单对账', submitLabel: '提交对账结果', body: `<div class="form-grid">${field({ name: 'action', label: '处理结果', options: [{ value: 'CONFIRM', label: '确认无误' }, { value: 'DISPUTE', label: '提出异议' }] })}${field({ name: 'reason', label: '异议原因（提出异议必填）', type: 'textarea', wide: true })}</div>`, async onSubmit(form) {
      await api.request(`/bills/${id}/reconcile`, { method: 'POST', data: formData(form) }); context.toast('对账结果已保存'); load();
    } });
  }
  await load();
}
