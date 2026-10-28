const request = require('../../utils/request')

Page({
  data: {
    petId: null,
    activeTab: 'medical',
    selectedPet: '',
    loadError: '',
    loading: false,
    medicalRecords: [],
    reports: [],
    reportPage: 0,
    reportTotal: 0,
    hasMore: false
  },

  onLoad(options) {
    if (!options.petId) {
      this.setData({ loadError: '缺少宠物信息', loading: false })
      return Promise.resolve()
    }
    this.setData({ petId: options.petId })
    return this.loadHealth()
  },

  async loadHealth() {
    this.setData({ loading: true, loadError: '' })
    try {
      const result = await request.get(`/pets/${this.data.petId}/health`, { page: 0, size: 20 })
      const medicalRecords = (result.medicalHistory || []).map(item => ({
        ...item,
        id: item.orderId,
        date: String(item.createDt || '').slice(0, 10),
        diagnosis: item.medicalHistory || item.diagnosis || '未填写病史',
        treatment: item.medication || item.clinicalSymptoms || '无补充记录'
      }))
      const reports = (result.reports && result.reports.items) || []
      this.setData({
        selectedPet: result.petName || `宠物 ${this.data.petId}`,
        medicalRecords,
        reports,
        reportPage: 0,
        reportTotal: (result.reports && result.reports.total) || 0,
        hasMore: reports.length < ((result.reports && result.reports.total) || 0)
      })
    } catch (error) {
      this.setData({ loadError: error.message || '健康档案加载失败', medicalRecords: [], reports: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  retryLoad() { return this.loadHealth() },
  onTabChange(e) { this.setData({ activeTab: e.currentTarget.dataset.tab }) },
  onViewReport(e) { wx.navigateTo({ url: `/pages/report-view/report-view?id=${e.currentTarget.dataset.id}` }) },

  async onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return
    this.setData({ loading: true })
    try {
      const page = this.data.reportPage + 1
      const result = await request.get(`/pets/${this.data.petId}/health`, { page, size: 20 })
      const incoming = (result.reports && result.reports.items) || []
      const reports = this.data.reports.concat(incoming)
      this.setData({ reports, reportPage: page, hasMore: reports.length < this.data.reportTotal })
    } catch (error) {
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
