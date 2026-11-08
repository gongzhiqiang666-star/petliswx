import { resultPayload } from '../adapters.js';
import { formData } from './shared.js';
import { escapeHtml, formatDate, pageItems, statusLabel } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { canLabAction } from '../permissions.js';

export function assignmentLabel(raw) {
  try {const value=typeof raw==='string'?JSON.parse(raw):raw;if(!value || typeof value!=='object')return '历史快照缺失';return `人员 ${value.assignedTo ? '#'+value.assignedTo : '未分配'} · 设备 ${value.equipmentCode || '未指定'}`;}catch{return '历史快照缺失';}
}

export function availableTaskActions(task,user) {
  return {
    results:canLabAction('results',user) && task.status==='TESTING' && Number(task.assignedTo ?? task.assigned_to)===Number(user.id ?? user.userId),
    reassign:canLabAction('reassign',user) && task.status==='TESTING' && !(task.results || []).length,
    return:canLabAction('return',user) && ['RECEIVED','TESTING','RESULT_ENTERED','REPORT_REVIEW','REJECTED'].includes(task.status),
    retest:canLabAction('retest',user) && task.status==='RETURNED'
  };
}

export async function render(container, context) {
  const { api, store } = context;
  const user = store.getState().user;
  let filters = { status: '', page: 0, size: 20 };
  const assign = canLabAction('assign', user) ? '<button class="button" id="assign-new">分配任务</button>' : '';
  container.innerHTML = `${pageHeader('实验室任务', '分配已签收样本、按批次录入结果并处理退样复检', assign)}<section class="panel"><form id="task-filter" class="toolbar"><label>任务状态<select name="status"><option value="">全部</option>${['RECEIVED','TESTING','RESULT_ENTERED','REPORT_REVIEW','COMPLETED','RETURNED'].map((status) => `<option value="${status}">${statusLabel(status)}</option>`).join('')}</select></label><button class="button secondary">查询</button></form><div id="task-list"></div></section>`;
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
    const actions=availableTaskActions(task,user);
    const controls='<div class="button-row">'+[
      actions.results?'<button class="button" id="task-results">录入新批次结果</button>':'',
      actions.reassign?'<button class="button secondary" id="task-reassign">改派人员/设备</button>':'',
      actions.return?'<button class="button secondary" id="task-return">退样</button>':'',
      actions.retest?'<button class="button secondary" id="task-retest">复检</button>':''
    ].join('')+'</div>';
    context.dialog({ title: `实验室任务 #${id}`, wide: true, body: `${renderDetail([{ label: '订单 ID', value: task.orderId ?? task.order_id }, { label: '实验室 ID', value: task.labId ?? task.laboratory_id }, { label: '检测人员 ID', value: task.assignedTo ?? task.assigned_to }, { label: '设备', value: task.equipmentName ?? task.equipment_code }, { label: '批次', value: task.testBatch }, { label: '状态', value: statusLabel(task.status) }])}<h3>分配记录</h3>${renderTable({rows:task.assignmentHistory || [],emptyText:'暂无分配记录',columns:[{label:'操作',render:row=>escapeHtml(({LAB_TASK_ASSIGNED:'首次分配',LAB_TASK_REASSIGNED:'改派人员/设备'})[row.action] || row.action)},{label:'操作人',key:'operatorId'},{label:'原因',key:'reason'},{label:'时间',render:row=>formatDate(row.createdAt)},{label:'调整前',render:row=>escapeHtml(assignmentLabel(row.beforeSnapshot))},{label:'调整后',render:row=>escapeHtml(assignmentLabel(row.afterSnapshot))}]})}<h3>批次历史（不可覆盖）</h3>${renderTable({ rows: task.results || [], columns: [{ label: '结果 ID', key: 'id' }, { label: '批次', key: 'batchNo' }, { label: '状态', render: row => escapeHtml(statusLabel(row.resultStatus)) }, { label: '录入时间', render: (row) => formatDate(row.enteredAt) }] })}${controls}` });
    document.querySelector('#task-reassign')?.addEventListener('click', () => openReassign(context,task,done));
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
async function openReassign(context,task,done) {
  try {
    const staff=pageItems(await context.api.request('/lab/tasks/assignees',{query:{labId:task.labId ?? task.laboratory_id}}));
    context.dialog({title:'改派检测人员与设备',submitLabel:'确认改派',body:'<p>仅可调整尚未录入结果的任务，首次开始时间保留。请核实交接并填写原因。</p><div class="form-grid">'+field({name:'assigneeId',label:'本实验室检测人员',value:String(task.assignedTo ?? task.assigned_to),options:staff.map(row=>({value:String(row.id),label:row.name || row.username || `人员 ${row.id}`})),required:true})+field({name:'equipmentCode',label:'检测设备',value:task.equipmentName ?? task.equipment_code,required:true})+field({name:'reason',label:'改派原因',type:'textarea',required:true,wide:true})+'</div>',async onSubmit(form){
      const data=formData(form);await context.api.request(`/lab/tasks/${task.id}/reassign`,{method:'POST',data:{assigneeId:Number(data.assigneeId),equipmentCode:data.equipmentCode.trim(),version:Number(task.version),reason:data.reason.trim()}});context.toast('人员与设备已更新');await done();
    }});
  } catch(error) {context.toast(error.message,'error');}
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
