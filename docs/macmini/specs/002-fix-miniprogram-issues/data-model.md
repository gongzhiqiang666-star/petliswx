# 数据模型: 修复微信小程序显示和流程问题

**功能**: 001-fix-miniprogram-issues
**日期**: 2025-12-28
**阶段**: Phase 1 - 数据模型设计

---

## 概述

本功能纯前端修改,不涉及后端数据模型变更。前端数据模型直接对应后端API返回的实体结构。

---

## 实体定义

### 1. Pet (宠物)

**数据来源**: 后端API `/api/pets`

```javascript
{
  "id": Number,                    // 宠物ID
  "AnimalName": String,            // 宠物中文名 (必填)
  "AnimalNameEn": String,          // 宠物英文名 (必填)
  "AnimalTypeId": Number,          // 品种ID
  "DictSexId": Number,             // 性别ID (0=未知,1=公,2=母,3=其他)
  "Age": String,                   // 年龄 (格式: "3岁2个月")
  "ChipNo": String,                // 芯片号
  "Birthday": String,              // 出生日期 (YYYY-MM-DD)
  "MasterName": String,            // 主人姓名 (必填)
  "MasterNameEn": String,          // 主人英文名
  "InfoAnimalId": Number,          // 动物物种ID
  "InfoHospitalId": Number,         // 医院ID
  "IsActive": Number,              // 是否激活 (0=禁用,1=启用)
  "CreateDt": String               // 创建时间
}
```

**关系**:
- 所属医院: `InfoHospitalId` → Hospital.id
- 品种: `AnimalTypeId` → InfoVariety.IID
- 物种: `InfoAnimalId` → InfoAnimal.InfoAnimalId
- 性别: `DictSexId` → DictSex.DictSexId

**验证规则**:
- 前端: 显示时检查必填字段是否有值
- 后端: 创建/更新时验证必填字段

---

### 2. Order (订单/检测申请)

**数据来源**: 后端API `/api/orders`

```javascript
{
  "id": Number,                    // 订单ID
  "ListDiagnosisProjectId": Number,// 主表ID
  "UserNO": String,                // 用户编号 (格式: "55550-{id}")
  "SampleStatus": Number,          // 样本状态 (-2=草稿,-1=全部,0=待审核,1=审核未通过,2=等待报告,3=已完成,4=已取消)
  "AnimalName": String,            // 宠物中文名 (注意字段名)
  "AnimalNameEn": String,          // 宠物英文名
  "AnimalTypeId": Number,          // 品种ID
  "DictSexId": Number,             // 性别ID
  "Age": String,                   // 年龄
  "ChipNo": String,                // 芯片号 (狂犬检测必填)
  "Birthday": String,              // 出生日期 (狂犬检测必填)
  "MasterName": String,            // 主人姓名
  "MasterNameEn": String,          // 主人英文名
  "DoctorName": String,            // 医师姓名
  "InfoSampleTypeId": Number,      // 采样类型ID
  "SampleArea": String,            // 采样部位
  "CollectDt": String,             // 采样日期
  "CityId": Number,                // 城市ID
  "Remark": String,                // 备注
  "TestFee": Number,               // 检测总费用
  "IsPayMoney": Number,            // 是否已结算 (0=未,1=已)
  "CreateDt": String,              // 创建时间
  "diagnosisDetails": Array<DiagnosisDetail>  // 检测明细列表
}
```

**关系**:
- 关联宠物: 通过AnimalName/AnimalNameEn关联
- 检测明细: 一对多关联 `diagnosisDetails`
- 城市: `CityId` → InfoCity.CityId
- 医院: `InfoHospitalId` → InfoHospital.InfoHospitalId

**状态流转**:
```
-2(草稿) → 0(待审核) → 2(等待报告) → 3(已完成)
                      ↘ 1(审核不通过)
                      ↘ 4(已取消)
```

---

### 3. DiagnosisDetail (检测明细)

**数据来源**: 嵌入在Order对象中

```javascript
{
  "DiagnosisDetailId": Number,    // 明细ID
  "ListDiagnosisProjectId": Number,// 关联主订单ID
  "InfoTestTypeId": Number,       // 检测项ID
  "GenterNO": String,             // 生成编号 (格式: "{订单ID}-{序号}")
  "TestPrice": Number,            // 检测价格
  "OrginalPrice": Number,         // 原始价格
  "OrginalDiscount": Number,      // 折扣 (10=原价)
  "PayStatus2": Number,           // 支付状态 (0=未结,1=现金,2=转账,3=网银,4=忽略)
  "DiagnosisStatus": Number,      // 诊断状态 (0=待检测)
  "DetailRemark": String,         // 详细备注 (狂犬检测必填快递单号)
  "IsActive": Number,             // 是否激活 (0=否,1=是)
  "CreateDt": String              // 创建时间
}
```

**关系**:
- 所属订单: `ListDiagnosisProjectId` → Order.ListDiagnosisProjectId
- 检测项: `InfoTestTypeId` → InfoTestType.InfoTestTypeId

**特殊验证**:
- 狂犬检测(InfoTestTypeId=1225):
  - 必须验证订单主表的ChipNO已填写
  - 必须验证订单主表的Birthday已填写
  - DetailRemark必填(快递单号)

---

### 4. User (用户)

**数据来源**: 后端API `/api/user/profile`

