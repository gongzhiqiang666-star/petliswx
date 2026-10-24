# Data Model: PETLIS UX体验优化

**Feature**: 003-ux-improvement
**Date**: 2025-12-28
**Phase**: Phase 1 - Data Model Design

## 概述

本文档定义了 PETLIS UX 优化项目涉及的数据实体、字段、关系和验证规则。由于本项目主要是前端 UX 改进,**不会修改数据库表结构**,但需要明确前端使用的数据结构和API返回的数据格式。

---

## 核心实体

### 1. 宠物 (Pet)

**用途**: 宠物档案信息,用于列表展示、搜索、选择

**数据来源**: `lin_lb_listpet` 表 (通过 API `GET /api/pets` 获取)

**字段定义**:

| 字段名 | 类型 | 说明 | 验证规则 | 显示格式 |
|--------|------|------|----------|----------|
| `id` | Number | 宠物ID (主键) | 必填, >0 | - |
| `animalName` | String | 宠物中文名 | 必填, 1-50字符 | 显示 |
| `animalName_en` | String | 宠物英文名 | 必填, 1-100字符 | 显示 |
| `infoAnimalId` | Number | 物种ID | 必填, >0 | 显示物种名称 |
| `infoVarietyId` | Number | 品种ID | 可选, >0 | 显示品种名称 |
| `dictSexId` | Number | 性别ID | 必填, ∈{1,2,6,8} | 显示性别名称 |
| `birthday` | String | 生日 (ISO 8601) | 可选, YYYY-MM-DD | 计算年龄 |
| `age` | String | 年龄 (计算字段) | 前端计算 | "X岁Y个月" 或 "X个月" |
| `chipNO` | String | 芯片号 | 可选, 0-50字符 | 显示 |
| `masterName` | String | 畜主中文名 | 必填, 1-50字符 | 显示 |
| `masterName_en` | String | 畜主英文名 | 必填, 1-100字符 | 显示 |
| `masterPhone` | String | 畜主电话 | 可选, 手机号格式 | 显示(脱敏) |
| `isActive` | Number | 是否启用 | 必填, ∈{0,1} | 仅显示=1的 |
| `createTime` | String | 创建时间 | ISO 8601 | YYYY-MM-DD HH:mm |
| `updateTime` | String | 更新时间 | ISO 8601 | YYYY-MM-DD HH:mm |

**关联关系**:
- **物种 (Species)**: `infoAnimalId` → `lin_lb_infoanimal.DictAnimalId`
- **品种 (Variety)**: `infoVarietyId` → `lin_lb_infovariety.DictVarietyId`
- **性别 (Sex)**: `dictSexId` → `lin_lb_dictsex.DictSexId`

**前端使用场景**:
1. **宠物列表页** (`pages/pets/pets`): 显示卡片,包含搜索、分页
2. **宠物选择页** (`pages/order-select-pet/order-select-pet`): 选择宠物,显示年龄
3. **宠物详情页** (`pages/pet-detail/pet-detail`): 显示完整信息
4. **预约检测页** (`pages/order-add-pet/order-add-pet`): 自动填充畜主信息

**年龄计算规则**:
```javascript
/**
 * 计算宠物年龄
 * @param {String} birthday - 格式: "YYYY-MM-DD"
 * @returns {String} "X岁Y个月" | "X个月" | "未知"
 */
function calculateAge(birthday) {
  if (!birthday) return "未知";

  const birth = new Date(birthday);
  const today = new Date();

  let years = today.getFullYear() - birth.getFullYear();
  let months = today.getMonth() - birth.getMonth();

  if (months < 0) {
    years--;
    months += 12;
  }

  if (years < 1) {
    return `${months === 0 ? 1 : months}个月`;
  } else {
    return `${years}岁${months}个月`;
  }
}
```

---

### 2. 畜主 (Owner)

**用途**: 畜主信息,用于自动填充到预约表单

**数据来源**: 嵌入在 Pet 对象中 (不独立提供 API)

**字段定义**:

