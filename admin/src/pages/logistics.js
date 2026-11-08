import {reshipEntry,openSfReship} from '../components/sf-reship.js';
import { formData } from './shared.js';
import { escapeHtml, formatDate, pageItems, statusLabel, idempotencyKey } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { canLogisticsAction } from '../permissions.js';

export async function render(container, context) {
  const { api, store } = context;
  const user = store.getState().user;
  let filters = { status: '', page: 0, size: 20 };
  const create = canLogisticsAction('create', user) ? '<button class="button secondary" id="sf-status">查询顺丰请求</button><button class="button" id="sf-create">顺丰寄件（沙箱）</button><button class="button secondary" id="shipment-create">录入已有运单</button>' : '';
  container.innerHTML = `${pageHeader('物流配送', '运单分配、取件运输、异常与实验室签收全程留痕', create)}<section class="panel"><form id="shipment-filter" class="toolbar"><label>物流状态<select name="status"><option value="">全部</option>${['PENDING_PICKUP','PICKED_UP','IN_TRANSIT','DELIVERED','RECEIVED','CANCELLED'].map((status) => `<option value="${status}">${statusLabel(status)}</option>`).join('')}</select></label><button class="button secondary">查询</button></form><div id="shipment-list"></div></section>`;
  const list = container.querySelector('#shipment-list');
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const data = await api.request('/logistics', { query: filters });
      list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无运单', columns: [
        { label: '运单', render: (row) => `<strong>#${row.id} · 第 ${escapeHtml(row.attemptNo||1)} 次</strong><small class="cell-sub">${escapeHtml(row.trackingNo || '')}${row.sandbox?' · 沙箱':''}</small>` },
        { label: '订单', key: 'orderId' }, { label: '实验室', key: 'laboratoryId' }, { label: '快递员', key: 'courierUserId' },
        { label: '状态', render: (row) => badge(row.status) }, { label: '预计到达', render: (row) => formatDate(row.expectedArrivedAt) }
      ], actions: (row) => actionButtons(row, user) }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#sf-create')?.addEventListener('click', () => openSfCreate(context, load));
  container.querySelector('#sf-status')?.addEventListener('click', () => openSfStatus(context, load));
  container.querySelector('#shipment-create')?.addEventListener('click', () => openCreate(context, load));
  container.querySelector('#shipment-filter').addEventListener('submit', (event) => { event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load(); });
  container.addEventListener('click', async (event) => {
    const action = event.target.closest('[data-action]');
    const page = event.target.closest('[data-page]');
    if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); return; }
    if (!action) return;
    const id = action.dataset.id;
    try {
      if(action.dataset.action==='sf-label') {
        const label=await api.request(`/logistics/sf/orders/${action.dataset.orderId}/label`,{method:'POST'});
        const file=await api.download(label.url);const url=URL.createObjectURL(file.blob);
        const link=document.createElement('a');link.href=url;link.download=file.filename;link.click();URL.revokeObjectURL(url);return;
      }
      if (action.dataset.action === 'sf-cancel') return openSfCancel(context,action.dataset.orderId,load);
      if (action.dataset.action === 'sf-routes') {
        await api.request(`/logistics/sf/orders/${action.dataset.orderId}/routes/refresh`,{method:'POST'});context.toast('已查询顺丰路由');load();return;
      }
      if (action.dataset.action === 'assign') return openAssign(context, id, load);
      if (action.dataset.action === 'exception') return openException(context, id, load);
      if(action.dataset.action==='history')return showShipmentHistory(context,action.dataset.orderId);
      if (action.dataset.action === 'detail') return showDetail(context, id, load);
      await api.request(`/logistics/${id}/${action.dataset.action}`, { method: 'POST' });
      context.toast('物流状态已更新'); load();
    } catch (error) { context.toast(error.message, 'error'); }
  });
  await load();
}

