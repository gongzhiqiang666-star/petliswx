# Laboklin送检流程字段详细清单

## 文档说明
本文档基于Laboklin系统源码分析，精确列出送检流程中涉及的所有字段，并标注每个字段在送检流程中的使用步骤，确保与现有系统属性完全一致。

---

## 一、主表：lb_listdiagnosisproject（送检项目主表）

### 1.1 基础信息字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `ListDiagnosisProjectId` | int(11) | 自增ID | 步骤1创建 | 主键ID，系统自动生成 |
| `UserNO` | varchar(50) | "55550-" | 步骤1创建 | 用户编号，系统自动生成前缀 |
| `CreateDataTypeIId` | int(11) | 数据组ID | 步骤1创建 | 数据分组ID，从用户权限获取 |
| `InfoHospitalId` | int(11) | 0 | 步骤1创建 | 医院ID，从登录信息获取（前端提交后会被重置为0，实际通过权限验证） |
| `SampleStatus` | tinyint(1) | -2 | 步骤1创建/步骤3更新 | 样本状态：**值域**: -2(草稿), -1(全部), 0(待审核), 1(审核未通过), 2(等待报告), 3(已完成), 4(已取消) |
| `CreateDt` | datetime | 当前时间 | 步骤1创建 | 创建时间，自动写入 |
| `CityId` | int(11) | 0 | 步骤1创建 | 城市ID，用于价格计算和地区管理 |
| `MarketingName` | varchar(100) | "" | 步骤1创建 | 市场活动或合作公司名称 |

### 1.2 动物基本信息字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `InfoAnimalId` | int(11) | 0 | 步骤1填写 | 动物物种ID，**默认值为4(乌龟)** |
| `AnimalTypeId` | int(11) | 0 | 步骤1填写 | 动物品种ID，关联lb_infovariety表（主键IID） |
| `AnimalName` | varchar(100) | "" | 步骤1填写 | 动物中文名，必填 |
| `AnimalName_en` | varchar(100) | "" | 步骤1填写 | 动物英文名，必填（国际订单使用） |
| `AnimalType` | int(11) | 0 | 步骤1创建 | 动物类型（冗余字段，系统使用） |
| `AnimalType_en` | varchar(100) | "" | 步骤1创建 | 动物类型英文名（冗余字段，系统使用） |
| `AnimalWeight` | varchar(50) | "" | 步骤1创建 | 动物体重，可选 |
| `DictSexId` | int(11) | 0 | 步骤1填写 | 性别ID，**值域**: 0(未知), 1(公), 2(母), 3(其他) |
| `Age` | varchar(50) | "" | 步骤1填写 | 动物年龄，格式如"3岁2个月"，默认为"- 岁 - 个月"时会被清空 |
| `ChipNO` | varchar(50) | "" | 步骤1填写 | 芯片号，狂犬检测必填 |
| `Birthday` | date | null | 步骤1填写 | 出生日期，狂犬检测必填 |

### 1.3 采样信息字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `InfoSampleTypeId` | int(11) | 0 | 步骤1填写 | 采样类型ID，**默认值为1**，关联lb_infosampletype表 |
| `SampleArea` | varchar(100) | "" | 步骤1填写 | 采样部位，必填 |
| `CollectDt` | date | null | 步骤1填写 | 采样日期，必填，默认为当前日期 |
| `CreateSampleId` | int(11) | 0 | 步骤1创建 | 采样人ID（系统使用） |
| `InfoCheckReceivedId` | int(11) | 0 | 步骤1创建 | 收样人ID（系统使用） |

### 1.4 主人信息字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `MasterName` | varchar(100) | "" | 步骤1填写 | 畜主中文名，必填 |
| `MasterName_en` | varchar(100) | "" | 步骤1填写 | 畜主英文名，必填（国际订单使用） |
| `DoctorName` | varchar(100) | "" | 步骤1填写 | 送检医师姓名，必填 |

