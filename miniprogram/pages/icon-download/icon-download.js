// pages/icon-download/icon-download.js
Page({
  data: {
    searchQuery: '',
    selectedCategory: '全部',
    selectedCount: 0,
    isAllSelected: false,
    icons: [
      { name: 'Home', category: '导航', iconFile: 'home.png', selected: false },
      { name: 'Pet', category: '导航', iconFile: 'pet.png', selected: false },
      { name: 'Order', category: '导航', iconFile: 'order.png', selected: false },
      { name: 'Profile', category: '导航', iconFile: 'profile.png', selected: false },
      { name: 'Back', category: '导航', iconFile: 'back.png', selected: false },
      { name: 'ArrowDown', category: '导航', iconFile: 'arrow-down.png', selected: false },
      { name: 'Search', category: '功能', iconFile: 'icon-search.png', selected: false },
      { name: 'Filter', category: '功能', iconFile: 'filter.png', selected: false },
      { name: 'Calendar', category: '功能', iconFile: 'icon-monthly.png', selected: false },
      { name: 'File', category: '文档', iconFile: 'icon-file.png', selected: false },
      { name: 'Refresh', category: '操作', iconFile: 'refresh.png', selected: false },
      { name: 'Download', category: '操作', iconFile: 'download.png', selected: false },
      { name: 'Share', category: '操作', iconFile: 'share.png', selected: false },
      { name: 'Bell', category: '功能', iconFile: 'search.png', selected: false },
      { name: 'Settings', category: '功能', iconFile: 'support.png', selected: false },
      { name: 'Help', category: '功能', iconFile: 'tips.png', selected: false },
      { name: 'Add', category: '操作', iconFile: 'add-icon.png', selected: false },
      { name: 'Edit', category: '操作', iconFile: 'edit-icon.png', selected: false },
      { name: 'Delete', category: '操作', iconFile: 'delete-icon.png', selected: false },
      { name: 'Check', category: '状态', iconFile: 'check-circle.png', selected: false },
      { name: 'Close', category: '操作', iconFile: 'close-icon.png', selected: false },
      { name: 'Error', category: '状态', iconFile: 'error.png', selected: false },
      { name: 'Info', category: '信息', iconFile: 'info-icon.png', selected: false },
      { name: 'Camera', category: '功能', iconFile: 'camera.png', selected: false },
      { name: 'Phone', category: '联系', iconFile: 'search-icon.png', selected: false },
      { name: 'Report', category: '文档', iconFile: 'report.png', selected: false }
    ],
    categories: [],
    filteredIcons: []
  },

  onLoad() {
    this.setData({
      categories: ['全部', ...Array.from(new Set(this.data.icons.map(i => i.category)))],
      filteredIcons: this.data.icons
    });
  },

  get filteredIcons() {
    return this.data.icons.filter(icon => {
      const matchCategory = this.data.selectedCategory === '全部' || icon.category === this.data.selectedCategory;
      const matchSearch = icon.name.toLowerCase().includes(this.data.searchQuery.toLowerCase());
      return matchCategory && matchSearch;
    });
  },

  onSearchInput(e) {
    this.setData({
      searchQuery: e.detail.value
    });
    this.updateFilteredIcons();
  },

  onClearSearch() {
    this.setData({ searchQuery: '' });
    this.updateFilteredIcons();
  },

  onCategoryChange(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({
      selectedCategory: category
    });
    this.updateFilteredIcons();
  },

  updateFilteredIcons() {
    const filtered = this.data.icons.filter(icon => {
      const matchCategory = this.data.selectedCategory === '全部' || icon.category === this.data.selectedCategory;
      const matchSearch = icon.name.toLowerCase().includes(this.data.searchQuery.toLowerCase());
      return matchCategory && matchSearch;
    });
    this.setData({ filteredIcons: filtered });
    this.updateSelectionStatus();
  },

  onToggleIcon(e) {
    const name = e.currentTarget.dataset.name;
    const icons = this.data.icons.map(icon => {
      if (icon.name === name) {
        return { ...icon, selected: !icon.selected };
      }
      return icon;
    });
    this.setData({ icons });
    this.updateFilteredIcons();
    this.updateSelectionStatus();
  },

  onToggleSelectAll() {
    const isAllSelected = this.data.isAllSelected;
    const icons = this.data.icons.map(icon => ({
      ...icon,
      selected: !isAllSelected
    }));
    this.setData({ icons });
    this.updateFilteredIcons();
    this.updateSelectionStatus();
  },

  updateSelectionStatus() {
    const selected = this.data.filteredIcons.filter(i => i.selected);
    this.setData({
      selectedCount: selected.length,
      isAllSelected: selected.length === this.data.filteredIcons.length && this.data.filteredIcons.length > 0
    });
  },

  onDownloadSelected() {
    if (this.data.selectedCount === 0) {
      wx.showToast({
        title: '请至少选择一个图标',
        icon: 'none'
      });
      return;
    }

    wx.showModal({
      title: '下载确认',
      content: `即将下载 ${this.data.selectedCount} 个图标\n尺寸：81px × 81px（微信小程序规范）\n格式：PNG 透明背景\n\n是否继续？`,
      success: (res) => {
        if (res.confirm) {
          wx.showToast({
            title: `已开始下载 ${this.data.selectedCount} 个图标`,
            icon: 'success'
          });
        }
      }
    });
  },

  onDownloadAll() {
    wx.showModal({
      title: '下载全部图标',
      content: `即将下载所有 ${this.data.icons.length} 个图标\n尺寸：81px × 81px\n格式：PNG 透明背景\n\n是否继续？`,
      success: (res) => {
        if (res.confirm) {
          wx.showToast({
            title: '下载功能开发中',
            icon: 'none'
          });
        }
      }
    });
  },

  onNavigateToExport() {
    wx.navigateTo({
      url: '/pages/icon-export/icon-export'
    });
  }
});
