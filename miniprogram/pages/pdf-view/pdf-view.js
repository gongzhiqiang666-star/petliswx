// pages/pdf-view/pdf-view.js
Page({
  data: {
    pdfUrl: ''
  },

  onLoad(options) {
    const { pdf_url } = options;
    if (pdf_url) {
      // URL解码
      const decodedUrl = decodeURIComponent(pdf_url);
      console.log('[PdfView] PDF URL:', decodedUrl);

      this.setData({
        pdfUrl: decodedUrl
      });
    } else {
      wx.showToast({
        title: 'PDF地址不存在',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },

  onShareAppMessage() {
    return {
      title: '检测报告',
      path: '/pages/index/index'
    };
  }
});
