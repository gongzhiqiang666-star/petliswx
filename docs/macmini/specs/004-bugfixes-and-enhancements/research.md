# Research Documentation: Bug Fixes and Enhancements

**Feature**: 004-bugfixes-and-enhancements
**Date**: 2025-12-28
**Purpose**: Verify technical assumptions and document existing architecture

## Executive Summary

本研究通过数据库查询和代码审查验证了功能规格说明书中的假设。关键发现：

1. ✅ **数据库表结构已验证**: 所有核心表存在，字段名使用PascalCase（如animal_name, hospital_id），非snake_case
2. ✅ **后端实体已创建**: Pet, User, Hospital等实体类已实现
3. ⚠️ **订单表结构需确认**: 数据库中未找到独立的"订单"表，lin_lb_infocheckreceived字段不足
4. ✅ **用户表包含hospital_id字段**: lin_user表有hospital_id字段，可用于查询用户所属医院
5. ✅ **宠物表包含createtime字段**: lin_lb_pet表有createtime和updatetime字段（int类型）
6. ❓ **通知表不存在**: 需要创建新的通知记录表
7. ✅ **微信小程序前端结构清晰**: 已有订单、宠物、我的等页面

---

## 1. 数据库表结构验证

### 1.1 核心业务表

#### ✅ lin_lb_pet (宠物表)
- **主键**: `id` (int, AUTO_INCREMENT)
- **字段命名**: PascalCase (e.g., animal_name, owner_name, info_hospital_id)
- **关键字段**:
  - `id`: 宠物ID
  - `animal_name`: 宠物中文名
  - `animal_name_en`: 宠物英文名
  - `info_animal_id`: 物种ID
  - `info_variety_id`: 品种ID
  - `dict_sex_id`: 性别ID
  - `birthday`: 生日 (varchar, yyyy-MM-dd格式)
  - `chip_no`: 芯片号
  - `owner_name`: 畜主中文名
  - `owner_name_en`: 畜主英文名
  - `info_hospital_id`: 所属医院ID (int, 非空)
  - `createtime`: 创建时间 (int, Unix时间戳)
  - `updatetime`: 更新时间 (int, Unix时间戳)
  - `IsActive`: 是否激活 (int)

**结论**: ✅ 完全满足需求，可用于判断"最新创建的宠物"

#### ✅ lin_user (用户表)
- **主键**: `id` (int, AUTO_INCREMENT)
- **字段命名**: snake_case
- **关键字段**:
  - `id`: 用户ID
  - `username`: 用户名
  - `nickname`: 昵称
  - `mobile`: 手机号
  - `avatar`: 头像URL
  - `hospital_id`: 医院ID (int, 默认0)
  - `openid`: 微信openid (用于小程序登录)
  - `createtime`: 创建时间
  - `updatetime`: 更新时间

**结论**: ✅ 包含hospital_id字段，可用于查询用户所属医院

#### ⚠️ lin_lb_infocheckreceived (检测申请表?)
- **主键**: `InfoCheckReceivedId`
- **字段**:
  - `InfoCheckReceivedId`: 主键
  - `CheckType`: 检测类型 (varchar)
  - `IsActive`: 是否激活
  - `createtime`: 创建时间
  - `updatetime`: 更新时间

**结论**: ⚠️ **字段不足，可能不是订单主表**。需要查找其他订单相关表。

#### ✅ lin_lb_payordermain (支付订单主表)
- **主键**: `IID`
- **字段**:
  - `IID`: 主键
  - `OrderId`: 订单号
  - `Md5Id`: MD5 ID
  - `AliTradeId`: 支付宝交易ID
  - `HostId`: 主机ID
  - `PayMonth`: 支付月份
  - `CHKMoney`: 支付金额
  - `CreateDt`: 创建时间
  - `UpdateDt`: 更新时间
  - `IsSuccess`: 是否成功
  - `Remark`: 备注

**结论**: ✅ 这是支付订单表，但不是检测申请订单表。需要查找检测申请相关的主表。

### 1.2 字典表

#### ✅ lin_lb_infosampletype (样本类型表)
- **主键**: `InfoSampleTypeId`
- **字段**:
  - `InfoSampleTypeId`: 样本类型ID
  - `SampleName`: 样本名称 (中文)
  - `SampleName_en`: 样本名称 (英文)
  - `IsActive`: 是否激活
  - `createtime`: 创建时间
  - `updatetime`: 更新时间

**验证**: ✅ 当前已实现样本类型选择页面，API返回66条记录，字段映射正确

