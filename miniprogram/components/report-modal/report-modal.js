// components/report-modal/report-modal.js
const api = require('../../api/index.js')

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false
    },
    report: {
      type: Object,
      value: null
    }
  },

  data: {
    loading: false
  },

  methods: {
    onClose() {
      this.triggerEvent('close');
    },

    // 阻止冒泡，避免点击内容区域时关闭弹窗
    onContentTap() {
      // 空函数，阻止事件冒泡
    },

    /**
     * 报告预览
     */
    async onPreviewReport() {
      if (!this.data.report || !this.data.report.id) {
        wx.showToast({
          title: '报告信息无效',
          icon: 'none'
        })
        return
      }

      wx.showLoading({ title: '加载中...' })

      try {
        // 获取PDF URL
        const result = await api.reports.getPdfUrl(this.data.report.id)
        const pdfUrl = result.pdfUrl || result.url || result

        if (!pdfUrl) {
          throw new Error('未获取到报告地址')
        }

        wx.hideLoading()

        // 下载文件到本地
        wx.downloadFile({
          url: pdfUrl,
          success: (res) => {
            if (res.statusCode === 200) {
              const filePath = res.tempFilePath
              // 打开文档
              wx.openDocument({
                filePath: filePath,
                fileType: 'pdf',
                success: () => {
                  console.log('[ReportModal] 报告预览成功')
                },
                fail: (err) => {
                  console.error('[ReportModal] 打开报告失败:', err)
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
            console.error('[ReportModal] 下载报告失败:', err)
            wx.showToast({
              title: '下载报告失败',
              icon: 'none'
            })
          }
        })
      } catch (err) {
        console.error('[ReportModal] 获取报告URL失败:', err)
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
    async onDownloadReport() {
      if (!this.data.report || !this.data.report.id) {
        wx.showToast({
          title: '报告信息无效',
          icon: 'none'
        })
        return
      }

      wx.showLoading({ title: '下载中...' })

      try {
        // 获取PDF URL
        const result = await api.reports.getPdfUrl(this.data.report.id)
        const pdfUrl = result.pdfUrl || result.url || result

        if (!pdfUrl) {
          throw new Error('未获取到报告地址')
        }

        // 下载文件到本地
        wx.downloadFile({
          url: pdfUrl,
          success: (res) => {
            if (res.statusCode === 200) {
              wx.hideLoading()

              // 保存文件到本地
              const fileName = `检测报告_${this.data.report.pet?.animalName || '宠物'}_${this.data.report.orderNo || Date.now()}.pdf`

              wx.saveFile({
                tempFilePath: res.tempFilePath,
                success: (saveRes) => {
                  wx.showToast({
                    title: '下载成功',
                    icon: 'success'
                  })
                  console.log('[ReportModal] 报告下载成功:', saveRes.savedFilePath)
                },
                fail: (err) => {
                  console.error('[ReportModal] 保存报告失败:', err)
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
            console.error('[ReportModal] 下载报告失败:', err)
            wx.hideLoading()
            wx.showToast({
              title: '下载报告失败',
              icon: 'none'
            })
          }
        })
      } catch (err) {
        console.error('[ReportModal] 获取报告URL失败:', err)
        wx.hideLoading()
        wx.showToast({
          title: err.message || '下载失败',
          icon: 'none'
        })
      }
    }
  }
});
