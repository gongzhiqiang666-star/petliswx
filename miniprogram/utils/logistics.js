const statusLabels = {
  PENDING_PICKUP: '待揽收', ASSIGNED: '已分配', PICKED_UP: '已揽收',
  IN_TRANSIT: '运输中', DELIVERED: '已送达', RECEIVED: '实验室已签收', EXCEPTION: '物流异常', EXCEPTION_RESOLVED:'异常已处理', CANCELLED: '寄件已取消'
}
const actionLabels = {
  PENDING_PICKUP: '运单已创建', ASSIGNED: '运单已分配', PICKED_UP: '已揽收',
  IN_TRANSIT: '运输中', DELIVERED: '已送达', RECEIVED: '实验室已签收', EXCEPTION: '物流异常', EXCEPTION_RESOLVED:'异常已处理', CANCELLED: '寄件已取消'
}

const builtinReasons={PICKUP:'配送员已揽收',TRANSIT:'开始运输',DELIVER:'已送达',RECEIVE:'实验室已签收'}

function adaptShipment(raw) {
  if (!raw) return null
  return {
    ...raw,
    statusLabel: statusLabels[raw.status] || raw.status || '物流待更新',
    expectedArrivedAt: raw.expectedArrivedAt ? String(raw.expectedArrivedAt).replace('T', ' ').slice(0, 16) : '',
    exceptionTypeLabel: ({DAMAGE:'标本破损',LOSS:'标本丢失',DELAY:'超时未送达',OTHER:'其他异常'})[raw.exceptionType] || raw.exceptionType || '',
    exceptionNote: raw.exceptionNote || '',
    exceptions: (raw.exceptions || []).map(event => ({...event,typeLabel:({DAMAGE:'标本破损',LOSS:'标本丢失',DELAY:'超时未送达',OTHER:'其他异常'})[event.exceptionType] || event.exceptionType || '其他异常',stateLabel:event.resolvedAt?'已处理':'待处理',reportedTime:event.reportedAt?String(event.reportedAt).replace('T',' ').slice(0,16):'',resolvedTime:event.resolvedAt?String(event.resolvedAt).replace('T',' ').slice(0,16):''})),
    timeline: (raw.timeline || []).map(item => ({
      ...item,
      title: builtinReasons[item.reason] || item.reason || actionLabels[item.action] || item.action,
      time: item.createdAt ? String(item.createdAt).replace('T', ' ').slice(0, 16) : '',
      abnormal: item.action === 'EXCEPTION'
    }))
  }
}

function adaptListPage(raw) {
  const items = (raw && (raw.items || raw.content)) || []
  return items.map(adaptShipment)
}

module.exports = { adaptShipment, adaptListPage, statusLabels }
