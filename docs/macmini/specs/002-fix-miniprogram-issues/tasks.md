# 任务清单: 修复微信小程序显示和流程问题

**功能分支**: `001-fix-miniprogram-issues`
**生成日期**: 2025-12-28
**规格**: [spec.md](./spec.md) | **计划**: [plan.md](./plan.md)

---

## 概览

本文档将实施计划分解为具体的、可执行的任务。任务按用户故事组织,每个用户故事都是独立可测试和交付的增量。

**总任务数**: 50个任务
**预计工作量**: 2-3天
**MVP范围**: Phase 3 (用户故事1: 完成预约检测流程)

---

## Phase 1: 项目设置 (2个任务)

**目标**: 准备开发环境,验证后端服务可用性

- [ ] T001 验证后端API服务运行状态
  - 检查后端服务是否在http://localhost:8080运行
  - 测试API连通性: `curl http://localhost:8080/api/dictionary/animals`
  - 预期响应: `{"code":200,"message":"操作成功","data":[...]}`
  - 如服务未运行,启动后端服务

- [ ] T002 打开微信开发者工具并加载项目
  - 打开`miniprogram`目录
  - 检查`app.js`中的API配置是否正确
  - 编译项目并检查控制台无错误
  - 确认能正常进入小程序首页

---

## Phase 2: 基础设施 (4个任务)

**目标**: 建立共享工具函数和错误处理机制,为所有页面提供基础支持

- [ ] T003 [P] 在`miniprogram/utils/request.js`中添加统一错误处理函数
  - 实现`handleError(error)`函数,返回友好错误消息
  - 处理401状态码: 跳转到登录页面
  - 处理403状态码: 显示"无权限访问"
  - 处理500状态码: 显示"服务器错误,请稍后重试"
  - 使用`wx.showToast()`显示错误提示

- [ ] T004 [P] 在`miniprogram/utils/request.js`中实现手动重试机制
  - 在页面`data`中添加`showError`和`errorMessage`状态
  - 实现统一的API调用wrapper函数,自动catch错误
  - 在错误发生时设置`showError: true`
  - 在WXML中添加重试按钮组件

- [ ] T005 [P] 创建`miniprogram/utils/validators.js`表单验证工具
  - 实现`validateSampleInfo(data)`函数,验证采样信息必填字段
  - 实现`validateDeliveryInfo(data)`函数,验证送检信息必填字段
  - 实现`validateRabiesTest(orderData, testItems)`函数,验证狂犬检测特殊要求
  - 每个函数返回错误数组或null

- [ ] T006 [P] 在`miniprogram/app.js`中初始化全局数据
  - 添加`globalData.userInfo`用于存储当前用户信息
  - 添加`globalData.token`用于存储JWT token
  - 添加`globalData.hospitalInfo`用于存储医院信息
  - 添加`globalData.apiBaseURL`指向API基础URL

---

## Phase 3: 用户故事1 - 完成预约检测流程 (20个任务)

**目标**: 实现从选择宠物到提交预约的完整流程

**独立测试标准**:
- 选择一只已存在的宠物,完成信息填写、项目选择、预览、提交全流程
- 验证狂犬检测特殊验证生效
- 确认订单创建成功并在订单列表中显示为"草稿"状态

**页面**:
- `miniprogram/pages/order-select-pet/order-select-pet.*` (修改)
- `miniprogram/pages/order-add-pet/order-add-pet.*` (新建)
- `miniprogram/pages/order-test-items/order-test-items.*` (新建)
- `miniprogram/pages/order-preview/order-preview.*` (新建)

- [ ] T007 [P] [US1] 修改`miniprogram/pages/order-select-pet/order-select-pet.wxml`
  - 移除"快速预约"和"自定义预约"按钮
  - 将宠物卡片点击事件改为调用`onConfirm`函数
  - 确认按钮文本改为"继续预约"

- [ ] T008 [P] [US1] 修改`miniprogram/pages/order-select-pet/order-select-pet.js`
  - 移除`onQuickOrder()`和`onCustomOrder()`函数
  - 实现`onConfirm(e)`函数:
    - 获取`petId = e.currentTarget.dataset.id`
    - 如果未选择宠物,显示错误提示
    - 跳转到预约信息填写页面: `wx.navigateTo({ url: '/pages/order-add-pet/order-add-pet?petId=' + petId })`

