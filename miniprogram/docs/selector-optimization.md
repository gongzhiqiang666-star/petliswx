# 大数据量选择框优化方案

## 项目概述

本文档记录了 PETLIS 微信小程序中选择框组件的优化方案，针对不同数据量和使用场景提供了多种实现方式。

---

## 当前实现分析

### 现有方案：独立页面选择器
- **物种选择** (`species-select`)：33个物种选项
- **品种选择** (`breed-select`)：根据物种动态加载，可能上百个品种
- **检测项目** (`order-test-items`)：多选，支持分类标签

**优点**：空间充足，支持搜索和分页
**缺点**：需要页面跳转，交互层级深

---

## 优化方案总览

| 数据量 | 场景 | 推荐方案 | 实例 | 状态 |
|-------|------|---------|------|------|
| < 20 | 固定选项 | 原生 Picker | 性别、绝育状态 | ✅ 已实现 |
| 20-100 | 需要搜索 | 底部弹出选择器 | 物种选择（33项） | ✅ 已实现 |
| > 100 | 大数据量 | 独立页面 + 字母索引 | 品种选择（可能上百项） | ✅ 已实现 |
| 层级数据 | 多级分类 | 级联选择器 | 检测项目分类 | 🔄 待实现 |
| 常用项 | 快速选择 | Tag 标签选择器 | 常用采样类型 | 🔄 待实现 |

---

## 方案1：底部弹出选择器（适合数据量 20-100项）

### 特点
- 半屏弹窗，无需页面跳转
- 保持上下文，操作流畅
- 支持实时搜索
- 选中状态高亮显示

### 组件位置
```
/components/popup-selector/
├── popup-selector.js    # 组件逻辑
├── popup-selector.wxml  # 组件模板
├── popup-selector.wxss  # 组件样式
└── popup-selector.json  # 组件配置
```

### 使用示例

#### 1. 注册组件
```json
{
  "usingComponents": {
    "popup-selector": "/components/popup-selector/popup-selector"
  }
}
```

#### 2. 在页面中使用
```xml
<!-- 触发按钮 -->
<view class="picker-trigger" bindtap="showSelector">
  <text>{{selectedValue || '请选择'}}</text>
  <image src="/assets/icons/arrow-down.png"></image>
</view>

<!-- 底部弹出选择器 -->
<popup-selector
  visible="{{showSelector}}"
  title="选择物种"
  items="{{itemList}}"
  selectedId="{{selectedId}}"
  itemIdKey="id"
  itemNameKey="name"
  itemNameEnKey="nameEn"
  searchPlaceholder="搜索物种名称"
  emptyText="暂无数据"
  bindselect="onSelect"
  bindclose="hideSelector" />
```

#### 3. 页面逻辑
```javascript
Page({
  data: {
    showSelector: false,
    selectedId: null,
    selectedValue: '',
    itemList: []
  },

  // 显示选择器
  showSelector() {
    this.setData({ showSelector: true });
  },

  // 隐藏选择器
  hideSelector() {
    this.setData({ showSelector: false });
  },

  // 选择事件
  onSelect(e) {
    const item = e.detail.item;
    this.setData({
      selectedId: item.id,
      selectedValue: item.name
    });
  }
});
```

### 组件属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| visible | Boolean | false | 是否显示弹窗 |
| title | String | '请选择' | 弹窗标题 |
| items | Array | [] | 选项列表 |
| selectedId | Number/String | null | 当前选中ID |
| showSearch | Boolean | true | 是否显示搜索框 |
| searchPlaceholder | String | '搜索' | 搜索框占位符 |
| emptyText | String | '暂无数据' | 空状态提示 |
| itemIdKey | String | 'id' | 数据ID字段名 |
| itemNameKey | String | 'name' | 数据名称字段名 |
| itemNameEnKey | String | 'nameEn' | 数据英文名称字段名 |

### 组件事件

| 事件名 | 说明 | 参数 |
|--------|------|------|
| select | 选中选项时触发 | { item, id } |
| close | 关闭弹窗时触发 | - |
| scrolltolower | 滚动到底部时触发 | - |

