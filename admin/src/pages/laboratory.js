import { resultPayload } from '../adapters.js';
import { formData } from './shared.js';
import { escapeHtml, formatDate, pageItems } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { canLabAction } from '../permissions.js';

export async function render(container, context) {
  const { api, store } = context;
  const user = store.getState().user;
  let filters = { status: '', page: 0, size: 20 };
  const assign = canLabAction('assign', user) ? '<button class="button" id="assign-new">分配任务</button>' : '';
  container.innerHTML = `${pageHeader('实验室任务', '分配已签收样本、按批次录入结果并处理退样复检', assign)}<section class="panel"><form id="task-filter" class="toolbar"><label>任务状态<select name="status"><option value="">全部</option>${['RECEIVED','TESTING','RESULT_ENTERED','REPORT_REVIEW','COMPLETED','RETURNED'].map((status) => `<option>${status}</option>`).join('')}</select></label><button class="button secondary">查询</button></form><div id="task-list"></div></section>`;
  const list = container.querySelector('#task-list');
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const data = await api.request('/lab/tasks', { query: filters });
      list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无实验室任务', columns: [
        { label: '任务', render: (row) => `<strong>#${row.id}</strong><small class="cell-sub">订单 #${row.orderId}</small>` },
        { label: '实验室', key: 'labId' }, { label: '检测人员', key: 'assignedTo' }, { label: '设备', key: 'equipmentName' },
        { label: '类型', key: 'taskType' }, { label: '状态', render: (row) => badge(row.status) }, { label: '开始时间', render: (row) => formatDate(row.assignedAt) }
      ], actions: (row) => `<button class="button text" data-task="${row.id}">详情/结果</button>` }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#assign-new')?.addEventListener('click', () => openAssign(context, load));
  container.querySelector('#task-filter').addEventListener('submit', (event) => { event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load(); });
  container.addEventListener('click', (event) => {
    const task = event.target.closest('[data-task]'); if (task) showTask(context, task.dataset.task, user, load);
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
  });
  await load();
}

async function showTask(context, id, user, done) {
  try {
    const task = await context.api.request(`/lab/tasks/${id}`);
    const controls = canLabAction('results', user) ? `<div class="button-row"><button class="button" id="task-results">录入新批次结果</button><button class="button secondary" id="task-return">退样</button><button class="button secondary" id="task-retest">复检</button></div>` : '';
    context.dialog({ title: `实验室任务 #${id}`, wide: true, body: `${renderDetail([{ label: '订单 ID', value: task.orderId ?? task.order_id }, { label: '实验室 ID', value: task.labId ?? task.laboratory_id }, { label: '检测人员 ID', value: task.assignedTo ?? task.assigned_to }, { label: '设备', value: task.equipmentName ?? task.equipment_code }, { label: '批次', value: task.testBatch }, { label: '状态', value: task.status }])}<h3>批次历史（不可覆盖）</h3>${renderTable({ rows: task.results || [], columns: [{ label: '结果 ID', key: 'id' }, { label: '批次', key: 'batchNo' }, { label: '状态', key: 'resultStatus' }, { label: '录入时间', render: (row) => formatDate(row.enteredAt) }] })}${controls}` });
    document.querySelector('#task-results')?.addEventListener('click', () => openResults(context, task, done));
    document.querySelector('#task-return')?.addEventListener('click', () => reasonAction(context, id, 'return', '退样', done));
    document.querySelector('#task-retest')?.addEventListener('click', () => reasonAction(context, id, 'retest', '发起复检', done));
  } catch (error) { context.toast(error.message, 'error'); }
}
function openAssign(context, done) {
  context.dialog({ title: '分配实验室任务', submitLabel: '分配并开始检测', body: `<div class="form-grid">${field({ name: 'orderId', label: '已签收送检单 ID', type: 'number', required: true })}${field({ name: 'labId', label: '实验室 ID', type: 'number', required: true })}${field({ name: 'assigneeId', label: '本实验室检测人员', options: [], required: true })}${field({ name: 'equipmentCode', label: '检测设备编码', required: true })}</div>`, async onSubmit(form) {
    const value = formData(form); await context.api.request(`/lab/tasks/order/${Number(value.orderId)}/assign`, { method: 'POST', data: { assigneeId: Number(value.assigneeId), equipmentCode: value.equipmentCode.trim() } }); context.toast('任务已分配'); done();
  } });
  const form = document.querySelector('#active-dialog-form');
  async function loadAssignees() {
    if (!form.elements.labId.value) return;
    const assignees = pageItems(await context.api.request('/lab/tasks/assignees', { query: { labId: Number(form.elements.labId.value) } }));
    form.elements.assigneeId.innerHTML = assignees.map((row) => `<option value="${row.id}">${escapeHtml(row.name || row.account || `人员 ${row.id}`)}</option>`).join('') || '<option value="">该实验室暂无检测人员</option>';
  }
  form.elements.labId.addEventListener('change', loadAssignees);
}
function openResults(context, task, done) {
  const rows = task.orderItems || [];
  if (!rows.length) { context.toast('该订单没有可录入的检测项目', 'error'); return; }
  const body = `<p class="form-hint">当前任务必须完整提交所有项目；复检会生成新任务和新批次，旧结果不可修改。</p>${rows.map((item, index) => `<fieldset class="result-row" data-result-row data-order-item="${item.id}"><legend>${escapeHtml(item.itemNameSnapshot)}</legend><div class="form-grid">${field({ name: `name-${index}`, label: '结果名称', value: item.itemNameSnapshot, required: true })}${field({ name: `value-${index}`, label: '结果值', required: true })}${field({ name: `unit-${index}`, label: '单位', required: true })}${field({ name: `referenceRange-${index}`, label: '参考范围', required: true, placeholder: '0-40 / <5 / 阴性' })}${field({ name: `abnormalFlag-${index}`, label: '异常标记', options: [{ value: '', label: '按数值范围自动判断' }, { value: 'false', label: '正常' }, { value: 'true', label: '异常' }] })}</div></fieldset>`).join('')}`;
  context.dialog({ title: `录入任务 #${task.id} 检测结果`, wide: true, submitLabel: '保存不可变批次', body, async onSubmit(form) {
    const values = [...form.querySelectorAll('[data-result-row]')].map((row, index) => ({ orderItemId: row.dataset.orderItem, name: form.elements[`name-${index}`].value, value: form.elements[`value-${index}`].value, unit: form.elements[`unit-${index}`].value, referenceRange: form.elements[`referenceRange-${index}`].value, abnormalFlag: form.elements[`abnormalFlag-${index}`].value }));
    await context.api.request(`/lab/tasks/${task.id}/results`, { method: 'POST', data: resultPayload(values) }); context.toast('检测结果批次已保存'); done();
  } });
}
function reasonAction(context, id, action, title, done) {
  context.dialog({ title, submitLabel: title, body: field({ name: 'reason', label: '原因说明', type: 'textarea', required: true, wide: true }), async onSubmit(form) {
    await context.api.request(`/lab/tasks/${id}/${action}`, { method: 'POST', data: { reason: formData(form).reason.trim() } }); context.toast(`${title}已提交`); done();
  } });
}
