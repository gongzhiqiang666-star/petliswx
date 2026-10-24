# API契约: 订单模块 (Order API)

**Feature**: 004-bugfixes-and-enhancements
**Version**: 1.0.0
**Date**: 2025-12-28

---

## 1. 订单列表 (分页)

### 1.1 获取订单列表

**接口**: `GET /api/orders/list`

**功能说明**:
- 支持分页查询订单列表
- 默认每页5条记录（FR-003）
- 支持按状态筛选
- 支持按时间倒序排列

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| pageNum | int | 是 | 页码（从1开始） | 1 |
| pageSize | int | 是 | 每页条数（建议5或10） | 5 |
| status | int | 否 | 订单状态筛选（-2=草稿, 0=待审核, 2=等待报告, 3=已完成） | 0 |

#### 响应数据

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 23,
    "pageNum": 1,
    "pageSize": 5,
    "pages": 5,
    "list": [
      {
        "diagnosisDetailId": 123,
        "genterNO": "ORD20251228001",
        "diagnosisStatus": 2,
        "statusLabel": "等待报告",
        "payStatus2": 1,
        "createDt": "2025-12-28 10:30:00",
        "pet": {
          "id": 456,
          "animalName": "旺财",
          "animalNameEn": "WangCai",
          "chipNo": "CN20251228001"
        },
        "hospital": {
          "infoHospitalId": 789,
          "hospitalName": "北京宠物医院"
        },
        "testItems": [
          {
            "infoTestTypeId": 101,
            "testName": "血常规检查",
            "testPrice": 50.00
          }
        ],
        "realFee": 50.00,
        "pdfHttpUrl": "https://example.com/report/123.pdf"
      }
    ]
  }
}
```

#### 字段说明

**订单对象字段**:
- `diagnosisDetailId`: 订单ID
- `genterNO`: 订单编号
- `diagnosisStatus`: 订单状态码
- `statusLabel`: 状态标签（中文）
- `payStatus2`: 支付状态（0=未支付, 1=已支付）
- `createDt`: 创建时间
- `pet`: 宠物信息（嵌套对象）
- `hospital`: 医院信息（嵌套对象）
- `testItems`: 检测项目列表（数组）
- `realFee`: 实际费用
- `pdfHttpUrl`: 报告PDF地址

#### 错误响应

```json
{
  "code": 500,
  "message": "获取订单列表失败",
  "data": null
}
```

---

## 2. 订单详情

### 2.1 获取订单详情

**接口**: `GET /api/orders/{orderId}`

**功能说明**:
- 根据订单ID获取完整的订单详细信息
- 包括宠物信息、检测项目、采样信息、送检信息等

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| orderId | int | 是 | 订单ID | 123 |

#### 响应数据

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisDetailId": 123,
    "genterNO": "ORD20251228001",
    "diagnosisStatus": 2,
    "statusLabel": "等待报告",
    "payStatus2": 1,
    "createDt": "2025-12-28 10:30:00",
    "sendMailDt": "2025-12-28 15:30:00",
    "detailRemark": "加急处理",

    "pet": {
      "id": 456,
      "animalName": "旺财",
      "animalNameEn": "WangCai",
      "infoAnimalId": 1,
      "infoVarietyId": 10,
      "dictSexId": 1,
      "birthday": "2020-05-15",
      "chipNo": "CN20251228001",
      "ownerName": "张三",
      "ownerNameEn": "Zhang San",
      "infoHospitalId": 789
    },

    "sampleInfo": {
      "infoSampleTypeId": 5,
      "sampleName": "血液",
      "sampleNameEn": "Blood",
      "sampleDate": "2025-12-28",
      "sampleNote": "空腹采样"
    },

    "testItems": [
      {
        "infoTestTypeId": 101,
        "testName": "血常规检查",
        "testNO": "TEST001",
        "testPrice": 50.00,
        "orginalPrice": 100.00,
        "orginalDiscount": 50.00
      },
      {
        "infoTestTypeId": 102,
        "testName": "生化检查",
        "testNO": "TEST002",
        "testPrice": 150.00,
        "orginalPrice": 200.00,
        "orginalDiscount": 25.00
      }
    ],

    "hospital": {
      "infoHospitalId": 789,
      "hospitalName": "北京宠物医院",
      "hospitalNameEn": "Beijing Pet Hospital",
      "adress": "北京市朝阳区xxx",
      "tel": "010-12345678"
    },

    "realFee": 200.00,
    "pdfHttpUrl": "https://example.com/report/123.pdf",
    "isCheckPass": 1,
    "isSendMail": 1
  }
}
```

#### 字段说明

**宠物信息 (pet)**:
- `id`: 宠物ID
- `animalName`: 宠物中文名
- `animalNameEn`: 宠物英文名
- `infoAnimalId`: 物种ID
- `infoVarietyId`: 品种ID
- `dictSexId`: 性别ID
- `birthday`: 生日
- `chipNo`: 芯片号
- `ownerName`: 畜主姓名
- `infoHospitalId`: 医院ID

**采样信息 (sampleInfo)**:
- `infoSampleTypeId`: 样本类型ID
- `sampleName`: 样本类型名称
- `sampleDate`: 采样日期
- `sampleNote`: 采样备注