- [ ] T009 [P] [US1] 创建`miniprogram/pages/order-add-pet/`目录

- [ ] T010 [P] [US1] 创建`miniprogram/pages/order-add-pet/order-add-pet.wxml`
  - 添加采样信息表单区域(标题+表单字段)
  - 添加采样类型选择器(`<picker mode="selector">)
  - 添加采样部位输入框(`<input bindinput="onSampleAreaInput">`)
  - 添加采样日期选择器(`<picker mode="date">)
  - 添加送检信息表单区域
  - 添加主人姓名输入框(自动填充宠物信息)
  - 添加主人英文名输入框(自动填充宠物信息)
  - 添加医师姓名输入框
  - 添加其他信息表单区域
  - 添加备注输入框(`<textarea>`)
  - 添加"选择检测项目"按钮(`<button bindtap="onSelectTestItems">`)

- [ ] T011 [US1] 创建`miniprogram/pages/order-add-pet/order-add-pet.wxss`
  - 添加表单区域样式`.form-section { padding: 20rpx; }`
  - 添加表单标题样式`.form-title { font-size: 32rpx; font-weight: bold; }`
  - 添加输入框样式`.input-field { border: 1rpx solid #ddd; padding: 20rpx; margin: 20rpx 0; }`
  - 添加按钮样式`.btn-primary { background: #007aff; color: white; }`

- [ ] T012 [P] [US1] 创建`miniprogram/pages/order-add-pet/order-add-pet.js`
  - 在`data`中添加表单数据结构:
    ```javascript
    data: {
      petId: null,
      petInfo: null,
      sampleInfo: { InfoSampleTypeId: null, SampleArea: '', CollectDt: '', CityId: null },
      deliveryInfo: { MasterName: '', MasterNameEn: '', DoctorName: '' },
      otherInfo: { Remark: '' }
    }
    ```
  - 实现`onLoad(options)`函数,获取URL参数`petId`
  - 实现`loadPetDetail(petId)`函数,调用`api.pets.getDetail(petId)`
  - 实现`loadSampleTypes()`函数,调用`api.dictionary.getSampleTypes()`
  - 实现`loadHospitalInfo()`函数,获取医院信息并自动填充`CityId`
  - 实现`onSampleTypeChange(e)`函数,更新采样类型ID
  - 实现`onSampleAreaInput(e)`函数,更新采样部位
  - 实现`onCollectDateChange(e)`函数,更新采样日期
  - 实现`onMasterNameInput(e)`函数,更新主人姓名
  - 实现`onDoctorNameInput(e)`函数,更新医师姓名
  - 实现`onRemarkInput(e)`函数,更新备注

- [ ] T013 [US1] 在`miniprogram/pages/order-add-pet/order-add-pet.js`中添加表单验证逻辑
  - 导入`validators.js`: `const validators = require('../../utils/validators.js')`
  - 实现`validateForm()`函数,调用`validators.validateSampleInfo()`和`validators.validateDeliveryInfo()`
  - 如有验证错误,使用`wx.showToast()`显示第一个错误消息

- [ ] T014 [P] [US1] 在`miniprogram/pages/order-add-pet/order-add-pet.js`中实现表单数据保存
  - 实现`onSelectTestItems()`函数:
    - 调用`validateForm()`验证表单
    - 如验证失败,return
    - 保存表单数据到storage: `wx.setStorageSync('order_form', { ...this.data })`
    - 跳转到检测项目选择页面

- [ ] T015 [P] [US1] 创建`miniprogram/pages/order-test-items/`目录

- [ ] T016 [P] [US1] 创建`miniprogram/pages/order-test-items/order-test-items.wxml`
  - 添加横向滚动分类标签(`<scroll-view scroll-x class="category-tabs">`)
  - 添加"全部"标签(点击时`currentCategory=0`)
  - 添加分类标签列表(循环渲染`categories`数组)
  - 添加搜索框(`<input bindinput="onSearchInput" placeholder="搜索检测项目">`)
  - 添加项目列表(循环渲染`filteredItems`)
  - 每个项目卡片显示项目名称和价格
  - 选中项目显示勾选图标
  - 添加底部统计栏(显示"已选X项,共¥XXX")
  - 添加"下一步预览"按钮

