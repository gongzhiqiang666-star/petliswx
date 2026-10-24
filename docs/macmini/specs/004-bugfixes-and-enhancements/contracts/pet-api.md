# API契约: 宠物模块 (Pet API)

**Feature**: 004-bugfixes-and-enhancements
**Version**: 1.0.0
**Date**: 2025-12-28

---

## 1. 宠物详情

### 1.1 获取宠物详情

**接口**: `GET /api/pets/{petId}`

**功能说明**:
- 根据宠物ID获取完整的宠物信息
- 用于宠物编辑页面预填充（FR-008）
- 用于宠物详情页检测报告展示（FR-009）

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| petId | int | 是 | 宠物ID | 456 |

#### 响应数据

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 456,
    "animalName": "旺财",
    "animalNameEn": "WangCai",
    "infoAnimalId": 1,
    "infoAnimalName": "犬",
    "infoVarietyId": 10,
    "infoVarietyName": "金毛",
    "dictSexId": 1,
    "dictSexName": "公",
    "birthday": "2020-05-15",
    "chipNo": "CN20251228001",
    "ownerName": "张三",
    "ownerNameEn": "Zhang San",
    "infoHospitalId": 789,
    "hospitalName": "北京宠物医院",
    "createtime": 1703721600,
    "updatetime": 1703808000,
    "isActive": 1
  }
}
```

#### 字段说明

**基本信息**:
- `id`: 宠物ID
- `animalName`: 宠物中文名 ✅ **必填**
- `animalNameEn`: 宠物英文名
- `infoAnimalId`: 物种ID
- `infoAnimalName`: 物种名称（关联查询）
- `infoVarietyId`: 品种ID
- `infoVarietyName`: 品种名称（关联查询）
- `dictSexId`: 性别ID
- `dictSexName`: 性别名称（关联查询）
- `birthday`: 生日 ✅ **必填**
- `chipNo`: 芯片号

**畜主信息**:
- `ownerName`: 畜主中文名 ✅ **必填**
- `ownerNameEn`: 畜主英文名

**归属信息**:
- `infoHospitalId`: 医院ID
- `hospitalName`: 医院名称（关联查询）

**时间戳**:
- `createtime`: 创建时间（Unix时间戳）
- `updatetime`: 更新时间（Unix时间戳）
- `isActive`: 是否激活

---

## 2. 宠物列表

### 2.1 获取用户的宠物列表

**接口**: `GET /api/pets/list`

**功能说明**:
- 获取当前用户的所有宠物
- 支持按创建时间倒序排列
- 用于选择宠物页面

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| orderBy | string | 否 | 排序字段（默认createtime） | createtime |
| order | string | 否 | 排序方向（desc/asc） | desc |

#### 响应数据

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 3,
    "list": [
      {
        "id": 458,
        "animalName": "小黑",
        "animalNameEn": "XiaoHei",
        "infoVarietyName": "拉布拉多",
        "dictSexName": "母",
        "birthday": "2021-03-20",
        "chipNo": "CN20251228003",
        "ownerName": "张三",
        "createtime": 1703894400,
        "isActive": 1
      },
      {
        "id": 457,
        "animalName": "小白",
        "animalNameEn": "XiaoBai",
        "infoVarietyName": "萨摩耶",
        "dictSexName": "母",
        "birthday": "2021-02-10",
        "chipNo": "CN20251228002",
        "ownerName": "张三",
        "createtime": 1703808000,
        "isActive": 1
      },
      {
        "id": 456,
        "animalName": "旺财",
        "animalNameEn": "WangCai",
        "infoVarietyName": "金毛",
        "dictSexName": "公",
        "birthday": "2020-05-15",
        "chipNo": "CN20251228001",
        "ownerName": "张三",
        "createtime": 1703721600,
        "isActive": 1
      }
    ]
  }
}
```

**排序规则** (FR-013):
- 默认按 `createtime` 倒序（最新的在前）
- 新增宠物后，新宠物应排在第一位

---

## 3. 宠物检测报告

### 3.1 获取宠物的检测报告列表

**接口**: `GET /api/pets/{petId}/reports`

**功能说明**:
- 获取指定宠物的所有检测报告
- 返回申请时间和报告状态（FR-009）
- 按时间倒序排列（FR-010）

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| petId | int | 是 | 宠物ID | 456 |
| pageNum | int | 否 | 页码（默认1） | 1 |
| pageSize | int | 否 | 每页条数（默认10） | 10 |

