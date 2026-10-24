# Data Model: Bug Fixes and Enhancements

**Feature**: 004-bugfixes-and-enhancements
**Date**: 2025-12-28
**Version**: 1.0.0

## Overview

本文档定义了PETLIS Bug修复和增强功能的数据模型，包括实体关系、字段定义、索引设计、约束条件和数据流转。

---

## 1. 核心实体 (Entity)

### 1.1 订单/检测申请 (DiagnosisDetail)

**表名**: `lin_lb_diagnosisdetail`
**主键**: `DiagnosisDetailId`
**实体类**: `DiagnosisDetail.java`

#### 字段定义

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| DiagnosisDetailId | int | NO | AUTO | 主键ID |
| ListDiagnosisProjectId | int | NO | - | 所属项目ID (关联lin_lb_listdiagnosisproject) |
| GenterNO | varchar(250) | NO | - | 检测编号（唯一） |
| InfoTestTypeId | int | NO | - | 检测项目ID (关联lin_lb_infotesttype) |
| InfoTestTypeModelId | int | NO | - | 检测项目模型ID |
| MailTitle | varchar(250) | NO | - | 邮件标题 |
| CreateDt | datetime | YES | NULL | 创建时间 |
| IsChangeModelTemple | int | NO | - | 是否修改模板 (0=否, 1=是) |
| IsChangeMailModelTemple | int | NO | - | 是否修改邮件模板 (0=否, 1=是) |
| IsSendMail | int | NO | - | 是否已发送邮件 (0=否, 1=是) |
| RealFee | double | NO | - | 实际费用 |
| IsCheckPass | int | NO | - | 是否审核通过 (0=否, 1=是) |
| PDFHttpUrl | varchar(250) | NO | - | PDF报告地址 |
| TestPrice | decimal(18,2) | NO | - | 测试价格 |
| OrginalPrice | decimal(18,2) | NO | - | 原始价格 |
| OrginalDiscount | decimal(18,2) | YES | 0.00 | 原始折扣 |
| SendMailDt | datetime | NO | - | 发送邮件时间 |
| DetailRemark | varchar(250) | NO | - | 明细备注 |
| **PayStatus2** | **int** | **NO** | **-** | **支付状态 (0=未支付, 1=已支付)** |
| **DiagnosisStatus** | **int** | **NO** | **-** | **订单状态 (见状态枚举)** |
| IsActive | int | NO | - | 是否启用 (0=禁用, 1=启用) |
| is_invoiced | tinyint | NO | 0 | 是否已开发票 (0=否, 1=是) |

#### 订单状态枚举 (DiagnosisStatus)

| 状态码 | 枚举名 | 说明 | 业务含义 |
|--------|--------|------|----------|
| -2 | DRAFT | 草稿 | 订单创建但未提交 |
| -1 | ALL | 全部 | 查询条件，非实际状态 |
| 0 | PENDING_REVIEW | 待审核 | 等待实验室审核 |
| 1 | REVIEW_FAILED | 审核未通过 | 审核失败，需要修改 |
| 2 | WAITING_REPORT | 等待报告 | 实验室检测中，等待报告 |
| 3 | COMPLETED | 已完成 | 检测完成，报告已发出 |
| 4 | CANCELLED | 已取消 | 订单已取消 |

#### 状态流转规则

```
草稿(-2) ─┬→ 待审核(0) ──→ 审核未通过(1)
          │              └→ 等待报告(2) ─→ 已完成(3)
          └→ 已取消(4)
```

**业务规则**:
- 草稿可以转为待审核或已取消
- 待审核可以转为审核不通过或等待报告
- 等待报告只能转为已完成
- 已完成和已取消不能流转

#### 关联关系

- **所属项目** (`ListDiagnosisProjectId`) → `lin_lb_listdiagnosisproject`
- **检测项目** (`InfoTestTypeId`) → `lin_lb_infotesttype`
- **宠物**: 通过`ListDiagnosisProjectId`间接关联

---

### 1.2 宠物 (Pet)

**表名**: `lin_lb_pet`
**主键**: `id`
**实体类**: `Pet.java`