```javascript
{
  "id": Number,                    // 用户ID
  "openid": String,                // 微信openid
  "nickname": String,              // 昵称
  "mobile": String,                // 手机号
  "email": String,                 // 邮箱
  "avatarUrl": String,             // 头像URL
  "InfoHospitalId": Number,        // 所属医院ID
  "IsActive": Number,              // 是否激活
  "CreateDt": String               // 创建时间
}
```

**关系**:
- 所属医院: `InfoHospitalId` → Hospital.InfoHospitalId

---

### 5. Hospital (医院)

**数据来源**: 后端API `/api/dictionary/hospitals` 或从User对象嵌入

```javascript
{
  "InfoHospitalId": Number,       // 医院ID
  "HospitalName": String,         // 医院中文名
  "HospitalNameEn": String,       // 医院英文名
  "Address": String,              // 地址
  "Phone": String,                // 电话
  "BusinessHours": String,        // 营业时间
  "CityId": Number,               // 所在城市ID
  "PayType": Number,              // 支付类型 (1=月结)
  "IsActive": Number              // 是否启用
}
```

**关系**:
- 所在城市: `CityId` → InfoCity.CityId

---

### 6. InfoTestType (检测项目字典)

**数据来源**: 后端API `/api/dictionary/test-types`

```javascript
{
  "InfoTestTypeId": Number,       // 检测项ID
  "TestTypeName": String,         // 检测项名称
  "TestTypeCode": String,         // 检测项代码
  "CategoryId": Number,           // 分类ID (用于分组)
  "IsActive": Number              // 是否启用
}
```

**价格信息**:
通过 `/api/dictionary/test-types/{id}/price?cityId={cityId}` 获取

---

## 表单数据模型(临时存储)

### 预约表单数据

存储在`wx.setStorageSync('order_form')`:

```javascript
{
  "petId": Number,                 // 选中的宠物ID
  "sampleInfo": {
    "InfoSampleTypeId": Number,    // 采样类型ID
    "SampleArea": String,          // 采样部位
    "CollectDt": String,           // 采样日期
    "CityId": Number               // 城市ID (自动填充)
  },
  "otherInfo": {
    "Remark": String               // 备注
  },
  "deliveryInfo": {
    "MasterName": String,          // 主人姓名 (自动从宠物信息填充)
    "MasterNameEn": String,        // 主人英文名
    "DoctorName": String           // 医师姓名
  },
  "selectedTestItems": Array<{    // 已选检测项目
    "InfoTestTypeId": Number,
    "TestTypeName": String,
    "TestPrice": Number
  }>,
  "currentStep": Number            // 当前步骤 (1-4)
}
```

---

## 状态码映射

### SampleStatus (订单状态)

| 值 | 标签 | 颜色 | 描述 |
|----|------|------|------|
| -2 | 草稿 | 灰色 | 已创建但未提交 |
| -1 | 全部 | - | 筛选用,不显示 |
| 0  | 待审核 | 橙色 | 等待实验室审核 |
| 1  | 审核不通过 | 红色 | 审核失败 |
| 2  | 等待报告 | 蓝色 | 样本检测中 |
| 3  | 已完成 | 绿色 | 报告已生成 |
| 4  | 已取消 | 灰色 | 订单已取消 |

### DictSexId (性别)

| 值 | 标签 |
|----|------|
| 0  | 未知 |
| 1  | 公 ♂ |
| 2  | 母 ♀ |
| 3  | 其他 |

---

## 数据验证规则

### 前端验证(提交前)

```javascript
// 采样信息验证
function validateSampleInfo(data) {
  if (!data.InfoSampleTypeId) return '请选择采样类型';
  if (!data.SampleArea) return '请填写采样部位';
  if (!data.CollectDt) return '请选择采样日期';
  return null; // 验证通过
}

// 送检信息验证
function validateDeliveryInfo(data) {
  if (!data.MasterName) return '请填写主人姓名';
  if (!data.DoctorName) return '请填写医师姓名';
  return null;
}

// 狂犬检测特殊验证
function validateRabiesTest(orderData, testItems) {
  const hasRabies = testItems.some(item => item.InfoTestTypeId === 1225);
  if (hasRabies) {
    if (!orderData.ChipNo) return '狂犬检测必须填写芯片号';
    if (!orderData.Birthday) return '狂犬检测必须填写出生日期';
    const rabiesItem = testItems.find(item => item.InfoTestTypeId === 1225);
    if (!rabiesItem.DetailRemark) return '狂犬检测必须填写快递单号';
  }
  return null;
}
```

---

## API响应格式

### 成功响应

```javascript
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // 实际数据
  }
}
```

### 错误响应

```javascript
{
  "code": 400,
  "message": "参数校验失败: ChipNo不能为空",
  "data": null
}
```

---

## 数据缓存策略

### 缓存列表

| 缓存Key | 数据类型 | 过期时间 | 用途 |
|---------|----------|----------|------|
| `order_form` | Object | 手动清理 | 预约表单临时数据 |
| `selected_pet_id` | Number | 提交后清理 | 选中的宠物ID |
| `user_info` | Object | 7天 | 当前用户信息 |
| `hospital_info` | Object | 7天 | 所属医院信息 |

### 清理策略

```javascript
// 提交预约成功后清理
wx.removeStorageSync('order_form');
wx.removeStorageSync('selected_pet_id');

// 退出登录后清理
wx.removeStorageSync('user_info');
wx.removeStorageSync('hospital_info');
```

---

**结论**: 前端数据模型已定义完整,直接对应后端API结构,可进入下一阶段(接口契约定义)。