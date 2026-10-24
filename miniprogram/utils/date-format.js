/**
 * 日期格式化工具函数
 * 用于处理前端与后端之间的日期格式转换
 */

/**
 * 将日期转换为API需要的格式 (YYYY-MM-DDTHH:mm:ss)
 * @param {String} dateStr - 日期字符串，格式为 YYYY-MM-DD
 * @returns {String} ISO 8601格式的日期字符串
 */
function formatDateTimeForAPI(dateStr) {
  if (!dateStr) return null;

  // 如果已经是 ISO 格式（包含T），直接返回
  if (dateStr.includes('T')) {
    return dateStr;
  }

  // 将 YYYY-MM-DD 转换为 YYYY-MM-DDT00:00:00
  return `${dateStr}T00:00:00`;
}

/**
 * 格式化日期为显示格式
 * @param {Date|String} date - 日期对象或日期字符串
 * @returns {String} 格式化后的日期字符串 YYYY-MM-DD
 */
function formatDate(date) {
  if (!date) return '';

  const d = new Date(date);
  if (isNaN(d.getTime())) return '';

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

/**
 * 格式化日期时间为显示格式
 * @param {Date|String} date - 日期对象或日期字符串
 * @returns {String} 格式化后的日期时间字符串 YYYY-MM-DD HH:mm:ss
 */
function formatDateTime(date) {
  if (!date) return '';

  const d = new Date(date);
  if (isNaN(d.getTime())) return '';

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

/**
 * 格式化时间戳为可读格式
 * @param {Number} timestamp - 时间戳（毫秒）
 * @returns {String} 格式化后的日期时间字符串
 */
function formatTimestamp(timestamp) {
  if (!timestamp) return '';
  return formatDateTime(new Date(timestamp));
}

module.exports = {
  formatDateTimeForAPI,
  formatDate,
  formatDateTime,
  formatTimestamp
};
