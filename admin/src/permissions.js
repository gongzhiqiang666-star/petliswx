const has = (user, role) => Boolean(user?.roles?.includes(role));
const any = (user, roles) => roles.some((role) => has(user, role));

export const canOrderReview = (user) => has(user, 'ADMIN');
export function canRefund(user, order) {
  return any(user, ['ADMIN', 'HOSPITAL_ADMIN', 'DOCTOR'])
    && order?.settlementType === 'CURRENT' && order?.status === 'PENDING_PICKUP'
    && Boolean(order?.paidAt) && !order?.refundedAt;
}
export function canLogisticsAction(action, user) {
  if (['create', 'assign'].includes(action)) return has(user, 'ADMIN');
  if (action === 'receive') return has(user, 'LAB_STAFF');
  if (['pickup', 'transit', 'deliver', 'exception'].includes(action)) return has(user, 'COURIER');
  return false;
}
export function canLabAction(action, user) {
  return ['assign', 'results', 'return', 'retest'].includes(action) && any(user, ['ADMIN', 'LAB_STAFF']);
}
export function canReportAction(action, user) {
  if (['create', 'review', 'publish', 'template'].includes(action)) return any(user, ['ADMIN', 'LAB_STAFF']);
  if (action === 'download') return any(user, ['ADMIN', 'LAB_STAFF', 'HOSPITAL_ADMIN', 'DOCTOR']);
  return false;
}
export function canFinanceAction(action, user) {
  if (['generate', 'issue', 'reject', 'overdue'].includes(action)) return has(user, 'ADMIN');
  if (['reconcile', 'settle', 'download'].includes(action)) return any(user, ['ADMIN', 'HOSPITAL_ADMIN']);
  if (action === 'refund') return any(user, ['ADMIN', 'HOSPITAL_ADMIN', 'DOCTOR']);
  return false;
}
export const canSendMessage = (user) => has(user, 'ADMIN');