| 字段名 | 类型 | 说明 | 验证规则 | 显示格式 |
|--------|------|------|----------|----------|
| `masterName` | String | 畜主中文名 | 必填, 1-50字符 | 显示 |
| `masterName_en` | String | 畜主英文名 | 必填, 1-100字符 | 显示 |
| `masterPhone` | String | 联系电话 | 可选, 手机号格式 | 显示(脱敏) |
| `masterAddress` | String | 地址 | 可选, 0-200字符 | 显示 |

**自动填充规则**:
1. 用户选择宠物后,从宠物对象中提取畜主信息
2. 自动填充到预约表单的畜主字段
3. 用户可以修改自动填充的信息
4. 如果API失败或数据不完整,显示友好提示: "畜主信息加载失败,请手动填写"

---

### 3. 物种 (Species)

**用途**: 动物物种字典,用于物种选择独立页面

**数据来源**: `lin_lb_infoanimal` 表 (通过 API `GET /api/dictionary/animals` 获取)

**字段定义**:

| 字段名 | 类型 | 说明 | 验证规则 | 显示格式 |
|--------|------|------|----------|----------|
| `dictAnimalId` | Number | 物种ID (主键) | 必填, >0 | - |
| `animalName` | String | 物种中文名 | 必填, 1-50字符 | 显示 |
| `animalName_en` | String | 物种英文名 | 必填, 1-100字符 | 显示 |
| `isActive` | Number | 是否启用 | 必填, ∈{0,1} | 仅显示=1的 |

**前端使用场景**:
1. **物种选择页面** (`components/species-selector/`): 独立页面,支持搜索、分页
2. **宠物建档页** (`pages/pet-add/pet-add`): 点击物种选择框,跳转到选择页面

**物种选择页面数据结构**:
```javascript
{
  "list": [
    {
      "id": 1,
      "name": "猫",
      "nameEn": "Cat",
      "selected": false
    },
    {
      "id": 2,
      "name": "狗",
      "nameEn": "Dog",
      "selected": false
    }
  ],
  "total": 100,
  "pageNum": 1,
  "pageSize": 20,
  "searchKeyword": ""
}
```

---

### 4. 品种 (Variety)

**用途**: 品种字典,按物种过滤

**数据来源**: `lin_lb_infovariety` 表 (通过 API `GET /api/dictionary/varieties?animalId={id}` 获取)

**字段定义**:

| 字段名 | 类型 | 说明 | 验证规则 | 显示格式 |
|--------|------|------|----------|----------|
| `dictVarietyId` | Number | 品种ID (主键) | 必填, >0 | - |
| `varietyName` | String | 品种中文名 | 必填, 1-50字符 | 显示 |
| `varietyName_en` | String | 品种英文名 | 必填, 1-100字符 | 显示 |
| `infoAnimalId` | Number | 所属物种ID | 必填, >0 | - |
| `isActive` | Number | 是否启用 | 必填, ∈{0,1} | 仅显示=1的 |

**前端使用场景**:
- 物种选择后,品种选择框自动过滤显示该物种的品种

---

### 5. 性别 (Sex)

**用途**: 性别字典,用于宠物建档和编辑

**数据来源**: `lin_lb_dictsex` 表 (通过 API `GET /api/dictionary/sexes` 获取)

**字段定义**:

| 字段名 | 类型 | 说明 | 验证规则 | 显示格式 |
|--------|------|------|----------|----------|
| `dictSexId` | Number | 性别ID (主键) | 必填, >0 | - |
| `sexName` | String | 性别中文名 | 必填, 1-20字符 | 显示 |
| `sexName_en` | String | 性别英文名 | 必填, 1-50字符 | 显示 |
| `isActive` | Number | 是否启用 | 必填, ∈{0,1} | 仅显示=1的 |

**启用后的性别选项** (需要数据库更新):
```sql
UPDATE lin_lb_dictsex SET IsActive = 1 WHERE DictSexId IN (1, 2, 6, 8);
```

**返回数据示例**:
```json
[
  {"dictSexId": 6, "sexName": "不详", "sexName_en": "Unknown"},
  {"dictSexId": 1, "sexName": "雄性", "sexName_en": "Male"},
  {"dictSexId": 2, "sexName": "雌性", "sexName_en": "Female"},
  {"dictSexId": 8, "sexName": "已绝育", "sexName_en": "Neutered"}
]
```

