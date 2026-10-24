# Data Model: 预约信息填写菜单优化

**Feature**: 预约信息填写菜单优化
**Date**: 2025-12-30
**Related Spec**: [spec.md](./spec.md)

---

## Overview

本功能主要涉及以下实体：
1. **DiagnosisProject** - 预约订单主表（修改：age字段验证、reportNO生成逻辑）
2. **DiagnosisDetail** - 检测项目明细表（无结构变化）
3. **InfoTestType** - 检测项目字典表（新增分页查询支持）
4. **InfoTestTypePrice** - 检测项目价格表（已有）
5. **SampleType** - 样本类型字典表（已有）

---

## Entity Definitions

### 1. DiagnosisProject (lin_lb_diagnosisproject)

**表名**: `lin_lb_diagnosisproject`
**描述**: 预约订单主表，存储宠物信息、样本信息、配送信息、订单状态和报告编号

**主要字段**:

| 字段名 | 类型 | 约束 | 描述 | 变更说明 |
|--------|------|------|------|----------|
| iid | INTEGER | PK, AUTO_INCREMENT | 主键ID | - |
| AnimalName | VARCHAR(100) | NOT NULL | 宠物姓名 | - |
| AnimalTypeId | INTEGER | FK | 物种ID（关联字典表） | - |
| AnimalType | VARCHAR(50) | | 物种名称 | - |
| DictSexId | INTEGER | | 性别（0=未知，1=公，2=母，3=其他） | - |
| Birthday | DATE | | 出生日期（狂犬检测必填） | - |
| **Age** | **VARCHAR(20)** | **无变化** | **年龄，格式"Y/M"，如"3Y7M"、"5M"、"3Y"** | **新增验证逻辑** |
| **AnimalWeight** | **VARCHAR(20)** | **改为可选** | **体重（如"15kg"）** | **从必填改为可选** |
| ChipNO | VARCHAR(50) | | 芯片号（狂犬检测必填） | - |
| InfoSampleTypeId | INTEGER | FK | 样本类型ID | - |
| CollectDt | DATE | | 采样日期 | - |
| SampleStatus | INTEGER | NOT NULL | 订单状态（-2=草稿，0=待审核） | 提交时从-2更新为0 |
| **ReportNO** | **VARCHAR(50)** | **新增** | **德国报告编号，格式"RAB{年}{订单ID}"** | **提交时生成** |
| OwnerName | VARCHAR(100) | NOT NULL | 畜主姓名 | - |
| SendDoctor | VARCHAR(100) | | 送检医师 | - |
| Remark | TEXT | | 备注（狂犬检测必填快递单号） | - |
| CreateDt | DATETIME | | 创建时间 | - |
| UpdateDt | DATETIME | | 更新时间 | - |
| IsActive | INTEGER | DEFAULT 1 | 逻辑删除标记（0=删除，1=有效） | - |

**状态转换**:
```
订单创建: SampleStatus = 0 (待审核)
   ↓
提交订单: SampleStatus = 0 (已在创建时设置为待审核，无需额外转换)
```

**验证规则**:
- **Age**: 格式必须为"Y/M"、"Y"或"M"，总月数不超过1200（100岁）
  - 正则: `^(\d+)Y(\d+)M$|^(\d+)Y$|^(\d+)M$`
  - 示例有效值: "3Y7M"、"5M"、"3Y"
  - 示例无效值: "3"、"Y7M"、"abc"
- **AnimalWeight**: 可选字段，如果填写则格式建议为"{数字}kg"
- **SampleStatus**: 提交时必须为0（待审核）
- **ReportNO**: 格式必须为"RAB{4位年份}{订单ID}"

**业务规则**:
1. 狂犬检测（infoTestTypeId=1225）特殊验证：
   - ChipNO必填
   - Birthday必填
   - Remark必填（快递单号）
2. 报告编号生成规则：`RAB{当前年份}{订单ID}`
   - 示例：第一个订单 → "RAB20251"
   - 示例：第100个订单 → "RAB2025100"

---

### 2. DiagnosisDetail (lin_lb_diagnosisdetail)

**表名**: `lin_lb_diagnosisdetail`
**描述**: 检测项目明细表，关联订单和检测项目

**主要字段**:

