import { formData } from './shared.js';
import { formatDate, idempotencyKey, pageItems, value, cell } from '../utils.js';
import { badge, field, pageHeader, pagination, renderState, renderTable } from '../components/ui.js';
import { renderCouponAdmin } from '../components/coupons.js';

const tabs = [['coupons', '优惠券'], ['points', '积分规则'], ['gifts', '积分礼品'], ['redemptions', '兑换订单'], ['articles', '资讯内容']];

export async function render(container, context) {
  const { api } = context;
  let tab = 'coupons';
  let filters = { page: 0, size: 20 };
  container.innerHTML = `${pageHeader('运营管理', '优惠券库存、积分规则和调整、礼品取消补偿与资讯发布')}
    <div class="tabs scrollable">${tabs.map(([id, label]) => `<button class="tab ${id === tab ? 'active' : ''}" data-tab="${id}">${label}</button>`).join('')}</div><section class="panel"><div id="operation-tools"></div><div id="operation-list"></div></section>`;
  const list = container.querySelector('#operation-list');
  const tools = container.querySelector('#operation-tools');
  async function load() {
    if (tab === 'coupons') { tools.innerHTML = ''; await renderCouponAdmin(list, context); return; }
    list.innerHTML = renderState('loading');
    tools.innerHTML = ({ points: '<button class="button" id="rule-new">新增积分规则</button><button class="button secondary" id="point-adjust">调整用户积分</button>', gifts: '<button class="button" id="gift-new">新增礼品</button>', redemptions: '<span class="form-hint">仅待处理兑换可取消，服务端原子退回积分和库存。</span>', articles: '<button class="button" id="article-new">发布资讯</button>' })[tab];
    bindTools();
    try {
      const endpoint = ({ points: '/admin/operations/configs', gifts: '/mall/gifts', redemptions: '/mall/redemptions', articles: '/articles' })[tab];
      const data = await api.request(endpoint, { query: { ...filters, ...(tab === 'gifts' || tab === 'redemptions' ? { admin: true } : {}) } });
      if (tab === 'points') list.innerHTML = renderTable({ rows: pageItems(data).filter((row) => value(row, 'configKey', '').startsWith('points')), emptyText: '暂无积分规则', columns: [
        { label: '规则键', render: (row) => cell(row, 'configKey') }, { label: '规则 JSON', render: (row) => cell(row, 'configValue') }, { label: '版本', render: (row) => cell(row, 'version') }, { label: '更新时间', render: (row) => formatDate(value(row, 'updatedAt', null)) }
      ], actions: (row) => `<button class="button text" data-rule-edit="${value(row, 'configKey')}" data-version="${row.version}" data-value="${cell(row, 'configValue')}">编辑</button>` }) + pagination(data);
      if (tab === 'gifts') list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无积分礼品', columns: [
        { label: '礼品', render: (row) => cell(row, 'name') }, { label: '所需积分', render: (row) => cell(row, 'pointsCost') }, { label: '库存', render: (row) => cell(row, 'stock') }, { label: '状态', render: (row) => badge(Number(row.active) === 1 ? 'ACTIVE' : 'INACTIVE') }
      ] }) + pagination(data);
      if (tab === 'redemptions') list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无礼品兑换记录', columns: [
        { label: '兑换 ID', render: (row) => cell(row, 'id') }, { label: '礼品', render: (row) => cell(row, 'giftName') }, { label: '用户', render: (row) => cell(row, 'userId') },
        { label: '消耗积分', render: (row) => cell(row, 'pointsSpent') }, { label: '状态', render: (row) => badge(value(row, 'status', '')) }
      ], actions: (row) => row.status === 'pending' ? `<button class="button danger text" data-redemption-cancel="${row.id}">取消并退积分</button>` : '' }) + pagination(data);
      if (tab === 'articles') list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无资讯内容', columns: [
        { label: '标题', render: (row) => cell(row, 'title') }, { label: '分类', render: (row) => cell(row, 'category') }, { label: '类型', render: (row) => cell(row, 'type') }, { label: '状态', render: (row) => badge(value(row, 'status', 'published')) }, { label: '发布时间', render: (row) => formatDate(value(row, 'publishedAt', null)) }
      ] }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  function bindTools() {
    tools.querySelector('#rule-new')?.addEventListener('click', () => openRule());
    tools.querySelector('#point-adjust')?.addEventListener('click', () => context.dialog({ title: '调整用户积分', submitLabel: '确认调整', body: `<div class="form-grid">${field({ name: 'userId', label: '用户 ID', type: 'number', required: true, min: 1 })}${field({ name: 'delta', label: '调整积分', type: 'number', required: true })}${field({ name: 'reason', label: '调整原因', type: 'textarea', required: true, wide: true })}</div>`, async onSubmit(form) {
      const data = formData(form); await api.request('/admin/operations/points-adjustments', { method: 'POST', data: { userId: Number(data.userId), delta: Number(data.delta), reason: data.reason.trim(), requestKey: idempotencyKey() } }); context.toast('用户积分已调整'); load();
    } }));
    tools.querySelector('#gift-new')?.addEventListener('click', () => context.dialog({ title: '新增积分礼品', submitLabel: '保存礼品', body: `<div class="form-grid">${field({ name: 'name', label: '礼品名称', required: true })}${field({ name: 'pointsCost', label: '所需积分', type: 'number', min: 1, required: true })}${field({ name: 'stock', label: '库存', type: 'number', min: 0, required: true })}${field({ name: 'imageUrl', label: '图片 URL', wide: true })}${field({ name: 'description', label: '礼品说明', type: 'textarea', wide: true })}</div>`, async onSubmit(form) {
      const data = formData(form); await api.request('/mall/gifts', { method: 'POST', data: { ...data, pointsCost: Number(data.pointsCost), stock: Number(data.stock) } }); context.toast('礼品已保存'); load();
    } }));
    tools.querySelector('#article-new')?.addEventListener('click', () => context.dialog({ title: '发布资讯', wide: true, submitLabel: '发布资讯', body: `<div class="form-grid">${field({ name: 'category', label: '分类', required: true })}${field({ name: 'type', label: '类型', options: [{ value: 'article', label: '图文' }, { value: 'live', label: '直播' }, { value: 'activity', label: '活动' }] })}${field({ name: 'title', label: '标题', required: true })}${field({ name: 'coverUrl', label: '封面 URL' })}${field({ name: 'liveAt', label: '直播时间', type: 'datetime-local' })}${field({ name: 'liveUrl', label: '直播 HTTPS 地址' })}${field({ name: 'summary', label: '摘要', type: 'textarea', wide: true })}${field({ name: 'content', label: '正文', type: 'textarea', required: true, wide: true })}${field({ name: 'popupEnabled', label: '弹窗展示', options: [{ value: 'false', label: '否' }, { value: 'true', label: '是' }] })}</div>`, async onSubmit(form) {
      const data = formData(form); await api.request('/articles', { method: 'POST', data: { ...data, popupEnabled: data.popupEnabled === 'true', liveAt: data.liveAt || null, liveUrl: data.liveUrl || null } }); context.toast('资讯已发布并生成一次通知'); load();
    } }));
  }
  function openRule(key = '', current = '{}', version = 0) {
    context.dialog({ title: key ? '编辑积分规则' : '新增积分规则', submitLabel: '保存规则', body: `<div class="form-grid">${field({ name: 'key', label: '规则键', value: key || 'points-rule', required: true })}${field({ name: 'value', label: '规则 JSON', value: current, type: 'textarea', required: true, wide: true })}${field({ name: 'reason', label: '修改原因', type: 'textarea', wide: true })}</div>`, async onSubmit(form) {
      const data = formData(form); const parsed = JSON.parse(data.value); if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw new Error('规则必须是 JSON 对象');
      await api.request(key ? `/admin/operations/configs/${encodeURIComponent(key)}` : '/admin/operations/configs', { method: key ? 'PUT' : 'POST', data: key ? { value: parsed, version: Number(version), reason: data.reason } : { key: data.key, value: parsed, description: data.reason } }); context.toast('积分规则已保存'); load();
    } });
  }
  container.addEventListener('click', async (event) => {
    const tabButton = event.target.closest('[data-tab]'); if (tabButton) { tab = tabButton.dataset.tab; filters = { page: 0, size: 20 }; container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton)); load(); }
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const rule = event.target.closest('[data-rule-edit]'); if (rule) openRule(rule.dataset.ruleEdit, rule.dataset.value, rule.dataset.version);
    const cancel = event.target.closest('[data-redemption-cancel]'); if (cancel) context.dialog({ title: '取消礼品兑换', submitLabel: '确认取消并补偿', body: field({ name: 'reason', label: '取消原因', type: 'textarea', required: true, wide: true }), async onSubmit(form) {
      await api.request(`/mall/redemptions/${cancel.dataset.redemptionCancel}/cancel`, { method: 'POST', data: { reason: formData(form).reason.trim() } }); context.toast('兑换已取消，积分与库存已退回'); load();
    } });
  });
  await load();
}