**前端使用场景**:
- 宠物建档页 (`pages/pet-add/pet-add`): 使用 picker 组件选择性别
- 默认选择: "不详" (dictSexId=6)

---

### 6. 检测报告 (Report)

**用途**: 检测报告信息,用于报告列表和详情展示

**数据来源**: `lin_lb_listdiagnosisproject` 和 `lin_lb_diagnosisdetail` 表 (通过 API `GET /api/reports` 获取)

**字段定义**:

| 字段名 | 类型 | 说明 | 验证规则 | 显示格式 |
|--------|------|------|----------|----------|
| `id` | Number | 报告ID (主键) | 必填, >0 | - |
| `petId` | Number | 关联宠物ID | 必填, >0 | - |
| `petName` | String | 宠物姓名 | 必填 | 显示 |
| `petBreed` | String | 宠物品种 | 可选 | 显示 |
| `petAge` | String | 宠物年龄 | 计算字段 | "X岁Y个月" |
| `petSex` | String | 宠物性别 | 可选 | 显示性别名称 |
| `testItems` | Array | 检测项目列表 | 必填, 非空 | 见下方 |
| `status` | Number | 报告状态 | 必填 | 见状态枚举 |
| `statusLabel` | String | 状态标签 | 必填 | 显示 |
| `applyTime` | String | 申请时间 | ISO 8601 | YYYY-MM-DD HH:mm |
| `publishTime` | String | 发布时间 | ISO 8601 | YYYY-MM-DD HH:mm |
| `pdfUrl` | String | PDF下载链接 | 可选 | 用于下载 |
| `remark` | String | 备注信息 | 可选, 0-500字符 | 显示 |

**检测项目 (TestItem) 子结构**:
```javascript
{
  "itemId": 123,
  "testTypeName": "血常规检查",
  "testTypeCode": "BLOOD_ROUTINE",
  "price": 150.00,
  "status": 3,  // 3=已完成
  "result": "白细胞计数: 7.2×10^9/L",
  "pdfUrl": "https://..."
}
```

**报告状态枚举**:
| 值 | 标签 | 说明 |
|----|------|------|
| -2 | 草稿 | 申请单未提交 |
| 0 | 待审核 | 等待审核 |
| 1 | 审核不通过 | 审核失败 |
| 2 | 检测中 | 正在检测 |
| 3 | 已完成 | 检测完成,报告已发布 |
| 4 | 已取消 | 订单取消 |

**前端使用场景**:
1. **报告列表页** (`pages/report-list/report-list`): 显示卡片,支持排序、搜索、分页
2. **报告详情弹窗**: 显示宠物信息、检测项目、检测结果概要

**报告列表数据结构** (带分页):
```javascript
{
  "list": [
    {
      "id": 456,
      "petName": "小白",
      "petBreed": "英短猫",
      "testItemCount": 3,
      "status": 3,
      "statusLabel": "已完成",
      "applyTime": "2025-12-20 10:30:00",
      "publishTime": "2025-12-22 15:45:00"
    }
  ],
  "total": 200,
  "pageNum": 1,
  "pageSize": 20
}
```

**排序选项**:
- `applyTime_desc`: 申请时间倒序 (默认)
- `applyTime_asc`: 申请时间正序
- `publishTime_desc`: 发布时间倒序
- `publishTime_asc`: 发布时间正序

---

## 数据验证规则

### 前端表单验证

#### 宠物建档表单 (`pages/pet-add/pet-add`)

**必填字段** (带 `*` 标记):
1. 宠物姓名 (`animalName`) - 非空,1-50字符
2. 宠物英文名 (`animalName_en`) - 非空,1-100字符
3. 物种 (`infoAnimalId`) - 必须选择
4. 品种 (`infoVarietyId`) - 必须选择
5. 性别 (`dictSexId`) - 必须选择
6. 生日 (`birthday`) - 可选,但如果选择狂犬检测必填
7. 畜主中文名 (`masterName`) - 非空,1-50字符
8. 畜主英文名 (`masterName_en`) - 非空,1-100字符

**可选字段**:
- 芯片号 (`chipNO`) - 0-50字符
- 畜主电话 (`masterPhone`) - 手机号格式验证
- 畜主地址 (`masterAddress`) - 0-200字符
- 备注 (`remark`) - 0-500字符

