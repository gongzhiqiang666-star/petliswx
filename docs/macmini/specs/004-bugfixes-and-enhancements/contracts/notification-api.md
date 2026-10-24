# API契约: 通知模块 (Notification API)

**Feature**: 004-bugfixes-and-enhancements
**Version**: 1.0.0
**Priority**: P3 (未来功能)
**Date**: 2025-12-28

---

## 1. 概述

通知模块用于订单状态变化时的微信通知推送（FR-023至FR-028）。

**注意**: 此模块为P3优先级，当前仅设计API接口和数据库结构，实际微信推送功能暂不实现。

---

## 2. 数据库表

### 2.1 通知记录表

**表名**: `lin_lb_notification`

详见 `data-model.md` 第1.7节。

---

## 3. API接口

### 3.1 获取医院待发送通知列表

**接口**: `GET /api/notifications/pending`

**功能说明**:
- 获取指定医院待发送的微信通知
- 用于定时任务扫描并发送通知

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| hospitalId | int | 是 | 医院ID | 789 |
| limit | int | 否 | 限制条数（默认50） | 50 |

#### 响应数据

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 10,
    "list": [
      {
        "id": 1,
        "orderId": 123,
        "hospitalId": 789,
        "oldStatus": 0,
        "newStatus": 2,
        "newStatusLabel": "等待报告",
        "operatorId": 456,
        "operationTime": "2025-12-28 15:30:00",
        "wechatSentStatus": 0,
        "wechatMessageId": null
      }
    ]
  }
}
```

---

### 3.2 更新通知发送状态

**接口**: `PUT /api/notifications/{notificationId}/status`

**功能说明**:
- 更新微信通知的发送状态
- 标记成功或失败

#### 请求体

```json
{
  "wechatSentStatus": 1,
  "wechatMessageId": "msg_1234567890",
  "errorMessage": null
}
```

#### 字段说明

- `wechatSentStatus`: 发送状态（0=待发送, 1=已发送, 2=失败）
- `wechatMessageId`: 微信消息ID（成功时必填）
- `errorMessage`: 错误信息（失败时必填）

#### 响应数据

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 1,
    "wechatSentStatus": 1,
    "updatetime": 1703894400
  }
}
```

---

### 3.3 获取通知历史

**接口**: `GET /api/notifications/history`

**功能说明**:
- 查询指定订单的状态变化历史

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
    "total": 3,
    "list": [
      {
        "id": 3,
        "oldStatus": 2,
        "newStatus": 3,
        "oldStatusLabel": "等待报告",
        "newStatusLabel": "已完成",
        "operationTime": "2025-12-28 16:00:00",
        "operatorName": "实验室管理员"
      },
      {
        "id": 2,
        "oldStatus": 0,
        "newStatus": 2,
        "oldStatusLabel": "待审核",
        "newStatusLabel": "等待报告",
        "operationTime": "2025-12-28 15:30:00",
        "operatorName": "实验室管理员"
      },
      {
        "id": 1,
        "oldStatus": -2,
        "newStatus": 0,
        "oldStatusLabel": "草稿",
        "newStatusLabel": "待审核",
        "operationTime": "2025-12-28 14:00:00",
        "operatorName": "张三"
      }
    ]
  }
}
```

---

## 4. MySQL触发器

### 4.1 触发器定义

详见 `data-model.md` 第4.1节。

**触发时机**: `lin_lb_diagnosisdetail` 表的 `DiagnosisStatus` 字段发生变化时自动创建通知记录。

---

## 5. 微信通知模板

### 5.1 通知内容格式

**订单状态变化通知**:
```
【PETLIS检测通知】
订单号：{genterNO}
宠物：{animalName}
状态：{newStatusLabel}
时间：{operationTime}

点击查看详情
```

### 5.2 跳转链接

通知点击后跳转到订单详情页：
```
pages/order-detail/order-detail?id={orderId}
```

---

## 6. 定时任务

### 6.1 通知发送任务

**执行频率**: 每1分钟扫描一次

**任务逻辑**:
1. 查询 `wechat_sent_status=0` 的通知记录
2. 按医院分组
3. 调用微信API发送通知
4. 更新发送状态
5. 失败的记录 `wechat_sent_status=2`，等待重试

### 6.2 重试策略

- 最大重试次数: 3次
- 重试间隔: 5分钟
- 超过重试次数后标记为失败，不再重试

---

## 7. 接口预留

### 7.1 支付接口预留（FR-022）

**接口**: `POST /api/payments/create`

**状态**: 预留，暂不实现

**请求体**:
```json
{
  "orderId": 123,
  "payAmount": 200.00,
  "payType": "wechat"
}
```

**响应** (当前实现):
```json
{
  "code": 501,
  "message": "支付功能开发中，请暂时使用其他支付方式",
  "data": {
    "orderId": 123,
    "payAmount": 200.00
  }
}
```

---

## 8. 数据验证

### 8.1 通知记录必填字段

- ✅ `order_id`: 订单ID
- ✅ `hospital_id`: 医院ID
- ✅ `new_status`: 新状态
- ✅ `operation_time`: 操作时间

### 8.2 发送状态验证

- `wechat_sent_status`: 只能是 0, 1, 2
- `wechat_message_id`: `wechat_sent_status=1` 时必填
- `error_message`: `wechat_sent_status=2` 时必填

---

## 9. 测试用例

| 用例编号 | 测试场景 | 预期结果 |
|---------|---------|---------|
| TC-NOT-001 | 订单状态变化 | 触发器自动创建通知记录 |
| TC-NOT-002 | 查询待发送通知 | 返回wechat_sent_status=0的记录 |
| TC-NOT-003 | 更新发送状态为成功 | wechatSentStatus=1, wechatMessageId有值 |
| TC-NOT-004 | 查询通知历史 | 返回完整的状态变化历史 |

---

**文档版本**: 1.0.0
**最后更新**: 2025-12-28
**状态**: 设计完成，待实现
