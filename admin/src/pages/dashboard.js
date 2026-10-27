import { escapeHtml, money, pageItems } from '../utils.js';
import { pageHeader, renderState, renderTable } from '../components/ui.js';

export async function render(container, { api }) {
  container.innerHTML = `${pageHeader('数据看板', '平台送检、收入与检测项目实时汇总')}<div id="dashboard-content">${renderState('loading')}</div>`;
  const content = container.querySelector('#dashboard-content');
  try {
    const data = await api.request('/admin/dashboard');
    const hospitals = pageItems(data.hospitalRanking);
    const items = pageItems(data.itemStatistics);
    content.innerHTML = `
      <section class="metric-grid">
        <article class="metric-card accent"><span>有效送检单</span><strong>${Number(data.orderCount || 0)}</strong><small>排除草稿、驳回、取消与退款</small></article>
        <article class="metric-card"><span>平台实收金额</span><strong>${money(data.revenueCents)}</strong><small>现结实付与已结算月结账单</small></article>
        <article class="metric-card"><span>合作医院</span><strong>${hospitals.length}</strong><small>按有效送检量排名</small></article>
        <article class="metric-card"><span>检测项目</span><strong>${items.length}</strong><small>本期产生送检的项目数</small></article>
      </section>
      <div class="dashboard-grid">
        <section class="panel"><div class="panel-title"><h2>医院送检排行</h2><span>实时数据</span></div>${renderTable({ columns: [
          { label: '医院', render: (row) => `<strong>${escapeHtml(row.name)}</strong>` },
          { label: '送检单数', render: (row) => Number(row.orderCount ?? row.order_count ?? 0) },
          { label: '送检金额', render: (row) => money(row.orderAmountCents ?? row.order_amount_cents) }
        ], rows: hospitals, emptyText: '暂无医院送检数据' })}</section>
        <section class="panel"><div class="panel-title"><h2>检测项目统计</h2><span>按数量排序</span></div>${renderTable({ columns: [
          { label: '项目', key: 'itemNameSnapshot' }, { label: '送检单', key: 'orderCount' },
          { label: '数量', key: 'quantity' }, { label: '分摊金额', render: (row) => money(row.amountCents) }
        ], rows: items, emptyText: '暂无项目统计数据' })}</section>
      </div>`;
  } catch (error) {
    content.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error);
    container.querySelector('[data-action="retry"]')?.addEventListener('click', () => render(container, { api }));
  }
}
