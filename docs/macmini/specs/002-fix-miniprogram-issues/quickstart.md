# 快速开始指南: 修复微信小程序显示和流程问题

**功能**: 001-fix-miniprogram-issues
**预计工作量**: 2-3天
**难度等级**: 中等

---

## 前置条件

### 开发环境

- ✅ 微信开发者工具(最新稳定版)
- ✅ Node.js 16+ (如需npm工具)
- ✅ 后端API服务已启动(localhost:8080)
- ✅ MySQL数据库已配置并初始化

### 权限要求

- 微信小程序开发权限
- 后端API访问权限(JWT token)
- 代码仓库读写权限

---

## 快速启动步骤

### 1. 克隆并切换分支

```bash
cd /path/to/PETLIS
git checkout 001-fix-miniprogram-issues
```

### 2. 验证后端服务

```bash
# 检查后端是否运行
curl http://localhost:8080/api/dictionary/animals

# 预期响应
{
  "code": 200,
  "message": "操作成功",
  "data": [...]
}
```

### 3. 打开微信开发者工具

```bash
# 打开项目
open -a "微信开发者工具" /path/to/PETLIS/miniprogram
```

### 4. 编译并预览

1. 点击"编译"按钮
2. 查看控制台是否有错误
3. 确认能正常登录(使用测试账号)

---

## 开发任务清单

### Phase 1: 修复宠物管理显示 (预计2小时)

- [ ] **任务1.1**: 修改`pages/pets/pets.wxml`,使用后端字段名
  ```xml
  <!-- 修改前 -->
  <text>{{item.animalName}}</text>

  <!-- 修改后 -->
  <text>{{item.AnimalName}}</text>
  ```

- [ ] **任务1.2**: 修改`pages/pets/pets.js`,更新字段引用
  ```javascript
  // 搜索时使用后端字段名
  if (pet.AnimalName && pet.AnimalName.includes(query)) {
    return true;
  }
  ```

- [ ] **任务1.3**: 修改`pages/pet-detail/pet-detail.*`,显示宠物详情
- [ ] **任务1.4**: 修改`pages/pet-add/pet-add.*`,编辑时预填充数据

**验收**:
- ✅ 宠物列表正确显示所有字段
- ✅ 搜索功能正常工作
- ✅ 编辑功能正确预填充数据

---

### Phase 2: 修复订单列表显示 (预计2小时)

- [ ] **任务2.1**: 修改`pages/orders/orders.wxml`,使用AnimalName字段
  ```xml
  <text>{{item.AnimalName}}</text>
  ```

- [ ] **任务2.2**: 修改`pages/orders/orders.js`,添加状态映射
  ```javascript
  getStatusLabel(status) {
    const labels = {
      '-2': '草稿',
      '0': '待审核',
      '2': '等待报告',
      '3': '已完成'
    };
    return labels[String(status)] || '未知';
  }
  ```

- [ ] **任务2.3**: 修改`pages/order-detail/order-detail.*`,显示完整订单信息

**验收**:
- ✅ 订单列表显示正确的宠物名称
- ✅ 状态标签显示可读文本
- ✅ 订单详情显示所有检测项

---

### Phase 3: 实现预约检测流程 (预计8小时)

#### 3.1 创建预约信息填写页面

- [ ] **任务3.1.1**: 新建`pages/order-add-pet/`目录
- [ ] **任务3.1.2**: 创建`order-add-pet.wxml`
  ```xml
  <!-- 采样信息表单 -->
  <view class="form-section">
    <view class="form-title">采样信息</view>
    <picker mode="selector" range="{{sampleTypes}}" bindchange="onSampleTypeChange">
      <view class="picker">采样类型: {{selectedSampleType}}</view>
    </picker>
    <input bindinput="onSampleAreaInput" placeholder="采样部位" value="{{sampleInfo.SampleArea}}"/>
    <picker mode="date" bindchange="onCollectDateChange">
      <view class="picker">采样日期: {{sampleInfo.CollectDt}}</view>
    </picker>
  </view>

  <!-- 送检信息表单 -->
  <view class="form-section">
    <view class="form-title">送检信息</view>
    <input bindinput="onMasterNameInput" placeholder="主人姓名" value="{{deliveryInfo.MasterName}}"/>
    <input bindinput="onDoctorNameInput" placeholder="医师姓名" value="{{deliveryInfo.DoctorName}}"/>
  </view>

  <!-- 其他信息 -->
  <view class="form-section">
    <view class="form-title">其他信息</view>
    <textarea bindinput="onRemarkInput" placeholder="备注" value="{{otherInfo.Remark}}"/>
  </view>

  <button bindtap="onSelectTestItems">选择检测项目</button>
  ```