### 1.5 物流和时间字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `ReceiveSampleDt` | date | "1900-01-01" | 步骤3/管理员设置 | 收到样本日期，未设置时为"1900-01-01" |
| `SendSampleDt` | date | "1900-01-01" | 步骤3/管理员设置 | 发送样本日期到德国，未设置时为"1900-01-01" |
| `SendReportDt` | date | "1900-01-01" | 步骤3/管理员设置 | 发送报告日期，未设置时为"1900-01-01"，发送邮件后自动更新 |
| `ReportNO` | varchar(50) | "" | 步骤3填写 | 德国报告编号，格式通常为"RAB"开头 |

### 1.6 费用和支付字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `TestFee` | decimal(10,2) | 0.00 | 步骤2/3计算 | 检测总费用，自动计算所有检测项价格 |
| `IsPayMoney` | tinyint(1) | 0 | 步骤3设置 | 是否已结算（0:未结算,1:已结算） |
| `DeliverySendGerFee` | decimal(10,2) | 0 | 步骤1创建 | 发送到德国的费用（系统使用） |
| `DeliverySendCNFee` | decimal(10,2) | 0 | 步骤1创建 | 发送到中国的费用（系统使用） |

### 1.7 报告和通知字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `IsSendNoticeMail` | tinyint(1) | 0 | 步骤3设置 | 是否已发送邮件通知（0:否,1:是） |
| `SaveMedium` | varchar(50) | "" | 步骤1创建 | 保存介质（系统使用） |

### 1.8 备注字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `Remark` | text | "" | 步骤1填写 | 备注，狂犬检测必须填写快递单号 |

---

## 二、明细表：lb_diagnosisdetail（检测明细表）

### 2.1 基础信息字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `DiagnosisDetailId` | int(11) | 自增ID | 步骤2创建 | 主键ID，系统自动生成 |
| `ListDiagnosisProjectId` | int(11) | 关联主表ID | 步骤2创建 | 关联lb_listdiagnosisproject表主键 |
| `InfoTestTypeId` | int(11) | 检测项ID | 步骤2选择 | 检测类型ID，关联lb_infotesttype表 |
| `GenterNO` | varchar(50) | 项目ID + "-" + 序号 | 步骤2创建 | 生成编号，格式如"12345-1" |
| `IsActive` | tinyint(1) | 1 | 步骤2创建 | 是否激活（0:否,1:是） |
| `IsCheckPass` | tinyint(1) | 0 | 步骤2创建/更新 | 是否作废（0:有效,1:作废） |

### 2.2 价格和折扣字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `OrginalPrice` | decimal(10,2) | 检测项价格 | 步骤2设置 | 原始价格，从lb_infotesttype_price表获取 |
| `TestPrice` | decimal(10,2) | 检测项价格 | 步骤2设置 | 实际价格，通常等于原始价格 |
| `OrginalDiscount` | tinyint(2) | 10 | 步骤2设置 | 折扣比例（10表示原价，8表示8折） |
| `DiscountReal` | decimal(10,2) | 0 | 步骤1创建 | 实际折扣（系统使用） |
| `DiscountNext` | decimal(10,2) | 0 | 步骤1创建 | 下次折扣（系统使用） |

### 2.3 支付状态字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `PayStatus2` | tinyint(1) | 0 | 步骤2创建/更新 | 支付状态，**值域**: 0(未结), 1(现金), 2(转账), 3(网银已支付), 4(忽略) |

### 2.4 报告和邮件字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `ReportResult` | text | "" | 管理员填写 | 报告结果，检测完成后填写 |
| `MailReport` | text | "" | 步骤3设置 | 邮件报告正文 |
| `MailTitle` | varchar(255) | "" | 步骤3设置 | 邮件标题 |
| `DictMailDiagnosisReportId` | int(11) | 1 | 步骤3设置 | 邮件报告模板ID，关联lb_dictmaildiagnosisreport表 |
| `InfoTestTypeModelId` | int(11) | 0 | 步骤3设置 | 检测类型模型ID，关联lb_infotesttypemodel表 |
| `IsSendMail` | tinyint(1) | 0 | 步骤3设置 | 是否已发送邮件（0:否,1:是） |
| `SendMailDt` | datetime | "1900-01-01 00:00:00" | 步骤3设置 | 发送邮件时间，发送后自动更新 |
| `IsChangeModelTemple` | tinyint(1) | 1 | 步骤2创建 | 是否更改模板（系统使用） |
| `IsChangeMailModelTemple` | tinyint(1) | 1 | 步骤2创建 | 是否更改邮件模板（系统使用） |
| `PDFHttpUrl` | varchar(255) | "" | 步骤3生成 | PDF报告链接，检测完成后生成 |
| `TextModel` | text | "" | 步骤1创建 | 文本模型（系统使用） |

