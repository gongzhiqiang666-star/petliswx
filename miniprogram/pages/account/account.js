const request = require('../../utils/request')
const { isHospitalAdmin, ensureRoles } = require('../../utils/roles')
const links = [
  { title: '账号与安全', page: 'security' }, { title: '个人账户充值', page: 'recharge' },
  { title: '余额流水', page: 'transactions?kind=wallet' }, { title: '积分流水', page: 'transactions?kind=points' },
  { title: '月结账单', page: 'bills', adminOnly: true }, { title: '积分商城', page: 'mall' },
  { title: '我的优惠券', page: 'coupons' }, { title: '开票管理', page: 'invoices' }, { title: '通知偏好', page: 'preferences' }
]
Page({
  data: { account: null, error: '', loading: false, links: [] },
  async onShow() {
    const userInfo = await ensureRoles(getApp().getUserInfo(), () => request.get('/user/roles'))
    getApp().globalData.userInfo = userInfo
    this.setData({ links: links.filter(link => !link.adminOnly || isHospitalAdmin(userInfo)) })
    this.load()
  },
  async load() {
    this.setData({ loading: true, error: '' })
    try { this.setData({ account: await request.get('/account') }) } catch (error) { this.setData({ error: error.message }) } finally { this.setData({ loading: false }) }
  },
  open(e) {
    const page = e.currentTarget.dataset.page
    if (page === 'bills' && !isHospitalAdmin(getApp().getUserInfo())) return
    const [name, query] = page.split('?')
    wx.navigateTo({ url: `/pages/${name}/${name}${query ? `?${query}` : ''}` })
  }
})
