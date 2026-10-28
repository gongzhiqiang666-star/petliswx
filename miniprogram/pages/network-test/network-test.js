// pages/network-test/network-test.js
const config = require('../../config/config.js')
const app = getApp()

Page({
  data: {
    baseURL: config.baseURL,
    ipAddress: '由环境配置管理',
    testResult: '点击按钮开始测试...',
    systemInfo: {},
    loginStatus: '检查中...',
    hasToken: false,
    hasUserInfo: false
  },

  onLoad() {
    // 获取系统信息
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      systemInfo: {
        system: systemInfo.system,
        platform: systemInfo.platform,
        version: systemInfo.version
      }
    })

    // 检查登录状态
    this.checkLoginStatus()

    console.log('[网络测试] 配置信息:', {
      baseURL: config.baseURL,
      systemInfo: systemInfo
    })
  },

  /**
   * 检查登录状态
   */
  checkLoginStatus() {
    const token = wx.getStorageSync(config.storageKeys.TOKEN)
    const userInfo = wx.getStorageSync(config.storageKeys.USER_INFO)

    this.setData({
      hasToken: !!token,
      hasUserInfo: !!userInfo,
      loginStatus: token ? '已登录（可能token已过期）' : '未登录'
    })
  },

  /**
   * 清除缓存并重新登录
   */
  clearCache() {
    wx.showModal({
      title: '确认清除',
      content: '这将清除所有本地缓存并重新登录，确定吗？',
      success: (res) => {
        if (res.confirm) {
          this.doClearCache()
        }
      }
    })
  },

  /**
   * 执行清除缓存
   */
  doClearCache() {
    wx.showLoading({
      title: '清除中...',
      mask: true
    })

    try {
      // 清除所有本地存储
      wx.clearStorageSync()

      // 清空全局数据
      app.globalData.token = null
      app.globalData.userInfo = null
      app.globalData.hospitalId = null
      app.globalData.cityId = null

      // 重置登录重试计数
      app.loginRetryCount = 0

      wx.hideLoading()

      wx.showToast({
        title: '已清除缓存',
        icon: 'success'
      })

      // 延迟后重新登录
      setTimeout(() => {
        this.setData({
          testResult: '正在重新登录...\n请等待登录完成',
          hasToken: false,
          hasUserInfo: false,
          loginStatus: '重新登录中...'
        })

        // 调用app的登录方法
        app.login()

        // 登录成功后更新状态
        setTimeout(() => {
          this.checkLoginStatus()
          this.setData({
            testResult: '✅ 缓存已清除，登录已重新初始化\n\n如果之前有问题，现在应该解决了'
          })
        }, 3000)

      }, 1000)

    } catch (err) {
      wx.hideLoading()
      wx.showToast({
        title: '清除失败',
        icon: 'none'
      })
      console.error('[网络测试] 清除缓存失败:', err)
    }
  },

  /**
   * 测试后端连接
   */
  testConnection() {
    this.setData({ testResult: '正在测试连接...\n' })

    const testURL = `${config.baseURL}/auth/wechat/login`

    console.log('[网络测试] 测试URL:', testURL)

    wx.request({
      url: testURL,
      method: 'POST',
      data: { code: 'test' },
      header: {
        'Content-Type': 'application/json'
      },
      success: (res) => {
        console.log('[网络测试] 连接成功:', res)
        this.setData({
          testResult: `✅ 连接成功！\n\n状态码: ${res.statusCode}\n响应: ${JSON.stringify(res.data).substring(0, 100)}`
        })
      },
      fail: (err) => {
        console.error('[网络测试] 连接失败:', err)
        this.setData({
          testResult: `❌ 连接失败！\n\n错误信息: ${err.errMsg}\n\n可能原因:\n1. 未勾选"不校验合法域名"\n2. 手机和电脑不在同一WiFi\n3. 后端服务未运行`
        })

        // 显示详细错误
        wx.showModal({
          title: '网络请求失败',
          content: `错误: ${err.errMsg}\n\n解决方案:\n\n1. 确认开发者工具已勾选"不校验合法域名"\n2. 确认手机和电脑在同一WiFi\n3. 点击"清除所有缓存并重新登录"按钮`,
          showCancel: false
        })
      }
    })
  },

  /**
   * 测试登录接口
   */
  testLogin() {
    this.setData({ testResult: '正在测试登录接口...\n' })

    wx.request({
      url: `${config.baseURL}/auth/wechat/login`,
      method: 'POST',
      data: {
        code: 'test123'
      },
      header: {
        'Content-Type': 'application/json'
      },
      success: (res) => {
        console.log('[网络测试] 登录测试成功:', res)
        this.setData({
          testResult: `✅ 登录接口正常！\n\n状态码: ${res.statusCode}\n响应: ${JSON.stringify(res.data).substring(0, 100)}`
        })
      },
      fail: (err) => {
        console.error('[网络测试] 登录测试失败:', err)
        this.setData({
          testResult: `❌ 登录测试失败！\n\n错误: ${err.errMsg}`
        })

        wx.showModal({
          title: '请求失败',
          content: `错误信息: ${err.errMsg}`,
          showCancel: false
        })
      }
    })
  }
})
