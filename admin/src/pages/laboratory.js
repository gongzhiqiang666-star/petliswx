import { resultPayload } from '../adapters.js';
import { formData } from './shared.js';
import { escapeHtml, formatDate, pageItems } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { directoryOptions } from '../options.js';

export async function render(container, context) {
  const { api } = context;
  let filters = { status: '', page: 0, size: 20 };
  container.innerHTML = `${pageHeader('实验室任务', '分配已签收样本、录入结果并处理退样复检', '<button class="button" id="assign-new">分配任务</button>')}
    <section class="panel"><form id="task-filter" class="toolbar"><label>任务状态<select name="status"><option value="">全部</option><option value="TESTING">检测中</option><option value="REPORT_REVIEW">报告审核中</option><option value="COMPLETED">已完成</option><option value="RETURNED">已退样</option></select></label><button class="button secondary">查询</button></form><div id="task-list"></div></section>`;
  const list = container.querySelector('#task-list');
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const data = await api.request('/lab/tasks', { query: filters });
      list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无实验室任务', columns: [
        { label: '任务', render: (row) => `<strong>#${row.id}</strong><small class="cell-sub">订单 #${row.orderId}</small>` },
        { label: '实验室', key: 'labId' }, { label: '检测人员', key: 'assignedTo' }, { label: '设备', key: 'equipmentName' },
        { label: '批次', key: 'testBatch' }, { label: '状态', render: (row) => badge(row.status) },
        { label: '分配时间', render: (row) => formatDate(row.assignedAt) }
      ], actions: (row) => `<button class="button text" data-results="${row.id}" data-order="${row.orderId}">录入结果</button><button class="button text" data-task="${row.id}">详情</button>` }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  async function showTask(id) {
    try {
      const task = await api.request(`/lab/tasks/${id}`);
      context.dialog({ title: `实验室任务 #${id}`, wide: true, body: `${renderDetail([
        { label: '订单 ID', value: task.orderId }, { label: '实验室 ID', value: task.labId },
        { label: '检测人员 ID', value: task.assignedTo }, { label: '设备', value: task.equipmentName },
        { label: '批次', value: task.testBatch }, { label: '状态', value: task.status },
        { label: '订单状态', value: task.orderStatus }, { label: '完成时间', value: formatDate(task.completedAt) }
      ])}<h3>当前批次结果</h3>${renderTable({ rows: task.results || [], columns: [{ label: '项目结果', key: 'name' }, { label: '结果值', key: 'value' }, { label: '单位', key: 'unit' }, { label: '参考范围', key: 'referenceRange' }, { label: '异常', render: (row) => row.abnormalFlag ? '<span class="text-danger">异常</span>' : '正常' }] })}<div class="button-row"><button class="button secondary" id="task-return">退样</button><button class="button secondary" id="task-retest">复检</button></div>` });
      document.querySelector('#task-return')?.addEventListener('click', () => reasonAction(context, id, 'return', '退样', load));
      document.querySelector('#task-retest')?.addEventListener('click', () => reasonAction(context, id, 'retest', '发起复检', load));
    } catch (error) { context.toast(error.message, 'error'); }
  }
  container.querySelector('#assign-new').addEventListener('click', () => openAssign(context, load));
  container.querySelector('#task-filter').addEventListener('submit', (event) => { event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load(); });
  container.addEventListener('click', (event) => {
    const task = event.target.closest('[data-task]'); if (task) showTask(task.dataset.task);
    const result = event.target.closest('[data-results]'); if (result) openResults(context, result.dataset.results, result.dataset.order, load);
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
  });
  await load();
}

