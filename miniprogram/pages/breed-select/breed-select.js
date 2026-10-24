// pages/breed-select/breed-select.js
const api = require('../../api/index.js');
const pinyinUtils = require('../../utils/pinyin-utils.js');

Page({
  data: {
    searchQuery: '',
    allBreeds: [],
    groupedBreeds: [],
    filteredBreeds: [],
    selectedAnimalId: null,
    animalName: '', // 物种名称
    pageSize: 20, // 每页显示数量
    currentPage: 1, // 当前页码
    hasMore: true, // 是否还有更多数据
    loading: false,
    indexLetters: [],
    currentLetter: '',
    scrollToId: ''
  },

  onLoad(options) {
    // 获取传递的物种ID和名称
    if (options.animalId) {
      const animalId = parseInt(options.animalId);
      // 验证animalId是否为有效数字
      if (isNaN(animalId) || animalId <= 0) {
        wx.showToast({
          title: '物种ID无效',
          icon: 'none'
        });
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
        return;
      }
      this.setData({
        selectedAnimalId: animalId
      });
    } else {
      // 没有传递animalId
      wx.showToast({
        title: '缺少物种ID参数',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
      return;
    }

    if (options.animalName) {
      this.setData({
        animalName: decodeURIComponent(options.animalName)
      });
    }

    // 加载品种数据
    this.loadBreeds();
  },

  /**
   * 加载品种数据
   */
  async loadBreeds() {
    try {
      this.setData({ loading: true });

      // 从后端API获取品种数据
      const animalId = this.data.selectedAnimalId;
      const result = await api.dictionary.getVarieties(animalId);

      // 解析返回数据
      const breeds = Array.isArray(result) ? result : (result.data || []);

      console.log('[BreedSelect] 品种数据:', breeds);

      // 按首字母分组
      const grouped = pinyinUtils.groupByFirstLetter(breeds, 'varietyCn');

      // 提取所有字母作为索引
      const indexLetters = grouped.map(g => g.letter).sort();

      this.setData({
        allBreeds: breeds,
        groupedBreeds: grouped,
        filteredBreeds: breeds.slice(0, this.data.pageSize),
        indexLetters: indexLetters,
        currentPage: 1,
        hasMore: breeds.length > this.data.pageSize,
        loading: false
      });
    } catch (error) {
      console.error('[BreedSelect] 加载品种数据失败:', error);
      this.setData({ loading: false });
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      });
    }
  },

  /**
   * 加载更多品种
   */
  loadMore() {
    if (!this.data.hasMore || this.data.searchQuery) {
      return; // 没有更多数据或在搜索状态时不加载
    }

    const nextPage = this.data.currentPage + 1;
    const startIndex = 0;
    const endIndex = nextPage * this.data.pageSize;

    const moreBreeds = this.data.allBreeds.slice(startIndex, endIndex);

    this.setData({
      filteredBreeds: moreBreeds,
      currentPage: nextPage,
      hasMore: endIndex < this.data.allBreeds.length
    });
  },

  /**
   * 滚动到底部事件
   */
  onScrollToLower() {
    console.log('[BreedSelect] 滚动到底部，加载更多');
    this.loadMore();
  },

  /**
   * 字母索引点击
   */
  onIndexTap(e) {
    const letter = e.currentTarget.dataset.letter;
    console.log('[BreedSelect] 点击字母索引:', letter);

    this.setData({
      currentLetter: letter,
      scrollToId: `group-${letter}`
    });
  },

  /**
   * 搜索输入
   */
  onSearchInput(e) {
    const query = e.detail.value.trim().toLowerCase();
    this.setData({
      searchQuery: e.detail.value
    });

    if (!query) {
      // 如果搜索框为空，恢复分组显示
      const grouped = pinyinUtils.groupByFirstLetter(this.data.allBreeds, 'varietyCn');
      this.setData({
        groupedBreeds: grouped,
        hasMore: false
      });
      return;
    }

    // 搜索匹配（支持中文和英文）- 显示所有匹配结果
    const filtered = this.data.allBreeds.filter(breed => {
      const cnMatch = breed.varietyCn.toLowerCase().includes(query);
      const enMatch = breed.varietyEn.toLowerCase().includes(query);
      return cnMatch || enMatch;
    });

    // 搜索结果也按字母分组
    const grouped = pinyinUtils.groupByFirstLetter(filtered, 'varietyCn');

    this.setData({
      groupedBreeds: grouped,
      hasMore: false // 搜索状态下不显示"加载更多"
    });
  },

  /**
   * 选择品种
   */
  onSelectBreed(e) {
    const breed = e.currentTarget.dataset.breed;

    // 获取当前页面栈
    const pages = getCurrentPages();
    const prevPage = pages[pages.length - 2];

    // 将选择的品种传回上一页
    if (prevPage) {
      prevPage.setData({
        selectedBreed: breed,
        'formData.infoVarietyId': breed.iid
      });
    }

    // 返回上一页
    wx.navigateBack();
  }
});
