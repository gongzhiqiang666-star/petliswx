import { formData } from '../pages/shared.js';
import { cell, formatDate, idempotencyKey, moneyYuan, pageItems, value } from '../utils.js';
import { directoryOptions } from '../options.js';
import { badge, field, pagination, renderState, renderTable } from './ui.js';

export async function renderCouponAdmin(container, context) {
  const { api } = context;
  let filters = { page: 0, size: 20 };
  container.innerHTML = '<div id="coupon-tools" class="toolbar"><button class="button" id="coupon-template-new">新建券模板</button><button class="button secondary" id="coupon-issue">发放优惠券</button></div><div id="coupon-list"></div>';
  const list = container.querySelector('#coupon-list');
  container.querySelector('#coupon-template-new').addEventListener('click', () => openTemplate());
  container.querySelector('#coupon-issue').addEventListener('click', openIssue);
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const data = await api.request('/admin/coupons/templates', { query: filters });
      list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无优惠券模板', columns: [
        { label: '模板', render: (row) => `<strong>${cell(row, 'name')}</strong><small class="cell-sub">ID ${cell(row, 'id')}</small>` },
        { label: '优惠（元）', render: (row) => moneyYuan(row.amount) }, { label: '门槛（元）', render: (row) => moneyYuan(value(row, 'minimumAmount', 0)) },
        { label: '有效期', render: (row) => `${formatDate(value(row, 'startsAt'))} ~ ${formatDate(value(row, 'expiresAt'))}` },
        { label: '状态', render: (row) => badge(Number(row.active) === 1 ? 'ACTIVE' : 'INACTIVE') }
      ], actions: (row) => `<button class="button text" data-template-edit="${row.id}" data-name="${cell(row, 'name')}" data-amount="${row.amount}" data-minimum="${value(row, 'minimumAmount', 0)}" data-starts="${value(row, 'startsAt')}" data-expires="${value(row, 'expiresAt')}">编辑</button>${Number(row.active) === 1 ? `<button class="button danger text" data-template-disable="${row.id}">停用</button>` : ''}` }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  function openTemplate(id = '', current = {}) {
    context.dialog({ title: id ? '编辑优惠券模板' : '新建优惠券模板', wide: true, submitLabel: '保存模板', body: `<div class="form-grid">${field({ name: 'name', label: '模板名称', value: current.name || '', required: true })}${field({ name: 'amount', label: '优惠金额（元）', type: 'number', value: current.amount || '', min: 0.01, step: '0.01', required: true })}${field({ name: 'minimumAmount', label: '使用门槛（元）', type: 'number', value: current.minimumAmount || 0, min: 0, step: '0.01', required: true })}${field({ name: 'totalQuantity', label: '发行总量', type: 'number', value: 1, min: 1, required: true })}${field({ name: 'startsAt', label: '生效时间', type: 'datetime-local', value: current.startsAt || '', required: true })}${field({ name: 'expiresAt', label: '失效时间', type: 'datetime-local', value: current.expiresAt || '', required: true })}</div>`, async onSubmit(form) {
      const data = formData(form); const payload = { name: data.name.trim(), amount: Number(data.amount), minimumAmount: Number(data.minimumAmount), laboratoryId: null, startsAt: data.startsAt, expiresAt: data.expiresAt };
      if (id) await api.request(`/admin/coupons/${id}`, { method: 'PUT', query: { totalQuantity: Number(data.totalQuantity), reason: '后台编辑券模板' }, data: payload });
      else { const created = await api.request('/admin/coupons', { method: 'POST', data: payload }); await api.request(`/admin/coupons/${created}`, { method: 'PUT', query: { totalQuantity: Number(data.totalQuantity), reason: '初始化券库存' }, data: payload }); }
      context.toast('优惠券模板与库存已保存'); load();
    } });
  }
  async function openIssue() {
    try {
      const [templates, users] = await Promise.all([api.request('/admin/coupons/templates', { query: { page: 0, size: 100 } }), api.request('/admin/users', { query: { status: 'ACTIVE' } })]);
      const active = pageItems(templates).filter((row) => Number(row.active) === 1);
      context.dialog({ title: '发放优惠券', submitLabel: '确认发放', body: `<div class="form-grid">${field({ name: 'templateId', label: '券模板', options: active.map((row) => ({ value: row.id, label: row.name })) })}${field({ name: 'userId', label: '接收用户', options: directoryOptions(pageItems(users)) })}</div>`, async onSubmit(form) {
        const data = formData(form); await api.request(`/admin/coupons/${data.templateId}/users/${data.userId}`, { method: 'POST', data: { requestKey: idempotencyKey() } }); context.toast('优惠券已发放并扣减库存');
      } });
    } catch (error) { context.toast(error.message, 'error'); }
  }
  container.onclick = async (event) => {
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const edit = event.target.closest('[data-template-edit]'); if (edit) openTemplate(edit.dataset.templateEdit, edit.dataset);
    const disable = event.target.closest('[data-template-disable]'); if (disable && globalThis.confirm('确认停用该券模板？')) {
      try { await api.request(`/admin/coupons/${disable.dataset.templateDisable}`, { method: 'DELETE' }); context.toast('券模板已停用'); load(); } catch (error) { context.toast(error.message, 'error'); }
    }
  };
  await load();
}
