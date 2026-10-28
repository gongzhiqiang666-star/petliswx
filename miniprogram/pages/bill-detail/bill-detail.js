const request = require('../../utils/request')
const { isHospitalAdmin, ensureRoles } = require('../../utils/roles')
const { adaptBill } = require('../../utils/bills')

Page({
  data: { bill: null, items: [], error: '', forbidden: false, busy: false, loadingItems: false, page: 0, size: 100, total: 0, hasMore: false, canReconcile: false },
  async onLoad(options) {
    try {
      const userInfo = await ensureRoles(getApp().getUserInfo(), () => request.get('/user/roles'))
      getApp().globalData.userInfo = userInfo
      if (!isHospitalAdmin(userInfo)) { this.setData({ forbidden: true }); return }
      this.billId = options.id
      return await this.load()
    } catch (error) {
      this.setData({ error: error.message === '角色信息加载失败' ? error.message : `角色信息加载失败，请稍后重试` })
    }
  },
  async load() {
    this.setData({ error: '', items: [], page: 0, total: 0, hasMore: false, canReconcile: false })
    try {
      const bill = adaptBill(await request.get(`/bills/${this.billId}`))
      this.setData({ bill })
      await this.loadItems(0)
      while (this.data.hasMore) await this.loadItems(this.data.page + 1)
    } catch (error) {
      this.setData({ error: error.message || '账单加载失败', canReconcile: false })
    }
  },
  async loadItems(page) {
    if (this.data.loadingItems) return
    this.setData({ loadingItems: true })
    try {
      const result = await request.get(`/bills/${this.billId}/items`, { page, size: this.data.size })
      const incoming = result.items || result.content || []
      const items = page === 0 ? incoming : this.data.items.concat(incoming)
      const total = result.total === undefined ? result.totalElements : result.total
      const hasMore = items.length < total
      this.setData({ items, page, total, hasMore, canReconcile: !hasMore && this.data.bill.reconcilable })
    } finally {
      this.setData({ loadingItems: false })
    }
  },
  onReachBottom() { if (this.data.hasMore && !this.data.loadingItems) return this.loadItems(this.data.page + 1) },
  retryLoad() { return this.onLoad({ id: this.billId }) },
  confirm() {
    if (!this.data.canReconcile) {
      wx.showToast({ title: this.data.bill && this.data.bill.reconcilable ? '账单明细未加载完整，暂不能对账' : '当前状态不可对账', icon: 'none' })
      return
    }
    wx.showModal({ title: '确认对账', content: '确认账单周期、明细和金额均无误吗？', success: result => { if (result.confirm) this.reconcile('CONFIRM', '') } })
  },
  dispute() {
    if (!this.data.canReconcile) return
    wx.showModal({ title: '提出账单异议', content: '请联系实验室核对异议明细；确认后将记录“明细待核对”状态。', editable: true, placeholderText: '请填写异议原因', success: result => { if (result.confirm && result.content) this.reconcile('DISPUTE', result.content) } })
  },
  async reconcile(action, reason) {
    if (this.data.busy || !this.data.canReconcile) return
    this.setData({ busy: true })
    try {
      const bill = adaptBill(await request.post(`/bills/${this.billId}/reconciliation`, { action, reason }))
      this.setData({ bill, canReconcile: bill.reconcilable })
      wx.showToast({ title: action === 'CONFIRM' ? '已确认对账' : '异议已提交', icon: 'success' })
    } catch (error) { wx.showToast({ title: error.message || '操作失败', icon: 'none' }) } finally { this.setData({ busy: false }) }
  }
})
