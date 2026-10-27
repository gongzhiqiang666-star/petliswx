import { formData } from './shared.js';
import { cell, moneyYuan, pageItems, value } from '../utils.js';
import { badge, field, pageHeader, renderState, renderTable } from '../components/ui.js';
import { hospitalOptions } from '../options.js';

export async function render(container, context) {
  const { api } = context;
  let tab = 'items';
  let laboratories = [];
  let selectedLab = '';

  container.innerHTML = `${pageHeader('实验室项目与签约定价', '旧项目主表、医院签约、额度与协议价', '<button class="button" id="contract-new">新建签约</button>')}
    <div class="tabs"><button class="tab active" data-tab="items">检测项目</button><button class="tab" data-tab="laboratories">实验室</button><button class="tab" data-tab="contracts">签约合同</button></div><section class="panel"><div id="catalog-tools"></div><div id="catalog-list"></div></section>`;
  const list = container.querySelector('#catalog-list');
  const tools = container.querySelector('#catalog-tools');

  async function load() {
    list.innerHTML = renderState('loading');
    try {
      if (!laboratories.length) laboratories = pageItems(await api.request('/admin/catalog/laboratories'));
      if (tab === 'laboratories') {
        tools.innerHTML = '';
        list.innerHTML = renderTable({ rows: laboratories, emptyText: '暂无实验室', columns: [
          { label: '实验室', key: 'name' }, { label: '编码', key: 'code' }, { label: '联系人', key: 'contactName' }, { label: '电话', key: 'contactPhone' }
        ] });
        return;
      }
      if (tab === 'contracts') {
        tools.innerHTML = '';
        const rows = pageItems(await api.request('/admin/contracts'));
        list.innerHTML = renderTable({ rows, emptyText: '暂无签约', columns: [
          { label: '合同号', render: (row) => cell(row, 'contractNo') },
          { label: '医院 ID', render: (row) => cell(row, 'hospitalId') },
          { label: '实验室 ID', render: (row) => cell(row, 'laboratoryId') },
          { label: '结算', render: (row) => value(row, 'settlementType') === 'MONTHLY' ? '月结' : '现结' },
          { label: '账期（天）', render: (row) => cell(row, 'billingCycleDays') },
          { label: '信用额度（元）', render: (row) => moneyYuan(value(row, 'creditAmount', 0)) },
          { label: '状态', render: (row) => badge(value(row, 'status', '')) }
        ], actions: (row) => `<button class="button text" data-price="${row.id}" data-lab="${value(row, 'laboratoryId')}">设置协议价</button>` });
        return;
      }
      if (!selectedLab && laboratories[0]) selectedLab = String(laboratories[0].id);
      tools.innerHTML = `<label>所属实验室<select id="catalog-lab">${laboratories.map((lab) => `<option value="${lab.id}">${cell(lab, 'name')}</option>`).join('')}</select></label>`;
      const labSelect = tools.querySelector('#catalog-lab');
      if (labSelect) {
        labSelect.value = selectedLab;
        labSelect.addEventListener('change', (event) => { selectedLab = event.target.value; load(); });
      }
      const rows = selectedLab ? pageItems(await api.request('/admin/catalog/items', { query: { laboratoryId: selectedLab } })) : [];
      list.innerHTML = renderTable({ rows, emptyText: '暂无检测项目', columns: [
        { label: '项目 ID', render: (row) => cell(row, 'id') },
        { label: '项目', render: (row) => cell(row, 'name') },
        { label: '目录价（元）', render: (row) => moneyYuan(row.price) },
        { label: '状态', render: (row) => badge(Number(row.active) === 1 ? 'ACTIVE' : 'INACTIVE') }
      ], actions: (row) => `<button class="button text" data-item-status="${row.id}" data-active="${row.active}">${Number(row.active) === 1 ? '停用' : '启用'}</button>` });
    } catch (error) {
      list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error);
    }
  }

  container.querySelector('#contract-new').addEventListener('click', () => {
    openContract(context, laboratories, () => { tab = 'contracts'; load(); });
  });

  container.addEventListener('click', async (event) => {
    const tabButton = event.target.closest('[data-tab]');
    if (tabButton) {
      tab = tabButton.dataset.tab;
      container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton));
      await load();
      return;
    }
    const status = event.target.closest('[data-item-status]');
    if (status) {
      try {
        await api.request(`/admin/catalog/laboratories/${selectedLab}/items/${status.dataset.itemStatus}/status`, { method: 'PATCH', data: { active: Number(status.dataset.active) !== 1 } });
        context.toast('项目状态已更新');
        await load();
      } catch (error) { context.toast(error.message, 'error'); }
      return;
    }
    const price = event.target.closest('[data-price]');
    if (price) await openPrice(context, price.dataset.price, price.dataset.lab, load);
  });

  await load();
}

