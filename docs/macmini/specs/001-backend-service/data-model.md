# Data Model: PETLIS后端API服务

**Feature**: PETLIS后端API服务
**Date**: 2025-12-27
**Database**: MySQL 8.0+ (laboklin)

本文档定义PETLIS系统的JPA实体类映射,基于现有MySQL数据库表结构。

## 实体概览

系统包含以下核心实体:

1. **User** - 用户(对应lin_user表)
2. **InfoHospital** - 医院(对应lin_lb_infohospital表)
3. **InfoAnimal** - 动物物种(对应lin_lb_infoanimal表)
4. **InfoVariety** - 品种(对应lin_lb_infovariety表)
5. **DictSex** - 性别字典(对应lin_lb_dictsex表)
6. **InfoSampleType** - 样本类型(对应lin_lb_infosampletype表)
7. **InfoTestType** - 检测项目类型(对应lin_lb_infotesttype表)
8. **InfoTestTypePrice** - 检测项目价格(对应lin_lb_infotesttype_price表)
9. **ListDiagnosisProject** - 检测申请主表(对应lin_lb_listdiagnosisproject表)
10. **DiagnosisDetail** - 检测明细(对应lin_lb_diagnosisdetail表)
11. **DiagnosisDetailInfo** - 检测报告内容(对应lin_lb_diagnosisdetail_info表)
12. **City** - 城市(对应lin_lb_city表)

## 实体详细定义

### 1. User (用户)

**表名**: `lin_user`

**字段映射**:
- `id` (Integer, PK) - 用户ID,自增主键
- `username` (String) - 用户名
- `nickname` (String) - 昵称
- `avatar` (String) - 头像URL
- `mobile` (String) - 手机号
- `openid` (String) - 微信openid
- `hospitalId` (Integer, FK) - 关联医院ID,对应InfoHospital
- `status` (String) - 状态(normal/hidden)
- `createtime` (Integer) - 创建时间(Unix时间戳)
- `updatetime` (Integer) - 更新时间(Unix时间戳)

**关系**:
- ManyToOne → InfoHospital (通过hospitalId)

**JPA注解要点**:
```java
@Entity
@Table(name = "lin_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "openid", unique = true)
    private String openid;

    @Column(name = "hospital_id")
    private Integer hospitalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", insertable = false, updatable = false)
    private InfoHospital hospital;

    // 其他字段...
}
```

---

### 2. InfoHospital (医院)

**表名**: `lin_lb_infohospital`

**字段映射**:
- `infoHospitalId` (Integer, PK) - 医院ID
- `hospitalName` (String) - 医院名称
- `hospitalName_en` (String) - 医院英文名称
- `cityId` (Integer) - 城市ID
- `place` (String) - 医院地址
- `tel` (String) - 电话
- `fax` (String) - 传真
- `isActive` (Integer) - 启用状态(0=禁用,1=启用)
- `loginName` (String) - 登录名
- `loginPwd` (String) - 登录密码
- `amount` (BigDecimal) - 钱包金额
- `payType` (Integer) - 支付类型

**关系**:
- ManyToOne → City (通过cityId)
- OneToMany → User
- OneToMany → ListDiagnosisProject

---

### 3. InfoAnimal (动物物种)

**表名**: `lin_lb_infoanimal`

**字段映射**:
- `infoAnimalId` (Integer, PK) - 物种ID
- `animalName` (String) - 物种名称(中文)
- `animalName_en` (String) - 物种名称(英文)
- `isActive` (Integer) - 启用状态
- `createtime` (Integer) - 创建时间
- `updatetime` (Integer) - 更新时间

**关系**:
- OneToMany → InfoVariety

---

### 4. InfoVariety (品种)

**表名**: `lin_lb_infovariety`

**字段映射**:
- `iid` (Long, PK) - 品种ID (注意:字段名为iid,不是infoVarietyId)
- `variety_CN` (String) - 品种中文名
- `variety_EN` (String) - 品种英文名
- `createtime` (Integer) - 创建时间
- `updatetime` (Integer) - 更新时间

