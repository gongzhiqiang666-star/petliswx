const statusLabels = {
  PENDING_RECONCILIATION: '待对账',
  DISPUTED: '有异议',
  RECONCILED: '已对账',
  SETTLED: '已结算',
  OVERDUE: '已逾期',
  CANCELLED: '已取消'
}
const statusClasses = {
  PENDING_RECONCILIATION: 'warning',
  DISPUTED: 'error',
  RECONCILED: 'success',
  SETTLED: 'success',
  OVERDUE: 'error',
  CANCELLED: 'neutral'
}

function adaptBill(bill) {
  const status = bill.status || ''
  return {
    ...bill,
    statusLabel: statusLabels[status] || '未知状态',
    statusClass: statusClasses[status] || 'neutral',
    reconcilable: status === 'PENDING_RECONCILIATION' || status === 'DISPUTED'
  }
}

module.exports = { adaptBill, statusLabels }
