export const pageMeta = {
  orders: { endpoint: '/orders', actions: ['review', 'refund'] },
  logistics: { endpoint: '/logistics/orders', actions: ['pickup', 'exception', 'receive'] },
  laboratory: { endpoint: '/lab/tasks', actions: ['assign', 'results', 'return', 'retest'] },
  reports: { endpoint: '/reports', actions: ['create', 'review', 'publish', 'download'] },
  finance: { endpoint: '/bills', actions: ['generate', 'reconcile', 'settle', 'invoice', 'refund'] }
};

export function buildQuery(query) {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params.set(key, value);
  });
  const text = params.toString();
  return text ? `?${text}` : '';
}

export function actionFeedback(error, successMessage) {
  if (!error) return { type: 'success', message: successMessage };
  if (error.status === 403) return { type: 'forbidden', message: '没有权限执行此操作，请联系平台管理员' };
  return { type: 'error', message: error.message || '操作失败，请稍后重试' };
}

export function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

export function confirmDanger(message) {
  return globalThis.confirm(message);
}