- [ ] T017 [P] [US1] 创建`miniprogram/pages/order-test-items/order-test-items.wxss`
  - 添加分类标签样式`.category-tabs { display: flex; white-space: nowrap; }`
  - 添加标签项样式`.tab-item { display: inline-block; padding: 20rpx 40rpx; }`
  - 添加激活状态样式`.tab-item.active { color: #007aff; }`
  - 添加项目列表样式`.items-list { margin-bottom: 120rpx; }`
  - 添加项目卡片样式`.item-card { padding: 30rpx; border-bottom: 1rpx solid #eee; }`
  - 添加选中状态样式`.item-card.selected { background: #f0f8ff; }`
  - 添加底部栏样式`.footer { position: fixed; bottom: 0; left: 0; right: 0; }`

- [ ] T018 [P] [US1] 创建`miniprogram/pages/order-test-items/order-test-items.js`
  - 在`data`中添加状态:
    ```javascript
    data: {
      allItems: [],
      categories: [],
      currentCategory: 0,
      searchQuery: '',
      selectedIds: [],
      filteredItems: []
    }
    ```
  - 实现`onLoad()`函数
  - 实现`loadTestItems()`函数,调用`api.dictionary.getTestTypes()`获取所有检测项目
  - 实现`loadCategories()`函数,从检测项目中提取唯一分类或调用分类API
  - 实现`filterItems()`函数,根据当前分类和搜索关键词过滤项目
  - 实现`onCategoryChange(e)`函数,更新`currentCategory`并调用`filterItems()`
  - 实现`onSearchInput(e)`函数,更新`searchQuery`并调用`filterItems()`
  - 实现`onToggleItem(e)`函数:
    - 获取`itemId = e.currentTarget.dataset.id`
    - 切换选中状态(添加或移除`selectedIds`)
    - 调用`updateSummary()`更新统计
  - 实现`updateSummary()`函数:
    - 计算已选项目数量
    - 计算总价(遍历`allItems`,筛选`selectedIds`,累加`TestPrice`)
    - 更新`selectedCount`和`totalPrice`

- [ ] T019 [US1] 在`miniprogram/pages/order-test-items/order-test-items.js`中实现下一步跳转
  - 实现`onNext()`函数:
    - 如果`selectedIds.length === 0`,显示错误提示
    - 从storage读取表单数据: `const formData = wx.getStorageSync('order_form') || {}`
    - 筛选已选项目: `const selectedItems = this.data.allItems.filter(item => this.data.selectedIds.includes(item.InfoTestTypeId))`
    - 更新表单数据: `formData.selectedTestItems = selectedItems`
    - 保存回storage: `wx.setStorageSync('order_form', formData)`
    - 跳转到预览页面

- [ ] T020 [P] [US1] 创建`miniprogram/pages/order-preview/`目录

- [ ] T021 [P] [US1] 创建`miniprogram/pages/order-preview/order-preview.wxml`
  - 添加"宠物信息"区域(显示`formData.petInfo.AnimalName`等字段)
  - 添加"采样信息"区域(显示采样类型、部位、日期)
  - 添加"送检信息"区域(显示主人姓名、医师姓名)
  - 添加"检测项目"区域(循环渲染`selectedTestItems`,显示名称和价格)
  - 添加总价格显示(计算所有检测项价格总和)
  - 添加"返回修改"按钮(`<button bindtap="onBack">`)
  - 添加"提交预约"按钮(`<button bindtap="onSubmit">`)

- [ ] T022 [P] [US1] 创建`miniprogram/pages/order-preview/order-preview.wxss`
  - 添加预览容器样式`.preview-container { padding: 30rpx; }`
  - 添加区域样式`.section { margin-bottom: 40rpx; }`
  - 添加区域标题样式`.section-title { font-size: 32rpx; font-weight: bold; margin-bottom: 20rpx; }`
  - 添加信息行样式`.info-row { padding: 10rpx 0; }`
  - 添加检测项样式`.test-item { display: flex; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid #eee; }`
  - 添加总价样式`.total-price { font-size: 36rpx; font-weight: bold; text-align: right; }`