### 2.5 状态和时间字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `DiagnosisStatus` | tinyint(1) | 0 | 步骤2创建 | 诊断状态（0:待检测） |
| `CreateDt` | datetime | 当前时间 | 步骤2创建 | 创建时间，自动写入 |

### 2.6 备注字段

| 字段名 | 数据类型 | 默认值 | 使用步骤 | 说明 |
|--------|----------|--------|----------|------|
| `DetailRemark` | text | "" | 步骤2填写 | 详细备注，狂犬检测（InfoTestTypeId=1225）必填快递单号 |

---

## 三、送检流程步骤与字段对应关系

### 3.1 第一步：宠物基本信息（SendPro1）

#### 填写字段：
- **动物信息**：
  - `InfoAnimalId` - 动物物种选择（默认4-乌龟）
  - `AnimalTypeId` - 动物品种选择
  - `AnimalName` - 宠物中文名（必填）
  - `AnimalName_en` - 宠物英文名（必填）
  - `DictSexId` - 性别选择（值域：0,1,2,3）
  - `Age` - 年龄填写（格式：X岁X月）
  - `ChipNO` - 芯片号（狂犬检测必填）
  - `Birthday` - 出生日期（狂犬检测必填）

- **主人信息**：
  - `MasterName` - 畜主中文名（必填）
  - `MasterName_en` - 畜主英文名（必填）
  - `DoctorName` - 送检医师姓名（必填）

- **采样信息**：
  - `InfoSampleTypeId` - 采样类型选择（默认1）
  - `SampleArea` - 采样部位（必填）
  - `CollectDt` - 采样日期（必填，默认当前日期）

- **其他信息**：
  - `CityId` - 城市选择
  - `Remark` - 备注（狂犬检测必填快递单号）

#### 系统自动创建字段：
- `ListDiagnosisProjectId` - 主键ID
- `UserNO` - 用户编号前缀"55550-"
- `SampleStatus` - 设置为-2（草稿状态）
- `CreateDt` - 当前时间
- `MarketingName` - 市场活动名称
- 所有其他字段的默认值

### 3.2 第二步：选择检测项目（SendPro2）

#### 操作内容：
- **选择检测项**：每个检测项创建一条lb_diagnosisdetail记录
  - `ListDiagnosisProjectId` - 关联主订单
  - `InfoTestTypeId` - 检测项ID
  - `GenterNO` - 生成编号（格式：订单ID-序号）
  - `OrginalPrice` - 从价格表获取原价
  - `TestPrice` - 实际价格
  - `OrginalDiscount` - 默认10（原价）
  - `IsActive` - 1（激活）
  - `IsCheckPass` - 0（有效）
  - 所有其他字段的默认值

#### 特殊验证：
- 狂犬检测（InfoTestTypeId=1225）：
  - 必须验证主表的`ChipNO`已填写
  - 必须验证主表的`Birthday`已填写
  - 必须填写`DetailRemark`（快递单号）
  - 验证代码位置：lb_diagnosisdetail模型的beforeInsert回调
  - 错误提示："备注不能为空,请填写备注(请检查是否因为选择了狂犬检测项,如果是请填写快递单号)"

### 3.3 第三步：预览并提交（SendPro3）

#### 操作内容：
- **更新主表状态**：
  - `SampleStatus` - 从-2更新为0（待审核）
  - `ReportNO` - 生成德国报告编号（通常以"RAB"开头）

- **提交处理**：
  - 清除session中的project_id
  - 触发通知给实验室

### 3.4 管理员处理阶段

#### 审核阶段：
- **审核通过**：`SampleStatus` = 2
- **审核不通过**：`SampleStatus` = 1

