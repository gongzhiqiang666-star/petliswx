import { formData } from './shared.js';
import { cell, formatDate, moneyYuan, pageItems, value } from '../utils.js';
import { badge, field, pageHeader, pagination, renderState, renderTable } from '../components/ui.js';

export async function render(container, { api, toast }) {
  let filters = { page: 0, size: 20, status: '' };
  container.innerHTML = `${pageHeader('送检管理', '查询统一旧订单主表并打印送检单与可扫描标本标签')}
    <section class="panel"><form id="order-filter" class="toolbar">${field({ name: 'status', label: '订单状态', options: [{ value: '', label: '全部' }, { value: '0', label: '待审核' }, { value: '-3', label: '待支付' }, { value: '2', label: '等待报告' }, { value: '3', label: '已完成' }, { value: '4', label: '已取消' }] })}<button class="button secondary">查询</button></form><div id="order-list"></div></section>`;
  const list = container.querySelector('#order-list');
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const data = await api.request('/admin/orders', { query: filters });
      list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无送检订单', columns: [
        { label: '订单', render: (row) => `<strong>${cell(row, 'orderNo')}</strong><small class="cell-sub">旧 ID ${cell(row, 'orderId')}</small>` },
        { label: '医院', render: (row) => cell(row, 'hospitalName') }, { label: '宠物', render: (row) => cell(row, 'animalName') },
        { label: '金额（元）', render: (row) => moneyYuan(row.amount) }, { label: '结算', render: (row) => value(row, 'settlementMode', 'cash') === 'monthly' ? '月结' : '现结' },
        { label: '状态', render: (row) => badge(value(row, 'status', '')) }, { label: '创建时间', render: (row) => formatDate(value(row, 'createdAt', null)) }
      ], actions: (row) => `<button class="button text" data-print-order="${row.orderId}">送检单</button><button class="button text" data-print-label="${row.orderId}">标本标签</button>` }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#order-filter').addEventListener('submit', (event) => { event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load(); });
  container.addEventListener('click', async (event) => {
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const order = event.target.closest('[data-print-order]'); const label = event.target.closest('[data-print-label]');
    if (order) printHtml(api, `/admin/print/orders/${order.dataset.printOrder}`, toast);
    if (label) printHtml(api, `/admin/print/specimens/${label.dataset.printLabel}`, toast);
  });
  await load();
}

export async function printHtml(api, path, toast) {
  try {
    const file = await api.download(path); const url = URL.createObjectURL(file.blob); const frame = document.createElement('iframe');
    frame.style.display = 'none'; frame.src = url; frame.onload = () => { frame.contentWindow.print(); setTimeout(() => { URL.revokeObjectURL(url); frame.remove(); }, 1000); }; document.body.append(frame);
  } catch (error) { toast(error.message, 'error'); }
}
