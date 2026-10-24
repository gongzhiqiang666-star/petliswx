// pages/pet-health/pet-health.js
Page({
  data: {
    activeTab: 'vaccines',
    selectedPet: '小花',
    vaccineRecords: [
      {
        id: 1,
        name: '狂犬疫苗',
        date: '2023-12-01',
        dose: '第3针',
        nextDate: '2024-12-01',
        validUntil: '2025-12-01',
        type: 'required'
      },
      {
        id: 2,
        name: '猫三联疫苗',
        date: '2023-11-15',
        dose: '第2针',
        nextDate: '2024-11-15',
        validUntil: '2025-11-15',
        type: 'required'
      },
      {
        id: 3,
        name: '猫瘟疫苗',
        date: '2023-10-20',
        dose: '第1针',
        nextDate: '2024-10-20',
        validUntil: '2025-10-20',
        type: 'recommended'
      }
    ],
    checkupRecords: [
      {
        id: 1,
        date: '2023-06-15',
        weight: '4.5kg',
        weightChange: '+0.2kg',
        doctor: '李医生',
        summary: '各项指标正常'
      },
      {
        id: 2,
        date: '2023-03-10',
        weight: '4.3kg',
        weightChange: '+0.1kg',
        doctor: '王医生',
        summary: '健康状况良好'
      },
      {
        id: 3,
        date: '2022-12-01',
        weight: '4.2kg',
        weightChange: '-',
        doctor: '李医生',
        summary: '首次体检，状态良好'
      }
    ],
    medicalRecords: [
      {
        id: 1,
        type: '就诊',
        date: '2023-08-20',
        doctor: '张医生',
        diagnosis: '轻微感冒',
        treatment: '开具消炎药，多休息，多喝水',
        followUp: '一周后复查'
      },
      {
        id: 2,
        type: '用药',
        date: '2023-05-10',
        doctor: '李医生',
        diagnosis: '驱虫治疗',
        treatment: '口服驱虫药',
        followUp: '三个月后再次驱虫'
      }
    ]
  },

  onLoad(options) {
    const petId = options.petId;
    // 可以根据petId加载数据
  },

  onBack() {
    wx.navigateBack();
  },

  onSelectPet() {
    wx.showToast({
      title: '选择宠物功能开发中',
      icon: 'none'
    });
  },

  onTabChange(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({
      activeTab: tab
    });
  },

  onAddVaccine() {
    wx.showToast({
      title: '添加疫苗记录功能开发中',
      icon: 'none'
    });
  },

  onAddCheckup() {
    wx.showToast({
      title: '添加体检记录功能开发中',
      icon: 'none'
    });
  },

  onAddMedical() {
    wx.showToast({
      title: '添加医疗记录功能开发中',
      icon: 'none'
    });
  },

  onAddRecord() {
    // 根据当前tab添加对应记录
    const actions = ['vaccines', 'checkups', 'medical'];
    const tabs = ['疫苗', '体检', '医疗'];
    const currentIndex = actions.indexOf(this.data.activeTab);

    wx.showActionSheet({
      itemList: ['添加疫苗记录', '添加体检记录', '添加医疗记录'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.onAddVaccine();
        } else if (res.tapIndex === 1) {
          this.onAddCheckup();
        } else if (res.tapIndex === 2) {
          this.onAddMedical();
        }
      }
    });
  },

  onViewCheckupDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.showToast({
      title: '查看体检详情功能开发中',
      icon: 'none'
    });
  }
});