---

## 方案2：独立页面 + 字母索引（适合数据量 >100项）

### 特点
- 独立页面展示，空间充足
- 右侧字母索引导航（类似通讯录）
- 自动按拼音首字母分组
- 支持搜索和分页加载

### 实现位置
```
/pages/breed-select/
├── breed-select.js       # 页面逻辑
├── breed-select.wxml     # 页面模板
├── breed-select.wxss     # 页面样式
└── breed-select.json     # 页面配置

/utils/pinyin-utils.js    # 拼音转换工具
```

### 拼音工具使用

```javascript
const pinyinUtils = require('../../utils/pinyin-utils.js');

// 获取单个汉字的拼音首字母
const letter = pinyinUtils.getFirstLetter('狗'); // 返回 'G'

// 将数据按首字母分组
const grouped = pinyinUtils.groupByFirstLetter(items, 'name');
// 返回: [{ letter: 'G', items: [{ name: '狗', ... }, { name: '龟', ... }] }]
```

### 页面模板示例

```xml
<view class="page">
  <!-- 搜索框 -->
  <view class="search-bar">
    <image src="/assets/icons/icon-search.png" class="search-icon"></image>
    <input placeholder="搜索品种名称" bindinput="onSearch" />
  </view>

  <!-- 列表容器 -->
  <view class="list-container">
    <!-- 滚动列表 -->
    <scroll-view
      class="item-list"
      scroll-y
      scroll-into-view="{{scrollToId}}"
      scroll-with-animation>

      <!-- 分组显示 -->
      <block wx:for="{{groupedItems}}" wx:key="letter">
        <!-- 分组标题 -->
        <view class="group-header" id="group-{{item.letter}}">
          {{item.letter}}
        </view>

        <!-- 该分组下的项 -->
        <view class="list-item"
              wx:for="{{item.items}}"
              wx:for-item="subItem"
              wx:key="id"
              bindtap="onSelect"
              data-item="{{subItem}}">
          <view class="item-name">{{subItem.name}}</view>
          <view class="item-check" wx:if="{{selectedId === subItem.id}}">
            <image src="/assets/icons/check-circle.png"></image>
          </view>
        </view>
      </block>
    </scroll-view>

    <!-- 右侧字母索引 -->
    <view class="index-bar">
      <view class="index-item {{currentLetter === item ? 'active' : ''}}"
            wx:for="{{indexLetters}}"
            wx:key="*this"
            bindtap="onIndexTap"
            data-letter="{{item}}">
        {{item}}
      </view>
    </view>
  </view>
</view>
```

### 页面逻辑示例

```javascript
const pinyinUtils = require('../../utils/pinyin-utils.js');

Page({
  data: {
    allItems: [],
    groupedItems: [],
    indexLetters: [],
    selectedId: null,
    searchKeyword: '',
    scrollToId: '',
    currentLetter: ''
  },

  onLoad() {
    this.loadData();
  },

  async loadData() {
    const items = await api.getItems();

    // 按首字母分组
    const grouped = pinyinUtils.groupByFirstLetter(items, 'name');

    // 提取字母索引
    const indexLetters = grouped.map(g => g.letter).sort();

    this.setData({
      allItems: items,
      groupedItems: grouped,
      indexLetters: indexLetters
    });
  },

  // 字母索引点击
  onIndexTap(e) {
    const letter = e.currentTarget.dataset.letter;
    this.setData({
      currentLetter: letter,
      scrollToId: `group-${letter}`
    });
  },

  // 搜索
  onSearch(e) {
    const keyword = e.detail.value.trim().toLowerCase();

    if (!keyword) {
      // 恢复分组显示
      const grouped = pinyinUtils.groupByFirstLetter(this.data.allItems, 'name');
      this.setData({ groupedItems: grouped });
      return;
    }

    // 搜索过滤
    const filtered = this.data.allItems.filter(item => {
      return item.name.toLowerCase().includes(keyword) ||
             item.nameEn.toLowerCase().includes(keyword);
    });

    // 搜索结果也按字母分组
    const grouped = pinyinUtils.groupByFirstLetter(filtered, 'name');
    this.setData({ groupedItems: grouped });
  }
});
```

