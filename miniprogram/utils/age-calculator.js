/**
 * 年龄计算工具
 * 用于计算宠物基于生日的当前年龄
 */

/**
 * 计算宠物年龄
 * @param {String} birthday - 宠物生日,格式: "YYYY-MM-DD"
 * @returns {String} 年龄字符串 "X岁Y个月" | "X个月" | "未知"
 */
function calculateAge(birthday) {
  // 如果没有生日信息，返回"未知"
  if (!birthday) {
    return "未知";
  }

  const birth = new Date(birthday);
  const today = new Date();

  // 检查日期是否有效
  if (isNaN(birth.getTime())) {
    return "未知";
  }

  // 计算年份差和月份差
  let years = today.getFullYear() - birth.getFullYear();
  let months = today.getMonth() - birth.getMonth();
  let days = today.getDate() - birth.getDate();

  // 如果天数差为负，从月份借位
  if (days < 0) {
    months--;
    // 获取上个月的天数
    const lastMonth = new Date(today.getFullYear(), today.getMonth(), 0);
    days += lastMonth.getDate();
  }

  // 如果月份差为负，借位（年份减1，月份加12）
  if (months < 0) {
    years--;
    months += 12;
  }

  // 如果小于1岁，只显示月份
  if (years < 1) {
    // 至少显示1个月
    return `${months === 0 ? 1 : months}个月`;
  } else {
    // 大于等于1岁，显示岁和月
    return `${years}岁${months}个月`;
  }
}

/**
 * 计算宠物年龄（返回age-picker兼容格式）
 * @param {String} birthday - 宠物生日,格式: "YYYY-MM-DD"
 * @returns {String} 年龄字符串 "XYM" | "XY" | "XM" | ""
 * 例如: "1Y7M" 表示1岁7个月, "3Y" 表示3岁, "5M" 表示5个月
 */
function calculateAgeForPicker(birthday) {
  // 如果没有生日信息，返回空字符串
  if (!birthday) {
    return "";
  }

  const birth = new Date(birthday);
  const today = new Date();

  // 检查日期是否有效
  if (isNaN(birth.getTime())) {
    return "";
  }

  // 计算年份差和月份差
  let years = today.getFullYear() - birth.getFullYear();
  let months = today.getMonth() - birth.getMonth();
  let days = today.getDate() - birth.getDate();

  // 如果天数差为负，从月份借位
  if (days < 0) {
    months--;
    // 获取上个月的天数
    const lastMonth = new Date(today.getFullYear(), today.getMonth(), 0);
    days += lastMonth.getDate();
  }

  // 如果月份差为负，借位（年份减1，月份加12）
  if (months < 0) {
    years--;
    months += 12;
  }

  // 格式化为 age-picker 兼容格式
  if (years > 0 && months > 0) {
    // X岁Y个月：XYM
    return `${years}Y${months}M`;
  } else if (years > 0) {
    // X岁0个月：XY
    return `${years}Y`;
  } else {
    // 0岁Y个月：XM（至少1个月）
    // 如果是0个月，默认为1个月（宠物刚出生）
    return `${months === 0 ? 1 : months}M`;
  }
}

module.exports = {
  calculateAge,
  calculateAgeForPicker
}
