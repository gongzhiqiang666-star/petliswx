// pages/icon-export/icon-export.js
Page({
  data: {
    tabIcons: [
      { name: 'home', label: '首页', iconFile: 'home.png', selected: true },
      { name: 'pet', label: '宠物', iconFile: 'pet.png', selected: true },
      { name: 'order', label: '订单', iconFile: 'order.png', selected: true },
      { name: 'profile', label: '我的', iconFile: 'profile.png', selected: true }
    ],
    currentPreview: null,
    showPreview: false,
    selectedCount: 4,
    isAllSelected: true,
    exportFormat: 'png'
  },

  onLoad() {
    this.setData({
      currentPreview: this.data.tabIcons[0]
    });
  },

  onToggleIcon(e) {
    const name = e.currentTarget.dataset.name;
    const tabIcons = this.data.tabIcons.map(icon => {
      if (icon.name === name) {
        return { ...icon, selected: !icon.selected };
      }
      return icon;
    });
    const selectedCount = tabIcons.filter(i => i.selected).length;
    this.setData({
      tabIcons,
      selectedCount,
      isAllSelected: selectedCount === tabIcons.length
    });
  },

  onToggleSelectAll() {
    const isAllSelected = this.data.isAllSelected;
    const tabIcons = this.data.tabIcons.map(icon => ({
      ...icon,
      selected: !isAllSelected
    }));
    this.setData({
      tabIcons,
      selectedCount: !isAllSelected ? tabIcons.length : 0,
      isAllSelected: !isAllSelected
    });
  },

  onPreviewIcon(e) {
    const name = e.currentTarget.dataset.name;
    const icon = this.data.tabIcons.find(i => i.name === name);
    this.setData({
      currentPreview: icon,
      showPreview: true
    });
  },

  onClosePreview() {
    this.setData({
      showPreview: false
    });
  },

  onFormatChange(e) {
    this.setData({
      exportFormat: e.detail.value
    });
  },

  onExport() {
    if (this.data.selectedCount === 0) {
      wx.showToast({
        title: '请至少选择一个图标',
        icon: 'none'
      });
      return;
    }

    const selected = this.data.tabIcons.filter(i => i.selected);
    const format = this.data.exportFormat.toUpperCase();

    wx.showModal({
      title: '导出确认',
      content: `即将导出 ${this.data.selectedCount} 个TabBar图标\n\n尺寸：81px × 81px\n格式：${format}\n背景：透明\n\n是否继续？`,
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({
            title: '导出中...'
          });

          setTimeout(() => {
            wx.hideLoading();
            wx.showToast({
              title: `成功导出 ${this.data.selectedCount} 个图标`,
              icon: 'success'
            });
          }, 1500);
        }
      }
    });
  },

  onDownloadAll() {
    wx.showModal({
      title: '下载全部',
      content: `下载所有 ${this.data.tabIcons.length} 个TabBar图标\n尺寸：81px × 81px\n格式：PNG\n\n是否继续？`,
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({
            title: '准备中...'
          });

          setTimeout(() => {
            wx.hideLoading();
            wx.showToast({
              title: '下载功能开发中',
              icon: 'none'
            });
          }, 1000);
        }
      }
    });
  }
});
