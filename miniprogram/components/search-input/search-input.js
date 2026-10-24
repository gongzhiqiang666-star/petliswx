/**
 * 搜索输入组件
 * 带防抖功能的搜索输入框
 */
Component({
  properties: {
    placeholder: {
      type: String,
      value: '搜索...'
    },
    debounceTime: {
      type: Number,
      value: 400  // 防抖时间400ms
    }
  },

  data: {
    value: ''
  },

  methods: {
    onInput(e) {
      const value = e.detail.value;
      this.setData({ value });

      console.log('[SearchInput] 输入值:', value);

      // 防抖处理
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer);
      }

      this.debounceTimer = setTimeout(() => {
        console.log('[SearchInput] 触发搜索事件，值:', value);
        this.triggerEvent('search', { value });
      }, this.data.debounceTime);
    },

    onClear() {
      this.setData({ value: '' });
      console.log('[SearchInput] 清空搜索');
      this.triggerEvent('search', { value: '' });
    }
  },

  detached() {
    // 清理定时器
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  }
})