**关系**:
- ManyToOne → InfoAnimal (需要通过application逻辑关联,表中无外键字段)

---

### 5. DictSex (性别字典)

**表名**: `lin_lb_dictsex`

**字段映射**:
- `dictSexId` (Integer, PK) - 性别ID
- `sexName` (String) - 性别名称
- `sexName_en` (String) - 性别英文名称
- `isActive` (Integer) - 启用状态
- `createtime` (Integer) - 创建时间
- `updatetime` (Integer) - 更新时间

---

### 6. InfoSampleType (样本类型)

**表名**: `lin_lb_infosampletype`

**字段映射**:
- `infoSampleTypeId` (Integer, PK) - 样本类型ID
- `sampleName` (String) - 采样类型名称
- `sampleName_en` (String) - 采样类型英文名称
- `isActive` (Integer) - 启用状态
- `createtime` (Integer) - 创建时间
- `updatetime` (Integer) - 更新时间

---

### 7. InfoTestType (检测项目类型)

**表名**: `lin_lb_infotesttype`

**字段映射**:
- `infoTestTypeId` (Integer, PK) - 检测项目ID
- `testName` (String) - 检测项目名称
- `testNO` (String) - 检测编号
- `price` (Double) - 价格
- `ctoG` (Double) - 中国到德国费用
- `gtoC` (Double) - 德国到中国费用
- `discountReal` (Double) - 当前折扣
- `discountNext` (Double) - 待审折扣
- `textModel` (String) - 报告模板
- `createDt` (Date) - 创建日期
- `isActive` (Integer) - 启用状态
- `type` (Integer) - 类型(0=other, 1=细胞学&组织病理, 2=过敏原, 3=微生物, 4=综合)

**关系**:
- OneToMany → InfoTestTypePrice
- OneToMany → DiagnosisDetail

---

### 8. InfoTestTypePrice (检测项目价格)

**表名**: `lin_lb_infotesttype_price`

**字段映射**:
- `iid` (Long, PK) - 价格ID
- `infoTestTypeid` (Long) - 检测项目ID
- `price` (BigDecimal) - 价格
- `isActive` (Integer) - 启用状态
- `createtime` (Integer) - 创建时间
- `updatetime` (Integer) - 更新时间

**关系**:
- ManyToOne → InfoTestType (通过infoTestTypeid)

---

### 9. ListDiagnosisProject (检测申请主表)

**表名**: `lin_lb_listdiagnosisproject`

**字段映射**:
- `listDiagnosisProjectId` (Integer, PK) - 申请单ID
- `createDataTypeIId` (Long) - 数据组ID
- `userNO` (String) - 用户编号,格式"55550-"
- `cityId` (Integer) - 城市ID
- `infoHospitalId` (Integer) - 医院ID
- `infoAnimalId` (Integer) - 动物物种ID
- `infoSampleTypeId` (Integer) - 采样类型ID
- `animalName` (String) - 宠物姓名(中文)
- `animalName_en` (String) - 宠物姓名(英文)
- `animalTypeId` (Integer) - 动物品种ID (对应InfoVariety.iid)
- `animalType` (String) - 品种
- `animalType_en` (String) - 品种(英文)
- `animalWeight` (String) - 体重
- `chipNO` (String) - 芯片号
- `age` (String) - 年龄
- `birthday` (Date) - 出生日期
- `dictSexId` (Integer) - 性别ID
- `sampleArea` (String) - 采样部位
- `masterName` (String) - 畜主姓名(中文)
- `masterName_en` (String) - 畜主姓名(英文)
- `diagnosticResults` (String) - 病例诊断结果
- `collectDt` (Date) - 采样日期
- `receiveSampleDt` (Date) - 收到样本日期
- `sendSampleDt` (Date) - 寄出样本日期
- `reportNO` (String) - 报告编号
- `sendReportDt` (Date) - 报告发出日期
- `testFee` (Double) - 检测总费用
- `deliverySendGerFee` (Double) - 发送到德国费用
- `deliverySendCNFee` (Double) - 发送到中国费用
- `infoCheckReceivedId` (Integer) - 确认方式ID
- `saveMedium` (Integer) - 保存介质
- `isPayMoney` (Integer) - 是否已结算
- `remark` (String) - 备注
- `isSendNoticeMail` (Integer) - 是否已发送邮件通知
- `sampleStatus` (Integer) - 样本状态(-2=草稿, -1=全部, 0=待审核, 1=审核未通过, 2=等待报告, 3=已完成, 4=已取消)
- `createDt` (Date) - 创建时间
- `doctorName` (String) - 送检医师姓名
- `marketingName` (String) - 市场活动名称
- `createSampleId` (Integer) - 采样人ID

