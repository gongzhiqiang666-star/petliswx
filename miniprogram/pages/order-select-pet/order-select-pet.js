// pages/order-select-pet/order-select-pet.js
const api = require('../../api/index.js');
const ageCalculator = require('../../utils/age-calculator.js');

Page({
  data: {
    selectedPetId: null,
    pets: [],
    filteredPets: [],
    searchKeyword: '',
    pageSize: 50,  // 增加pageSize，确保能获取所有宠物
    loading: false
  },

  onLoad(options) {
    // Check if there's a newPetId parameter (from newly created pet)
    if (options.newPetId) {
      this.setData({
        selectedPetId: parseInt(options.newPetId)
      });
    }

    this.loadPets();
  },

  onShow() {
    // Refresh pet list when returning from other pages
    // This handles the case when user creates a new pet
    this.loadPets();
  },

  async loadPets() {
    this.setData({ loading: true });

    try {
      // 从后端获取数据，由后端数据库按createtime倒序排序
      const result = await api.pets.getList({
        page: 0,  // Spring Data JPA page starts from 0
        size: this.data.pageSize,
        sort: 'createtime,desc'  // 后端按创建时间倒序排序
      });

      let pets = result.list || result.content || result.data?.list || [];

      console.log('[OrderSelectPet] 从后端获取的原始宠物数据:', pets);
      console.log('[OrderSelectPet] 原始数据长度:', pets.length);
      console.log('[OrderSelectPet] API完整响应:', result);

      // 为每个宠物计算年龄
      pets = pets.map(pet => {
        const processedPet = {
          ...pet,
          // 如果后端没有返回age或age为空，则根据birthday计算
          age: pet.age || this.calculateAgeFromBirthday(pet.birthday)
        };
        // 打印每个宠物的关键信息
        console.log('[OrderSelectPet] 宠物:', processedPet.animalName, {
          ID: processedPet.id,
          createtime: processedPet.createtime,
          animalName: processedPet.animalName,
          animalNameEn: processedPet.animalNameEn,
          ownerName: processedPet.ownerName,
          ownerNameEn: processedPet.ownerNameEn
        });
        return processedPet;
      });

      // 注意：不再进行前端排序，由后端数据库层面排序
      console.log('[OrderSelectPet] 处理后的宠物列表（后端已排序）:', pets.map(p => ({
        name: p.animalName,
        createtime: p.createtime
      })));

      // If no pet is selected, auto-select the newest (first in list)
      let selectedPetId = this.data.selectedPetId;
      if (!selectedPetId && pets.length > 0) {
        selectedPetId = pets[0].id;
        console.log('[OrderSelectPet] 自动选择最新的宠物:', pets[0].animalName);
      }

      this.setData({
        pets,
        filteredPets: pets,
        selectedPetId
      });

      console.log('[OrderSelectPet] 宠物加载完成，总数:', pets.length);
    } catch (error) {
      console.error('[OrderSelectPet] 加载宠物列表失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 根据生日计算年龄
   */
  calculateAgeFromBirthday(birthday) {
    if (!birthday) return '未知'

    try {
      return ageCalculator.calculateAge(birthday)
    } catch (err) {
      console.error('[OrderSelectPet] 计算年龄失败:', err)
      return '未知'
    }
  },

  /**
   * 搜索宠物
   * 支持搜索：宠物中文名、宠物英文名、主人中文名、主人英文名
   */
  async onSearch(e) {
    const keyword = e.detail.value;
    console.log('[OrderSelectPet] 搜索关键词:', keyword);
    console.log('[OrderSelectPet] 当前宠物列表:', this.data.pets);

    this.setData({ searchKeyword: keyword });

    if (!keyword || keyword.trim() === '') {
      this.setData({ filteredPets: this.data.pets });
      return;
    }

    // 本地搜索：支持宠物姓名、主人姓名及其英文名
    const lowerKeyword = keyword.toLowerCase().trim();

    const filtered = this.data.pets.filter(pet => {
      // 宠物中文名搜索
      const animalNameMatch = pet.animalName && pet.animalName.includes(keyword);
      // 宠物英文名搜索（不区分大小写）
      const animalNameEnMatch = pet.animalNameEn && pet.animalNameEn.toLowerCase().includes(lowerKeyword);
      // 主人中文名搜索
      const ownerNameMatch = pet.ownerName && pet.ownerName.includes(keyword);
      // 主人英文名搜索（不区分大小写）
      const ownerNameEnMatch = pet.ownerNameEn && pet.ownerNameEn.toLowerCase().includes(lowerKeyword);

      // 调试：打印匹配结果
      const isMatch = animalNameMatch || animalNameEnMatch || ownerNameMatch || ownerNameEnMatch;
      if (keyword === '平头哥') {
        console.log('[OrderSelectPet] 检查宠物:', pet.animalName, {
          animalNameMatch,
          animalNameEnMatch,
          ownerNameMatch,
          ownerNameEnMatch,
          isMatch,
          pet
        });
      }

      return isMatch;
    });

    console.log('[OrderSelectPet] 搜索结果:', filtered);

    this.setData({
      filteredPets: filtered
    });
  },

  /**
   * 清空搜索
   */
  onClearSearch() {
    this.setData({
      searchKeyword: '',
      filteredPets: this.data.pets
    });
  },

  onSelectPet(e) {
    const petId = e.currentTarget.dataset.id;
    this.setData({
      selectedPetId: petId
    });
  },

  onAddPet() {
    // Navigate to pet-add page with callback URL
    wx.navigateTo({
      url: `/pages/pet-add/pet-add?callback=/pages/order-select-pet/order-select-pet`
    });
  },

  onConfirm(e) {
    const petId = e.currentTarget.dataset.id || this.data.selectedPetId;
    if (!petId) {
      wx.showToast({
        title: '请先选择宠物',
        icon: 'none'
      });
      return;
    }

    // 跳转到预约信息填写页面
    wx.navigateTo({
      url: `/pages/order-add-pet/order-add-pet?petId=${petId}`
    });
  }
});