#### 字段定义

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | int | NO | AUTO | 宠物ID |
| animal_name | varchar(100) | NO | - | 宠物中文名 |
| animal_name_en | varchar(100) | YES | NULL | 宠物英文名 |
| info_animal_id | int | YES | NULL | 物种ID (关联lin_lb_infoanimal) |
| info_variety_id | int | YES | NULL | 品种ID (关联lin_lb_infovariety) |
| dict_sex_id | int | YES | NULL | 性别ID (关联lin_lb_dictsex) |
| birthday | varchar(20) | YES | NULL | 生日 (格式: yyyy-MM-dd) |
| chip_no | varchar(50) | YES | NULL | 芯片号 |
| owner_name | varchar(100) | NO | - | 畜主中文名 |
| owner_name_en | varchar(100) | YES | NULL | 畜主英文名 |
| info_hospital_id | int | NO | - | 所属医院ID (关联lin_lb_infohospital) |
| IsActive | int | NO | 1 | 是否激活 (0=禁用, 1=启用) |
| **createtime** | **int** | **YES** | **NULL** | **创建时间 (Unix时间戳)** |
| **updatetime** | **int** | **YES** | **NULL** | **更新时间 (Unix时间戳)** |

#### 索引设计

```sql
-- 主键索引
PRIMARY KEY (id)

-- 医院ID索引 (用于查询某医院的所有宠物)
INDEX idx_info_hospital_id (info_hospital_id)

-- 芯片号唯一索引
UNIQUE INDEX uk_chip_no (chip_no)

-- 创建时间索引 (用于查询最新宠物)
INDEX idx_createtime (createtime)
```

#### 关联关系

- **物种** (`info_animal_id`) → `lin_lb_infoanimal`
- **品种** (`info_variety_id`) → `lin_lb_infovariety`
- **性别** (`dict_sex_id`) → `lin_lb_dictsex`
- **医院** (`info_hospital_id`) → `lin_lb_infohospital`

---

### 1.3 用户 (User)

**表名**: `lin_user`
**主键**: `id`
**实体类**: `User.java`

#### 字段定义

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | int | NO | AUTO | 用户ID |
| group_id | int | NO | 0 | 组别ID |
| username | varchar(32) | YES | "" | 用户名 |
| nickname | varchar(50) | YES | "" | 昵称 |
| password | varchar(32) | YES | "" | 密码 (MD5) |
| salt | varchar(30) | YES | "" | 密码盐 |
| email | varchar(100) | YES | "" | 电子邮箱 |
| mobile | varchar(11) | YES | "" | 手机号 |
| avatar | varchar(255) | YES | "" | 头像URL |
| level | tinyint | NO | 0 | 等级 |
| gender | tinyint | NO | 0 | 性别 (0=未知, 1=男, 2=女) |
| birthday | date | YES | NULL | 生日 |
| bio | varchar(100) | YES | "" | 格言 |
| money | decimal(10,2) | NO | 0.00 | 余额 |
| score | int | NO | 0 | 积分 |
| successions | int | NO | 1 | 连续登录天数 |
| maxsuccessions | int | NO | 1 | 最大连续登录天数 |
| prevtime | int | YES | NULL | 上次登录时间 (Unix时间戳) |
| logintime | int | YES | NULL | 登录时间 (Unix时间戳) |
| loginip | varchar(50) | YES | "" | 登录IP |
| loginfailure | tinyint | NO | 0 | 失败次数 |
| joinip | varchar(50) | YES | "" | 加入IP |
| jointime | int | YES | NULL | 加入时间 (Unix时间戳) |
| createtime | int | YES | NULL | 创建时间 (Unix时间戳) |
| updatetime | int | YES | NULL | 更新时间 (Unix时间戳) |
| token | varchar(50) | YES | "" | Token |
| status | varchar(30) | NO | "" | 状态 (normal=正常, hidden=隐藏) |
| verification | varchar(255) | YES | "" | 验证信息 |
| openid | varchar(100) | YES | NULL | **微信openid (唯一)** |
| **hospital_id** | **int** | **YES** | **0** | **所属医院ID** |

