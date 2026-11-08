const request = require('../../utils/request')
const { isHospitalAdmin, ensureRoles } = require('../../utils/roles')
const links = [
  { title: '账号与安全', page: 'security' }, { title: '个人账户充值', page: 'recharge' },
  { title: '余额流水', page: 'transactions?kind=wallet' }, { title: '积分流水', page: 'transactions?kind=points' },
  { title: '月结账单', page: 'bills', adminOnly: true }, { title: '积分商城', page: 'mall' },
  { title: '我的优惠券', page: 'coupons' }, { title: '开票管理', page: 'invoices' }, { title: '通知偏好', page: 'preferences' }
]
Page({
  data: { account: null, error: '', loading: false, links: [], hospitalWallets: [], hospitalFundsError: '' },
  async onShow() {
    let userInfo = { ...(getApp().getUserInfo() || {}), roles: [] }
    try {
      userInfo = await ensureRoles(getApp().getUserInfo(), () => request.get('/user/roles'))
      getApp().globalData.userInfo = userInfo
    } catch (error) {}
    this.setData({ links: links.filter(link => !link.adminOnly || isHospitalAdmin(userInfo)) })
    return this.load()
  },
  async load() {
    const sequence = this._accountLoadSequence = (this._accountLoadSequence || 0) + 1
    this.setData({ loading: true, error: '', hospitalWallets: [], hospitalFundsError: '' })
    const [account, hospitals] = await Promise.allSettled([request.get('/account'), request.get('/account/hospital-wallets')])
    if (sequence !== this._accountLoadSequence) return
    this.setData({
      account: account.status === 'fulfilled' ? account.value : null,
      error: account.status === 'rejected' ? account.reason.message : '',
      hospitalWallets: hospitals.status === 'fulfilled' && Array.isArray(hospitals.value) ? hospitals.value : [],
      hospitalFundsError: hospitals.status === 'rejected' ? hospitals.reason.message : '', loading: false
    })
  },
  openHospitalFund(e) {
    const id = Number(e.currentTarget.dataset.id)
    if (!this.data.hospitalWallets.some(row => Number(row.hospitalId) === id)) return
    wx.navigateTo({ url: `/pages/transactions/transactions?kind=hospital&hospitalId=${id}` })
  },
  open(e) {
    const page = e.currentTarget.dataset.page
    if (page === 'bills' && !isHospitalAdmin(getApp().getUserInfo())) return
    const [name, query] = page.split('?')
    wx.navigateTo({ url: `/pages/${name}/${name}${query ? `?${query}` : ''}` })
  }
})