- [ ] T023 [P] [US1] 创建`miniprogram/pages/order-preview/order-preview.js`
  - 在`data`中添加`formData: {}`和`totalPrice: 0`
  - 实现`onLoad()`函数:
    - 从storage读取表单数据: `const formData = wx.getStorageSync('order_form')`
    - 计算总价: `const totalPrice = formData.selectedTestItems.reduce((sum, item) => sum + item.TestPrice, 0)`
    - 更新`this.setData({ formData, totalPrice })`

- [ ] T024 [US1] 在`miniprogram/pages/order-preview/order-preview.js`中实现返回修改功能
  - 实现`onBack()`函数,调用`wx.navigateBack()`

- [ ] T025 [US1] 在`miniprogram/pages/order-preview/order-preview.js`中实现预约提交
  - 导入`validators.js`
  - 实现`onSubmit()`函数:
    - 检查是否选择了狂犬检测: `const hasRabies = formData.selectedTestItems.some(item => item.InfoTestTypeId === 1225)`
    - 如有狂犬检测,验证`formData.petInfo.ChipNo`和`formData.petInfo.Birthday`是否有值
    - 如验证失败,显示错误提示并return
    - 构造订单数据对象(合并`petInfo`, `sampleInfo`, `deliveryInfo`, `otherInfo`,设置`SampleStatus: -2`)
    - 添加`diagnosisDetails`数组(从`selectedTestItems`映射)
    - 调用`api.orders.create(orderData)`提交订单
    - 如成功,显示成功提示,清理storage(`wx.removeStorageSync('order_form')`)
    - 跳转到订单列表页面: `wx.switchTab({ url: '/pages/orders/orders' })`
    - 如失败,显示错误提示

- [ ] T026 [P] [US1] 在`miniprogram/pages/order-preview/order-preview.js`中添加加载状态
  - 在`data`中添加`submitting: false`
  - 在`onSubmit()`中,提交前设置`this.setData({ submitting: true })`
  - 提交完成后设置`this.setData({ submitting: false })`
  - 在WXML中,提交按钮添加`disabled="{{submitting}}"`属性

---

## Phase 4: 用户故事2 - 修复宠物管理显示 (6个任务)

**目标**: 修复宠物列表和详情页面的字段显示问题

**独立测试标准**: 访问宠物列表页面,验证所有字段正确显示,无undefined或空白

**页面**:
- `miniprogram/pages/pets/pets.*` (修改)
- `miniprogram/pages/pet-detail/pet-detail.*` (修改)
- `miniprogram/pages/pet-add/pet-add.*` (修改)

- [ ] T027 [P] [US2] 修改`miniprogram/pages/pets/pets.wxml`使用后端字段名
  - 将`{{item.animalName}}`改为`{{item.AnimalName}}`
  - 将`{{item.animalNameEn}}`改为`{{item.AnimalNameEn}}`
  - 将`{{item.dictSexId}}`改为`{{item.DictSexId}}`
  - 将`{{item.chipNo}}`改为`{{item.ChipNo}}`
  - 将`{{item.masterName}}`改为`{{item.MasterName}}`
  - 验证性别图标逻辑: `{{item.DictSexId === 1 ? 'male' : 'female'}}`

- [ ] T028 [US2] 修改`miniprogram/pages/pets/pets.js`中的搜索逻辑
  - 在`filterPets()`函数中更新字段引用:
    - `if (pet.AnimalName && pet.AnimalName.includes(query))`
    - `if (pet.AnimalNameEn && pet.AnimalNameEn.toLowerCase().includes(query.toLowerCase()))`
    - `if (pet.ChipNo && pet.ChipNo.includes(query))`

- [ ] T029 [P] [US2] 修改`miniprogram/pages/pet-detail/pet-detail.wxml`使用后端字段名
  - 检查所有宠物信息显示字段,确保使用`AnimalName`、`AnimalNameEn`、`DictSexId`、`ChipNo`、`Birthday`、`MasterName`等字段名
  - 特别注意性别显示: 使用`{{pet.DictSexId}}`并映射为文本(0=未知,1=公,2=母,3=其他)

