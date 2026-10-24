// pages/error-page/error-page.js
const errorHandler = require('../../utils/error-handler.js')

Page({
  /**
   * 页面的初始数据
   */
  data: {
    errorData: {},
    errorTime: '',
    showDetails: false
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    try {
      // 解析错误数据
      const errorDataStr = decodeURIComponent(options.data || '{}')
      const errorData = JSON.parse(errorDataStr)

      // 格式化时间
      const now = new Date()
      const errorTime = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`

      this.setData({
        errorData,
        errorTime
      })

      console.log('[ErrorPage] 加载错误信息:', errorData)
    } catch (err) {
      console.error('[ErrorPage] 解析错误数据失败:', err)
      // 使用默认错误信息
      this.setData({
        errorData: {
          title: '发生错误',
          message: '无法解析错误信息',
          icon: '❓',
          action: '返回'
        }
      })
    }
  },

  /**
   * 切换详情显示
   */
  onToggleDetails() {
    this.setData({
      showDetails: !this.data.showDetails
    })
  },

  /**
   * 主操作按钮点击
   */
  onPrimaryAction() {
    const { errorData } = this.data

    // 如果有自定义重试回调
    if (errorData.onRetry) {
      // 获取当前页面栈
      const pages = getCurrentPages()
      const prevPage = pages[pages.length - 2]

      if (prevPage && prevPage.onRetry) {
        prevPage.onRetry()
      }

      wx.navigateBack()
      return
    }

    // 默认返回上一页
    if (getCurrentPages().length > 1) {
      wx.navigateBack()
    } else {
      wx.reLaunch({
        url: '/pages/home/home'
      })
    }
  },

  /**
   * 次要操作按钮点击 - 返回首页
   */
  onSecondaryAction() {
    wx.reLaunch({
      url: '/pages/home/home'
    })
  },

  /**
   * 联系客服
   */
  onContactSupport() {
    // 这里使用微信的客服会话功能
    // 在按钮上已经设置了 open-type="contact"
  },

  /**
   * 分享给朋友
   */
  onShareAppMessage() {
    return {
      title: '宠物检测管理系统',
      path: '/pages/home/home'
    }
  }
})
