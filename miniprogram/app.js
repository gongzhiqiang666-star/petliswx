// app.js
const api = require('./api/index.js')
const config = require('./config/config.js')
const {installUpdates} = require('./utils/updates')
const diagnostics = require('./utils/diagnostics')

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

  onError(error){diagnostics.capture('error',error)},
  onUnhandledRejection(event){diagnostics.capture('rejection',event && event.reason)},

  // 登录重试计数器
  loginRetryCount: 0,
  maxLoginRetry: 3,

  onLaunch() {
    installUpdates()
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
      if (userInfo && userInfo.reviewer) wx.reLaunch({url: '/pages/review/review'})
      else this.refreshUserInfo()
    } else {
      // 未登录，执行登录
      this.login()
    }
  },

  /**
   * 微信登录
   */
  async login() {
    const pages = getCurrentPages()
    if (!pages.length || pages[pages.length - 1].route !== 'pages/login/login') {
      wx.reLaunch({url: '/pages/login/login'})
    }
  },

  acceptSession(result,remember=true) {
    this.globalData.rememberSession=remember
    if(remember){wx.setStorageSync(config.storageKeys.TOKEN,result.token);wx.setStorageSync(config.storageKeys.USER_INFO,result);wx.setStorageSync(config.storageKeys.HOSPITAL_ID,result.hospitalId || '')}else{wx.removeStorageSync(config.storageKeys.TOKEN);wx.removeStorageSync(config.storageKeys.USER_INFO);wx.removeStorageSync(config.storageKeys.HOSPITAL_ID)}
    this.globalData.token = result.token
    this.globalData.userInfo = result
    this.globalData.hospitalId = result.hospitalId || null
    this.globalData.cityId = result.hospital ? result.hospital.cityId : null
    if (result.reviewer) wx.reLaunch({url: '/pages/review/review'})
    else wx.switchTab({url: '/pages/home/home'})
  },

  /**
   * 刷新用户信息
   */
  async refreshUserInfo() {
    try {
      const userInfo = await api.user.getProfile()

      this.globalData.userInfo = userInfo
      if(this.globalData.rememberSession!==false)wx.setStorageSync(config.storageKeys.USER_INFO,userInfo)

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
  clearSession(){
    wx.removeStorageSync(config.storageKeys.TOKEN)
    wx.removeStorageSync(config.storageKeys.USER_INFO)
    wx.removeStorageSync(config.storageKeys.HOSPITAL_ID)
    wx.removeStorageSync('order_form')
    this.globalData.token=null;this.globalData.userInfo=null;this.globalData.hospitalId=null;this.globalData.cityId=null
    this.clearCachedOrderForm()
    wx.reLaunch({url:'/pages/login/login'})
  },
  async logout() {
    try { await require('./utils/request').post('/auth/logout', {}) } catch (e) {
      this.logWarn('Logout', '服务端会话撤销失败，本地登录保持不变')
      throw e
    }
    this.clearSession()
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
