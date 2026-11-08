/**
 * 小程序配置文件
 * 用于配置不同环境下的API地址和其他参数
 */

const getEnvironment = () => {
  try {
    const version = wx.getAccountInfoSync().miniProgram.envVersion
    if (!['develop','trial','release'].includes(version)) throw new Error('无法识别小程序运行版本')
    return version
  } catch (e) {
    throw new Error('无法确认小程序运行版本，请重新打开小程序')
  }
}

const normalizeBaseURL = value => String(value || '').replace(/\/+$/, '').replace(/\/api$/, '') + '/api'

const getBaseURL = () => {
  const environment = getEnvironment()
  let extConfig = {}
  try { extConfig = wx.getExtConfigSync() || {} } catch (e) {}
  if (extConfig.baseURL) {
    if (typeof extConfig.baseURL !== 'string' || extConfig.baseURL !== extConfig.baseURL.trim() || !/^https?:\/\/[^\s/?#@]+(?:\/[^\s?#]*)?$/.test(extConfig.baseURL)) {
      throw new Error('接口地址配置无效')
    }
    if (environment !== 'develop' && !extConfig.baseURL.startsWith('https://')) throw new Error('体验版和正式版必须配置HTTPS接口')
    return normalizeBaseURL(extConfig.baseURL)
  }
  if (environment !== 'develop') throw new Error('请为体验版或正式版配置独立的HTTPS baseURL')
  return 'http://127.0.0.1:8082/api'
}

const environment = getEnvironment()
module.exports = {
  // 后端API基础地址
  baseURL: getBaseURL(),
  environment,
  allowMockPayment: environment === 'develop',

  // API版本
  apiVersion: 'v1',

  // 请求超时时间（毫秒）
  timeout: 30000,

  // 存储键名
  storageKeys: {
    TOKEN: 'token',
    USER_INFO: 'userInfo',
    HOSPITAL_ID: 'hospitalId'
  },

  // 状态码映射
  statusCode: {
    SUCCESS: 200,
    UNAUTHORIZED: 401,
    FORBIDDEN: 403,
    NOT_FOUND: 404,
    SERVER_ERROR: 500
  }
}
