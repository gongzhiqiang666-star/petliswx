// pages/report-view/report-view.js
const api = require('../../api/index.js');

Page({
  data: {
    reportId: '',
    report: null,
    loading: true,
    error: false
  },

  onLoad(options) {
    const reportId = options.id;
    if (!reportId) {
      wx.showToast({
        title: '报告ID不存在',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
      return;
    }

    this.setData({ reportId });
    this.loadReport(reportId);
  },

  async loadReport(reportId) {
    try {
      wx.showLoading({
        title: '加载中...',
        mask: true
      });

      const report = await api.reports.getDetail(reportId);

      const formatDate = value => {
        if (!value) return '';
        const date = new Date(value);
        // 旧库用 1900-01-01 表示未填写，不作为真实日期展示
        if (isNaN(date.getTime()) || date.getFullYear() <= 1901) return '';
        const pad = n => String(n).padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
      };
      const stripHtml = value => String(value || '')
        .replace(/<[^>]*>/g, ' ')
        .replace(/&nbsp;/gi, ' ')
        .replace(/&amp;/gi, '&')
        .replace(/&lt;/gi, '<')
        .replace(/&gt;/gi, '>')
        .replace(/&quot;/gi, '"')
        .replace(/&#39;/g, "'")
        .replace(/\s+/g, ' ')
        .trim();
      // 接口字段（ReportVO）与页面展示字段的视图映射
      const view = {
        ...report,
        id: report.reportNO || report.genterNO || String(report.reportId || ''),
        petName: report.animalName || '',
        testDate: formatDate(report.collectDt),
        reportDate: formatDate(report.sendReportDt) || formatDate(report.createTime),
        fileName: `报告_${report.genterNO || report.reportId || ''}.pdf`,
        statusText: report.statusText || (Number(report.diagnosisStatus) ? '已完成' : '检测中'),
        reportSummary: stripHtml(report.reportResult) || stripHtml(report.mailReport) || '详见 PDF 报告'
      };
      this.setData({
        report: view,
        loading: false,
        error: false
      });

      wx.hideLoading();
    } catch (err) {
      console.error('[ReportView] 加载报告失败:', err);
      this.setData({
        loading: false,
        error: true
      });

      wx.hideLoading();
      wx.showToast({
        title: err.message || '加载失败，请重试',
        icon: 'none',
        duration: 2000
      });
    }
  },

  async onDownload() {
    if(this.data.report && this.data.report.paymentRestricted){wx.showToast({title:'订单未支付，报告暂未开放',icon:'none'});return}
    wx.showLoading({title:'下载中...',mask:true});
    try{await require('../../utils/reports').openReportById(this.data.reportId)}catch(e){wx.showToast({title:e.message || '报告暂不可用',icon:'none'})}finally{wx.hideLoading()}
  },
  onViewPDF(){
    if(this.data.report && this.data.report.paymentRestricted){wx.showToast({title:'订单未支付，报告暂未开放',icon:'none'});return}
    wx.navigateTo({url:'/pages/pdf-view/pdf-view?id='+this.data.reportId})
  },
  onPayOrder(){
    if(this.data.report && this.data.report.orderId)wx.navigateTo({url:'/pages/order-detail/order-detail?id='+this.data.report.orderId})
  },

  onRetry() {
    if (this.data.reportId) {
      this.loadReport(this.data.reportId);
    }
  }
});
