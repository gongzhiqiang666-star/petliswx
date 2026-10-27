import { rolePayload, userStatusPayload } from '../adapters.js';
import { formData } from './shared.js';
import { escapeHtml, pageItems } from '../utils.js';
import { badge, field, pageHeader, renderDetail, renderState, renderTable } from '../components/ui.js';

const roleOptions = ['DOCTOR', 'HOSPITAL_ADMIN', 'LAB_STAFF', 'COURIER', 'ADMIN'].map((role) => ({ value: role, label: role }));

export async function render(container, context) {
  const { api, dialog } = context;
  let tab = 'users';
  let status = 'ACTIVE';
  container.innerHTML = `${pageHeader('用户与医院', '查询真实账号、维护角色归属与医院状态')}
    <div class="tabs"><button class="tab active" data-tab="users">用户账号</button><button class="tab" data-tab="hospitals">医院档案</button></div>
    <section class="panel"><div class="toolbar"><label>状态<select id="org-status"><option value="ACTIVE">启用</option><option value="INACTIVE">停用</option><option value="PENDING">待审核</option><option value="REJECTED">已驳回</option><option value="ALL">全部</option></select></label></div><div id="org-list"></div></section>`;
  const list = container.querySelector('#org-list');
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const data = await api.request(tab === 'users' ? '/admin/users' : '/admin/hospitals', { query: { status, page: 0, size: 50 } });
      list.innerHTML = renderTable({
        rows: pageItems(data),
        columns: tab === 'users' ? [
          { label: '用户', render: (row) => `<strong>${escapeHtml(row.displayName)}</strong><small class="cell-sub">${escapeHtml(row.username)}</small>` },
          { label: '联系电话', key: 'phone' }, { label: '医院', render: (row) => escapeHtml(row.hospitalName || row.hospitalId) },
          { label: '角色', render: (row) => escapeHtml((row.roles || []).join('、')) },
          { label: '微信', render: (row) => row.wechatBound ? '已绑定' : '未绑定' }, { label: '状态', render: (row) => badge(row.status) }
        ] : [
          { label: '医院名称', key: 'name' }, { label: '登录编码', key: 'code' }, { label: '状态', render: (row) => badge(row.status) }
        ],
        actions: (row) => tab === 'hospitals'
          ? `<button class="button text" data-hospital="${row.id}">查看档案</button>`
          : `<button class="button text" data-user-roles="${row.id}">角色</button><button class="button ${row.status === 'ACTIVE' ? 'danger' : ''} text" data-user-status="${row.id}" data-current-status="${row.status}">${row.status === 'ACTIVE' ? '停用' : '启用'}</button>`
      });
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }

  async function openRoles(userId) {
    try {
      const assignments = await api.request(`/admin/users/${userId}/roles`);
      dialog({
        title: `维护用户 #${userId} 角色`, wide: true, submitLabel: '新增角色',
        body: `${renderTable({ rows: assignments, emptyText: '当前使用兼容医生角色', columns: [
          { label: '分配 ID', key: 'id' }, { label: '角色', key: 'roleCode' },
          { label: '医院 ID', key: 'hospitalId' }, { label: '实验室 ID', key: 'laboratoryId' }
        ], actions: (row) => `<button class="button danger text" type="button" data-remove-role="${row.id}">删除</button>` })}<div class="form-grid">${field({ name: 'roleCode', label: '角色', options: roleOptions, required: true })}${field({ name: 'hospitalId', label: '医院 ID（医生/医院管理员必填）', type: 'number', min: 1 })}${field({ name: 'laboratoryId', label: '实验室 ID（实验人员必填）', type: 'number', min: 1 })}</div>`,
        async onSubmit(form) {
          await api.request(`/admin/users/${userId}/roles`, { method: 'POST', data: rolePayload(formData(form)) });
          context.toast('角色已保存，用户旧会话已撤销'); load();
        }
      });
      document.querySelectorAll('[data-remove-role]').forEach((button) => button.addEventListener('click', () => {
        const assignment = assignments.find((row) => String(row.id) === button.dataset.removeRole);
        dialog({
          title: '确认删除角色', submitLabel: '确认删除',
          body: `<p>确认删除用户 #${userId} 的 ${escapeHtml(assignment?.roleCode)} 角色？删除后该用户旧会话将撤销。</p>`,
          async onSubmit() {
            await api.request(`/admin/users/${userId}/roles/${button.dataset.removeRole}`, { method: 'DELETE' });
            context.toast('角色已删除，用户旧会话已撤销'); load();
          }
        });
      }));
    } catch (error) { dialog({ title: '无法读取角色', body: renderState('error', error) }); }
  }

  container.addEventListener('click', async (event) => {
    const tabButton = event.target.closest('[data-tab]');
    if (tabButton) {
      tab = tabButton.dataset.tab;
      container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton));
      load();
    }
    const hospitalButton = event.target.closest('[data-hospital]');
    if (hospitalButton) {
      try {
        const row = await api.request(`/admin/hospitals/${hospitalButton.dataset.hospital}`);
        dialog({ title: row.name, body: renderDetail([
          { label: '医院 ID', value: row.id }, { label: '登录编码', value: row.code }, { label: '状态', value: row.status },
          { label: '联系电话', value: row.contactPhone }, { label: '地址', value: row.address, wide: true },
          { label: '开票抬头', value: row.invoice, wide: true }
        ]) });
      } catch (error) { dialog({ title: '无法查看医院', body: renderState('error', error) }); }
    }
    const roleButton = event.target.closest('[data-user-roles]');
    if (roleButton) openRoles(roleButton.dataset.userRoles);
    const statusButton = event.target.closest('[data-user-status]');
    if (statusButton) {
      const active = statusButton.dataset.currentStatus !== 'ACTIVE';
      dialog({
        title: active ? '启用用户' : '停用用户', submitLabel: active ? '确认启用' : '确认停用',
        body: `<div class="form-grid">${field({ name: 'active', label: '目标状态', value: String(active), options: [{ value: 'true', label: '启用' }, { value: 'false', label: '停用' }] })}${field({ name: 'reason', label: '操作原因（停用必填）', type: 'textarea', wide: true })}</div>`,
        async onSubmit(form) {
          await api.request(`/admin/users/${statusButton.dataset.userStatus}/status`, { method: 'PATCH', data: userStatusPayload(formData(form)) });
          context.toast(active ? '用户已启用' : '用户已停用并撤销旧会话'); load();
        }
      });
    }
  });
  container.querySelector('#org-status').addEventListener('change', (event) => { status = event.target.value; load(); });
  await load();
}
