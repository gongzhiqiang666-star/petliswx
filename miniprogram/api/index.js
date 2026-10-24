/**
 * PETLIS API接口封装
 * 所有后端API接口的统一调用入口
 */
const request = require('../utils/request.js')

/**
 * 用户认证相关API
 */
const auth = {
  /**
   * 微信登录
   * @param {String} code 微信登录code
   */
  wechatLogin: (code) => request.post('/auth/wechat/login', { code }, false),  // false = 不需要token

  /**
   * 刷新token
   * @param {String} token 旧的token
   */
  refreshToken: (token) => request.post('/auth/refresh', { token }, false),  // false = 不需要token

  /**
   * 登出
   */
  logout: () => request.post('/auth/logout', {}, false)  // false = 不需要token
}

/**
 * 宠物管理相关API
 */
const pets = {
  /**
   * 获取宠物列表
   * @param {Object} params 查询参数 {pageNum, pageSize}
   */
  getList: (params) => request.get('/pets', params),

  /**
   * 获取宠物详情
   * @param {Number} id 宠物ID
   */
  getDetail: (id) => request.get(`/pets/${id}`),

  /**
   * 搜索宠物
   * @param {String} name 宠物名称（中文名或英文名）
   */
  search: (name) => request.get('/pets/search', { name }),

  /**
   * 创建宠物档案
   * @param {Object} data 宠物信息
   */
  create: (data) => request.post('/pets', data),

  /**
   * 更新宠物档案
   * @param {Number} id 宠物ID
   * @param {Object} data 宠物信息
   */
  update: (id, data) => request.put(`/pets/${id}`, data),

  /**
   * 删除宠物（逻辑删除）
   * @param {Number} id 宠物ID
   */
  delete: (id) => request.delete(`/pets/${id}`)
}

/**
 * 检测申请（订单）相关API
 */
const orders = {
  /**
   * 获取订单列表
   * @param {Object} params 查询参数 {pageNum, pageSize, status, startDate, endDate}
   */
  getList: (params) => request.get('/orders', params),

  /**
   * 获取订单详情
   * @param {Number} id 订单ID
   */
  getDetail: (id) => request.get(`/orders/${id}`),

  /**
   * 创建检测申请
   * @param {Object} data 订单信息
   */
  create: (data) => request.post('/orders', data),

  /**
   * 更新检测申请
   * @param {Number} id 订单ID
   * @param {Object} data 订单信息
   */
  update: (id, data) => request.put(`/orders/${id}`, data),

  /**
   * 更新订单状态
   * @param {Number} id 订单ID
   * @param {Number} status 状态
   */
  updateStatus: (id, status) => request.put(`/orders/${id}/status?status=${status}`),

  /**
   * 添加检测项目
   * @param {Number} id 订单ID
   * @param {Object} item 检测项目信息
   */
  addItem: (id, item) => request.post(`/orders/${id}/items`, item),

  /**
   * 删除检测项目
   * @param {Number} id 订单ID
   * @param {Number} itemId 检测项目ID
   */
  deleteItem: (id, itemId) => request.delete(`/orders/${id}/items/${itemId}`),

  /**
   * 计算订单总费用
   * @param {Number} id 订单ID
   */
  calculateFee: (id) => request.get(`/orders/${id}/fee`)
}

/**
 * 报告查询相关API
 */
const reports = {
  /**
   * 获取报告列表
   * @param {Object} params 查询参数 {pageNum, pageSize, petId, status, startDate, endDate}
   */
  getList: (params) => request.get('/reports', params),

  /**
   * 获取报告详情
   * @param {Number} id 报告ID
   */
  getDetail: (id) => request.get(`/reports/${id}`),

  /**
   * 下载PDF报告
   * @param {Number} id 报告ID
   */
  downloadPdf: (id) => request.get(`/reports/${id}/download`),

  /**
   * 获取PDF下载URL
   * @param {Number} id 报告ID
   */
  getPdfUrl: (id) => request.get(`/reports/${id}/pdf-url`)
}

/**
 * 数据字典相关API
 */
const dictionary = {
  /**
   * 获取动物物种列表
   */
  getAnimals: () => request.get('/dictionary/animals'),

  /**
   * 获取品种列表
   * @param {Number} animalId 动物物种ID（可选）
   */
  getVarieties: (animalId) => request.get('/dictionary/varieties', { animalId }),

  /**
   * 获取性别列表
   */
  getSexes: () => request.get('/dictionary/sexes'),

  /**
   * 获取样本类型列表
   */
  getSampleTypes: () => request.get('/dictionary/sample-types'),

  /**
   * 获取检测项目列表
   */
  getTestTypes: () => request.get('/dictionary/test-types'),

  /**
   * 分页查询检测项目（支持关键词搜索）
   * @param {Object} params 查询参数 {keyword, page, size}
   */
  getTestTypesPaginated: (params) => request.get('/dictionary/test-types/paginated', params),

  /**
   * 获取检测项目价格
   * @param {Number} id 检测项目ID
   * @param {Number} cityId 城市ID（可选）
   */
  getTestTypePrice: (id, cityId) => request.get(`/dictionary/test-types/${id}/price`, { cityId }),

  /**
   * 获取城市列表
   */
  getCities: () => request.get('/dictionary/cities'),

  /**
   * 获取医院列表
   */
  getHospitals: () => request.get('/dictionary/hospitals'),

  /**
   * 根据ID获取单个医院信息
   * @param {Number} id 医院ID
   */
  getHospitalById: (id) => request.get(`/dictionary/hospitals/${id}`)
}

/**
 * 用户管理相关API
 */
const user = {
  /**
   * 获取当前用户信息
   */
  getProfile: () => request.get('/user/profile'),

  /**
   * 更新用户信息
   * @param {Object} data 用户信息 {nickname, avatarUrl, mobile, email, gender, birthday, bio}
   */
  updateProfile: (data) => request.put('/user/profile', data),

  /**
   * 上传头像
   * @param {String} filePath 本地文件路径
   */
  uploadAvatar: (filePath) => request.uploadFile('/user/avatar', filePath, 'file')
}

/**
 * 账单管理相关API
 */
const billing = {
  /**
   * 检查月结客户支付状态
   */
  checkStatus: () => request.get('/billing/check')
}

/**
 * 微信支付相关API
 */
const payment = {
  /**
   * 创建支付订单
   * @param {Object} data 支付信息 {orderId, amount}
   */
  create: (data) => request.post('/payment/create', data),

  /**
   * 查询支付状态
   * @param {String} paymentNo 支付编号
   */
  queryStatus: (paymentNo) => request.get('/payment/query', { paymentNo })
}

/**
 * 消息通知相关API
 */
const notifications = {
  /**
   * 获取当前医院的通知列表（分页）
   * @param {Object} params 查询参数 {pageNum, pageSize}
   */
  getMyHospitalNotifications: (params) => request.get('/notifications/hospitals/my', params),

  /**
   * 获取未读通知数量
   */
  getUnreadCount: () => request.get('/notifications/hospitals/my/unread-count'),

  /**
   * 获取通知详情
   * @param {Number} id 通知ID
   */
  getDetail: (id) => request.get(`/notifications/${id}`),

  /**
   * 标记通知为已读
   * @param {Number} id 通知ID
   */
  markAsRead: (id) => request.put(`/notifications/${id}/read`, {}),

  /**
   * 重试发送失败的通知
   * @param {Number} id 通知ID
   */
  retry: (id) => request.post(`/notifications/${id}/retry`, {})
}

module.exports = {
  auth,
  pets,
  orders,
  reports,
  dictionary,
  user,
  billing,
  payment,
  notifications
}
