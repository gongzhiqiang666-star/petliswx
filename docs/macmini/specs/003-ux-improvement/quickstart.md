# Quickstart Guide: PETLIS UX体验优化

**Feature**: 003-ux-improvement
**Date**: 2025-12-28
**Phase**: Phase 1 - Quickstart Guide

## 概述

本文档为开发人员提供快速上手指南,帮助理解如何实施 PETLIS UX 优化项目。包含环境准备、开发步骤、测试要点等。

---

## 前置准备

### 1. 数据库准备

**性别字典数据修正** (必须执行):

```sql
-- 启用必要的性别选项
UPDATE lin_lb_dictsex SET IsActive = 1 WHERE DictSexId IN (1, 2, 6, 8);

-- 验证结果
SELECT DictSexId, SexName, SexName_en, IsActive FROM lin_lb_dictsex WHERE IsActive = 1;
-- 应该返回4条记录: 不详(6)、雄性(1)、雌性(2)、已绝育(8)
```

**为什么需要**: 当前数据库只有1条启用的性别记录,导致前端选择时只显示1个选项。

### 2. 后端 API 准备

**确认后端 API 是否需要调整**:

根据 `research.md` 的调研,可能需要后端同事协助:

1. **增强宠物搜索 API** (`GET /api/pets/search`):
   - 新增参数: `masterName` (畜主姓名搜索)
   - 新增参数: `pageNum`, `pageSize` (分页)

2. **增强报告列表 API** (`GET /api/reports`):
   - 新增参数: `sortBy` (applyTime/publishTime)
   - 新增参数: `sortOrder` (asc/desc)
   - 新增参数: `petName` (宠物姓名搜索)

3. **确认宠物详情 API** (`GET /api/pets/:id`):
   - 确保返回畜主完整信息 (masterName, masterName_en, masterPhone)

**如果后端无法及时调整**: 前端可以先使用降级方案(纯前端搜索),后续替换为API搜索。

### 3. 开发环境准备

**微信开发者工具**:
- 下载最新版本: https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html
- 导入项目: 选择 `miniprogram/` 目录
- 开启"不校验合法域名"选项 (开发阶段)

**代码编辑器**:
- 推荐 VS Code
- 安装插件: ESLint, Prettier, WXML - Language Service

---

## 开发步骤

### 阶段1: 工具函数开发 (基础)

#### 1.1 年龄计算工具

创建文件: `miniprogram/utils/age-calculator.js`

```javascript
/**
 * 计算宠物年龄
 * @param {String} birthday - 宠物生日,格式: "YYYY-MM-DD"
 * @returns {String} 年龄字符串 "X岁Y个月" | "X个月" | "未知"
 */
function calculateAge(birthday) {
  if (!birthday) return "未知";

  const birth = new Date(birthday);
  const today = new Date();

  // 检查日期是否有效
  if (isNaN(birth.getTime())) return "未知";

  let years = today.getFullYear() - birth.getFullYear();
  let months = today.getMonth() - birth.getMonth();

  // 如果月份差为负,借位
  if (months < 0) {
    years--;
    months += 12;
  }

  // 如果小于1岁,显示月份
  if (years < 1) {
    return `${months === 0 ? 1 : months}个月`;
  } else {
    return `${years}岁${months}个月`;
  }
}

module.exports = {
  calculateAge
}
```

**测试用例**:
```javascript
// 在微信开发者工具控制台测试
const ageCalculator = require('../../utils/age-calculator.js');

console.log(ageCalculator.calculateAge("2023-06-01")); // 约1岁多
console.log(ageCalculator.calculateAge("2024-11-01")); // 约2个月
console.log(ageCalculator.calculateAge(null)); // "未知"
console.log(ageCalculator.calculateAge("invalid-date")); // "未知"
```

---

### 阶段2: 可复用组件开发

#### 2.1 搜索输入组件

创建目录: `miniprogram/components/search-input/`

**search-input.js**:
```javascript
Component({
  properties: {
    placeholder: {
      type: String,
      value: '搜索...'
    },
    debounceTime: {
      type: Number,
      value: 400  // 防抖时间400ms
    }
  },

  data: {
    value: ''
  },

  methods: {
    onInput(e) {
      const value = e.detail.value;
      this.setData({ value });

      // 防抖处理
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer);
      }

      this.debounceTimer = setTimeout(() => {
        this.triggerEvent('search', { value });
      }, this.data.debounceTime);
    },

    onClear() {
      this.setData({ value: '' });
      this.triggerEvent('search', { value: '' });
    }
  }
})
```

