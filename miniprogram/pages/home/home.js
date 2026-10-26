const request = require('../../utils/request')
const api = require('../../api/index.js')
const app = getApp()

const statusLabels = {
  '-3': '待支付',
  '-2': '草稿',
  '0': '待审核',
  '1': '审核未通过',
  '2': '等待报告',
  '3': '检测完成',
  '4': '已取消'
}

const statusTones = {
  '-3': 'warn',
  '3': 'muted',
  '4': 'muted'
}

const qualificationStatuses = {
  approved: { label: '资质审核通过', tone: 'success' },
  pending: { label: '资质待审核', tone: 'pending' },
  rejected: { label: '资质已驳回', tone: 'rejected' }
}

Page({
  data: {
    doctorName: '送检医生',
    hospitalName: '宠物医院工作台',
    qualificationStatusLabel: '',
    qualificationTone: '',
    notificationCount: 0,
    notificationLoading: true,
    notificationError: '',
    recentActivities: [],
    isEmpty: true,
    shortcuts: [
      { id: 'test', title: '我要检测', icon: '/assets/icons/calendar.png' },
      { id: 'pets', title: '档案中心', icon: '/assets/icons/pet.png' },
      { id: 'reports', title: '报告中心', icon: '/assets/icons/report.png' },
      { id: 'notifications', title: '消息中心', icon: '/assets/icons/bell.png' }
    ],
    services: [
      { id: 'account', title: '钱包与充值', icon: '/assets/icons/icon-pay.png' },
      { id: 'coupons', title: '优惠券', icon: '/assets/icons/activity.png' },
      { id: 'orders', title: '送检记录', icon: '/assets/icons/order.png' },
      { id: 'invoices', title: '发票管理', icon: '/assets/icons/filetext.png' }
    ],
    todoChips: [
      { status: -3, label: '待支付', value: null, tone: 'warn' },
      { status: 0, label: '待审核', value: null, tone: 'primary' },
      { status: 2, label: '待报告', value: null, tone: 'primary' }
    ],
    todoLoading: true,
    todoError: '',
    recentOrders: [],
    ordersLoading: true,
    ordersError: '',
    balance: null,
    financeLoading: true,
    financeError: ''
  },

  onShow() {
    this.waitForLoginAndLoad()
  },

  async waitForLoginAndLoad() {
    const maxWaitTime = 10000
    const checkInterval = 100
    let elapsedTime = 0

    while (!app.isLoggedIn() && elapsedTime < maxWaitTime) {
      await new Promise(resolve => setTimeout(resolve, checkInterval))
      elapsedTime += checkInterval
    }
    if (!app.isLoggedIn()) return

    if (this._homeLoad) return this._homeLoad
    this._homeLoad = (async () => {
      await app.refreshUserInfo()
      const user = app.globalData.userInfo || {}
      this.setData({
        doctorName: user.nickname || user.doctorName || '送检医生',
        hospitalName: user.hospital?.hospitalName || user.hospitalName || '宠物医院工作台'
      })
      await Promise.all([
        this.loadRecentNotifications(),
        this.loadTodoSummary(),
        this.loadRecentOrders(),
        this.loadAccount()
      ])
    })()
    try {
      await this._homeLoad
    } finally {
      this._homeLoad = null
    }
  },

  async loadRecentNotifications() {
    const requestId = (this._notificationRequestId || 0) + 1
    this._notificationRequestId = requestId
    this.setData({ notificationError: '', notificationLoading: true })
    try {
      const [result, count] = await Promise.all([
        request.get('/inbox', { page: 0, size: 2 }),
        request.get('/inbox/unread-count')
      ])
      if (requestId !== this._notificationRequestId) return
      const activities = (result.items || []).slice(0, 2).map(item => ({
        id: String(item.id),
        title: item.title,
        description: item.message,
        time: this.formatTime(item.createdAt)
      }))
      this.setData({
        notificationCount: count,
        recentActivities: activities,
        isEmpty: !activities.length
      })
      await this.checkArticlePopup()
    } catch (error) {
      if (requestId !== this._notificationRequestId) return
      this.setData({ notificationError: '消息暂时无法加载，请稍后重试' })
    } finally {
      if (requestId === this._notificationRequestId) this.setData({ notificationLoading: false })
    }
  },

  async loadTodoSummary() {
    const requestId = (this._todoRequestId || 0) + 1
    this._todoRequestId = requestId
    const chips = this.data.todoChips
    this.setData({ todoLoading: true, todoError: '' })
    try {
      const results = await Promise.all(chips.map(chip =>
        api.orders.getList({ page: 0, size: 1, status: chip.status })
      ))
      if (requestId !== this._todoRequestId) return
      const todoChips = chips.map((chip, index) => {
        const result = results[index]
        const total = result.totalElements !== undefined ? result.totalElements : result.total
        if (total === undefined || total === null || !Number.isFinite(Number(total))) {
          throw new Error('待办计数返回格式异常')
        }
        return { ...chip, value: Number(total) }
      })
      this.setData({ todoChips })
    } catch (error) {
      if (requestId !== this._todoRequestId) return
      this.setData({
        todoChips: chips.map(chip => ({ ...chip, value: null })),
        todoError: '待办暂时无法加载，请稍后重试'
      })
    } finally {
      if (requestId === this._todoRequestId) this.setData({ todoLoading: false })
    }
  },

  async loadRecentOrders() {
    const requestId = (this._ordersRequestId || 0) + 1
    this._ordersRequestId = requestId
    this.setData({ ordersLoading: true, ordersError: '' })
    try {
      const result = await api.orders.getList({ page: 0, size: 3 })
      if (requestId !== this._ordersRequestId) return
      const rows = result.content || result.list || []
      this.setData({
        recentOrders: rows.slice(0, 3).map(order => ({
          id: order.orderId,
          petName: order.animalName || order.orderNo || '',
          statusLabel: statusLabels[String(order.sampleStatus)] || order.sampleStatusDesc || '状态更新',
          statusTone: statusTones[String(order.sampleStatus)] || 'primary',
          time: this.formatTime(order.createDt || order.collectDt)
        }))
      })
    } catch (error) {
      if (requestId !== this._ordersRequestId) return
      this.setData({ ordersError: '业务进度暂时无法加载，请稍后重试' })
    } finally {
      if (requestId === this._ordersRequestId) this.setData({ ordersLoading: false })
    }
  },

  async loadAccount() {
    const requestId = (this._accountRequestId || 0) + 1
    this._accountRequestId = requestId
    this.setData({ financeLoading: true, financeError: '' })
    try {
      const account = await request.get('/account')
      if (requestId !== this._accountRequestId) return
      if (account.balance === undefined || account.balance === null) throw new Error('余额返回格式异常')
      const qualification = qualificationStatuses[account.reviewStatus] || { label: '', tone: '' }
      this.setData({
        balance: account.balance,
        qualificationStatusLabel: qualification.label,
        qualificationTone: qualification.tone
      })
    } catch (error) {
      if (requestId !== this._accountRequestId) return
      this.setData({ balance: null, financeError: '余额暂时无法加载，请稍后重试' })
    } finally {
      if (requestId === this._accountRequestId) this.setData({ financeLoading: false })
    }
  },

  async checkArticlePopup() {
    if (this._popupInFlight) return
    this._popupInFlight = true
    try {
      const article = await request.get('/articles/popup')
      if (!article.id || wx.getStorageSync('article-popup-' + article.id)) return
      wx.setStorageSync('article-popup-' + article.id, true)
      const answer = await wx.showModal({
        title: article.title,
        content: article.summary || '有新的资讯发布',
        confirmText: '查看详情',
        cancelText: '稍后查看'
      })
      if (answer.confirm) wx.navigateTo({ url: '/pages/article-detail/article-detail?id=' + article.id })
    } catch (error) {
    } finally {
      this._popupInFlight = false
    }
  },

  formatTime(timeStr) {
    if (!timeStr) return ''
    const time = new Date(timeStr)
    if (Number.isNaN(time.getTime())) return ''
    const diff = new Date().getTime() - time.getTime()
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
    if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
    const month = String(time.getMonth() + 1).padStart(2, '0')
    const day = String(time.getDate()).padStart(2, '0')
    return `${month}-${day}`
  },

  onPosterError() {
    this.setData({ posterError: true })
  },

  openShortcut(e) {
    const id = e.currentTarget.dataset.id
    if (id === 'test') return this.onOrderSelect()
    if (id === 'reports') return this.onReportList()
    if (id === 'notifications') return this.onViewAll()
    wx.navigateTo({ url: `/pages/${id}/${id}` })
  },

  onTodoTap() {
    wx.navigateTo({ url: '/pages/orders/orders' })
  },

  onOrdersViewAll() {
    wx.navigateTo({ url: '/pages/orders/orders' })
  },

  onRecentOrderTap(e) {
    wx.navigateTo({ url: `/pages/order-detail/order-detail?id=${e.currentTarget.dataset.id}` })
  },

  onRecharge() {
    wx.navigateTo({ url: '/pages/recharge/recharge' })
  },

  onHelp() {
    wx.showModal({
      title: '使用帮助',
      content: '点击“我要检测”开始送检，可在送检记录、报告中心和消息中心查看后续进度。',
      showCancel: false,
      confirmText: '知道了'
    })
  },

  onOrderSelect() {
    wx.switchTab({ url: '/pages/order-select-pet/order-select-pet' })
  },

  onReportList() {
    wx.switchTab({ url: '/pages/report-list/report-list' })
  },

  onViewAll() {
    wx.switchTab({ url: '/pages/notifications/notifications' })
  },

  async onPullDownRefresh() {
    try {
      await this.waitForLoginAndLoad()
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  onActivityTap() {
    this.onViewAll()
  }
})
