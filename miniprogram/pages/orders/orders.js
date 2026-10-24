// pages/orders/orders.js
const api = require('../../api/index.js')
const app = getApp()
const logger = require('../../utils/logger.js')

Page({
  data: {
    activeTab: 'all',
    tabs: [
      { id: 'all', label: '全部' },
      { id: 'draft', label: '草稿' },  // 合并 -3（待支付）和 -2（草稿）
      { id: '0', label: '待审核' },
      { id: '2', label: '等待报告' },
      { id: '3', label: '已完成' }
    ],
    orders: [],
    filteredOrders: [],
    loading: false,
    isEmpty: false,
    pageNum: 1,
    pageSize: 10,
    hasMore: true,
    isLoadingMore: false,
    hospitalId: null,  // 当前医院ID
    searchKeyword: ''  // 搜索关键字
  },

  onLoad() {
    // 获取当前医院ID
    const hospitalId = app.getHospitalId()
    this.setData({ hospitalId })
    this.loadOrders()
    this._lastLoadTime = Date.now() // 记录首次加载时间
  },

  onShow() {
    // 性能优化：只在距离上次加载超过5分钟时才刷新
    const lastLoadTime = this._lastLoadTime || 0
    const now = Date.now()
    const REFRESH_INTERVAL = 5 * 60 * 1000 // 5分钟

    if (now - lastLoadTime > REFRESH_INTERVAL || this.data.orders.length === 0) {
      this.resetAndLoadOrders()
      this._lastLoadTime = now
    }
  },

  /**
   * 重置并加载订单列表
   */
  resetAndLoadOrders() {
    this.setData({
      pageNum: 1,
      orders: [],
      hasMore: true
    })
    this.loadOrders()
  },

  /**
   * 加载订单列表（第一页）
   */
  async loadOrders() {
    // 防止重复加载
    if (this.data.loading) {
      return
    }

    this.setData({ loading: true })

    try {
      // 构建查询参数，添加医院ID过滤
      const params = {
        page: 0,  // Spring Data JPA page starts from 0
        size: this.data.pageSize
      }

      // 如果有医院ID，添加到查询参数
      if (this.data.hospitalId) {
        params.hospitalId = this.data.hospitalId
        logger.log('Orders', '加载医院订单', { hospitalId: this.data.hospitalId })
      }

      const result = await api.orders.getList(params)

      // 处理返回的数据
      const orderList = result.content || result.list || result || []

      logger.log('Orders', '加载订单成功', {
        totalElements: result.totalElements,
        totalPages: result.totalPages,
        last: result.last,
        first: result.first,
        currentPage: orderList.length,
        hasMore: !result.last,
        hospitalId: this.data.hospitalId
      })

      this.setData({
        orders: orderList,
        loading: false,
        isEmpty: orderList.length === 0,
        hasMore: !result.last  // 使用后端返回的 last 字段判断
      })

      // 根据当前选中的tab筛选订单
      this.filterOrders()
    } catch (err) {
      logger.error('Orders', '加载订单列表失败', err)
      this.setData({
        loading: false,
        isEmpty: false
      })

      // 网络错误提示
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    }
  },

  /**
   * 加载更多订单
   */
  async loadMore() {
    // 防止重复加载
    if (this.data.isLoadingMore || !this.data.hasMore) {
      return
    }

    this.setData({ isLoadingMore: true })

    try {
      const nextPage = this.data.pageNum + 1

      // 构建查询参数，添加医院ID过滤
      const params = {
        page: nextPage - 1,  // Spring Data JPA page starts from 0
        size: this.data.pageSize
      }

      // 如果有医院ID，添加到查询参数
      if (this.data.hospitalId) {
        params.hospitalId = this.data.hospitalId
      }

      const result = await api.orders.getList(params)

      // 处理返回的数据
      const newOrders = result.content || result.list || result || []

      logger.log('Orders', '加载更多订单成功', {
        pageNum: nextPage,
        newOrdersCount: newOrders.length,
        last: result.last,
        hasMore: !result.last
      })

      this.setData({
        orders: this.data.orders.concat(newOrders),
        pageNum: nextPage,
        isLoadingMore: false,
        hasMore: !result.last  // 使用后端返回的 last 字段判断
      })

      // 重新筛选订单
      this.filterOrders()
    } catch (err) {
      logger.error('Orders', '加载更多订单失败', err)
      this.setData({ isLoadingMore: false })

      // 网络错误提示
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    }
  },

  /**
   * 页面滚动到底部时触发
   */
  onReachBottom() {
    logger.log('Orders', '触发加载更多', {
      hasMore: this.data.hasMore,
      isLoadingMore: this.data.isLoadingMore,
      pageNum: this.data.pageNum
    })
    this.loadMore()
  },

  /**
   * 筛选订单（优化版：避免不必要的数据复制，支持搜索）
   */
  filterOrders() {
    const { activeTab, orders, searchKeyword } = this.data

    // 性能优化：只在tab切换或订单数量变化或搜索关键字变化时重新筛选
    if (this._lastActiveTab === activeTab &&
        this._lastOrdersLength === orders.length &&
        this._lastSearchKeyword === searchKeyword) {
      return
    }

    this._lastActiveTab = activeTab
    this._lastOrdersLength = orders.length
    this._lastSearchKeyword = searchKeyword

    // 性能优化：直接过滤，不复制对象
    let filtered = activeTab === 'all'
      ? orders
      : activeTab === 'draft'
        ? orders.filter(order => order.sampleStatus === -3 || order.sampleStatus === -2)
        : orders.filter(order => String(order.sampleStatus) === activeTab)

    // 如果有搜索关键字，进行搜索过滤
    if (searchKeyword && searchKeyword.trim()) {
      const keyword = searchKeyword.trim().toLowerCase()
      filtered = filtered.filter(order => {
        // 搜索订单号
        if (order.userNO && order.userNO.toLowerCase().includes(keyword)) {
          return true
        }
        // 搜索宠物名字
        if (order.animalName && order.animalName.toLowerCase().includes(keyword)) {
          return true
        }
        // 搜索畜主名字
        if (order.masterName && order.masterName.toLowerCase().includes(keyword)) {
          return true
        }
        return false
      })
    }

    // 格式化日期字段
    filtered = filtered.map(order => ({
      ...order,
      collectDt: this.formatDate(order.collectDt),
      createDtText: this.formatDateTime(order.createDt),
      statusLabel: this.getStatusLabel(order.sampleStatus)
    }))

    // 只设置必要的数据
    this.setData({
      filteredOrders: filtered
    })
  },

  /**
   * 搜索输入处理
   */
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({
      searchKeyword: keyword
    })
    // 重新筛选订单
    this.filterOrders()
  },

  /**
   * 格式化日期时间（显示创建时间）
   */
  formatDateTime(dateStr) {
    if (!dateStr) return '';

    let date;
    // 处理时间戳（数字）
    if (typeof dateStr === 'number') {
      date = new Date(dateStr);
    }
    // 处理时间戳字符串
    else if (typeof dateStr === 'string' && /^\d+$/.test(dateStr)) {
      date = new Date(parseInt(dateStr));
    }
    // 处理字符串日期
    else {
      // 替换空格为T，使其符合ISO格式 "yyyy-MM-ddTHH:mm:ss"
      const isoStr = dateStr.replace(' ', 'T');
      date = new Date(isoStr);
    }

    if (isNaN(date.getTime())) return '';

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}`;
  },

  /**
   * 格式化日期（兼容 iOS，添加缓存优化）
   */
  formatDate(dateStr) {
    if (!dateStr) return '';

    // 性能优化：使用缓存避免重复计算
    if (!this._dateCache) {
      this._dateCache = {};
    }

    if (this._dateCache[dateStr]) {
      return this._dateCache[dateStr];
    }

    // 处理 "2025-12-29 00:00:00" 格式 -> "2025-12-29T00:00:00"
    let formattedDateStr = dateStr;
    if (dateStr.includes(' ') && !dateStr.includes('T')) {
      formattedDateStr = dateStr.replace(' ', 'T');
    }

    const date = new Date(formattedDateStr);
    let formatted = '';

    if (isNaN(date.getTime())) {
      // 如果日期解析失败，直接返回原始字符串的前10位（YYYY-MM-DD部分）
      formatted = dateStr.substring(0, 10);
    } else {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      formatted = `${year}-${month}-${day}`;
    }

    // 缓存结果
    this._dateCache[dateStr] = formatted;
    return formatted;
  },

  /**
   * 获取状态标签
   */
  getStatusLabel(status) {
    const labels = {
      '-3': '草稿',  // 待支付改为草稿
      '-2': '草稿',
      '0': '待审核',
      '1': '审核不通过',
      '2': '等待报告',
      '3': '已完成',
      '4': '已取消'
    }
    return labels[String(status)] || '未知'
  },

  /**
   * 切换Tab
   */
  onTabChange(e) {
    const tabId = e.currentTarget.dataset.id
    this.setData({
      activeTab: tabId
    })
    this.filterOrders()
  },

  /**
   * 查看详情
   */
  onViewDetail(e) {
    const orderId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/order-detail/order-detail?id=${orderId}`
    })
  },

  /**
   * 报告预览
   */
  async onPreviewReport(e) {
    const orderId = e.currentTarget.dataset.id

    wx.showLoading({ title: '加载中...' })

    try {
      // 获取PDF URL
      const result = await api.reports.getPdfUrl(orderId)
      const pdfUrl = result.pdfUrl || result.url || result

      if (!pdfUrl) {
        throw new Error('未获取到报告地址')
      }

      wx.hideLoading()

      // 下载文件到本地临时目录
      wx.downloadFile({
        url: pdfUrl,
        success: (res) => {
          if (res.statusCode === 200) {
            const filePath = res.tempFilePath
            // 打开文档预览
            wx.openDocument({
              filePath: filePath,
              fileType: 'pdf',
              success: () => {
                logger.log('Orders', '报告预览成功')
              },
              fail: (err) => {
                logger.error('Orders', '打开报告失败', err)
                wx.showToast({
                  title: '打开报告失败',
                  icon: 'none'
                })
              }
            })
          } else {
            wx.showToast({
              title: '下载报告失败',
              icon: 'none'
            })
          }
        },
        fail: (err) => {
          logger.error('Orders', '下载报告失败', err)
          wx.showToast({
            title: '下载报告失败',
            icon: 'none'
          })
        }
      })
    } catch (err) {
      logger.error('Orders', '获取报告URL失败', err)
      wx.hideLoading()
      wx.showToast({
        title: err.message || '获取报告失败',
        icon: 'none'
      })
    }
  },

  /**
   * 报告下载
   */
  async onDownloadReport(e) {
    const orderId = e.currentTarget.dataset.id

    wx.showLoading({ title: '下载中...' })

    try {
      // 获取PDF URL
      const result = await api.reports.getPdfUrl(orderId)
      const pdfUrl = result.pdfUrl || result.url || result

      if (!pdfUrl) {
        throw new Error('未获取到报告地址')
      }

      // 下载文件到本地临时目录
      wx.downloadFile({
        url: pdfUrl,
        success: (res) => {
          if (res.statusCode === 200) {
            wx.hideLoading()

            // 获取订单信息用于文件名
            const order = this.data.filteredOrders.find(o => o.orderId === orderId)
            const petName = order ? order.animalName : '宠物'
            const collectDate = order ? order.collectDt : new Date().toISOString().slice(0, 10)
            const fileName = `检测报告_${petName}_${collectDate}.pdf`

            // 保存文件到本地
            wx.saveFile({
              tempFilePath: res.tempFilePath,
              success: (saveRes) => {
                wx.showToast({
                  title: '下载成功',
                  icon: 'success'
                })
                logger.log('Orders', '报告下载成功', { savedFilePath: saveRes.savedFilePath })
              },
              fail: (err) => {
                logger.error('Orders', '保存报告失败', err)
                wx.showToast({
                  title: '保存失败',
                  icon: 'none'
                })
              }
            })
          } else {
            wx.hideLoading()
            wx.showToast({
              title: '下载报告失败',
              icon: 'none'
            })
          }
        },
        fail: (err) => {
          logger.error('Orders', '下载报告失败', err)
          wx.hideLoading()
          wx.showToast({
            title: '下载报告失败',
            icon: 'none'
          })
        }
      })
    } catch (err) {
      logger.error('Orders', '获取报告URL失败', err)
      wx.hideLoading()
      wx.showToast({
        title: err.message || '下载失败',
        icon: 'none'
      })
    }
  },

  /**
   * 新建订单
   */
  onNewOrder() {
    wx.navigateTo({
      url: '/pages/order-select-pet/order-select-pet'
    })
  },

  /**
   * 去支付（重新发起支付）
   */
  async onPayOrder(e) {
    const { id, amount } = e.currentTarget.dataset;

    logger.log('Orders', '发起支付', { orderId: id, amount })

    try {
      wx.showLoading({ title: '加载中...' });

      // 获取用户openid
      const app = getApp();
      const userInfo = app.globalData.userInfo;

      if (!userInfo || !userInfo.openid) {
        wx.hideLoading();
        wx.showModal({
          title: '支付失败',
          content: '用户信息异常，请重新登录',
          showCancel: false
        });
        return;
      }

      // 创建支付订单
      const paymentData = {
        orderId: id,
        amount: amount,
        openid: userInfo.openid
      };

      const paymentResult = await api.payment.create(paymentData);
      const paymentParams = paymentResult.data || paymentResult;

      logger.log('Orders', '支付参数', paymentParams);

      wx.hideLoading();

      // 调起微信支付
      wx.requestPayment({
        timeStamp: paymentParams.timeStamp,
        nonceStr: paymentParams.nonceStr,
        package: paymentParams.package,
        signType: paymentParams.signType || 'MD5',
        paySign: paymentParams.paySign,
        success: (res) => {
          logger.log('Orders', '微信支付成功', res);
          wx.showToast({
            title: '支付成功',
            icon: 'success',
            duration: 1500
          });

          // 刷新订单列表
          setTimeout(() => {
            this.resetAndLoadOrders();
          }, 1500);
        },
        fail: (err) => {
          logger.error('Orders', '微信支付失败', err);
          // 用户取消支付
          if (err.errMsg.includes('cancel')) {
            wx.showToast({
              title: '已取消支付',
              icon: 'none',
              duration: 1500
            });
          } else {
            wx.showModal({
              title: '支付失败',
              content: err.errMsg || '支付失败，请重试',
              showCancel: false
            });
          }
        }
      });
    } catch (error) {
      logger.error('Orders', '创建支付订单失败', error);
      wx.hideLoading();
      wx.showModal({
        title: '支付失败',
        content: error.message || '创建支付订单失败，请重试',
        showCancel: false
      });
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadOrders().then(() => {
      wx.stopPullDownRefresh()
    })
  }
})
