# Data Model: 宠物医院检验系统

## 核心实体设计

### 1. 用户实体 (User)

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;           // 微信openid/手机号

    @Column(nullable = false)
    private String name;             // 用户姓名

    @Column(nullable = false)
    private String phone;            // 手机号码

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;           // 用户角色

    @Enumerated(EnumType.STRING)
    private LoginType loginType;      // 登录方式

    @Column
    private String avatar;           // 头像URL

    @Column
    private String position;         // 职位

    @Column
    private Boolean isActive = true;  // 账户状态

    @Column
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;

    // 关联医院
    @ManyToOne
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;
}
```

### 2. 医院实体 (Hospital)

```java
@Entity
@Table(name = "hospitals")
public class Hospital {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String hospitalCode;     // 医院编码

    @Column(nullable = false)
    private String name;             // 医院名称

    @Column
    private String address;          // 地址

    @Column
    private String contactPhone;     // 联系电话

    @Column
    private String licenseNumber;    // 营业执照号

    @Enumerated(EnumType.STRING)
    private HospitalStatus status;   // 医院状态

    @Column
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;
}
```

### 3. 宠物信息实体 (Pet)

```java
@Entity
@Table(name = "pets")
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;             // 宠物姓名

    @Enumerated(EnumType.STRING)
    private PetType type;            // 宠物类型

    @Column
    private String breed;            // 品种

    @Column
    private Integer age;             // 年龄

    @Enumerated(EnumType.STRING)
    private Gender gender;           // 性别

    @Column
    private Double weight;           // 体重

    @Column
    private String medicalHistory;   // 病史

    @Column
    private String allergies;         // 过敏史

    @Column
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;

    // 关联医院
    @ManyToOne
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;
}
```

### 4. 检验项目实体 (TestItem)

```java
@Entity
@Table(name = "test_items")
public class TestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;             // 项目编码

    @Column(nullable = false)
    private String name;             // 项目名称

    @Enumerated(EnumType.STRING)
    private TestCategory category;    // 检验类别

    @Column
    private String method;           // 检验方法

    @Column
    private String referenceRange;   // 参考范围

    @Column(nullable = false)
    private BigDecimal price;        // 价格

    @Column
    private Integer estimatedHours;  // 预计完成时间(小时)

    @Column
    private String description;      // 项目描述

    @Column
    private Boolean isActive = true; // 是否启用

    @Column
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;
}
```

### 5. 检验申请实体 (TestApplication)

```java
@Entity
@Table(name = "test_applications")
public class TestApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String applicationNo;    // 申请编号

    @ManyToOne
    @JoinColumn(name = "pet_id")
    private Pet pet;                 // 关联宠物

    @ManyToOne
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;       // 关联医院

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;               // 申请用户

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private List<ApplicationItem> items; // 检验项目列表

    @Column
    private String expressNo;        // 快递单号

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status; // 申请状态

    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgency;    // 紧急程度

    @Column
    private BigDecimal totalPrice;   // 总价

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // 支付状态

    @Column
    private String remarks;          // 备注

    @Column
    private LocalDateTime submitTime; // 提交时间

    @Column
    private LocalDateTime updateTime;
}
```

### 6. 申请项目实体 (ApplicationItem)

```java
@Entity
@Table(name = "application_items")
public class ApplicationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private TestApplication application; // 关联申请

    @ManyToOne
    @JoinColumn(name = "test_item_id")
    private TestItem testItem;       // 检验项目

    @Column
    private BigDecimal price;        // 项目价格

    @Column
    private String remarks;          // 项目备注
}
```

### 7. 检验报告实体 (Report)

```java
@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reportNo;         // 报告编号

    @ManyToOne
    @JoinColumn(name = "application_id")
    private TestApplication application; // 关联申请

    @Column
    private String filePath;         // 报告文件路径

    @Column
    private String fileType;         // 文件类型(PDF/IMAGE)

    @Column
    private String summary;          // 报告摘要

    @Column
    private String doctorAdvice;     // 医生建议

    @Column
    private String abnormalFindings; // 异常发现

    @Column
    private LocalDateTime generateTime; // 生成时间

    @ManyToOne
    @JoinColumn(name = "lab_user_id")
    private User labUser;            // 实验室操作员

    @Column
    private LocalDateTime updateTime;
}
```

### 8. 进度记录实体 (ProgressRecord)

```java
@Entity
@Table(name = "progress_records")
public class ProgressRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private TestApplication application; // 关联申请

    @Enumerated(EnumType.STRING)
    private ProgressStatus status;  // 进度状态

    @Column
    private String remarks;          // 备注信息

    @ManyToOne
    @JoinColumn(name = "operator_id")
    private User operator;           // 操作人

    @Column
    private LocalDateTime createTime;
}
```

## 枚举定义

### 用户角色 (UserRole)
```java
public enum UserRole {
    NORMAL,      // 普通用户
    ADMIN,       // 管理员
    SUPER_ADMIN  // 超级管理员
}
```

### 登录方式 (LoginType)
```java
public enum LoginType {
    PHONE,       // 手机号
    WECHAT       // 微信
}
```

### 医院状态 (HospitalStatus)
```java
public enum HospitalStatus {
    ACTIVE,      // 正常
    SUSPENDED,   // 暂停
    CLOSED       // 关闭
}
```

### 宠物类型 (PetType)
```java
public enum PetType {
    DOG,         // 狗
    CAT,         // 猫
    RABBIT,      // 兔子
    HAMSTER,     // 仓鼠
    OTHER        // 其他
}
```

### 检验类别 (TestCategory)
```java
public enum TestCategory {
    BLOOD,       // 血液检验
    URINE,       // 尿液检验
    PATHOLOGY,   // 病理检验
    IMAGING,     // 影像检验
    OTHER        // 其他
}
```

### 申请状态 (ApplicationStatus)
```java
public enum ApplicationStatus {
    DRAFT,       // 草稿
    SUBMITTED,   // 已提交
    SHIPPED,     // 已寄送
    RECEIVED,    // 已接收
    TESTING,     // 检验中
    COMPLETED,   // 已完成
    CANCELLED     // 已取消
}
```

### 紧急程度 (UrgencyLevel)
```java
public enum UrgencyLevel {
    NORMAL,      // 普通
    URGENT,      // 紧急
    EMERGENCY    // 急诊
}
```

### 支付状态 (PaymentStatus)
```java
public enum PaymentStatus {
    UNPAID,      // 未支付
    PAID,        // 已支付
    REFUNDED     // 已退款
}
```

### 进度状态 (ProgressStatus)
```java
public enum ProgressStatus {
    SUBMITTED,   // 已提交
    SHIPPED,     // 已寄送
    RECEIVED,    // 已接收
    TESTING,     // 检验中
    REVIEWING,   // 审核中
    COMPLETED,   // 已完成
    EXCEPTION    // 异常
}
```

## 数据关系设计

### 主要关系
1. **Hospital** 1:N **User** - 一个医院有多个用户
2. **Hospital** 1:N **Pet** - 一个医院有多个宠物
3. **User** 1:N **TestApplication** - 一个用户提交多个申请
4. **Pet** 1:N **TestApplication** - 一个宠物有多个检验申请
5. **TestApplication** N:N **TestItem** - 申请包含多个检验项目
6. **TestApplication** 1:1 **Report** - 一个申请对应一个报告
7. **TestApplication** 1:N **ProgressRecord** - 申请有多个进度记录

### 索引设计
```sql
-- 用户表索引
CREATE INDEX idx_user_phone ON users(phone);
CREATE INDEX idx_user_hospital ON users(hospital_id);

