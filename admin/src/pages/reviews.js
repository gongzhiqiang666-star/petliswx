import { reviewRequest } from '../adapters.js';
import { formData } from './shared.js';
import { escapeHtml, formatDate, moneyYuan, pageItems } from '../utils.js';
import { badge, field, pageHeader, renderState, renderTable } from '../components/ui.js';

const configs = {
  users: { title: '注册申请', endpoint: '/review/registrations', idField: 'userId' },
  profiles: { title: '资料变更', endpoint: '/review/profile-changes', idField: 'id' },
  orders: { title: '送检申请', endpoint: '/review/orders', idField: 'orderId' },
  invoices: { title: '发票申请', endpoint: '/review/invoices', idField: 'id' },
  hospitals: { title: '医院启停', endpoint: '/admin/hospitals', idField: 'id', query: { status: 'ALL' } }
};

export async function render(container, context) {
  const { api } = context;
  let tab = 'users';
  container.innerHTML = `${pageHeader('审核中心', '处理真实注册、资料、送检与发票待审申请')}
    <div class="tabs" role="tablist">${Object.entries(configs).map(([key, item]) => `<button data-tab="${key}" class="tab ${key === tab ? 'active' : ''}">${item.title}</button>`).join('')}</div>
    <section class="panel"><div class="toolbar"><button class="button secondary" id="review-refresh">刷新</button></div><div id="review-list"></div></section>`;
  const list = container.querySelector('#review-list');

  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const config = configs[tab];
      const data = await api.request(config.endpoint, { query: { page: 0, size: 50, ...config.query } });
      list.innerHTML = renderTable({
        rows: pageItems(data),
        emptyText: `暂无${config.title}记录`,
        columns: columns(tab),
        actions: (row) => `<button class="button small" data-review="${row[config.idField]}">${tab === 'hospitals' ? '处理' : '审核'}</button>`
      });
    } catch (error) {
      list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error);
    }
  }

  container.addEventListener('click', (event) => {
    const tabButton = event.target.closest('[data-tab]');
    if (tabButton) {
      tab = tabButton.dataset.tab;
      container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton));
      load();
    }
    const button = event.target.closest('[data-review]');
    if (!button) return;
    const id = button.dataset.review;
    context.dialog({
      title: `审核${configs[tab].title}`,
      body: `<div class="form-grid">${field({ name: 'approved', label: '审核结论', options: [{ value: 'true', label: tab === 'invoices' ? '开具' : '通过' }, { value: 'false', label: '驳回' }] })}${tab === 'invoices' ? field({ name: 'invoiceUrl', label: '电子发票 HTTPS 地址（开具必填）', wide: true }) : ''}${field({ name: 'reason', label: '审核意见（驳回必填）', type: 'textarea', wide: true })}</div>`,
      submitLabel: '提交审核',
      async onSubmit(form) {
        const request = reviewRequest(tab, id, formData(form));
        await api.request(request.path, { method: request.method, data: request.data });
        context.toast('审核结果已保存');
        load();
      }
    });
  });
  container.querySelector('#review-refresh').addEventListener('click', load);
  await load();
}

function columns(tab) {
  if (tab === 'hospitals') return [
    { label: '医院', render: (row) => `<strong>${escapeHtml(row.name)}</strong><small class="cell-sub">${escapeHtml(row.code)}</small>` },
    { label: '状态', render: (row) => badge(row.status) }
  ];
  if (tab === 'users') return [
    { label: '姓名', render: (row) => `<strong>${escapeHtml(row.nickname)}</strong><small class="cell-sub">${escapeHtml(row.username)}</small>` },
    { label: '手机号', key: 'mobile' }, { label: '职称', key: 'title' },
    { label: '申请医院', render: (row) => escapeHtml(row.hospitalName || row.requestedHospitalId) },
    { label: '申请时间', render: (row) => formatDate(row.createdAt) }
  ];
  if (tab === 'profiles') return [
    { label: '申请编号', key: 'id' }, { label: '用户 ID', key: 'userId' },
    { label: '变更资料', render: (row) => escapeHtml(row.requestedData) },
    { label: '申请时间', render: (row) => formatDate(row.createdAt) }
  ];
  if (tab === 'orders') return [
    { label: '送检单号', key: 'orderNo' }, { label: '宠物', key: 'animalName' },
    { label: '金额（元）', render: (row) => moneyYuan(row.amount) },
    { label: '提交时间', render: (row) => formatDate(row.submittedAt) }
  ];
  return [
    { label: '申请编号', key: 'id' }, { label: '用户 ID', key: 'userId' },
    { label: '金额（元）', render: (row) => moneyYuan(row.amount) },
    { label: '申请时间', render: (row) => formatDate(row.createdAt) }
  ];
}