#### ✅ lin_lb_infotesttype (检测项目表)
- **主键**: `InfoTestTypeId`
- **字段**:
  - `InfoTestTypeId`: 检测项目ID
  - `TestName`: 项目名称
  - `TestNO`: 项目编号
  - `Price`: 价格
  - `CtoG`: 宠物到实验室价格
  - `GtoC`: 实验室到宠物价格
  - `DiscountReal`: 实际折扣
  - `DiscountNext`: 下次折扣
  - `TextModel`: 文本模板 (longtext)
  - `CreateDt`: 创建时间
  - `IsActive`: 是否激活

**结论**: ✅ 检测项目表结构完整

### 1.3 医院表

#### ✅ lin_lb_infohospital (医院表)
- **主键**: `InfoHospitalId`
- **字段**:
  - `InfoHospitalId`: 医院ID
  - `HospitalName`: 医院名称 (中文)
  - `HospitalName_en`: 医院名称 (英文)
  - `Invoice`: 发票信息
  - `CityId`: 城市ID
  - `Place`: 地点
  - `Adress`: 地址 (text)
  - `Tel`: 联系电话
  - `Fax`: 传真
  - `InfoHospitalContactId`: 联系人ID
  - `CreateDt`: 创建时间
  - `IsActive`: 是否激活
  - `LoginName`: 登录用户名
  - `LoginPwd`: 登录密码
  - `amount`: 金额
  - `PayType`: 支付类型 (int, 可能表示月结/次结)

**结论**: ✅ 医院表结构完整，PayType字段可能表示客户类型（月结/次结）

### 1.4 表结构命名规范总结

**重要发现**: 数据库字段命名不统一！
- **lin_lb_pet**: PascalCase (animal_name, owner_name)
- **lin_user**: snake_case (hospital_id, createtime)
- **lin_lb_infohospital**: PascalCase (HospitalName, InfoHospitalId)
- **lin_lb_infosampletype**: PascalCase (SampleName, InfoSampleTypeId)

**结论**: ⚠️ **后端实体类应与数据库字段名保持一致，使用@Column注解明确映射**

---

## 2. 后端API结构验证

### 2.1 实体类 (Entity)

#### ✅ Pet.java
- **位置**: `/petlis-backend/src/main/java/com/petlis/entity/business/Pet.java`
- **继承**: `ActivableEntity` (提供createtime, updatetime, IsActive字段)
- **JPA映射**: 正确使用@Column注解映射数据库字段
- **字段验证**: 所有字段与lin_lb_pet表一一对应

**代码示例**:
```java
@Entity
@Table(name = "lin_lb_pet")
public class Pet extends ActivableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "animal_name", nullable = false, length = 100)
    private String animalName;

    @Column(name = "info_hospital_id", nullable = false)
    private Integer infoHospitalId;

    @Column(name = "createtime")
    private Integer createtime;

    @Column(name = "updatetime")
    private Integer updatetime;
}
```

**结论**: ✅ **实体类设计正确，字段命名遵循数据库**

#### ✅ User.java
- **位置**: `/petlis-backend/src/main/java/com/petlis/entity/business/User.java`
- **继承**: `BaseEntity`
- **JPA映射**: 正确映射lin_user表
- **关键字段**: `hospitalId` (用于查询用户所属医院)

**代码示例**:
```java
@Entity
@Table(name = "lin_user")
public class User extends BaseEntity {
    @Column(name = "hospital_id", columnDefinition = "int unsigned")
    private Integer hospitalId;

    @Column(name = "openid", unique = true, length = 100)
    private String openid;
}
```

**结论**: ✅ **用户实体包含hospitalId字段，满足需求**

### 2.2 控制器 (Controller)

#### ✅ 已实现的Controller
- **AuthController**: 认证相关 (微信登录)
- **PetController**: 宠物管理 (列表、详情、新增、编辑)
- **OrderController**: 订单管理
- **DictionaryController**: 字典数据 (样本类型、检测项目等)
- **ReportController**: 报告管理
- **FileController**: 文件上传下载

**结论**: ✅ **核心Controller已创建**

### 2.3 服务层 (Service)

#### ✅ 已实现的Service
- **AuthService**: 认证服务
- **PetService**: 宠物服务
- **OrderService**: 订单服务
- **DictionaryService**: 字典服务
- **ReportService**: 报告服务
- **UserService**: 用户服务
- **FileService**: 文件服务

**结论**: ✅ **服务层架构完整**