function actionButtons(row, user) {
  const buttons = [];
  if(row.sandbox && row.status!=='CANCELLED' && canLogisticsAction('create',user)) buttons.push(`<button class="button text" data-action="sf-routes" data-order-id="${row.orderId}" data-id="${row.id}">同步顺丰路由</button>`);
  if (canLogisticsAction('assign', user) && row.status === 'PENDING_PICKUP') buttons.push(`<button class="button text" data-action="assign" data-id="${row.id}">分配</button>`);
  if(row.sandbox && row.status==='PENDING_PICKUP' && canLogisticsAction('create',user)) buttons.push(`<button class="button text" data-action="sf-cancel" data-order-id="${row.orderId}" data-id="${row.id}">取消寄件</button>`);
  if(row.sandbox && row.status!=='CANCELLED' && canLogisticsAction('create',user)) buttons.push(`<button class="button text" data-action="sf-label" data-order-id="${row.orderId}" data-id="${row.id}">面单 PDF</button>`);
  const transitions = { PENDING_PICKUP: ['pickup', '取件'], PICKED_UP: ['transit', '开始运输'], IN_TRANSIT: ['deliver', '送达'] };
  if (row.status === 'DELIVERED' && canLogisticsAction('receive', user)) buttons.push(`<button class="button text" data-action="receive" data-id="${row.id}">签收</button>`);
  const transition = transitions[row.status];
  if (transition && canLogisticsAction(transition[0], user)) buttons.push(`<button class="button text" data-action="${transition[0]}" data-id="${row.id}">${transition[1]}</button>`);
  if (!['RECEIVED','CANCELLED'].includes(row.status) && canLogisticsAction('exception', user)) buttons.push(`<button class="button text" data-action="exception" data-id="${row.id}">异常</button>`);
  buttons.push(`<button class="button text" data-action="history" data-order-id="${row.orderId}" data-id="${row.id}">寄件历史</button>`);
  buttons.push(`<button class="button text" data-action="detail" data-id="${row.id}">时间线</button>`);
  return buttons.join('');
}