| 字段名 | 类型 | 约束 | 描述 |
|--------|------|------|------|
| iid | INTEGER | PK, AUTO_INCREMENT | 主键ID |
| DiagnosisProjectId | INTEGER | FK → lin_lb_diagnosisproject.iid | 关联订单ID |
| InfoTestTypeId | INTEGER | FK → lin_lb_infotesttype.iid | 检测项目ID |
| TestPrice | DECIMAL(10,2) | | 检测项目价格（从InfoTestTypePrice表获取） |
| OrginalPrice | DECIMAL(10,2) | | 原价（等于TestPrice） |
| OrginalDiscount | DECIMAL(10,2) | | 折扣（固定为10） |
| SendMailDt | DATETIME | DEFAULT '1900-01-01 00:00:00' | 发送邮件时间（默认值） |
| IsActive | INTEGER | DEFAULT 1 | 逻辑删除标记 |
| CreateDt | DATETIME | | 创建时间 |
| UpdateDt | DATETIME | | 更新时间 |

**验证规则**:
- InfoTestTypeId必填且必须在InfoTestTypePrice表中存在且IsActive=1
- TestPrice从InfoTestTypePrice表获取，不由前端传递
- OrginalDiscount固定为10

---

### 3. InfoTestType (lin_lb_infotesttype)

**表名**: `lin_lb_infotesttype`
**描述**: 检测项目字典表

**主要字段**:

| 字段名 | 类型 | 约束 | 描述 | 变更说明 |
|--------|------|------|------|----------|
| iid | INTEGER | PK, AUTO_INCREMENT | 主键ID | - |
| TestName | VARCHAR(200) | NOT NULL | 检测项目名称 | 用于搜索 |
| TestNameEn | VARCHAR(200) | | 检测项目英文名称 | - |
| Price | DECIMAL(10,2) | | 价格（参考值，实际价格在InfoTestTypePrice表） | - |
| IsActive | INTEGER | DEFAULT 1 | 是否启用（0=禁用，1=启用） | - |
| CreateDt | DATETIME | | 创建时间 | - |
| UpdateDt | DATETIME | | 更新时间 | - |

**查询优化**:
- 新增分页查询方法：`findByTestNameContaining(String keyword, Pageable pageable)`
- 新增按ID查询：`findById(Integer id)`
- 新增按启用状态查询：`findByIsActive(Integer isActive)`

---

### 4. InfoTestTypePrice (lin_lb_infotesttype_price)

**表名**: `lin_lb_infotesttype_price`
**描述**: 检测项目价格表（按城市定价）

**主要字段**:

| 字段名 | 类型 | 约束 | 描述 |
|--------|------|------|------|
| iid | INTEGER | PK, AUTO_INCREMENT | 主键ID |
| InfoTestTypeId | INTEGER | FK → lin_lb_infotesttype.iid | 检测项目ID |
| CityId | INTEGER | | 城市ID |
| Price | DECIMAL(10,2) | | 价格 |
| CtoG | DECIMAL(10,2) | | 中德价格（暂未使用） |
| GtoC | DECIMAL(10,2) | | 德中价格（暂未使用） |
| IsActive | INTEGER | DEFAULT 1 | 是否启用（0=禁用，1=启用） |
| CreateDt | INTEGER | | 创建时间（Unix时间戳） |
| UpdateDt | INTEGER | | 更新时间（Unix时间戳） |

**查询方法**:
- `findByInfoTestTypeidAndIsActive(Integer testTypeId, Integer isActive)` - 查询指定检测项目的启用价格

---

### 5. SampleType (lin_lb_sampletype)

**表名**: `lin_lb_sampletype`
**描述**: 样本类型字典表

**主要字段**:

| 字段名 | 类型 | 约束 | 描述 |
|--------|------|------|------|
| iid | INTEGER | PK, AUTO_INCREMENT | 主键ID |
| SampleTypeName | VARCHAR(100) | NOT NULL | 样本类型名称（如"血液"、"尿液"） |
| SampleTypeNameEn | VARCHAR(100) | | 样本类型英文名称 |
| IsActive | INTEGER | DEFAULT 1 | 是否启用（0=禁用，1=启用） |
| CreateDt | DATETIME | | 创建时间 |
| UpdateDt | DATETIME | | 更新时间 |

