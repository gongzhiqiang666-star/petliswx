const has = (user, role) => Boolean(user?.roles?.includes(role));
const any = (user, roles) => roles.some((role) => has(user, role));

export const canOrderReview = (user) => has(user, 'ADMIN');
export function canRefund(user, order) {
  return any(user, ['ADMIN', 'HOSPITAL_ADMIN', 'DOCTOR'])
    && order?.settlementType === 'CURRENT' && order?.status === 'PENDING_PICKUP'
    && Boolean(order?.paidAt) && !order?.refundedAt;
}
export function canLogisticsAction(action, user) {
  if (action === 'receive') return any(user, ['ADMIN', 'LAB_STAFF']);
  return any(user, ['ADMIN', 'COURIER']);
}
export function canFinanceAction(action, user) {
  if (['generate', 'issue', 'reject', 'overdue'].includes(action)) return has(user, 'ADMIN');
  if (['reconcile', 'settle', 'download'].includes(action)) return any(user, ['ADMIN', 'HOSPITAL_ADMIN']);
  if (action === 'refund') return any(user, ['ADMIN', 'HOSPITAL_ADMIN', 'DOCTOR']);
  return false;
}
export const canSendMessage = (user) => has(user, 'ADMIN');