- [ ] **任务3.1.3**: 创建`order-add-pet.js`
  ```javascript
  Page({
    data: {
      petId: null,
      petInfo: null,
      sampleInfo: {
        InfoSampleTypeId: null,
        SampleArea: '',
        CollectDt: '',
        CityId: null  // 自动填充
      },
      deliveryInfo: {
        MasterName: '',
        MasterNameEn: '',
        DoctorName: ''
      },
      otherInfo: {
        Remark: ''
      },
      sampleTypes: []
    },

    onLoad(options) {
      const petId = options.petId;
      this.setData({ petId });
      this.loadPetDetail(petId);
      this.loadSampleTypes();
      this.loadHospitalInfo();
    },

    async loadPetDetail(petId) {
      const pet = await api.pets.getDetail(petId);
      this.setData({
        petInfo: pet,
        deliveryInfo: {
          MasterName: pet.MasterName,
          MasterNameEn: pet.MasterNameEn,
          DoctorName: ''
        }
      });
    },

    async loadHospitalInfo() {
      const hospital = await api.dictionary.getHospitals();
      this.setData({
        'sampleInfo.CityId': hospital[0].CityId
      });
    },

    onSelectTestItems() {
      // 验证表单
      const errors = this.validateForm();
      if (errors) {
        wx.showToast({ title: errors[0], icon: 'none' });
        return;
      }

      // 保存到storage
      wx.setStorageSync('order_form', {
        petId: this.data.petId,
        petInfo: this.data.petInfo,
        sampleInfo: this.data.sampleInfo,
        deliveryInfo: this.data.deliveryInfo,
        otherInfo: this.data.otherInfo
      });

      wx.navigateTo({
        url: '/pages/order-test-items/order-test-items'
      });
    }
  });
  ```

#### 3.2 创建检测项目选择页面

- [ ] **任务3.2.1**: 新建`pages/order-test-items/`目录
- [ ] **任务3.2.2**: 创建`order-test-items.wxml`
  ```xml
  <!-- 分类标签 -->
  <scroll-view scroll-x class="category-tabs">
    <view class="tab-item {{currentCategory === 0 ? 'active' : ''}}"
          bindtap="onCategoryChange" data-id="0">
      全部
    </view>
    <view class="tab-item {{currentCategory === item.CategoryId ? 'active' : ''}}"
          wx:for="{{categories}}" wx:key="CategoryId"
          bindtap="onCategoryChange" data-id="{{item.CategoryId}}">
      {{item.CategoryName}}
    </view>
  </scroll-view>

  <!-- 搜索框 -->
  <view class="search-bar">
    <input bindinput="onSearchInput" placeholder="搜索检测项目"/>
  </view>

  <!-- 项目列表 -->
  <scroll-view scroll-y class="items-list">
    <view class="item-card {{item.selected ? 'selected' : ''}}"
          wx:for="{{filteredItems}}" wx:key="InfoTestTypeId"
          bindtap="onToggleItem" data-id="{{item.InfoTestTypeId}}">
      <view class="item-name">{{item.TestTypeName}}</view>
      <view class="item-price">¥{{item.TestPrice}}</view>
      <view class="check-icon" wx:if="{{item.selected}}">✓</view>
    </view>
  </scroll-view>

  <!-- 底部统计 -->
  <view class="footer">
    <view class="summary">已选{{selectedCount}}项,共¥{{totalPrice}}</view>
    <button bindtap="onNext">下一步预览</button>
  </view>
  ```

- [ ] **任务3.2.3**: 创建`order-test-items.js`
  ```javascript
  Page({
    data: {
      allItems: [],
      categories: [],
      currentCategory: 0,
      searchQuery: '',
      selectedIds: [],
      filteredItems: [],
      selectedCount: 0,
      totalPrice: 0
    },

    onLoad() {
      this.loadTestItems();
      this.loadCategories();
    },

    async loadTestItems() {
      const items = await api.dictionary.getTestTypes();
      this.setData({ allItems: items });
      this.filterItems();
    },

    onToggleItem(e) {
      const itemId = e.currentTarget.dataset.id;
      let selectedIds = this.data.selectedIds;

      if (selectedIds.includes(itemId)) {
        selectedIds = selectedIds.filter(id => id !== itemId);
      } else {
        selectedIds.push(itemId);
      }

      this.setData({ selectedIds });
      this.updateSummary();
    },

    onNext() {
      if (this.data.selectedIds.length === 0) {
        wx.showToast({ title: '请至少选择一个检测项目', icon: 'none' });
        return;
      }

      const formData = wx.getStorageSync('order_form') || {};
      formData.selectedTestItems = this.data.allItems.filter(item =>
        this.data.selectedIds.includes(item.InfoTestTypeId)
      );
      wx.setStorageSync('order_form', formData);

      wx.navigateTo({
        url: '/pages/order-preview/order-preview'
      });
    }
  });
  ```

