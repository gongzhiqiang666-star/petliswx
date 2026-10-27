import { articlePayload, giftPayload, pointAdjustmentPayload } from '../adapters.js';
import { formData } from './shared.js';
import { formatDate, idempotencyKey, money, pageItems, saveBlob, value, cell } from '../utils.js';
import { badge, field, pageHeader, pagination, renderState, renderTable } from '../components/ui.js';
import { renderCouponAdmin } from '../components/coupons.js';

const tabs = [
  ['coupons', '优惠券'], ['points', '积分规则'], ['gifts', '积分礼品'], ['redemptions', '兑换订单'], ['articles', '资讯内容']
];

export async function render(container, context) {
  const { api } = context;
  let tab = 'coupons';
  let filters = { page: 0, size: 20, status: '' };
  container.innerHTML = `${pageHeader('运营管理', '优惠券查询、积分规则调整、礼品履约与资讯发布')}
    <div class="tabs scrollable">${tabs.map(([id, label]) => `<button class="tab ${id === tab ? 'active' : ''}" data-tab="${id}">${label}</button>`).join('')}</div><section class="panel"><div id="operation-tools"></div><div id="operation-list"></div></section>`;
  const list = container.querySelector('#operation-list');
  const tools = container.querySelector('#operation-tools');

  async function load() {
    if (tab === 'coupons') { tools.innerHTML = ''; await renderCouponAdmin(list, context); return; }
    list.innerHTML = renderState('loading');
    tools.innerHTML = ({
      points: '<button class="button" id="rule-new">新增积分规则</button><button class="button secondary" id="point-adjust">调整用户积分</button><button class="button text" id="point-export">导出积分流水</button>',
      gifts: '<button class="button" id="gift-new">新增礼品</button>',
      redemptions: '<span class="form-hint">按状态机处理发货、完成或取消；取消会真实退回积分和库存。</span>',
      articles: '<button class="button" id="article-new">发布资讯</button><button class="button secondary" id="category-new">新增分类</button>'
    })[tab];
    bindTools();
    try {
      const endpoint = ({ points: '/points/rules', gifts: '/gifts', redemptions: '/gifts/redemptions', articles: '/articles' })[tab];
      const data = await api.request(endpoint, { query: filters });
      if (tab === 'points') list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无积分规则', columns: [
        { label: '规则编码', render: (row) => cell(row, 'ruleCode') }, { label: '名称', render: (row) => cell(row, 'name') },
        { label: '积分比例', render: (row) => `${value(row, 'pointsPerCentsNumerator')} / ${value(row, 'pointsPerCentsDenominator')}（每分）` },
        { label: '状态', render: (row) => badge(value(row, 'active') ? 'ACTIVE' : 'INACTIVE') }
      ], actions: (row) => `<button class="button text" data-rule-edit="${cell(row, 'id')}" data-name="${cell(row, 'name')}" data-code="${cell(row, 'ruleCode')}">编辑</button>` }) + pagination(data);
      if (tab === 'gifts') list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无积分礼品', columns: [
        { label: '礼品', render: (row) => cell(row, 'name') }, { label: '所需积分', render: (row) => cell(row, 'pointsCost') },
        { label: '库存', render: (row) => cell(row, 'stock') }, { label: '状态', render: (row) => badge(value(row, 'status', '')) }
      ], actions: (row) => `<button class="button text" data-gift-edit="${value(row, 'id')}">编辑</button>` }) + pagination(data);
      if (tab === 'redemptions') list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无礼品兑换记录', columns: [
        { label: '兑换 ID', render: (row) => cell(row, 'id') }, { label: '礼品', render: (row) => cell(row, 'giftName') },
        { label: '用户', render: (row) => cell(row, 'userId') }, { label: '消耗积分', render: (row) => cell(row, 'pointsSpent') },
        { label: '收件人', render: (row) => cell(row, 'deliveryName') }, { label: '状态', render: (row) => badge(value(row, 'status', '')) }
      ], actions: (row) => `<button class="button text" data-redemption="${value(row, 'id')}" data-status="${value(row, 'status', '')}">处理</button>` }) + pagination(data);
      if (tab === 'articles') list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无资讯内容', columns: [
        { label: '标题', render: (row) => `<strong>${cell(row, 'title')}</strong><small class="cell-sub">${cell(row, 'slug')}</small>` },
        { label: '分类', render: (row) => cell(row, 'categoryName') }, { label: '类型', render: (row) => cell(row, 'contentType') },
        { label: '浏览', render: (row) => cell(row, 'viewCount') }, { label: '点赞', render: (row) => cell(row, 'likeCount') },
        { label: '状态', render: (row) => badge(value(row, 'status', '')) }, { label: '发布时间', render: (row) => formatDate(value(row, 'publishedAt', null)) }
      ], actions: (row) => `<button class="button text" data-article-edit="${value(row, 'id')}">编辑</button>` }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }

  function bindTools() {
    tools.querySelector('#rule-new')?.addEventListener('click', () => openRule());
    tools.querySelector('#point-adjust')?.addEventListener('click', () => context.dialog({ title: '调整用户积分', submitLabel: '确认调整', body: `<p class="form-hint">正数增加、负数扣减。每次操作生成唯一幂等键，避免重复提交。</p><div class="form-grid">${field({ name: 'userId', label: '用户 ID', type: 'number', required: true, min: 1 })}${field({ name: 'points', label: '调整积分', type: 'number', required: true })}${field({ name: 'reason', label: '调整原因', type: 'textarea', required: true, wide: true })}</div>`, async onSubmit(form) {
      const payload = pointAdjustmentPayload(formData(form), idempotencyKey()); await api.request('/points/adjust', { method: 'POST', data: payload.data, headers: { 'Idempotency-Key': payload.idempotencyKey } }); context.toast('用户积分已调整'); load();
    } }));
    tools.querySelector('#point-export')?.addEventListener('click', async () => { try { const file = await api.download('/points/export'); saveBlob(file.blob, file.filename); } catch (error) { context.toast(error.message, 'error'); } });
    tools.querySelector('#gift-new')?.addEventListener('click', () => openGift());
    tools.querySelector('#article-new')?.addEventListener('click', () => openArticle());
    tools.querySelector('#category-new')?.addEventListener('click', () => context.dialog({ title: '新增资讯分类', submitLabel: '保存分类', body: `<div class="form-grid">${field({ name: 'name', label: '分类名称', required: true })}${field({ name: 'sortOrder', label: '排序', type: 'number', value: 0 })}</div>`, async onSubmit(form) {
      const data = formData(form); await api.request('/articles/categories', { method: 'POST', data: { name: data.name.trim(), sortOrder: Number(data.sortOrder) } }); context.toast('资讯分类已保存');
    } }));
  }

  function openRule(id = '', code = 'ORDER', name = '') {
    context.dialog({ title: id ? '编辑积分规则' : '新增积分规则', submitLabel: '保存规则', body: `<div class="form-grid">${field({ name: 'ruleCode', label: '规则编码', value: code, required: true })}${field({ name: 'name', label: '规则名称', value: name, required: true })}${field({ name: 'pointsPerCentsNumerator', label: '积分分子', type: 'number', value: 1, min: 0, required: true })}${field({ name: 'pointsPerCentsDenominator', label: '金额分母（分）', type: 'number', value: 100, min: 1, required: true })}${field({ name: 'active', label: '是否启用', options: [{ value: 'true', label: '启用' }, { value: 'false', label: '停用' }] })}</div>`, async onSubmit(form) {
      const data = formData(form); await api.request(id ? `/points/rules/${id}` : '/points/rules', { method: id ? 'PUT' : 'POST', data: { ruleCode: data.ruleCode.trim(), name: data.name.trim(), pointsPerCentsNumerator: Number(data.pointsPerCentsNumerator), pointsPerCentsDenominator: Number(data.pointsPerCentsDenominator), active: data.active === 'true' } }); context.toast('积分规则已保存'); load();
    } });
  }

  function openGift(id = '', current = {}) {
    context.dialog({ title: id ? '编辑积分礼品' : '新增积分礼品', submitLabel: '保存礼品', body: `<div class="form-grid">${field({ name: 'name', label: '礼品名称', value: value(current, 'name', ''), required: true })}${field({ name: 'pointsCost', label: '所需积分', type: 'number', value: value(current, 'pointsCost', ''), min: 1, required: true })}${field({ name: 'stock', label: '库存', type: 'number', value: value(current, 'stock', ''), min: 0, required: true })}${field({ name: 'status', label: '状态', value: value(current, 'status', 'ACTIVE'), options: [{ value: 'ACTIVE', label: '上架' }, { value: 'INACTIVE', label: '下架' }] })}${field({ name: 'imageUrl', label: '图片 URL', value: value(current, 'imageUrl', ''), wide: true })}${field({ name: 'description', label: '礼品说明', value: value(current, 'description', ''), type: 'textarea', wide: true })}</div>`, async onSubmit(form) {
      await api.request(id ? `/gifts/${id}` : '/gifts', { method: id ? 'PUT' : 'POST', data: giftPayload(formData(form)) }); context.toast('礼品已保存'); load();
    } });
  }

  async function openArticle(id = '') {
    let current = {};
    try { if (id) current = await api.request(`/articles/${id}`); } catch (error) { context.toast(error.message, 'error'); return; }
    context.dialog({ title: id ? '编辑资讯' : '发布资讯', wide: true, submitLabel: '保存资讯', body: `<div class="form-grid">${field({ name: 'title', label: '标题', value: value(current, 'title', ''), required: true })}${field({ name: 'slug', label: '唯一 Slug', value: value(current, 'slug', ''), required: true })}${field({ name: 'categoryId', label: '分类 ID', type: 'number', value: value(current, 'categoryId', '') })}${field({ name: 'contentType', label: '内容类型', value: value(current, 'contentType', 'RICH_TEXT'), options: [{ value: 'RICH_TEXT', label: '图文' }, { value: 'ANNOUNCEMENT', label: '公告' }] })}${field({ name: 'status', label: '状态', value: value(current, 'status', 'DRAFT'), options: [{ value: 'DRAFT', label: '草稿' }, { value: 'PUBLISHED', label: '发布' }, { value: 'ARCHIVED', label: '归档' }] })}${field({ name: 'coverUrl', label: '封面 URL', value: value(current, 'coverUrl', '') })}${field({ name: 'liveStartAt', label: '上线时间', type: 'datetime-local', value: value(current, 'liveStartAt', '') })}${field({ name: 'liveEndAt', label: '下线时间', type: 'datetime-local', value: value(current, 'liveEndAt', '') })}${field({ name: 'summary', label: '摘要', value: value(current, 'summary', ''), type: 'textarea', wide: true })}${field({ name: 'content', label: '正文', value: value(current, 'content', ''), type: 'textarea', required: true, wide: true })}${field({ name: 'popupEnabled', label: '弹窗展示', value: String(Boolean(value(current, 'popupEnabled', false))), options: [{ value: 'false', label: '否' }, { value: 'true', label: '是' }] })}</div>`, async onSubmit(form) {
      const data = formData(form); await api.request(id ? `/articles/${id}` : '/articles', { method: id ? 'PUT' : 'POST', data: articlePayload({ ...data, popupEnabled: data.popupEnabled === 'true' }) }); context.toast('资讯内容已保存'); load();
    } });
  }

  container.addEventListener('click', async (event) => {
    const tabButton = event.target.closest('[data-tab]'); if (tabButton) { tab = tabButton.dataset.tab; filters = { page: 0, size: 20, status: '' }; container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton)); load(); }
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const rule = event.target.closest('[data-rule-edit]'); if (rule) openRule(rule.dataset.ruleEdit, rule.dataset.code, rule.dataset.name);
    const gift = event.target.closest('[data-gift-edit]'); if (gift) { try { openGift(gift.dataset.giftEdit, await api.request(`/gifts/${gift.dataset.giftEdit}`)); } catch (error) { context.toast(error.message, 'error'); } }
    const article = event.target.closest('[data-article-edit]'); if (article) openArticle(article.dataset.articleEdit);
    const redemption = event.target.closest('[data-redemption]'); if (redemption) context.dialog({ title: '处理礼品兑换', submitLabel: '确认状态变更', body: field({ name: 'status', label: '目标状态', options: redemption.dataset.status === 'SHIPPED' ? [{ value: 'COMPLETED', label: '完成' }] : [{ value: 'SHIPPED', label: '发货' }, { value: 'CANCELLED', label: '取消并退积分' }] }), async onSubmit(form) {
      const target = formData(form).status; if (target === 'CANCELLED' && !globalThis.confirm('确认取消兑换？系统将退回积分和库存。')) return;
      await api.request(`/gifts/redemptions/${redemption.dataset.redemption}/process`, { method: 'POST', data: { status: target } }); context.toast('兑换状态已更新'); load();
    } });
  });
  await load();
}