#### 索引设计

```sql
-- 主键索引
PRIMARY KEY (id)

-- openid唯一索引 (用于微信登录)
UNIQUE INDEX uk_openid (openid)

-- 医院ID索引
INDEX idx_hospital_id (hospital_id)
```

#### 关联关系

- **医院** (`hospital_id`) → `lin_lb_infohospital`

---

### 1.4 医院 (Hospital)

**表名**: `lin_lb_infohospital`
**主键**: `InfoHospitalId`
**实体类**: `InfoHospital.java` (需要创建)

#### 字段定义

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| InfoHospitalId | int | NO | AUTO | 医院ID |
| HospitalName | varchar | NO | - | 医院名称 |
| HospitalName_en | varchar | NO | - | 医院英文名称 |
| Invoice | varchar | YES | NULL | 发票信息 |
| CityId | int | YES | NULL | 城市ID (关联lin_lb_city) |
| Place | text | YES | NULL | 地点 |
| Adress | text | YES | NULL | 地址 |
| Tel | varchar | YES | NULL | 联系电话 |
| Fax | varchar | YES | NULL | 传真 |
| InfoHospitalContactId | int | YES | NULL | 联系人ID (关联lin_lb_infohospitalcontact) |
| CreateDt | datetime | YES | NULL | 创建时间 |
| IsActive | int | YES | NULL | 是否激活 (0=禁用, 1=启用) |
| LoginName | varchar | YES | NULL | 登录用户名 |
| LoginPwd | varchar | YES | NULL | 登录密码 |
| amount | decimal | YES | 0.00 | 金额 |
| **PayType** | **int** | **NO** | **-** | **支付类型 (0=月结, 1=次结)** |

#### 支付类型枚举 (PayType)

| 值 | 说明 | 业务含义 |
|----|------|----------|
| 0 | 月结客户 | 月底统一结算，无需预支付 |
| 1 | 次结客户 | 每次订单需要立即支付 |

#### 关联关系

- **城市** (`CityId`) → `lin_lb_city`
- **联系人** (`InfoHospitalContactId`) → `lin_lb_infohospitalcontact`

---

### 1.5 样本类型 (SampleType)

**表名**: `lin_lb_infosampletype`
**主键**: `InfoSampleTypeId`
**实体类**: `InfoSampleType.java`

#### 字段定义

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| InfoSampleTypeId | int | NO | AUTO | 样本类型ID |
| SampleName | varchar | NO | - | 样本名称 (中文) |
| SampleName_en | varchar | NO | - | 样本名称 (英文) |
| IsActive | int | NO | - | 是否激活 (0=禁用, 1=启用) |
| createtime | int | YES | NULL | 创建时间 |
| updatetime | int | YES | NULL | 更新时间 |

---

### 1.6 检测项目 (TestType)

**表名**: `lin_lb_infotesttype`
**主键**: `InfoTestTypeId`
**实体类**: `InfoTestType.java`

#### 字段定义

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| InfoTestTypeId | int | NO | AUTO | 检测项目ID |
| TestName | varchar | NO | - | 项目名称 |
| TestNO | varchar | NO | - | 项目编号 |
| Price | double | NO | - | 价格 |
| CtoG | double | NO | - | 宠物到实验室价格 |
| GtoC | double | NO | - | 实验室到宠物价格 |
| DiscountReal | double | NO | - | 实际折扣 |
| DiscountNext | double | NO | - | 下次折扣 |
| TextModel | longtext | NO | - | 文本模板 |
| CreateDt | datetime | NO | - | 创建时间 |
| IsActive | int | NO | - | 是否激活 (0=禁用, 1=启用) |
| type | tinyint | YES | 0 | 类型 |
| createtime | int | YES | NULL | 创建时间 |
| updatetime | int | YES | NULL | 更新时间 |

---

### 1.7 通知记录 (NotificationRecord) - **新增表**

**表名**: `lin_lb_notification`
**主键**: `id`
**实体类**: `NotificationRecord.java` (需要创建)

