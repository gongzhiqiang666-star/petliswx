export function escapeHtml(input) {
  return String(input ?? '').replace(/[&<>"']/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character]);
}

export function centsToYuan(cents) {
  if (cents === null || cents === undefined || cents === '') return '—';
  const amount = BigInt(cents);
  const negative = amount < 0n;
  const absolute = negative ? -amount : amount;
  return `${negative ? '-' : ''}${absolute / 100n}.${String(absolute % 100n).padStart(2, '0')}`;
}

export function yuanToCents(input) {
  const text = String(input ?? '').trim();
  if (!/^-?\d+(?:\.\d{1,2})?$/.test(text)) {
    if (/^-?\d+\.\d{3,}$/.test(text)) throw new Error('金额最多两位小数');
    throw new Error('请输入有效金额');
  }
  const negative = text.startsWith('-');
  const unsigned = negative ? text.slice(1) : text;
  const [yuan, decimal = ''] = unsigned.split('.');
  const cents = BigInt(yuan) * 100n + BigInt(decimal.padEnd(2, '0') || '0');
  const result = negative ? -cents : cents;
  if (result > BigInt(Number.MAX_SAFE_INTEGER) || result < BigInt(Number.MIN_SAFE_INTEGER)) throw new Error('金额超出支持范围');
  return Number(result);
}

export function money(cents) {
  const formatted = centsToYuan(cents);
  return formatted === '—' ? formatted : `¥${formatted}`;
}

export function formatDate(input, timeZone) {
  if (!input) return '—';
  const date = new Date(input);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
    hour12: false, ...(timeZone ? { timeZone } : {})
  }).format(date);
}

export function value(row, key, fallback = '—') {
  if (!row) return fallback;
  const snake = key.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`);
  return row[key] ?? row[snake] ?? fallback;
}

export function cell(row, key, fallback = '—') {
  return escapeHtml(value(row, key, fallback));
}

export function moneyYuan(input) {
  if (input === null || input === undefined || input === '') return '—';
  const text = String(input).trim();
  if (!/^-?\d+(?:\.\d{1,2})?$/.test(text)) return '—';
  const negative = text.startsWith('-');
  const [whole, decimal = ''] = (negative ? text.slice(1) : text).split('.');
  return `${negative ? '-' : ''}¥${whole}.${decimal.padEnd(2, '0')}`;
}

export function isModulePending(error) {
  return error?.status === 404 || error?.code === 404;
}

export function pageItems(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.items)) return data.items;
  return Array.isArray(data?.content) ? data.content : [];
}

export function actionData(data) {
  return encodeURIComponent(JSON.stringify(data));
}

export function decodeActionData(data) {
  return JSON.parse(decodeURIComponent(data));
}

export function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export const statusText = {
  PENDING: '待审核', ACTIVE: '启用', INACTIVE: '停用', REJECTED: '已驳回',
  DRAFT: '草稿', PENDING_REVIEW: '待审核', PENDING_PAYMENT: '待支付', PENDING_PICKUP: '待取件',
  IN_TRANSIT: '运输中', RECEIVED: '已签收', TESTING: '检测中', REPORT_REVIEW: '报告审核中',
  COMPLETED: '已完成', CANCELLED: '已取消', RETURNED: '已退样', REFUNDED: '已退款',
  APPROVED: '已通过', PUBLISHED: '已发布', FAILED: '发送失败', SENT: '已发送',
  PENDING_RECONCILIATION: '待对账', DISPUTED: '有异议', CONFIRMED: '已确认', SETTLED: '已结算',
  OVERDUE: '已逾期', ISSUED: '已开票', REDEEMED: '待发货', SHIPPED: '已发货',
  AVAILABLE: '可使用', USED: '已使用', EXPIRED: '已过期', EXCEPTION: '异常'
};

export function statusLabel(status) {
  return statusText[status] || status || '—';
}
