// components/popup-selector/popup-selector.js
Component({
  properties: {
    // 是否显示
    visible: {
      type: Boolean,
      value: false
    },
    // 标题
    title: {
      type: String,
      value: '请选择'
    },
    // 选项列表
    items: {
      type: Array,
      value: []
    },
    // 当前选中ID
    selectedId: {
      type: Number,
      value: null
    },
    // 是否显示搜索框
    showSearch: {
      type: Boolean,
      value: true
    },
    // 搜索框占位符
    searchPlaceholder: {
      type: String,
      value: '搜索'
    },
    // 空状态提示文本
    emptyText: {
      type: String,
      value: '暂无数据'
    },
    loading: {
      type: Boolean,
      value: false
    },
    errorMessage: {
      type: String,
      value: ''
    },
    // 数据字段映射 - ID字段
    itemIdKey: {
      type: String,
      value: 'id'
    },
    // 数据字段映射 - 名称字段
    itemNameKey: {
      type: String,
      value: 'name'
    },
    // 数据字段映射 - 英文名称字段
    itemNameEnKey: {
      type: String,
      value: 'nameEn'
    }
  },

  data: {
    filteredItems: [],
    searchKeyword: '',
    loading: false,
    hasMore: true
  },

  observers: {
    'items': function(items) {
      this.filterItems(this.data.searchKeyword);
    },
    'visible': function(visible) {
      if (visible) {
        // 打开时重置搜索
        this.setData({
          searchKeyword: '',
          filteredItems: this.properties.items
        });
      }
    }
  },

  methods: {
    /**
     * 点击遮罩关闭
     */
    onMaskTap() {
      this.triggerEvent('close');
    },

    /**
     * 阻止内容区域点击冒泡
     */
    onContentTap() {
      // 空函数，阻止事件冒泡
    },

    /**
     * 关闭弹窗
     */
    onClose() {
      this.triggerEvent('close');
    },

    /**
     * 搜索输入
     */
    onSearch(e) {
      const keyword = e.detail.value;
      this.setData({ searchKeyword: keyword });

      // 防抖搜索
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
      }

      this.searchTimer = setTimeout(() => {
        this.filterItems(keyword);
      }, 300);
    },

    /**
     * 清除搜索
     */
    onClearSearch() {
      this.setData({
        searchKeyword: '',
        filteredItems: this.properties.items
      });
    },

    /**
     * 过滤列表项
     */
    filterItems(keyword) {
      const items = this.properties.items || [];
      const itemIdKey = this.properties.itemIdKey;
      const itemNameKey = this.properties.itemNameKey;
      const itemNameEnKey = this.properties.itemNameEnKey;

      if (!keyword || !keyword.trim()) {
        this.setData({ filteredItems: items });
        return;
      }

      const lowerKeyword = keyword.toLowerCase().trim();
      const filtered = items.filter(item => {
        const name = item[itemNameKey] || '';
        const nameEn = item[itemNameEnKey] || '';

        return name.toLowerCase().includes(lowerKeyword) ||
               nameEn.toLowerCase().includes(lowerKeyword);
      });

      this.setData({ filteredItems: filtered });
    },

    /**
     * 选择选项
     */
    onSelect(e) {
      const item = e.currentTarget.dataset.item;
      const itemIdKey = this.properties.itemIdKey;

      this.triggerEvent('select', {
        item: item,
        id: item[itemIdKey]
      });

      // 选中后关闭弹窗
      this.onClose();
    },

    onRetry() {
      this.triggerEvent('retry');
    },

    /**
     * 滚动到底部（可用于实现分页加载）
     */
    onScrollToLower() {
      this.triggerEvent('scrolltolower');
    }
  }
});
