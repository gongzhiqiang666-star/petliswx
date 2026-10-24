/**
 * 错误处理工具
 * 提供友好的错误提示和处理
 */

/**
 * 错误类型枚举
 */
const ErrorType = {
  NETWORK_ERROR: 'NETWORK_ERROR',           // 网络连接失败
  TIMEOUT: 'TIMEOUT',                       // 请求超时
  SERVER_ERROR: 'SERVER_ERROR',             // 服务器错误
  AUTH_ERROR: 'AUTH_ERROR',                 // 认证失败
  PERMISSION_ERROR: 'PERMISSION_ERROR',     // 权限不足
  NOT_FOUND: 'NOT_FOUND',                   // 资源不存在
  BUSINESS_ERROR: 'BUSINESS_ERROR',         // 业务错误
  UNKNOWN: 'UNKNOWN'                        // 未知错误
}

/**
 * 错误信息映射
 */
const ErrorMessages = {
  [ErrorType.NETWORK_ERROR]: {
    title: '网络连接失败',
    message: '请检查网络设置后重试',
    icon: '📡',
    suggest: '建议：\n• 检查WiFi或移动网络是否开启\n• 尝试切换网络环境\n• 稍后再试'
  },
  [ErrorType.TIMEOUT]: {
    title: '请求超时',
    message: '网络响应过慢，请稍后重试',
    icon: '⏰',
    suggest: '建议：\n• 检查网络信号强度\n• 关闭其他占用网络的应用\n• 稍后再试'
  },
  [ErrorType.SERVER_ERROR]: {
    title: '服务器错误',
    message: '服务器暂时无法响应，请稍后重试',
    icon: '⚠️',
    suggest: '建议：\n• 稍后重试\n• 如果问题持续，请联系客服'
  },
  [ErrorType.AUTH_ERROR]: {
    title: '登录已过期',
    message: '请重新登录',
    icon: '🔒',
    action: '重新登录'
  },
  [ErrorType.PERMISSION_ERROR]: {
    title: '权限不足',
    message: '您没有权限访问此功能',
    icon: '🚫',
    suggest: '建议：\n• 联系管理员开通权限\n• 确认账号状态是否正常'
  },
  [ErrorType.NOT_FOUND]: {
    title: '资源不存在',
    message: '请求的内容不存在或已被删除',
    icon: '🔍',
    suggest: '建议：\n• 返回上一页\n• 刷新页面重试'
  },
  [ErrorType.BUSINESS_ERROR]: {
    title: '操作失败',
    message: '', // 动态设置
    icon: '❌',
    suggest: '建议：\n• 检查输入信息是否正确\n• 稍后重试'
  },
  [ErrorType.UNKNOWN]: {
    title: '未知错误',
    message: '发生了未知错误，请稍后重试',
    icon: '❓',
    suggest: '建议：\n• 重新启动小程序\n• 联系客服反馈问题'
  }
}

/**
 * 错误处理器类
 */
class ErrorHandler {
  /**
   * 解析错误类型
   * @param {Error} error 错误对象
   * @returns {String} 错误类型
   */
  parseErrorType(error) {
    const message = error.message || error.errMsg || ''

    // 网络连接失败
    if (message.includes('fail') ||
        message.includes('ERR_CONNECTION_REFUSED') ||
        message.includes('request:fail')) {
      return ErrorType.NETWORK_ERROR
    }

    // 请求超时
    if (message.includes('timeout')) {
      return ErrorType.TIMEOUT
    }

    // 认证错误
    if (message.includes('登录') || message.includes('401')) {
      return ErrorType.AUTH_ERROR
    }

    // 权限错误
    if (message.includes('权限') || message.includes('403')) {
      return ErrorType.PERMISSION_ERROR
    }

    // 资源不存在
    if (message.includes('不存在') || message.includes('404')) {
      return ErrorType.NOT_FOUND
    }

    // 服务器错误
    if (message.includes('服务器') || message.includes('500')) {
      return ErrorType.SERVER_ERROR
    }

    return ErrorType.UNKNOWN
  }

