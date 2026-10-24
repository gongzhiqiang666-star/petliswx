# API接口契约: 微信小程序前端

**功能**: 001-fix-miniprogram-issues
**日期**: 2025-12-28
**类型**: 前端页面接口契约(不涉及后端API定义)

---

## 概述

本功能纯前端修改,不涉及后端API变更。本文档定义前端页面之间的接口契约和数据传递规范。

---

## 页面跳转接口

### 1. 选择宠物 → 预约信息填写

**起点**: `pages/order-select-pet/order-select-pet`
**终点**: `pages/order-add-pet/order-add-pet`

**触发**: 用户点击"继续预约"按钮

**传递数据**:
```javascript
// 方式1: URL参数
wx.navigateTo({
  url: `/pages/order-add-pet/order-add-pet?petId=${petId}&petName=${petName}`
});

// 方式2: 本地存储
wx.setStorageSync('selected_pet', { petId, petName, ...petInfo });
wx.navigateTo({
  url: '/pages/order-add-pet/order-add-pet'
});
```

**接收数据**:
```javascript
onLoad(options) {
  const petId = options.petId;
  // 或从storage读取
  const pet = wx.getStorageSync('selected_pet');
}
```

---

### 2. 预约信息填写 → 检测项目选择

**起点**: `pages/order-add-pet/order-add-pet`
**终点**: `pages/order-test-items/order-test-items`

**触发**: 用户点击"选择检测项目"按钮

**前置条件**: 用户已填写采样信息、其他信息、送检信息

**传递数据**:
```javascript
// 保存表单数据到storage
wx.setStorageSync('order_form', {
  petId: this.data.petId,
  sampleInfo: this.data.sampleInfo,
  otherInfo: this.data.otherInfo,
  deliveryInfo: this.data.deliveryInfo
});

wx.navigateTo({
  url: '/pages/order-test-items/order-test-items'
});
```

**接收数据**:
```javascript
onLoad() {
  const formData = wx.getStorageSync('order_form') || {};
  this.setData({
    petId: formData.petId,
    cityId: formData.sampleInfo.CityId
  });
}
```

---

### 3. 检测项目选择 → 预约预览

**起点**: `pages/order-test-items/order-test-items`
**终点**: `pages/order-preview/order-preview`

**触发**: 用户点击"下一步预览"按钮

**前置条件**: 用户至少选择一个检测项目

**传递数据**:
```javascript
// 更新storage中的已选项目
const formData = wx.getStorageSync('order_form') || {};
formData.selectedTestItems = this.data.selectedItems;
wx.setStorageSync('order_form', formData);

wx.navigateTo({
  url: '/pages/order-preview/order-preview'
});
```

---

### 4. 预约预览 → 订单列表

**起点**: `pages/order-preview/order-preview`
**终点**: `pages/orders/orders`

**触发**: 提交预约成功后自动跳转

**操作**:
```javascript
async function submitOrder() {
  try {
    await api.orders.create(orderData);
    wx.showToast({ title: '预约创建成功', icon: 'success' });

    // 清理storage
    wx.removeStorageSync('order_form');
    wx.removeStorageSync('selected_pet');

    // 跳转到订单列表(TabBar页面)
    setTimeout(() => {
      wx.switchTab({
        url: '/pages/orders/orders'
      });
    }, 1500);
  } catch (error) {
    wx.showToast({ title: '提交失败', icon: 'none' });
  }
}
```

---

## 组件接口契约

### 宠物卡片组件

**用途**: 在宠物列表、订单详情等页面复用

**Props**:
```javascript
{
  "pet": {
    "id": Number,
    "AnimalName": String,
    "AnimalNameEn": String,
    "DictSexId": Number,
    "Age": String,
    "ChipNo": String
  },
  "showEdit": Boolean,     // 是否显示编辑按钮
  "showDelete": Boolean,   // 是否显示删除按钮
  "onTap": Function        // 点击卡片回调
}
```

**Events**:
```javascript
this.triggerEvent('edit', { petId: this.data.pet.id });
this.triggerEvent('delete', { petId: this.data.pet.id });
```

---

### 检测项目选择组件

**用途**: 检测项目选择页面的核心组件

**Props**:
```javascript
{
  "testItems": Array<{      // 所有检测项目列表
    "InfoTestTypeId": Number,
    "TestTypeName": String,
    "TestPrice": Number,
    "CategoryId": Number
  }>,
  "selectedIds": Array<Number>,  // 已选项目ID列表
  "categories": Array<{    // 分类列表
    "CategoryId": Number,
    "CategoryName": String
  }>,
  "currentCategory": Number  // 当前选中分类
}
```