- [ ] T030 [US2] 修改`miniprogram/pages/pet-detail/pet-detail.js`
  - 确保`onLoad()`函数中正确使用`api.pets.getDetail(id)`获取详情
  - 更新所有字段引用,使用后端字段名

- [ ] T031 [P] [US2] 修改`miniprogram/pages/pet-add/pet-add.wxml`表单字段
  - 确保所有表单输入的`name`属性与后端字段名一致
  - 或者确保提交时正确映射到后端字段名
  - 例如: `<input name="AnimalName">`而不是`<input name="animalName">`

- [ ] T032 [US2] 修改`miniprogram/pages/pet-add/pet-add.js`
  - 在`onLoad()`中,如`options.id`存在(编辑模式),调用`api.pets.getDetail(options.id)`获取宠物详情
  - 预填充表单数据: `this.setData({ AnimalName: pet.AnimalName, AnimalNameEn: pet.AnimalNameEn, ... })`
  - 在提交函数中,使用后端字段名构造数据对象

---

## Phase 5: 用户故事3 - 修复订单列表显示 (5个任务)

**目标**: 修复订单列表和详情页面的字段显示问题

**独立测试标准**: 访问订单列表页面,验证宠物名称正确显示,状态标签显示可读文本

**页面**:
- `miniprogram/pages/orders/orders.*` (修改)
- `miniprogram/pages/order-detail/order-detail.*` (修改)

- [ ] T033 [P] [US3] 修改`miniprogram/pages/orders/orders.wxml`使用AnimalName字段
  - 将`{{item.petName}}`改为`{{item.AnimalName}}`
  - 将`{{item.petBreed}}`改为`{{item.AnimalTypeName}}`(或其他品种字段)
  - 添加状态标签: `<view class="status-badge status-{{item.SampleStatus}}">{{statusLabel}}</view>`

- [ ] T034 [US3] 在`miniprogram/pages/orders/orders.js`中添加状态映射函数
  - 实现`getStatusLabel(status)`函数:
    ```javascript
    const labels = { '-2': '草稿', '0': '待审核', '1': '审核不通过', '2': '等待报告', '3': '已完成', '4': '已取消' };
    return labels[String(status)] || '未知';
    ```
  - 在`filterOrders()`函数中,为每个订单添加`statusLabel`属性: `order.statusLabel = this.getStatusLabel(order.SampleStatus)`

- [ ] T035 [P] [US3] 在`miniprogram/pages/orders/orders.js`中更新订单详情显示逻辑
  - 确保点击订单卡片时,跳转到详情页面: `wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + order.id })`
  - 在`onViewDetail(e)`函数中,获取`orderId = e.currentTarget.dataset.id`

- [ ] T036 [P] [US3] 修改`miniprogram/pages/order-detail/order-detail.wxml`
  - 使用`order.AnimalName`显示宠物名称
  - 使用`order.SampleStatus`并映射为文本显示状态
  - 循环渲染`order.diagnosisDetails`显示检测项列表
  - 显示每个检测项的`TestTypeName`和`TestPrice`

- [ ] T037 [US3] 修改`miniprogram/pages/order-detail/order-detail.js`
  - 在`onLoad(options)`中,使用`api.orders.getDetail(options.id)`获取订单详情
  - 添加状态映射逻辑,将`SampleStatus`转换为可读文本
  - 计算检测项总价(如需要)

---

## Phase 6: 用户故事4和5 - "我的"页面优化 (6个任务)

**目标**: 实现"我的"页面加载真实数据,移除所有菜单项

**独立测试标准**: 访问"我的"页面,验证显示真实用户和医院信息,无菜单项

**页面**:
- `miniprogram/pages/mine/mine.*` (修改)

- [ ] T038 [P] [US4] 在`miniprogram/pages/mine/mine.js`中添加数据加载逻辑
  - 实现`loadUserInfo()`函数: 调用`api.user.getProfile()`
  - 实现`loadHospitalInfo()`函数: 调用`api.dictionary.getHospitals()`并筛选当前用户所属医院
  - 在`onLoad()`中调用这两个函数

