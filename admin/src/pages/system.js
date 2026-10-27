import { configPayload } from '../adapters.js';
import { formData } from './shared.js';
import { actionData, decodeActionData, formatDate, pageItems, value, cell } from '../utils.js';
import { field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';

export async function render(container, context) {
  const { api } = context;
  let tab = 'audits';
  let filters = { page: 0, size: 20, targetType: '', action: '', operatorId: '' };
  container.innerHTML = `${pageHeader('审计与系统配置', '追踪关键操作快照并维护平台运营参数')}
    <div class="tabs"><button class="tab active" data-tab="audits">操作审计</button><button class="tab" data-tab="config">系统配置</button></div><section class="panel"><div id="system-tools"></div><div id="system-list"></div></section>`;
  const list = container.querySelector('#system-list');
  const tools = container.querySelector('#system-tools');
  async function load() {
    list.innerHTML = renderState('loading');
    tools.innerHTML = tab === 'audits' ? `<form id="audit-filter" class="toolbar">${field({ name: 'targetType', label: '资源类型', value: filters.targetType, placeholder: 'ORDER / REPORT' })}${field({ name: 'action', label: '操作', value: filters.action })}${field({ name: 'operatorId', label: '操作人 ID', type: 'number', value: filters.operatorId })}<button class="button secondary">筛选</button></form>` : '<button class="button" id="config-new">新增 / 更新配置</button>';
    tools.querySelector('#audit-filter')?.addEventListener('submit', (event) => { event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load(); });
    tools.querySelector('#config-new')?.addEventListener('click', () => openConfig());
    try {
      const data = await api.request(tab === 'audits' ? '/admin/audits' : '/admin/config', { query: filters });
      list.innerHTML = tab === 'audits' ? renderTable({ rows: pageItems(data), emptyText: '暂无审计记录', columns: [
        { label: 'ID', render: (row) => cell(row, 'id') }, { label: '资源', render: (row) => `${cell(row, 'targetType')} #${cell(row, 'targetId')}` },
        { label: '操作', render: (row) => cell(row, 'action') }, { label: '操作人', render: (row) => cell(row, 'operatorId') },
        { label: '原因', render: (row) => cell(row, 'reason') }, { label: '时间', render: (row) => formatDate(value(row, 'createdAt', null)) }
      ], actions: (row) => `<button class="button text" data-audit="${value(row, 'id')}">快照</button>` }) + pagination(data)
        : renderTable({ rows: pageItems(data), emptyText: '暂无系统配置', columns: [
          { label: '配置键', render: (row) => `<code>${cell(row, 'key')}</code>` }, { label: '配置值', render: (row) => cell(row, 'value') },
          { label: '说明', render: (row) => cell(row, 'description') }, { label: '更新人', render: (row) => cell(row, 'updatedBy') },
          { label: '更新时间', render: (row) => formatDate(value(row, 'updatedAt', null)) }
        ], actions: (row) => `<button class="button text" data-config="${actionData({ key: value(row, 'key', ''), value: value(row, 'value', ''), description: value(row, 'description', '') })}">编辑</button>` }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  function openConfig(key = '', current = '', description = '') {
    context.dialog({ title: key ? `编辑配置 ${key}` : '新增系统配置', submitLabel: '保存配置', body: `<div class="form-grid">${field({ name: 'key', label: '配置键', value: key, required: true, placeholder: 'feature.flag' })}${field({ name: 'value', label: '配置值', value: current, required: true, wide: true })}${field({ name: 'description', label: '用途说明', value: description, type: 'textarea', wide: true })}</div>`, async onSubmit(form) {
      const data = formData(form); await api.request(`/admin/config/${encodeURIComponent(data.key)}`, { method: 'PUT', data: configPayload(data) }); context.toast('系统配置已保存'); load();
    } });
  }
  container.addEventListener('click', async (event) => {
    const tabButton = event.target.closest('[data-tab]'); if (tabButton) { tab = tabButton.dataset.tab; filters = { page: 0, size: 20, targetType: '', action: '', operatorId: '' }; container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton)); load(); }
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const config = event.target.closest('[data-config]'); if (config) {
      const current = decodeActionData(config.dataset.config);
      openConfig(current.key, current.value, current.description);
    }
    const audit = event.target.closest('[data-audit]'); if (audit) {
      try { const row = await api.request(`/admin/audits/${audit.dataset.audit}`); context.dialog({ title: `审计记录 #${audit.dataset.audit}`, wide: true, body: renderDetail([
        { label: '资源类型', value: value(row, 'targetType') }, { label: '资源 ID', value: value(row, 'targetId') },
        { label: '操作', value: value(row, 'action') }, { label: '操作人', value: value(row, 'operatorId') },
        { label: '时间', value: formatDate(value(row, 'createdAt', null)) }, { label: '原因', value: value(row, 'reason'), wide: true },
        { label: '数据快照', value: value(row, 'snapshot'), wide: true }
      ]) }); } catch (error) { context.toast(error.message, 'error'); }
    }
  });
  await load();
}