**检测项目 (testItems)**:
- `infoTestTypeId`: 检测项目ID
- `testName`: 项目名称
- `testPrice`: 实际价格
- `orginalPrice`: 原始价格
- `orginalDiscount`: 折扣

#### 错误响应

```json
{
  "code": 404,
  "message": "订单不存在",
  "data": null
}
```

---

## 3. 前端调用示例

### 3.1 订单列表分页加载

```javascript
// pages/orders/orders.js

Page({
  data: {
    orders: [],
    pageNum: 1,
    pageSize: 5,
    total: 0,
    hasMore: true,
    loading: false
  },

  async loadOrders(isLoadMore = false) {
    if (this.data.loading) return

    this.setData({ loading: true })

    try {
      const { pageNum, pageSize } = this.data
      const result = await api.orders.getList({
        pageNum: isLoadMore ? pageNum + 1 : 1,
        pageSize
      })

      if (isLoadMore) {
        // 追加数据
        this.setData({
          orders: this.data.orders.concat(result.list),
          pageNum: pageNum + 1,
          hasMore: result.list.length >= pageSize
        })
      } else {
        // 替换数据
        this.setData({
          orders: result.list,
          pageNum: 1,
          hasMore: result.list.length >= pageSize,
          total: result.total
        })
      }
    } catch (err) {
      console.error('[Orders] 加载失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onReachBottom() {
    // 滚动到底部时加载更多
    if (this.data.hasMore && !this.data.loading) {
      this.loadOrders(true)
    }
  }
})
```

### 3.2 订单详情数据展示

```javascript
// pages/order-detail/order-detail.js

Page({
  data: {
    orderId: null,
    orderDetail: null,
    loading: false
  },

  onLoad(options) {
    this.setData({ orderId: options.id })
    this.loadOrderDetail()
  },

  async loadOrderDetail() {
    this.setData({ loading: true })

    try {
      const result = await api.orders.getDetail(this.data.orderId)

      // 数据验证：确保关键字段存在
      if (!result.pet) {
        console.error('[OrderDetail] 宠物信息缺失')
      }

      this.setData({
        orderDetail: result
      })
    } catch (err) {
      console.error('[OrderDetail] 加载失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
```

---

## 4. 数据验证规则

### 4.1 订单列表验证

**必填字段**:
- ✅ `diagnosisDetailId`: 必须存在且>0
- ✅ `genterNO`: 必须存在且非空
- ✅ `diagnosisStatus`: 必须是有效状态码（-2, 0, 1, 2, 3, 4）
- ✅ `pet.animalName`: 必须存在且非空（FR-001要求）
- ✅ `pet.id`: 必须存在且>0

**可选字段**:
- `pdfHttpUrl`: 可能为空（报告未生成时）

### 4.2 订单详情验证

**必填字段**:
- ✅ 所有订单列表字段
- ✅ `pet.chipNo`: 芯片号（如有）
- ✅ `sampleInfo.sampleName`: 样本类型名称
- ✅ `testItems`: 至少包含一个检测项目

**显示规则** (FR-002):
- 如果字段为空，显示"-"或"未填写"
- 如果数组为空，显示"暂无数据"

---

## 5. 性能要求

### 5.1 响应时间

- 订单列表API: < 200ms (p95)
- 订单详情API: < 300ms (p95)

### 5.2 分页优化

**后端优化**:
- 使用索引: `idx_create_dt`, `idx_diagnosis_status`
- 限制最大pageSize: 100

**前端优化**:
- 节流滚动事件: 300ms
- 防抖重复请求: loading标志位

---

## 6. 测试用例

### 6.1 订单列表测试

| 用例编号 | 测试场景 | 输入 | 预期输出 |
|---------|---------|------|---------|
| TC-001 | 正常分页查询 | pageNum=1, pageSize=5 | 返回5条订单，total>0 |
| TC-002 | 最后一页 | pageNum=999, pageSize=5 | 返回0-5条订单 |
| TC-003 | 状态筛选 | status=2 | 只返回等待报告的订单 |
| TC-004 | 空列表 | 新用户无订单 | list=[], total=0 |
| TC-005 | 数据完整性 | 任意订单 | 所有必填字段非空 |

### 6.2 订单详情测试

| 用例编号 | 测试场景 | 输入 | 预期输出 |
|---------|---------|------|---------|
| TC-101 | 正常查询 | orderId=123 | 返回完整订单详情 |
| TC-102 | 订单不存在 | orderId=99999 | code=404, message="订单不存在" |
| TC-103 | 宠物信息缺失 | 脏数据订单 | pet=null或空对象 |
| TC-104 | 检测项目为空 | 草稿订单 | testItems=[] |

---

## 7. 错误码定义

| 错误码 | 说明 | 处理建议 |
|--------|------|---------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 检查参数格式 |
| 401 | 未登录 | 跳转登录页 |
| 403 | 无权限 | 提示无权限 |
| 404 | 资源不存在 | 提示订单不存在 |
| 500 | 服务器错误 | 重试或联系客服 |

---

**文档版本**: 1.0.0
**最后更新**: 2025-12-28
