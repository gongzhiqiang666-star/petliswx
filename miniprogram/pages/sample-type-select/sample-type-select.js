// pages/sample-type-select/sample-type-select.js
const api = require('../../api/index.js');

Page({
  data: {
    allSamples: [],          // 所有样本类型
    filteredSamples: [],     // 筛选后的样本类型
    searchQuery: '',         // 搜索关键词
    pageSize: 50,            // 每页显示数量
    currentPage: 1,          // 当前页码
    hasMore: true,           // 是否还有更多数据
    isLoadingMore: false,    // 是否正在加载更多
    activeTab: 0,            // 当前激活的分类标签索引
    categories: [            // 分类标签
      { name: '全部', filter: null },
      { name: '血液类', filter: 'blood' },
      { name: '组织类', filter: 'tissue' },
      { name: '液体类', filter: 'fluid' },
      { name: '拭子类', filter: 'swab' },
      { name: '其他', filter: 'other' }
    ]
  },

  onLoad() {
    this.loadSampleTypes();
  },

  async loadSampleTypes() {
    try {
      wx.showLoading({ title: '加载中...' });

      const samples = await api.dictionary.getSampleTypes();

      // 处理数据，为空名称提供默认值
      const samplesWithNames = (samples || []).map(item => ({
        ...item,
        sampleName: item.sampleName || `样本类型${item.infoSampleTypeId}`
      }));

      // 只显示第一页数据
      const firstPageSamples = samplesWithNames.slice(0, this.data.pageSize);
      const hasMore = samplesWithNames.length > this.data.pageSize;

      this.setData({
        allSamples: samplesWithNames,
        filteredSamples: firstPageSamples,
        hasMore
      });

      wx.hideLoading();
    } catch (error) {
      console.error('[SampleTypeSelect] 加载样本类型失败:', error);
      wx.hideLoading();
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      });
    }
  },

  /**
   * 根据分类筛选样本类型
   * @param {String} filter 分类过滤器
   */
  filterByCategory(filter) {
    let filtered;

    if (!filter) {
      // 全部
      filtered = this.data.allSamples;
    } else {
      // 根据分类过滤
      filtered = this.data.allSamples.filter(sample => {
        const name = (sample.sampleName || '').toLowerCase();
        const nameEn = (sample.sampleNameEn || '').toLowerCase();

        switch (filter) {
          case 'blood':
            // 血液类：血液、血清、血浆、EDTA、肝素、NAFB(氟化钠)
            return /血|serum|plasma|edta|肝素|nafb/i.test(name) ||
                   /blood|serum|plasma|edta|heparin/i.test(nameEn);
          case 'tissue':
            // 组织类：组织、蜡块、玻片、皮肤、皮屑、毛发、结痂
            return /组织|蜡块|玻片|皮肤|皮屑|毛发|结痂/i.test(name) ||
                   /tissue|wax|slide|skin|hair|crust/i.test(nameEn);
          case 'fluid':
            // 液体类：尿液、脑脊液、胸腔液、腹腔液、灌洗液、冲洗液、关节液、胆汁、抽吸、穿刺、嗉囊、呕吐
            return /尿液|脑脊液|胸腔液|腹腔液|灌洗|冲洗|关节液|胆汁|抽吸|穿刺|嗉囊|呕吐/i.test(name) ||
                   /urine|csf|pleural|abdominal|lavage|rinsing|joint|bile|aspirat|puncture/i.test(nameEn);
          case 'swab':
            // 拭子类：转运拭子、拭子
            return /拭子/i.test(name) || /swab/i.test(nameEn);
          case 'other':
            // 其他：粪便、蜱虫、结石、精液、羽毛、血培养
            return !/血|serum|plasma|edta|肝素|组织|蜡块|玻片|皮肤|尿液|脑脊液|胸腔液|腹腔液|灌洗|拭子/i.test(name) &&
                   !/blood|serum|plasma|edta|tissue|wax|slide|skin|urine|csf|pleural|abdominal|lavage|swab/i.test(nameEn);
          default:
            return true;
        }
      });
    }

    // 只显示第一页
    const firstPage = filtered.slice(0, this.data.pageSize);
    const hasMore = filtered.length > this.data.pageSize;

    this.setData({
      filteredSamples: firstPage,
      currentPage: 1,
      hasMore,
      allFilteredSamples: filtered  // 保存完整的筛选结果用于加载更多
    });
  },

  /**
   * 切换分类标签
   */
  onTabTap(e) {
    const index = e.currentTarget.dataset.index;
    const filter = this.data.categories[index].filter;

    this.setData({
      activeTab: index,
      searchQuery: ''  // 切换分类时清空搜索
    });

    this.filterByCategory(filter);
  },

  // 搜索输入
  onSearchInput(e) {
    const searchQuery = e.detail.value;
    this.setData({ searchQuery });

    if (!searchQuery.trim()) {
      // 清空搜索，恢复当前分类的数据
      this.filterByCategory(this.data.categories[this.data.activeTab].filter);
      return;
    }

    // 执行搜索
    this.performSearch(searchQuery);
  },

  // 执行搜索
  performSearch(query) {
    const lowerQuery = query.toLowerCase();

    // 在当前分类结果中搜索
    const sourceSamples = this.data.allFilteredSamples || this.data.allSamples;

    const filtered = sourceSamples.filter(sample => {
      const cnName = (sample.sampleName || '').toLowerCase();
      const enName = (sample.sampleNameEn || '').toLowerCase();
      return cnName.includes(lowerQuery) || enName.includes(lowerQuery);
    });

    this.setData({
      filteredSamples: filtered,
      hasMore: false  // 搜索状态下不支持加载更多
    });
  },

  // 清除搜索
  onClearSearch() {
    this.setData({ searchQuery: '' });
    // 恢复当前分类的数据
    this.filterByCategory(this.data.categories[this.data.activeTab].filter);
  },

  // 滚动到底部加载更多
  onScrollToLower() {
    // 搜索状态下不加载更多
    if (this.data.searchQuery || !this.data.hasMore || this.data.isLoadingMore) {
      return;
    }

    this.loadMore();
  },

  // 加载更多数据
  loadMore() {
    this.setData({ isLoadingMore: true });

    // 从当前分类的完整结果中加载更多
    const sourceSamples = this.data.allFilteredSamples || this.data.allSamples;
    const startIndex = this.data.currentPage * this.data.pageSize;
    const endIndex = startIndex + this.data.pageSize;
    const moreSamples = sourceSamples.slice(startIndex, endIndex);

    const filteredSamples = [...this.data.filteredSamples, ...moreSamples];
    const hasMore = endIndex < sourceSamples.length;

    this.setData({
      filteredSamples,
      currentPage: this.data.currentPage + 1,
      hasMore,
      isLoadingMore: false
    });
  },

  // 选择样本类型
  onSelectSample(e) {
    const sample = e.currentTarget.dataset.sample;

    // 获取当前页面栈
    const pages = getCurrentPages();
    const prevPage = pages[pages.length - 2];

    // 将选择的样本类型传回上一页
    if (prevPage) {
      prevPage.setData({
        selectedSampleType: sample
      });
    }

    // 返回上一页
    wx.navigateBack();
  }
});
