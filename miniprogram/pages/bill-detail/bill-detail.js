const request = require('../../utils/request')
const { isHospitalAdmin, ensureRoles } = require('../../utils/roles')

Page({
  data: { bill: null, items: [], error: '', forbidden: false, busy: false },
  async onLoad(options) {
    const userInfo = await ensureRoles(getApp().getUserInfo(), () => request.get('/user/roles'))
    getApp().globalData.userInfo = userInfo
    if (!isHospitalAdmin(userInfo)) { this.setData({ forbidden: true }); return }
    this.billId = options.id
    return this.load()
  },
  async load() {
    this.setData({ error: '' })
    try {
      const [bill, result] = await Promise.all([request.get(`/bills/${this.billId}`), request.get(`/bills/${this.billId}/items`, { page: 0, size: 100 })])
      this.setData({ bill, items: result.items || result.content || [] })
    } catch (error) { this.setData({ error: error.message || '账单加载失败' }) }
  },
  confirm() {
    wx.showModal({ title: '确认对账', content: '确认账单周期、明细和金额均无误吗？', success: result => { if (result.confirm) this.reconcile('CONFIRM', '') } })
  },
  dispute() {
    wx.showModal({ title: '提出账单异议', content: '请联系实验室核对异议明细；确认后将记录“明细待核对”状态。', editable: true, placeholderText: '请填写异议原因', success: result => { if (result.confirm && result.content) this.reconcile('DISPUTE', result.content) } })
  },
  async reconcile(action, reason) {
    if (this.data.busy) return
    this.setData({ busy: true })
    try {
      const bill = await request.post(`/bills/${this.billId}/reconciliation`, { action, reason })
      this.setData({ bill })
      wx.showToast({ title: action === 'CONFIRM' ? '已确认对账' : '异议已提交', icon: 'success' })
    } catch (error) { wx.showToast({ title: error.message || '操作失败', icon: 'none' }) } finally { this.setData({ busy: false }) }
  }
})
