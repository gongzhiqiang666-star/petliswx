import {openSampleNotice} from '../components/legacy-sample-notice.js';
import {openLegacyMainOrder} from '../components/legacy-main-order.js';
import { openLegacyOrderDetails } from '../components/legacy-order-details.js';
import { formData } from './shared.js';
import { cell, formatDate, moneyYuan, pageItems, value } from '../utils.js';
import { badge, field, pageHeader, pagination, renderState, renderTable } from '../components/ui.js';

export async function render(container, context) {
  const { api, toast } = context;
  let sampleNotice=false;try{sampleNotice=(await api.request('/admin/legacy/sample-notices/capabilities')).sampleNotice===true}catch(_){}
  let detailEnabled = false;
  try { detailEnabled = (await api.request('/admin/legacy/order-details/capabilities')).orderDetails === true; } catch (error) { if (error.status !== 403) toast(error.message, 'error'); }
  let mainOrderWrites=false,mainOrders=false,mainOrderCreates=false;
  if(context.store?.getState().user?.roles?.includes('ADMIN'))try{const cap=await api.request('/admin/legacy/main-orders/capabilities');mainOrderWrites=cap.mainOrderWrites===true;mainOrders=cap.mainOrders===true;mainOrderCreates=cap.mainOrderCreates===true}catch(_){}
  let filters = { page: 0, size: 20, status: '' };
  container.innerHTML = `${pageHeader('送检管理', '查询统一旧订单主表并打印送检单与可扫描标本标签',mainOrderCreates?'<button type="button" class="button" data-main-order-new>新增原后台主单</button>':'')}
    <section class="panel"><form id="order-filter" class="toolbar">${field({ name: 'status', label: '订单状态', options: [{ value: '', label: '全部' }, { value: '0', label: '待审核' }, { value: '-3', label: '待支付' }, { value: '2', label: '等待报告' }, { value: '3', label: '已完成' }, { value: '4', label: '已取消' }] })}<button class="button secondary">查询</button></form><div id="order-list"></div></section>`;
  const list = container.querySelector('#order-list');
  async function load() {
    list.innerHTML = renderState('loading');
    try {
      const data = mainOrders?await api.request('/admin/legacy/main-orders',{query:filters}):await api.request('/admin/orders', { query: filters });
      list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无送检订单', columns: [
        { label: '订单', render: (row) => `<strong>${cell(row, 'orderNo')}</strong><small class="cell-sub">旧 ID ${cell(row, 'orderId')}</small>` },
        { label: '医院', render: (row) => cell(row, 'hospitalName') }, { label: '宠物', render: (row) => cell(row, 'animalName') },
        { label: '金额（元）', render: (row) => moneyYuan(row.amount) }, { label: '结算', render: (row) => value(row, 'settlementMode', 'cash') === 'monthly' ? '月结' : '现结' },
        { label: '状态', render: (row) => badge(value(row, 'status', ''))+(Number(row.refunded)?'<small class="cell-sub">已退款（开发模拟）</small>':'') }, { label: '创建时间', render: (row) => formatDate(value(row, 'createdAt', null)) }
      ], actions: (row) => `${sampleNotice?`<button class="button text" data-sample-notice="${Number(row.orderId)}">收样通知</button>`:''}${mainOrderWrites?`<button class="button text" data-main-order-edit="${Number(row.orderId)}">编辑主单</button>`:''}${detailEnabled?`<button class="button text" data-order-details="${Number(row.orderId)}">检测明细</button>`:''}<button class="button text" data-print-order="${row.orderId}">送检单</button><button class="button text" data-print-label="${row.orderId}">标本标签</button>` }) + pagination(data);
    } catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#order-filter').addEventListener('submit', (event) => { event.preventDefault(); filters = { ...formData(event.currentTarget), page: 0, size: 20 }; load(); });
  container.addEventListener('click', async (event) => {
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const create=event.target.closest('[data-main-order-new]');if(create){const requestKey=crypto.randomUUID();let sent=null;context.dialog({title:'新增原后台主单',submitLabel:'创建并填写资料',body:field({name:'autoLogistics',label:'自动添加运费模板（项目246）',value:'false',options:[{value:'false',label:'不添加'},{value:'true',label:'添加'}]}),async onSubmit(form){const command={autoLogistics:formData(form).autoLogistics==='true',requestKey};const canonical=JSON.stringify(command);if(sent&&sent!==canonical)throw Error('上次结果未确认，请先用原选项重试');sent=canonical;const id=await api.request('/admin/legacy/main-orders',{method:'POST',data:command});if(form.isConnected)await openLegacyMainOrder(context,id,load);}});return;}
    const notice=event.target.closest('[data-sample-notice]');if(notice&&!notice.disabled){notice.disabled=true;try{await openSampleNotice(context,Number(notice.dataset.sampleNotice))}catch(error){toast(error.message,'error')}finally{notice.disabled=false;}return;}
    const main=event.target.closest('[data-main-order-edit]');if(main){try{await openLegacyMainOrder(context,Number(main.dataset.mainOrderEdit),load)}catch(error){toast(error.message,'error')}return;}
    const detail = event.target.closest('[data-order-details]');
    if (detail && !detail.disabled) { detail.disabled = true; try { await openLegacyOrderDetails(context, Number(detail.dataset.orderDetails)); } catch (error) { toast(error.message, 'error'); } finally { detail.disabled = false; } }
    const order = event.target.closest('[data-print-order]'); const label = event.target.closest('[data-print-label]');
    if (order) printHtml(api, `/admin/print/orders/${order.dataset.printOrder}`, toast);
    if (label) printHtml(api, `/admin/print/specimens/${label.dataset.printLabel}`, toast);
  });
  await load();
}

export async function printHtml(api, path, toast) {
  try {
    const file = await api.download(path); const url = URL.createObjectURL(file.blob); const frame = document.createElement('iframe');
    frame.style.display = 'none'; frame.src = url; frame.onload = () => { frame.contentWindow.print(); setTimeout(() => { URL.revokeObjectURL(url); frame.remove(); }, 1000); }; document.body.append(frame);
  } catch (error) { toast(error.message, 'error'); }
}