#### 字段定义

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | int | NO | AUTO | 主键ID |
| order_id | int | NO | - | 订单ID (DiagnosisDetailId) |
| hospital_id | int | NO | - | 医院ID (InfoHospitalId) |
| old_status | int | YES | NULL | 原状态 (DiagnosisStatus) |
| new_status | int | NO | - | 新状态 (DiagnosisStatus) |
| operator_id | int | YES | NULL | 操作人ID (lin_user.id) |
| operation_time | datetime | NO | - | 操作时间 |
| wechat_sent_status | tinyint | NO | 0 | **微信发送状态** (0=待发送, 1=已发送, 2=失败) |
| wechat_message_id | varchar(100) | YES | NULL | 微信消息ID |
| error_message | text | YES | NULL | 错误信息 |
| createtime | int | YES | NULL | 创建时间 (Unix时间戳) |
| updatetime | int | YES | NULL | 更新时间 (Unix时间戳) |

#### 微信发送状态枚举

| 值 | 说明 |
|----|------|
| 0 | 待发送 |
| 1 | 已发送 |
| 2 | 发送失败 |

#### 索引设计

```sql
-- 主键索引
PRIMARY KEY (id)

-- 订单ID索引 (用于查询订单的所有状态变化)
INDEX idx_order_id (order_id)

-- 医院ID索引 (用于查询医院的所有通知)
INDEX idx_hospital_id (hospital_id)

-- 操作时间索引 (用于按时间查询通知历史)
INDEX idx_operation_time (operation_time)

-- 复合索引 (用于查询某医院待发送的通知)
INDEX idx_hospital_status (hospital_id, wechat_sent_status)
```

#### 建表SQL

```sql
CREATE TABLE lin_lb_notification (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  order_id INT NOT NULL COMMENT '订单ID (DiagnosisDetailId)',
  hospital_id INT NOT NULL COMMENT '医院ID (InfoHospitalId)',
  old_status INT COMMENT '原状态 (DiagnosisStatus)',
  new_status INT NOT NULL COMMENT '新状态 (DiagnosisStatus)',
  operator_id INT COMMENT '操作人ID (lin_user.id)',
  operation_time DATETIME NOT NULL COMMENT '操作时间',
  wechat_sent_status TINYINT NOT NULL DEFAULT 0 COMMENT '微信发送状态 0-待发送 1-已发送 2-失败',
  wechat_message_id VARCHAR(100) COMMENT '微信消息ID',
  error_message TEXT COMMENT '错误信息',
  createtime INT UNSIGNED COMMENT '创建时间 (Unix时间戳)',
  updatetime INT UNSIGNED COMMENT '更新时间 (Unix时间戳)',
  INDEX idx_order_id (order_id),
  INDEX idx_hospital_id (hospital_id),
  INDEX idx_operation_time (operation_time),
  INDEX idx_hospital_status (hospital_id, wechat_sent_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态变化通知记录表';
```

---

## 2. 实体关系图 (ERD)

```
┌─────────────────────┐
│      lin_user       │
├─────────────────────┤
│ id (PK)            │
│ hospital_id (FK)   │◄───────┐
│ openid             │        │
│ ...                │        │
└─────────────────────┘        │
                              │
                ┌─────────────┴──────────────┐
                │                            │
                ▼                            ▼
    ┌───────────────────────┐    ┌─────────────────────┐
    │ lin_lb_infohospital   │    │   lin_lb_pet        │
    ├───────────────────────┤    ├─────────────────────┤
    │ InfoHospitalId (PK)  │    │ id (PK)            │
    │ HospitalName         │    │ animal_name        │
    │ PayType              │    │ info_hospital_id(FK)│
    │ ...                  │    │ createtime         │
    └───────────────────────┘    │ ...                │
                ▲                └─────────────────────┘
                │                            │
                │                            │
    ┌───────────┴──────────┐    ┌───────────┴──────────┐
    │ lin_lb_listdiagnosis │    │ lin_lb_diagnosisdetail│
    │project              │    ├─────────────────────┤
    ├─────────────────────┤    │ DiagnosisDetailId(PK)│
    │ ListDiagnosisProject│    │ ListDiagnosisProject │
    │    Id (PK)          │◄───│    Id (FK)          │
    │ pet_id (FK)         │    │ InfoTestTypeId (FK) │
    │ ...                 │    │ DiagnosisStatus     │
    └─────────────────────┘    │ PayStatus2          │
                               │ CreateDt            │
                               │ ...                 │
                               └─────────────────────┘
                                        │
                                        │ 新增关联
                                        ▼
                               ┌─────────────────────┐
                               │ lin_lb_notification │ (NEW)
                               ├─────────────────────┤
                               │ id (PK)            │
                               │ order_id (FK)      │
                               │ hospital_id (FK)   │
                               │ old_status         │
                               │ new_status         │
                               │ wechat_sent_status │
                               │ ...                │
                               └─────────────────────┘
```