- [ ] T039 [US4] 在`miniprogram/pages/mine/mine.js`中添加错误处理
  - 在`loadUserInfo()`的`catch`块中显示错误提示和重试按钮
  - 在`loadHospitalInfo()`的`catch`块中显示错误提示和重试按钮
  - 实现`onRetryUserInfo()`和`onRetryHospitalInfo()`函数

- [ ] T040 [P] [US5] 修改`miniprogram/pages/mine/mine.wxml`移除所有菜单项
  - 删除"图标下载"菜单项及其WXML代码
  - 删除"帮助中心"菜单项及其WXML代码
  - 删除"隐私政策"菜单项及其WXML代码
  - 删除"消息通知"菜单项及其WXML代码
  - 删除"系统设置"菜单项及其WXML代码

- [ ] T041 [US5] 在`miniprogram/pages/mine/mine.wxml`中保留信息卡片和退出按钮
  - 保留用户信息卡片区域(使用真实数据: `{{userInfo.nickname}}`, `{{userInfo.mobile}}`等)
  - 保留医院信息卡片区域(使用真实数据: `{{hospitalInfo.HospitalName}}`, `{{hospitalInfo.Address}}`等)
  - 保留退出登录按钮(`<button bindtap="onLogout">`)

- [ ] T042 [US4] 在`miniprogram/pages/mine/mine.wxml`中添加头像占位符逻辑
  - 使用条件渲染: `<image src="{{userInfo.avatarUrl || '/assets/default-avatar.png'}}"/>`
  - 或使用`wx:if`判断是否有头像

- [ ] T043 [US4] 在`miniprogram/pages/mine/mine.js`中实现退出登录功能
  - 实现`onLogout()`函数:
    - 显示确认对话框: `wx.showModal({ title: '提示', content: '确定要退出登录吗?' })`
    - 如确认,调用`api.auth.logout()`
    - 清理本地存储: `wx.removeStorageSync('user_info')`, `wx.removeStorageSync('hospital_info')`
    - 清理全局数据: `app.globalData.userInfo = null`, `app.globalData.token = null`
    - 跳转到登录页面或首页

---

## Phase 7: 测试与验证 (7个任务)

**目标**: 完整测试所有功能,修复发现的问题

- [ ] T044 测试宠物列表显示
  - 打开宠物列表页面
  - 验证所有宠物正确显示中文名、英文名、品种、性别、年龄、主人姓名
  - 测试搜索功能(按中文名、英文名、芯片号)
  - 测试空状态(无宠物时显示提示和添加按钮)
  - 测试编辑功能(预填充数据是否正确)

- [ ] T045 测试订单列表显示
  - 打开订单列表页面
  - 验证所有订单正确显示宠物名称(使用AnimalName字段)
  - 验证状态标签显示可读文本(草稿、待审核、等待报告、已完成)
  - 测试状态筛选功能
  - 测试订单详情(显示完整检测项列表和价格)

- [ ] T046 测试完整预约流程
  - 从宠物列表选择一只宠物
  - 点击"继续预约"按钮
  - 填写采样信息、送检信息、其他信息
  - 点击"选择检测项目"
  - 选择2-3个检测项目,验证价格计算正确
  - 点击"下一步预览"
  - 查看预览页面信息是否完整
  - 点击"提交预约"
  - 验证订单创建成功,在订单列表中显示为"草稿"状态

- [ ] T047 测试狂犬检测特殊验证
  - 选择宠物,进入预约流程
  - 选择狂犬检测项目(InfoTestTypeId=1225)
  - 不填写芯片号和出生日期,尝试提交
  - 验证显示错误提示:"狂犬检测必须填写芯片号"
  - 填写芯片号和出生日期,再次提交
  - 验证提交成功

- [ ] T048 测试"我的"页面
  - 打开"我的"页面
  - 验证显示真实用户信息(昵称、手机号等)
  - 验证显示真实医院信息(名称、地址、电话等)
  - 验证无菜单项(图标下载、帮助中心、隐私政策等全部移除)
  - 验证只有用户信息卡片、医院信息卡片和退出登录按钮
  - 测试退出登录功能