**查询方法**:
- `findByIsActive(Integer isActive)` - 查询所有启用的样本类型
- `findById(Integer id)` - 按ID查询

---

## Data Relationships

### ER Diagram (简化版)

```
DiagnosisProject (1) ←───── (N) DiagnosisDetail
       │                                │
       │                                │
       ├───── AnimalType (Species)       ├───── InfoTestType
       │
       ├───── SampleType
       │
       └───── InfoTestTypePrice (通过InfoTestType关联)
```

### 关键关系说明

1. **DiagnosisProject → DiagnosisDetail** (1:N)
   - 一个订单包含多个检测项目明细
   - 外键：DiagnosisDetail.DiagnosisProjectId → DiagnosisProject.iid

2. **DiagnosisDetail → InfoTestType** (N:1)
   - 每个明细关联一个检测项目
   - 外键：DiagnosisDetail.InfoTestTypeId → InfoTestType.iid

3. **InfoTestType ← InfoTestTypePrice** (1:N)
   - 一个检测项目可以有多个价格（按城市）
   - 外键：InfoTestTypePrice.InfoTestTypeId → InfoTestType.iid
   - 业务逻辑：选择IsActive=1的价格记录

4. **DiagnosisProject → SampleType** (N:1)
   - 每个订单关联一个样本类型
   - 外键：DiagnosisProject.InfoSampleTypeId → SampleType.iid

---

## Validation Matrix

| Entity | Field | Validation Rule | Error Message |
|--------|-------|-----------------|---------------|
| DiagnosisProject | Age | 正则`^(\d+)Y(\d+)M$\|^(\\d+)Y$\|^(\\d+)M$`，总月数≤1200 | "年龄格式错误，正确格式如：3Y7M、5M、3Y" |
| DiagnosisProject | AnimalWeight | 可选，填写时建议格式"{数字}kg" | 无（非必填） |
| DiagnosisProject | ChipNO | 如果包含狂犬检测则必填 | "狂犬检测必须填写芯片号" |
| DiagnosisProject | Birthday | 如果包含狂犬检测则必填 | "狂犬检测必须填写出生日期" |
| DiagnosisProject | Remark | 如果包含狂犬检测则必填 | "狂犬检测必须在备注中填写快递单号" |
| DiagnosisDetail | InfoTestTypeId | 必须在InfoTestTypePrice表中存在且IsActive=1 | "检测项目价格未启用" |
| InfoTestType | TestName | 用于搜索，支持模糊匹配 | 无 |

---

## Index Recommendations

现有索引（推测）:
```sql
-- DiagnosisProject
CREATE INDEX idx_sample_status ON lin_lb_diagnosisproject(SampleStatus);
CREATE INDEX idx_report_no ON lin_lb_diagnosisproject(ReportNO);

-- DiagnosisDetail
CREATE INDEX idx_project_id ON lin_lb_diagnosisdetail(DiagnosisProjectId);
CREATE INDEX idx_test_type_id ON lin_lb_diagnosisdetail(InfoTestTypeId);

-- InfoTestType
CREATE INDEX idx_test_name ON lin_lb_infotesttype(TestName);
CREATE INDEX idx_is_active ON lin_lb_infotesttype(IsActive);

-- InfoTestTypePrice
CREATE INDEX idx_info_test_type_id ON lin_lb_infotesttype_price(InfoTestTypeId);
CREATE INDEX idx_city_id ON lin_lb_infotesttype_price(CityId);
CREATE INDEX idx_is_active_price ON lin_lb_infotesttype_price(IsActive);

-- SampleType
CREATE INDEX idx_sample_type_active ON lin_lb_sampletype(IsActive);
```

---

## Migration Notes

### 无需数据库迁移

本功能**不涉及数据库结构变更**，所有改动都是：
1. 应用层验证逻辑增强（Age字段）
2. 业务逻辑优化（ReportNO生成、SampleStatus设置）
3. 前端UI交互优化（年龄选择器、分页加载、样本类型选择器）

### 数据完整性考虑

1. **历史数据处理**: 现有订单的Age字段如果格式不符合新规则，需要在查询时兼容处理
2. **价格数据迁移**: 确保InfoTestTypePrice表中的价格数据完整，IsActive字段正确设置
3. **测试数据准备**: 准备测试用的检测项目数据（至少50条）用于验证分页功能

---

**End of Data Model**
