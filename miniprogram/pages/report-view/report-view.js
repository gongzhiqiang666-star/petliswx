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


      this.setData({
        report,
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

  onDownload() {
    wx.showToast({
      title: '下载功能开发中',
      icon: 'none'
    });
  },

  onViewPDF() {
    wx.showToast({
      title: 'PDF预览功能开发中',
      icon: 'none'
    });
  },

  onRetry() {
    if (this.data.reportId) {
      this.loadReport(this.data.reportId);
    }
  }
});