- [ ] T049 测试API错误处理
  - 关闭后端服务
  - 尝试加载宠物列表或订单列表
  - 验证显示友好的错误提示:"加载失败,请重试"
  - 验证显示手动重试按钮
  - 点击重试按钮,验证能重新加载数据
  - 重启后端服务

- [ ] T050 测试边界情况
  - 宠物列表为空时的提示
  - 订单列表为空时的提示
  - 字段为null时不显示"null"或"undefined"
  - 用户信息包含特殊字符或表情符号时的渲染
  - 快速连续点击按钮时的防抖处理

---

## 依赖关系

```
Phase 1 (设置) → Phase 2 (基础设施) → Phase 3-6 (并行)
                                                    ↓
                                              Phase 7 (测试)
```

**用户故事独立性**:
- ✅ Phase 3 (US1): 预约检测流程 - 独立可测试
- ✅ Phase 4 (US2): 宠物管理显示 - 独立可测试
- ✅ Phase 5 (US3): 订单列表显示 - 独立可测试
- ✅ Phase 6 (US4+US5): "我的"页面 - 独立可测试
- ⚠️ Phase 7 (测试): 需要所有功能完成后才能完整测试

---

## 并行执行机会

### Phase 2内部并行
- T003, T004, T005, T006 可以同时开发(不同的文件)

### Phase 3内部并行
- T007-T008 (order-select-pet页面) 可以与 T009-T014 (order-add-pet页面) 并行
- T015-T019 (order-test-items页面) 可以与前面并行
- 注意: 需要按照页面依赖关系,数据流可能受影响

### Phase 4, 5, 6 并行
- Phase 4 (宠物管理,6个任务)
- Phase 5 (订单管理,5个任务)
- Phase 6 ("我的"页面,6个任务)
- 这三个阶段可以完全并行开发,互不影响

---

## MVP范围建议

**最小可行产品(MVP)**: Phase 1 + Phase 2 + Phase 3

**理由**:
- 核心业务价值: 完成预约检测流程(用户故事1)
- 独立可测试: 可以单独验证这个用户故事
- 技术风险最低: 主要是新建页面,不涉及复杂的字段修改

**增量交付顺序**:
1. **Sprint 1**: Phase 1-3 (预约检测流程) - 2天
2. **Sprint 2**: Phase 4-5 (修复显示问题) - 0.5天
3. **Sprint 3**: Phase 6 ("我的"页面) - 0.5天
4. **Sprint 4**: Phase 7 (测试与修复) - 0.5天

---

## 实施策略

### 渐进式开发
1. **先完成设置和基础设施**(Phase 1-2),确保所有工具函数就绪
2. **优先开发核心业务流程**(Phase 3),提供最大用户价值
3. **修复显示问题**(Phase 4-5),提升用户体验
4. **优化页面**(Phase 6),简化UI
5. **充分测试**(Phase 7),确保质量

### 质量保证
- 每个任务完成后立即测试该功能
- 遵循"直接使用后端字段名"原则,不做映射转换
- 遵循"快速失败+手动重试"错误处理策略
- 所有表单验证在前端完成,后端API会二次验证

### 风险控制
- **风险1**: 后端API字段名不确定 → **缓解**: 使用Postman或查看后端源码确认
- **风险2**: 页面跳转数据丢失 → **缓解**: 使用`wx.setStorageSync()`持久化存储
- **风险3**: 表单验证遗漏 → **缓解**: 创建统一的验证工具函数,强制调用

---

## 任务格式验证

✅ **所有50个任务都遵循严格的checklist格式**:
- 以`- [ ]`开头(markdown checkbox)
- 包含Task ID(T001-T050)
- 可并行任务标记`[P]`
- 用户故事任务标记`[US1]-[US5]`
- 包含清晰的描述和文件路径

✅ **按用户故事组织**,每个故事独立可测试

✅ **包含依赖关系、并行机会、MVP范围、实施策略**

---

**生成时间**: 2025-12-28
**预计开始**: [待定]
**预计完成**: [待定]
