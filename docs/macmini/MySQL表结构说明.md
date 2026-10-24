# MySQL 表结构说明文档

基于 Untitled.sql 文件生成的完整表结构说明

## 目录

- [系统管理模块](#系统管理模块)
- [用户管理模块](#用户管理模块)
- [核心业务模块](#核心业务模块)
- [耗材管理模块](#耗材管理模块)
- [视图定义](#视图定义)

---

## 系统管理模块

### lin_admin - 管理员表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT | ID |
| username | varchar(20) | 用户名 |
| nickname | varchar(50) | 昵称 |
| password | varchar(32) | 密码 |
| salt | varchar(30) | 密码盐 |
| avatar | varchar(255) | 头像 |
| email | varchar(100) | 电子邮箱 |
| loginfailure | tinyint unsigned NOT NULL DEFAULT '0' | 失败次数 |
| logintime | int | 登录时间 |
| loginip | varchar(50) | 登录IP |
| createtime | int | 创建时间 |
| updatetime | int | 更新时间 |
| token | varchar(59) | Session标识 |
| datagroup | varchar(1000) | 数据分组 |
| status | varchar(30) NOT NULL DEFAULT 'normal' | 状态 |

**索引：**
- PRIMARY KEY (`id`)
- UNIQUE KEY `username` (`username`) USING BTREE

---

### lin_admin_log - 管理员日志表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT | ID |
| admin_id | int unsigned NOT NULL DEFAULT '0' | 管理员ID |
| username | varchar(30) | 管理员名字 |
| url | varchar(1500) | 操作页面 |
| title | varchar(100) | 日志标题 |
| content | text NOT NULL | 内容 |
| ip | varchar(50) | IP |
| useragent | varchar(255) | User-Agent |
| createtime | int | 操作时间 |

**索引：**
- PRIMARY KEY (`id`)
- KEY `name` (`username`)

---

### lin_area - 地区表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int NOT NULL AUTO_INCREMENT | ID |
| pid | int | 父id |
| shortname | varchar(100) | 简称 |
| name | varchar(100) | 名称 |
| mergename | varchar(255) | 全称 |
| level | tinyint | 层级 0 1 2 省市区县 |
| pinyin | varchar(100) | 拼音 |
| code | varchar(100) | 长途区号 |
| zip | varchar(100) | 邮编 |
| first | varchar(50) | 首字母 |
| lng | varchar(100) | 经度 |
| lat | varchar(100) | 纬度 |

**索引：**
- PRIMARY KEY (`id`)
- KEY `pid` (`pid`)

---

### lin_attachment - 附件表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT | ID |
| category | varchar(50) | 类别 |
| admin_id | int unsigned NOT NULL DEFAULT '0' | 管理员ID |
| user_id | int unsigned NOT NULL DEFAULT '0' | 会员ID |
| url | varchar(255) | 物理路径 |
| imagewidth | varchar(30) | 宽度 |
| imageheight | varchar(30) | 高度 |
| imagetype | varchar(30) | 图片类型 |
| imageframes | int unsigned NOT NULL DEFAULT '0' | 图片帧数 |
| filename | varchar(100) | 文件名称 |
| filesize | int unsigned NOT NULL DEFAULT '0' | 文件大小 |
| mimetype | varchar(100) | mime类型 |
| extparam | varchar(255) | 透传数据 |
| createtime | int | 创建日期 |
| updatetime | int | 更新时间 |
| uploadtime | int | 上传时间 |
| storage | varchar(100) NOT NULL DEFAULT 'local' | 存储位置 |
| sha1 | varchar(40) | 文件 sha1编码 |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_auth_group - 分组表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| pid | int unsigned NOT NULL DEFAULT '0' | 父组别 |
| name | varchar(100) | 组名 |
| rules | text NOT NULL | 规则ID |
| createtime | int | 创建时间 |
| updatetime | int | 更新时间 |
| status | varchar(30) | 状态 |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_auth_group_access - 权限分组表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| uid | int unsigned NOT NULL | 会员ID |
| group_id | int unsigned NOT NULL | 级别ID |

**索引：**
- UNIQUE KEY `uid_group_id` (`uid`,`group_id`)
- KEY `uid` (`uid`)
- KEY `group_id` (`group_id`)

---

### lin_auth_rule - 节点表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| type | enum('menu','file') NOT NULL DEFAULT 'file' | menu为菜单,file为权限节点 |
| pid | int unsigned NOT NULL DEFAULT '0' | 父ID |
| name | varchar(100) | 规则名称 |
| title | varchar(50) | 规则名称 |
| icon | varchar(50) | 图标 |
| url | varchar(255) | 规则URL |
| condition | varchar(255) | 条件 |
| remark | varchar(255) | 备注 |
| ismenu | tinyint unsigned NOT NULL DEFAULT '0' | 是否为菜单 |
| menutype | enum('addtabs','blank','dialog','ajax') | 菜单类型 |
| extend | varchar(255) | 扩展属性 |
| py | varchar(30) | 拼音首字母 |
| pinyin | varchar(100) | 拼音 |
| createtime | int | 创建时间 |
| updatetime | int | 更新时间 |
| weigh | int NOT NULL DEFAULT '0' | 权重 |
| status | varchar(30) | 状态 |

**索引：**
- PRIMARY KEY (`id`)
- UNIQUE KEY `name` (`name`) USING BTREE
- KEY `pid` (`pid`)
- KEY `weigh` (`weigh`)

---

### lin_category - 分类表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| pid | int unsigned NOT NULL DEFAULT '0' | 父ID |
| type | varchar(30) | 栏目类型 |
| name | varchar(30) |  |
| nickname | varchar(50) |  |
| flag | set('hot','index','recommend') |  |
| image | varchar(100) | 图片 |
| keywords | varchar(255) | 关键字 |
| description | varchar(255) | 描述 |
| diyname | varchar(30) | 自定义名称 |
| createtime | int | 创建时间 |
| updatetime | int | 更新时间 |
| weigh | int NOT NULL DEFAULT '0' | 权重 |
| status | varchar(30) | 状态 |

**索引：**
- PRIMARY KEY (`id`)
- KEY `weigh` (`weigh`,`id`)
- KEY `pid` (`pid`)

---

### lin_config - 系统配置
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| name | varchar(30) | 变量名 |
| group | varchar(30) | 分组 |
| title | varchar(100) | 变量标题 |
| tip | varchar(100) | 变量描述 |
| type | varchar(30) | 类型:string,text,int,bool,array,datetime,date,file |
| value | text | 变量值 |
| content | text | 变量字典数据 |
| rule | varchar(100) | 验证规则 |
| extend | varchar(255) | 扩展属性 |
| setting | varchar(255) | 配置 |

**索引：**
- PRIMARY KEY (`id`)
- UNIQUE KEY `name` (`name`)

---

### lin_ems - 邮箱验证码表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT | ID |
| event | varchar(30) | 事件 |
| email | varchar(100) | 邮箱 |
| code | varchar(10) | 验证码 |
| times | int unsigned NOT NULL DEFAULT '0' | 验证次数 |
| ip | varchar(30) | IP |
| createtime | int | 创建时间 |

**索引：**
- PRIMARY KEY (`id`) USING BTREE

---

### lin_sms - 短信验证码表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT | ID |
| event | varchar(30) | 事件 |
| mobile | varchar(20) | 手机号 |
| code | varchar(10) | 验证码 |
| times | int unsigned NOT NULL DEFAULT '0' | 验证次数 |
| ip | varchar(30) | IP |
| createtime | int unsigned DEFAULT '0' | 创建时间 |

**索引：**
- PRIMARY KEY (`id`)

---

## 用户管理模块

### lin_user - 会员表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT | ID |
| group_id | int unsigned NOT NULL DEFAULT '0' | 组别ID |
| username | varchar(32) | 用户名 |
| nickname | varchar(50) | 昵称 |
| password | varchar(32) | 密码 |
| salt | varchar(30) | 密码盐 |
| email | varchar(100) | 电子邮箱 |
| mobile | varchar(11) | 手机号 |
| avatar | varchar(255) | 头像 |
| level | tinyint unsigned NOT NULL DEFAULT '0' | 等级 |
| gender | tinyint unsigned NOT NULL DEFAULT '0' | 性别 |
| birthday | date | 生日 |
| bio | varchar(100) | 格言 |
| money | decimal(10,2) NOT NULL DEFAULT '0.00' | 余额 |
| score | int NOT NULL DEFAULT '0' | 积分 |
| successions | int unsigned NOT NULL DEFAULT '1' | 连续登录天数 |
| maxsuccessions | int unsigned NOT NULL DEFAULT '1' | 最大连续登录天数 |
| prevtime | int | 上次登录时间 |
| logintime | int | 登录时间 |
| loginip | varchar(50) | 登录IP |
| loginfailure | tinyint unsigned NOT NULL DEFAULT '0' | 失败次数 |
| joinip | varchar(50) | 加入IP |
| jointime | int | 加入时间 |
| createtime | int | 创建时间 |
| updatetime | int | 更新时间 |
| token | varchar(50) | Token |
| status | varchar(30) | 状态 |
| verification | varchar(255) | 验证 |
| hospital_id | int unsigned DEFAULT '0' | 医院ID |

**索引：**
- PRIMARY KEY (`id`)
- KEY `username` (`username`)
- KEY `email` (`email`)
- KEY `mobile` (`mobile`)

---

### lin_user_group - 会员组表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| name | varchar(50) | 组名 |
| rules | text | 权限节点 |
| createtime | int | 添加时间 |
| updatetime | int | 更新时间 |
| status | enum('normal','hidden') | 状态 |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_user_money_log - 会员余额变动表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| user_id | int unsigned NOT NULL DEFAULT '0' | 会员ID |
| money | decimal(10,2) NOT NULL DEFAULT '0.00' | 变更余额 |
| before | decimal(10,2) NOT NULL DEFAULT '0.00' | 变更前余额 |
| after | decimal(10,2) NOT NULL DEFAULT '0.00' | 变更后余额 |
| memo | varchar(255) | 备注 |
| createtime | int | 创建时间 |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_user_rule - 会员规则表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| pid | int | 父ID |
| name | varchar(50) | 名称 |
| title | varchar(50) | 标题 |
| remark | varchar(100) | 备注 |
| ismenu | tinyint(1) | 是否菜单 |
| createtime | int | 创建时间 |
| updatetime | int | 更新时间 |
| weigh | int DEFAULT '0' | 权重 |
| status | enum('normal','hidden') | 状态 |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_user_score_log - 会员积分变动表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| user_id | int unsigned NOT NULL DEFAULT '0' | 会员ID |
| score | int NOT NULL DEFAULT '0' | 变更积分 |
| before | int NOT NULL DEFAULT '0' | 变更前积分 |
| after | int NOT NULL DEFAULT '0' | 变更后积分 |
| memo | varchar(255) | 备注 |
| createtime | int | 创建时间 |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_user_token - 会员Token表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| token | varchar(50) NOT NULL | Token |
| user_id | int unsigned NOT NULL DEFAULT '0' | 会员ID |
| createtime | int | 创建时间 |
| expiretime | int | 过期时间 |

**索引：**
- PRIMARY KEY (`token`)

---

### lin_version - 版本表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int NOT NULL AUTO_INCREMENT | ID |
| oldversion | varchar(30) | 旧版本号 |
| newversion | varchar(30) | 新版本号 |
| packagesize | varchar(30) | 包大小 |
| content | varchar(500) | 升级内容 |
| downloadurl | varchar(255) | 下载地址 |
| enforce | tinyint unsigned NOT NULL DEFAULT '0' | 强制更新 |
| createtime | int | 创建时间 |
| updatetime | int | 更新时间 |
| weigh | int NOT NULL DEFAULT '0' | 权重 |
| status | varchar(30) | 状态 |

**索引：**
- PRIMARY KEY (`id`) USING BTREE

---

## 核心业务模块 (lin_lb_)

### lin_lb_city - 城市表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| CityId | int unsigned NOT NULL AUTO_INCREMENT |  |
| CityName | varchar(50) NOT NULL |  |
| AreaCode | int |  |
| Grade | varchar(50) |  |
| ZipCode | int |  |
| BelongCity_cityId | int |  |
| IsActive | int NOT NULL |  |

**索引：**
- PRIMARY KEY (`CityId`)

---

### lin_lb_dictsex - 性别字典表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| DictSexId | int unsigned NOT NULL AUTO_INCREMENT |  |
| SexName | varchar(50) NOT NULL | 性别名称 |
| SexName_en | varchar(50) NOT NULL | 性别名称(英文) |
| IsActive | int NOT NULL |  |
| createtime | int | 创建时间 |
| updatetime | int | 更新时间 |

**索引：**
- PRIMARY KEY (`DictSexId`)

---

### lin_lb_infoanimal - 动物物种表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| InfoAnimalId | int unsigned NOT NULL AUTO_INCREMENT |  |
| AnimalName | varchar(250) NOT NULL | 物种名称 |
| AnimalName_en | varchar(250) NOT NULL | 物种名称(英文) |
| IsActive | int NOT NULL |  |
| createtime | int | 创建时间 |
| updatetime | int | 更新时间 |

**索引：**
- PRIMARY KEY (`InfoAnimalId`)

---

### lin_lb_infocheckreceived - 确认方式表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| InfoCheckReceivedId | int unsigned NOT NULL AUTO_INCREMENT |  |
| CheckType | varchar(250) NOT NULL | 确认方式 |
| IsActive | int NOT NULL |  |
| createtime | int | 添加时间 |
| updatetime | int | 更新时间 |

**索引：**
- PRIMARY KEY (`InfoCheckReceivedId`)

---

### lin_lb_infohospital - 医院信息表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| InfoHospitalId | int unsigned NOT NULL AUTO_INCREMENT |  |
| HospitalName | varchar(250) NOT NULL | 医院名称 |
| HospitalName_en | varchar(250) NOT NULL |  |
| Invoice | varchar(250) | 发票抬头 |
| CityId | int | 地区 |
| Place | text | 医院地址 |
| Adress | text |  |
| Tel | varchar(50) |  |
| Fax | varchar(50) |  |
| InfoHospitalContactId | int | 联系方式,InfoHospitalContactId子表 |
| CreateDt | datetime |  |
| IsActive | int |  |
| LoginName | varchar(50) | 登录名 |
| LoginPwd | varchar(50) | 登录密码 |
| amount | decimal(10,2) DEFAULT '0.00' | 钱包金额 |
| PayType | int NOT NULL |  |

**索引：**
- PRIMARY KEY (`InfoHospitalId`)

---

### lin_lb_infohospitalcontact - 医院联系人表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| InfoHospitalContactId | int unsigned NOT NULL AUTO_INCREMENT |  |
| InfoHospitalId | int NOT NULL | 所属医院,InfoHospitalId父表 |
| isFinanceContact | int NOT NULL |  |
| Name | varchar(250) NOT NULL | 姓名 |
| Tel | varchar(250) NOT NULL | 联系电话 |
| Mail | varchar(250) NOT NULL |  |
| Fax | varchar(250) NOT NULL |  |
| CreateDt | datetime NOT NULL |  |
| LoginName | varchar(50) DEFAULT '' | 登录名(未使用) |
| LoginPwd | varchar(50) DEFAULT '' | 登录密码(未使用) |
| ContactRemark | varchar(250) DEFAULT '' |  |

**索引：**
- PRIMARY KEY (`InfoHospitalContactId`)

---

### lin_lb_infosampletype - 样本类型表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| InfoSampleTypeId | int unsigned NOT NULL AUTO_INCREMENT |  |
| SampleName | varchar(250) NOT NULL | 采样类型 |
| SampleName_en | varchar(250) NOT NULL | 采样类型(英文) |
| IsActive | int NOT NULL |  |
| createtime | int | 添加时间 |
| updatetime | int | 更新时间 |

**索引：**
- PRIMARY KEY (`InfoSampleTypeId`)

---

### lin_lb_infotesttype - 检测项目类型表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| InfoTestTypeId | int unsigned NOT NULL AUTO_INCREMENT |  |
| TestName | varchar(250) NOT NULL | 检测项目名 |
| TestNO | varchar(250) NOT NULL | 检测编号 |
| Price | double NOT NULL |  |
| CtoG | double NOT NULL |  |
| GtoC | double NOT NULL |  |
| DiscountReal | double NOT NULL | 当前折扣 |
| DiscountNext | double NOT NULL | 待审折扣 |
| TextModel | longtext NOT NULL | 报告模板,不显示 |
| CreateDt | datetime NOT NULL |  |
| IsActive | int NOT NULL |  |
| type | tinyint unsigned DEFAULT '0' | 0 other 1细胞学&组织病理  2过敏原  3微生物 4 综合(其他项目) |
| updatetime | int unsigned |  |
| createtime | int unsigned |  |

**索引：**
- PRIMARY KEY (`InfoTestTypeId`)
- KEY `IX_InfoTestType` (`TestName`)

---

### lin_lb_infotesttype_price - 检测类型价格表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| IID | bigint unsigned NOT NULL AUTO_INCREMENT |  |
| InfoTestTypeid | bigint |  |
| price | decimal(18,2) NOT NULL |  |
| IsActive | int unsigned NOT NULL |  |
| createtime | int |  |
| updatetime | int |  |

**索引：**
- PRIMARY KEY (`IID`)

---

### lin_lb_infotesttypemodel - 报告模板表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| InfoTestTypeModelId | int unsigned NOT NULL AUTO_INCREMENT |  |
| ModelName | varchar(1000) NOT NULL | 模板标题,含德文 |
| ModelInfo | longtext NOT NULL | 模板内容,含德文 |
| InfoTestTypeId | int NOT NULL | 检查,InfoTestTypeId父表 |
| CreateDt | datetime NOT NULL |  |
| updatetime | int unsigned |  |

**索引：**
- PRIMARY KEY (`InfoTestTypeModelId`)

---

### lin_lb_infovariety - 品种信息表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| IID | bigint unsigned NOT NULL AUTO_INCREMENT |  |
| Variety_CN | varchar(50) NOT NULL |  |
| Variety_EN | varchar(50) NOT NULL |  |
| createtime | int | 添加时间 |
| updatetime | int | 更新时间 |

**索引：**
- PRIMARY KEY (`IID`) USING BTREE

---

### lin_lb_kuhuirightdatagroup - 数据分组表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| IID | bigint unsigned NOT NULL AUTO_INCREMENT |  |
| GroupName | varchar(150) NOT NULL |  |
| GroupRemark | varchar(150) NOT NULL |  |
| StartWord | varchar(50) NOT NULL |  |
| StartNO | varchar(50) NOT NULL |  |
| ParentIID | varchar(50) NOT NULL |  |
| CreateDt | datetime NOT NULL |  |
| UpdateDt | datetime NOT NULL |  |

**索引：**
- PRIMARY KEY (`IID`)
- KEY `IX_KuHuiRightDataGroup` (`GroupName`)

---

### lin_lb_listdiagnosisproject - 检测项目申请表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| ListDiagnosisProjectId | int unsigned NOT NULL AUTO_INCREMENT |  |
| CreateDataTypeIId | bigint NOT NULL |  |
| UserNO | varchar(50) NOT NULL | 人工编号 |
| CityId | int NOT NULL | 地域 |
| InfoHospitalId | int NOT NULL | 诊所医院名称 |
| InfoAnimalId | int NOT NULL | 物种 |
| InfoSampleTypeId | int NOT NULL | 采样方式 |
| AnimalName | varchar(250) NOT NULL | 宠物姓名 |
| AnimalName_en | varchar(250) NOT NULL | 宠物姓名(英文) |
| AnimalTypeId | int NOT NULL | 动物品种 |
| AnimalType | varchar(50) NOT NULL | 品种 |
| AnimalType_en | varchar(50) NOT NULL | 品种(英文) |
| AnimalWeight | varchar(50) NOT NULL |  |
| ChipNO | varchar(50) NOT NULL | 芯片号(狂犬检测必填) |
| Age | varchar(50) NOT NULL | 年龄 |
| Birthday | datetime | 动物生日(狂犬检测必填) |
| DictSexId | int NOT NULL | 性别 |
| SampleArea | varchar(50) NOT NULL | 采样部位 |
| MasterName | varchar(250) NOT NULL | 畜主姓名 |
| MasterName_en | varchar(250) NOT NULL | 畜主姓名(英文) |
| DiagnosticResults | varchar(255) DEFAULT '' | 病例诊断结果 |
| CollectDt | datetime | 采样日期 |
| ReceiveSampleDt | datetime NOT NULL | 收到样本日期 |
| SendSampleDt | datetime NOT NULL | 寄出样本日期 |
| ReportNO | varchar(50) NOT NULL | 报告编号(德国寄来的编号,带扫描枪) |
| SendReportDt | datetime NOT NULL | 报告发出日期 |
| TestFee | double NOT NULL |  |
| DeliverySendGerFee | double NOT NULL | 中国发送至德国的快递费 |
| DeliverySendCNFee | double NOT NULL | 德国发回上海的快递费 |
| InfoCheckReceivedId | int NOT NULL | 与医院确认回收的样本 |
| SaveMedium | int NOT NULL | 报告是否存入媒介,复选框 |
| IsPayMoney | int NOT NULL |  |
| Remark | text NOT NULL |  |
| IsSendNoticeMail | int NOT NULL |  |
| SampleStatus | int NOT NULL |  |
| CreateDt | datetime NOT NULL |  |
| DoctorName | varchar(100) NOT NULL DEFAULT '' |  |
| MarketingName | varchar(100) NOT NULL DEFAULT '' | 市场活动或合作公司 |
| CreateSampleId | int NOT NULL |  |

**索引：**
- PRIMARY KEY (`ListDiagnosisProjectId`)
- KEY `IX_ListDiagnosisProject` (`ReceiveSampleDt`)
- KEY `IX_ListDiagnosisProject_1` (`CreateDataTypeIId`)
- KEY `IX_ListDiagnosisProject_2` (`SendReportDt`)

---

### lin_lb_diagnosisdetail - 检测详情表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| DiagnosisDetailId | int unsigned NOT NULL AUTO_INCREMENT |  |
| ListDiagnosisProjectId | int NOT NULL | 所属项目,ListDiagnosisProjectId父表 |
| GenterNO | varchar(150) NOT NULL |  |
| InfoTestTypeId | int NOT NULL | 测试项 |
| InfoTestTypeModelId | int NOT NULL |  |
| MailTitle | varchar(250) NOT NULL |  |
| CreateDt | datetime NOT NULL |  |
| IsChangeModelTemple | int NOT NULL | 是否已修改了模板 |
| IsChangeMailModelTemple | int |  |
| IsSendMail | int NOT NULL | 是否已发送,复选框 |
| RealFee | double |  |
| IsCheckPass | int NOT NULL |  |
| PDFHttpUrl | varchar(255) NOT NULL |  |
| TestPrice | decimal(18,2) NOT NULL |  |
| OrginalPrice | decimal(18,2) NOT NULL |  |
| OrginalDiscount | decimal(4,2) |  |
| SendMailDt | datetime NOT NULL |  |
| DetailRemark | varchar(100) NOT NULL |  |
| PayStatus2 | int NOT NULL |  |
| DiagnosisStatus | int NOT NULL |  |
| IsActive | int NOT NULL |  |
| is_invoiced | tinyint NOT NULL DEFAULT '0' | 是否已开发票 |

**索引：**
- PRIMARY KEY (`DiagnosisDetailId`)
- KEY `IX_DiagnosisDetail` (`InfoTestTypeId`)
- KEY `IX_DiagnosisDetail_1` (`IsCheckPass`)
- KEY `IX_DiagnosisDetail_2` (`DiagnosisDetailId`)
- KEY `IX_DiagnosisDetail_3` (`ListDiagnosisProjectId`)

---

### lin_lb_diagnosisdetail_info - 检测报告内容表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| diagnosisdetail_id | int NOT NULL |  |
| MailReport | longtext |  |
| ReportResult | longtext |  |

**索引：**
- PRIMARY KEY (`diagnosisdetail_id`)

---

### lin_lb_filediagnosisproject - 检测相关文件表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| FileDiagnosisProjectId | int unsigned NOT NULL AUTO_INCREMENT |  |
| DiagnosisDetailId | int NOT NULL |  |
| FileName | varchar(1000) |  |
| Path | varchar(1000) NOT NULL |  |
| HttpUrl | varchar(1000) NOT NULL |  |
| CreateDt | datetime NOT NULL |  |

**索引：**
- PRIMARY KEY (`FileDiagnosisProjectId`)

---

### lin_lb_paylog - 支付日志表(支付宝)
| 字段名 | 类型 | 注释 |
|--------|------|------|
| PayLogId | int unsigned NOT NULL AUTO_INCREMENT |  |
| BuyMail | varchar(250) NOT NULL | 卖家账户 |
| BuyerId | varchar(250) NOT NULL | 卖家支付宝ID |
| TradeNoAli | varchar(50) NOT NULL | 报告编号 |
| TradeNoUser | varchar(50) NOT NULL | 样本编号 |
| TotalFee | decimal(18,2) NOT NULL | 交易金额 |
| TradeStatus | varchar(250) NOT NULL | 交易状态 |
| Subject | varchar(250) NOT NULL | 标题 |
| TradeBody | varchar(250) NOT NULL | 内容 |
| Exterface | varchar(250) NOT NULL | 扩展 |
| CreateDt | datetime NOT NULL |  |

**索引：**
- PRIMARY KEY (`PayLogId`)

---

### lin_lb_payorderdetail - 支付订单明细表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| IID | bigint unsigned NOT NULL AUTO_INCREMENT |  |
| MainOrderId | varchar(50) NOT NULL |  |
| UserNo | varchar(50) NOT NULL |  |
| DetailId | bigint NOT NULL |  |
| ShowMoney | decimal(18,4) NOT NULL |  |
| PayMoney | decimal(18,4) NOT NULL |  |
| CreateDt | datetime NOT NULL |  |
| UpdateDt | datetime NOT NULL |  |

**索引：**
- PRIMARY KEY (`IID`)

---

### lin_lb_payordermain - 支付主订单表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| IID | bigint unsigned NOT NULL AUTO_INCREMENT |  |
| OrderId | varchar(100) NOT NULL |  |
| Md5Id | varchar(100) NOT NULL |  |
| AliTradeId | varchar(200) NOT NULL |  |
| HostId | int NOT NULL |  |
| PayMonth | varchar(50) NOT NULL |  |
| CHKMoney | decimal(18,4) NOT NULL |  |
| CreateDt | datetime NOT NULL |  |
| UpdateDt | datetime NOT NULL |  |
| IsSuccess | int NOT NULL |  |
| Remark | text NOT NULL |  |

**索引：**
- PRIMARY KEY (`IID`)

---

### lin_lb_noticemail - 通知邮件表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| IID | bigint unsigned NOT NULL AUTO_INCREMENT |  |
| HospId | int NOT NULL |  |
| PayMonth | varchar(50) NOT NULL |  |
| IsSendMail | int NOT NULL |  |
| Remark | varchar(50) NOT NULL |  |
| CreateDt | datetime NOT NULL |  |
| UpdateDt | datetime NOT NULL |  |

**索引：**
- PRIMARY KEY (`IID`)

---

### lin_lb_dictbindkey - 绑定字段表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| BindTable | varchar(50) NOT NULL | 绑定模板 |
| ColumnName | varchar(50) NOT NULL | 数据库字段名 |
| DescName | varchar(50) NOT NULL | 描述名 |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_lb_dictfinancemailreport - 财务邮件报告模板表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| model_name | varchar(1000) NOT NULL | 模板名,含德文 |
| model_content | longtext NOT NULL | 模板内容,含德文 |
| IsActive | int NOT NULL |  |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_lb_dictfinancereport - 财务报告模板表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| model_name | varchar(1000) NOT NULL | 模板名称,含德文 |
| model_content | longtext NOT NULL | 模板内容,不显示,含德文 |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_lb_dictmaildiagnosisreport - 诊断报告邮件模板表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| model_type | varchar(1000) NOT NULL | 模板名,含德文 |
| model_content | longtext | 模板内容,含德文 |
| createtime | int unsigned |  |
| updatetime | int unsigned |  |

**索引：**
- PRIMARY KEY (`id`)

---

### lin_lb_dictmailreportnotice - 邮件通知模板表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | bigint unsigned NOT NULL AUTO_INCREMENT |  |
| tplname | varchar(1000) NOT NULL | 模板名,含德文 |
| tpltext | longtext NOT NULL | 模板内容,含德文 |
| is_active | int NOT NULL |  |
| createtime | int | 操作时间 |

**索引：**
- PRIMARY KEY (`id`) USING BTREE

---

### lin_lb_dictmailserver - 邮件服务器配置表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| DictMailServerId | int NOT NULL | 单行数据表 |
| ServerHost | varchar(250) NOT NULL | 服务器地址 |
| Port | int NOT NULL | 端口 |
| UserName | varchar(250) NOT NULL | 用户名 |
| Psw | varchar(250) NOT NULL | 密码 |
| BCCMail | text NOT NULL | 密送到邮箱 |

**索引：**
- PRIMARY KEY (`DictMailServerId`)

---

### lin_infohospital_money - 医院金额管理表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| id | int unsigned NOT NULL AUTO_INCREMENT |  |
| infohospital_id | int unsigned DEFAULT '0' | 医院ID |
| amount | decimal(10,2) | 金额负为扣钱 |
| remark | varchar(255) DEFAULT '' | 备注 |
| admin_id | int unsigned DEFAULT '0' | 管理员ID |
| createtime | int unsigned |  |
| updatetime | int unsigned |  |

**索引：**
- PRIMARY KEY (`id`)

---

## 耗材管理模块

### listsupplymaterial - 耗材信息表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| ListSupplyMaterialId | int NOT NULL |  |
| SupplyName | varchar(50) NOT NULL | 品名 |
| Specifications | varchar(50) NOT NULL | 规格 |
| Unit | varchar(50) NOT NULL | 单位 |
| Remark | longtext NOT NULL | 备注 |
| CreateDt | datetime NOT NULL |  |
| IsActive | int NOT NULL |  |

**索引：**
- PRIMARY KEY (`ListSupplyMaterialId`)

---

### listsupplyhistory - 耗材出入库历史表
| 字段名 | 类型 | 注释 |
|--------|------|------|
| ListSupplyHistoryId | int NOT NULL |  |
| ListSupplyMaterialId | int NOT NULL | 耗材名称 |
| InfoHospitalId | int NOT NULL | 医院名称 |
| DictOutTypeId | int NOT NULL | 进出类型,(0=出,1=进) |
| Count | int NOT NULL | 数量 |
| CreateDt | datetime NOT NULL |  |
| IsActive | int NOT NULL |  |
| IsPay | int NOT NULL | 是否已付款 |

**索引：**
- PRIMARY KEY (`ListSupplyHistoryId`)

---

## 视图定义

数据库包含以下7个视图：

1. **vw_diagnosisdetail_all** - 检测详情综合视图（50个字段）
2. **vw_diagnosisdetail_all2** - 检测详情综合视图第二版
3. **vw_hospmonthreport** - 医院月度报告视图
4. **vw_hospmonthreport3_sendmaildt** - 医院月度报告发送邮件时间视图
5. **vw_hospmonthreport3_sendmaildt_copy** - 医院月度报告发送邮件时间视图副本
6. **vwdiagnosisdetail** - 检测详情视图（48个字段）
7. **vwlistdiagnosisproject** - 检测项目列表视图（46个字段）

---

## 数据库统计

- **总表数**: 47个
- **视图数**: 7个
- **系统管理表**: 13个（lin_开头）
- **业务表**: 30个（lin_lb_开头）
- **耗材管理表**: 2个（无前缀）
- **其他表**: 2个（lin_infohospital_money, lin_version）

---

## 字符集和排序规则

- 主要字符集：`utf8mb4` 和 `utf8mb3`
- 主要排序规则：`utf8mb4_unicode_ci` 和 `utf8mb3_general_ci`
- 支持中文、英文、德文多语言存储

---

文档生成时间：2025-12-14
基于文件：Untitled.sql
数据库版本：MySQL 8.0.44