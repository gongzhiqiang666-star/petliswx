// pages/species-select/species-select.js
const api = require('../../api/index.js');
const pinyinUtils = require('../../utils/pinyin-utils.js');

Page({
  data: {
    allSpecies: [],
    groupedSpecies: [],
    filteredSpecies: [],
    searchKeyword: '',
    pageSize: 20,
    currentPage: 1,
    hasMore: true,
    loading: false,
    selectedId: null,
    indexLetters: [],
    currentLetter: '',
    scrollToId: ''
  },

  onLoad(options) {
    // 获取传入的选中值
    if (options.selectedId) {
      this.setData({ selectedId: parseInt(options.selectedId) });
    }
    this.loadSpecies();
  },

  async loadSpecies() {
    this.setData({ loading: true });

    try {
      const result = await api.dictionary.getAnimals();
      // 后端直接返回数组，不需要取 list 或 data.list
      const species = Array.isArray(result) ? result : (result.list || result.data?.list || []);

      // 在列表最前面添加"请选择物种"选项
      const defaultOption = {
        infoAnimalId: 0,
        animalName: '请选择物种',
        animalNameEn: 'Please select species',
        isActive: 1
      };
      const allSpecies = [defaultOption, ...species];

      console.log('[SpeciesSelect] 物种数据:', allSpecies);

      // 过滤掉默认选项，对真实物种数据进行分组
      const realSpecies = species;

      // 按首字母分组
      const grouped = pinyinUtils.groupByFirstLetter(realSpecies, 'animalName');

      // 提取所有字母作为索引
      const indexLetters = grouped.map(g => g.letter).sort();

      // 初始只显示前几个分组（包含默认选项）
      this.setData({
        allSpecies,
        groupedSpecies: grouped,
        filteredSpecies: allSpecies.slice(0, this.data.pageSize),
        indexLetters: indexLetters,
        currentPage: 1,
        hasMore: allSpecies.length > this.data.pageSize,
        loading: false
      });
    } catch (error) {
      console.error('[SpeciesSelect] 加载物种列表失败:', error);
      this.setData({ loading: false });
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    }
  },

  /**
   * 加载更多物种
   */
  loadMore() {
    if (!this.data.hasMore || this.data.searchKeyword) {
      return; // 没有更多数据或在搜索状态时不加载
    }

    const nextPage = this.data.currentPage + 1;
    const endIndex = nextPage * this.data.pageSize;

    const moreSpecies = this.data.allSpecies.slice(0, endIndex);

    this.setData({
      filteredSpecies: moreSpecies,
      currentPage: nextPage,
      hasMore: endIndex < this.data.allSpecies.length
    });
  },

  /**
   * 滚动到底部事件
   */
  onScrollToLower() {
    console.log('[SpeciesSelect] 滚动到底部，加载更多');
    this.loadMore();
  },

  /**
   * 字母索引点击
   */
  onIndexTap(e) {
    const letter = e.currentTarget.dataset.letter;
    console.log('[SpeciesSelect] 点击字母索引:', letter);

    this.setData({
      currentLetter: letter,
      scrollToId: `group-${letter}`
    });
  },

  onSearch(e) {
    const keyword = e.detail.value.trim().toLowerCase();
    this.setData({ searchKeyword: e.detail.value });

    if (!keyword) {
      // 搜索框为空，恢复分组显示
      const realSpecies = this.data.allSpecies.filter(s => s.infoAnimalId !== 0);
      const grouped = pinyinUtils.groupByFirstLetter(realSpecies, 'animalName');
      this.setData({
        groupedSpecies: grouped,
        hasMore: false
      });
      return;
    }

    // 搜索匹配（支持中文和英文）- 显示所有匹配结果
    const allSpecies = this.data.allSpecies;
    const filtered = allSpecies.filter(item => {
      const nameMatch = item.animalName && item.animalName.toLowerCase().includes(keyword);
      const nameEnMatch = item.animalNameEn && item.animalNameEn.toLowerCase().includes(keyword);
      return nameMatch || nameEnMatch;
    });

    // 将搜索结果按字母分组显示
    const realFiltered = filtered.filter(s => s.infoAnimalId !== 0);
    const grouped = pinyinUtils.groupByFirstLetter(realFiltered, 'animalName');

    this.setData({
      groupedSpecies: grouped,
      hasMore: false // 搜索状态下不显示"加载更多"
    });
  },

  onSelectSpecies(e) {
    const species = e.currentTarget.dataset.species;

    // 检查是否选择了"请选择物种"默认选项
    if (!species.infoAnimalId || species.infoAnimalId === 0) {
      wx.showToast({
        title: '请选择一个物种',
        icon: 'none',
        duration: 2000
      });
      return;
    }

    // 返回上一页并传递选中的物种
    const pages = getCurrentPages();
    const prevPage = pages[pages.length - 2];

    if (prevPage) {
      prevPage.setData({
        selectedSpecies: species
      });
    }

    wx.navigateBack();
  }
});
