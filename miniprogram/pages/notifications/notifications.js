// pages/notifications/notifications.js
const api = require('../../api/index.js')

Page({
  data: {
    notifications: [],
    unreadCount: 0,
    loading: false,
    isEmpty: false,
    pageNum: 1,
    pageSize: 20,
    hasMore: true,
    isLoadingMore: false
  },

  onLoad() {
    this.loadNotifications()
    this.loadUnreadCount()
    this._lastLoadTime = Date.now() // 记录首次加载时间
  },

  onShow() {
    // 性能优化：只在距离上次加载超过5分钟时才刷新
    const lastLoadTime = this._lastLoadTime || 0
    const now = Date.now()
    const REFRESH_INTERVAL = 5 * 60 * 1000 // 5分钟

    if (now - lastLoadTime > REFRESH_INTERVAL || this.data.notifications.length === 0) {
      this.resetAndLoadNotifications()
      this._lastLoadTime = now
    }

    // 每次显示页面时刷新未读数量
    this.loadUnreadCount()
  },

  /**
   * 重置并加载通知列表
   */
  resetAndLoadNotifications() {
    this.setData({
      pageNum: 1,
      notifications: [],
      hasMore: true
    })
    this.loadNotifications()
  },

  /**
   * 加载通知列表（第一页）
   */
  async loadNotifications() {
    if (this.data.loading) {
      return
    }

    this.setData({ loading: true })

    try {
      const result = await api.notifications.getMyHospitalNotifications({
        pageNum: 0,  // Spring Data JPA page starts from 0
        pageSize: this.data.pageSize
      })

      const notifications = result.content || result.list || result.data || []

      this.setData({
        notifications: this.formatNotifications(notifications),
        loading: false,
        isEmpty: notifications.length === 0,
        hasMore: !result.last  // 使用后端返回的 last 字段判断
      })
    } catch (err) {
      console.error('[Notifications] 加载通知列表失败:', err)
      this.setData({
        loading: false,
        isEmpty: false
      })

      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    }
  },

  /**
   * 加载更多通知
   */
  async loadMore() {
    if (this.data.isLoadingMore || !this.data.hasMore) {
      return
    }

    this.setData({ isLoadingMore: true })

    try {
      const nextPage = this.data.pageNum + 1

      const result = await api.notifications.getMyHospitalNotifications({
        pageNum: nextPage - 1,  // Spring Data JPA page starts from 0
        pageSize: this.data.pageSize
      })

      const notifications = result.content || result.list || result.data || []

      this.setData({
        notifications: this.data.notifications.concat(this.formatNotifications(notifications)),
        pageNum: nextPage,
        isLoadingMore: false,
        hasMore: !result.last
      })
    } catch (err) {
      console.error('[Notifications] 加载更多通知失败:', err)
      this.setData({ isLoadingMore: false })

      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    }
  },

  /**
   * 加载未读通知数量
   */
  async loadUnreadCount() {
    try {
      const result = await api.notifications.getUnreadCount()
      const count = result.data || result || 0

      this.setData({ unreadCount: count })

      // 更新tabBar徽标
      if (count > 0) {
        wx.setTabBarBadge({
          index: 2, // 通知tab的位置
          text: count > 99 ? '99+' : String(count)
        })
      } else {
        wx.removeTabBarBadge({ index: 2 })
      }
    } catch (err) {
      console.error('[Notifications] 加载未读数量失败:', err)
    }
  },

  /**
   * 格式化通知数据
   */
  formatNotifications(notifications) {
    return notifications.map(item => {
      // 格式化时间
      const time = this.formatTime(item.operationTime)

      // 状态颜色和图标
      let statusColor = '#6366f1'
      let statusIcon = '📋'
      let statusClass = 'processing'

      if (item.newStatus === 3) {
        statusColor = '#10b981' // 已完成 - 绿色
        statusIcon = '✓'
        statusClass = 'completed'
      } else if (item.newStatus === 1) {
        statusColor = '#ef4444' // 审核未通过 - 红色
        statusIcon = '✕'
        statusClass = 'rejected'
      } else if (item.newStatus === 2) {
        statusColor = '#f59e0b' // 等待报告 - 橙色
        statusIcon = '⏳'
        statusClass = 'pending'
      } else if (item.newStatus === 0) {
        statusColor = '#6366f1' // 待审核 - 蓝紫色
        statusIcon = '⏱'
        statusClass = 'processing'
      }

      return {
        ...item,
        time,
        statusColor,
        statusIcon,
        statusClass
      }
    })
  },

  /**
   * 格式化时间
   */
  formatTime(timeStr) {
    if (!timeStr) return ''

    const time = new Date(timeStr)
    const now = new Date()
    const diff = now.getTime() - time.getTime()

    // 1分钟内
    if (diff < 60000) {
      return '刚刚'
    }

    // 1小时内
    if (diff < 3600000) {
      const minutes = Math.floor(diff / 60000)
      return `${minutes}分钟前`
    }

    // 24小时内
    if (diff < 86400000) {
      const hours = Math.floor(diff / 3600000)
      return `${hours}小时前`
    }

    // 7天内
    if (diff < 604800000) {
      const days = Math.floor(diff / 86400000)
      return `${days}天前`
    }

    // 超过7天显示具体日期
    const year = time.getFullYear()
    const month = String(time.getMonth() + 1).padStart(2, '0')
    const day = String(time.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  /**
   * 点击通知项
   */
  async onNotificationTap(e) {
    const notification = e.currentTarget.dataset.notification

    // 标记为已读
    try {
      await api.notifications.markAsRead(notification.id)

      // 更新已读状态
      const notifications = this.data.notifications.map(item => {
        if (item.id === notification.id) {
          return { ...item, wechatSentStatus: 1 }
        }
        return item
      })

      this.setData({ notifications })

      // 刷新未读数量
      this.loadUnreadCount()
    } catch (err) {
      console.error('[Notifications] 标记已读失败:', err)
    }

    // 跳转到订单详情页
    wx.navigateTo({
      url: `/pages/order-detail/order-detail?id=${notification.orderId}`
    })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.resetAndLoadNotifications()
    this.loadUnreadCount()
    wx.stopPullDownRefresh()
  },

  /**
   * 触底加载更多
   */
  onReachBottom() {
    this.loadMore()
  }
})
