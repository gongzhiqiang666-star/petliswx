import { formData } from './shared.js';
import { cell, moneyYuan, pageItems, saveBlob } from '../utils.js';
import { field, pageHeader, renderState, renderTable } from '../components/ui.js';

export async function render(container, { api, toast }) {
  const today = new Date();
  const first = new Date(today.getFullYear(), today.getMonth(), 1);
  const tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);
  const iso = (date) => date.toISOString().slice(0, 10);
  let filters = { from: iso(first), to: iso(tomorrow) };
  container.innerHTML = `${pageHeader('数据看板', '真实订单、现结收入、已结算月结与项目聚合')}
    <section class="panel"><form id="dashboard-filter" class="toolbar">${field({ name: 'from', label: '开始日期', type: 'date', value: filters.from, required: true })}${field({ name: 'to', label: '结束日期', type: 'date', value: filters.to, required: true })}<button class="button secondary">查询</button><button class="button text" id="orders-export" type="button">导出订单 CSV</button></form><div id="dashboard-content">${renderState('loading')}</div></section>`;
  const content = container.querySelector('#dashboard-content');
  async function load() {
    content.innerHTML = renderState('loading');
    try {
      const data = await api.request('/admin/dashboard', { query: filters });
      const hospitals = pageItems(data.hospitalRanking);
      const projects = pageItems(data.projectRanking);
      content.innerHTML = `<section class="metric-grid">
        <article class="metric-card accent"><span>送检单</span><strong>${Number(data.orderCount || 0)}</strong></article>
        <article class="metric-card"><span>实际收入</span><strong>${moneyYuan(data.revenue)}</strong><small>现结 ${moneyYuan(data.cashRevenue)} · 月结 ${moneyYuan(data.monthlyRevenue)}</small></article>
        <article class="metric-card"><span>未结欠款</span><strong>${moneyYuan(data.arrears)}</strong></article>
        <article class="metric-card"><span>检测项目</span><strong>${projects.length}</strong></article></section>
        <div class="dashboard-grid"><section class="panel"><div class="panel-title"><h2>医院排行</h2></div>${renderTable({ columns: [
          { label: '医院', render: (row) => cell(row, 'hospitalName') }, { label: '送检单', render: (row) => cell(row, 'orders') }, { label: '收入（元）', render: (row) => moneyYuan(row.revenue) }
        ], rows: hospitals, emptyText: '暂无医院数据' })}</section><section class="panel"><div class="panel-title"><h2>项目统计</h2></div>${renderTable({ columns: [
          { label: '项目', render: (row) => cell(row, 'testName') }, { label: '数量', render: (row) => cell(row, 'quantity') }
        ], rows: projects, emptyText: '暂无项目数据' })}</section></div>`;
    } catch (error) { content.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#dashboard-filter').addEventListener('submit', (event) => { event.preventDefault(); filters = formData(event.currentTarget); load(); });
  container.querySelector('#orders-export').addEventListener('click', async () => {
    try { const query = new URLSearchParams(filters).toString(); const file = await api.download(`/admin/export/orders.csv?${query}`); saveBlob(file.blob, file.filename); }
    catch (error) { toast(error.message, 'error'); }
  });
  await load();
}
