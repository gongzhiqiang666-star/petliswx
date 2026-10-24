// pages/order-add-pet/order-add-pet.js
const api = require('../../api/index.js');
const ageCalculator = require('../../utils/age-calculator.js');
const app = getApp();

Page({
  data: {
    petId: null,
    petInfo: null,
    sampleInfo: {
      infoSampleTypeId: null,
      sampleArea: '',
      collectDt: '',
      cityId: null
    },
    deliveryInfo: {
      masterName: '',
      masterNameEn: '',
      doctorName: ''
    },
    otherInfo: {
      remark: ''
    },
    // 新增: 年龄和体重字段
    animalInfo: {
      age: '',
      animalWeight: ''
    },
    sampleTypes: [],
    selectedSampleTypeIndex: null,
    selectedSampleTypeName: ''
  },

  onLoad(options) {
    const petId = options.petId;
    if (!petId) {
      wx.showToast({
        title: '参数错误',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
      return;
    }

    this.setData({ petId });
    this.loadPetDetail(petId);

    // 清除旧的订单表单缓存，确保数据新鲜
    try {
      wx.removeStorageSync('order_form');
      console.log('[OrderAddPet] 已清除旧的订单表单缓存');
    } catch (error) {
      console.error('[OrderAddPet] 清除缓存失败:', error);
    }

    // 设置采样日期为当天（格式：yyyy-MM-dd）
    const today = this.getTodayDate();
    this.setData({
      'sampleInfo.collectDt': today
    });
    console.log('[OrderAddPet] 采样日期已设置为当天:', today);
  },

  /**
   * 页面显示时检查是否有从样本类型选择页面返回的数据
   * 与宠物新增中的品种选择保持一致的交互方式
   */
  onShow() {
    // 从样本类型选择页面返回时，数据已通过页面栈传递到当前页
    const selectedSampleType = this.data.selectedSampleType;

    if (selectedSampleType) {
      this.setData({
        selectedSampleTypeName: selectedSampleType.sampleName,
        'sampleInfo.infoSampleTypeId': selectedSampleType.infoSampleTypeId,
        selectedSampleType: null  // 清除选中状态，避免下次进入时重复设置
      });

      console.log('[OrderAddPet] 样本类型已选择:', selectedSampleType);
    }
  },

  async loadPetDetail(petId) {
    try {
      wx.showLoading({ title: '加载中...' });
      const petInfo = await api.pets.getDetail(petId);

      // 自动填充畜主信息，使用后端 PetVO 返回的字段
      const masterName = petInfo.ownerName || '';
      const masterNameEn = petInfo.ownerNameEn || '';

      this.setData({
        petInfo,
        'deliveryInfo.masterName': masterName,
        'deliveryInfo.masterNameEn': masterNameEn
      });

      // 如果有出生日期，自动计算年龄
      if (petInfo.birthday) {
        const age = ageCalculator.calculateAgeForPicker(petInfo.birthday);
        if (age) {
          this.setData({
            'animalInfo.age': age
          });
          console.log('[OrderAddPet] 已根据出生日期自动计算年龄:', age);
        }
      }

      wx.hideLoading();

      console.log('[OrderAddPet] 宠物信息加载成功:', petInfo);
    } catch (error) {
      console.error('[OrderAddPet] 加载宠物详情失败:', error);
      wx.hideLoading();
      // 显示友好提示，允许用户继续操作
      wx.showToast({
        title: '宠物信息加载失败，请手动填写',
        icon: 'none',
        duration: 3000
      });
    }
  },

  // 跳转到样本类型选择页面
  onSelectSampleType() {
    wx.navigateTo({
      url: '/pages/sample-type-select/sample-type-select'
    });
  },

  // 样本类型选择完成后的回调已移除
  // 现在使用 onShow() 生命周期方法接收选择结果（与品种选择保持一致）

  /**
   * 获取当天日期（格式：yyyy-MM-dd）
   */
  getTodayDate() {
    const date = new Date();
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  onSampleAreaInput(e) {
    this.setData({
      'sampleInfo.sampleArea': e.detail.value
    });
  },

  onCollectDateChange(e) {
    this.setData({
      'sampleInfo.collectDt': e.detail.value
    });
  },

  onMasterNameInput(e) {
    this.setData({
      'deliveryInfo.masterName': e.detail.value
    });
  },

  onMasterNameEnInput(e) {
    this.setData({
      'deliveryInfo.masterNameEn': e.detail.value
    });
  },

  onDoctorNameInput(e) {
    this.setData({
      'deliveryInfo.doctorName': e.detail.value
    });
  },

  onRemarkInput(e) {
    this.setData({
      'otherInfo.remark': e.detail.value
    });
  },

  onAgeInput(e) {
    this.setData({
      'animalInfo.age': e.detail.value
    });
  },

  onAgeChange(e) {
    // age-picker 组件的变更事件
    const ageValue = e.detail.value;
    this.setData({
      'animalInfo.age': ageValue
    });
  },

  onWeightInput(e) {
    this.setData({
      'animalInfo.animalWeight': e.detail.value
    });
  },

  validateForm() {
    const { sampleInfo, deliveryInfo, animalInfo } = this.data;

    if (!sampleInfo.infoSampleTypeId) {
      return '请选择样本类型';
    }
    if (!sampleInfo.sampleArea || !sampleInfo.sampleArea.trim()) {
      return '请填写采样部位';
    }
    if (!sampleInfo.collectDt) {
      return '请选择采样日期';
    }
    if (!animalInfo.age || !animalInfo.age.trim()) {
      return '请填写年龄';
    }
    // 体重改为选填，移除验证
    if (!deliveryInfo.masterName || !deliveryInfo.masterName.trim()) {
      return '请填写畜主中文名';
    }
    if (!deliveryInfo.masterNameEn || !deliveryInfo.masterNameEn.trim()) {
      return '请填写畜主英文名';
    }
    if (!deliveryInfo.doctorName || !deliveryInfo.doctorName.trim()) {
      return '请填写送检医师姓名';
    }

    return null;
  },

  onSelectTestItems() {
    const error = this.validateForm();
    if (error) {
      wx.showToast({
        title: error,
        icon: 'none'
      });
      return;
    }

    const formData = {
      petId: this.data.petId,
      petInfo: this.data.petInfo,
      sampleInfo: {
        ...this.data.sampleInfo,
        sampleTypeName: this.data.selectedSampleTypeName  // 添加样本类型名称
      },
      deliveryInfo: this.data.deliveryInfo,
      otherInfo: this.data.otherInfo,
      animalInfo: this.data.animalInfo  // 新增: 包含年龄和体重
    };

    wx.setStorageSync('order_form', formData);

    wx.navigateTo({
      url: '/pages/order-test-items/order-test-items'
    });
  }
});
