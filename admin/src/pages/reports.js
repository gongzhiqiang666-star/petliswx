import { reportPayload } from '../adapters.js';
import { formData } from './shared.js';
import { formatDate, pageItems, statusLabel, escapeHtml } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { canReportAction } from '../permissions.js';
import { printHtml } from './orders.js';

export async function render(container, context) {
  const { api, store } = context;
  const user = store.getState().user;
  let templateMode = false;
  let templateRows = [];
  let templateFilters = { q: '', status: '' };
  let loadSequence = 0;
  let filters = { status: '', page: 0, size: 20 };
  const actions = canReportAction('create', user) ? '<button class="button secondary" id="template-create">模板管理</button><button class="button" id="report-create">生成报告</button>' : '';
  container.innerHTML = `${pageHeader('报告审核发布', '按检测项目模板生成中文报告，内部审核后发布至医院', actions)}<section class="panel"><form id="report-filter" class="toolbar"><label>报告状态<select name="status"><option value="">全部</option>${['PENDING_REVIEW','APPROVED','REJECTED','PUBLISHED','CANCELLED'].map((status) => `<option value="${status}">${statusLabel(status)}</option>`).join('')}</select></label><button class="button secondary">查询</button></form><form id="template-filter" class="toolbar" style="display:none"><label>模板或项目<input name="q" maxlength="100" placeholder="名称、编码或检测项目"></label><label>模板状态<select name="status"><option value="">全部版本</option><option value="ACTIVE">使用中</option><option value="ARCHIVED">已归档</option></select></label><button class="button secondary">筛选模板</button></form><div id="report-list"></div></section>`;
  const list = container.querySelector('#report-list');
  async function load() {
    const sequence = ++loadSequence;
    list.innerHTML = renderState('loading');
    try {
      const data = await api.request(templateMode ? '/report-workflows/templates' : '/report-workflows', { query: templateMode ? { ...templateFilters, page: filters.page, size: filters.size } : filters });
      if (sequence !== loadSequence) return;
      if (templateMode) {
        templateRows = pageItems(data);
        list.innerHTML = `<p>修改会保存为新版本，历史报告保留原模板。归档版本可查看并另存新版本。</p><button class="button secondary" data-template-action="create">新建模板</button>` + renderTable({ rows: templateRows, emptyText: '暂无报告模板', columns: [
          { label: '模板', render: row => `<strong>${escapeHtml(row.template_name)}</strong><small class="cell-sub">${escapeHtml(row.template_code)}</small>` },
          { label: '检测项目', key: 'test_name' }, { label: '实验室', key: 'laboratory_id' },
          { label: '版本', key: 'version' }, { label: '状态', render: row => row.status === 'ACTIVE' ? '使用中' : '已归档' }
        ], actions: row => `<button class="button text" data-template-action="view" data-id="${row.id}">查看</button><button class="button text" data-template-action="edit" data-id="${row.id}">另存新版本</button>${row.status === 'ACTIVE' ? `<button class="button text" data-template-action="archive" data-id="${row.id}">停用</button>` : ''}` }) + pagination(data);
        return;
      }
      list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无报告工作流', columns: [
        { label: '报告', render: (row) => `<strong>#${row.id}</strong><small class="cell-sub">明细 #${row.detailId}</small>` },
        { label: '订单', key: 'orderId' }, { label: '实验室', key: 'laboratoryId' },
        { label: '状态', render: (row) => badge(row.status) }, { label: '提交时间', render: (row) => formatDate(row.submittedAt) },
        { label: '发布时间', render: (row) => formatDate(row.publishedAt) }
      ], actions: (row) => reportActions(row, user) }) + pagination(data);
    } catch (error) { if (sequence !== loadSequence) return; list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#report-create')?.addEventListener('click', () => openGenerate(context, load));
  container.querySelector('#template-create')?.addEventListener('click', () => { templateMode = !templateMode; filters.page = 0; container.querySelector('#template-create').textContent = templateMode ? '返回报告列表' : '模板管理'; container.querySelector('#report-filter').style.display = templateMode ? 'none' : ''; container.querySelector('#template-filter').style.display = templateMode ? '' : 'none';  load(); });
  container.querySelector('#template-filter').addEventListener('submit', event => { event.preventDefault(); templateFilters = formData(event.currentTarget); filters.page = 0; load(); });
  container.querySelector('#report-filter').addEventListener('submit', (event) => { event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load(); });
  container.addEventListener('click', async (event) => {
    const page = event.target.closest('[data-page]');
    if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); return; }
    const templateButton = event.target.closest('[data-template-action]');
    if (templateButton) {
      const row = templateRows.find(item => String(item.id) === templateButton.dataset.id);
      if (templateButton.dataset.templateAction === 'create') return openTemplate(context, null, load);
      if (!row) return;
      if (templateButton.dataset.templateAction === 'edit') return openTemplate(context, row, load);
      if (templateButton.dataset.templateAction === 'archive') return context.dialog({ title: '停用报告模板', submitLabel: '确认停用', body: `<p>停用“${escapeHtml(row.template_name)}”第 ${Number(row.version)} 版后，新生成的报告将选择其他启用模板；若没有则使用默认检测模板。历史报告与PDF保持不变，可从归档版本另存新版本恢复使用。</p>`, async onSubmit() {
        await api.request(`/report-workflows/templates/${row.id}/archive`, { method: 'POST' }); context.toast('模板已停用'); load();
      } });
      return context.dialog({ title: `${row.template_name} · 第 ${row.version} 版`, body: renderDetail([
        { label: '模板编码', value: row.template_code }, { label: '检测项目', value: row.test_name },
        { label: '实验室 ID', value: row.laboratory_id }, { label: '版本', value: row.version },
        { label: '状态', value: row.status === 'ACTIVE' ? '使用中' : '已归档' }, { label: '模板内容', value: row.content, wide: true }
      ]) });
    }
    const button = event.target.closest('[data-report-action]');
    if (!button) return;
    const id = button.dataset.id;
    try {
      if (button.dataset.reportAction === 'review') return openReview(context, id, load);
      if (button.dataset.reportAction === 'detail') return showReport(context, id);
      if (button.dataset.reportAction === 'download') return download(context, id);
      if (button.dataset.reportAction === 'print') return printHtml(api, `/admin/print/reports/${button.dataset.detailId}`, context.toast);
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
  if (canReportAction('print', user)) buttons.push(`<button class="button text" data-report-action="print" data-id="${row.id}" data-detail-id="${row.detailId}">打印</button>`);
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
async function openTemplate(context, template = null, done = () => {}) {
  let laboratories = [];
  if (!template) {
    try { laboratories = (await context.api.request('/report-workflows/templates/options')).laboratories; }
    catch (error) { context.toast(error.message, 'error'); return; }
    if (!laboratories?.length) { context.toast('暂无可配置的启用实验室', 'error'); return; }
  }
  const identity = template ? renderDetail([{ label: '检测项目', value: template.test_name }, { label: '实验室 ID', value: template.laboratory_id }, { label: '模板编码', value: template.template_code }, { label: '来源版本', value: template.version }]) : `${field({ name: 'laboratoryId', label: '实验室', options: [{ value: '', label: '请选择实验室' }, ...laboratories.map(lab => ({ value: lab.id, label: lab.name }))], required: true })}${field({ name: 'projectSearch', label: '查找检测项目', placeholder: '输入项目名称或编码' })}${field({ name: 'infoTestTypeId', label: '检测项目', options: [{ value: '', label: '先选择实验室' }], required: true })}${field({ name: 'templateCode', label: '模板编码', required: true })}`;
  context.dialog({ title: template ? '另存模板新版本' : '新建报告模板', submitLabel: '保存新版本', body: `<p>保存后新生成的报告使用新版本，已经生成的报告不受影响。</p><div class="form-grid">${identity}${field({ name: 'templateName', label: '模板名称', value: template?.template_name || '', required: true })}${field({ name: 'content', label: '中文模板内容', type: 'textarea', value: template?.content || '', required: true, wide: true })}</div>`, onReady(form) {
    if (template) return;
    const lab = form.querySelector('[name="laboratoryId"]');
    const item = form.querySelector('[name="infoTestTypeId"]');
    const search = form.querySelector('[name="projectSearch"]');
    let availableItems = [];
    let request = 0;
    const showItems = () => {
      const selected = item.value;
      const keyword = search.value.trim().toLowerCase();
      const matches = availableItems.filter(row => `${row.name || ''} ${row.code || ''} ${row.id}`.toLowerCase().includes(keyword));
      item.innerHTML = `<option value="">${matches.length ? '请选择检测项目' : '没有匹配的启用项目'}</option>` + matches.map(row => `<option value="${Number(row.id)}">${escapeHtml(String(row.name || '').trim() || '未命名项目')}（${escapeHtml(row.code || `ID ${Number(row.id)}`)}）</option>`).join('');
      if (matches.some(row => String(row.id) === selected)) item.value = selected;
    };
    search.disabled = true;
    search.addEventListener('input', showItems);
    item.disabled = true;
    lab.addEventListener('change', async () => {
      const current = ++request;
      availableItems = []; search.value = ''; search.disabled = true;
      item.disabled = true; item.innerHTML = '<option value="">正在加载项目…</option>';
      if (!lab.value) { item.innerHTML = '<option value="">先选择实验室</option>'; return; }
      try {
        const data = await context.api.request('/report-workflows/templates/options', { query: { laboratoryId: lab.value } });
        if (current !== request || !form.isConnected) return;
        availableItems = data.items;
        search.disabled = false; item.disabled = false;
        showItems();
      } catch (error) {
        if (current !== request || !form.isConnected) return;
        item.innerHTML = '<option value="">加载失败，请重选实验室</option>'; context.toast(error.message, 'error');
      }
    });
  }, async onSubmit(form) {
    const value = formData(form);
    if (!template && !value.infoTestTypeId) throw new Error('请选择实验室及检测项目');
    await context.api.request(`/report-workflows/templates/${Number(template?.info_test_type_id ?? value.infoTestTypeId)}`, { method: 'PUT', data: { laboratoryId: Number(template?.laboratory_id ?? value.laboratoryId), templateName: value.templateName.trim(), templateCode: template?.template_code ?? value.templateCode.trim(), content: value.content } }); context.toast('模板新版本已保存'); done();
  } });
}
async function showReport(context, id) {
  try { const row = await context.api.request(`/report-workflows/${id}`); context.dialog({ title: `报告工作流 #${id}`, body: renderDetail([{ label: '订单 ID', value: row.order_id ?? row.orderId }, { label: '明细 ID', value: row.detail_id ?? row.detailId }, { label: '状态', value: statusLabel(row.status) }, { label: '模板 ID', value: row.template_id ?? row.templateId }, { label: '模板版本', value: row.template_version ?? row.templateVersion }, { label: '审核意见', value: row.review_note ?? row.reviewNote }, { label: '提交时间', value: formatDate(row.submitted_at ?? row.submittedAt) }, { label: '审核时间', value: formatDate(row.reviewed_at ?? row.reviewedAt) }, { label: '发布时间', value: formatDate(row.published_at ?? row.publishedAt) }]) }); }
  catch (error) { context.toast(error.message, 'error'); }
}
async function download(context, id) {
  const file = await context.api.download(`/report-workflows/${id}/download`);
  const url = URL.createObjectURL(file.blob); const link = document.createElement('a'); link.href = url; link.download = file.filename; link.click(); URL.revokeObjectURL(url);
}
