import { formData } from './shared.js';
import { escapeHtml, formatDate, pageItems } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { canLogisticsAction } from '../permissions.js';

export async function render(container, context) {
  const { api, store } = context;
  const user = store.getState().user;
  let filters = { status: '', page: 0, size: 20 };
  const create = canLogisticsAction('create', user) ? '<button class="button" id="shipment-create">创建运单</button>' : '';
  container.innerHTML = `${pageHeader('物流配送', '运单分配、取件运输、异常与实验室签收全程留痕', create)}<section class="panel"><form id="shipment-filter" class="toolbar"><label>物流状态<select name="status"><option value="">全部</option>${['PENDING_PICKUP','PICKED_UP','IN_TRANSIT','DELIVERED','RECEIVED'].map((status) => `<option>${status}</option>`).join('')}</select></label><button class="button secondary">查询</button></form><div id="shipment-list"></div></section>`;
  const list = container.querySelector('#shipment-list');
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const data = await api.request('/logistics', { query: filters });
      list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无运单', columns: [
        { label: '运单', render: (row) => `<strong>#${row.id}</strong><small class="cell-sub">${escapeHtml(row.trackingNo || '')}</small>` },
        { label: '订单', key: 'orderId' }, { label: '实验室', key: 'laboratoryId' }, { label: '快递员', key: 'courierUserId' },
        { label: '状态', render: (row) => badge(row.status) }, { label: '预计到达', render: (row) => formatDate(row.expectedArrivedAt) }
      ], actions: (row) => actionButtons(row, user) }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#shipment-create')?.addEventListener('click', () => openCreate(context, load));
  container.querySelector('#shipment-filter').addEventListener('submit', (event) => { event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load(); });
  container.addEventListener('click', async (event) => {
    const action = event.target.closest('[data-action]');
    const page = event.target.closest('[data-page]');
    if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); return; }
    if (!action) return;
    const id = action.dataset.id;
    try {
      if (action.dataset.action === 'assign') return openAssign(context, id, load);
      if (action.dataset.action === 'exception') return openException(context, id, load);
      if (action.dataset.action === 'detail') return showDetail(context, id);
      await api.request(`/logistics/${id}/${action.dataset.action}`, { method: 'POST' });
      context.toast('物流状态已更新'); load();
    } catch (error) { context.toast(error.message, 'error'); }
  });
  await load();
}

function actionButtons(row, user) {
  const buttons = [];
  if (canLogisticsAction('assign', user) && row.status === 'PENDING_PICKUP') buttons.push(`<button class="button text" data-action="assign" data-id="${row.id}">分配</button>`);
  const transitions = { PENDING_PICKUP: ['pickup', '取件'], PICKED_UP: ['transit', '开始运输'], IN_TRANSIT: ['deliver', '送达'] };
  if (row.status === 'DELIVERED' && canLogisticsAction('receive', user)) buttons.push(`<button class="button text" data-action="receive" data-id="${row.id}">签收</button>`);
  const transition = transitions[row.status];
  if (transition && canLogisticsAction(transition[0], user)) buttons.push(`<button class="button text" data-action="${transition[0]}" data-id="${row.id}">${transition[1]}</button>`);
  if (row.status !== 'RECEIVED' && canLogisticsAction('exception', user)) buttons.push(`<button class="button text" data-action="exception" data-id="${row.id}">异常</button>`);
  buttons.push(`<button class="button text" data-action="detail" data-id="${row.id}">时间线</button>`);
  return buttons.join('');
}

function openCreate(context, done) {
  context.dialog({ title: '创建运单', submitLabel: '创建', body: `<div class="form-grid">${field({ name: 'orderId', label: '送检单 ID', type: 'number', required: true })}${field({ name: 'laboratoryId', label: '实验室 ID', type: 'number', required: true })}${field({ name: 'trackingNo', label: '运单号', required: true })}${field({ name: 'courierUserId', label: '快递员用户 ID', type: 'number' })}${field({ name: 'pickupAddress', label: '揽收地址', required: true, wide: true })}${field({ name: 'expectedArrivedAt', label: '预计到达时间', placeholder: '2026-10-28T10:00:00', wide: true })}</div>`, async onSubmit(form) {
    const value = formData(form); await context.api.request('/logistics', { method: 'POST', data: { orderId: Number(value.orderId), laboratoryId: Number(value.laboratoryId), courierUserId: value.courierUserId ? Number(value.courierUserId) : null, trackingNo: value.trackingNo.trim(), pickupAddress: value.pickupAddress.trim(), expectedArrivedAt: value.expectedArrivedAt || null } }); context.toast('运单已创建'); done();
  } });
}
function openAssign(context, id, done) {
  context.dialog({ title: `分配运单 #${id}`, submitLabel: '分配', body: field({ name: 'courierUserId', label: '快递员用户 ID', type: 'number', required: true }), async onSubmit(form) {
    await context.api.request(`/logistics/${id}/assign`, { method: 'POST', data: { courierUserId: Number(formData(form).courierUserId) } }); context.toast('运单已分配'); done();
  } });
}
function openException(context, id, done) {
  context.dialog({ title: `登记运单 #${id} 异常`, submitLabel: '保存异常', body: `<div class="form-grid">${field({ name: 'exceptionType', label: '异常类型', required: true })}${field({ name: 'note', label: '异常说明', type: 'textarea', required: true, wide: true })}</div>`, async onSubmit(form) {
    const value = formData(form); await context.api.request(`/logistics/${id}/exception`, { method: 'POST', data: { exceptionType: value.exceptionType.trim(), note: value.note.trim() } }); context.toast('异常已记录'); done();
  } });
}
async function showDetail(context, id) {
  try {
    const row = await context.api.request(`/logistics/${id}`);
    context.dialog({ title: `运单 #${id} 时间线`, wide: true, body: `${renderDetail([{ label: '当前状态', value: row.status }, { label: '异常类型', value: row.exceptionType }, { label: '异常说明', value: row.exceptionNote }])}${renderTable({ rows: row.timeline || [], columns: [{ label: '节点', key: 'action' }, { label: '说明', key: 'reason' }, { label: '操作时间', render: (item) => formatDate(item.createdAt) }] })}` });
  } catch (error) { context.toast(error.message, 'error'); }
}
