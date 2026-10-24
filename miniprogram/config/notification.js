/**
 * 订阅消息配置
 *
 * 重要说明：
 * 1. 需要在微信公众平台配置订阅消息模板
 * 2. 获取模板ID后，替换下面的 TEMPLATE_ID
 * 3. 详见：https://mp.weixin.qq.com/ -> 功能 -> 订阅消息
 */

module.exports = {
  /**
   * 订单状态通知模板ID
   * @type {String}
   * @description 用于发送订单状态变更通知（待审核、检测中、完成等）
   *
   * 获取步骤：
   * 1. 登录微信公众平台 https://mp.weixin.qq.com/
   * 2. 进入"功能" -> "订阅消息"
   * 3. 点击"选用模板"，搜索"订单状态"或"审批通知"
   * 4. 选择合适的模板，点击"确定"
   * 5. 记录模板ID，格式如：'ABC123-DEF456-GHI789'
   */
  TEMPLATE_ID: 'YOUR_TEMPLATE_ID_HERE',  // ⚠️ 请替换为真实的模板ID

  /**
   * 是否启用订阅消息
   * @type {Boolean}
   * @description 开发环境可以设为 false，生产环境必须设为 true
   */
  ENABLED: true
}