-- 申请表索引
CREATE INDEX idx_application_no ON test_applications(application_no);
CREATE INDEX idx_application_user ON test_applications(user_id);
CREATE INDEX idx_application_status ON test_applications(status);
CREATE INDEX idx_application_submit_time ON test_applications(submit_time);

-- 报告表索引
CREATE INDEX idx_report_no ON reports(report_no);
CREATE INDEX idx_report_application ON reports(application_id);

-- 进度记录索引
CREATE INDEX idx_progress_application ON progress_records(application_id);
CREATE INDEX idx_progress_create_time ON progress_records(create_time);
```

## 数据验证规则

### 输入验证
1. **手机号**: 11位数字，符合中国手机号格式
2. **宠物年龄**: 0-30之间的整数
3. **宠物体重**: 0.1-100.0之间的数值
4. **检验价格**: 大于0的数值，最多2位小数
5. **申请编号**: 唯一性约束，格式：VET+年月日+序号

### 业务规则
1. **用户权限**: 普通用户只能查看本医院的申请，管理员可管理本院所有申请
2. **申请状态**: 状态流转必须符合业务逻辑，不能倒退
3. **支付状态**: 只有已支付的申请才能生成报告
4. **报告生成**: 申请完成后才能生成报告，一个申请只能有一个报告
5. **数据删除**: 采用软删除，重要数据不物理删除