#### 检测阶段：
- **收到样本**：更新`ReceiveSampleDt`
- **发送样本**：更新`SendSampleDt`

#### 完成阶段：
- **生成报告**：
  - 更新`ReportResult`
  - 生成`PDFHttpUrl`
  - 更新`SendReportDt`（发送邮件时自动更新）
  - 如果选择发送邮件，设置`IsSendNoticeMail`=1

---

## 四、特殊业务规则说明

### 4.1 狂犬检测特殊处理
- **检测项ID**: 1225
- **必填验证**：
  1. `ChipNO`（芯片号）
  2. `Birthday`（出生日期）
  3. `Remark`（备注）- 必须填写快递单号
  4. `DetailRemark`（详细备注）- 必填快递单号
- **验证代码位置**：lb_diagnosisdetail模型的beforeInsert回调（第70-72行）

### 4.2 价格计算逻辑
- 查询`lb_infotesttype_price`表获取价格
- 支持不同城市不同价格
- 默认折扣为10（表示原价）
- OrginalDiscount转换：字符串"原价"→10，"其它折扣"→-1

### 4.3 状态流转规则
```
-2 (草稿) → [用户提交] → 0 (待审核) → [审核通过] → 2 (等待报告) → [检测完成] → 3 (已完成)
                                      ↘ [审核不通过] → 1 (审核未通过)
                                      ↘ [取消订单] → 4 (已取消)
```

### 4.4 支付状态说明（PayStatus2）
通过DealPayMoneyShow函数处理：
- 0: 未结（显示红色）
- 1: 现金
- 2: 转账
- 3: 网银已支付
- 4: 忽略

### 4.5 月结账户特殊逻辑
- 医院`PayType`=1的为月结账户
- 需要在规定时间内结清账单才能查看报告
- 每月20日为结账点

### 4.6 字典表IsActive字段统一规则
所有字典表都包含IsActive字段：
- **IsActive = 1**：数据正常使用，在前端显示
- **IsActive = 0**：数据被禁用，在前端不显示

---

## 五、字段属性总结

### 5.1 必填字段汇总
**第一步必填**：
- `MasterName`（畜主姓名）
- `AnimalName`（动物姓名）
- `AnimalTypeId`（动物品种）
- `DictSexId`（动物性别）
- `CollectDt`（采样日期）
- `SampleArea`（采样部位）
- `DoctorName`（送检医师）
- 狂犬检测额外必填：`ChipNO`、`Birthday`、`Remark`

### 5.2 默认值汇总
- 日期字段默认："1900-01-01"（表示未设置）
- 数值字段默认：0
- 字符串字段默认：空字符串""
- 布尔字段默认：0（否/未选择）
- AnimalTypeId默认：0（选择时必填）
- DictSexId默认：0（未知）
- InfoSampleTypeId默认：1

### 5.3 特殊处理字段
- `UserNO`：自动生成前缀"55550-"
- `SampleStatus`：草稿状态为-2
- `GenterNO`：格式为"订单ID-序号"
- `PDFHttpUrl`：检测完成后自动生成
- `SendReportDt`：发送邮件时自动更新

### 5.4 字典表主键说明
- `lb_dictsex`: DictSexId
- `lb_infoanimal`: InfoAnimalId
- `lb_infovariety`: IID（不是InfoVarietyId）
- `lb_infosampletype`: InfoSampleTypeId
- `lb_infotesttype`: InfoTestTypeId
- `lb_infotesttype_price`: Id

---

## 六、开发注意事项

1. **字段验证**：严格按照必填字段进行前端验证
2. **状态管理**：确保SampleStatus正确流转
3. **特殊检测项**：狂犬检测必须验证三个必填项
4. **价格计算**：从lb_infotesttype_price表获取，注意城市因素
5. **权限控制**：医院只能操作自己的订单
6. **ID一致性**：注意lb_infovariety表使用IID作为主键，不是InfoVarietyId

---

## 更新日志
- **2024-01-14**: 根据源码创建完整字段清单
- **2024-01-14**: 标注每个字段的送检流程步骤
- **2024-01-14**: 补充特殊业务规则说明
- **2024-01-14**: **更新版** - 精确核实所有字典的值域范围和默认值