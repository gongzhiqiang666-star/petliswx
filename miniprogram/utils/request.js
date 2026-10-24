/**
 * HTTP请求封装
 * 基于微信小程序wx.request封装的请求工具
 */
const config = require('../config/config.js')
const errorHandler = require('./error-handler.js')

/**
 * HTTP请求类
 */
class Request {
  constructor() {
    this.baseURL = config.baseURL
    this.timeout = config.timeout
  }

  /**
   * 获取请求头
   * @param {Boolean} needAuth 是否需要认证
   */
  getHeaders(needAuth = true) {
    const headers = {
      'Content-Type': 'application/json'
    }

    // 添加token
    if (needAuth) {
      const token = wx.getStorageSync(config.storageKeys.TOKEN)
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
    }

    return headers
  }

  /**
   * 处理响应
   * @param {Object} res 响应对象
   */
  handleResponse(res) {
    const { statusCode, data } = res

    // HTTP状态码检查
    if (statusCode !== 200) {
      // 处理401未授权 - 只清除本地数据，不自动跳转，避免循环
      if (statusCode === 401) {
        console.log('[Request] 401 未授权，清除本地登录信息')
        // 清除登录信息
        wx.removeStorageSync(config.storageKeys.TOKEN)
        wx.removeStorageSync(config.storageKeys.USER_INFO)
        wx.removeStorageSync(config.storageKeys.HOSPITAL_ID)

        // 不自动跳转，避免死循环
        // 让app.js的自然流程处理重新登录
        throw new Error('登录已过期')
      }

      // 处理403禁止访问
      if (statusCode === 403) {
        throw new Error('没有权限访问')
      }

      // 处理404未找到
      if (statusCode === 404) {
        throw new Error('请求的资源不存在')
      }

      // 处理500服务器错误
      if (statusCode >= 500) {
        throw new Error('服务器错误，请稍后重试')
      }

      throw new Error(`请求失败，状态码：${statusCode}`)
    }

    // 业务状态码检查
    if (data.code !== undefined && data.code !== 200) {
      throw new Error(data.message || '请求失败')
    }

    // 返回数据
    return data.data !== undefined ? data.data : data
  }

  /**
   * 处理错误
   * @param {Error} err 错误对象
   */
  handleError(err) {
    console.error('[Request Error]', err)

    // 网络错误
    if (err.errMsg) {
      if (err.errMsg.includes('timeout')) {
        throw new Error('请求超时，请检查网络连接')
      }
      if (err.errMsg.includes('fail')) {
        throw new Error('网络连接失败，请检查网络设置')
      }
    }

    throw err
  }

  /**
   * 显示错误提示（可选）
   * @param {Error} err 错误对象
   * @param {Object} options 处理选项
   */
  showError(err, options = {}) {
    // 使用错误处理器显示友好的错误提示
    errorHandler.handle(err, {
      showType: 'toast',  // 默认使用toast提示
      ...options
    })
  }

  /**
   * 通用请求方法
   * @param {String} method 请求方法
   * @param {String} url 请求地址
   * @param {Object} data 请求数据
   * @param {Boolean} needAuth 是否需要认证
   */
  request(method, url, data = {}, needAuth = true) {
    return new Promise((resolve, reject) => {
      // 完整URL
      const fullURL = /^https?:\/\//.test(url) ? url : `${this.baseURL}${url}`

      console.log(`[Request] ${method} ${fullURL}`, data)

      wx.request({
        method: method.toUpperCase(),
        url: fullURL,
        data: data,
        header: this.getHeaders(needAuth),
        timeout: this.timeout,
        success: (res) => {
          try {
            const result = this.handleResponse(res)
            console.log(`[Response]`, result)
            resolve(result)
          } catch (err) {
            reject(err)
          }
        },
        fail: (err) => {
          try {
            this.handleError(err)
          } catch (e) {
            reject(e)
          }
        }
      })
    })
  }

  /**
   * GET请求
   * @param {String} url 请求地址
   * @param {Object} params 查询参数
   * @param {Boolean} needAuth 是否需要认证
   */
  get(url, params = {}, needAuth = true) {
    // 将参数拼接到URL
    let fullURL = url
    if (Object.keys(params).length > 0) {
      const query = Object.keys(params)
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
        .join('&')
      fullURL = `${url}?${query}`
    }

    return this.request('GET', fullURL, {}, needAuth)
  }

  /**
   * POST请求
   * @param {String} url 请求地址
   * @param {Object} data 请求数据
   * @param {Boolean} needAuth 是否需要认证
   */
  post(url, data = {}, needAuth = true) {
    return this.request('POST', url, data, needAuth)
  }

  /**
   * PUT请求
   * @param {String} url 请求地址
   * @param {Object} data 请求数据
   * @param {Boolean} needAuth 是否需要认证
   */
  put(url, data = {}, needAuth = true) {
    return this.request('PUT', url, data, needAuth)
  }

  /**
   * DELETE请求
   * @param {String} url 请求地址
   * @param {Object} data 请求数据
   * @param {Boolean} needAuth 是否需要认证
   */
  delete(url, data = {}, needAuth = true) {
    return this.request('DELETE', url, data, needAuth)
  }

  /**
   * 上传文件
   * @param {String} url 上传地址
   * @param {String} filePath 本地文件路径
   * @param {String} name 文件字段名
   * @param {Object} formData 额外的表单数据
   */
  uploadFile(url, filePath, name = 'file', formData = {}) {
    return new Promise((resolve, reject) => {
      const fullURL = /^https?:\/\//.test(url) ? url : `${this.baseURL}${url}`

      console.log(`[Upload] ${fullURL}`, filePath)

      wx.uploadFile({
        url: fullURL,
        filePath: filePath,
        name: name,
        formData: formData,
        header: this.getHeaders(true),
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            const result = this.handleResponse({ statusCode: res.statusCode, data })
            console.log(`[Upload Response]`, result)
            resolve(result)
          } catch (err) {
            reject(err)
          }
        },
        fail: (err) => {
          try {
            this.handleError(err)
          } catch (e) {
            reject(e)
          }
        }
      })
    })
  }

  /**
   * 下载文件
   * @param {String} url 下载地址
   */
  downloadFile(url) {
    return new Promise((resolve, reject) => {
      const fullURL = /^https?:\/\//.test(url) ? url : `${this.baseURL}${url}`

      console.log(`[Download] ${fullURL}`)

      wx.downloadFile({
        url: fullURL,
        header: this.getHeaders(true),
        success: (res) => {
          if (res.statusCode === 200) {
            console.log(`[Download Success]`, res.tempFilePath)
            resolve(res.tempFilePath)
          } else {
            reject(new Error(`下载失败，状态码：${res.statusCode}`))
          }
        },
        fail: (err) => {
          try {
            this.handleError(err)
          } catch (e) {
            reject(e)
          }
        }
      })
    })
  }
}

// 创建实例并导出
const request = new Request()
module.exports = request
