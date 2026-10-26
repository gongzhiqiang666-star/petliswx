/**
 * 小程序配置文件
 * 用于配置不同环境下的API地址和其他参数
 */

// 检测当前环境
// 开发环境：使用localhost
// 生产环境：使用实际域名
const getBaseURL = () => {
  // 开发环境 - 后端服务地址
  // 模拟器调试使用 localhost
  // return 'http://127.0.0.1:8082/api'

  // 真机调试时使用局域网IP（自动获取本机IP）
  // 如果真机仍无法访问，请手动修改IP为您的电脑局域网IP
  return 'http://192.168.0.3:8082/api'

  // 生产环境（部署时取消注释）
  // return 'https://your-domain.com/api'
}

module.exports = {
  // 后端API基础地址
  baseURL: getBaseURL(),

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
