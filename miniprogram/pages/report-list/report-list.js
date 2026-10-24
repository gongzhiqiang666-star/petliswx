// pages/report-list/report-list.js
const api = require('../../api/index.js')

Page({
  data: {
    searchKeyword: '',
    reports: [],
    filteredReports: [],
    allReportsLoaded: [], // 所有已加载的报告（用于客户端筛选）
    loading: false,
    loadingMore: false,
    isEmpty: false,
    selectedReport: null,
    showModal: false,
    currentPage: 1,      // 当前页码（从1开始）
    pageSize: 20,         // 每页数量
    hasMore: true,        // 是否还有更多数据
    totalCount: 0         // 总数据量
  },

  onLoad() {
    this.loadReports()
  },

  onShow() {
    // 从其他页面返回时，如果是第一页则刷新列表
    if (this.data.currentPage === 1) {
      this.loadReports()
    }
  },

  /**
   * 加载报告列表（分页）
   * @param {Boolean} loadMore 是否为加载更多
   */
  async loadReports(loadMore = false) {
    // 防止重复加载
    if (this.data.loading || this.data.loadingMore) {
      return
    }

    // 如果没有更多数据，不再加载
    if (loadMore && !this.data.hasMore) {
      return
    }

    // 设置加载状态
    if (loadMore) {
      this.setData({ loadingMore: true })
    } else {
      // 首次加载或刷新，重置状态
      this.setData({
        loading: true,
        currentPage: 1,
        hasMore: true,
        allReportsLoaded: []
      })
    }

    try {
      const result = await api.reports.getList({
        pageNum: this.data.currentPage,
        pageSize: this.data.pageSize
      })

      // 处理返回的数据
      const reportList = result.list || result.content || result || []
      const totalCount = result.total || result.totalElements || 0

      // 判断是否还有更多数据
      const hasMore = (this.data.currentPage * this.data.pageSize) < totalCount

      // 为新加载的报告添加状态标签和格式化日期
      const reportsWithLabels = reportList.map(report => ({
        ...report,
        statusLabel: this.getStatusLabel(report.diagnosisStatus),
        samplingDate: this.formatDate(report.collectDt),
        reportDate: this.formatReportDate(report.sendMailDt, report.createDt),
        testTypeName: report.testTypeName || '-'
      }))

      // 合并数据
      let newAllReports
      if (loadMore) {
        newAllReports = [...this.data.allReportsLoaded, ...reportsWithLabels]
      } else {
        newAllReports = reportsWithLabels
      }

      // 根据搜索关键词筛选报告
      const filteredReports = this.filterReports(newAllReports)

      this.setData({
        reports: newAllReports,
        allReportsLoaded: newAllReports,
        filteredReports: filteredReports,
        loading: false,
        loadingMore: false,
        isEmpty: newAllReports.length === 0,
        hasMore: hasMore,
        totalCount: totalCount
      })

      console.log('[ReportList] 加载报告成功:', {
        page: this.data.currentPage,
        pageSize: this.data.pageSize,
        loadedCount: reportList.length,
        totalCount: totalCount,
        hasMore: hasMore
      })
    } catch (err) {
      console.error('[Reports] 加载报告列表失败:', err)
      this.setData({
        loading: false,
        loadingMore: false
      })

      // 网络错误提示
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      })
    }
  },

  /**
   * 解析日期为时间戳（iOS兼容）
   * 支持多种输入格式：时间戳、"yyyy-MM-dd"、"yyyy-MM-dd HH:mm:ss"等
   */
  parseDate(dateStr) {
    if (!dateStr) return 0;

    // 处理时间戳（数字）
    if (typeof dateStr === 'number') {
      return dateStr;
    }
    // 处理时间戳字符串
    if (typeof dateStr === 'string' && /^\d+$/.test(dateStr)) {
      return parseInt(dateStr);
    }
    // 处理字符串日期
    const isoStr = dateStr.replace(' ', 'T');
    const date = new Date(isoStr);
    return isNaN(date.getTime()) ? 0 : date.getTime();
  },

  /**
   * 筛选报告（客户端筛选）
   * @param {Array} reportsToFilter 要筛选的报告列表（默认使用 allReportsLoaded）
   */
  filterReports(reportsToFilter = null) {
    const reports = reportsToFilter || this.data.allReportsLoaded
    let filtered = [...reports]

    // 按关键词搜索
    if (this.data.searchKeyword) {
      const keyword = this.data.searchKeyword.toLowerCase()
      filtered = filtered.filter(r => {
        // 宠物名称搜索
        if (r.animalName && r.animalName.toLowerCase().includes(keyword)) {
          return true
        }
        // 检测项目搜索
        if (r.testTypeName && r.testTypeName.toLowerCase().includes(keyword)) {
          return true
        }
        return false
      })
    }

    // 按时间倒序排序（最新的在前）（iOS兼容）
    filtered.sort((a, b) => {
      const dateA = this.parseDate(a.createDt || a.sendMailDt || a.collectDt || 0)
      const dateB = this.parseDate(b.createDt || b.sendMailDt || b.collectDt || 0)
      return dateB - dateA
    })

    return filtered
  },

  /**
   * 滚动到底部加载更多
   */
  onScrollToLower() {
    console.log('[ReportList] 滚动到底部，加载更多')

    // 搜索状态下不加载更多（因为搜索是客户端筛选）
    if (this.data.searchKeyword) {
      return
    }

    // 如果没有更多数据或正在加载，不执行
    if (!this.data.hasMore || this.data.loading || this.data.loadingMore) {
      return
    }

    // 页码加1并加载更多
    this.setData({
      currentPage: this.data.currentPage + 1
    }, () => {
      this.loadReports(true)
    })
  },

  /**
   * 获取状态标签
   */
  getStatusLabel(status) {
    // diagnosisStatus=0表示检测中，其他表示已完成
    return String(status) === '0' ? '检测中' : '已完成'
  },

  /**
   * 格式化日期（iOS兼容格式）
   * 支持多种输入格式：时间戳、"yyyy-MM-dd"、"yyyy-MM-dd HH:mm:ss"等
   */
  formatDate(dateStr) {
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
      // 替换空格为T，使其符合ISO格式
      const isoStr = dateStr.replace(' ', 'T');
      date = new Date(isoStr);
    }

    if (isNaN(date.getTime())) return '';

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  /**
   * 格式化报告日期（优先显示发送日期，否则显示创建日期）
   */
  formatReportDate(sendMailDt, createDt) {
    // 优先使用报告发送日期
    if (sendMailDt) {
      const date = this.formatDate(sendMailDt);
      if (date) return date;
    }
    // 其次使用创建日期
    if (createDt) {
      return this.formatDate(createDt);
    }
    return '';
  },

  /**
   * 搜索输入（带防抖）
   */
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({
      searchKeyword: keyword
    })

    // 防抖处理
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
    }
    this.searchTimer = setTimeout(() => {
      // 客户端筛选
      const filtered = this.filterReports()
      this.setData({
        filteredReports: filtered
      })
    }, 400)
  },

  /**
   * 查看报告详情（弹窗）
   * 只有已完成的报告才显示详情弹窗
   */
  onViewReport(e) {
    const reportId = e.currentTarget.dataset.id
    const report = this.data.filteredReports.find(r => r.reportId === reportId)

    if (!report) {
      return
    }

    // 已完成，显示详情弹窗
    this.setData({
      selectedReport: report,
      showModal: true
    })
  },

  /**
   * 关闭弹窗
   */
  onCloseModal() {
    this.setData({
      showModal: false,
      selectedReport: null
    })
  },

  /**
   * 新建预约
   */
  onNewAppointment() {
    wx.navigateTo({
      url: '/pages/order-select-pet/order-select-pet'
    })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadReports(false).then(() => {
      wx.stopPullDownRefresh()
    })
  },

  /**
   * 预览PDF报告
   */
  onPreviewPDF(e) {
    const pdfUrl = e.currentTarget.dataset.url;
    if (!pdfUrl) {
      wx.showToast({
        title: 'PDF地址不存在',
        icon: 'none'
      });
      return;
    }

    console.log('[ReportList] 预览PDF:', pdfUrl);

    // 跳转到PDF预览页面
    wx.navigateTo({
      url: `/pages/pdf-view/pdf-view?pdf_url=${encodeURIComponent(pdfUrl)}`
    });
  },

  /**
   * 下载PDF报告
   */
  onDownloadPDF(e) {
    const pdfUrl = e.currentTarget.dataset.url;
    const reportName = e.currentTarget.dataset.name || '检测报告';

    if (!pdfUrl) {
      wx.showToast({
        title: 'PDF地址不存在',
        icon: 'none'
      });
      return;
    }

    console.log('[ReportList] 下载PDF:', pdfUrl);

    wx.showLoading({
      title: '下载中...',
      mask: true
    });

    // 下载PDF文件
    wx.downloadFile({
      url: pdfUrl,
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          // 打开PDF文档
          wx.openDocument({
            filePath: res.tempFilePath,
            fileType: 'pdf',
            showMenu: true,
            success: () => {
              console.log('[ReportList] PDF打开成功');
            },
            fail: (err) => {
              console.error('[ReportList] PDF打开失败:', err);
              wx.showToast({
                title: '打开失败',
                icon: 'none'
              });
            }
          });
        } else {
          wx.showToast({
            title: '下载失败',
            icon: 'none'
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('[ReportList] PDF下载失败:', err);
        wx.showToast({
          title: '下载失败',
          icon: 'none'
        });
      }
    });
  }
})