---

## 3. 数据流转

### 3.1 订单创建流程

```
用户提交申请
    ↓
创建 DiagnosisDetail (状态: 草稿 -2)
    ↓
更新为待审核 (0)
    ↓
触发器自动创建 NotificationRecord
    ↓
微信通知推送到医院操作员
```

### 3.2 订单状态变化流程

```
待审核(0) ──实验室审核──→ 审核不通过(1)
    │
    ├──审核通过──→ 等待报告(2)
    │                │
    │                └─实验室检测──→ 已完成(3)
    │
    └──用户取消──→ 已取消(4)

每次状态变化都会:
1. 更新 DiagnosisDetail.DiagnosisStatus
2. 触发器创建 NotificationRecord 记录
3. 异步任务发送微信通知
```

### 3.3 支付流程 (根据PayType)

**月结客户 (PayType=0)**:
```
提交申请 → 待审核(0) → 审核通过 → 等待报告(2) → 月底统一结算
```

**次结客户 (PayType=1)**:
```
提交申请 → 草稿(-2) → 跳转支付 → 支付成功 → 待审核(0) → 等待报告(2)
```

---

## 4. MySQL触发器设计

### 4.1 订单状态变化触发器

**触发器名称**: `trg_diagnosis_status_change`

**触发时机**: AFTER UPDATE on `lin_lb_diagnosisdetail`

**触发条件**: `DiagnosisStatus` 字段发生变化

**触发动作**: INSERT into `lin_lb_notification`

#### 触发器SQL

```sql
DELIMITER $$

CREATE TRIGGER trg_diagnosis_status_change
AFTER UPDATE ON lin_lb_diagnosisdetail
FOR EACH ROW
BEGIN
  DECLARE v_hospital_id INT;
  DECLARE v_operator_id INT;

  -- 检查状态是否发生变化
  IF OLD.DiagnosisStatus != NEW.DiagnosisStatus THEN

    -- 获取医院ID (从关联的宠物或项目中获取)
    -- 注意: 这里需要根据实际的关联关系查询
    SELECT p.info_hospital_id INTO v_hospital_id
    FROM lin_lb_listdiagnosisproject ldp
    JOIN lin_lb_pet p ON ldp.pet_id = p.id
    WHERE ldp.ListDiagnosisProjectId = NEW.ListDiagnosisProjectId
    LIMIT 1;

    -- 操作人ID从应用层传入，这里设置为NULL或从session获取
    SET v_operator_id = NULL;

    -- 插入通知记录
    INSERT INTO lin_lb_notification (
      order_id,
      hospital_id,
      old_status,
      new_status,
      operator_id,
      operation_time,
      createtime,
      updatetime
    ) VALUES (
      NEW.DiagnosisDetailId,
      v_hospital_id,
      OLD.DiagnosisStatus,
      NEW.DiagnosisStatus,
      v_operator_id,
      NOW(),
      UNIX_TIMESTAMP(),
      UNIX_TIMESTAMP()
    );

  END IF;
END$$

DELIMITER ;
```

### 4.2 触发器优化建议

1. **错误处理**: 添加异常处理，避免触发器失败影响业务
2. **性能优化**: 批量更新时触发器会执行多次，可考虑应用层批量创建通知
3. **日志记录**: 可添加触发器执行日志表

---

## 5. 数据字典

### 5.1 订单状态字典

