import { couponTemplatePayload } from '../adapters.js';
import { formData } from '../pages/shared.js';
import { centsToYuan, cell, formatDate, money, pageItems } from '../utils.js';
import { directoryOptions } from '../options.js';
import { badge, field, pagination, renderState, renderTable } from './ui.js';

export async function renderCouponAdmin(container, context) {
  const { api } = context;
  let mode = 'templates';
  let filters = { page: 0, size: 20, status: '', userId: '' };
  container.innerHTML = `<div class="tabs"><button class="tab active" data-coupon-mode="templates">券模板</button><button class="tab" data-coupon-mode="users">用户券管理</button></div><div id="coupon-tools" class="toolbar"></div><div id="coupon-list"></div>`;
  const tools = container.querySelector('#coupon-tools');
  const list = container.querySelector('#coupon-list');

  async function load() {
    list.innerHTML = renderState('loading');
    tools.innerHTML = mode === 'templates'
      ? '<button class="button" id="coupon-template-new">新建券模板</button>'
      : '<button class="button" id="coupon-issue">发放优惠券</button><button class="button secondary" id="coupon-validate">校验订单券</button>';
    tools.querySelector('#coupon-template-new')?.addEventListener('click', () => openTemplate());
    tools.querySelector('#coupon-issue')?.addEventListener('click', openIssue);
    tools.querySelector('#coupon-validate')?.addEventListener('click', openValidate);
    try {
      const data = await api.request(mode === 'templates' ? '/admin/coupons/templates' : '/admin/coupons', { query: filters });
      if (mode === 'templates') {
        list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无优惠券模板', columns: [
          { label: '模板', render: (row) => `<strong>${cell(row, 'name')}</strong><small class="cell-sub">${cell(row, 'code')} · ID ${cell(row, 'id')}</small>` },
          { label: '优惠', render: (row) => money(row.valueCents) }, { label: '门槛', render: (row) => money(row.minSpendCents) },
          { label: '已发 / 总量', render: (row) => `${row.issuedQuantity} / ${row.totalQuantity}` },
          { label: '有效期', render: (row) => `${formatDate(row.validFrom)} ~ ${formatDate(row.validTo)}` },
          { label: '状态', render: (row) => badge(row.status) }
        ], actions: (row) => `<button class="button text" data-template-edit="${row.id}">编辑</button>${row.status === 'ACTIVE' ? `<button class="button danger text" data-template-disable="${row.id}">停用</button>` : ''}` }) + pagination(data);
      } else {
        list.innerHTML = `${userCouponFilter()}${renderTable({ rows: pageItems(data), emptyText: '暂无用户优惠券', columns: [
          { label: '券码', render: (row) => `<strong>${cell(row, 'code')}</strong><small class="cell-sub">${cell(row, 'name')}</small>` },
          { label: '持有用户 ID', key: 'userId' }, { label: '优惠', render: (row) => money(row.valueCents) },
          { label: '使用订单', key: 'orderId' }, { label: '到期时间', render: (row) => formatDate(row.expiresAt) },
          { label: '状态', render: (row) => badge(row.status) }
        ], actions: (row) => ['AVAILABLE', 'CANCELLED'].includes(row.status) ? `<button class="button text" data-user-coupon="${row.id}" data-status="${row.status}">${row.status === 'AVAILABLE' ? '作废' : '恢复'}</button>` : '' })}${pagination(data)}`;
        list.querySelector('#user-coupon-filter')?.addEventListener('submit', (event) => {
          event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load();
        });
      }
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }

  function userCouponFilter() {
    return `<form id="user-coupon-filter" class="toolbar compact">${field({ name: 'userId', label: '持有用户 ID', type: 'number', value: filters.userId, min: 1 })}${field({ name: 'status', label: '状态', value: filters.status, options: [{ value: '', label: '全部' }, { value: 'AVAILABLE', label: '可使用' }, { value: 'USED', label: '已使用' }, { value: 'CANCELLED', label: '已作废' }, { value: 'EXPIRED', label: '已过期' }] })}<button class="button secondary">筛选</button></form>`;
  }

  async function openTemplate(id = '') {
    let current = { couponType: 'FIXED', status: 'ACTIVE', value: '0.00', minSpend: '0.00', totalQuantity: 1 };
    if (id) {
      try {
        const row = await api.request(`/admin/coupons/templates/${id}`);
        current = { ...row, value: centsToYuan(row.valueCents), minSpend: centsToYuan(row.minSpendCents), validFrom: row.validFrom?.slice(0, 16), validTo: row.validTo?.slice(0, 16) };
      } catch (error) { context.toast(error.message, 'error'); return; }
    }
    context.dialog({ title: id ? '编辑优惠券模板' : '新建优惠券模板', wide: true, submitLabel: '保存模板', body: `<div class="form-grid">${field({ name: 'code', label: '模板编码', value: current.code || '', required: true })}${field({ name: 'name', label: '模板名称', value: current.name || '', required: true })}${field({ name: 'couponType', label: '类型', value: current.couponType, options: [{ value: 'FIXED', label: '固定金额' }] })}${field({ name: 'status', label: '状态', value: current.status, options: [{ value: 'ACTIVE', label: '启用' }, { value: 'INACTIVE', label: '停用' }] })}${field({ name: 'value', label: '优惠金额（元）', type: 'number', value: current.value, min: 0, step: '0.01', required: true })}${field({ name: 'minSpend', label: '使用门槛（元）', type: 'number', value: current.minSpend, min: 0, step: '0.01', required: true })}${field({ name: 'totalQuantity', label: '发行总量', type: 'number', value: current.totalQuantity, min: 1, required: true })}${field({ name: 'validFrom', label: '生效时间', type: 'datetime-local', value: current.validFrom || '', required: true })}${field({ name: 'validTo', label: '失效时间', type: 'datetime-local', value: current.validTo || '', required: true })}</div>`, async onSubmit(form) {
      await api.request(id ? `/admin/coupons/templates/${id}` : '/admin/coupons/templates', { method: id ? 'PUT' : 'POST', data: couponTemplatePayload(formData(form)) });
      context.toast('优惠券模板已保存'); load();
    } });
  }

  async function openIssue() {
    try {
      const [templates, users] = await Promise.all([
        api.request('/admin/coupons/templates', { query: { page: 0, size: 100, status: 'ACTIVE' } }),
        api.request('/admin/users', { query: { status: 'ACTIVE' } })
      ]);
      const active = pageItems(templates).filter((row) => row.status === 'ACTIVE' && row.issuedQuantity < row.totalQuantity);
      if (!active.length) throw new Error('暂无可发放且未售罄的券模板');
      context.dialog({ title: '发放优惠券', submitLabel: '确认发放', body: `<div class="form-grid">${field({ name: 'templateId', label: '券模板', options: active.map((row) => ({ value: row.id, label: `${row.name}（ID ${row.id} · 剩余 ${row.totalQuantity - row.issuedQuantity}）` })) })}${field({ name: 'userId', label: '接收用户', options: directoryOptions(pageItems(users)) })}</div>`, async onSubmit(form) {
        const data = formData(form); await api.request('/admin/coupons/issue', { method: 'POST', data: { templateId: Number(data.templateId), userId: Number(data.userId) } });
        context.toast('优惠券已真实发放'); mode = 'users'; load();
      } });
    } catch (error) { context.toast(error.message, 'error'); }
  }

  function openValidate() {
    context.dialog({ title: '校验订单优惠券', submitLabel: '校验', body: `<p class="form-hint">服务端按订单医生校验持券归属，管理员代验不会改成管理员持券。</p><div class="form-grid">${field({ name: 'orderId', label: '送检单 ID', type: 'number', required: true, min: 1 })}${field({ name: 'couponCode', label: '用户券码', required: true })}</div>`, async onSubmit(form) {
      const data = formData(form); const result = await api.request('/coupons/validate', { method: 'POST', data: { orderId: Number(data.orderId), couponCode: data.couponCode.trim() } });
      context.toast(`校验通过，可优惠 ${money(result.discountCents)}`);
    } });
  }

  container.onclick = async (event) => {
    const modeButton = event.target.closest('[data-coupon-mode]');
    if (modeButton) { mode = modeButton.dataset.couponMode; filters = { page: 0, size: 20, status: '', userId: '' }; container.querySelectorAll('[data-coupon-mode]').forEach((button) => button.classList.toggle('active', button === modeButton)); load(); }
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const edit = event.target.closest('[data-template-edit]'); if (edit) openTemplate(edit.dataset.templateEdit);
    const disable = event.target.closest('[data-template-disable]');
    if (disable && globalThis.confirm('确认停用该券模板？已发放用户券也将不可使用。')) {
      try { await api.request(`/admin/coupons/templates/${disable.dataset.templateDisable}`, { method: 'DELETE' }); context.toast('券模板已停用'); load(); } catch (error) { context.toast(error.message, 'error'); }
    }
    const userCoupon = event.target.closest('[data-user-coupon]');
    if (userCoupon) {
      const status = userCoupon.dataset.status === 'AVAILABLE' ? 'CANCELLED' : 'AVAILABLE';
      if (status === 'CANCELLED' && !globalThis.confirm('确认作废该用户券？')) return;
      try { await api.request(`/admin/coupons/${userCoupon.dataset.userCoupon}/status`, { method: 'PATCH', data: { status } }); context.toast('用户券状态已更新'); load(); } catch (error) { context.toast(error.message, 'error'); }
    }
  };
  await load();
}
