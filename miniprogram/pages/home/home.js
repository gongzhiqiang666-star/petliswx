// pages/home/home.js
const api = require('../../api/index.js')
const app = getApp()

Page({
  data: {
    recentActivities: [],
    isEmpty: true,
    hasLoadedOnce: false
  },

  onLoad() {
    // 等待登录完成后再加载通知
    this.waitForLoginAndLoad()
  },

  onShow() {
    // 只在首次加载时请求，避免频繁触发
    // 用户可通过下拉刷新更新数据
    if (!this.data.hasLoadedOnce) {
      this.waitForLoginAndLoad()
      this.setData({ hasLoadedOnce: true })
    }
  },

  /**
   * 等待登录完成后再加载通知
   */
  async waitForLoginAndLoad() {
    console.log('[Home] 等待登录完成...')

    // 等待登录完成，最多等待10秒
    const maxWaitTime = 10000  // 10秒
    const checkInterval = 100  // 100ms检查一次
    let elapsedTime = 0

    while (!app.isLoggedIn() && elapsedTime < maxWaitTime) {
      await new Promise(resolve => setTimeout(resolve, checkInterval))
      elapsedTime += checkInterval
    }

    if (!app.isLoggedIn()) {
      console.log('[Home] 登录超时，不加载通知')
      return
    }

    console.log('[Home] 登录已完成，开始加载通知')
    await this.loadRecentNotifications()
  },

  /**
   * 加载最近3条通知
   */
  async loadRecentNotifications() {
    try {
      console.log('[Home] 开始加载通知...')
      const result = await api.notifications.getMyHospitalNotifications({
        pageNum: 0,
        pageSize: 3
      })

      console.log('[Home] API返回结果:', result)

      // 处理分页数据结构
      let notifications = []
      if (Array.isArray(result)) {
        notifications = result
      } else if (result && result.content) {
        notifications = result.content
      } else if (result && result.list) {
        notifications = result.list
      } else if (result && result.data) {
        notifications = result.data
      }

      console.log('[Home] 提取的通知数据:', notifications)

      const activities = this.formatNotifications(notifications)

      console.log('[Home] 格式化后的活动数据:', activities)

      this.setData({
        recentActivities: activities,
        isEmpty: activities.length === 0
      })

      console.log('[Home] 首页通知加载完成，数量:', activities.length)
    } catch (err) {
      console.error('[Home] 加载通知失败:', err)
      // 不显示错误提示，静默失败
    }
  },

  /**
   * 格式化通知数据
   */
  formatNotifications(notifications) {
    if (!Array.isArray(notifications) || notifications.length === 0) {
      console.log('[Home] 没有通知数据')
      return []
    }

    console.log('[Home] 开始格式化通知，数量:', notifications.length)

    return notifications.map(item => {
      console.log('[Home] 处理通知项:', item)

      // 格式化时间
      const time = this.formatTime(item.operationTime)

      // 使用notificationMessage或生成默认消息
      let title = item.notificationMessage || '订单状态更新'
      let description = ''

      if (!item.notificationMessage) {
        // 如果没有详细消息，使用状态信息
        const statusMap = {
          '-3': '已取消',
          '-2': '草稿',
          '0': '待审核',
          '1': '已审核',
          '2': '检测中',
          '3': '检测完成'
        }
        const statusText = statusMap[item.newStatus] || '状态更新'
        description = `订单状态已更新为：${statusText}`
      } else {
        // 提取消息的前30个字符作为描述
        description = item.notificationMessage.substring(0, 30)
        if (item.notificationMessage.length > 30) {
          description += '...'
        }
      }

      const formatted = {
        id: String(item.id),
        type: 'notification',
        title: title,
        description: description,
        time: time,
        orderId: item.orderId
      }

      console.log('[Home] 格式化后的通知:', formatted)
      return formatted
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
    const month = String(time.getMonth() + 1).padStart(2, '0')
    const day = String(time.getDate()).padStart(2, '0')
    return `${month}-${day}`
  },

  // 导航到预约检测
  onOrderSelect() {
    wx.navigateTo({
      url: '/pages/order-select-pet/order-select-pet'
    })
  },

  // 导航到宠物建档
  onPetAdd() {
    wx.navigateTo({
      url: '/pages/pet-add/pet-add'
    })
  },

  // 导航到报告列表
  onReportList() {
    wx.navigateTo({
      url: '/pages/report-list/report-list'
    })
  },

  // 查看全部通知
  onViewAll() {
    wx.navigateTo({
      url: '/pages/notifications/notifications'
    })
  },

  // 下拉刷新
  async onPullDownRefresh() {
    try {
      await this.loadRecentNotifications()
      wx.showToast({
        title: '刷新成功',
        icon: 'success',
        duration: 1500
      })
    } catch (err) {
      console.error('[Home] 刷新失败:', err)
      wx.showToast({
        title: '刷新失败',
        icon: 'none',
        duration: 1500
      })
    } finally {
      // 停止下拉刷新动画
      wx.stopPullDownRefresh()
    }
  },

  // 点击通知项
  onActivityTap(e) {
    const item = e.currentTarget.dataset.item
    if (item.orderId) {
      // 跳转到订单详情
      wx.navigateTo({
        url: `/pages/order-detail/order-detail?id=${item.orderId}`
      })
    }
  }
})