function openCreate(context, done) {
  context.dialog({ title: '创建运单', submitLabel: '创建', body: `<div class="form-grid">${field({ name: 'orderId', label: '送检单 ID', type: 'number', required: true })}${field({ name: 'laboratoryId', label: '实验室 ID', type: 'number', required: true })}${field({ name: 'trackingNo', label: '运单号', required: true })}${field({ name: 'courierUserId', label: '快递员用户 ID', type: 'number' })}${field({ name: 'pickupAddress', label: '揽收地址', required: true, wide: true })}${field({ name: 'expectedArrivedAt', label: '预计到达时间', placeholder: '2026-10-28T10:00:00', wide: true })}</div>`, async onSubmit(form) {
    const value = formData(form); await context.api.request('/logistics', { method: 'POST', data: { orderId: Number(value.orderId), laboratoryId: Number(value.laboratoryId), courierUserId: value.courierUserId ? Number(value.courierUserId) : null, trackingNo: value.trackingNo.trim(), pickupAddress: value.pickupAddress.trim(), expectedArrivedAt: value.expectedArrivedAt || null } }); context.toast('运单已创建'); done();
  } });
}
function openAssign(context, id, done) {
  context.dialog({ title: `分配运单 #${id}`, submitLabel: '分配', body: field({ name: 'courierUserId', label: '快递员用户 ID', type: 'number', required: true }), async onSubmit(form) {
    await context.api.request(`/logistics/${id}/assign`, { method: 'POST', data: { courierUserId: Number(formData(form).courierUserId) } }); context.toast('运单已分配'); done();
  } });
}
function openException(context, id, done) {
  const requestKey=idempotencyKey();
  context.dialog({ title: `登记运单 #${id} 异常`, submitLabel: '保存异常', body: `<div class="form-grid">${field({ name: 'exceptionType', label: '异常类型', options: [{value:'DAMAGE',label:'标本破损'},{value:'LOSS',label:'标本丢失'},{value:'DELAY',label:'超时未送达'},{value:'OTHER',label:'其他异常'}], required: true })}${field({ name: 'note', label: '异常说明', type: 'textarea', required: true, wide: true })}</div>`, async onSubmit(form) {
    const value = formData(form); await context.api.request(`/logistics/${id}/exception`, { method: 'POST', data: { exceptionType: value.exceptionType.trim(), note: value.note.trim(), requestKey } }); context.toast('异常已记录'); done();
  } });
}
async function showDetail(context, id, done = () => {}) {
  try {
    const row = await context.api.request(`/logistics/${id}`);
    const labels={DAMAGE:'标本破损',LOSS:'标本丢失',DELAY:'超时未送达',OTHER:'其他异常'};
    const resolutionActions=row.current!==false && (row.exceptions || []).some(event=>!event.resolvedAt) && canLogisticsAction('resolveException',context.store.getState().user) ? event=>!event.resolvedAt?`<button class="button text" data-resolve-exception="${Number(event.id)}">记录处理结果</button>`:'' : undefined;
    context.dialog({ title: `运单 #${id} 时间线`, wide: true, body: `${renderDetail([{ label: '当前状态', value: statusLabel(row.status) }, { label: '当前异常', value: row.exceptionNote }])}<h3>异常处理记录</h3>${renderTable({rows:row.exceptions || [],emptyText:'暂无异常',columns:[{label:'类型',render:event=>escapeHtml(labels[event.exceptionType] || event.exceptionType)},{label:'原因',key:'note'},{label:'报告时间',render:event=>formatDate(event.reportedAt)},{label:'处理状态',render:event=>event.resolvedAt?'已处理':'待处理'},{label:'处理说明',key:'resolutionNote'},{label:'处理时间',render:event=>formatDate(event.resolvedAt)}],actions:resolutionActions})}<h3>物流时间线</h3>${renderTable({ rows: row.timeline || [], columns: [{ label: '节点', render: item => escapeHtml(({ASSIGNED:'已分配配送员',EXCEPTION:'物流异常',EXCEPTION_RESOLVED:'异常已处理'})[item.action] || statusLabel(item.action)) }, { label: '说明', render: item => escapeHtml(({PICKUP:'配送员已取件',TRANSIT:'开始运输',DELIVER:'已送达',RECEIVE:'实验室已签收'})[item.reason] || item.reason) }, { label: '操作时间', render: (item) => formatDate(item.createdAt) }] })}`,
      onReady(root) { root.querySelectorAll('[data-resolve-exception]').forEach(button=>button.addEventListener('click',()=>{
        context.dialog({title:'记录异常处理结果',submitLabel:'确认已处理',body:'<p>请核实处理结果并填写说明。其他未处理异常会继续保留，运输状态按实际进度操作。</p>'+field({name:'reason',label:'处理说明',type:'textarea',required:true,wide:true}),async onSubmit(form){
          const reason=formData(form).reason.trim();if(!reason || reason.length>1000)throw new Error('请填写1000字以内的处理说明');
          await context.api.request(`/logistics/${id}/exceptions/${button.dataset.resolveException}/resolve`,{method:'POST',data:{reason}});
          context.toast('异常处理结果已记录');await done();
        }});
      })); }
    });
  } catch (error) { context.toast(error.message, 'error'); }
}

