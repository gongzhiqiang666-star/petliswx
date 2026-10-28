const request = require('../../utils/request')
const { isHospitalAdmin, ensureRoles } = require('../../utils/roles')
const { adaptBill } = require('../../utils/bills')

Page({
  data: { items: [], error: '', forbidden: false, page: 0, size: 100, total: 0, hasMore: false, loading: false },
  async onLoad() {
    try {
      const userInfo = await ensureRoles(getApp().getUserInfo(), () => request.get('/user/roles'))
      getApp().globalData.userInfo = userInfo
      if (!isHospitalAdmin(userInfo)) { this.setData({ forbidden: true }); return }
      return await this.load(0)
    } catch (error) {
      this.setData({ error: '角色信息加载失败，请稍后重试' })
    }
  },
  async load(page) {
    if (this.data.loading) return
    this.setData({ error: '', loading: true })
    try {
      const result = await request.get('/bills', { arrears: true, page, size: this.data.size })
      const incoming = (result.items || result.content || []).map(adaptBill)
      const items = page === 0 ? incoming : this.data.items.concat(incoming)
      const total = result.total === undefined ? result.totalElements : result.total
      this.setData({ items, page, total, hasMore: items.length < total })
    } catch (error) {
      this.setData({ error: error.message || '欠款加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },
  onReachBottom() { if (this.data.hasMore && !this.data.loading) return this.load(this.data.page + 1) },
  retryLoad() { return this.onLoad() },
  openDetail(e) { wx.navigateTo({ url: `/pages/bill-detail/bill-detail?id=${e.currentTarget.dataset.id}` }) }
})