#### 响应数据

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "pageNum": 1,
    "pageSize": 10,
    "list": [
      {
        "diagnosisDetailId": 124,
        "genterNO": "ORD20251228002",
        "diagnosisStatus": 3,
        "statusLabel": "已完成",
        "createDt": "2025-12-28 14:00:00",
        "applicationTime": "2025-12-28 14:00:00",
        "pdfHttpUrl": "https://example.com/report/124.pdf",
        "testItems": [
          {
            "testName": "血常规检查"
          },
          {
            "testName": "生化检查"
          }
        ]
      },
      {
        "diagnosisDetailId": 123,
        "genterNO": "ORD20251228001",
        "diagnosisStatus": 2,
        "statusLabel": "等待报告",
        "createDt": "2025-12-27 10:00:00",
        "applicationTime": "2025-12-27 10:00:00",
        "pdfHttpUrl": null,
        "testItems": [
          {
            "testName": "疫苗接种"
          }
        ]
      }
    ]
  }
}
```

#### 字段说明

**报告列表项**:
- `diagnosisDetailId`: 订单/报告ID
- `genterNO`: 订单编号
- `diagnosisStatus`: 报告状态
- `statusLabel`: 状态标签（中文）
- `createDt`: 创建时间 ✅ **显示为申请时间**
- `applicationTime`: 申请时间（同createDt）✅ **FR-009要求**
- `pdfHttpUrl`: 报告PDF地址（未生成时为null）
- `testItems`: 检测项目列表

**排序规则** (FR-010):
- 按 `createDt` 倒序（最新的申请在前）

---

## 4. 宠物编辑/新增

### 4.1 创建宠物

**接口**: `POST /api/pets/create`

**功能说明**:
- 创建新宠物档案
- 返回新创建的宠物ID（用于自动选择）

#### 请求体

```json
{
  "animalName": "小黑",
  "animalNameEn": "XiaoHei",
  "infoAnimalId": 1,
  "infoVarietyId": 12,
  "dictSexId": 2,
  "birthday": "2021-03-20",
  "chipNo": "CN20251228003",
  "ownerName": "张三",
  "ownerNameEn": "Zhang San",
  "infoSampleTypeId": 5,
  "sampleInfo": {
    "sampleDate": "2025-12-28",
    "sampleNote": "空腹采样"
  }
}
```

#### 响应数据

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 458,
    "animalName": "小黑",
    "createtime": 1703894400
  }
}
```

**重要**: 返回的 `id` 用于新增宠物后自动选中（FR-012）

---

### 4.2 更新宠物信息

**接口**: `PUT /api/pets/{petId}`

**功能说明**:
- 更新宠物信息
- 用于宠物编辑页面保存

#### 请求体

```json
{
  "animalName": "旺财",
  "animalNameEn": "WangCai",
  "infoAnimalId": 1,
  "infoVarietyId": 10,
  "dictSexId": 1,
  "birthday": "2020-05-15",
  "chipNo": "CN20251228001",
  "ownerName": "张三",
  "ownerNameEn": "Zhang San"
}
```

#### 响应数据

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 456,
    "updatetime": 1703808000
  }
}
```

---

## 5. 前端调用示例

### 5.1 宠物编辑页面预填充（FR-008）

```javascript
// pages/pet-edit/pet-edit.js

