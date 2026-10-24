// pages/order-step2/order-step2.js
Page({
  data: {
    searchQuery: '',
    selectedCategory: 'all',
    selectedTests: [],
    categories: ['全部', '基础检查', '生化检查', '基因检测', '其他'],
    testItems: [
      {
        InfoTestTypeId: 1225,
        name: '狂犬病毒抗体检测',
        category: '基础检查',
        price: 680,
        originalPrice: 800,
        duration: '5-7工作日',
        description: '检测狂犬病毒抗体水平，用于出入境检疫',
        isHot: true,
        requiresChipAndBirthday: true
      },
      {
        InfoTestTypeId: 1,
        name: '血常规',
        category: '基础检查',
        price: 120,
        originalPrice: 150,
        duration: '1-2工作日',
        description: '检测血液各项指标，评估整体健康状况',
        isHot: true,
        requiresChipAndBirthday: false
      },
      {
        InfoTestTypeId: 2,
        name: '生化全项',
        category: '生化检查',
        price: 280,
        originalPrice: 350,
        duration: '2-3工作日',
        description: '检测肝肾功能、血糖、血脂等生化指标',
        isHot: false,
        requiresChipAndBirthday: false
      },
      {
        InfoTestTypeId: 3,
        name: 'DNA检测',
        category: '基因检测',
        price: 1200,
        originalPrice: 1500,
        duration: '10-14工作日',
        description: '检测遗传疾病风险、血统鉴定',
        isHot: false,
        requiresChipAndBirthday: false
      },
      {
        InfoTestTypeId: 4,
        name: '过敏原检测',
        category: '其他',
        price: 450,
        originalPrice: 550,
        duration: '3-5工作日',
        description: '检测常见过敏原，帮助制定饮食计划',
        isHot: false,
        requiresChipAndBirthday: false
      }
    ],
    step1Data: null
  },

  onLoad(options) {
    // 获取步骤1的数据
    const step1Data = options.step1Data ? JSON.parse(decodeURIComponent(options.step1Data)) : null;
    this.setData({ step1Data });
  },

  get filteredTests() {
    return this.data.testItems.filter(test => {
      const matchesSearch = test.name.toLowerCase().includes(this.data.searchQuery.toLowerCase()) ||
                           test.description.toLowerCase().includes(this.data.searchQuery.toLowerCase());
      const matchesCategory = this.data.selectedCategory === 'all' || test.category === this.data.selectedCategory;
      return matchesSearch && matchesCategory;
    });
  },

  onSearchInput(e) {
    this.setData({ searchQuery: e.detail.value });
  },

  onClearSearch() {
    this.setData({ searchQuery: '' });
  },

  onCategoryChange(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({
      selectedCategory: category === '全部' ? 'all' : category
    });
  },

  onToggleTest(e) {
    const testId = e.currentTarget.dataset.id;
    const test = this.data.testItems.find(t => t.InfoTestTypeId === testId);

    if (this.data.selectedTests.includes(testId)) {
      this.setData({
        selectedTests: this.data.selectedTests.filter(id => id !== testId)
      });
    } else {
      // 验证狂犬检测要求
      if (test?.requiresChipAndBirthday) {
        if (!this.data.step1Data?.ChipNO || !this.data.step1Data?.Birthday) {
          wx.showModal({
            title: '提示',
            content: '选择狂犬检测需要填写芯片号和出生日期，请返回步骤1完善信息',
            showCancel: false
          });
          return;
        }
        if (!this.data.step1Data?.Remark) {
          wx.showModal({
            title: '提示',
            content: '选择狂犬检测需要在备注中填写快递单号，请返回步骤1完善信息',
            showCancel: false
          });
          return;
        }
      }

      this.setData({
        selectedTests: [...this.data.selectedTests, testId]
      });
    }
  },

  calculateTotal() {
    return this.data.selectedTests.reduce((total, testId) => {
      const test = this.data.testItems.find(t => t.InfoTestTypeId === testId);
      return total + (test?.price || 0);
    }, 0);
  },

  calculateOriginalTotal() {
    return this.data.selectedTests.reduce((total, testId) => {
      const test = this.data.testItems.find(t => t.InfoTestTypeId === testId);
      return total + (test?.originalPrice || 0);
    }, 0);
  },

  onSubmit() {
    if (this.data.selectedTests.length === 0) {
      wx.showToast({
        title: '请至少选择一个检测项目',
        icon: 'none'
      });
      return;
    }

    // 提交订单
    wx.showLoading({
      title: '提交中...'
    });

    setTimeout(() => {
      wx.hideLoading();
      wx.showToast({
        title: '预约提交成功',
        icon: 'success'
      });

      setTimeout(() => {
        wx.switchTab({
          url: '/pages/orders/orders'
        });
      }, 1500);
    }, 1000);
  },

  onBack() {
    wx.navigateBack();
  }
});