async function openAssign(context, done) {
  try {
    const data = await context.api.request('/orders', { query: { status: 'RECEIVED', page: 0, size: 100 } });
    const orders = pageItems(data);
    if (!orders.length) { context.toast('暂无可分配的已签收送检单', 'error'); return; }
    const orderOptions = orders.map((order) => ({ value: order.id, label: `${order.orderNo}（ID ${order.id} · 实验室 ${order.labId}）` }));
    context.dialog({ title: '分配实验室任务', submitLabel: '分配并开始检测', body: `<p class="form-hint">选择已签收送检单后，检测人员列表仅显示该实验室在岗人员。</p><div class="form-grid">${field({ name: 'orderId', label: '已签收送检单', options: orderOptions, required: true })}${field({ name: 'assigneeId', label: '检测人员', options: [], required: true })}${field({ name: 'equipmentName', label: '检测设备', required: true, wide: true })}</div>`, async onSubmit(form) {
      const values = formData(form); await context.api.request(`/lab/tasks/order/${values.orderId}/assign`, { method: 'POST', data: { assigneeId: Number(values.assigneeId), equipmentName: values.equipmentName.trim() } }); context.toast('任务已分配'); done();
    } });
    const form = document.querySelector('#active-dialog-form');
    async function loadAssignees() {
      const order = orders.find((item) => String(item.id) === form.elements.orderId.value);
      const assignees = pageItems(await context.api.request('/lab/tasks/assignees', { query: { labId: order.labId } }));
      const select = form.elements.assigneeId;
      select.innerHTML = directoryOptions(assignees).map((option) => `<option value="${option.value}">${escapeHtml(option.label)}</option>`).join('');
      if (!assignees.length) select.innerHTML = '<option value="">该实验室暂无检测人员</option>';
    }
    form.elements.orderId.addEventListener('change', loadAssignees);
    await loadAssignees();
  } catch (error) { context.toast(error.message, 'error'); }
}

async function openResults(context, taskId, orderId, done) {
  try {
    const order = await context.api.request(`/orders/${orderId}`);
    const rows = order.items || [];
    const body = `<p class="form-hint">每个订单项目提交一条结果；数值范围可自动判断异常，定性结果必须明确选择。</p>${rows.map((item, index) => `<fieldset class="result-row" data-result-row data-order-item="${item.id}"><legend>${escapeHtml(item.itemNameSnapshot)}</legend><div class="form-grid">${field({ name: `name-${index}`, label: '结果名称', value: item.itemNameSnapshot, required: true })}${field({ name: `value-${index}`, label: '结果值', required: true })}${field({ name: `unit-${index}`, label: '单位', required: true })}${field({ name: `referenceRange-${index}`, label: '参考范围', required: true, placeholder: '0-40 / <5 / 阴性' })}${field({ name: `abnormalFlag-${index}`, label: '异常标记', options: [{ value: '', label: '按数值范围自动判断' }, { value: 'false', label: '正常' }, { value: 'true', label: '异常' }] })}</div></fieldset>`).join('')}`;
    context.dialog({ title: `录入任务 #${taskId} 检测结果`, wide: true, submitLabel: '保存检测结果', body, async onSubmit(form) {
      const values = [...form.querySelectorAll('[data-result-row]')].map((row, index) => ({ orderItemId: row.dataset.orderItem, name: form.elements[`name-${index}`].value, value: form.elements[`value-${index}`].value, unit: form.elements[`unit-${index}`].value, referenceRange: form.elements[`referenceRange-${index}`].value, abnormalFlag: form.elements[`abnormalFlag-${index}`].value }));
      await context.api.request(`/lab/tasks/${taskId}/results`, { method: 'POST', data: resultPayload(values) }); context.toast('检测结果已保存'); done();
    } });
  } catch (error) { context.toast(error.message, 'error'); }
}

function reasonAction(context, id, action, title, done) {
  context.dialog({ title, submitLabel: title, body: field({ name: 'reason', label: '原因说明', type: 'textarea', required: true, wide: true }), async onSubmit(form) {
    await context.api.request(`/lab/tasks/${id}/${action}`, { method: 'POST', data: { reason: formData(form).reason.trim() } }); context.toast(`${title}已提交`); done();
  } });
}
