const request = require('../../utils/request')
const { isHospitalAdmin, ensureRoles } = require('../../utils/roles')
Page({
  data: { items: [], error: '', forbidden: false },
  async onLoad() {
    const userInfo = await ensureRoles(getApp().getUserInfo(), () => request.get('/user/roles'))
    getApp().globalData.userInfo = userInfo
    if (!isHospitalAdmin(userInfo)) { this.setData({ forbidden: true }); return }
    return this.load()
  },
  async load() {
    this.setData({ error: '' })
    try { const result = await request.get('/bills', { arrears: true, page: 0, size: 100 }); this.setData({ items: result.items || result.content || [] }) } catch (error) { this.setData({ error: error.message }) }
  },
  openDetail(e) { wx.navigateTo({ url: `/pages/bill-detail/bill-detail?id=${e.currentTarget.dataset.id}` }) }
})
