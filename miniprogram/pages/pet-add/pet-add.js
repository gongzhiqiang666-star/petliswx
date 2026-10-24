// pages/pet-add/pet-add.js
// 宠物建档页面 - 全面优化版本
const app = getApp();
const api = require('../../api/index.js');
const ageCalculator = require('../../utils/age-calculator.js');
const pinyinUtils = require('../../utils/pinyin-full.js');

// 防抖函数
function debounce(func, wait) {
  let timeout;
  return function(...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait);
  };
}

// 验证规则
const validationRules = {
  animalName: {
    required: true,
    minLength: 2,
    maxLength: 20,
    message: '请输入2-20个字符的宠物中文名'
  },
  animalNameEn: {
    required: true,
    minLength: 2,
    maxLength: 30,
    pattern: /^[a-zA-Z\s]+$/,
    message: '请输入2-30个英文字符'
  },
  infoAnimalId: {
    required: true,
    message: '请选择物种'
  },
  dictSexId: {
    required: true,
    message: '请选择性别'
  },
  birthday: {
    required: true,
    message: '请选择出生日期'
  },
  age: {
    required: true,
    message: '请选择出生日期自动计算年龄'
  },
  ownerName: {
    required: true,
    minLength: 2,
    maxLength: 20,
    message: '请输入2-20个字符的中文姓名'
  },
  ownerNameEn: {
    required: true,
    minLength: 2,
    maxLength: 50,
    message: '请输入2-50个拼音字符'
  }
};