**search-input.wxml**:
```xml
<view class="search-input-wrapper">
  <image src="/assets/icons/icon-search.png" class="search-icon" mode="aspectFit"></image>
  <input
    class="search-input"
    placeholder="{{placeholder}}"
    value="{{value}}"
    bindinput="onInput"
  />
  <image
    wx:if="{{value}}"
    src="/assets/icons/icon-close.png"
    class="clear-icon"
    mode="aspectFit"
    bindtap="onClear"
  ></image>
</view>
```

**使用方式** (在页面中):
```xml
<search-input
  placeholder="搜索宠物姓名或主人姓名"
  bind:search="onSearch"
/>
```

#### 2.2 分页组件

创建目录: `miniprogram/components/pagination/`

**pagination.js**:
```javascript
Component({
  properties: {
    current: {
      type: Number,
      value: 1
    },
    total: {
      type: Number,
      value: 0
    },
    pageSize: {
      type: Number,
      value: 20
    }
  },

  computed: {
    totalPages() {
      return Math.ceil(this.properties.total / this.properties.pageSize);
    }
  },

  methods: {
    onPrev() {
      if (this.properties.current > 1) {
        this.triggerEvent('change', { page: this.properties.current - 1 });
      }
    },

    onNext() {
      if (this.properties.current < this.totalPages) {
        this.triggerEvent('change', { page: this.properties.current + 1 });
      }
    }
  }
})
```

**pagination.wxml**:
```xml
<view class="pagination">
  <button class="page-btn" disabled="{{current <= 1}}" bindtap="onPrev">上一页</button>
  <view class="page-info">{{current}} / {{totalPages}}</view>
  <button class="page-btn" disabled="{{current >= totalPages}}" bindtap="onNext">下一页</button>
</view>
```

#### 2.3 宠物卡片组件

创建目录: `miniprogram/components/pet-card/`

**pet-card.js**:
```javascript
const ageCalculator = require('../../utils/age-calculator.js');

Component({
  properties: {
    pet: {
      type: Object,
      value: {}
    },
    selected: {
      type: Boolean,
      value: false
    }
  },

  computed: {
    age() {
      return ageCalculator.calculateAge(this.properties.pet.birthday);
    }
  }
})
```

**pet-card.wxml**:
```xml
<view class="pet-card {{selected ? 'selected' : ''}}" bindtap="onTap">
  <view class="pet-avatar">{{pet.animalName[0] || '🐱'}}</view>
  <view class="pet-info">
    <view class="pet-name">{{pet.animalName}}</view>
    <view class="pet-breed">{{pet.infoVarietyName || '未知品种'}}</view>
    <view class="pet-meta">
      <text class="pet-age">{{age}}</text>
      <text class="pet-sex">{{pet.dictSexName}}</text>
    </view>
  </view>
  <view class="pet-owner">主人: {{pet.masterName}}</view>
  <view class="pet-check" wx:if="{{selected}}">
    <image src="/assets/icons/check-circle.png" class="check-icon"></image>
  </view>
</view>
```

---

### 阶段3: 页面修改

#### 3.1 宠物选择页面 (`pages/order-select-pet/order-select-pet`)

**核心修改点**:

1. **引入搜索组件**:
```xml
<view class="page-content">
  <search-input
    placeholder="搜索宠物姓名或主人姓名"
    bind:search="onSearch"
  />

  <view class="pet-list">
    <pet-card
      wx:for="{{pets}}"
      wx:key="id"
      pet="{{item}}"
      selected="{{selectedPetId === item.id}}"
      bindtap="onSelectPet"
      data-id="{{item.id}}"
    />
  </view>

  <pagination
    current="{{pageNum}}"
    total="{{total}}"
    pageSize="{{pageSize}}"
    bind:change="onPageChange"
  />
</view>
```

2. **页面逻辑**:
```javascript
const api = require('../../api/index.js');

Page({
  data: {
    pets: [],
    filteredPets: [],
    selectedPetId: null,
    searchKeyword: '',
    pageNum: 1,
    pageSize: 20,
    total: 0,
    loading: false
  },

  onLoad() {
    this.loadPets();
  },

  async loadPets() {
    this.setData({ loading: true });

    try {
      const res = await api.pets.getList({
        pageNum: this.data.pageNum,
        pageSize: this.data.pageSize
      });

      this.setData({
        pets: res.data.list,
        filteredPets: res.data.list,
        total: res.data.total
      });
    } catch (error) {
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onSearch(e) {
    const keyword = e.detail.value;
    this.setData({ searchKeyword: keyword, pageNum: 1 });

    if (!keyword) {
      this.setData({ filteredPets: this.data.pets });
      return;
    }

    // 调用增强的搜索 API
    try {
      const res = await api.pets.search({
        name: keyword,
        masterName: keyword,
        pageNum: 1,
        pageSize: this.data.pageSize
      });

      this.setData({
        filteredPets: res.data.list,
        total: res.data.total
      });
    } catch (error) {
      console.error('搜索失败', error);
      // 降级方案: 本地过滤
      const filtered = this.data.pets.filter(pet =>
        pet.animalName.includes(keyword) ||
        pet.masterName.includes(keyword)
      );
      this.setData({ filteredPets: filtered });
    }
  },

  onSelectPet(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ selectedPetId: id });
  },

  onPageChange(e) {
    const page = e.detail.page;
    this.setData({ pageNum: page });
    this.loadPets();
  }
})
```

