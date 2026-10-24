/**
 * 分页组件
 * 提供上一页/下一页导航
 */
Component({
  properties: {
    current: {
      type: Number,
      value: 1
    },
    // 总数据条数（用于计算总页数）
    total: {
      type: Number,
      value: 0
    },
    // 每页显示条数（用于计算总页数）
    pageSize: {
      type: Number,
      value: 20
    }
  },

  data: {
    totalPages: 0
  },

  observers: {
    'total, pageSize': function(total, pageSize) {
      this.setData({
        totalPages: Math.ceil(total / pageSize)
      });
    }
  },

  methods: {
    onPrev() {
      if (this.properties.current > 1) {
        this.triggerEvent('change', { page: this.properties.current - 1 });
      }
    },

    onNext() {
      if (this.properties.current < this.data.totalPages) {
        this.triggerEvent('change', { page: this.properties.current + 1 });
      }
    }
  }
})
