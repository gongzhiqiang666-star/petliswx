import {openHospitalProfile} from '../components/hospital-profile.js';
import {openHospitalContacts} from '../components/hospital-contacts.js';
import { rolePayload, userStatusPayload } from '../adapters.js';
import { formData } from './shared.js';
import { escapeHtml, pageItems, roleLabel } from '../utils.js';
import { badge, field, pageHeader, renderDetail, renderState, renderTable } from '../components/ui.js';
import {openHospitalFunds} from '../components/hospital-funds.js';
import {openHospitalDocuments} from '../components/hospital-documents.js';

const roleOptions = ['DOCTOR', 'HOSPITAL_ADMIN', 'LAB_STAFF', 'COURIER', 'ADMIN'].map((role) => ({ value: role, label: roleLabel(role) }));

export async function render(container, context) {
  const { api, dialog } = context;
  let fundCapabilities={};
  if(context.store?.getState().user?.roles?.includes('ADMIN'))try{fundCapabilities=await api.request('/admin/legacy/hospital-funds/capabilities')}catch(_){}
  let contactCapabilities={};
  if(context.store?.getState().user?.roles?.includes('ADMIN'))try{contactCapabilities=await api.request('/admin/legacy/hospital-contacts/capabilities')}catch(_){}
  let profileCapabilities={};
  if(context.store?.getState().user?.roles?.includes('ADMIN'))try{profileCapabilities=await api.request('/admin/legacy/hospital-profiles/capabilities')}catch(_){}
  let tab = 'users';
  let status = 'ACTIVE';
  container.innerHTML = `${pageHeader('用户与医院', '查询真实账号、维护角色归属与医院状态', context.store?.getState().user?.roles?.includes('ADMIN') ? '<button class="button" id="staff-new">新增员工</button>' : '')}
    <div class="tabs"><button class="tab active" data-tab="users">用户账号</button><button class="tab" data-tab="hospitals">医院档案</button></div>
    <section class="panel"><div class="toolbar">${profileCapabilities.hospitalProfileCreates===true?'<button type="button" class="button" data-hospital-new>新增医院</button>':''}<label>状态<select id="org-status"><option value="ACTIVE">启用</option><option value="INACTIVE">停用</option><option value="PENDING">待审核</option><option value="REJECTED">已驳回</option><option value="ALL">全部</option></select></label></div><div id="org-list"></div></section>`;
  container.querySelector('#staff-new')?.addEventListener('click',async()=>{
    try {
      const labs=pageItems(await api.request('/admin/catalog/laboratories')).filter(row=>row.active===undefined || Number(row.active)===1);
      dialog({title:'新增员工账号',submitLabel:'创建账号',body:'<p>员工使用独立账号登录，实验人员仅关联所选实验室，配送人员按运单分配。医生请使用注册审核流程。</p><p class="muted">初始密码至少8位，须包含字母和数字。</p><div class="form-grid">'+field({name:'username',label:'登录账号',required:true})+field({name:'displayName',label:'员工姓名',required:true})+field({name:'roleCode',label:'岗位',options:[{value:'LAB_STAFF',label:'实验人员'},{value:'COURIER',label:'配送人员'}],required:true})+field({name:'laboratoryId',label:'所属实验室（实验人员必选）',options:labs.map(row=>({value:row.id,label:row.name})),required:false})+field({name:'password',label:'初始密码',type:'password',required:true})+field({name:'confirmPassword',label:'再次输入密码',type:'password',required:true})+field({name:'reason',label:'建号原因（最多400字）',type:'textarea',required:true,wide:true})+'</div>',onReady(form){
        const role=form.querySelector('[name="roleCode"]');
        const laboratory=form.querySelector('[name="laboratoryId"]');
        const sync=()=>{const needed=role.value==='LAB_STAFF';role.closest('.form-field').style.gridColumn=needed?'':'1 / -1';laboratory.disabled=!needed;laboratory.required=needed;laboratory.closest('.form-field').style.display=needed?'':'none';};
        role.addEventListener('change',sync);sync();
      },async onSubmit(form){
        const data=formData(form);if(data.password!==data.confirmPassword)throw new Error('两次密码输入不一致');
        await api.request('/admin/staff',{method:'POST',data:{username:data.username.trim(),displayName:data.displayName.trim(),password:data.password,roleCode:data.roleCode,laboratoryId:data.roleCode==='LAB_STAFF'?Number(data.laboratoryId):null,reason:data.reason.trim()}});
        context.toast('员工账号已创建');tab='users';container.querySelectorAll('[data-tab]').forEach(button=>button.classList.toggle('active',button.dataset.tab==='users'));await load();
      }});
    }catch(error){context.toast(error.message,'error');}
  });
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
          { label: '角色', render: (row) => escapeHtml((row.roles || []).map(roleLabel).join('、')) },
          { label: '微信', render: (row) => row.wechatBound ? '已绑定' : '未绑定' }, { label: '状态', render: (row) => badge(row.status) }
        ] : [
          { label: '医院名称', key: 'name' }, { label: '登录编码', key: 'code' }, { label: '状态', render: (row) => badge(row.status) }
        ],
        actions: (row) => tab === 'hospitals'
          ? `${contactCapabilities.hospitalContacts===true?`<button class="button text" data-hospital-contacts="${row.id}">联系人</button>`:''}<button class="button text" data-hospital="${row.id}">查看档案</button>${profileCapabilities.hospitalProfileWrites===true?`<button class="button text" data-hospital-profile="${row.id}">编辑档案</button>`:''}<button class="button text" data-hospital-documents="${row.id}">资料文件</button>${fundCapabilities.hospitalFunds===true?`<button class="button text" data-hospital-funds="${row.id}">医院资金</button>`:''}`
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
    const contacts=event.target.closest('[data-hospital-contacts]');if(contacts){try{await openHospitalContacts(context,Number(contacts.dataset.hospitalContacts))}catch(error){context.toast(error.message,'error')}return;}
    const funds=event.target.closest('[data-hospital-funds]');if(funds){try{await openHospitalFunds(context,Number(funds.dataset.hospitalFunds),fundCapabilities.hospitalFundWrites===true)}catch(error){context.toast(error.message,'error')}return;}
    const documents=event.target.closest('[data-hospital-documents]');if(documents){try{await openHospitalDocuments(context,Number(documents.dataset.hospitalDocuments));}catch(error){context.toast(error.message,'error');}return;}
    const tabButton = event.target.closest('[data-tab]');
    if (tabButton) {
      tab = tabButton.dataset.tab;
      container.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button === tabButton));
      load();
    }
    const newHospital=event.target.closest('[data-hospital-new]');if(newHospital){try{await openHospitalProfile(context,null,load)}catch(error){context.toast(error.message,'error')}return;}
    const profileButton=event.target.closest('[data-hospital-profile]');if(profileButton){try{await openHospitalProfile(context,Number(profileButton.dataset.hospitalProfile),load)}catch(error){context.toast(error.message,'error')}return;}
    const hospitalButton = event.target.closest('[data-hospital]');
    if (hospitalButton) {
      try {
        const row = await api.request(`/admin/hospitals/${hospitalButton.dataset.hospital}`);
        dialog({ title: row.name, body: renderDetail([
          { label: '医院 ID', value: row.id }, { label: '医院英文名', value: row.englishName }, { label: '登录编码', value: row.code }, { label: '状态', value: row.status },
          { label: '结算方式', value: Number(row.payType)===1?'月结':Number(row.payType)===0?'次结':`原状态 ${row.payType??''}` }, { label: '地区 ID', value: row.cityId }, { label: '所在地区', value: row.place }, { label: '传真', value: row.fax },
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
