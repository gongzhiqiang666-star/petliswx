const statusLabels = {
  PENDING_PICKUP: '待揽收', ASSIGNED: '已分配', PICKED_UP: '已揽收',
  IN_TRANSIT: '运输中', DELIVERED: '已送达', RECEIVED: '实验室已签收', EXCEPTION: '物流异常'
}
const actionLabels = {
  PENDING_PICKUP: '运单已创建', ASSIGNED: '运单已分配', PICKED_UP: '已揽收',
  IN_TRANSIT: '运输中', DELIVERED: '已送达', RECEIVED: '实验室已签收', EXCEPTION: '物流异常'
}

function adaptShipment(raw) {
  if (!raw) return null
  return {
    ...raw,
    statusLabel: statusLabels[raw.status] || raw.status || '物流待更新',
    expectedArrivedAt: raw.expectedArrivedAt ? String(raw.expectedArrivedAt).replace('T', ' ').slice(0, 16) : '',
    exceptionNote: raw.exceptionNote || '',
    timeline: (raw.timeline || []).map(item => ({
      ...item,
      title: item.reason || actionLabels[item.action] || item.action,
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
