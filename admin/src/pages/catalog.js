import { contractPayload } from '../adapters.js';
import { formData } from './shared.js';
import { cell, money, pageItems, yuanToCents } from '../utils.js';
import { badge, field, pageHeader, renderState, renderTable } from '../components/ui.js';
import { hospitalOptions, itemOptions } from '../options.js';

export async function render(container, context) {
  const { api } = context;
  let tab = 'items';
  let labs = [];
  let contracts = [];
  let selectedLab = '';
  container.innerHTML = `${pageHeader('实验室项目与签约定价', '维护项目状态、医院合同、结算额度与协议价', '<button class="button" id="contract-new">新建签约</button>')}
    <div class="tabs"><button class="tab active" data-tab="items">检测项目</button><button class="tab" data-tab="labs">实验室</button><button class="tab" data-tab="contracts">签约合同</button></div><section class="panel"><div id="catalog-tools"></div><div id="catalog-list"></div></section>`;
  const list = container.querySelector('#catalog-list');
  const tools = container.querySelector('#catalog-tools');
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      if (!labs.length) labs = pageItems(await api.request('/labs'));
      if (tab === 'items') {
        if (!selectedLab && labs[0]) selectedLab = String(labs[0].id);
        tools.innerHTML = `<div class="toolbar"><label>所属实验室<select id="catalog-lab">${labs.map((lab) => `<option value="${cell(lab, 'id')}" ${String(lab.id) === selectedLab ? 'selected' : ''}>${cell(lab, 'name')}</option>`).join('')}</select></label></div>`;
        tools.querySelector('#catalog-lab')?.addEventListener('change', (event) => { selectedLab = event.target.value; load(); });
        if (!selectedLab) { list.innerHTML = renderTable({ columns: [{ label: '项目', key: 'name' }], rows: [], emptyText: '暂无可用实验室' }); return; }
        const data = await api.request('/test-items', { query: { labId: selectedLab } });
        list.innerHTML = renderTable({ rows: pageItems(data), columns: [
          { label: '项目', render: (row) => `<strong>${cell(row, 'name')}</strong><small class="cell-sub">${cell(row, 'code')}</small>` },
          { label: '分类', key: 'category' }, { label: '样本', key: 'sampleType' },
          { label: '目录价', render: (row) => money(row.priceCents) }, { label: '时效', render: (row) => `${row.turnaroundHours || '—'} 小时` },
          { label: '状态', render: (row) => badge(row.status) }
        ], actions: (row) => `<button class="button text" data-item-status="${row.id}" data-status="${row.status}">${row.status === 'ACTIVE' ? '停用' : '启用'}</button><button class="button text" data-template="${row.id}">报告模板</button>` });
      } else if (tab === 'labs') {
        tools.innerHTML = '';
        list.innerHTML = renderTable({ rows: labs, columns: [{ label: '实验室', key: 'name' }, { label: '编码', key: 'code' }, { label: '联系人', key: 'contactName' }, { label: '电话', key: 'contactPhone' }, { label: '地址', key: 'address' }] });
      } else {
        tools.innerHTML = '';
        const data = await api.request('/contracts');
        contracts = pageItems(data);
        list.innerHTML = renderTable({ rows: contracts, columns: [
          { label: '合同', key: 'name' }, { label: '医院 ID', key: 'hospitalId' }, { label: '实验室 ID', key: 'labId' },
          { label: '结算', render: (row) => row.settlementType === 'MONTHLY' ? '月结' : '现结' },
          { label: '折扣', render: (row) => `${row.discountBps / 100}%` }, { label: '月结额度', render: (row) => money(row.monthlyCreditLimitCents) },
          { label: '有效期', render: (row) => `${row.effectiveFrom} 至 ${row.effectiveTo || '长期'}` }, { label: '状态', render: (row) => badge(row.status) }
        ], actions: (row) => `<button class="button text" data-price="${row.id}">设置协议价</button>` });
      }
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#contract-new').addEventListener('click', async () => {
    try {
      const hospitals = pageItems(await api.request('/admin/hospitals', { query: { status: 'ACTIVE' } }));
      if (!hospitals.length || !labs.length) throw new Error('暂无可用医院或实验室');
      context.dialog({ title: '新建医院签约', wide: true, submitLabel: '创建合同', body: `<div class="form-grid">${field({ name: 'name', label: '合同名称', required: true })}${field({ name: 'hospitalId', label: '医院', options: hospitalOptions(hospitals), required: true })}${field({ name: 'labId', label: '实验室', options: labs.map((lab) => ({ value: lab.id, label: `${lab.name}（ID ${lab.id}）` })), required: true })}${field({ name: 'settlementType', label: '结算方式', options: [{ value: 'CURRENT', label: '现结' }, { value: 'MONTHLY', label: '月结' }] })}${field({ name: 'discount', label: '折扣（100 为无折扣）', type: 'number', value: 100, min: 0, step: '0.01', required: true })}${field({ name: 'monthlyCreditLimit', label: '月结信用额度（元）', type: 'number', value: '0.00', min: 0, step: '0.01', required: true })}${field({ name: 'effectiveFrom', label: '生效日期', type: 'date', required: true })}${field({ name: 'effectiveTo', label: '失效日期', type: 'date' })}</div>`, async onSubmit(form) {
        await api.request('/admin/contracts', { method: 'POST', data: contractPayload(formData(form)) }); context.toast('签约合同已创建'); tab = 'contracts'; load();
      } });
    } catch (error) { context.toast(error.message, 'error'); }
  });
  container.addEventListener('click', async (event) => {
    const tabButton = event.target.closest('[data-tab]');
    if (tabButton) { tab = tabButton.dataset.tab; container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton)); load(); }
    const status = event.target.closest('[data-item-status]');
    if (status) context.dialog({ title: status.dataset.status === 'ACTIVE' ? '停用检测项目' : '启用检测项目', submitLabel: '确认变更', body: field({ name: 'reason', label: '变更原因', type: 'textarea', required: true, wide: true }), async onSubmit(form) {
      await api.request(`/admin/test-items/${status.dataset.itemStatus}/status`, { method: 'PATCH', data: { status: status.dataset.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE', reason: formData(form).reason } }); context.toast('项目状态已更新'); load();
    } });
    const price = event.target.closest('[data-price]');
    if (price) {
      try {
        const contract = contracts.find((row) => String(row.id) === price.dataset.price);
        const items = pageItems(await api.request('/test-items', { query: { labId: contract.labId } }));
        if (!items.length) throw new Error('该合同实验室暂无检测项目');
        context.dialog({ title: '设置合同项目协议价', submitLabel: '保存协议价', body: `<div class="form-grid">${field({ name: 'itemId', label: '检测项目', options: itemOptions(items), required: true })}${field({ name: 'price', label: '协议单价（元）', type: 'number', min: 0, step: '0.01', required: true })}</div>`, async onSubmit(form) {
          const data = formData(form); await api.request(`/admin/contracts/${price.dataset.price}/items/${data.itemId}`, { method: 'PUT', data: { negotiatedPriceCents: yuanToCents(data.price) } }); context.toast('协议价已保存'); load();
        } });
      } catch (error) { context.toast(error.message, 'error'); }
    }
    const template = event.target.closest('[data-template]');
    if (template) {
      let templateJson = '{}';
      try { templateJson = (await api.request(`/reports/templates/${template.dataset.template}`)).templateJson; }
      catch (error) { if (error.status !== 404) { context.toast(error.message, 'error'); return; } }
      context.dialog({ title: '维护报告模板 JSON', submitLabel: '保存模板', wide: true, body: field({ name: 'templateJson', label: 'JSON 对象', type: 'textarea', value: templateJson, required: true, wide: true }), async onSubmit(form) {
        await api.request(`/reports/templates/${template.dataset.template}`, { method: 'PUT', data: { templateJson: formData(form).templateJson } }); context.toast('报告模板已保存');
      } });
    }
  });
  await load();
}
