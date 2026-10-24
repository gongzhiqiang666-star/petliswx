// pages/order-test-items/order-test-items.js
const api = require('../../api/index.js');

Page({
  data: {
    allItems: [],          // 所有已加载的数据（用于维护选中状态）
    allItemsMap: {},       // 所有项目的Map，key为infoTestTypeId，用于快速查找
    displayItems: [],      // 实际显示的项目
    searchQuery: '',
    selectedIds: [],
    filteredItems: [],
    selectedCount: 0,
    totalPrice: '0.00',
    pageSize: 20,          // 每页显示数量
    currentPage: 0,        // 当前页码（从0开始）
    hasMore: true,         // 是否还有更多数据
    isLoadingMore: false,  // 是否正在加载更多
    loading: false,        // 初始加载状态
    showDrawer: false,     // 是否显示已选项目抽屉
    selectedItemsList: [], // 已选项目列表
    searchMode: false,     // 是否处于搜索模式
    searchTimer: null      // 搜索防抖定时器
  },

  onLoad() {
    // 注意：这里不清除缓存，因为用户可能从 preview 页面返回
    // 缓存会在 order-add-pet 和 order-preview 页面清除
    this.loadTestItems();
  },

  onShow() {
    // 如果从 preview 页面返回，需要恢复已选择的项目
    // 从 storage 读取之前的选择状态
    try {
      const formData = wx.getStorageSync('order_form');
      if (formData && formData.selectedTestItems) {
        const selectedIds = formData.selectedTestItems.map(item => item.infoTestTypeId);
        this.setData({
          selectedIds: selectedIds,
          selectedCount: selectedIds.length
        });

        // 更新所有项目的选中状态
        this.updateAllItemsSelection(selectedIds);

        console.log('[OrderTestItems] 已恢复之前的选择:', selectedIds);
      }
    } catch (error) {
      console.error('[OrderTestItems] 恢复选择状态失败:', error);
    }
  },

  async loadTestItems(isLoadMore = false) {
    if (this.data.isLoadingMore) {
      return;
    }

    // 搜索模式下不加载更多
    if (isLoadMore && this.data.searchMode) {
      return;
    }

    if (!isLoadMore) {
      this.setData({ loading: true, currentPage: 0 });
    } else {
      this.setData({ isLoadingMore: true });
    }

    try {
      // 调用分页API
      const pageData = await api.dictionary.getTestTypesPaginated({
        keyword: '',  // 非搜索模式下关键词为空
        page: this.data.currentPage,
        size: this.data.pageSize
      });

      // pageData已经被request.js解包，直接就是分页数据：{ content, totalElements, totalPages, ... }
      const items = pageData.content || [];
      const totalElements = pageData.totalElements || 0;
      const totalPages = pageData.totalPages || 0;

      // 处理数据
      const itemsWithPrices = items.map(item => ({
        ...item,
        testName: item.testName || `检测项目${item.infoTestTypeId}`,
        price: item.price || 0,
        selected: this.data.selectedIds.includes(item.infoTestTypeId)
      }));

      // 合并数据
      const newAllItems = isLoadMore
        ? [...this.data.allItems, ...itemsWithPrices]
        : itemsWithPrices;

      // 维护项目Map，用于快速查找和更新已选列表
      const newAllItemsMap = { ...this.data.allItemsMap };
      itemsWithPrices.forEach(item => {
        newAllItemsMap[item.infoTestTypeId] = item;
      });

      // 判断是否还有更多数据
      const hasMore = (this.data.currentPage + 1) < totalPages;

      this.setData({
        allItems: newAllItems,
        allItemsMap: newAllItemsMap,
        displayItems: newAllItems,
        filteredItems: newAllItems,
        hasMore,
        loading: false,
        isLoadingMore: false
      });

      console.log('[OrderTestItems] 分页加载成功:', {
        currentPage: this.data.currentPage,
        pageSize: this.data.pageSize,
        pageItems: itemsWithPrices.length,
        totalElements,
        totalPages,
        hasMore
      });
    } catch (error) {
      console.error('[OrderTestItems] 加载检测项目失败:', error);
      this.setData({
        loading: false,
        isLoadingMore: false
      });
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      });
    }
  },

  // 搜索功能（支持中文和拼音，带300ms防抖）
  onSearchInput(e) {
    const searchQuery = e.detail.value.trim();

    // 清除之前的定时器
    if (this.data.searchTimer) {
      clearTimeout(this.data.searchTimer);
    }

    this.setData({
      searchQuery,
      searchMode: searchQuery.length > 0
    });

    if (searchQuery.length === 0) {
      // 清空搜索，恢复显示已加载的数据
      this.setData({
        filteredItems: this.data.displayItems,
        hasMore: (this.data.currentPage + 1) < Math.ceil((this.data.displayItems.length) / this.data.pageSize),
        searchMode: false
      });
      return;
    }

    // 设置300ms防抖
    const timer = setTimeout(() => {
      this.performSearch(searchQuery);
    }, 300);

    this.setData({
      searchTimer: timer
    });
  },

  // 执行搜索（调用后端分页API）
  async performSearch(query) {
    this.setData({ loading: true });

    try {
      // 调用后端分页搜索API
      const pageData = await api.dictionary.getTestTypesPaginated({
        keyword: query,
        page: 0,  // 搜索从第一页开始
        size: 100  // 搜索结果可以多一点
      });

      // pageData已经被request.js解包，直接就是分页数据：{ content, totalElements, ... }
      const items = pageData.content || [];
      const totalElements = pageData.totalElements || 0;

      // 处理数据
      const itemsWithPrices = items.map(item => ({
        ...item,
        testName: item.testName || `检测项目${item.infoTestTypeId}`,
        price: item.price || 0,
        selected: this.data.selectedIds.includes(item.infoTestTypeId)
      }));

      // 维护项目Map，将搜索结果也加入Map中
      const newAllItemsMap = { ...this.data.allItemsMap };
      itemsWithPrices.forEach(item => {
        newAllItemsMap[item.infoTestTypeId] = item;
      });

      this.setData({
        filteredItems: itemsWithPrices,
        allItemsMap: newAllItemsMap,
        hasMore: false,  // 搜索模式下不支持加载更多
        loading: false
      });

      console.log('[OrderTestItems] 搜索成功:', {
        keyword: query,
        resultCount: itemsWithPrices.length,
        totalElements
      });
    } catch (error) {
      console.error('[OrderTestItems] 搜索失败:', error);
      this.setData({ loading: false });
      wx.showToast({
        title: '搜索失败，请重试',
        icon: 'none'
      });
    }
  },

  onClearSearch() {
    // 清除搜索防抖定时器
    if (this.data.searchTimer) {
      clearTimeout(this.data.searchTimer);
    }

    this.setData({
      searchQuery: '',
      searchMode: false,
      filteredItems: this.data.displayItems,
      hasMore: (this.data.currentPage + 1) < Math.ceil((this.data.displayItems.length) / this.data.pageSize),
      searchTimer: null
    });
  },

  // 滚动到底部加载更多
  onScrollToLower() {
    // 搜索模式下不加载更多
    if (this.data.searchMode) {
      return;
    }

    // 如果已加载完所有数据，不继续加载
    if (!this.data.hasMore || this.data.isLoadingMore) {
      return;
    }

    // 增加页码并加载更多
    this.setData({
      currentPage: this.data.currentPage + 1
    }, () => {
      this.loadTestItems(true);
    });
  },

  // 切换项目选中状态
  onToggleItem(e) {
    const itemId = e.currentTarget.dataset.id;
    let selectedIds = [...this.data.selectedIds];

    // 切换选中状态
    if (selectedIds.includes(itemId)) {
      selectedIds = selectedIds.filter(id => id !== itemId);
    } else {
      selectedIds.push(itemId);
    }

    // 更新选中状态
    this.updateSelection(selectedIds);
  },

  // 更新所有项目的选中状态（用于从preview页面返回时恢复）
  updateAllItemsSelection(selectedIds) {
    if (!this.data.allItems || this.data.allItems.length === 0) {
      console.warn('[OrderTestItems] allItems为空，无法更新选中状态');
      return;
    }

    const allItems = this.data.allItems.map(item => ({
      ...item,
      selected: selectedIds.includes(item.infoTestTypeId)
    }));

    this.setData({ allItems });

    // 同时更新displayItems和filteredItems
    if (this.data.displayItems && this.data.displayItems.length > 0) {
      const displayItems = this.data.displayItems.map(item => ({
        ...item,
        selected: selectedIds.includes(item.infoTestTypeId)
      }));
      this.setData({ displayItems });
    }

    if (this.data.filteredItems && this.data.filteredItems.length > 0) {
      const filteredItems = this.data.filteredItems.map(item => ({
        ...item,
        selected: selectedIds.includes(item.infoTestTypeId)
      }));
      this.setData({ filteredItems });
    }

    console.log('[OrderTestItems] 已更新所有项目的选中状态');
  },

  // 更新选中状态和已选项目列表
  updateSelection(selectedIds) {
    // 从allItemsMap中获取选中的项目（包括搜索结果中的项目）
    const selectedItems = selectedIds.map(id => this.data.allItemsMap[id]).filter(Boolean);

    // 计算总价
    const totalPrice = selectedItems.reduce((sum, item) => {
      const price = parseFloat(item.price) || 0;
      return sum + price;
    }, 0);

    // 更新列表中的选中状态
    const filteredItems = this.data.filteredItems.map(item => ({
      ...item,
      selected: selectedIds.includes(item.infoTestTypeId)
    }));

    this.setData({
      selectedIds,
      filteredItems,
      selectedItemsList: selectedItems,
      selectedCount: selectedItems.length,
      totalPrice: totalPrice.toFixed(2)
    });
  },

  // 显示已选项目抽屉
  onShowSelectedDrawer() {
    if (this.data.selectedCount > 0) {
      this.setData({
        showDrawer: true
      });
    }
  },

  // 隐藏已选项目抽屉
  onHideDrawer() {
    this.setData({
      showDrawer: false
    });
  },

  // 从抽屉中移除项目
  onRemoveItem(e) {
    const itemId = e.currentTarget.dataset.id;
    const selectedIds = this.data.selectedIds.filter(id => id !== itemId);

    this.updateSelection(selectedIds);
  },

  // 抽屉确认按钮
  onConfirmDrawer() {
    this.setData({
      showDrawer: false
    });
  },

  // 下一步
  onNext() {
    if (this.data.selectedIds.length === 0) {
      wx.showToast({
        title: '请至少选择一个检测项目',
        icon: 'none'
      });
      return;
    }

    const formData = wx.getStorageSync('order_form') || {};

    // 使用 allItemsMap 获取所有选中的项目（包括搜索结果中选择的）
    const selectedItems = this.data.selectedIds
      .map(id => this.data.allItemsMap[id])
      .filter(Boolean);  // 过滤掉undefined

    if (selectedItems.length !== this.data.selectedIds.length) {
      console.warn('[OrderTestItems] 部分选中项目未找到:', {
        selectedIds: this.data.selectedIds,
        foundItems: selectedItems.length
      });
    }

    formData.selectedTestItems = selectedItems;
    wx.setStorageSync('order_form', formData);

    wx.navigateTo({
      url: '/pages/order-preview/order-preview'
    });
  }
});
