import { messagePayload } from '../adapters.js';
import { formData } from './shared.js';
import { formatDate, pageItems, value, cell } from '../utils.js';
import { badge, field, pageHeader, pagination, renderDetail, renderState, renderTable } from '../components/ui.js';
import { canSendMessage } from '../permissions.js';
import { directoryOptions } from '../options.js';

export async function render(container, context) {
  const { api } = context;
  const admin = canSendMessage(context.store.getState().user);
  let filters = { page: 0, size: 20, status: '', unreadOnly: false, sentOnly: false, userId: '' };
  container.innerHTML = `${pageHeader('消息中心', admin ? '查询全部或自己发出的消息，按接收者筛选并重试投递' : '查看接收消息、失败重试与投递日志', admin ? '<button class="button" id="message-send">发送消息</button>' : '')}
    <section class="panel"><form id="message-filter" class="toolbar"><label>状态<select name="status"><option value="">全部</option><option value="SENT">已发送</option><option value="FAILED">失败</option></select></label>${admin ? '<label class="check-field"><input type="checkbox" name="sentOnly" value="true">仅我发出</label><label>接收用户 ID<input name="userId" type="number" min="1"></label>' : ''}<label class="check-field"><input type="checkbox" name="unreadOnly" value="true">仅未读</label><button class="button secondary">查询</button></form><div id="message-list"></div></section>`;
  const list = container.querySelector('#message-list');
  async function load() {
    list.innerHTML = renderState('loading');
    try { const data = await api.request('/messages', { query: filters }); list.innerHTML = renderTable({ rows: pageItems(data), emptyText: '暂无消息', columns: [
      { label: '标题', render: (row) => `<strong>${cell(row, 'title')}</strong><small class="cell-sub">${cell(row, 'messageType')}</small>` },
      { label: '接收用户', render: (row) => cell(row, 'userId') }, { label: '发送人', render: (row) => cell(row, 'senderId') },
      { label: '状态', render: (row) => badge(value(row, 'status', '')) },
      { label: '重试次数', render: (row) => cell(row, 'retryCount') }, { label: '发送时间', render: (row) => formatDate(value(row, 'sentAt', null)) },
      { label: '最近错误', render: (row) => cell(row, 'lastError') }
    ], actions: (row) => `<button class="button text" data-logs="${value(row, 'id')}">日志</button>${value(row, 'status', '') === 'FAILED' ? `<button class="button small" data-retry="${value(row, 'id')}">重试</button>` : ''}<button class="button text" data-read="${value(row, 'id')}">标为已读</button>` }) + pagination(data); }
    catch (error) { list.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error); }
  }
  container.querySelector('#message-send')?.addEventListener('click', async () => {
    try {
      const users = pageItems(await api.request('/admin/users', { query: { status: 'ACTIVE' } }));
      context.dialog({ title: '发送业务消息', wide: true, submitLabel: '确认发送', body: `<p class="form-hint">微信渠道在未配置服务商时会真实记录失败，不会伪造送达。</p><div class="form-grid">${field({ name: 'userId', label: '接收用户', options: directoryOptions(users), required: true })}${field({ name: 'channel', label: '渠道', options: [{ value: 'IN_APP', label: '站内消息' }, { value: 'WECHAT', label: '微信消息' }] })}${field({ name: 'messageType', label: '类型', options: [{ value: 'BUSINESS', label: '业务消息' }, { value: 'SECURITY', label: '安全消息' }] })}${field({ name: 'title', label: '标题', required: true, wide: true })}${field({ name: 'content', label: '正文', type: 'textarea', required: true, wide: true })}</div>`, async onSubmit(form) {
        const result = await api.request('/messages', { method: 'POST', data: messagePayload(formData(form)) }); context.toast(value(result, 'status', '') === 'FAILED' ? '消息已记录，但投递失败，请在列表重试' : '消息已发送', value(result, 'status', '') === 'FAILED' ? 'error' : 'success'); load();
      } });
    } catch (error) { context.toast(error.message, 'error'); }
  });
  container.querySelector('#message-filter').addEventListener('submit', (event) => { event.preventDefault(); const data = formData(event.currentTarget); filters = { page: 0, size: 20, status: data.status, unreadOnly: data.unreadOnly === 'true', sentOnly: data.sentOnly === 'true', userId: data.userId || '' }; load(); });
  container.addEventListener('click', async (event) => {
    const page = event.target.closest('[data-page]'); if (page && !page.disabled) { filters.page = Number(page.dataset.page); load(); }
    const retry = event.target.closest('[data-retry]'); if (retry) { try { const result = await api.request(`/messages/${retry.dataset.retry}/retry`, { method: 'POST' }); context.toast(value(result, 'status', '') === 'SENT' ? '重试发送成功' : '重试仍失败，已记录原因', value(result, 'status', '') === 'SENT' ? 'success' : 'error'); load(); } catch (error) { context.toast(error.message, 'error'); } }
    const read = event.target.closest('[data-read]'); if (read) { try { await api.request(`/messages/${read.dataset.read}/read`, { method: 'PATCH' }); context.toast('已标为已读'); load(); } catch (error) { context.toast(error.message, 'error'); } }
    const logs = event.target.closest('[data-logs]'); if (logs) {
      try { const [message, data] = await Promise.all([api.request(`/messages/${logs.dataset.logs}`), api.request(`/messages/${logs.dataset.logs}/logs`, { query: { size: 100 } })]); context.dialog({ title: `消息 #${logs.dataset.logs} 投递日志`, wide: true, body: `${renderDetail([{ label: '标题', value: value(message, 'title') }, { label: '状态', value: value(message, 'status') }, { label: '内容', value: value(message, 'content'), wide: true }, { label: '最近错误', value: value(message, 'lastError'), wide: true }])}${renderTable({ rows: pageItems(data), columns: [{ label: '渠道', render: (row) => cell(row, 'channel') }, { label: '状态', render: (row) => badge(value(row, 'status', '')) }, { label: '尝试次数', render: (row) => cell(row, 'attemptNo') }, { label: '服务商消息 ID', render: (row) => cell(row, 'providerMessageId') }, { label: '错误', render: (row) => cell(row, 'errorMessage') }, { label: '时间', render: (row) => formatDate(value(row, 'sentAt', null)) }] })}` }); } catch (error) { context.toast(error.message, 'error'); }
    }
  });
  await load();
}
