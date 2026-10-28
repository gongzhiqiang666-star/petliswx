import { escapeHtml, isModulePending, statusLabel } from '../utils.js';

export function renderTable({ columns, rows = [], actions, emptyText = '暂无数据' }) {
  if (!rows.length) return `<div class="empty-state"><span class="empty-icon">▤</span><p>${escapeHtml(emptyText)}</p></div>`;
  return `<div class="table-wrap"><table><thead><tr>${columns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join('')}${actions ? '<th class="actions-column">操作</th>' : ''}</tr></thead><tbody>${rows.map((row) => `<tr>${columns.map((column) => `<td>${column.render ? column.render(row) : escapeHtml(row[column.key] ?? '—')}</td>`).join('')}${actions ? `<td><div class="table-actions">${actions(row)}</div></td>` : ''}</tr>`).join('')}</tbody></table></div>`;
}

export function renderState(type, error = {}) {
  if (type === 'loading') return '<div class="state loading-state" role="status"><span class="spinner"></span><p>正在加载数据…</p></div>';
  if (type === 'forbidden') return '<div class="state error-state"><strong>没有权限</strong><p>当前账号无权查看此内容，如需访问请联系平台管理员。</p></div>';
  if (type === 'pending' || type === 'error' && isModulePending(error)) return '<div class="state empty-state"><strong>模块待接入</strong><p>该模块将在后续合并任务中接入，当前不展示模拟数据。</p></div>';
  return `<div class="state error-state" role="alert"><strong>加载失败</strong><p>${escapeHtml(error.message || '请稍后重试')}</p><button class="button secondary" data-action="retry">重新加载</button></div>`;
}

export function renderDetail(fields) {
  const items = fields.map((field) => {
    const content = field.html ? field.value : escapeHtml(field.value ?? '—');
    return `<div class="detail-item ${field.wide ? 'wide' : ''}"><dt>${escapeHtml(field.label)}</dt><dd>${content}</dd></div>`;
  }).join('');
  return `<dl class="detail-grid">${items}</dl>`;
}

export function modal({ title, body, footer = '', wide = false }) {
  return `<div class="modal-backdrop" data-action="close-modal"><section class="modal ${wide ? 'modal-wide' : ''}" role="dialog" aria-modal="true" aria-label="${escapeHtml(title)}" data-modal-panel><header class="modal-header"><h2>${escapeHtml(title)}</h2><button class="icon-button" data-action="close-modal" aria-label="关闭">×</button></header><div class="modal-body">${body}</div>${footer ? `<footer class="modal-footer">${footer}</footer>` : ''}</section></div>`;
}

export function pagination({ page = 0, size = 20, total = 0 }) {
  const pages = Math.max(1, Math.ceil(total / size));
  return `<div class="pagination"><span>共 ${Number(total)} 条</span><div><button class="button text" data-page="${page - 1}" ${page <= 0 ? 'disabled' : ''}>上一页</button><span>${page + 1} / ${pages}</span><button class="button text" data-page="${page + 1}" ${page + 1 >= pages ? 'disabled' : ''}>下一页</button></div></div>`;
}

export function badge(status) {
  const normalized = String(status || 'UNKNOWN').toLowerCase().replaceAll('_', '-');
  return `<span class="badge status-${escapeHtml(normalized)}">${escapeHtml(statusLabel(status))}</span>`;
}

export function pageHeader(title, description, actions = '') {
  return `<header class="page-header"><div><h1>${escapeHtml(title)}</h1><p>${escapeHtml(description)}</p></div>${actions ? `<div class="page-actions">${actions}</div>` : ''}</header>`;
}

export function field({ name, label, type = 'text', value = '', required = false, options, placeholder = '', min, step, wide = false }) {
  const attrs = `id="field-${escapeHtml(name)}" name="${escapeHtml(name)}" ${required ? 'required' : ''} ${min !== undefined ? `min="${escapeHtml(min)}"` : ''} ${step ? `step="${escapeHtml(step)}"` : ''}`;
  let control;
  if (options) control = `<select ${attrs}>${options.map((option) => `<option value="${escapeHtml(option.value)}" ${String(option.value) === String(value) ? 'selected' : ''}>${escapeHtml(option.label)}</option>`).join('')}</select>`;
  else if (type === 'textarea') control = `<textarea ${attrs} placeholder="${escapeHtml(placeholder)}">${escapeHtml(value)}</textarea>`;
  else control = `<input type="${escapeHtml(type)}" ${attrs} value="${escapeHtml(value)}" placeholder="${escapeHtml(placeholder)}">`;
  return `<label class="form-field ${wide ? 'wide' : ''}"><span>${escapeHtml(label)}${required ? '<em>*</em>' : ''}</span>${control}</label>`;
}

export function toast(message, type = 'success') {
  return `<div class="toast toast-${escapeHtml(type)}" role="status">${escapeHtml(message)}</div>`;
}
