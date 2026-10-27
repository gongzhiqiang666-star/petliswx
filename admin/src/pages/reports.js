import { reportPayload } from '../adapters.js';
import { formData } from './shared.js';
import { formatDate, pageItems } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { canReportAction } from '../permissions.js';

export async function render(container, context) {
  const { api, store } = context;
  const user = store.getState().user;
  let filters = { status: '', page: 0, size: 20 };
  const actions = canReportAction('create', user) ? '<button class="button secondary" id="template-create">配置模板</button><button class="button" id="report-create">生成报告</button>' : '';
  container.innerHTML = `${pageHeader('报告审核发布', '按检测项目模板生成中文报告，内部审核后发布至医院', actions)}<section class="panel"><form id="report-filter" class="toolbar"><label>报告状态<select name="status"><option value="">全部</option>${['PENDING_REVIEW','APPROVED','REJECTED','PUBLISHED'].map((status) => `<option>${status}</option>`).join('')}</select></label><button class="button secondary">查询</button></form><div id="report-list"></div></section>`;
  const list = container.querySelector('#report-list');
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const data = await api.request('/report-workflows', { query: filters });
      list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无报告工作流', columns: [
        { label: '报告', render: (row) => `<strong>#${row.id}</strong><small class="cell-sub">明细 #${row.detailId}</small>` },
        { label: '订单', key: 'orderId' }, { label: '实验室', key: 'laboratoryId' },
        { label: '状态', render: (row) => badge(row.status) }, { label: '提交时间', render: (row) => formatDate(row.submittedAt) },
        { label: '发布时间', render: (row) => formatDate(row.publishedAt) }
      ], actions: (row) => reportActions(row, user) }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#report-create')?.addEventListener('click', () => openGenerate(context, load));
  container.querySelector('#template-create')?.addEventListener('click', () => openTemplate(context));
  container.querySelector('#report-filter').addEventListener('submit', (event) => { event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load(); });
  container.addEventListener('click', async (event) => {
    const page = event.target.closest('[data-page]');
    if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); return; }
    const button = event.target.closest('[data-report-action]');
    if (!button) return;
    const id = button.dataset.id;
    try {
      if (button.dataset.reportAction === 'review') return openReview(context, id, load);
      if (button.dataset.reportAction === 'detail') return showReport(context, id);
      if (button.dataset.reportAction === 'download') return download(context, id);
      await api.request(`/report-workflows/${id}/publish`, { method: 'POST' }); context.toast('报告已发布并同步医院端'); load();
    } catch (error) { context.toast(error.message, 'error'); }
  });
  await load();
}

function reportActions(row, user) {
  const buttons = [];
  if (row.status === 'PENDING_REVIEW' && canReportAction('review', user)) buttons.push(`<button class="button text" data-report-action="review" data-id="${row.id}">审核</button>`);
  if (row.status === 'APPROVED' && canReportAction('publish', user)) buttons.push(`<button class="button text" data-report-action="publish" data-id="${row.id}">发布</button>`);
  if (canReportAction('download', user)) buttons.push(`<button class="button text" data-report-action="download" data-id="${row.id}">下载</button>`);
  buttons.push(`<button class="button text" data-report-action="detail" data-id="${row.id}">详情</button>`);
  return buttons.join('');
}
function openGenerate(context, done) {
  context.dialog({ title: '生成检测报告', submitLabel: '生成并提交审核', body: `<div class="form-grid">${field({ name: 'taskId', label: '结果已录入的任务 ID', type: 'number', required: true })}${field({ name: 'summary', label: '样本摘要', type: 'textarea', required: true, wide: true })}${field({ name: 'conclusion', label: '临床结论', type: 'textarea', required: true, wide: true })}</div>`, async onSubmit(form) {
    const value = formData(form); await context.api.request('/report-workflows', { method: 'POST', data: { taskId: Number(value.taskId), ...reportPayload(value) } }); context.toast('报告已生成并提交审核'); done();
  } });
}
function openReview(context, id, done) {
  context.dialog({ title: `审核报告 #${id}`, submitLabel: '提交审核', body: `<div class="form-grid">${field({ name: 'decision', label: '审核决定', options: [{ value: 'APPROVE', label: '通过' }, { value: 'REJECT', label: '驳回' }], required: true })}${field({ name: 'reason', label: '审核意见（驳回必填）', type: 'textarea', wide: true })}</div>`, async onSubmit(form) {
    await context.api.request(`/report-workflows/${id}/review`, { method: 'POST', data: formData(form) }); context.toast('审核结果已保存'); done();
  } });
}
function openTemplate(context) {
  context.dialog({ title: '按旧检测项目配置报告模板', submitLabel: '保存新版本', body: `<div class="form-grid">${field({ name: 'infoTestTypeId', label: 'InfoTestTypeId', type: 'number', required: true })}${field({ name: 'laboratoryId', label: '实验室 ID', type: 'number', required: true })}${field({ name: 'templateName', label: '模板名称', required: true })}${field({ name: 'templateCode', label: '模板编码', required: true })}${field({ name: 'content', label: '中文模板内容', type: 'textarea', required: true, wide: true })}</div>`, async onSubmit(form) {
    const value = formData(form); await context.api.request(`/report-workflows/templates/${Number(value.infoTestTypeId)}`, { method: 'PUT', data: { laboratoryId: Number(value.laboratoryId), templateName: value.templateName.trim(), templateCode: value.templateCode.trim(), content: value.content } }); context.toast('模板新版本已保存');
  } });
}
async function showReport(context, id) {
  try { const row = await context.api.request(`/report-workflows/${id}`); context.dialog({ title: `报告工作流 #${id}`, body: renderDetail([{ label: '订单 ID', value: row.order_id ?? row.orderId }, { label: '明细 ID', value: row.detail_id ?? row.detailId }, { label: '状态', value: row.status }, { label: '提交时间', value: formatDate(row.submitted_at ?? row.submittedAt) }, { label: '审核时间', value: formatDate(row.reviewed_at ?? row.reviewedAt) }, { label: '发布时间', value: formatDate(row.published_at ?? row.publishedAt) }]) }); }
  catch (error) { context.toast(error.message, 'error'); }
}
async function download(context, id) {
  const file = await context.api.download(`/report-workflows/${id}/download`);
  const url = URL.createObjectURL(file.blob); const link = document.createElement('a'); link.href = url; link.download = file.filename; link.click(); URL.revokeObjectURL(url);
}