| 状态码 | 中文名 | English | 允许的操作 |
|--------|--------|---------|-----------|
| -2 | 草稿 | Draft | 提交、取消 |
| -1 | 全部 | All | (查询用) |
| 0 | 待审核 | Pending Review | 审核通过、审核不通过 |
| 1 | 审核未通过 | Review Failed | 重新提交 |
| 2 | 等待报告 | Waiting Report | 完成检测 |
| 3 | 已完成 | Completed | 查看报告 |
| 4 | 已取消 | Cancelled | (无) |

### 5.2 支付状态字典

| 状态码 | 说明 |
|--------|------|
| 0 | 未支付 |
| 1 | 已支付 |

### 5.3 微信通知状态字典

| 状态码 | 说明 | 业务处理 |
|--------|------|----------|
| 0 | 待发送 | 加入发送队列 |
| 1 | 已发送 | 标记成功，记录wechat_message_id |
| 2 | 发送失败 | 记录error_message，等待重试 |

---

## 6. 数据一致性

### 6.1 事务边界

**创建订单事务**:
```java
@Transactional
public void createOrder(OrderRequest request) {
    // 1. 创建订单
    DiagnosisDetail order = new DiagnosisDetail();
    order.setDiagnosisStatus(-2); // 草稿
    order.setPayStatus2(0);       // 未支付
    diagnosisDetailRepository.save(order);

    // 2. 关联宠物
    // 3. 关联检测项目
    // 4. 创建通知记录 (如果状态不是草稿)
}
```

**更新订单状态事务**:
```java
@Transactional
public void updateOrderStatus(Integer orderId, Integer newStatus) {
    // 1. 检查状态流转合法性
    // 2. 更新订单状态 (触发器自动创建通知记录)
    // 3. 异步发送微信通知
}
```

### 6.2 数据完整性约束

1. **外键约束**:
   - `lin_lb_pet.info_hospital_id` → `lin_lb_infohospital.InfoHospitalId`
   - `lin_lb_diagnosisdetail.InfoTestTypeId` → `lin_lb_infotesttype.InfoTestTypeId`

2. **唯一约束**:
   - `lin_user.openid` UNIQUE
   - `lin_lb_pet.chip_no` UNIQUE

3. **非空约束**:
   - 所有主键非空
   - 关键业务字段非空 (如DiagnosisStatus, PayStatus2)

---

## 7. 数据迁移

### 7.1 新增通知表

**执行顺序**:
1. 创建 `lin_lb_notification` 表
2. 创建触发器 `trg_diagnosis_status_change`
3. 部署微信通知服务

### 7.2 历史数据补充

如果需要补充历史订单的状态变化记录，可以执行:

```sql
-- 为现有订单创建初始通知记录（仅用于初始化）
INSERT INTO lin_lb_notification (order_id, hospital_id, new_status, operation_time, wechat_sent_status, createtime, updatetime)
SELECT
    d.DiagnosisDetailId,
    p.info_hospital_id,
    d.DiagnosisStatus,
    FROM_UNIXTIME(d.createtime),
    1, -- 标记为已发送（历史数据不需要发送）
    UNIX_TIMESTAMP(),
    UNIX_TIMESTAMP()
FROM lin_lb_diagnosisdetail d
JOIN lin_lb_listdiagnosisproject ldp ON d.ListDiagnosisProjectId = ldp.ListDiagnosisProjectId
JOIN lin_lb_pet p ON ldp.pet_id = p.id
WHERE d.DiagnosisStatus != -2; -- 不包括草稿
```

---

## 8. 性能优化

### 8.1 索引策略

**高频查询字段索引**:
- `DiagnosisStatus`: 订单列表筛选
- `CreateDt`: 订单时间排序
- `hospital_id`: 医院数据权限过滤
- `createtime` (pet): 最新宠物查询

### 8.2 查询优化

**分页查询**:
```sql
-- 订单列表分页
SELECT d.* FROM lin_lb_diagnosisdetail d
JOIN lin_lb_listdiagnosisproject ldp ON d.ListDiagnosisProjectId = ldp.ListDiagnosisProjectId
WHERE ldp.pet_id IN (SELECT id FROM lin_lb_pet WHERE info_hospital_id = ?)
  AND d.DiagnosisStatus IN (0, 2, 3)
ORDER BY d.CreateDt DESC
LIMIT ? OFFSET ?;
```