#### 3.2 报告列表页面 (`pages/report-list/report-list`)

**核心修改点**:

1. **排序选项卡**:
```xml
<view class="sort-tabs">
  <view
    class="sort-tab {{sortBy === 'applyTime' ? 'active' : ''}}"
    bindtap="onSortChange"
    data-sortBy="applyTime"
  >
    申请时间
    <image wx:if="{{sortBy === 'applyTime'}}" src="/assets/icons/sort-{{sortOrder}}.png"></image>
  </view>
  <view
    class="sort-tab {{sortBy === 'publishTime' ? 'active' : ''}}"
    bindtap="onSortChange"
    data-sortBy="publishTime"
  >
    发布时间
    <image wx:if="{{sortBy === 'publishTime'}}" src="/assets/icons/sort-{{sortOrder}}.png"></image>
  </view>
</view>

<search-input placeholder="搜索报告" bind:search="onSearch" />
```

2. **页面逻辑**:
```javascript
Page({
  data: {
    reports: [],
    filteredReports: [],
    sortBy: 'applyTime',
    sortOrder: 'desc',
    searchKeyword: '',
    pageNum: 1,
    pageSize: 20,
    total: 0
  },

  onLoad() {
    // 从本地缓存读取排序偏好
    const sortPref = wx.getStorageSync('reportSortPreference') || {
      sortBy: 'applyTime',
      sortOrder: 'desc'
    };
    this.setData({
      sortBy: sortPref.sortBy,
      sortOrder: sortPref.sortOrder
    });

    this.loadReports();
  },

  async loadReports() {
    try {
      const res = await api.reports.getList({
        pageNum: this.data.pageNum,
        pageSize: this.data.pageSize,
        sortBy: this.data.sortBy,
        sortOrder: this.data.sortOrder,
        petName: this.data.searchKeyword
      });

      this.setData({
        reports: res.data.list,
        filteredReports: res.data.list,
        total: res.data.total
      });
    } catch (error) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  onSortChange(e) {
    const sortBy = e.currentTarget.dataset.sortBy;
    let sortOrder = 'desc';

    // 如果点击当前排序字段,切换排序方向
    if (sortBy === this.data.sortBy) {
      sortOrder = this.data.sortOrder === 'desc' ? 'asc' : 'desc';
    }

    this.setData({ sortBy, sortOrder, pageNum: 1 });

    // 保存排序偏好
    wx.setStorageSync('reportSortPreference', { sortBy, sortOrder });

    this.loadReports();
  },

  onSearch(e) {
    const keyword = e.detail.value;
    this.setData({ searchKeyword: keyword, pageNum: 1 });
    this.loadReports();
  }
})
```

#### 3.3 页面布局居中修复

在 `app.wxss` 中添加全局样式:

```css
/* 统一的页面容器 */
.page {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  background-color: #f9fafb;
}

.page-content {
  width: 100%;
  max-width: 750px;
  padding: 0 32rpx;
  box-sizing: border-box;
}

/* 需要修复的页面添加这些类 */
```

然后在需要修复的页面的 WXML 中:

```xml
<view class="page">
  <view class="page-content">
    <!-- 页面内容 -->
  </view>
</view>
```

---

## 测试清单

### 功能测试

- [ ] **搜索功能**:
  - [ ] 输入关键词后能正确过滤结果
  - [ ] 防抖功能正常 (输入停止400ms后才触发搜索)
  - [ ] 清空搜索后显示完整列表
  - [ ] 搜索结果为空时显示"未找到匹配结果"

- [ ] **分页功能**:
  - [ ] 切换页面后数据正确更新
  - [ ] 页码显示正确 (当前页/总页数)
  - [ ] 上一页/下一页按钮禁用状态正确

- [ ] **年龄计算**:
  - [ ] 有生日信息的宠物显示正确年龄
  - [ ] 无生日信息的宠物显示"未知"
  - [ ] 年龄格式正确 ("X岁Y个月" 或 "X个月")

