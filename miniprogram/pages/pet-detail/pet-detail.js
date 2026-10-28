// pages/pet-detail/pet-detail.js
const api = require('../../api/index.js');

Page({
  data: {
    petData: {
      id: '',
      animalName: '',
      animalNameEn: '',
      infoAnimalId: 1,
      infoVarietyId: null,
      animalType: '',
      age: '',
      dictSexId: 2,
      animalWeight: '',
      chipNo: '',
      birthday: '',
      ownerName: '',
      ownerNameEn: '',
      speciesText: '',
      genderText: '',
      createtime: null
    },
    reports: [],
    loading: true,
    error: false
  },

  onLoad(options) {
    // 隐藏 tabBar，让页面全屏显示
    wx.hideTabBar({
      animation: true
    });

    const petId = options.id;
    if (petId) {
      this.loadPetDetail(petId);
      this.loadPetReports(petId);
    } else {
      this.setData({ error: true, loading: false });
      wx.showToast({
        title: '宠物ID缺失',
        icon: 'none'
      });
    }
  },

  onUnload() {
    // 页面卸载时恢复 tabBar 显示
    wx.showTabBar({
      animation: true
    });
  },

  /**
   * 加载宠物详情
   */
  async loadPetDetail(petId) {
    try {
      const petData = await api.pets.getDetail(petId);

      this.setData({
        petData: {
          id: petData.id || petData.petId,
          animalName: petData.animalName || '',
          animalNameEn: petData.animalNameEn || '',
          infoAnimalId: petData.infoAnimalId || 1,
          infoVarietyId: petData.infoVarietyId || null,
          animalType: petData.infoVarietyName || '',  // 使用后端返回的品种名称
          age: this.calculateAge(petData.birthday),  // 根据生日计算年龄
          dictSexId: petData.dictSexId || 2,
          animalWeight: petData.animalWeight || 0,  // 后端暂未返回此字段
          chipNo: petData.chipNo || '',
          birthday: this.formatDate(petData.birthday),
          ownerName: petData.ownerName || '',
          ownerNameEn: petData.ownerNameEn || '',
          speciesText: petData.infoAnimalName || this.getSpeciesText(petData.infoAnimalId),  // 优先使用后端返回的物种名称
          genderText: petData.dictSexName || this.getGenderText(petData.dictSexId),  // 优先使用后端返回的性别名称
          createtime: petData.createtime
        },
        loading: false
      });
    } catch (err) {
      console.error('[PetDetail] 加载宠物详情失败:', err);
      this.setData({ loading: false, error: true });

      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    }
  },

  /**
   * 加载宠物的检测报告列表
   */
  async loadPetReports(petId) {
    try {
      const result = await api.reports.getList({
        petId: petId,
        pageSize: 100
      });

      const reports = (result.content || result.list || result || []).map(report => ({
        id: report.reportId || report.diagnosisDetailId,
        applicationTime: this.formatDateTime(report.createTime || report.createDt),
        status: report.diagnosisStatus,
        statusLabel: this.getReportStatusLabel(report.diagnosisStatus),
        testTypeName: report.testTypeName || '检测项目'
      }));

      // 按申请时间倒序排列（最新的在前）（iOS兼容）
      reports.sort((a, b) => {
        const dateA = this.parseDate(a.applicationTime);
        const dateB = this.parseDate(b.applicationTime);
        return dateB - dateA;
      });

      this.setData({ reports });
    } catch (err) {
      console.error('[PetDetail] 加载检测报告失败:', err);
      // 不显示toast，只记录错误
    }
  },

  /**
   * 获取报告状态标签
   */
  getReportStatusLabel(status) {
    const labels = {
      0: '检测中',
      1: '检测中',
      2: '已完成',
      3: '已完成'
    };
    return labels[status] || '未知';
  },

  /**
   * 获取物种文本
   */
  getSpeciesText(speciesId) {
    const map = { 1: '猫', 2: '狗', 3: '兔', 4: '乌龟', 5: '其他' };
    return map[speciesId] || '未知';
  },

  /**
   * 获取性别文本
   */
  getGenderText(genderId) {
    const map = { 0: '未知', 1: '公', 2: '母', 3: '其他' };
    return map[genderId] || '未知';
  },

  /**
   * 解析日期为时间戳（iOS兼容）
   * 支持多种输入格式：时间戳、"yyyy-MM-dd"、"yyyy-MM-dd HH:mm:ss"等
   */
  parseDate(dateStr) {
    if (!dateStr) return 0;

    // 处理时间戳（数字）
    if (typeof dateStr === 'number') {
      return dateStr;
    }
    // 处理时间戳字符串
    if (typeof dateStr === 'string' && /^\d+$/.test(dateStr)) {
      return parseInt(dateStr);
    }
    // 处理字符串日期
    const isoStr = dateStr.replace(' ', 'T');
    const date = new Date(isoStr);
    return isNaN(date.getTime()) ? 0 : date.getTime();
  },

  /**
   * 格式化日期（iOS兼容格式）
   * 支持多种输入格式：时间戳、"yyyy-MM-dd"、"yyyy-MM-dd HH:mm:ss"等
   */
  formatDate(dateStr) {
    if (!dateStr) return '';

    let date;

    // 处理时间戳（数字）
    if (typeof dateStr === 'number') {
      date = new Date(dateStr);
    }
    // 处理时间戳字符串
    else if (typeof dateStr === 'string' && /^\d+$/.test(dateStr)) {
      date = new Date(parseInt(dateStr));
    }
    // 处理字符串日期
    else {
      // 替换空格为T，使其符合ISO格式 "yyyy-MM-ddTHH:mm:ss"
      const isoStr = dateStr.replace(' ', 'T');
      date = new Date(isoStr);
    }

    if (isNaN(date.getTime())) return '';

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  /**
   * 格式化日期时间（iOS兼容格式）
   * 支持多种输入格式：时间戳、"yyyy-MM-dd"、"yyyy-MM-dd HH:mm:ss"等
   */
  formatDateTime(dateStr) {
    if (!dateStr) return '';

    let date;

    // 处理时间戳（数字）
    if (typeof dateStr === 'number') {
      date = new Date(dateStr);
    }
    // 处理时间戳字符串
    else if (typeof dateStr === 'string' && /^\d+$/.test(dateStr)) {
      date = new Date(parseInt(dateStr));
    }
    // 处理字符串日期
    else {
      // 替换空格为T，使其符合ISO格式 "yyyy-MM-ddTHH:mm:ss"
      const isoStr = dateStr.replace(' ', 'T');
      date = new Date(isoStr);
    }

    if (isNaN(date.getTime())) return '';

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  },

  /**
   * 计算年龄（iOS兼容格式）
   */
  calculateAge(birthday) {
    if (!birthday) return '';

    let birth;

    // 处理时间戳（数字）
    if (typeof birthday === 'number') {
      birth = new Date(birthday);
    }
    // 处理时间戳字符串
    else if (typeof birthday === 'string' && /^\d+$/.test(birthday)) {
      birth = new Date(parseInt(birthday));
    }
    // 处理字符串日期
    else {
      // 替换空格为T，使其符合ISO格式 "yyyy-MM-ddTHH:mm:ss"
      const isoStr = birthday.replace(' ', 'T');
      birth = new Date(isoStr);
    }

    if (isNaN(birth.getTime())) return '';

    const today = new Date();
    let years = today.getFullYear() - birth.getFullYear();
    let months = today.getMonth() - birth.getMonth();
    let days = today.getDate() - birth.getDate();

    // 如果天数差为负，从月份借位
    if (days < 0) {
      months--;
      // 获取上个月的天数
      const lastMonth = new Date(today.getFullYear(), today.getMonth(), 0);
      days += lastMonth.getDate();
    }

    // 如果月份差为负，借位（年份减1，月份加12）
    if (months < 0) {
      years--;
      months += 12;
    }

    if (years > 0) {
      return `${years}岁${months > 0 ? months + '个月' : ''}`;
    } else if (months > 0) {
      return `${months}个月`;
    } else {
      const daysCount = Math.floor((today - birth) / (1000 * 60 * 60 * 24));
      return `${daysCount}天`;
    }
  },

  /**
   * 编辑宠物
   */
  onEdit() {
    wx.navigateTo({
      url: `/pages/pet-add/pet-add?id=${this.data.petData.id}&mode=edit`
    });
  },

  onViewHealth() {
    wx.navigateTo({ url: `/pages/pet-health/pet-health?petId=${this.data.petData.id}` });
  },

  /**
   * 预约检测 - 直接跳转到预约信息填写页面，带入宠物信息
   */
  onBookTest() {
    // 直接跳转到预约信息填写页面，并传递宠物ID
    wx.navigateTo({
      url: `/pages/order-add-pet/order-add-pet?petId=${this.data.petData.id}&mode=book`
    });
  },

  /**
   * 查看报告详情
   */
  onViewReport(e) {
    const reportId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/report-view/report-view?id=${reportId}`
    });
  }
});
