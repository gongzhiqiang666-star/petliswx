// pages/pets/pets.js
const api = require('../../api/index.js')
const errorHandler = require('../../utils/error-handler.js')
const ageCalculator = require('../../utils/age-calculator.js')

Page({
  data: {
    searchQuery: '',
    pets: [],
    filteredPets: [],
    loading: false,
    isEmpty: false,
    pageNum: 1,
    pageSize: 10,
    total: 0,
    hasMore: true,
    isLoadingMore: false,
    loadError: ''
  },

  onRetryLoad() {
    this.loadPets()
  },

  onLoad() {
    this.loadPets()
  },

  onShow() {
    // 性能优化：只在列表为空时才自动刷新
    if (this.data.pets.length === 0) {
      this.loadPets()
    }
  },

  /**
   * 加载宠物列表（第一页）
   */
  async loadPets() {
    // 防止重复加载
    if (this.data.loading) {
      return
    }

    this.setData({ loading: true })

    try {
      // 从后端获取数据，由后端按创建时间倒序排序
      const pets = await api.pets.getList({
        page: 0,  // Spring Data JPA page starts from 0
        size: this.data.pageSize,
        sort: 'createtime,desc'  // 后端按创建时间倒序排序
      })

      // 处理返回的数据
      const petList = pets.list || pets.content || pets || []

      // 为每个宠物计算年龄
      const petsWithAge = petList.map(pet => ({
        ...pet,
        // 如果后端没有返回age或age为空，则根据birthday计算
        age: pet.age || this.calculateAgeFromBirthday(pet.birthday)
      }))

      // 注意：不再进行前端排序，由后端数据库层面排序
      console.log('[Pets] 从后端获取的宠物数据（已按createtime倒序）:', petsWithAge.map(p => ({
        name: p.animalName,
        createtime: p.createtime
      })))

      this.setData({
        pets: petsWithAge,
        filteredPets: petsWithAge,
        loading: false,
        isEmpty: petsWithAge.length === 0,
        pageNum: 1,
        total: pets.totalElements || pets.total || 0,
        hasMore: !pets.last,
        loadError: ''
      })
    } catch (err) {
      console.error('[Pets] 加载宠物列表失败:', err)
      this.setData({
        loading: false,
        isEmpty: false,
        loadError: '加载宠物列表失败，请重试'
      })

      // 使用统一错误处理器
      errorHandler.handle(err, {
        showType: 'toast',
        customMessage: '加载宠物列表失败',
        onRetry: () => this.loadPets()
      })
    }
  },

  /**
   * 加载更多宠物
   */
  async loadMore() {
    // 防止重复加载
    if (this.data.isLoadingMore || !this.data.hasMore) {
      return
    }

    this.setData({ isLoadingMore: true })

    try {
      const nextPage = this.data.pageNum

      const pets = await api.pets.getList({
        page: nextPage - 1,  // Spring Data JPA page starts from 0
        size: this.data.pageSize
      })

      // 处理返回的数据
      const petList = pets.list || pets.content || pets || []

      // 为每个宠物计算年龄
      const petsWithAge = petList.map(pet => ({
        ...pet,
        age: pet.age || this.calculateAgeFromBirthday(pet.birthday)
      }))

      this.setData({
        pets: this.data.pets.concat(petsWithAge),
        filteredPets: this.data.pets.concat(petsWithAge),
        pageNum: nextPage + 1,
        isLoadingMore: false,
        total: pets.totalElements || pets.total || 0,
        hasMore: !pets.last
      })
    } catch (err) {
      console.error('[Pets] 加载更多宠物失败:', err)
      this.setData({ isLoadingMore: false })

      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
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
      console.error('[Pets] 计算年龄失败:', err)
      return '未知'
    }
  },

  /**
   * 搜索输入
   */
  onSearchInput(e) {
    const query = e.detail.value
    this.setData({
      searchQuery: query
    })
    this.filterPets(query)
  },

  /**
   * 清除搜索
   */
  onClearSearch() {
    this.setData({
      searchQuery: ''
    })
    this.filterPets('')
  },

  /**
   * 筛选宠物
   * 支持搜索：宠物中文名、宠物英文名、主人中文名、主人英文名、芯片号
   */
  filterPets(query) {
    if (!query) {
      this.setData({
        filteredPets: this.data.pets
      })
      return
    }

    const lowerQuery = query.toLowerCase().trim()

    const filtered = this.data.pets.filter(pet => {
      // 宠物中文名搜索
      if (pet.animalName && pet.animalName.includes(query)) {
        return true
      }
      // 宠物英文名搜索（不区分大小写）
      if (pet.animalNameEn && pet.animalNameEn.toLowerCase().includes(lowerQuery)) {
        return true
      }
      // 主人中文名搜索
      if (pet.ownerName && pet.ownerName.includes(query)) {
        return true
      }
      // 主人英文名搜索（不区分大小写）
      if (pet.ownerNameEn && pet.ownerNameEn.toLowerCase().includes(lowerQuery)) {
        return true
      }
      // 芯片号搜索
      if (pet.chipNo && pet.chipNo.includes(query)) {
        return true
      }
      return false
    })

    this.setData({
      filteredPets: filtered,
      isEmpty: filtered.length === 0
    })
  },

  /**
   * 添加宠物
   */
  onAddPet() {
    wx.navigateTo({
      url: '/pages/pet-add/pet-add'
    })
  },

  /**
   * 查看详情
   */
  onViewDetail(e) {
    const petId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/pet-detail/pet-detail?id=${petId}`
    })
  },

  /**
   * 编辑宠物
   */
  onEditPet(e) {
    const petId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/pet-add/pet-add?id=${petId}`
    })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.resetAndLoadPets().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  /**
   * 触底加载更多
   */
  onReachBottom() {
    console.log('[Pets] 触发加载更多', {
      hasMore: this.data.hasMore,
      isLoadingMore: this.data.isLoadingMore,
      pageNum: this.data.pageNum
    })
    this.loadMore()
  },

  /**
   * 重置并加载宠物列表
   */
  resetAndLoadPets() {
    this.setData({
      pageNum: 1,
      pets: [],
      hasMore: true
    })
    this.loadPets()
  }
})