#### 3.3 创建预约预览页面

- [ ] **任务3.3.1**: 新建`pages/order-preview/`目录
- [ ] **任务3.3.2**: 创建`order-preview.wxml`
  ```xml
  <view class="preview-container">
    <!-- 宠物信息 -->
    <view class="section">
      <view class="section-title">宠物信息</view>
      <view class="info-row">中文名: {{formData.petInfo.AnimalName}}</view>
      <view class="info-row">英文名: {{formData.petInfo.AnimalNameEn}}</view>
      <view class="info-row">品种: {{formData.petInfo.VarietyName}}</view>
    </view>

    <!-- 采样信息 -->
    <view class="section">
      <view class="section-title">采样信息</view>
      <view class="info-row">采样类型: {{formData.sampleInfo.SampleTypeName}}</view>
      <view class="info-row">采样部位: {{formData.sampleInfo.SampleArea}}</view>
      <view class="info-row">采样日期: {{formData.sampleInfo.CollectDt}}</view>
    </view>

    <!-- 检测项目 -->
    <view class="section">
      <view class="section-title">检测项目</view>
      <view class="test-item" wx:for="{{formData.selectedTestItems}}" wx:key="InfoTestTypeId">
        <view class="item-name">{{item.TestTypeName}}</view>
        <view class="item-price">¥{{item.TestPrice}}</view>
      </view>
      <view class="total-price">总计: ¥{{totalPrice}}</view>
    </view>

    <button bindtap="onSubmit">提交预约</button>
    <button bindtap="onBack">返回修改</button>
  </view>
  ```

- [ ] **任务3.3.3**: 创建`order-preview.js`
  ```javascript
  Page({
    data: {
      formData: {},
      totalPrice: 0
    },

    onLoad() {
      const formData = wx.getStorageSync('order_form');
      const totalPrice = formData.selectedTestItems.reduce((sum, item) =>
        sum + item.TestPrice, 0
      );

      this.setData({ formData, totalPrice });
    },

    async onSubmit() {
      // 狂犬检测验证
      const hasRabies = this.data.formData.selectedTestItems.some(
        item => item.InfoTestTypeId === 1225
      );

      if (hasRabies) {
        if (!this.data.formData.petInfo.ChipNo) {
          wx.showToast({ title: '狂犬检测必须填写芯片号', icon: 'none' });
          return;
        }
      }

      try {
        // 构造订单数据
        const orderData = {
          ...this.data.formData.petInfo,
          ...this.data.formData.sampleInfo,
          ...this.data.formData.deliveryInfo,
          Remark: this.data.formData.otherInfo.Remark,
          SampleStatus: -2,  // 草稿状态
          diagnosisDetails: this.data.formData.selectedTestItems.map(item => ({
            InfoTestTypeId: item.InfoTestTypeId,
            TestPrice: item.TestPrice
          }))
        };

        await api.orders.create(orderData);

        wx.showToast({ title: '预约创建成功', icon: 'success' });

        // 清理storage
        wx.removeStorageSync('order_form');
        wx.removeStorageSync('selected_pet');

        setTimeout(() => {
          wx.switchTab({ url: '/pages/orders/orders' });
        }, 1500);

      } catch (error) {
        wx.showToast({ title: '提交失败: ' + error.message, icon: 'none' });
      }
    },

    onBack() {
      wx.navigateBack();
    }
  });
  ```

#### 3.4 修改选择宠物页面

- [ ] **任务3.4.1**: 修改`pages/order-select-pet/order-select-pet.wxml`
  ```xml
  <!-- 移除快速预约和自定义预约按钮 -->
  <button class="btn-primary" bindtap="onConfirm" data-id="{{item.id}}">
    继续预约
  </button>
  ```

- [ ] **任务3.4.2**: 修改`pages/order-select-pet/order-select-pet.js`
  ```javascript
  onConfirm(e) {
    const petId = e.currentTarget.dataset.id;
    if (!petId) {
      wx.showToast({ title: '请先选择宠物', icon: 'none' });
      return;
    }

    wx.navigateTo({
      url: `/pages/order-add-pet/order-add-pet?petId=${petId}`
    });
  }
  ```