**验证时机**:
1. **实时验证**: 用户输入时立即检查格式
2. **提交前验证**: 检查所有必填项
3. **错误提示**: 在字段下方显示红色错误信息

---

## API 数据格式规范

### 统一响应格式

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // 实际数据
  }
}
```

**失败响应**:
```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": null
}
```

### 分页响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 5
  }
}
```

### 日期时间格式

- **统一使用 ISO 8601 格式**: `YYYY-MM-DDTHH:mm:ss.sssZ`
- **前端显示格式**: `YYYY-MM-DD HH:mm`
- **数据库存储**: `DATETIME` 或 `DATE`

---

## 前端状态管理

### 页面级状态 (Page State)

由于小程序不使用 Redux/Vuex,使用页面级状态管理:

#### 宠物选择页 (`pages/order-select-pet/order-select-pet`)

```javascript
Page({
  data: {
    pets: [],              // 宠物列表
    filteredPets: [],      // 过滤后的列表
    selectedPetId: null,   // 选中的宠物ID
    searchKeyword: "",     // 搜索关键词
    pageNum: 1,            // 当前页
    pageSize: 20,          // 每页数量
    total: 0,              // 总数
    loading: false         // 加载状态
  }
})
```

#### 报告列表页 (`pages/report-list/report-list`)

```javascript
Page({
  data: {
    reports: [],           // 报告列表
    filteredReports: [],   // 过滤后的列表
    sortBy: "applyTime",   // 排序字段
    sortOrder: "desc",     // 排序方向
    searchKeyword: "",     // 搜索关键词
    pageNum: 1,
    pageSize: 20,
    total: 0,
    loading: false
  }
})
```

### 本地缓存 (Storage)

**存储用户偏好设置**:
```javascript
// 报告排序偏好
wx.setStorageSync('reportSortPreference', {
  sortBy: 'publishTime',
  sortOrder: 'desc'
})

// 读取偏好
const sortPref = wx.getStorageSync('reportSortPreference') || {
  sortBy: 'applyTime',
  sortOrder: 'desc'
}
```

---

## 数据流程图

### 宠物选择 + 自动填充流程

```
用户输入关键词
    ↓
前端防抖 (300-500ms)
    ↓
调用 API: GET /api/pets/search?name={keyword}&masterName={keyword}&pageNum=1&pageSize=20
    ↓
后端搜索返回分页数据
    ↓
前端计算年龄 (前端实时计算)
    ↓
渲染宠物列表 (显示姓名、品种、主人、年龄)
    ↓
用户点击选择宠物
    ↓
从宠物对象提取畜主信息
    ↓
自动填充到预约表单
    ↓
用户可修改畜主信息
```

### 报告列表排序 + 搜索流程

```
用户选择排序方式
    ↓
调用 API: GET /api/reports?sortBy={sortBy}&sortOrder={sortOrder}&petName={keyword}&pageNum=1&pageSize=20
    ↓
后端排序并返回分页数据
    ↓
前端渲染报告列表
    ↓
用户输入搜索关键词
    ↓
前端防抖 (300-500ms)
    ↓
调用 API: GET /api/reports?...&petName={keyword}
    ↓
前端过滤并更新列表
```

---

## 数据一致性保证

### 年龄计算一致性

- **规则**: 前端统一使用 `calculateAge(birthday)` 函数
- **实现**: 在 `utils/age-calculator.js` 中定义
- **使用**: 所有页面import该工具函数,确保计算逻辑一致

### 性别选项一致性

- **来源**: 统一从 API `GET /api/dictionary/sexes` 获取
- **缓存**: 首次获取后缓存到本地,避免重复请求
- **更新**: 当数据库更新后,清除缓存重新获取

### 搜索行为一致性

- **防抖时间**: 统一使用 400ms
- **空结果处理**: 统一显示 "未找到匹配{宠物/报告}" + "清空搜索" 按钮
- **搜索关键词**: 实时高亮显示在结果中

---

**数据模型设计完成时间**: 2025-12-28
**下一步**: 生成 API 契约文档 (contracts/)