async function openContract(context, laboratories, done) {
  const { api } = context;
  try {
    const hospitals = pageItems(await api.request('/admin/hospitals', { query: { status: 'ACTIVE' } }));
    const fields = [
      field({ name: 'contractNo', label: '合同编号', required: true }),
      field({ name: 'hospitalId', label: '医院', options: hospitalOptions(hospitals), required: true }),
      field({ name: 'laboratoryId', label: '实验室', options: laboratories.map((lab) => ({ value: lab.id, label: lab.name })), required: true }),
      field({ name: 'settlementType', label: '结算方式', options: [{ value: 'CASH', label: '现结' }, { value: 'MONTHLY', label: '月结' }] }),
      field({ name: 'billingCycleDays', label: '账期天数', type: 'number', value: 30, min: 1, required: true }),
      field({ name: 'creditAmount', label: '信用额度（元）', type: 'number', value: '0.00', min: 0, step: '0.01', required: true }),
      field({ name: 'effectiveFrom', label: '生效日期', type: 'date', required: true }),
      field({ name: 'effectiveTo', label: '失效日期', type: 'date' })
    ];
    context.dialog({
      title: '新建医院签约', wide: true, submitLabel: '创建合同', body: `<div class="form-grid">${fields.join('')}</div>`,
      async onSubmit(form) {
        const data = formData(form);
        await api.request('/admin/contracts', { method: 'POST', data: {
          ...data,
          hospitalId: Number(data.hospitalId), laboratoryId: Number(data.laboratoryId), billingCycleDays: Number(data.billingCycleDays),
          creditAmount: Number(data.creditAmount), effectiveTo: data.effectiveTo || null
        } });
        context.toast('签约合同已创建');
        done();
      }
    });
  } catch (error) { context.toast(error.message, 'error'); }
}

async function openPrice(context, contractId, laboratoryId, done) {
  const { api } = context;
  try {
    const items = pageItems(await api.request('/admin/catalog/items', { query: { laboratoryId } }));
    const fields = [
      field({ name: 'infoTestTypeId', label: '检测项目', options: items.map((item) => ({ value: item.id, label: item.name })), required: true }),
      field({ name: 'listPrice', label: '目录价（元）', type: 'number', min: 0, step: '0.01', required: true }),
      field({ name: 'discountRate', label: '协议折扣（10 为无折扣）', type: 'number', value: 10, min: 0.01, max: 10, step: '0.0001', required: true }),
      field({ name: 'agreedPrice', label: '固定协议价（元，优先于折扣）', type: 'number', min: 0, step: '0.01' }),
      field({ name: 'effectiveFrom', label: '生效日期', type: 'date', required: true }),
      field({ name: 'effectiveTo', label: '失效日期', type: 'date' })
    ];
    context.dialog({
      title: '设置合同项目协议价', submitLabel: '保存协议价', body: `<div class="form-grid">${fields.join('')}</div>`,
      async onSubmit(form) {
        const data = formData(form);
        await api.request(`/admin/contracts/${contractId}/prices`, { method: 'POST', data: {
          infoTestTypeId: Number(data.infoTestTypeId), listPrice: Number(data.listPrice), discountRate: Number(data.discountRate),
          agreedPrice: data.agreedPrice === '' ? null : Number(data.agreedPrice), effectiveFrom: data.effectiveFrom, effectiveTo: data.effectiveTo || null, status: 'ACTIVE'
        } });
        context.toast('协议价已保存');
        done();
      }
    });
  } catch (error) { context.toast(error.message, 'error'); }
}