**验收**:
- ✅ 选择宠物后能进入预约信息填写页面
- ✅ 能填写采样信息、送检信息、其他信息
- ✅ 能选择多个检测项目,按分类筛选
- ✅ 预览页面显示所有填写信息
- ✅ 提交成功后跳转到订单列表,显示"草稿"状态
- ✅ 狂犬检测验证芯片号和出生日期

---

### Phase 4: 修复"我的"页面 (预计2小时)

- [ ] **任务4.1**: 修改`pages/mine/mine.wxml`,移除所有菜单项
  ```xml
  <!-- 保留用户信息卡片 -->
  <view class="user-card">
    <view class="user-avatar">
      <image src="{{userInfo.avatarUrl || '/assets/default-avatar.png'}}"/>
    </view>
    <view class="user-info">
      <view class="user-name">{{userInfo.nickname}}</view>
      <view class="user-mobile">{{userInfo.mobile}}</view>
    </view>
  </view>

  <!-- 保留医院信息卡片 -->
  <view class="hospital-card">
    <view class="hospital-name">{{hospitalInfo.HospitalName}}</view>
    <view class="hospital-address">{{hospitalInfo.Address}}</view>
    <view class="hospital-phone">{{hospitalInfo.Phone}}</view>
  </view>

  <!-- 移除所有菜单项 -->

  <!-- 保留退出登录按钮 -->
  <button bindtap="onLogout">退出登录</button>
  ```

- [ ] **任务4.2**: 修改`pages/mine/mine.js`,加载真实数据
  ```javascript
  Page({
    data: {
      userInfo: {},
      hospitalInfo: {}
    },

    onLoad() {
      this.loadUserInfo();
      this.loadHospitalInfo();
    },

    async loadUserInfo() {
      try {
        const user = await api.user.getProfile();
        this.setData({ userInfo: user });
      } catch (error) {
        console.error('加载用户信息失败', error);
      }
    },

    async loadHospitalInfo() {
      try {
        const hospitals = await api.dictionary.getHospitals();
        const hospitalId = app.globalData.hospitalId;
        const hospital = hospitals.find(h => h.InfoHospitalId === hospitalId);
        this.setData({ hospitalInfo: hospital });
      } catch (error) {
        console.error('加载医院信息失败', error);
      }
    }
  });
  ```

**验收**:
- ✅ 显示真实用户信息和医院信息
- ✅ 使用后端返回的实际字段名
- ✅ 无菜单项,只有信息卡片和退出登录按钮

---

## 测试清单

### 功能测试

- [ ] 宠物列表显示所有字段正确
- [ ] 宠物搜索功能正常
- [ ] 订单列表显示宠物名称正确
- [ ] 订单状态标签显示可读文本
- [ ] 完整预约流程可正常走通
- [ ] 狂犬检测验证生效
- [ ] "我的"页面显示真实数据
- [ ] API错误提示友好

### 边界测试

- [ ] 宠物列表为空时显示空状态
- [ ] 选择宠物后未填写信息时不能进入下一步
- [ ] 未选择检测项目时提示错误
- [ ] API失败时显示错误提示和重试按钮
- [ ] 字段为null时不显示"null"

### 兼容性测试

- [ ] iOS微信客户端正常
- [ ] Android微信客户端正常
- [ ] 不同屏幕尺寸适配正常

---

## 常见问题

### Q1: 后端API字段名不确定怎么办?

**A**: 查看后端源码`petlis-backend/src/main/java/com/petlis/entity/`目录下的实体类定义,或使用Postman测试API返回的实际字段名。

### Q2: 如何调试页面跳转?

**A**:
```javascript
// 在页面跳转前后添加console.log
console.log('跳转前数据', this.data);
wx.navigateTo({ url: '...' });

// 在目标页面的onLoad中打印
onLoad(options) {
  console.log('接收参数', options);
}
```

### Q3: 如何模拟API失败?

**A**: 临时修改`utils/request.js`,在请求前抛出异常:
```javascript
// 模拟网络错误
if (url.includes('/pets')) {
  return Promise.reject(new Error('模拟网络错误'));
}
```

### Q4: 本地存储数据看不到怎么办?

**A**: 在微信开发者工具的"调试器" → "Storage"标签中查看本地存储数据。

---

## 提交检查清单

- [ ] 所有任务已完成
- [ ] 测试清单全部通过
- [ ] 代码无console.error或warning
- [ ] WXML和WXSS无语法错误
- [ ] 提交前已执行`git status`检查修改文件
- [ ] 提交信息清晰描述修改内容

---

## 下一步

完成本功能后,可以执行:
```bash
/speckit.tasks
```

生成详细的任务分解清单,用于实施跟踪。

---

**开发者**: [您的名字]
**审核者**: [待定]
**预计完成日期**: [待定]