Page({
  data: {
    // 加载状态
    isLoading: true,
    isSubmitting: false,

    // 表单数据
    petId: null,
    isEditMode: false,
    formData: {
      infoAnimalId: 0,
      infoVarietyId: 0,
      animalName: '',
      animalNameEn: '',
      dictSexId: 0,
      age: '',
      chipNo: '',
      birthday: '',
      ownerName: '',
      ownerNameEn: ''
    },

    // 表单验证错误
    formErrors: {},

    // 选择相关
    selectedBreed: {},
    animalSpecies: [],
    genderOptions: [],
    selectedAnimalIndex: 0,
    selectedAnimalName: '',
    speciesList: [],
    showSpeciesSelector: false,

    // UI状态
    keyboardShown: false,
    keyboardHeight: 0,
    keyboardStyle: '',
    scrollIntoView: '',
    showDraftHint: false,
    draftSaveTimer: null,

    // 进度
    currentStep: 1,
    completedCount: 0,
    totalFields: 7
  },

  async onLoad(options) {
    const today = new Date();
    const birthday = this.formatDate(today);

    this.setData({
      'formData.birthday': birthday,
      isLoading: true
    });

    // 尝试恢复草稿
    this.loadDraft();

    await this.loadDictionaryData();

    // 检查是否为编辑模式
    if (options.id) {
      this.setData({
        petId: options.id,
        isEditMode: true
      });
      wx.setNavigationBarTitle({
        title: '编辑宠物'
      });
      await this.loadPetDetail(options.id);
    } else {
      wx.setNavigationBarTitle({
        title: '宠物建档'
      });
    }

    this.setData({ isLoading: false });
    this.updateProgress();

    // 监听键盘高度变化
    this.setupKeyboardListener();
  },

  /**
   * 监听键盘弹起/收起
   */
  setupKeyboardListener() {
    // 微信小程序支持键盘高度监听
    try {
      wx.onKeyboardHeightChange((res) => {
        const height = res.height;
        console.log('[PetAdd] 键盘高度变化:', height);
        if (height > 0) {
          this.setData({
            keyboardShown: true,
            keyboardHeight: height,
            keyboardStyle: `transform: translateY(-${height}px)`
          });
        } else {
          this.setData({
            keyboardShown: false,
            keyboardHeight: 0,
            keyboardStyle: ''
          });
        }
      });
    } catch (e) {
      console.log('[PetAdd] 键盘监听不支持:', e);
    }
  },

  /**
   * 加载宠物详情（编辑模式）
   */
  async loadPetDetail(petId) {
    try {
      wx.showLoading({ title: '加载中...' });

      const petData = await api.pets.getDetail(petId);

      this.setData({
        'formData.infoAnimalId': petData.infoAnimalId || 0,
        'formData.infoVarietyId': petData.infoVarietyId || 0,
        'formData.animalName': petData.animalName || '',
        'formData.animalNameEn': petData.animalNameEn || '',
        'formData.dictSexId': petData.dictSexId || 0,
        'formData.chipNo': petData.chipNo || '',
        'formData.birthday': this.formatDate(petData.birthday),
        'formData.ownerName': petData.ownerName || '',
        'formData.ownerNameEn': petData.ownerNameEn || ''
      });

      if (petData.infoAnimalId) {
        const index = this.data.animalSpecies.findIndex(
          item => item.id === petData.infoAnimalId
        );
        if (index !== -1) {
          this.setData({
            selectedAnimalIndex: index,
            selectedAnimalName: this.data.animalSpecies[index].name
          });
        }
      }

      if (petData.infoVarietyId && petData.infoVarietyName) {
        this.setData({
          selectedBreed: {
            infoVarietyId: petData.infoVarietyId,
            varietyCn: petData.infoVarietyName || petData.varietyName,
            varietyEn: petData.varietyNameEn || ''
          }
        });
      }

      this.calculateAgeFromBirthday();
      wx.hideLoading();
    } catch (error) {
      console.error('[PetAdd] 加载宠物详情失败:', error);
      wx.hideLoading();
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    }
  },

  async loadDictionaryData() {
    try {
      wx.showLoading({
        title: '加载中...',
        mask: true
      });

      const app = getApp();

      // 尝试从缓存获取字典数据
      let animals = app.getCachedDictionary('animals');
      let sexes = app.getCachedDictionary('sexes');

      // 如果缓存不存在或过期，从API获取
      if (!animals || !sexes) {
        console.log('[PetAdd] 缓存未命中，从API获取字典数据');

        // 改为串行请求，避免并发触发限流
        if (!animals) {
          animals = await api.dictionary.getAnimals();
          app.setCachedDictionary('animals', animals);
        }

        if (!sexes) {
          sexes = await api.dictionary.getSexes();
          app.setCachedDictionary('sexes', sexes);
        }
      } else {
        console.log('[PetAdd] 使用缓存的字典数据');
      }

      const speciesList = animals || [];

      const animalSpecies = (animals || []).map(item => ({
        id: Number(item.infoAnimalId) || 0,
        name: item.animalName,
        name_en: item.animalNameEn
      }));

      const genderOptions = (sexes || []).map(item => ({
        id: Number(item.dictSexId) || 0,
        name: item.sexName
      }));

      const defaultAnimalId = animalSpecies.length > 0 ? animalSpecies[0].id : 0;
      const defaultAnimalName = animalSpecies.length > 0 ? animalSpecies[0].name : '';
      const defaultSexId = genderOptions.find(g => g.id === 6) ? 6 : (genderOptions.length > 0 ? genderOptions[0].id : 0);

      this.setData({
        animalSpecies,
        genderOptions,
        speciesList,
        'formData.infoAnimalId': defaultAnimalId,
        'formData.dictSexId': defaultSexId,
        selectedAnimalIndex: 0,
        selectedAnimalName: defaultAnimalName
      });

      wx.hideLoading();
      this.calculateAgeFromBirthday();
    } catch (error) {
      console.error('[PetAdd] 加载字典数据失败:', error);
      wx.hideLoading();

      // 优化：如果是429错误，提供更友好的提示
      let errorMessage = '加载失败';
      if (error.message && error.message.includes('429')) {
        errorMessage = '请求过于频繁，请稍后再试';
      } else if (error.message) {
        errorMessage = error.message;
      }

      wx.showToast({
        title: errorMessage,
        icon: 'none'
      });
    }
  },

  /**
   * 输入事件处理（带实时验证）
   */
  onInputChange: debounce(function(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;

    if (field === 'ownerName') {
      const pinyin = pinyinUtils.convertToPinyin(value);
      this.setData({
        'formData.ownerName': value,
        'formData.ownerNameEn': pinyin
      });
    } else {
      this.setData({
        [`formData.${field}`]: value
      });
    }

    // 实时验证
    this.validateField(field, value);

    // 更新进度
    this.updateProgress();

    // 自动保存草稿
    this.saveDraftDebounced();
  }, 300),

  /**
   * 输入框聚焦事件
   */
  onInputFocus(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      scrollIntoView: `field-${field}`
    });
  },

  /**
   * 输入框失焦事件
   */
  onFieldBlur(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;

    // 失焦时再次验证
    this.validateField(field, value);
  },

  /**
   * 字段验证
   */
  validateField(field, value) {
    const rule = validationRules[field];
    if (!rule) return true;

    let error = '';

    // 必填验证（兼容字符串和数字类型）
    const isEmpty = value === '' || value === null || value === undefined ||
      (typeof value === 'string' && !value.trim()) ||
      (typeof value === 'number' && value === 0);

    if (rule.required && isEmpty) {
      error = rule.message;
      this.setData({
        [`formErrors.${field}`]: error
      });
      return false;
    }

    // 长度验证（只对字符串类型有效）
    if (typeof value === 'string') {
      if (value && rule.minLength && value.length < rule.minLength) {
        error = rule.message;
      }
      if (value && rule.maxLength && value.length > rule.maxLength) {
        error = rule.message;
      }
    }

    // 正则验证（只对字符串类型有效）
    if (typeof value === 'string' && value && rule.pattern && !rule.pattern.test(value)) {
      error = rule.message;
    }

    if (error) {
      this.setData({
        [`formErrors.${field}`]: error
      });
      return false;
    } else {
      this.setData({
        [`formErrors.${field}`]: ''
      });
      return true;
    }
  },

  /**
   * 更新完成进度
   */
  updateProgress() {
    const { formData } = this.data;
    const requiredFields = ['animalName', 'animalNameEn', 'infoAnimalId', 'dictSexId', 'birthday', 'age', 'ownerName', 'ownerNameEn'];
    let completedCount = 0;

    requiredFields.forEach(field => {
      const value = formData[field];
      if (value && value.toString().trim()) {
        completedCount++;
      }
    });

    // 计算当前步骤
    let currentStep = 1;
    if (completedCount >= 8) {
      currentStep = 3;
    } else if (completedCount >= 5) {
      currentStep = 2;
    }

    this.setData({
      completedCount,
      currentStep
    });
  },

  /**
   * 清除物种选择
   */
  clearSpecies(e) {
    e.stopPropagation();
    this.setData({
      selectedAnimalName: '',
      'formData.infoAnimalId': 0,
      selectedBreed: {},
      'formData.infoVarietyId': 0
    });
    this.updateProgress();
    this.saveDraftDebounced();
  },

  /**
   * 清除品种选择
   */
  clearBreed(e) {
    e.stopPropagation();
    this.setData({
      selectedBreed: {},
      'formData.infoVarietyId': 0
    });
    this.saveDraftDebounced();
  },

  /**
   * 显示物种选择器
   */
  showSpeciesSelector() {
    this.setData({ showSpeciesSelector: true });
  },

  /**
   * 隐藏物种选择器
   */
  hideSpeciesSelector() {
    this.setData({ showSpeciesSelector: false });
  },

  /**
   * 物种选择事件
   */
  onSpeciesSelect(e) {
    const item = e.detail.item;

    this.setData({
      selectedAnimalName: item.animalName,
      'formData.infoAnimalId': item.infoAnimalId,
      selectedBreed: {},
      'formData.infoVarietyId': 0
    });

    // 清除错误提示
    this.setData({
      'formErrors.infoAnimalId': ''
    });

    this.updateProgress();
    this.saveDraftDebounced();
  },

  onShow() {
    const pages = getCurrentPages();
    const currentPage = pages[pages.length - 1];
    const selectedBreed = currentPage.data.selectedBreed;

    if (selectedBreed) {
      currentPage.setData({ selectedBreed: null });

      this.setData({
        selectedBreed: selectedBreed,
        'formData.infoVarietyId': selectedBreed.iid || selectedBreed.infoVarietyId || 0
      });

      this.updateProgress();
      this.saveDraftDebounced();
    }
  },

  onGenderChange(e) {
    // 原生 radio-group 组件：从 e.detail.value 获取选中的值
    const genderId = parseInt(e.detail.value);
    this.setData({
      'formData.dictSexId': genderId
    });

    // 清除错误提示
    this.setData({
      'formErrors.dictSexId': ''
    });

    this.updateProgress();
    this.saveDraftDebounced();
  },

  onSelectBreed() {
    const animalId = this.data.formData.infoAnimalId;

    if (!animalId || animalId === 0) {
      wx.showToast({
        title: '请先选择物种',
        icon: 'none',
        duration: 2000
      });
      return;
    }

    const animalName = this.data.selectedAnimalName || '';

    wx.navigateTo({
      url: `/pages/breed-select/breed-select?animalId=${animalId}&animalName=${encodeURIComponent(animalName)}`
    });
  },

  onBirthdayChange(e) {
    const birthday = e.detail.value;

    // 计算年龄
    const age = ageCalculator.calculateAge(birthday);
    const ageForDB = this.convertAgeToDBFormat(age);

    // 计算完成进度
    const { formData } = this.data;
    const requiredFields = ['animalName', 'animalNameEn', 'infoAnimalId', 'dictSexId', 'birthday', 'age', 'ownerName', 'ownerNameEn'];
    let completedCount = 0;

    requiredFields.forEach(field => {
      const value = formData[field];
      if (field === 'birthday') {
        if (birthday && birthday.toString().trim()) completedCount++;
      } else if (field === 'age') {
        if (ageForDB && ageForDB.toString().trim()) completedCount++;
      } else {
        if (value && value.toString().trim()) completedCount++;
      }
    });

    let currentStep = 1;
    if (completedCount >= 8) {
      currentStep = 3;
    } else if (completedCount >= 5) {
      currentStep = 2;
    }

    // ✅ 性能优化：合并所有 setData 为一次
    this.setData({
      'formData.birthday': birthday,
      'formData.age': ageForDB,
      'formErrors.birthday': '',
      'formErrors.age': '',
      completedCount,
      currentStep
    });

    // 草稿保存（防抖）
    this.saveDraftDebounced();
  },

  calculateAgeFromBirthday() {
    const { birthday } = this.data.formData;

    if (!birthday) {
      this.setData({
        'formData.age': ''
      });
      return;
    }

    const age = ageCalculator.calculateAge(birthday);
    const ageForDB = this.convertAgeToDBFormat(age);

    this.setData({
      'formData.age': ageForDB
    });
  },

  convertAgeToDBFormat(ageStr) {
    if (!ageStr) return '';

    const yearMonthMatch = ageStr.match(/(\d+)岁(\d+)个月/);
    if (yearMonthMatch) {
      const years = yearMonthMatch[1];
      const months = yearMonthMatch[2];
      return `${years}Y${months}M`;
    }

    const yearMatch = ageStr.match(/(\d+)岁/);
    if (yearMatch) {
      const years = yearMatch[1];
      return `${years}Y`;
    }

    const monthMatch = ageStr.match(/(\d+)个月/);
    if (monthMatch) {
      const months = parseInt(monthMatch[1], 10);
      // 如果是0个月，默认为1个月（宠物刚出生）
      return `${months === 0 ? 1 : months}M`;
    }

    return ageStr;
  },

  /**
   * 表单验证
   */
  validateForm() {
    const { formData } = this.data;
    let isValid = true;
    let firstErrorField = null;

    // 验证所有必填字段
    Object.keys(validationRules).forEach(field => {
      const rule = validationRules[field];
      const value = formData[field];

      // 检查字段是否有值（兼容字符串和数字类型）
      const isEmpty = value === '' || value === null || value === undefined ||
        (typeof value === 'string' && !value.trim()) ||
        (typeof value === 'number' && value === 0);

      if (rule.required && isEmpty) {
        this.setData({
          [`formErrors.${field}`]: rule.message
        });
        isValid = false;

        if (!firstErrorField) {
          firstErrorField = field;
        }
      } else {
        // 重新验证该字段
        const fieldValid = this.validateField(field, value);
        if (!fieldValid && !firstErrorField) {
          firstErrorField = field;
        }
      }
    });

    // 滚动到第一个错误字段
    if (firstErrorField) {
      this.setData({
        scrollIntoView: `field-${firstErrorField}`
      });
    }

    return isValid;
  },

  async onSave() {
    console.log('[PetAdd] 保存按钮被点击');
    console.log('[PetAdd] 当前表单数据:', this.data.formData);

    if (!this.validateForm()) {
      console.log('[PetAdd] 表单验证失败');
      // 使用模态框替代 Toast，确保用户看到错误信息
      wx.showModal({
        title: '提示',
        content: '请完善必填信息',
        showCancel: false,
        confirmText: '我知道了'
      });
      return;
    }

    console.log('[PetAdd] 表单验证通过，开始保存');
    try {
      this.setData({ isSubmitting: true });

      wx.showLoading({
        title: this.data.isEditMode ? '更新中...' : '保存中...',
        mask: true
      });

      const submitData = { ...this.data.formData };

      let result;
      if (this.data.isEditMode) {
        result = await api.pets.update(this.data.petId, submitData);
      } else {
        result = await api.pets.create(submitData);
      }

      wx.hideLoading();

      // 清除草稿
      this.clearDraft();

      // 显示成功提示
      wx.showToast({
        title: this.data.isEditMode ? '更新成功' : '保存成功',
        icon: 'success',
        duration: 1000
      });

      setTimeout(() => {
        wx.navigateBack();
      }, 1000);
    } catch (err) {
      console.error('[PetAdd] 保存失败:', err);
      wx.hideLoading();
      this.setData({ isSubmitting: false });

      wx.showToast({
        title: err.message || '保存失败，请重试',
        icon: 'none',
        duration: 2000
      });
    }
  },

  onBack() {
    // 检查是否有未保存的内容
    const hasContent = Object.values(this.data.formData).some(v => v && v.toString().trim());
    if (hasContent) {
      wx.showModal({
        title: '提示',
        content: '您有未保存的内容，确定要离开吗？',
        confirmText: '离开',
        cancelText: '继续填写',
        success: (res) => {
          if (res.confirm) {
            wx.navigateBack();
          }
        }
      });
    } else {
      wx.navigateBack();
    }
  },

  /**
   * 草稿保存功能
   */
  saveDraftDebounced: debounce(function() {
    this.saveDraft();
  }, 1000),

  saveDraft() {
    const draftData = {
      formData: this.data.formData,
      selectedAnimalName: this.data.selectedAnimalName,
      selectedBreed: this.data.selectedBreed,
      timestamp: Date.now()
    };

    try {
      wx.setStorageSync('pet_add_draft', draftData);

      // 显示草稿保存提示
      this.setData({
        showDraftHint: true
      });

      // 3秒后隐藏提示
      setTimeout(() => {
        this.setData({
          showDraftHint: false
        });
      }, 3000);
    } catch (e) {
      console.error('[PetAdd] 保存草稿失败:', e);
    }
  },

  loadDraft() {
    try {
      const draftData = wx.getStorageSync('pet_add_draft');

      if (draftData && draftData.formData) {
        // 检查草稿是否在24小时内
        const oneDay = 24 * 60 * 60 * 1000;
        const isRecent = draftData.timestamp && (Date.now() - draftData.timestamp) < oneDay;

        if (isRecent) {
          // 恢复草稿数据
          this.setData({
            formData: draftData.formData,
            selectedAnimalName: draftData.selectedAnimalName,
            selectedBreed: draftData.selectedBreed
          });

          // 显示提示
          wx.showToast({
            title: '已恢复上次编辑的内容',
            icon: 'success',
            duration: 1500
          });
        } else {
          // 清除过期草稿
          this.clearDraft();
        }
      }
    } catch (e) {
      console.error('[PetAdd] 加载草稿失败:', e);
    }
  },

  clearDraft() {
    try {
      wx.removeStorageSync('pet_add_draft');
    } catch (e) {
      console.error('[PetAdd] 清除草稿失败:', e);
    }
  },

  formatDate(date) {
    if (!date) return '';

    let dateObj = date;
    if (typeof date === 'string') {
      dateObj = new Date(date);
    }

    if (!(dateObj instanceof Date) || isNaN(dateObj.getTime())) {
      return date;
    }

    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  onUnload() {
    // 页面卸载时保存草稿
    if (!this.data.isSubmitting) {
      this.saveDraft();
    }
  }
});
