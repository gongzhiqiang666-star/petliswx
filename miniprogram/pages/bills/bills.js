const request = require('../../utils/request')
const { isHospitalAdmin, ensureRoles } = require('../../utils/roles')

Page({
  data: { items: [], page: 0, size: 20, total: 0, hasMore: false, arrears: false, loading: false, error: '', forbidden: false },
  async onLoad(options = {}) {
    const userInfo = await ensureRoles(getApp().getUserInfo(), () => request.get('/user/roles'))
    getApp().globalData.userInfo = userInfo
    if (!isHospitalAdmin(userInfo)) {
      this.setData({ forbidden: true })
      return
    }
    if (options.arrears) this.setData({ arrears: true })
    return this.load(0)
  },
  async load(page) {
    this.setData({ loading: true, error: '' })
    try {
      const result = await request.get('/bills', { page, size: this.data.size, arrears: this.data.arrears })
      const incoming = result.items || result.content || []
      const items = page === 0 ? incoming : this.data.items.concat(incoming)
      this.setData({ items, page, total: result.total === undefined ? result.totalElements : result.total, hasMore: items.length < (result.total === undefined ? result.totalElements : result.total) })
    } catch (error) {
      this.setData({ error: error.message || '账单加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },
  showArrears() { this.setData({ arrears: true }); return this.load(0) },
  showAll() { this.setData({ arrears: false }); return this.load(0) },
  openArrears() { wx.navigateTo({ url: '/pages/bill-arrears/bill-arrears' }) },
  openDetail(e) { wx.navigateTo({ url: `/pages/bill-detail/bill-detail?id=${e.currentTarget.dataset.id}` }) },
  onReachBottom() { if (this.data.hasMore && !this.data.loading) this.load(this.data.page + 1) },
  retryLoad() { return this.load(this.data.page) }
})