**Events**:
```javascript
this.triggerEvent('selectitem', {
  itemId: item.InfoTestTypeId,
  selected: !isSelected
});

this.triggerEvent('categorychange', {
  categoryId: newCategoryId
});
```

**Data**:
```javascript
{
  "filteredItems": Array,    // 当前分类下的项目
  "totalPrice": Number,      // 已选项目总价
  "selectedCount": Number    // 已选项目数量
}
```

---

### 表单输入组件

**用途**: 采样信息、送检信息表单输入

**Props**:
```javascript
{
  "fields": Array<{         // 表单字段定义
    "key": String,          // 字段名
    "label": String,        // 字段标签
    "type": String,         // 输入类型(text/number/date/picker)
    "required": Boolean,    // 是否必填
    "options": Array        // 选项(picker类型)
  }>,
  "value": Object           // 表单数据
}
```

**Events**:
```javascript
this.triggerEvent('input', {
  key: 'SampleArea',
  value: e.detail.value
});
```

---

## 数据存储接口

### LocalStorage Key命名规范

```javascript
// 预约表单临时数据
'order_form'

// 选中的宠物信息
'selected_pet'

// 当前用户信息
'user_info'

// 当前医院信息
'hospital_info'

// 检测项目缓存(可选)
'test_items_cache'

// 字典数据缓存
'dictionary_{type}'  // 如: dictionary_sexes, dictionary_cities
```

### Storage操作规范

```javascript
// 读取(带默认值)
const formData = wx.getStorageSync('order_form') || {};

// 保存(带异常处理)
try {
  wx.setStorageSync('order_form', formData);
} catch (e) {
  console.error('保存失败', e);
}

// 清理
wx.removeStorageSync('order_form');

// 检查是否存在
const hasData = wx.getStorageSync('order_form') !== '';
```

---

## 全局数据接口

### app.globalData

```javascript
// app.js
App({
  globalData: {
    userInfo: null,      // 当前用户信息
    token: null,         // JWT token
    hospitalInfo: null,  // 医院信息
    apiBaseURL: 'http://localhost:8080/api'  // API基础URL
  }
});
```

### 访问方式

```javascript
const app = getApp();
const token = app.globalData.token;
```

---

## 错误处理接口

### 统一错误处理

```javascript
// utils/request.js
function handleError(error) {
  console.error('[API Error]', error);

  let message = '操作失败';

  if (error.statusCode === 401) {
    message = '登录已过期,请重新登录';
    wx.redirectTo({ url: '/pages/login/login' });
  } else if (error.statusCode === 403) {
    message = '无权限访问';
  } else if (error.statusCode === 500) {
    message = '服务器错误,请稍后重试';
  } else if (error.data && error.data.message) {
    message = error.data.message;
  }

  wx.showToast({
    title: message,
    icon: 'none',
    duration: 3000
  });

  return message;
}
```

### 页面级错误处理

```javascript
// 页面中显示错误状态
Page({
  data: {
    error: null,
    showError: false
  },

  onLoad() {
    this.loadData().catch(error => {
      this.setData({
        error: error.message,
        showError: true
      });
    });
  },

  onRetry() {
    this.setData({ showError: false });
    this.loadData();
  }
});
```

---

## 验证接口

### 表单验证函数

```javascript
// validators.js
const validators = {
  // 验证采样信息
  sampleInfo: function(data) {
    const errors = [];
    if (!data.InfoSampleTypeId) errors.push('请选择采样类型');
    if (!data.SampleArea) errors.push('请填写采样部位');
    if (!data.CollectDt) errors.push('请选择采样日期');
    return errors.length > 0 ? errors : null;
  },

  // 验证送检信息
  deliveryInfo: function(data) {
    const errors = [];
    if (!data.MasterName) errors.push('请填写主人姓名');
    if (!data.DoctorName) errors.push('请填写医师姓名');
    return errors.length > 0 ? errors : null;
  },

  // 狂犬检测特殊验证
  rabiesTest: function(orderData, testItems) {
    const hasRabies = testItems.some(item => item.InfoTestTypeId === 1225);
    if (!hasRabies) return null;

    const errors = [];
    if (!orderData.ChipNo) errors.push('狂犬检测必须填写芯片号');
    if (!orderData.Birthday) errors.push('狂犬检测必须填写出生日期');

    const rabiesItem = testItems.find(item => item.InfoTestTypeId === 1225);
    if (!rabiesItem.DetailRemark) errors.push('狂犬检测必须填写快递单号');

    return errors.length > 0 ? errors : null;
  }
};
```

---

**结论**: 前端接口契约已定义完整,包括页面跳转、组件通信、数据存储和错误处理规范。