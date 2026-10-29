// pages/order-add-pet/order-add-pet.js
const api = require('../../api/index.js');
const ageCalculator = require('../../utils/age-calculator.js');
const app = getApp();

Page({
  data: {
    petId: null,
    draftSaving:false,
    petInfo: null,
    displayAge: '',
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
    selectedSampleTypeName: '',
    sampleFilteredTypes: [],
    sampleCategories: [
      { name: '全部', filter: null },
      { name: '血液类', filter: 'blood' },
      { name: '组织类', filter: 'tissue' },
      { name: '液体类', filter: 'fluid' },
      { name: '拭子类', filter: 'swab' },
      { name: '其他', filter: 'other' }
    ],
    sampleActiveTab: 0,
    sampleSearchQuery: '',
    samplePickerVisible: false,
    sampleTypesLoaded: false,
    samplePickerLoading: false,
    samplePickerError: '',
    speciesOptions: [],
    breedOptions: [],
    breedAnimalId: null,
    speciesPickerVisible: false,
    breedPickerVisible: false,
    speciesLoading: false,
    breedLoading: false,
    speciesError: '',
    breedError: ''
  },

  onLoad(options) {
    const petId = options.petId;
    if(options.draft){const form=wx.getStorageSync('order_form');if(form && String(form.petId)===String(petId)){this.setData({petId:form.petId,petInfo:form.petInfo,sampleInfo:form.sampleInfo,deliveryInfo:form.deliveryInfo,otherInfo:form.otherInfo,animalInfo:form.animalInfo,displayAge:ageCalculator.formatAgeDisplay(form.animalInfo.age),selectedSampleTypeName:form.sampleInfo.sampleTypeName || '',resubmitting:!!form.resubmitOrderId});return;}}

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
    if(this.data.selectedSpecies){const x=this.data.selectedSpecies;this.setData({'petInfo.infoAnimalId':x.infoAnimalId,'petInfo.infoAnimalName':x.animalName,'petInfo.infoVarietyId':null,'petInfo.infoVarietyName':'','petInfo.infoVarietyNameEn':'',selectedSpecies:null});}
    if(this.data.selectedBreed){const x=this.data.selectedBreed;this.setData({'petInfo.infoVarietyId':x.iid,'petInfo.infoVarietyName':x.varietyCn,'petInfo.infoVarietyNameEn':x.varietyEn,selectedBreed:null});}
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
            'animalInfo.age': age,
            displayAge: ageCalculator.formatAgeDisplay(age)
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

  onSelectSampleType() {
    this.setData({ samplePickerVisible: true });
    if (!this.data.sampleTypesLoaded && !this.data.samplePickerLoading) {
      return this.loadSampleTypes();
    }
  },

  onCloseSamplePicker() {
    this.setData({ samplePickerVisible: false });
  },

  onRetrySamples() {
    return this.loadSampleTypes();
  },

  async loadSampleTypes() {
    this.setData({ samplePickerLoading: true, samplePickerError: '' });
    try {
      const samples = await api.dictionary.getSampleTypes();
      const sampleTypes = (samples || []).map(item => ({
        ...item,
        sampleName: item.sampleName || `样本类型${item.infoSampleTypeId}`
      }));
      this.setData({
        sampleTypes,
        sampleTypesLoaded: true,
        samplePickerLoading: false
      });
      this.filterSampleTypes();
    } catch (error) {
      this.setData({
        samplePickerLoading: false,
        samplePickerError: error.message || '加载失败，请重试'
      });
    }
  },

  onSampleTabTap(e) {
    this.setData({
      sampleActiveTab: Number(e.currentTarget.dataset.index),
      sampleSearchQuery: ''
    });
    this.filterSampleTypes();
  },

  onSampleSearchInput(e) {
    this.setData({ sampleSearchQuery: e.detail.value });
    this.filterSampleTypes();
  },

  onClearSampleSearch() {
    this.setData({ sampleSearchQuery: '' });
    this.filterSampleTypes();
  },

  filterSampleTypes() {
    const filter = this.data.sampleCategories[this.data.sampleActiveTab].filter;
    let filtered = this.data.sampleTypes;
    if (filter) {
      filtered = filtered.filter(sample => {
        const name = (sample.sampleName || '').toLowerCase();
        const nameEn = (sample.sampleNameEn || '').toLowerCase();
        switch (filter) {
          case 'blood':
            return /血|serum|plasma|edta|肝素|nafb/i.test(name) || /blood|serum|plasma|edta|heparin/i.test(nameEn);
          case 'tissue':
            return /组织|蜡块|玻片|皮肤|皮屑|毛发|结痂/i.test(name) || /tissue|wax|slide|skin|hair|crust/i.test(nameEn);
          case 'fluid':
            return /尿液|脑脊液|胸腔液|腹腔液|灌洗|冲洗|关节液|胆汁|抽吸|穿刺|嗉囊|呕吐/i.test(name) || /urine|csf|pleural|abdominal|lavage|rinsing|joint|bile|aspirat|puncture/i.test(nameEn);
          case 'swab':
            return /拭子/i.test(name) || /swab/i.test(nameEn);
          case 'other':
            return !/血|serum|plasma|edta|肝素|组织|蜡块|玻片|皮肤|尿液|脑脊液|胸腔液|腹腔液|灌洗|拭子/i.test(name) && !/blood|serum|plasma|edta|tissue|wax|slide|skin|urine|csf|pleural|abdominal|lavage|swab/i.test(nameEn);
          default:
            return true;
        }
      });
    }
    const query = this.data.sampleSearchQuery.trim().toLowerCase();
    if (query) {
      filtered = filtered.filter(sample => {
        const name = (sample.sampleName || '').toLowerCase();
        const nameEn = (sample.sampleNameEn || '').toLowerCase();
        return name.includes(query) || nameEn.includes(query);
      });
    }
    this.setData({ sampleFilteredTypes: filtered });
  },

  onSelectSample(e) {
    const sample = e.currentTarget.dataset.sample;
    this.setData({
      selectedSampleTypeName: sample.sampleName,
      'sampleInfo.infoSampleTypeId': sample.infoSampleTypeId,
      'formErrors.sampleType': '',
      samplePickerVisible: false
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
      'animalInfo.age': e.detail.value,
      displayAge: ageCalculator.formatAgeDisplay(e.detail.value)
    });
  },

  onAgeChange(e) {
    // age-picker 组件的变更事件
    const ageValue = e.detail.value;
    this.setData({
      'animalInfo.age': ageValue,
      displayAge: ageCalculator.formatAgeDisplay(ageValue)
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
    if (!deliveryInfo.doctorName || !deliveryInfo.doctorName.trim()) {
      return '请填写送检医师姓名';
    }

    return null;
  },

  patientField(e){this.setData({['petInfo.'+e.currentTarget.dataset.key]:e.detail.value})},
  birthday(e){this.setData({'petInfo.birthday':e.detail.value})},
  gender(e){this.setData({'petInfo.dictSexId':Number(e.detail.value)})},
  species() {
    this.setData({ speciesPickerVisible: true });
    if (!this.data.speciesOptions.length && !this.data.speciesLoading) {
      return this.loadSpeciesOptions();
    }
  },

  onCloseSpeciesPicker() {
    this.setData({ speciesPickerVisible: false });
  },

  onRetrySpecies() {
    return this.loadSpeciesOptions();
  },

  async loadSpeciesOptions() {
    this.setData({ speciesLoading: true, speciesError: '' });
    try {
      const result = await api.dictionary.getAnimals();
      const speciesOptions = Array.isArray(result) ? result : (result.list || result.data?.list || []);
      this.setData({ speciesOptions, speciesLoading: false });
    } catch (error) {
      this.setData({ speciesLoading: false, speciesError: error.message || '物种加载失败，请重试' });
    }
  },

  onSpeciesSelect(e) {
    const item = e.detail.item;
    this.setData({
      'petInfo.infoAnimalId': item.infoAnimalId,
      'petInfo.infoAnimalName': item.animalName,
      'petInfo.infoVarietyId': null,
      'petInfo.infoVarietyName': '',
      'petInfo.infoVarietyNameEn': '',
      speciesPickerVisible: false,
      breedOptions: [],
      breedAnimalId: null
    });
  },

  breed() {
    const animalId = this.data.petInfo && this.data.petInfo.infoAnimalId;
    if (!animalId) {
      wx.showToast({ title: '请先选择物种', icon: 'none' });
      return;
    }
    this.setData({ breedPickerVisible: true });
    if (this.data.breedAnimalId !== animalId) {
      this.setData({ breedOptions: [], breedAnimalId: animalId });
    }
    if (!this.data.breedOptions.length && !this.data.breedLoading) {
      return this.loadBreedOptions();
    }
  },

  onCloseBreedPicker() {
    this.setData({ breedPickerVisible: false });
  },

  onRetryBreed() {
    return this.loadBreedOptions();
  },

  async loadBreedOptions() {
    this.setData({ breedLoading: true, breedError: '' });
    try {
      const result = await api.dictionary.getVarieties(this.data.breedAnimalId);
      const breedOptions = Array.isArray(result) ? result : (result.data || []);
      this.setData({ breedOptions, breedLoading: false });
    } catch (error) {
      this.setData({ breedLoading: false, breedError: error.message || '品种加载失败，请重试' });
    }
  },

  onBreedSelect(e) {
    const item = e.detail.item;
    this.setData({
      'petInfo.infoVarietyId': item.iid,
      'petInfo.infoVarietyName': item.varietyCn,
      'petInfo.infoVarietyNameEn': item.varietyEn,
      breedPickerVisible: false
    });
  },
  async saveDraft(){
    if(this._savingDraft || !this.data.petInfo)return;this._savingDraft=true;this.setData({draftSaving:true});
    try{await require('../../utils/drafts').save({...wx.getStorageSync('order_form'),petId:this.data.petId,petInfo:this.data.petInfo,sampleInfo:{...this.data.sampleInfo,sampleTypeName:this.data.selectedSampleTypeName},deliveryInfo:this.data.deliveryInfo,otherInfo:this.data.otherInfo,animalInfo:this.data.animalInfo});wx.showToast({title:'草稿已保存'})}catch(e){wx.showModal({title:'保存草稿失败',content:e.message,showCancel:false})}finally{this._savingDraft=false;this.setData({draftSaving:false})}
  },
  onSelectTestItems() {
    // 所有提交（含驳回重提）都必须先补全后端必填的宠物资料，提前提示而不是返回 400。
    const pet=this.data.petInfo;
    if(!pet){wx.showToast({title:'宠物资料未加载完成，请稍后重试',icon:'none'});return;}
    if(!pet.animalName || !pet.animalNameEn){wx.showToast({title:'请补全宠物中/英文姓名',icon:'none'});return;}
    if(!pet.infoVarietyId){wx.showToast({title:'请选择宠物品种后再提交',icon:'none'});return;}
    const error = this.validateForm();
    if (error) {
      wx.showToast({
        title: error,
        icon: 'none'
      });
      return;
    }

    const formData = {
      ...(wx.getStorageSync('order_form') || {}),
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
