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
      const app=getApp();const token = (app && app.globalData && app.globalData.token) || wx.getStorageSync(config.storageKeys.TOKEN)
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
    }

    return headers
  }

  unauthorized() {
    wx.removeStorageSync(config.storageKeys.TOKEN)
    wx.removeStorageSync(config.storageKeys.USER_INFO)
    wx.removeStorageSync(config.storageKeys.HOSPITAL_ID)
    const app=getApp();if(app && app.globalData){app.globalData.token=null;app.globalData.userInfo=null;app.globalData.hospitalId=null;app.globalData.cityId=null}
    const pages=getCurrentPages();if(pages.length && pages[pages.length-1].route!=='pages/login/login' && !this._redirecting){this._redirecting=true;wx.reLaunch({url:'/pages/login/login',complete:()=>{this._redirecting=false}})}
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
        this.unauthorized()
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
    if (data.code === 401) this.unauthorized()
    if (data.code !== undefined && data.code !== 200) {
      const error=new Error(data.message || '请求失败')
      error.code=data.code
      throw error
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

      // Do not log credentials, tokens or patient data.

      wx.request({
        method: method.toUpperCase(),
        url: fullURL,
        data: data,
        header: this.getHeaders(needAuth),
        timeout: this.timeout,
        success: (res) => {
          try {
            const result = this.handleResponse(res)
            // Response bodies may contain private data.
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
  uploadFile(url, filePath, name = 'file', formData = {}, needAuth = true) {
    return new Promise((resolve, reject) => {
      const fullURL = /^https?:\/\//.test(url) ? url : `${this.baseURL}${url}`

      // Keep private file paths out of logs.

      wx.uploadFile({
        url: fullURL,
        filePath: filePath,
        name: name,
        formData: formData,
        header: Object.fromEntries(Object.entries(this.getHeaders(needAuth)).filter(([key]) => key !== 'Content-Type')),
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            const result = this.handleResponse({ statusCode: res.statusCode, data })
            // Keep uploaded qualification capabilities out of logs.
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

      wx.downloadFile({
        url: fullURL,
        header: this.getHeaders(Boolean(this.downloadOrigin(fullURL)) && this.downloadOrigin(fullURL)===this.downloadOrigin(this.baseURL)),
        success: (res) => {
          try {
            if(res.statusCode===401){this.unauthorized();throw new Error('登录已过期，请重新登录')}
            if(res.statusCode!==200)throw new Error(res.statusCode===403?'没有权限下载该文件':`下载失败，状态码：${res.statusCode}`)
            this.validateDownloadedFile(res.tempFilePath)
            resolve(res.tempFilePath)
          }catch(error){
            if(res.tempFilePath)try{wx.getFileSystemManager().unlinkSync(res.tempFilePath)}catch(e){}
            reject(error)
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

  downloadOrigin(value) {
    const match=String(value).match(/^(https?):\/\/([^/?#]+)/i)
    if(!match || match[2].includes('@'))return null
    const scheme=match[1].toLowerCase(),host=match[2].toLowerCase().replace(scheme==='https'?/:443$/:/:80$/,'')
    return scheme+'://'+host
  }

  validateDownloadedFile(filePath) {
    if(!filePath)throw new Error('下载文件不存在，请重试')
    const fs=wx.getFileSystemManager();let size,head
    try{size=Number(fs.statSync(filePath).size);if(!Number.isSafeInteger(size)||size<=0)throw new Error();head=new Uint8Array(fs.readFileSync(filePath,undefined,0,Math.min(size,512)))}catch(e){throw new Error('下载文件为空或无法读取，请重试')}
    let position=head[0]===239&&head[1]===187&&head[2]===191?3:0
    while([9,10,13,32].includes(head[position]))position++
    if(head[position]!==123&&head[position]!==91)return
    if(size>65536)throw new Error('下载响应不是有效文件，请重试')
    let body;try{body=JSON.parse(fs.readFileSync(filePath,'utf8',0,size).replace(/^\uFEFF/,''))}catch(e){throw new Error('下载响应无效，请重试')}
    if(body && !Array.isArray(body) && Object.prototype.hasOwnProperty.call(body,'code')){
      this.handleResponse({statusCode:200,data:body})
      throw new Error('服务器未返回可下载的文件，请稍后重试')
    }
  }
}

// 创建实例并导出
const request = new Request()
module.exports = request