  /**
   * 获取错误配置
   * @param {String} errorType 错误类型
   * @param {String} customMessage 自定义消息
   * @returns {Object} 错误配置
   */
  getErrorConfig(errorType, customMessage = '') {
    const config = ErrorMessages[errorType] || ErrorMessages[ErrorType.UNKNOWN]

    return {
      ...config,
      message: customMessage || config.message
    }
  }

  /**
   * 显示Toast提示（轻量级）
   * @param {String} title 标题
   * @param {String} message 消息
   */
  showToast(title, message = '') {
    wx.showToast({
      title: title || '操作失败',
      icon: 'none',
      duration: 2500,
      mask: true
    })
  }

  /**
   * 显示Modal弹窗（详细错误）
   * @param {String} errorType 错误类型
   * @param {String} customMessage 自定义消息
   * @param {Function} callback 确认回调
   */
  showModal(errorType, customMessage = '', callback = null) {
    const config = this.getErrorConfig(errorType, customMessage)

    wx.showModal({
      title: config.title,
      content: `${config.message}\n\n${config.suggest || ''}`,
      showCancel: true,
      cancelText: '关闭',
      confirmText: config.action || '重试',
      confirmColor: '#2563EB',
      success: (res) => {
        if (res.confirm && callback) {
          callback()
        }
      }
    })
  }

  /**
   * 显示完整错误页面（严重错误）
   * @param {String} errorType 错误类型
   * @param {String} customMessage 自定义消息
   * @param {Object} options 配置选项
   */
  showFullPage(errorType, customMessage = '', options = {}) {
    const config = this.getErrorConfig(errorType, customMessage)

    // 将错误信息存储到全局数据，供错误页面使用
    const errorPageData = {
      ...config,
      errorType,
      customMessage,
      ...options
    }

    // 跳转到错误页面
    wx.navigateTo({
      url: `/pages/error-page/error-page?data=${encodeURIComponent(JSON.stringify(errorPageData))}`
    })
  }

  /**
   * 统一错误处理入口
   * @param {Error} error 错误对象
   * @param {Object} options 处理选项
   */
  handle(error, options = {}) {
    const {
      showType = 'toast',      // toast/modal/full
      customMessage = '',
      onRetry = null,
      onError = null
    } = options

    console.error('[ErrorHandler]', error)

    const errorType = this.parseErrorType(error)

    // 认证错误特殊处理
    if (errorType === ErrorType.AUTH_ERROR) {
      this.handleAuthError()
      return
    }

    // 根据显示类型处理
    switch (showType) {
      case 'toast':
        this.showToast(
          this.getErrorConfig(errorType, customMessage).title,
          customMessage
        )
        break

      case 'modal':
        this.showModal(errorType, customMessage, onRetry)
        break

      case 'full':
        this.showFullPage(errorType, customMessage, {
          onRetry,
          ...options
        })
        break

      default:
        this.showToast('操作失败', customMessage)
    }

    // 执行错误回调
    if (onError) {
      onError(errorType, error)
    }
  }

  /**
   * 处理认证错误
   */
  handleAuthError() {
    const config = this.getErrorConfig(ErrorType.AUTH_ERROR)

    wx.showModal({
      title: config.title,
      content: config.message,
      showCancel: false,
      confirmText: config.action,
      confirmColor: '#2563EB',
      success: () => {
        // 清除登录信息
        wx.removeStorageSync('token')
        wx.removeStorageSync('userInfo')

        // 跳转到登录页
        wx.reLaunch({
          url: '/pages/login/login'
        })
      }
    })
  }

  /**
   * 简化的网络错误提示
   */
  showNetworkError() {
    this.handle(new Error('request:fail'), { showType: 'toast' })
  }

  /**
   * 简化的超时错误提示
   */
  showTimeoutError() {
    this.handle(new Error('request timeout'), { showType: 'toast' })
  }

  /**
   * 简化的服务器错误提示
   */
  showServerError() {
    this.handle(new Error('server error'), { showType: 'toast' })
  }
}

// 创建实例并导出
const errorHandler = new ErrorHandler()
module.exports = errorHandler