Page({
  data: {
    petId: null,
    formData: {
      animalName: '',
      animalNameEn: '',
      infoAnimalId: null,
      infoVarietyId: null,
      dictSexId: null,
      birthday: '',
      chipNo: '',
      ownerName: '',
      ownerNameEn: ''
    }
  },

  async onLoad(options) {
    const petId = options.id

    if (petId) {
      // 编辑模式：加载宠物详情
      this.setData({ petId })
      await this.loadPetDetail()
    } else {
      // 新增模式：使用默认值
      this.initDefaultValues()
    }
  },

  async loadPetDetail() {
    try {
      wx.showLoading({ title: '加载中...' })

      const pet = await api.pets.getDetail(this.data.petId)

      // 预填充表单 (FR-008)
      this.setData({
        formData: {
          animalName: pet.animalName || '',
          animalNameEn: pet.animalNameEn || '',
          infoAnimalId: pet.infoAnimalId,
          infoVarietyId: pet.infoVarietyId,
          dictSexId: pet.dictSexId,
          birthday: pet.birthday || '',
          chipNo: pet.chipNo || '',
          ownerName: pet.ownerName || '',
          ownerNameEn: pet.ownerNameEn || ''
        }
      })
    } catch (err) {
      console.error('[PetEdit] 加载失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  async onSave() {
    // 验证表单
    if (!this.validateForm()) {
      return
    }

    try {
      wx.showLoading({ title: '保存中...' })

      if (this.data.petId) {
        // 更新宠物
        await api.pets.update(this.data.petId, this.data.formData)
      } else {
        // 创建宠物
        const result = await api.pets.create(this.data.formData)

        // 新增成功后，通知上一页面选中新宠物（FR-012）
        const pages = getCurrentPages()
        const prevPage = pages[pages.length - 2]

        if (prevPage && prevPage.route === 'pages/order-select-pet/order-select-pet') {
          prevPage.setData({
            newPetId: result.id,
            selectedPetId: result.id
          })
        }
      }

      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    } catch (err) {
      console.error('[PetEdit] 保存失败:', err)
      wx.showToast({ title: '保存失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  validateForm() {
    const { animalName, birthday, ownerName } = this.data.formData

    if (!animalName) {
      wx.showToast({ title: '请输入宠物名称', icon: 'none' })
      return false
    }

    if (!birthday) {
      wx.showToast({ title: '请选择生日', icon: 'none' })
      return false
    }

    if (!ownerName) {
      wx.showToast({ title: '请输入畜主姓名', icon: 'none' })
      return false
    }

    return true
  }
})
```

### 5.2 宠物检测报告列表（FR-009, FR-010）

```javascript
// pages/pet-detail/pet-detail.js

Page({
  data: {
    petId: null,
    pet: null,
    reports: [],
    loading: false
  },

  async onLoad(options) {
    this.setData({ petId: options.id })
    await this.loadPetDetail()
    await this.loadReports()
  },

  async loadReports() {
    this.setData({ loading: true })

    try {
      const result = await api.pets.getReports(this.data.petId, {
        pageNum: 1,
        pageSize: 100
      })

      // 按申请时间倒序排列（FR-010）
      const sortedReports = result.list.sort((a, b) => {
        return new Date(b.createDt) - new Date(a.createDt)
      })

      this.setData({
        reports: sortedReports
      })
    } catch (err) {
      console.error('[PetDetail] 加载报告失败:', err)
    } finally {
      this.setData({ loading: false })
    }
  }
})
```

**WXML显示** (FR-009):
```xml
<!-- 显示申请时间和报告状态 -->
<view class="report-item" wx:for="{{reports}}" wx:key="diagnosisDetailId">
  <view class="report-time">申请时间：{{item.applicationTime}}</view>
  <view class="report-status">状态：{{item.statusLabel}}</view>
  <view class="report-items">
    <text wx:for="{{item.testItems}}" wx:for-item="test" wx:key="*this">
      {{test testName}}
    </text>
  </view>
</view>
```

### 5.3 新增宠物后自动选择（FR-012, FR-013）

```javascript
// pages/order-select-pet/order-select-pet.js

Page({
  data: {
    pets: [],
    selectedPetId: null
  },

  onLoad() {
    this.loadPets()
  },

  onShow() {
    // 从其他页面返回时刷新列表
    this.loadPets()

    // 检查是否有新创建的宠物ID（FR-012）
    const pages = getCurrentPages()
    const currPage = pages[pages.length - 1]

    if (currPage.data && currPage.data.newPetId) {
      this.setData({
        selectedPetId: currPage.data.newPetId
      })

      // 清除标记，避免重复选中
      delete currPage.data.newPetId
    }
  },

  async loadPets() {
    try {
      const result = await api.pets.getList({
        orderBy: 'createtime',
        order: 'desc'
      })

      let selectedPetId = this.data.selectedPetId

      // 如果没有选中宠物，默认选中最新的一条（FR-013）
      if (!selectedPetId && result.list.length > 0) {
        selectedPetId = result.list[0].id
      }

      this.setData({
        pets: result.list,
        selectedPetId
      })
    } catch (err) {
      console.error('[PetSelect] 加载失败:', err)
    }
  },

  onSelectPet(e) {
    const petId = e.currentTarget.dataset.id
    this.setData({ selectedPetId: petId })
  },

  onNext() {
    if (!this.data.selectedPetId) {
      wx.showToast({ title: '请选择宠物', icon: 'none' })
      return
    }

    wx.navigateTo({
      url: `/pages/order-add-pet/order-add-pet?petId=${this.data.selectedPetId}`
    })
  },

  onAddPet() {
    wx.navigateTo({
      url: '/pages/order-add-pet/order-add-pet?mode=create'
    })
  }
})
```

---

## 6. 数据验证规则

### 6.1 宠物详情验证（FR-008）

**必填字段必须预填充**:
- ✅ `animalName`: 宠物名称
- ✅ `birthday`: 生日
- ✅ `ownerName`: 畜主姓名
- ✅ `infoAnimalId`: 物种ID
- ✅ `infoVarietyId`: 品种ID
- ✅ `dictSexId`: 性别ID

**可选字段**:
- `animalNameEn`: 英文名（可为空字符串）
- `chipNo`: 芯片号（可为null）

### 6.2 检测报告验证（FR-009）

**必须显示的字段**:
- ✅ `applicationTime`: 申请时间（格式: yyyy-MM-dd HH:mm:ss）
- ✅ `statusLabel`: 报告状态（中文标签）

**可选字段**:
- `pdfHttpUrl`: 报告地址（未生成时为null，显示"报告生成中"）

---

## 7. 错误处理

### 7.1 常见错误场景

| 场景 | 错误码 | 错误信息 | 前端处理 |
|------|--------|---------|---------|
| 宠物不存在 | 404 | 宠物不存在 | 提示并返回列表页 |
| 芯片号重复 | 400 | 芯片号已存在 | 提示修改芯片号 |
| 无权限访问 | 403 | 无权限访问该宠物 | 提示并返回 |
| 网络超时 | 504 | 请求超时 | 提示重试 |

---

**文档版本**: 1.0.0
**最后更新**: 2025-12-28