### 2.4 API响应格式

#### ✅ 统一响应结构
```java
public class Result<T> {
    private Integer code;      // 状态码
    private String message;    // 消息
    private T data;           // 数据
}
```

**示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "animalName": "旺财",
    "ownerName": "张三"
  }
}
```

**结论**: ✅ **符合RESTful规范，统一{code, message, data}结构**

---

## 3. 前端小程序结构验证

### 3.1 订单页面 (pages/orders)

#### ✅ orders.js (订单列表)
- **API调用**: `api.orders.getList({pageNum: 1, pageSize: 100})`
- **当前实现**:
  - 固定加载100条记录
  - 使用tab筛选不同状态的订单
  - **未实现分页加载**（下拉加载更多）

**问题**:
1. ❌ 未实现分页加载（FR-003要求默认显示5条）
2. ❌ 未实现无限滚动（FR-004要求下拉加载更多）
3. ❌ 数据显示未验证（需要检查是否正确显示实际数据）

**代码片段**:
```javascript
async loadOrders() {
  const result = await api.orders.getList({
    pageNum: 1,
    pageSize: 100  // ❌ 固定100条，不符合需求
  })

  this.setData({
    orders: orderList,
    loading: false,
    isEmpty: orderList.length === 0
  })
}
```

**结论**: ⚠️ **需要重构订单列表页，实现分页加载**

### 3.2 订单详情页 (pages/order-detail)

#### ⚠️ 需要验证
- 文件存在: `/miniprogram/pages/order-detail/order-detail.js`
- **问题**: 需要检查是否正确显示订单详情数据
- **验证方法**: 读取文件并检查API调用和数据显示逻辑

### 3.3 宠物详情页 (pages/pet-detail)

#### ⚠️ 需要验证
- 文件存在: `/miniprogram/pages/pet-detail/pet-detail.js`
- **问题**:
  1. 检测报告列表是否正确显示申请时间和报告状态（FR-009）
  2. 是否按时间倒序排列（FR-010）

### 3.4 宠物编辑页 (pages/order-add-pet)

#### ✅ order-add-pet.js (新增/编辑宠物)
- **已实现**: 样本类型选择页面集成
- **问题**:
  1. 编辑模式下，表单是否正确预填充宠物信息？（FR-008）
  2. 需要检查loadPetDetail方法

### 3.5 宠物选择页 (pages/order-select-pet)

#### ⚠️ 需要验证
- 文件存在: `/miniprogram/pages/order-select-pet/order-select-pet.js`
- **问题**:
  1. 新增宠物后是否自动选中新宠物？（FR-012）
  2. 刷新后是否默认选中最新宠物？（FR-013）

### 3.6 我的页面 (pages/mine)

#### ⚠️ 需要验证
- 文件存在: `/miniprogram/pages/mine/mine.js`
- **问题**:
  1. 是否显示用户所属医院信息？（FR-015, FR-016）
  2. 是否根据user.hospitalId查询医院？

---

## 4. 未知项与需要澄清的问题

### 4.1 订单表结构 ❓

**问题**: 数据库中未找到完整的订单表。

**候选表**:
1. `lin_lb_infocheckreceived` - 字段不足，可能只是字典表
2. `lin_lb_payordermain` - 支付订单表，不是检测申请表
3. **可能存在其他订单表，未在lin_lb_前缀下**

**需要验证**:
1. 查询MySQL中所有包含"order"、"application"、"check"的表
2. 检查后端Order实体类映射的表名
3. 查看后端OrderService中使用的Repository

**下一步行动**:
```sql
-- 查找所有可能的订单表
SHOW TABLES LIKE '%order%';
SHOW TABLES LIKE '%application%';
SHOW TABLES LIKE '%check%';
SHOW TABLES;  -- 列出所有表
```

### 4.2 订单状态枚举值 ❓

**假设**: 订单状态可能为 -2, -1, 0, 1, 2, 3
- -2: 草稿/待支付
- -1: 已取消
- 0: 待审核
- 1: 已确认
- 2: 检测中
- 3: 已完成

**验证**: 需要查看后端`OrderStatusEnum.java`文件

### 4.3 微信通知基础设施 ⚠️

**假设**: 已有微信企业号或小程序订阅消息配置

**验证**:
1. 检查后端`WechatUtil.java`工具类
2. 确认是否有发送通知的方法
3. 检查application.yml中的微信配置

**可能缺失**:
- 微信模板消息ID
- 微信API调用接口
- 通知发送服务

### 4.4 客户类型字段 ⚠️

**假设**: 用户或医院表中有客户类型字段（月结/次结）

**发现**:
- `lin_lb_infohospital.PayType` (int) - 可能表示支付类型
- 需要确认PayType的枚举值（0=月结, 1=次结？）

**下一步**: 查询医院表PayType字段的实际值分布

---

## 5. 技术栈验证

### 5.1 后端技术栈 ✅

**已验证**:
- ✅ Java 17
- ✅ Spring Boot 3.x
- ✅ Spring Data JPA
- ✅ MySQL Connector
- ✅ JWT认证 (JwtUtil.java)
- ✅ Lombok (用于实体类)
- ✅ Swagger (SwaggerConfig.java)

**分层架构** ✅:
```
Controller → Service → Repository → Entity
```

### 5.2 前端技术栈 ✅

**已验证**:
- ✅ 微信小程序框架
- ✅ JavaScript ES6+
- ✅ WXML模板
- ✅ WXSS样式

**API调用**:
```javascript
// utils/api.js 或 api/index.js
const api = {
  orders: {
    getList: (params) => request.get('/api/orders/list', params)
  }
}
```

### 5.3 数据库 ✅

**已验证**:
- ✅ MySQL 8.0
- ✅ utf8mb4编码
- ✅ Schema: laboklin

**表前缀规律**:
- `lin_user` - 用户表
- `lin_lb_*` - 实验室业务表
  - `lin_lb_pet` - 宠物
  - `lin_lb_infohospital` - 医院
  - `lin_lb_infosampletype` - 样本类型
  - `lin_lb_infotesttype` - 检测项目

---

## 6. 关键假设验证结果

| 假设 | 验证结果 | 说明 |
|------|---------|------|
| 数据库字段名使用snake_case | ⚠️ 部分正确 | 实际为混合命名：lin_user使用snake_case，lin_lb_*使用PascalCase |
| 用户token包含hospitalId | ✅ 正确 | lin_user表有hospital_id字段，JWT应包含该字段 |
| 宠物表有createtime字段 | ✅ 正确 | lin_lb_pet表有createtime (int类型) |
| 订单状态为整数枚举 | ⚠️ 待确认 | 需查看OrderStatusEnum |
| 已有微信通知基础设施 | ⚠️ 待确认 | 需检查WechatUtil和配置 |
| 后端API返回{code, message, data} | ✅ 正确 | Result<T>统一响应结构 |
| 后端使用MySQL触发器 | ❓ 待实现 | FR-028要求创建触发器 |

---

## 7. Phase 1设计建议

基于研究结果，Phase 1设计时应注意：

### 7.1 数据模型设计

1. **创建通知记录表** (NEW):
   ```sql
   CREATE TABLE lin_lb_notification (
     id INT AUTO_INCREMENT PRIMARY KEY,
     order_id INT NOT NULL COMMENT '订单ID',
     hospital_id INT NOT NULL COMMENT '医院ID',
     old_status INT COMMENT '原状态',
     new_status INT NOT NULL COMMENT '新状态',
     operator_id INT COMMENT '操作人ID',
     operation_time DATETIME NOT NULL COMMENT '操作时间',
     wechat_sent_status TINYINT DEFAULT 0 COMMENT '微信发送状态 0-待发送 1-已发送 2-失败',
     wechat_message_id VARCHAR(100) COMMENT '微信消息ID',
     error_message TEXT COMMENT '错误信息',
     createtime INT UNSIGNED COMMENT '创建时间',
     updatetime INT UNSIGNED COMMENT '更新时间',
     INDEX idx_order_id (order_id),
     INDEX idx_hospital_id (hospital_id)
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态变化通知记录表';
   ```

2. **订单表需要确认**: 如果不存在订单主表，需要创建或找到正确的表

### 7.2 MySQL触发器设计

**触发器**: 订单状态变化时自动创建通知记录
```sql
DELIMITER $$

CREATE TRIGGER trg_order_status_change
AFTER UPDATE ON [order_table]
FOR EACH ROW
BEGIN
  IF OLD.order_status != NEW.order_status THEN
    INSERT INTO lin_lb_notification (
      order_id, hospital_id, old_status, new_status,
      operator_id, operation_time, createtime, updatetime
    ) VALUES (
      NEW.id, NEW.hospital_id, OLD.order_status, NEW.order_status,
      NEW.operator_id, NOW(), UNIX_TIMESTAMP(), UNIX_TIMESTAMP()
    );
  END IF;
END$$

DELIMITER ;
```

### 7.3 前端分页加载设计

**订单列表分页**:
- 默认加载5条 (pageSize: 5)
- 滚动到底部时加载更多 (onReachBottom)
- 使用concat追加数据，不替换

**伪代码**:
```javascript
Page({
  data: {
    orders: [],
    pageNum: 1,
    pageSize: 5,
    hasMore: true
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMore()
    }
  },

  async loadMore() {
    const { pageNum, pageSize } = this.data
    const result = await api.orders.getList({
      pageNum: pageNum + 1,
      pageSize
    })

    this.setData({
      orders: this.data.orders.concat(result.list),
      pageNum: pageNum + 1,
      hasMore: result.list.length >= pageSize
    })
  }
})
```

### 7.4 宠物选择自动选中逻辑

**新增宠物后自动选中**:
1. 新增宠物成功后，返回宠物ID
2. 宠物选择页面的onShow中检查参数
3. 如果有newPetId，设置为选中状态

**伪代码**:
```javascript
// order-add-pet.js (新增宠物页面)
onSaveSuccess() {
  const pages = getCurrentPages()
  const prevPage = pages[pages.length - 2]

  // 返回并传递新宠物ID
  prevPage.setData({
    newPetId: this.data.newPetId
  })

  wx.navigateBack()
}

