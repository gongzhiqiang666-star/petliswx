/**
 * 小程序配置文件
 * 用于配置不同环境下的API地址和其他参数
 */

const getEnvironment = () => {
  try {
    return wx.getAccountInfoSync().miniProgram.envVersion || 'develop'
  } catch (e) {
    return 'develop'
  }
}

const normalizeBaseURL = value => String(value || '').replace(/\/+$/, '').replace(/\/api$/, '') + '/api'

const getBaseURL = () => {
  const environment = getEnvironment()
  let extConfig = {}
  try { extConfig = wx.getExtConfigSync() || {} } catch (e) {}
  if (extConfig.baseURL) return normalizeBaseURL(extConfig.baseURL)
  if (environment === 'release') throw new Error('请在小程序 ext.json 配置生产环境 baseURL')
  return 'http://127.0.0.1:8082/api'
}

module.exports = {
  // 后端API基础地址
  baseURL: getBaseURL(),
  environment: getEnvironment(),
  allowMockPayment: getEnvironment() === 'develop',

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
