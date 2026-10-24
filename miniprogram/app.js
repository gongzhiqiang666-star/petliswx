// app.js
const api = require('./api/index.js')
const config = require('./config/config.js')

App({
  globalData: {
    userInfo: null,
    token: null,
    hospitalId: null,
    cityId: null,  // 添加城市ID
    pets: [],  // 添加宠物列表

    // 性能优化：全局缓存
    cache: {
      // 订单表单缓存
      orderForm: null,
      orderFormTimestamp: 0,
      ORDER_FORM_CACHE_TTL: 30 * 60 * 1000, // 30分钟

      // 字典数据缓存
      dictionaryData: {
        animals: null,
        varieties: null,
        sampleTypes: null,
        testTypes: null,
        cities: null
      },
      dictionaryTimestamp: 0,
      DICTIONARY_CACHE_TTL: 60 * 60 * 1000 // 60分钟
    },

    // 环境配置：根据版本自动设置DEBUG模式
    DEBUG: true  // 开发版和体验版开启日志，正式版关闭
  },

  // 登录重试计数器
  loginRetryCount: 0,
  maxLoginRetry: 3,

  onLaunch() {
    // 根据小程序版本自动设置DEBUG模式
    const accountInfo = wx.getAccountInfoSync()
    const envVersion = accountInfo.miniProgram.envVersion // develop, trial, release

    // 开发版和体验版开启日志，正式版关闭
    this.globalData.DEBUG = (envVersion === 'develop' || envVersion === 'trial')

    this.log('[App] 启动', `环境版本: ${envVersion}, DEBUG模式: ${this.globalData.DEBUG}`)

    // 检查登录状态
    this.checkLogin()
  },

  /**
   * 内部日志方法（仅在DEBUG模式下输出）
   */
  log(tag, message, data) {
    if (this.globalData.DEBUG) {
      if (data !== undefined) {
        console.log(`[${tag}]`, message, data)
      } else {
        console.log(`[${tag}]`, message)
      }
    }
  },

  /**
   * 内部错误日志方法（始终输出）
   */
  logError(tag, message, data) {
    if (data !== undefined) {
      console.error(`[${tag}]`, message, data)
    } else {
      console.error(`[${tag}]`, message)
    }
  },

  /**
   * 内部警告日志方法（仅在DEBUG模式下输出）
   */
  logWarn(tag, message, data) {
    if (this.globalData.DEBUG) {
      if (data !== undefined) {
        console.warn(`[${tag}]`, message, data)
      } else {
        console.warn(`[${tag}]`, message)
      }
    }
  },

  /**
   * 检查登录状态
   */
  checkLogin() {
    const token = wx.getStorageSync(config.storageKeys.TOKEN)
    const userInfo = wx.getStorageSync(config.storageKeys.USER_INFO)
    const hospitalId = wx.getStorageSync(config.storageKeys.HOSPITAL_ID)

    if (token) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
      this.globalData.hospitalId = hospitalId

      // 从用户信息中恢复城市ID
      if (userInfo && userInfo.hospital && userInfo.hospital.cityId) {
        this.globalData.cityId = userInfo.hospital.cityId
      }

      // 获取最新的用户信息
      this.refreshUserInfo()
    } else {
      // 未登录，执行登录
      this.login()
    }
  },

  /**
   * 微信登录
   */
  async login() {
    // 检查重试次数，避免无限重试
    if (this.loginRetryCount >= this.maxLoginRetry) {
      this.logError('Login', '登录重试次数已达上限')
      // 不再显示 toast，避免阻塞
      this.logWarn('Login', '登录失败，但允许继续使用应用')
      return
    }

    try {
      // 优化：移除 mask: true，避免阻塞UI渲染
      wx.showLoading({
        title: '登录中...',
        mask: false  // 改为 false，允许用户操作
      })

      // 1. 获取微信code
      const res = await wx.login()
      const code = res.code

      if (!code) {
        throw new Error('获取微信code失败')
      }


      // 2. 调用后端登录接口
      const result = await api.auth.wechatLogin(code)


      // 3. 保存token和用户信息
      wx.setStorageSync(config.storageKeys.TOKEN, result.token)
      wx.setStorageSync(config.storageKeys.USER_INFO, result)
      wx.setStorageSync(config.storageKeys.HOSPITAL_ID, result.hospitalId)

      this.globalData.token = result.token
      this.globalData.userInfo = result
      this.globalData.hospitalId = result.hospitalId

      // 保存城市ID（从医院信息中获取）- 必须字段
      if (result.hospital && result.hospital.cityId) {
        this.globalData.cityId = result.hospital.cityId
        this.log('Login', '城市ID已设置', this.globalData.cityId)
      } else {
        // cityId 为空，记录警告
        this.logError('Login', '城市ID未维护，请联系管理员')
        wx.showModal({
          title: '账号信息不完整',
          content: '您的账号城市信息未维护，无法提交检测申请，请联系管理员',
          showCancel: false,
          confirmText: '我知道了'
        })
      }

      // 登录成功，重置重试计数
      this.loginRetryCount = 0

      wx.hideLoading()
      wx.showToast({
        title: '登录成功',
        icon: 'success',
        duration: 1500
      })
    } catch (err) {
      this.logError('Login', '登录失败', err)
      wx.hideLoading()  // 确保隐藏 loading

      // 增加重试计数
      this.loginRetryCount++

      // 判断错误类型
      let errorMessage = '登录失败，请重试'
      if (err.message) {
        errorMessage = err.message
      } else if (err.errMsg && err.errMsg.includes('url not in domain list')) {
        errorMessage = '请在开发工具中关闭域名校验'
      } else if (err.errMsg && err.errMsg.includes('fail')) {
        errorMessage = '后端服务未启动，请先启动后端服务'
      } else if (err.errMsg) {
        errorMessage = '网络错误，请检查网络连接'
      }

      this.logWarn('Login', `${errorMessage} (${this.loginRetryCount}/${this.maxLoginRetry})`)

      // 如果还有重试机会，2秒后重试
      if (this.loginRetryCount < this.maxLoginRetry) {
        setTimeout(() => {
          this.login()
        }, 2000)
      } else {
        // 最后一次失败后，显示简短提示但不阻塞
        wx.showToast({
          title: '登录失败',
          icon: 'none',
          duration: 1500
        })
      }
    }
  },

  /**
   * 刷新用户信息
   */
  async refreshUserInfo() {
    try {
      const userInfo = await api.user.getProfile()

      this.globalData.userInfo = userInfo
      wx.setStorageSync(config.storageKeys.USER_INFO, userInfo)

      // 更新城市ID（必须字段）
      if (userInfo.hospital && userInfo.hospital.cityId) {
        this.globalData.cityId = userInfo.hospital.cityId
        this.log('App', '城市ID已更新', this.globalData.cityId)
      } else {
        this.logError('App', '城市ID未维护，请联系管理员')
      }

    } catch (err) {
      this.logError('App', '获取用户信息失败', err)
      // 静默失败，不影响用户体验
    }
  },

  /**
   * 退出登录
   */
  logout() {

    // 清除本地存储
    wx.removeStorageSync(config.storageKeys.TOKEN)
    wx.removeStorageSync(config.storageKeys.USER_INFO)
    wx.removeStorageSync(config.storageKeys.HOSPITAL_ID)

    // 清空全局数据
    this.globalData.token = null
    this.globalData.userInfo = null
    this.globalData.hospitalId = null

    // 提示用户
    wx.showToast({
      title: '已退出登录',
      icon: 'none'
    })

    // 重新登录
    setTimeout(() => {
      this.loginRetryCount = 0 // 重置重试计数
      this.login()
    }, 1500)
  },

  /**
   * 获取当前用户信息
   */
  getUserInfo() {
    return this.globalData.userInfo
  },

  /**
   * 获取当前token
   */
  getToken() {
    return this.globalData.token
  },

  /**
   * 获取当前医院ID
   */
  getHospitalId() {
    return this.globalData.hospitalId
  },

  /**
   * 获取当前城市ID
   */
  getCityId() {
    return this.globalData.cityId
  },

  /**
   * 检查是否已登录
   */
  isLoggedIn() {
    return !!this.globalData.token
  },

  /**
   * 获取缓存的订单表单
   */
  getCachedOrderForm() {
    const { orderForm, orderFormTimestamp, ORDER_FORM_CACHE_TTL } = this.globalData.cache
    const now = Date.now()

    if (orderForm && (now - orderFormTimestamp < ORDER_FORM_CACHE_TTL)) {
      this.log('App', '使用缓存的订单表单')
      return orderForm
    }

    return null
  },

  /**
   * 设置订单表单缓存
   */
  setCachedOrderForm(formData) {
    this.globalData.cache.orderForm = formData
    this.globalData.cache.orderFormTimestamp = Date.now()
    this.log('App', '订单表单已缓存')
  },

  /**
   * 清除订单表单缓存
   */
  clearCachedOrderForm() {
    this.globalData.cache.orderForm = null
    this.globalData.cache.orderFormTimestamp = 0
    this.log('App', '订单表单缓存已清除')
  },

  /**
   * 获取缓存的字典数据
   */
  getCachedDictionary(key) {
    const { dictionaryData, dictionaryTimestamp, DICTIONARY_CACHE_TTL } = this.globalData.cache
    const now = Date.now()

    if (dictionaryData[key] && (now - dictionaryTimestamp < DICTIONARY_CACHE_TTL)) {
      this.log('App', `使用缓存的字典数据: ${key}`)
      return dictionaryData[key]
    }

    return null
  },

  /**
   * 设置字典数据缓存
   */
  setCachedDictionary(key, data) {
    this.globalData.cache.dictionaryData[key] = data
    this.globalData.cache.dictionaryTimestamp = Date.now()
    this.log('App', `字典数据已缓存: ${key}`)
  },

  /**
   * 清除所有字典数据缓存
   */
  clearCachedDictionary() {
    this.globalData.cache.dictionaryData = {
      animals: null,
      varieties: null,
      sampleTypes: null,
      testTypes: null,
      cities: null
    }
    this.globalData.cache.dictionaryTimestamp = 0
    this.log('App', '字典数据缓存已清除')
  }
})
