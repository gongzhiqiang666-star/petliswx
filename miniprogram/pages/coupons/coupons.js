const request = require('../../utils/request')
const labels = { available: '可使用', reserved: '已预留', used: '已使用', expired: '已过期', upcoming: '未到使用时间' }

Page({
  data: {
    category: 'available',
    items: [],
    busy: false,
    error: '',
    detailVisible: false,
    selectedCoupon: null,
    filters: [
      { id: 'all', title: '全部' }, { id: 'upcoming', title: '未生效' },
      { id: 'available', title: '可用' }, { id: 'reserved', title: '使用中' },
      { id: 'used', title: '已用' }, { id: 'expired', title: '过期' }
    ]
  },
  onLoad() { this.load() },
  filter(e) { this.setData({ category: e.currentTarget.dataset.id }); this.load() },
  async load() {
    this.setData({ busy: true, error: '' })
    try {
      const result = await request.get('/coupons', { category: this.data.category })
      this.setData({ items: result.map(item => ({
        ...item,
        label: labels[item.status] || item.status,
        ends: String(item.expiresAt || '').slice(0, 10),
        starts: String(item.startsAt || '').slice(0, 10)
      })) })
    } catch (error) {
      this.setData({ error: error.message })
    } finally {
      this.setData({ busy: false })
    }
  },
  showDetail(e) {
    const selectedCoupon = this.data.items.find(item => String(item.id) === String(e.currentTarget.dataset.id))
    if (selectedCoupon) this.setData({ selectedCoupon, detailVisible: true })
  },
  closeDetail() { this.setData({ detailVisible: false }) }
})