---

## 方案3：原生 Picker（适合数据量 <20项）

### 特点
- 微信原生组件，轻量快速
- 符合系统规范
- 数据量小时体验最佳

### 使用示例

```xml
<picker mode="selector"
        range="{{genderOptions}}"
        range-key="name"
        value="{{genderIndex}}"
        bindchange="onGenderChange">
  <view class="picker-trigger">
    {{genderOptions[genderIndex].name}}
    <image src="/assets/icons/arrow-down.png"></image>
  </view>
</picker>
```

```javascript
Page({
  data: {
    genderOptions: [
      { id: 1, name: '公' },
      { id: 2, name: '母' },
      { id: 6, name: '不详' }
    ],
    genderIndex: 0
  },

  onGenderChange(e) {
    const index = e.detail.value;
    const selected = this.data.genderOptions[index];
    this.setData({ genderIndex: index });
    console.log('选中:', selected);
  }
});
```

---

## 性能优化技巧

### 1. 防抖搜索
```javascript
let searchTimer;

onSearch(e) {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    const keyword = e.detail.value;
    this.filterItems(keyword);
  }, 300);
}
```

### 2. 数据缓存
```javascript
async loadItems() {
  const cached = wx.getStorageSync('breed_cache');
  if (cached) {
    this.setData({ items: cached });
    return;
  }

  const items = await api.getItems();
  wx.setStorageSync('breed_cache', items);
  this.setData({ items });
}
```

### 3. 分页加载
```javascript
data: {
  pageSize: 20,
  currentPage: 1,
  hasMore: true
},

onScrollToLower() {
  if (!this.data.hasMore) return;

  const nextPage = this.data.currentPage + 1;
  const endIndex = nextPage * this.data.pageSize;

  this.setData({
    filteredItems: this.data.allItems.slice(0, endIndex),
    currentPage: nextPage,
    hasMore: endIndex < this.data.allItems.length
  });
}
```

---

## 当前项目应用情况

### 已实现的功能

| 页面 | 选择项 | 方案 | 状态 |
|------|--------|------|------|
| 宠物建档 - 物种 | 33项 | 底部弹出选择器 | ✅ 已实现 |
| 宠物建档 - 品种 | 可能上百项 | 独立页面 + 字母索引 | ✅ 已实现 |
| 宠物建档 - 性别 | 4项 | 原生 Picker | ✅ 已实现 |

### 待优化的功能

| 页面 | 选择项 | 当前方案 | 建议方案 | 优先级 |
|------|--------|---------|---------|--------|
| 订单创建 - 采样类型 | ~10项 | 独立页面 | 底部弹窗 | 中 |
| 订单创建 - 检测项目 | ~50项 | 独立页面 | 底部弹窗 + 字母索引 | 高 |
| 报告列表 | 不定 | 原生列表 | 虚拟滚动 | 低 |

---

## 文件清单

### 组件文件
- `/components/popup-selector/popup-selector.js`
- `/components/popup-selector/popup-selector.wxml`
- `/components/popup-selector/popup-selector.wxss`
- `/components/popup-selector/popup-selector.json`

### 工具文件
- `/utils/pinyin-utils.js`

### 页面文件
- `/pages/species-select/` - 物种选择页面
- `/pages/breed-select/` - 品种选择页面
- `/pages/pet-add/` - 宠物建档页面

---

## 更新日志

### 2025-01-XX
- ✅ 创建通用底部弹出选择器组件
- ✅ 物种选择改用底部弹出选择器
- ✅ 品种选择添加字母索引功能
- ✅ 创建拼音转换工具
- ✅ 实现年龄自动计算和手动输入功能

---

## 参考资料

- [微信小程序 scroll-view 文档](https://developers.weixin.qq.com/miniprogram/dev/component/scroll-view.html)
- [微信小程序自定义组件文档](https://developers.weixin.qq.com/miniprogram/dev/framework/custom-component/)