function openSfCreate(context, done) {
  const contactFields = (prefix, title) => `<h3 class="wide">${title}</h3>${[
    ['contact','联系人'],['mobile','手机号'],['province','省'],['city','市'],['county','区县'],['address','详细地址']
  ].map(([name,label]) => field({name: `${prefix}_${name}`,label,required:true,wide:name==='address'})).join('')}`;
  context.dialog({title:'顺丰寄件（沙箱）',submitLabel:'提交沙箱寄件',wide:true,body:`<p>仅生成测试运单，不预约真实取件。送检单需审核通过并已支付或月结挂账。</p><div class="form-grid">${field({name:'orderId',label:'送检单 ID',type:'number',required:true})}${contactFields('sender','寄件信息')}${contactFields('recipient','收件实验室')}</div>`,async onSubmit(form) {
    const values=formData(form);
    const read=(prefix)=>Object.fromEntries(['contact','mobile','province','city','county','address'].map(key=>[key,values[`${prefix}_${key}`].trim()]));
    const result=await context.api.request(`/logistics/sf/orders/${Number(values.orderId)}`,{method:'POST',data:{sender:read('sender'),recipient:read('recipient')}});
    context.toast(result.shipmentId ? `测试运单已创建：${result.trackingNo}` : '请求已记录，请查询顺丰请求状态');done();
  }});
}
function openSfStatus(context, done) {
  context.dialog({title:'查询顺丰请求',submitLabel:'查询',body:field({name:'orderId',label:'送检单 ID',type:'number',required:true}),async onSubmit(form) {
    const orderId=Number(formData(form).orderId);
    const result=await context.api.request(`/logistics/sf/orders/${orderId}`);
    let reshipOptions={enabled:false};try{reshipOptions=await context.api.request(`/logistics/sf/orders/${orderId}/reship-options`)}catch(error){/* Request status and recovery remain available if conditions cannot be loaded. */}
    const names={NOT_CREATED:'尚未寄件',RESERVED:'等待发送',SENDING:'正在确认寄件结果',UNKNOWN:'寄件结果待确认',SUCCEEDED:'寄件成功',REJECTED:'承运商未受理'};
    const recoverCancellation = !!result.shipmentId && result.cancellationState === 'SUCCEEDED';
    const recoverCreate = !result.shipmentId && ['SENDING','UNKNOWN','SUCCEEDED'].includes(result.state);
    // Open after the lookup dialog has closed; no create is ever repeated by this action.
    setTimeout(()=>context.dialog({title:`顺丰请求 · 送检单 #${orderId}`,submitLabel:recoverCancellation?'核对并恢复本地取消':(recoverCreate?'查单并恢复运单':undefined),
      body:renderDetail([{label:'状态',value:result.cancellationState==='SUCCEEDED'?'寄件已取消':(['RESERVED','SENDING','UNKNOWN'].includes(result.cancellationState)?'取消结果待确认':names[result.state]||result.state)},{label:'测试运单号',value:result.trackingNo||'—'},{label:'环境',value:'顺丰沙箱（不会真实取件）'},{label:'取消状态',value:({SUCCEEDED:'顺丰已确认取消',REJECTED:'顺丰未受理取消',SENDING:'取消结果待确认',UNKNOWN:'取消结果待确认',RESERVED:'取消请求已记录'})[result.cancellationState]||'未申请取消'}])+reshipEntry(reshipOptions),
      onReady(form){form.querySelector('[data-sf-reship]')?.addEventListener('click',()=>openSfReship(context,orderId,reshipOptions,done));},
      ...((recoverCreate || recoverCancellation)?{async onSubmit(){await context.api.request(`/logistics/sf/orders/${orderId}/reconcile`,{method:'POST'});context.toast(recoverCancellation?'已核对并恢复本地取消状态':'已查询顺丰结果');done();}}:{})}),0);
  }});
}

function openSfCancel(context,orderId,done) {
  context.dialog({title:'取消顺丰寄件（沙箱）',submitLabel:'确认取消寄件',body:'<p>仅取消当前待揽收运单，送检单和检测资料继续保留。承运商确认后才更新为已取消；结果待确认时请保留当前请求。</p>',async onSubmit(){
    const result=await context.api.request(`/logistics/sf/orders/${orderId}/cancel`,{method:'POST'});
    context.toast(result.cancellationState==='SUCCEEDED'?'顺丰已确认取消寄件':'取消结果待确认，请查询顺丰请求');done();
  }});
}

async function showShipmentHistory(context,orderId){const rows=await context.api.request(`/logistics/orders/${orderId}/history`);context.dialog({title:`送检单 #${orderId} 寄件历史`,wide:true,body:renderTable({rows,columns:[{label:'寄件次数',render:r=>'第 '+escapeHtml(r.attemptNo||1)+' 次'},{label:'版本',render:r=>r.current?'当前运单':'历史运单'},{label:'运单号',key:'trackingNo'},{label:'状态',render:r=>badge(r.status)},{label:'创建时间',render:r=>formatDate(r.createdAt)},{label:'进度记录',render:r=>(r.timeline||[]).map(n=>escapeHtml(formatDate(n.createdAt))+' '+escapeHtml(n.reason||statusLabel(n.action))).join('<br>')}]})});}