**关系**:
- ManyToOne → InfoHospital
- ManyToOne → InfoAnimal
- ManyToOne → DictSex
- OneToMany → DiagnosisDetail

---

### 10. DiagnosisDetail (检测明细)

**表名**: `lin_lb_diagnosisdetail`

**字段映射**:
- `diagnosisDetailId` (Integer, PK) - 明细ID
- `listDiagnosisProjectId` (Integer, FK) - 关联申请单ID
- `genterNO` (String) - 生成编号
- `infoTestTypeId` (Integer) - 检测项目ID
- `infoTestTypeModelId` (Integer) - 检测类型模型ID
- `mailTitle` (String) - 邮件标题
- `createDt` (Date) - 创建时间
- `isChangeModelTemple` (Integer) - 是否已修改模板
- `isChangeMailModelTemple` (Integer) - 是否已修改邮件模板
- `isSendMail` (Integer) - 是否已发送邮件
- `realFee` (Double) - 实际费用
- `isCheckPass` (Integer) - 是否作废(0=有效,1=作废)
- `pdfHttpUrl` (String) - PDF报告链接
- `testPrice` (BigDecimal) - 检测价格
- `orginalPrice` (BigDecimal) - 原始价格
- `orginalDiscount` (BigDecimal) - 原始折扣
- `sendMailDt` (Date) - 发送邮件时间
- `detailRemark` (String) - 详细备注
- `payStatus2` (Integer) - 支付状态(0=未结, 1=现金, 2=转账, 3=网银已支付, 4=忽略)
- `diagnosisStatus` (Integer) - 诊断状态(0=待检测)
- `isActive` (Integer) - 是否激活
- `isInvoiced` (Integer) - 是否已开发票

**关系**:
- ManyToOne → ListDiagnosisProject
- ManyToOne → InfoTestType
- OneToOne → DiagnosisDetailInfo

---

### 11. DiagnosisDetailInfo (检测报告内容)

**表名**: `lin_lb_diagnosisdetail_info`

**字段映射**:
- `diagnosisdetail_id` (Integer, PK) - 关联检测明细ID
- `mailReport` (String) - 邮件报告内容
- `reportResult` (String) - 报告结果

**关系**:
- OneToOne → DiagnosisDetail

---

### 12. City (城市)

**表名**: `lin_lb_city`

**字段映射**:
- `cityId` (Integer, PK) - 城市ID
- `cityName` (String) - 城市名称
- `areaCode` (Integer) - 区号
- `grade` (String) - 级别
- `zipCode` (Integer) - 邮编
- `belongCity_cityId` (Integer) - 所属城市ID
- `isActive` (Integer) - 启用状态

---

## 实体关系图 (ERD)

```
User (1) ----< (*) ListDiagnosisProject
    |
    +-----> InfoHospital (1)
                |
                +----< (*) ListDiagnosisProject

InfoAnimal (1) ----< (*) InfoVariety
    |
    +----< (*) ListDiagnosisProject

DictSex (1) ----< (*) ListDiagnosisProject

InfoTestType (1) ----< (*) InfoTestTypePrice
    |
    +----< (*) DiagnosisDetail

ListDiagnosisProject (1) ----< (*) DiagnosisDetail
                                    |
                                    +---- (1) DiagnosisDetailInfo
```

