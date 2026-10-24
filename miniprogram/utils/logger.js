/**
 * 统一日志工具
 * 根据环境变量控制日志输出
 */

const app = getApp()

/**
 * 普通日志
 * @param {String} tag - 日志标签
 * @param {String} message - 日志消息
 * @param {*} data - 附加数据
 */
function log(tag, message, data) {
  if (!app) return
  if (!app.globalData || !app.globalData.DEBUG) return

  if (data !== undefined) {
    console.log(`[${tag}]`, message, data)
  } else {
    console.log(`[${tag}]`, message)
  }
}

/**
 * 信息日志（蓝色）
 * @param {String} tag - 日志标签
 * @param {String} message - 日志消息
 * @param {*} data - 附加数据
 */
function info(tag, message, data) {
  if (!app) return
  if (!app.globalData || !app.globalData.DEBUG) return

  if (data !== undefined) {
    console.info(`[${tag}]`, message, data)
  } else {
    console.info(`[${tag}]`, message)
  }
}

/**
 * 警告日志（橙色）
 * @param {String} tag - 日志标签
 * @param {String} message - 日志消息
 * @param {*} data - 附加数据
 */
function warn(tag, message, data) {
  if (!app) return
  if (!app.globalData || !app.globalData.DEBUG) return

  if (data !== undefined) {
    console.warn(`[${tag}]`, message, data)
  } else {
    console.warn(`[${tag}]`, message)
  }
}

/**
 * 错误日志（红色）
 * @param {String} tag - 日志标签
 * @param {String} message - 日志消息
 * @param {*} data - 附加数据
 */
function error(tag, message, data) {
  // 错误日志始终输出，不受DEBUG控制
  if (data !== undefined) {
    console.error(`[${tag}]`, message, data)
  } else {
    console.error(`[${tag}]`, message)
  }
}

/**
 * 性能日志（记录时间差）
 * @param {String} tag - 日志标签
 * @param {String} message - 日志消息
 * @param {Number} startTime - 开始时间戳
 */
function performance(tag, message, startTime) {
  if (!app) return
  if (!app.globalData || !app.globalData.DEBUG) return

  const endTime = Date.now()
  const duration = endTime - startTime
  console.log(`[性能 ${tag}] ${message}，耗时: ${duration}ms`)
}

module.exports = {
  log,
  info,
  warn,
  error,
  performance
}