// order-select-pet.js (宠物选择页面)
onShow() {
  const pages = getCurrentPages()
  const currPage = pages[pages.length - 1]

  if (currPage.data.newPetId) {
    this.setData({
      selectedPetId: currPage.data.newPetId
    })
  }
}
```

---

## 8. 下一步行动

### 8.1 Phase 1 数据模型设计 ✅

创建以下文档:
1. **data-model.md**: 详细的数据模型设计（实体关系、字段定义）
2. **contracts/**: API接口契约（请求/响应格式）
3. **quickstart.md**: 开发环境配置和快速启动指南

### 8.2 订单表确认 🔴 **CRITICAL**

**必须完成**:
1. 查询所有表，找到订单主表
2. 如果不存在，需要设计并创建订单表
3. 确认订单状态字段的枚举值

### 8.3 前端代码审查 ⚠️

**需要审查的页面**:
1. order-detail.js - 订单详情数据显示
2. pet-detail.js - 检测报告显示
3. order-add-pet.js - 宠物编辑预填充
4. order-select-pet.js - 自动选择逻辑
5. mine.js - 医院信息显示

### 8.4 后端API审查 ⚠️

**需要验证的API**:
1. GET /api/orders/list - 订单列表（支持分页）
2. GET /api/orders/{id} - 订单详情
3. GET /api/pets/{id} - 宠物详情
4. GET /api/user/hospital - 获取用户医院信息
5. GET /api/pets/{id}/reports - 宠物检测报告列表

---

## 9. 风险与依赖

### 9.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 订单表结构不明确 | 🔴 高 | 优先查询确认，必要时创建新表 |
| 字段命名不统一 | 🟡 中 | 使用@Column注解明确映射 |
| 微信通知未实现 | 🟡 中 | P3优先级，可后期实现 |
| 支付模块未开发 | 🟢 低 | 仅预留接口，P3优先级 |

### 9.2 依赖项

**外部依赖**:
- ✅ MySQL数据库已连接
- ✅ 微信小程序基础框架
- ⚠️ 微信通知服务（待确认）
- ❌ 支付接口（未开发，P3）

**内部依赖**:
- ✅ 后端实体类已创建
- ✅ 前端页面框架已搭建
- ⚠️ 订单Service实现（需审查）

---

## 10. 结论

### ✅ 已验证

1. 数据库表结构基本清晰，字段命名虽有混合但可映射
2. 后端实体类正确映射数据库表
3. 用户表包含hospital_id字段，可用于查询用户医院
4. 宠物表包含createtime字段，可用于判断最新宠物
5. API响应格式统一，遵循RESTful规范

### ⚠️ 需要进一步调查

1. **订单主表未找到** - 这是关键问题，必须在Phase 1前解决
2. 订单状态枚举值 - 需查看OrderStatusEnum
3. 微信通知基础设施 - 需检查WechatUtil和配置
4. 前端页面实际数据绑定 - 需审查各页面JS文件

### 📋 Phase 1准备就绪

本研究已验证大部分技术假设，Phase 1可以开始数据模型和API契约设计。

**前提条件**: 必须先确认订单表结构，否则无法继续设计订单相关功能。