---

## 数据访问策略

### 1. 基础Repository接口

���有Repository继承自`JpaRepository<Entity, ID>`和`JpaSpecificationExecutor<Entity>`

```java
public interface UserRepository extends
    JpaRepository<User, Integer>,
    JpaSpecificationExecutor<User> {

    Optional<User> findByOpenid(String openid);

    @Query("SELECT u FROM User u WHERE u.hospitalId = :hospitalId AND u.status = 'normal'")
    List<User> findByHospitalId(@Param("hospitalId") Integer hospitalId);
}
```

### 2. 关联查询策略

- **默认**: 使用LAZY加载,避免N+1问题
- **优化**: 使用@EntityGraph动态加载关联
- **复杂查询**: 使用JPQL或原生SQL

### 3. 逻辑删除

所有包含`isActive`字段的实体,查询时默认过滤`isActive = 0`的记录:

```java
@Where(clause = "is_active = 1")
```

或使用@SQLDelete:

```java
@SQLDelete(sql = "UPDATE lin_lb_infoanimal SET is_active = 0 WHERE info_animal_id = ?")
```

---

## 索引策略

基于常用查询字段建立索引:

- **User**: openid (UNIQUE), hospital_id, status
- **ListDiagnosisProject**: info_hospital_id, sample_status, create_dt
- **DiagnosisDetail**: list_diagnosis_project_id, info_test_type_id, is_check_pass
- **InfoTestType**: test_name, is_active

---

## 数据验证约束

### 1. 必填字段 (应用层验证)

- **ListDiagnosisProject**:
  - animalName, animalName_en (NotBlank)
  - infoAnimalId, dictSexId (NotNull)
  - birthday (NotNull, 狂犬检测时)
  - chipNO (NotBlank, 狂犬检测时)
  - masterName, masterName_en (NotBlank)

- **DiagnosisDetail**:
  - infoTestTypeId (NotNull)
  - detailRemark (NotBlank, 狂犬检测时)

### 2. 字段长度限制

- 字符串字段使用@Column(length = N)限制长度
- 防止数据截断: varchar(50), varchar(100), varchar(250)

### 3. 值域验证

- sampleStatus: -2, -1, 0, 1, 2, 3, 4
- dictSexId: 0, 1, 2, 3
- payStatus2: 0, 1, 2, 3, 4
- isActive: 0, 1

---

## 数据库视图使用

系统包含7个视图,可在Repository中使用原生SQL查询:

```java
@Query(value = "SELECT * FROM vwdiagnosisdetail WHERE list_diagnosis_project_id = :projectId",
       nativeQuery = true)
List<Object[]> findDiagnosisDetailView(@Param("projectId") Integer projectId);
```

常用视图:
- **vwdiagnosisdetail**: 检测详情视图(48个字段)
- **vwlistdiagnosisproject**: 检测项目列表视图(46个字段)
- **vw_diagnosisdetail_all**: 检测详情综合视图(50个字段)

---

## 实体类命名约定

**重要**: 注意数据库表命名不统一:

- 有些表用全小写+下划线: `lin_user`
- 有些表用`lin_lb_`前缀+驼峰: `lin_lb_infoanimal`
- 主键字段命名不统一:
  - 大部分: `id`或`{TableName}Id`
  - InfoVariety: `iid`
  - City: `cityId`
- 创建时间字段不统一:
  - 大部分: `createtime` (Integer, Unix时间戳)
  - 检测相关: `createDt` (Date, 日期时间)

**解决策略**:
- 使用@Column注解明确映射
- 在BaseEntity中统一时间转换逻辑
- 使用@NamedQuery处理复杂查询

---

## 下一步

基于此数据模型,接下来将:
1. 创建API契约文档 (contracts/)
2. 生成任务列表 (tasks.md)
