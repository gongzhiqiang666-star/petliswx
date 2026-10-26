// pages/orders/orders.js
const api = require('../../api/index.js')
const app = getApp()
const logger = require('../../utils/logger.js')

Page({
  data: {
    activeTab: 'all',
    tabs: [
      { id: 'all', label: '全部' },
      { id: 'draft', label: '草稿' },
      { id: -3, label: '待支付' },
      { id: '0', label: '待审核' },
      { id: '1', label: '未通过' },
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
    startDate:'',endDate:'',errorMessage:'',
    searchKeyword: '',  // 搜索关键字
    showFilters: false
  },

  onLoad() {
    // 获取当前医院ID
    const hospitalId = app.getHospitalId()
    this.setData({ hospitalId })
    this._lastLoadTime = Date.now() // 记录首次加载时间
  },

  onShow() {this.resetAndLoadOrders()},
  onUnload(){clearTimeout(this._searchTimer)},
  query(page){const tab=this.data.activeTab;return {page,size:this.data.pageSize,...(tab==='all'?{}:{status:tab==='draft'?-2:Number(tab)}),keyword:this.data.searchKeyword,startDate:this.data.startDate,endDate:this.data.endDate}},
  dateChange(e){this.setData({[e.currentTarget.dataset.key]:e.detail.value});this.resetAndLoadOrders()},
  clearDates(){this.setData({startDate:'',endDate:''});this.resetAndLoadOrders()},
  toggleFilters(){this.setData({showFilters:!this.data.showFilters})},

  /**
   * 重置并加载订单列表
   */
  resetAndLoadOrders() {
    this.setData({
      pageNum: 1,
      isLoadingMore:false,
      orders: [],
      hasMore: true
    })
    this.loadOrders()
  },

  /**
   * 加载订单列表（第一页）
   */
  async loadOrders() {
    const version=(this._queryVersion || 0)+1;this._queryVersion=version;

    this.setData({ loading: true,errorMessage:'' })

    try {
      const params=this.query(0)

      const result = await api.orders.getList(params)
      if(version!==this._queryVersion)return

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
      if(version!==this._queryVersion)return
      this.setData({errorMessage:err.message || '加载失败，请重试'})
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

    if(this.data.loading)return
    const version=this._queryVersion;
    this.setData({ isLoadingMore: true })

    try {
      const nextPage = this.data.pageNum + 1

      const params=this.query(nextPage-1)

      const result = await api.orders.getList(params)
      if(version!==this._queryVersion)return

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
      if(version!==this._queryVersion)return
      this.setData({errorMessage:err.message || '加载失败，请重试'})
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

    // 性能优化：直接过滤，不复制对象
    let filtered = activeTab === 'all'
      ? orders
      : activeTab === 'draft'
        ? orders.filter(order => order.sampleStatus === -2)
        : orders.filter(order => String(order.sampleStatus) === String(activeTab))

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
      filteredOrders: filtered,isEmpty:filtered.length===0
    })
  },

  /**
   * 搜索输入处理
   */
  onSearchInput(e) {this.setData({searchKeyword:e.detail.value});clearTimeout(this._searchTimer);this._searchTimer=setTimeout(()=>this.resetAndLoadOrders(),300)},

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
      '-3': '待支付',
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
    this.resetAndLoadOrders()
  },

  /**
   * 查看详情
   */
  openDrafts(){wx.navigateTo({url:'/pages/drafts/drafts'})},
  onViewDetail(e) {
    const orderId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/order-detail/order-detail?id=${orderId}`
    })
  },

  /**
   * 报告预览
   */
  onPreviewReport(e) {
    wx.navigateTo({url:'/pages/order-detail/order-detail?id='+e.currentTarget.dataset.id})
  },

  /**
   * 报告下载
   */
  onDownloadReport(e) {
    wx.navigateTo({url:'/pages/order-detail/order-detail?id='+e.currentTarget.dataset.id})
  },

  /**
   * 新建订单
   */
  onNewOrder() {
    wx.switchTab({
      url: '/pages/order-select-pet/order-select-pet'
    })
  },

  /**
   * 去支付（重新发起支付）
   */
  async onPayOrder(e) {
    if(this._paying)return;this._paying=true;wx.showLoading({title:'正在获取支付信息',mask:true});
    try{const confirmed=await require('../../utils/payment').payOrder(e.currentTarget.dataset.id);wx.showToast({title:confirmed?'支付已确认':'支付结果确认中',icon:confirmed?'success':'none'});this.resetAndLoadOrders()}catch(error){wx.showModal({title:'付款提示',content:error.message || '支付未完成，请稍后重试',showCancel:false})}finally{wx.hideLoading();this._paying=false}
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