- [ ] **性别选择**:
  - [ ] 选择器显示4个选项 (不详、雄性、雌性、已绝育)
  - [ ] 默认选择"不详"
  - [ ] 选中的选项正确显示

- [ ] **畜主信息自动填充**:
  - [ ] 选择宠物后畜主信息自动填充
  - [ ] 可以修改自动填充的信息
  - [ ] API失败时显示友好提示

- [ ] **报告排序**:
  - [ ] 切换排序方式结果正确
  - [ ] 排序偏好保存到本地缓存
  - [ ] 下次打开应用时排序偏好正确加载

- [ ] **页面布局**:
  - [ ] 所有页面内容居中显示
  - [ ] 最大宽度不超过750px
  - [ ] 布局在不同屏幕尺寸下正常

### 性能测试

- [ ] **页面加载**: 首屏加载时间 < 2秒
- [ ] **搜索响应**: 搜索响应时间 < 1秒
- [ ] **分页切换**: 分页加载时间 < 500ms
- [ ] **动画流畅**: 页面滚动、切换动画流畅 (60fps)

### 兼容性测试

- [ ] **iOS**: 在 iPhone 上测试所有功能
- [ ] **Android**: 在 Android 手机上测试所有功能
- [ ] **不同屏幕**: 测试不同尺寸屏幕的显示效果

---

## 常见问题

### Q1: 年龄计算不准确?

**A**: 检查 `birthday` 字段格式是否为 "YYYY-MM-DD"。如果格式不正确,会导致 `new Date()` 返回 Invalid Date。

**调试方法**:
```javascript
console.log('生日:', birthday);
console.log('解析结果:', new Date(birthday));
console.log('是否有效:', !isNaN(new Date(birthday).getTime()));
```

### Q2: 搜索结果为空?

**A**: 检查以下几点:
1. API 调用是否成功 (查看 Network 面板)
2. 搜索参数是否正确传递
3. 后端是否实现了增强的搜索功能

**降级方案**: 如果后端API未实现,可以使用前端本地搜索:
```javascript
const filtered = this.data.pets.filter(pet =>
  pet.animalName.includes(keyword) ||
  pet.masterName.includes(keyword)
);
```

### Q3: 性别只显示1个选项?

**A**: 需要更新数据库,启用4个性别选项。参考"数据库准备"章节的SQL语句。

### Q4: 页面布局不居中?

**A**: 确保在页面 WXML 最外层使用 `<view class="page">`,内容区域使用 `<view class="page-content">`。

### Q5: 防抖不生效?

**A**: 检查 `setTimeout` 和 `clearTimeout` 是否正确使用。确保在每次输入前都清除之前的定时器。

---

## 代码规范

### 命名规范

- **组件名**: 大驼峰 (PascalCase) - `SearchInput`, `Pagination`
- **文件名**: 小写短横线 - `search-input.js`, `pet-card.wxml`
- **变量/函数**: 小驼峰 (camelCase) - `searchKeyword`, `loadPets`
- **常量**: 大写下划线 - `MAX_PAGE_SIZE`, `DEFAULT_DEBOUNCE_TIME`

### 注释规范

**文件头注释**:
```javascript
/**
 * 宠物卡片组件
 * 显示宠物信息,包括姓名、品种、年龄、性别等
 */
```

**函数注释**:
```javascript
/**
 * 加载宠物列表
 * @param {Number} pageNum - 页码
 * @param {Number} pageSize - 每页数量
 */
async loadPets(pageNum, pageSize) {
  // ...
}
```

### 样式规范

**使用 rpx 单位** (微信小程序响应式像素):
```css
.container {
  padding: 32rpx;  /* 在不同屏幕上自适应 */
}
```

**遵循BEM命名规范**:
```css
.pet-card { }           /* Block */
.pet-card__title { }    /* Element */
.pet-card--selected { } /* Modifier */
```

---

## 部署检查清单

- [ ] 所有 ESLint 错误已修复
- [ ] 所有测试用例通过
- [ ] 代码已提交到 Git
- [ ] 性能测试达标
- [ ] 在真机上测试通过
- [ ] 代码已通过 Code Review
- [ ] 更新了用户文档 (如需要)

---

## 下一步

完成开发后,执行以下步骤:

1. **代码审查**: 提交 Pull Request,邀请同事 review
2. **测试验证**: 使用测试清单逐项验证
3. **性能优化**: 使用微信开发者工具的性能面板检查
4. **发布上线**: 上传代码到微信平台,提交审核

---

**文档版本**: 1.0.0
**最后更新**: 2025-12-28