**最新宠物查询**:
```sql
-- 查询最新创建的宠物
SELECT * FROM lin_lb_pet
WHERE info_hospital_id = ?
ORDER BY createtime DESC
LIMIT 1;
```

---

## 9. 安全与权限

### 9.1 数据权限

**医院数据隔离**:
- 用户只能查看自己医院的宠物和订单
- 通过 `hospital_id` 过滤数据

**实现方式**:
```java
@Aspect
@Component
public class DataPermissionAspect {

    @Around("@annotation(DataPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) {
        Integer hospitalId = SecurityUtils.getCurrentUserHospitalId();
        // 在查询条件中追加 hospital_id 过滤
    }
}
```

### 9.2 敏感数据加密

**敏感字段**:
- `lin_user.password`: MD5加密
- `lin_user.mobile`: 脱敏显示
- `lin_lb_pet.chip_no`: 可选加密

---

## 10. 备份与恢复

### 10.1 备份策略

**全量备份**: 每日凌晨3点
```bash
mysqldump -u root -p laboklin > /backup/laboklin_$(date +%Y%m%d).sql
```

**增量备份**: 每小时一次 binlog备份

### 10.2 恢复流程

1. 停止应用服务
2. 恢复全量备份
3. 应用增量binlog
4. 验证数据完整性
5. 启动应用服务

---

## 11. 监控与告警

### 11.1 关键指标

- 订单创建成功率
- 订单状态变化频率
- 微信通知发送成功率
- 触发器执行失败次数

### 11.2 告警规则

- 触发器执行失败 → 立即告警
- 通知表堆积超过1000条 → 告警
- 微信通知发送失败率 > 10% → 告警

---

## 附录A: 实体类映射关系

### A.1 实体类与表映射

| 实体类 | 表名 | 状态 |
|--------|------|------|
| User.java | lin_user | ✅ 已创建 |
| Pet.java | lin_lb_pet | ✅ 已创建 |
| DiagnosisDetail.java | lin_lb_diagnosisdetail | ✅ 已创建 |
| DiagnosisDetailInfo.java | lin_lb_diagnosisdetail_info | ✅ 已创建 |
| ListDiagnosisProject.java | lin_lb_listdiagnosisproject | ✅ 已创建 |
| InfoSampleType.java | lin_lb_infosampletype | ✅ 已创建 |
| InfoTestType.java | lin_lb_infotesttype | ✅ 已创建 |
| **NotificationRecord.java** | **lin_lb_notification** | ❌ **需要创建** |
| InfoHospital.java | lin_lb_infohospital | ⚠️ 需要确认 |

### A.2 字段命名规范

**Java实体类**: camelCase (animalName)
**数据库表**: 混合命名
  - `lin_user`: snake_case (hospital_id)
  - `lin_lb_*`: PascalCase (animal_name)

**JPA映射**: 使用@Column注解明确映射
```java
@Column(name = "animal_name", nullable = false, length = 100)
private String animalName;
```

---

## 附录B: 状态流转验证

### B.1 状态流转测试用例

| 当前状态 | 目标状态 | 预期结果 |
|---------|---------|---------|
| -2 (草稿) | 0 (待审核) | ✅ 允许 |
| -2 (草稿) | 4 (已取消) | ✅ 允许 |
| -2 (草稿) | 2 (等待报告) | ❌ 不允许 |
| 0 (待审核) | 1 (审核不通过) | ✅ 允许 |
| 0 (待审核) | 2 (等待报告) | ✅ 允许 |
| 0 (待审核) | 3 (已完成) | ❌ 不允许 |
| 2 (等待报告) | 3 (已完成) | ✅ 允许 |
| 3 (已完成) | 0 (待审核) | ❌ 不允许 |

---

**文档版本**: 1.0.0
**最后更新**: 2025-12-28
**作者**: PETLIS Team
