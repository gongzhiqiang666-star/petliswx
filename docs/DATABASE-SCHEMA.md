# laboklin 数据库表结构清单

- 来源：本机真实 MySQL `laboklin`（127.0.0.1:13306）
- 数据表：88 张；视图：7 个；触发器：1 个
- **原有数据表：72 张；本次新增工作流表：16 张**
- 说明：仅读取 information_schema，不包含业务数据。

## 新增与原有划分

**本次新增的 16 张表：**

`lin_user_role`、`lin_shipment`、`lin_lab_task`、`lin_test_result`、`lin_report_workflow`、`lin_report_template`、`lin_order_clinical`、`lin_order_idempotency`、`lin_order_pricing_snapshot`、`lin_contract`、`lin_contract_item_price`、`lin_monthly_bill`、`lin_monthly_bill_item`、`lin_file`、`lin_audit_record`、`lin_operational_config`。

**其余 72 张数据表均为原有表。** 下方“表清单”的“分类”列也逐表标注了“既有”或“新增工作流”；“既有”即原有表。

## 表清单

| 表名 | 分类 | 字段数 |
|---|---|---:|
| `flyway_schema_history` | 既有 | 10 |
| `lin_admin` | 既有 | 15 |
| `lin_admin_log` | 既有 | 9 |
| `lin_area` | 既有 | 12 |
| `lin_article` | 既有 | 14 |
| `lin_article_action` | 既有 | 5 |
| `lin_attachment` | 既有 | 18 |
| `lin_audit_record` | 新增工作流 | 14 |
| `lin_auth_group` | 既有 | 7 |
| `lin_auth_group_access` | 既有 | 2 |
| `lin_auth_rule` | 既有 | 18 |
| `lin_category` | 既有 | 14 |
| `lin_config` | 既有 | 11 |
| `lin_contract` | 新增工作流 | 18 |
| `lin_contract_item_price` | 新增工作流 | 16 |
| `lin_coupon` | 既有 | 9 |
| `lin_doctor_profile` | 既有 | 13 |
| `lin_ems` | 既有 | 7 |
| `lin_file` | 新增工作流 | 15 |
| `lin_gift` | 既有 | 9 |
| `lin_infohospital_money` | 既有 | 7 |
| `lin_invoice_application` | 既有 | 10 |
| `lin_invoice_order` | 既有 | 2 |
| `lin_invoice_profile` | 既有 | 11 |
| `lin_lab_task` | 新增工作流 | 20 |
| `lin_lab_test` | 既有 | 4 |
| `lin_laboratory` | 既有 | 7 |
| `lin_lb_city` | 既有 | 7 |
| `lin_lb_diagnosisdetail` | 既有 | 22 |
| `lin_lb_diagnosisdetail_info` | 既有 | 3 |
| `lin_lb_dictbindkey` | 既有 | 4 |
| `lin_lb_dictfinancemailreport` | 既有 | 4 |
| `lin_lb_dictfinancereport` | 既有 | 3 |
| `lin_lb_dictmaildiagnosisreport` | 既有 | 5 |
| `lin_lb_dictmailreportnotice` | 既有 | 5 |
| `lin_lb_dictmailserver` | 既有 | 6 |
| `lin_lb_dictsex` | 既有 | 6 |
| `lin_lb_filediagnosisproject` | 既有 | 6 |
| `lin_lb_infoanimal` | 既有 | 6 |
| `lin_lb_infocheckreceived` | 既有 | 5 |
| `lin_lb_infohospital` | 既有 | 16 |
| `lin_lb_infohospitalcontact` | 既有 | 11 |
| `lin_lb_infosampletype` | 既有 | 6 |
| `lin_lb_infotesttype` | 既有 | 14 |
| `lin_lb_infotesttype_price` | 既有 | 6 |
| `lin_lb_infotesttypemodel` | 既有 | 6 |
| `lin_lb_infovariety` | 既有 | 5 |
| `lin_lb_kuhuirightdatagroup` | 既有 | 8 |
| `lin_lb_listdiagnosisproject` | 既有 | 40 |
| `lin_lb_noticemail` | 既有 | 7 |
| `lin_lb_notification` | 既有 | 15 |
| `lin_lb_notification_log` | 既有 | 15 |
| `lin_lb_paylog` | 既有 | 11 |
| `lin_lb_payorderdetail` | 既有 | 8 |
| `lin_lb_payordermain` | 既有 | 11 |
| `lin_lb_pet` | 既有 | 14 |
| `lin_monthly_bill` | 新增工作流 | 28 |
| `lin_monthly_bill_item` | 新增工作流 | 13 |
| `lin_notification_delivery` | 既有 | 7 |
| `lin_notification_event` | 既有 | 14 |
| `lin_notification_receipt` | 既有 | 3 |
| `lin_operational_config` | 新增工作流 | 11 |
| `lin_order_clinical` | 新增工作流 | 13 |
| `lin_order_draft` | 既有 | 7 |
| `lin_order_extension` | 既有 | 14 |
| `lin_order_idempotency` | 新增工作流 | 14 |
| `lin_order_pricing_snapshot` | 新增工作流 | 12 |
| `lin_profile_change` | 既有 | 9 |
| `lin_recharge_order` | 既有 | 10 |
| `lin_redemption` | 既有 | 11 |
| `lin_report_template` | 新增工作流 | 13 |
| `lin_report_workflow` | 新增工作流 | 22 |
| `lin_shipment` | 新增工作流 | 20 |
| `lin_sms` | 既有 | 7 |
| `lin_test_result` | 新增工作流 | 18 |
| `lin_user` | 既有 | 30 |
| `lin_user_coupon` | 既有 | 7 |
| `lin_user_group` | 既有 | 6 |
| `lin_user_money_log` | 既有 | 7 |
| `lin_user_preferences` | 既有 | 7 |
| `lin_user_role` | 新增工作流 | 11 |
| `lin_user_rule` | 既有 | 10 |
| `lin_user_score_log` | 既有 | 7 |
| `lin_user_token` | 既有 | 4 |
| `lin_version` | 既有 | 11 |
| `lin_wechat_payment` | 既有 | 25 |
| `listsupplyhistory` | 既有 | 8 |
| `listsupplymaterial` | 既有 | 7 |

## 表含义说明

| 表名 | 含义 |
|---|---|
| `flyway_schema_history` | Flyway 历史迁移版本、校验值及执行结果记录。 |
| `lin_admin` | 历史后台管理员账号及登录状态。 |
| `lin_admin_log` | 历史后台管理员操作日志。 |
| `lin_area` | 行政区域基础数据。 |
| `lin_article` | 资讯、活动、直播预告及弹窗内容。 |
| `lin_article_action` | 用户资讯点赞、收藏记录。 |
| `lin_attachment` | 历史通用附件及文件关联信息。 |
| `lin_audit_record` | 审核、金额、配置和关键状态变化的审计记录。 |
| `lin_auth_group` | 历史权限用户组。 |
| `lin_auth_group_access` | 历史用户与权限组关联。 |
| `lin_auth_rule` | 历史权限规则及菜单访问控制。 |
| `lin_category` | 历史内容分类。 |
| `lin_config` | 历史系统配置项。 |
| `lin_contract` | 医院与实验室签约、结算方式、账期及信用额度。 |
| `lin_contract_item_price` | 签约医院的项目协议价、折扣及有效期限。 |
| `lin_coupon` | 优惠券金额、使用门槛、实验室及有效期。 |
| `lin_doctor_profile` | 医生资质、职称、审核状态及登录凭据版本。 |
| `lin_ems` | 历史快递物流单及配送信息。 |
| `lin_file` | 新文件元数据、存储路径、内容摘要及业务归属。 |
| `lin_gift` | 积分商城礼品、积分价格及库存。 |
| `lin_infohospital_money` | 医院历史账户金额流水。 |
| `lin_invoice_application` | 发票申请、金额、审核状态及电子发票地址。 |
| `lin_invoice_order` | 发票申请与送检订单的关联。 |
| `lin_invoice_profile` | 用户开票抬头、税号、邮箱及银行账户资料。 |
| `lin_lab_task` | 标本检测任务、分配人员、设备、批次及任务状态。 |
| `lin_lab_test` | 各实验室独立检测项目目录、价格及上下架状态。 |
| `lin_laboratory` | 实验室基础资料及启用状态。 |
| `lin_lb_city` | 城市及业务区域数据。 |
| `lin_lb_diagnosisdetail` | 送检订单的检测项目明细、费用、报告地址及明细状态。 |
| `lin_lb_diagnosisdetail_info` | 检测明细的报告正文和结果内容。 |
| `lin_lb_dictbindkey` | 历史业务字典绑定关系。 |
| `lin_lb_dictfinancemailreport` | 历史财务邮件报告模板配置。 |
| `lin_lb_dictfinancereport` | 历史财务报告字典配置。 |
| `lin_lb_dictmaildiagnosisreport` | 历史检测报告邮件模板配置。 |
| `lin_lb_dictmailreportnotice` | 历史报告通知邮件配置。 |
| `lin_lb_dictmailserver` | 历史邮件服务器配置。 |
| `lin_lb_dictsex` | 宠物性别字典。 |
| `lin_lb_filediagnosisproject` | 历史送检相关文件记录。 |
| `lin_lb_infoanimal` | 宠物物种类别字典。 |
| `lin_lb_infocheckreceived` | 历史标本接收或签收类型资料。 |
| `lin_lb_infohospital` | 宠物医院主资料、地址、联系方式、余额及旧结算方式。 |
| `lin_lb_infohospitalcontact` | 医院联系人资料。 |
| `lin_lb_infosampletype` | 标本类型及采样材料字典。 |
| `lin_lb_infotesttype` | 全局检测项目字典、基础价格及报告模板。 |
| `lin_lb_infotesttype_price` | 全局检测项目历史价格记录。 |
| `lin_lb_infotesttypemodel` | 全局检测项目报告模板模型。 |
| `lin_lb_infovariety` | 宠物品种字典。 |
| `lin_lb_kuhuirightdatagroup` | 历史业务数据权限分组。 |
| `lin_lb_listdiagnosisproject` | 送检订单主表，保存宠物、医院、样本、费用及旧订单状态。 |
| `lin_lb_noticemail` | 历史业务通知邮件记录。 |
| `lin_lb_notification` | 旧订单状态变化通知记录。 |
| `lin_lb_notification_log` | 旧通知发送结果及重试日志。 |
| `lin_lb_paylog` | 历史支付操作日志。 |
| `lin_lb_payorderdetail` | 历史支付订单项目明细。 |
| `lin_lb_payordermain` | 历史支付订单主记录。 |
| `lin_lb_pet` | 宠物档案、主人、物种品种、性别及芯片信息。 |
| `lin_monthly_bill` | 月结账单主表、账期、对账、欠款、补充账单及结算状态。 |
| `lin_monthly_bill_item` | 月结账单的订单明细及优惠分摊金额。 |
| `lin_notification_delivery` | 微信或站内通知投递结果及失败原因。 |
| `lin_notification_event` | 业务通知事件、业务幂等键、阅读及推送状态。 |
| `lin_notification_receipt` | 通知接收确认记录。 |
| `lin_operational_config` | 运营参数、版本及配置变更信息。 |
| `lin_order_clinical` | 订单临床资料、采样信息和宠物历史快照。 |
| `lin_order_draft` | 送检草稿内容、实验室及草稿版本。 |
| `lin_order_extension` | 订单实验室、优惠券、计价、结算及审核扩展信息。 |
| `lin_order_idempotency` | 订单创建幂等键、请求指纹及响应结果。 |
| `lin_order_pricing_snapshot` | 订单签约、协议价、折扣及定价来源快照。 |
| `lin_profile_change` | 医生资料变更申请、前后快照及重审记录。 |
| `lin_recharge_order` | 医生钱包充值单、支付状态及幂等请求键。 |
| `lin_redemption` | 积分礼品兑换、数量、地址及物流状态。 |
| `lin_report_template` | 不同实验室和检测项目的报告模板及版本。 |
| `lin_report_workflow` | 报告生成、内部审核、发布及文件关联工作流。 |
| `lin_shipment` | 标本揽收、配送、异常、预计到达及实验室签收。 |
| `lin_sms` | 短信验证码及发送次数记录。 |
| `lin_test_result` | 检测指标结果、单位、参考范围、异常标记及结果批次。 |
| `lin_user` | 用户主账号、微信绑定、医院归属、钱包和积分余额。 |
| `lin_user_coupon` | 用户领取优惠券、预留、核销及过期状态。 |
| `lin_user_group` | 历史用户分组。 |
| `lin_user_money_log` | 医生钱包余额收支流水。 |
| `lin_user_preferences` | 用户订单、报告、发票和资讯通知偏好。 |
| `lin_user_role` | 用户角色、医院或实验室归属及资源范围。 |
| `lin_user_rule` | 历史用户规则配置。 |
| `lin_user_score_log` | 用户积分收支流水。 |
| `lin_user_token` | 历史用户登录令牌记录。 |
| `lin_version` | 历史应用版本信息。 |
| `lin_wechat_payment` | 微信支付单、订单金额、交易状态、退款及回调信息。 |
| `listsupplyhistory` | 医院耗材领用或出库历史。 |
| `listsupplymaterial` | 采样耗材、规格、单位及说明。 |

## 视图含义

| 视图 | 含义 |
|---|---|
| `vw_diagnosisdetail_all` | 检测明细及关联业务信息的综合查询视图。 |
| `vw_diagnosisdetail_all2` | 检测明细综合信息的另一历史统计视图。 |
| `vw_hospmonthreport` | 医院月度送检与财务汇总视图；当前保留历史查询行为。 |
| `vw_hospmonthreport3_sendmaildt` | 按报告发送时间统计的医院月度汇总视图。 |
| `vw_hospmonthreport3_sendmaildt_copy` | 上述月度汇总视图的历史副本。 |
| `vwdiagnosisdetail` | 历史检测明细查询视图。 |
| `vwlistdiagnosisproject` | 历史送检订单综合查询视图。 |

## 触发器含义

`trg_order_status_change` 在旧订单 `lin_lb_listdiagnosisproject` 更新后记录旧状态通知。合并系统保留该触发器，不修改其定义；新报告发布与旧桥接通知需保持去重。

## 字段明细

### `flyway_schema_history`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `installed_rank` | `int` | NO | PRI | — | — |
| `version` | `varchar(50)` | YES | — | — | — |
| `description` | `varchar(200)` | NO | — | — | — |
| `type` | `varchar(20)` | NO | — | — | — |
| `script` | `varchar(1000)` | NO | — | — | — |
| `checksum` | `int` | YES | — | — | — |
| `installed_by` | `varchar(100)` | NO | — | — | — |
| `installed_on` | `timestamp` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `execution_time` | `int` | NO | — | — | — |
| `success` | `tinyint(1)` | NO | — | — | — |

### `lin_admin`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `username` | `varchar(20)` | YES | UNI | — | — |
| `nickname` | `varchar(50)` | YES | — | — | — |
| `password` | `varchar(32)` | YES | — | — | — |
| `salt` | `varchar(30)` | YES | — | — | — |
| `avatar` | `varchar(255)` | YES | — | — | — |
| `email` | `varchar(100)` | YES | — | — | — |
| `loginfailure` | `tinyint unsigned` | NO | — | 0 | — |
| `logintime` | `int` | YES | — | — | — |
| `loginip` | `varchar(50)` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |
| `token` | `varchar(59)` | YES | — | — | — |
| `datagroup` | `varchar(1000)` | YES | — | — | — |
| `status` | `varchar(30)` | NO | — | normal | — |

### `lin_admin_log`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `admin_id` | `int unsigned` | NO | — | 0 | — |
| `username` | `varchar(30)` | YES | MUL | — | — |
| `url` | `varchar(1500)` | YES | — | — | — |
| `title` | `varchar(100)` | YES | — | — | — |
| `content` | `text` | NO | — | — | — |
| `ip` | `varchar(50)` | YES | — | — | — |
| `useragent` | `varchar(255)` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |

### `lin_area`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int` | NO | PRI | — | auto_increment |
| `pid` | `int` | YES | MUL | — | — |
| `shortname` | `varchar(100)` | YES | — | — | — |
| `name` | `varchar(100)` | YES | — | — | — |
| `mergename` | `varchar(255)` | YES | — | — | — |
| `level` | `tinyint` | YES | — | — | — |
| `pinyin` | `varchar(100)` | YES | — | — | — |
| `code` | `varchar(100)` | YES | — | — | — |
| `zip` | `varchar(100)` | YES | — | — | — |
| `first` | `varchar(50)` | YES | — | — | — |
| `lng` | `varchar(100)` | YES | — | — | — |
| `lat` | `varchar(100)` | YES | — | — | — |

### `lin_article`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `category` | `varchar(100)` | NO | — | — | — |
| `type` | `varchar(20)` | NO | — | article | — |
| `title` | `varchar(250)` | NO | — | — | — |
| `summary` | `varchar(1000)` | NO | — | — | — |
| `content` | `mediumtext` | NO | — | — | — |
| `cover_url` | `varchar(1000)` | NO | — | — | — |
| `live_at` | `datetime` | YES | — | — | — |
| `live_url` | `varchar(1000)` | NO | — | — | — |
| `popup_enabled` | `tinyint` | NO | — | 0 | — |
| `status` | `varchar(20)` | NO | MUL | draft | — |
| `published_at` | `datetime` | YES | — | — | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `updated_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |

### `lin_article_action`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `user_id` | `int unsigned` | NO | PRI | — | — |
| `article_id` | `bigint unsigned` | NO | PRI | — | — |
| `liked` | `tinyint` | NO | — | 0 | — |
| `bookmarked` | `tinyint` | NO | — | 0 | — |
| `updated_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |

### `lin_attachment`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `category` | `varchar(50)` | YES | — | — | — |
| `admin_id` | `int unsigned` | NO | — | 0 | — |
| `user_id` | `int unsigned` | NO | — | 0 | — |
| `url` | `varchar(255)` | YES | — | — | — |
| `imagewidth` | `varchar(30)` | YES | — | — | — |
| `imageheight` | `varchar(30)` | YES | — | — | — |
| `imagetype` | `varchar(30)` | YES | — | — | — |
| `imageframes` | `int unsigned` | NO | — | 0 | — |
| `filename` | `varchar(100)` | YES | — | — | — |
| `filesize` | `int unsigned` | NO | — | 0 | — |
| `mimetype` | `varchar(100)` | YES | — | — | — |
| `extparam` | `varchar(255)` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |
| `uploadtime` | `int` | YES | — | — | — |
| `storage` | `varchar(100)` | NO | — | local | — |
| `sha1` | `varchar(40)` | YES | — | — | — |

### `lin_audit_record`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `actor_user_id` | `int` | YES | MUL | — | — |
| `actor_role` | `varchar(32)` | YES | — | — | — |
| `hospital_id` | `int` | YES | — | — | — |
| `laboratory_id` | `int` | YES | — | — | — |
| `action` | `varchar(64)` | NO | — | — | — |
| `resource_type` | `varchar(64)` | NO | MUL | — | — |
| `resource_id` | `varchar(64)` | NO | — | — | — |
| `request_id` | `varchar(64)` | YES | MUL | — | — |
| `before_json` | `json` | YES | — | — | — |
| `after_json` | `json` | YES | — | — | — |
| `reason` | `varchar(1000)` | YES | — | — | — |
| `ip_address` | `varchar(64)` | YES | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |

### `lin_auth_group`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `pid` | `int unsigned` | NO | — | 0 | — |
| `name` | `varchar(100)` | YES | — | — | — |
| `rules` | `text` | NO | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |
| `status` | `varchar(30)` | YES | — | — | — |

### `lin_auth_group_access`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `uid` | `int unsigned` | NO | PRI | — | — |
| `group_id` | `int unsigned` | NO | PRI | — | — |

### `lin_auth_rule`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `type` | `enum('menu','file')` | NO | — | file | — |
| `pid` | `int unsigned` | NO | MUL | 0 | — |
| `name` | `varchar(100)` | YES | UNI | — | — |
| `title` | `varchar(50)` | YES | — | — | — |
| `icon` | `varchar(50)` | YES | — | — | — |
| `url` | `varchar(255)` | YES | — | — | — |
| `condition` | `varchar(255)` | YES | — | — | — |
| `remark` | `varchar(255)` | YES | — | — | — |
| `ismenu` | `tinyint unsigned` | NO | — | 0 | — |
| `menutype` | `enum('addtabs','blank','dialog','ajax')` | YES | — | — | — |
| `extend` | `varchar(255)` | YES | — | — | — |
| `py` | `varchar(30)` | YES | — | — | — |
| `pinyin` | `varchar(100)` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |
| `weigh` | `int` | NO | MUL | 0 | — |
| `status` | `varchar(30)` | YES | — | — | — |

### `lin_category`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `pid` | `int unsigned` | NO | MUL | 0 | — |
| `type` | `varchar(30)` | YES | — | — | — |
| `name` | `varchar(30)` | YES | — | — | — |
| `nickname` | `varchar(50)` | YES | — | — | — |
| `flag` | `set('hot','index','recommend')` | YES | — | — | — |
| `image` | `varchar(100)` | YES | — | — | — |
| `keywords` | `varchar(255)` | YES | — | — | — |
| `description` | `varchar(255)` | YES | — | — | — |
| `diyname` | `varchar(30)` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |
| `weigh` | `int` | NO | MUL | 0 | — |
| `status` | `varchar(30)` | YES | — | — | — |

### `lin_config`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `name` | `varchar(30)` | YES | UNI | — | — |
| `group` | `varchar(30)` | YES | — | — | — |
| `title` | `varchar(100)` | YES | — | — | — |
| `tip` | `varchar(100)` | YES | — | — | — |
| `type` | `varchar(30)` | YES | — | — | — |
| `value` | `text` | YES | — | — | — |
| `content` | `text` | YES | — | — | — |
| `rule` | `varchar(100)` | YES | — | — | — |
| `extend` | `varchar(255)` | YES | — | — | — |
| `setting` | `varchar(255)` | YES | — | — | — |

### `lin_contract`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `contract_no` | `varchar(64)` | NO | UNI | — | — |
| `hospital_id` | `int` | NO | MUL | — | — |
| `laboratory_id` | `int` | NO | — | — | — |
| `settlement_type` | `varchar(16)` | NO | — | — | — |
| `billing_cycle_days` | `int` | NO | — | 30 | — |
| `credit_amount` | `decimal(18,2)` | NO | — | 0.00 | — |
| `effective_from` | `date` | NO | — | — | — |
| `effective_to` | `date` | YES | — | — | — |
| `status` | `varchar(32)` | NO | — | — | — |
| `agreement_file_id` | `bigint` | YES | — | — | — |
| `signed_by` | `int` | YES | — | — | — |
| `signed_at` | `datetime(6)` | YES | — | — | — |
| `version` | `int` | NO | — | 0 | — |
| `created_by` | `int` | NO | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_by` | `int` | YES | — | — | — |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_contract_item_price`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `contract_id` | `bigint` | NO | MUL | — | — |
| `laboratory_id` | `int` | NO | MUL | — | — |
| `info_test_type_id` | `int` | NO | — | — | — |
| `currency` | `char(3)` | NO | — | CNY | — |
| `list_price` | `decimal(18,2)` | NO | — | — | — |
| `discount_rate` | `decimal(7,4)` | YES | — | — | — |
| `agreed_price` | `decimal(18,2)` | YES | — | — | — |
| `effective_from` | `date` | NO | — | — | — |
| `effective_to` | `date` | YES | — | — | — |
| `status` | `varchar(32)` | NO | — | — | — |
| `version` | `int` | NO | — | 0 | — |
| `created_by` | `int` | NO | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_by` | `int` | YES | — | — | — |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_coupon`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `name` | `varchar(100)` | NO | — | — | — |
| `amount` | `decimal(12,2)` | NO | — | — | — |
| `minimum_amount` | `decimal(12,2)` | NO | — | 0.00 | — |
| `laboratory_id` | `int unsigned` | YES | — | — | — |
| `starts_at` | `datetime` | NO | — | — | — |
| `expires_at` | `datetime` | NO | — | — | — |
| `active` | `tinyint` | NO | — | 1 | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |

### `lin_doctor_profile`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `user_id` | `int unsigned` | NO | PRI | — | — |
| `password_hash` | `varchar(255)` | YES | — | — | — |
| `title` | `varchar(100)` | NO | — | — | — |
| `qualification_files` | `json` | YES | — | — | — |
| `requested_hospital_id` | `int unsigned` | YES | — | — | — |
| `review_status` | `varchar(20)` | NO | — | pending | — |
| `review_reason` | `varchar(500)` | NO | — | — | — |
| `reviewed_by` | `int unsigned` | YES | — | — | — |
| `reviewed_at` | `datetime` | YES | — | — | — |
| `locked_until` | `datetime` | YES | — | — | — |
| `credential_version` | `int unsigned` | NO | — | 0 | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `updated_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |

### `lin_ems`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `event` | `varchar(30)` | YES | — | — | — |
| `email` | `varchar(100)` | YES | — | — | — |
| `code` | `varchar(10)` | YES | — | — | — |
| `times` | `int unsigned` | NO | — | 0 | — |
| `ip` | `varchar(30)` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |

### `lin_file`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `hospital_id` | `int` | YES | MUL | — | — |
| `laboratory_id` | `int` | YES | — | — | — |
| `owner_user_id` | `int` | YES | — | — | — |
| `biz_type` | `varchar(64)` | NO | MUL | — | — |
| `biz_id` | `varchar(64)` | NO | — | — | — |
| `original_name` | `varchar(255)` | NO | — | — | — |
| `storage_path` | `varchar(768)` | NO | UNI | — | — |
| `mime_type` | `varchar(128)` | NO | — | — | — |
| `file_size` | `bigint` | NO | — | — | — |
| `sha256` | `char(64)` | NO | — | — | — |
| `visibility` | `varchar(32)` | NO | — | PRIVATE | — |
| `uploaded_by` | `int` | NO | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `deleted_at` | `datetime(6)` | YES | — | — | — |

### `lin_gift`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `name` | `varchar(200)` | NO | — | — | — |
| `description` | `text` | YES | — | — | — |
| `image_url` | `varchar(1000)` | NO | — | — | — |
| `points_cost` | `int unsigned` | NO | — | — | — |
| `stock` | `int unsigned` | NO | — | 0 | — |
| `active` | `tinyint` | NO | — | 1 | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `updated_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |

### `lin_infohospital_money`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `infohospital_id` | `int unsigned` | YES | — | 0 | — |
| `amount` | `decimal(10,2)` | YES | — | — | — |
| `remark` | `varchar(255)` | YES | — | — | — |
| `admin_id` | `int unsigned` | YES | — | 0 | — |
| `createtime` | `int unsigned` | YES | — | — | — |
| `updatetime` | `int unsigned` | YES | — | — | — |

### `lin_invoice_application`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | MUL | — | — |
| `profile_snapshot` | `json` | NO | — | — | — |
| `amount` | `decimal(12,2)` | NO | — | — | — |
| `status` | `varchar(20)` | NO | — | pending | — |
| `request_key` | `varchar(64)` | NO | — | — | — |
| `invoice_url` | `varchar(1000)` | NO | — | — | — |
| `reject_reason` | `varchar(500)` | NO | — | — | — |
| `issued_at` | `datetime` | YES | — | — | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |

### `lin_invoice_order`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `application_id` | `bigint unsigned` | NO | — | — | — |
| `order_id` | `int unsigned` | NO | PRI | — | — |

### `lin_invoice_profile`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | MUL | — | — |
| `title` | `varchar(200)` | NO | — | — | — |
| `tax_number` | `varchar(50)` | NO | — | — | — |
| `email` | `varchar(200)` | NO | — | — | — |
| `address` | `varchar(500)` | NO | — | — | — |
| `phone` | `varchar(30)` | NO | — | — | — |
| `bank_name` | `varchar(200)` | NO | — | — | — |
| `bank_account` | `varchar(100)` | NO | — | — | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `updated_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |

### `lin_lab_task`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `shipment_id` | `bigint` | YES | — | — | — |
| `order_id` | `int` | NO | MUL | — | — |
| `detail_id` | `int` | YES | — | — | — |
| `laboratory_id` | `int` | NO | MUL | — | — |
| `parent_task_id` | `bigint` | YES | MUL | — | — |
| `task_type` | `varchar(32)` | NO | — | INITIAL | — |
| `status` | `varchar(32)` | NO | — | — | — |
| `assigned_to` | `int` | YES | MUL | — | — |
| `equipment_code` | `varchar(64)` | YES | — | — | — |
| `sample_check_json` | `json` | YES | — | — | — |
| `received_by` | `int` | YES | — | — | — |
| `received_at` | `datetime(6)` | YES | — | — | — |
| `started_at` | `datetime(6)` | YES | — | — | — |
| `completed_at` | `datetime(6)` | YES | — | — | — |
| `return_reason` | `varchar(1000)` | YES | — | — | — |
| `returned_at` | `datetime(6)` | YES | — | — | — |
| `version` | `int` | NO | — | 0 | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_lab_test`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `laboratory_id` | `int unsigned` | NO | PRI | — | — |
| `test_type_id` | `int unsigned` | NO | PRI | — | — |
| `price` | `decimal(12,2)` | YES | — | — | — |
| `active` | `tinyint` | NO | — | 1 | — |

### `lin_laboratory`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `code` | `varchar(50)` | NO | UNI | — | — |
| `name` | `varchar(200)` | NO | — | — | — |
| `description` | `varchar(1000)` | NO | — | — | — |
| `active` | `tinyint` | NO | — | 1 | — |
| `sort_order` | `int` | NO | — | 0 | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |

### `lin_lb_city`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `CityId` | `int unsigned` | NO | PRI | — | auto_increment |
| `CityName` | `varchar(50)` | NO | — | — | — |
| `AreaCode` | `int` | YES | — | — | — |
| `Grade` | `varchar(50)` | YES | — | — | — |
| `ZipCode` | `int` | YES | — | — | — |
| `BelongCity_cityId` | `int` | YES | — | — | — |
| `IsActive` | `int` | NO | — | — | — |

### `lin_lb_diagnosisdetail`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `DiagnosisDetailId` | `int unsigned` | NO | PRI | — | auto_increment |
| `ListDiagnosisProjectId` | `int` | NO | MUL | — | — |
| `GenterNO` | `varchar(150)` | NO | — | — | — |
| `InfoTestTypeId` | `int` | NO | MUL | — | — |
| `InfoTestTypeModelId` | `int` | NO | — | — | — |
| `MailTitle` | `varchar(250)` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `IsChangeModelTemple` | `int` | NO | — | — | — |
| `IsChangeMailModelTemple` | `int` | YES | — | — | — |
| `IsSendMail` | `int` | NO | — | — | — |
| `RealFee` | `double` | YES | — | — | — |
| `IsCheckPass` | `int` | NO | MUL | — | — |
| `PDFHttpUrl` | `varchar(255)` | NO | — | — | — |
| `TestPrice` | `decimal(18,2)` | NO | — | — | — |
| `OrginalPrice` | `decimal(18,2)` | NO | — | — | — |
| `OrginalDiscount` | `decimal(4,2)` | YES | — | — | — |
| `SendMailDt` | `datetime` | NO | — | — | — |
| `DetailRemark` | `varchar(100)` | NO | — | — | — |
| `PayStatus2` | `int` | NO | — | — | — |
| `DiagnosisStatus` | `int` | NO | — | — | — |
| `IsActive` | `int` | NO | — | — | — |
| `is_invoiced` | `tinyint` | NO | — | 0 | — |

### `lin_lb_diagnosisdetail_info`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `diagnosisdetail_id` | `int` | NO | PRI | — | — |
| `MailReport` | `longtext` | YES | — | — | — |
| `ReportResult` | `longtext` | YES | — | — | — |

### `lin_lb_dictbindkey`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `BindTable` | `varchar(50)` | NO | — | — | — |
| `ColumnName` | `varchar(50)` | NO | — | — | — |
| `DescName` | `varchar(50)` | NO | — | — | — |

### `lin_lb_dictfinancemailreport`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `model_name` | `varchar(1000)` | NO | — | — | — |
| `model_content` | `longtext` | NO | — | — | — |
| `IsActive` | `int` | NO | — | — | — |

### `lin_lb_dictfinancereport`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `model_name` | `varchar(1000)` | NO | — | — | — |
| `model_content` | `longtext` | NO | — | — | — |

### `lin_lb_dictmaildiagnosisreport`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `model_type` | `varchar(1000)` | NO | — | — | — |
| `model_content` | `longtext` | YES | — | — | — |
| `createtime` | `int unsigned` | YES | — | — | — |
| `updatetime` | `int unsigned` | YES | — | — | — |

### `lin_lb_dictmailreportnotice`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `tplname` | `varchar(1000)` | NO | — | — | — |
| `tpltext` | `longtext` | NO | — | — | — |
| `is_active` | `int` | NO | — | — | — |
| `createtime` | `int` | YES | — | — | — |

### `lin_lb_dictmailserver`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `DictMailServerId` | `int` | NO | PRI | — | — |
| `ServerHost` | `varchar(250)` | NO | — | — | — |
| `Port` | `int` | NO | — | — | — |
| `UserName` | `varchar(250)` | NO | — | — | — |
| `Psw` | `varchar(250)` | NO | — | — | — |
| `BCCMail` | `text` | NO | — | — | — |

### `lin_lb_dictsex`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `DictSexId` | `int unsigned` | NO | PRI | — | auto_increment |
| `SexName` | `varchar(50)` | NO | — | — | — |
| `SexName_en` | `varchar(50)` | NO | — | — | — |
| `IsActive` | `int` | NO | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |

### `lin_lb_filediagnosisproject`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `FileDiagnosisProjectId` | `int unsigned` | NO | PRI | — | auto_increment |
| `DiagnosisDetailId` | `int` | NO | — | — | — |
| `FileName` | `varchar(1000)` | YES | — | — | — |
| `Path` | `varchar(1000)` | NO | — | — | — |
| `HttpUrl` | `varchar(1000)` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |

### `lin_lb_infoanimal`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `InfoAnimalId` | `int unsigned` | NO | PRI | — | auto_increment |
| `AnimalName` | `varchar(250)` | NO | — | — | — |
| `AnimalName_en` | `varchar(250)` | NO | — | — | — |
| `IsActive` | `int` | NO | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |

### `lin_lb_infocheckreceived`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `InfoCheckReceivedId` | `int unsigned` | NO | PRI | — | auto_increment |
| `CheckType` | `varchar(250)` | NO | — | — | — |
| `IsActive` | `int` | NO | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |

### `lin_lb_infohospital`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `InfoHospitalId` | `int unsigned` | NO | PRI | — | auto_increment |
| `HospitalName` | `varchar(250)` | NO | — | — | — |
| `HospitalName_en` | `varchar(250)` | NO | — | — | — |
| `Invoice` | `varchar(250)` | YES | — | — | — |
| `CityId` | `int` | YES | — | — | — |
| `Place` | `text` | YES | — | — | — |
| `Adress` | `text` | YES | — | — | — |
| `Tel` | `varchar(50)` | YES | — | — | — |
| `Fax` | `varchar(50)` | YES | — | — | — |
| `InfoHospitalContactId` | `int` | YES | — | — | — |
| `CreateDt` | `datetime` | YES | — | — | — |
| `IsActive` | `int` | YES | — | — | — |
| `LoginName` | `varchar(50)` | YES | — | — | — |
| `LoginPwd` | `varchar(50)` | YES | — | — | — |
| `amount` | `decimal(10,2)` | YES | — | 0.00 | — |
| `PayType` | `int` | NO | — | — | — |

### `lin_lb_infohospitalcontact`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `InfoHospitalContactId` | `int unsigned` | NO | PRI | — | auto_increment |
| `InfoHospitalId` | `int` | NO | — | — | — |
| `isFinanceContact` | `int` | NO | — | — | — |
| `Name` | `varchar(250)` | NO | — | — | — |
| `Tel` | `varchar(250)` | NO | — | — | — |
| `Mail` | `varchar(250)` | NO | — | — | — |
| `Fax` | `varchar(250)` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `LoginName` | `varchar(50)` | YES | — | — | — |
| `LoginPwd` | `varchar(50)` | YES | — | — | — |
| `ContactRemark` | `varchar(250)` | YES | — | — | — |

### `lin_lb_infosampletype`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `InfoSampleTypeId` | `int unsigned` | NO | PRI | — | auto_increment |
| `SampleName` | `varchar(250)` | NO | — | — | — |
| `SampleName_en` | `varchar(250)` | NO | — | — | — |
| `IsActive` | `int` | NO | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |

### `lin_lb_infotesttype`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `InfoTestTypeId` | `int unsigned` | NO | PRI | — | auto_increment |
| `TestName` | `varchar(250)` | NO | MUL | — | — |
| `TestNO` | `varchar(250)` | NO | — | — | — |
| `Price` | `double` | NO | — | — | — |
| `CtoG` | `double` | NO | — | — | — |
| `GtoC` | `double` | NO | — | — | — |
| `DiscountReal` | `double` | NO | — | — | — |
| `DiscountNext` | `double` | NO | — | — | — |
| `TextModel` | `longtext` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `IsActive` | `int` | NO | — | — | — |
| `type` | `tinyint unsigned` | YES | — | 0 | — |
| `updatetime` | `int unsigned` | YES | — | — | — |
| `createtime` | `int unsigned` | YES | — | — | — |

### `lin_lb_infotesttype_price`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `IID` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `InfoTestTypeid` | `bigint` | YES | — | — | — |
| `price` | `decimal(18,2)` | NO | — | — | — |
| `IsActive` | `int unsigned` | NO | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int unsigned` | YES | — | — | — |

### `lin_lb_infotesttypemodel`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `InfoTestTypeModelId` | `int unsigned` | NO | PRI | — | auto_increment |
| `ModelName` | `varchar(1000)` | NO | — | — | — |
| `ModelInfo` | `longtext` | NO | — | — | — |
| `InfoTestTypeId` | `int` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `updatetime` | `int unsigned` | YES | — | — | — |

### `lin_lb_infovariety`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `IID` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `Variety_CN` | `varchar(50)` | NO | — | — | — |
| `Variety_EN` | `varchar(50)` | NO | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |

### `lin_lb_kuhuirightdatagroup`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `IID` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `GroupName` | `varchar(150)` | NO | MUL | — | — |
| `GroupRemark` | `varchar(150)` | NO | — | — | — |
| `StartWord` | `varchar(50)` | NO | — | — | — |
| `StartNO` | `varchar(50)` | NO | — | — | — |
| `ParentIID` | `varchar(50)` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `UpdateDt` | `datetime` | NO | — | — | — |

### `lin_lb_listdiagnosisproject`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `ListDiagnosisProjectId` | `int unsigned` | NO | PRI | — | auto_increment |
| `CreateDataTypeIId` | `bigint` | NO | MUL | — | — |
| `UserNO` | `varchar(50)` | NO | — | — | — |
| `CityId` | `int` | NO | — | — | — |
| `InfoHospitalId` | `int` | NO | — | — | — |
| `InfoAnimalId` | `int` | NO | — | — | — |
| `InfoSampleTypeId` | `int` | NO | — | — | — |
| `AnimalName` | `varchar(250)` | NO | — | — | — |
| `AnimalName_en` | `varchar(250)` | NO | — | — | — |
| `AnimalTypeId` | `int` | NO | — | — | — |
| `AnimalType` | `varchar(50)` | NO | — | — | — |
| `AnimalType_en` | `varchar(50)` | NO | — | — | — |
| `AnimalWeight` | `varchar(50)` | NO | — | — | — |
| `ChipNO` | `varchar(50)` | NO | — | — | — |
| `Age` | `varchar(50)` | NO | — | — | — |
| `Birthday` | `datetime` | YES | — | — | — |
| `DictSexId` | `int` | NO | — | — | — |
| `SampleArea` | `varchar(50)` | NO | — | — | — |
| `MasterName` | `varchar(250)` | NO | — | — | — |
| `MasterName_en` | `varchar(250)` | NO | — | — | — |
| `DiagnosticResults` | `varchar(255)` | YES | — | — | — |
| `CollectDt` | `datetime` | YES | — | — | — |
| `ReceiveSampleDt` | `datetime` | NO | MUL | — | — |
| `SendSampleDt` | `datetime` | NO | — | — | — |
| `ReportNO` | `varchar(50)` | NO | — | — | — |
| `SendReportDt` | `datetime` | NO | MUL | — | — |
| `TestFee` | `double` | NO | — | — | — |
| `DeliverySendGerFee` | `double` | NO | — | — | — |
| `DeliverySendCNFee` | `double` | NO | — | — | — |
| `InfoCheckReceivedId` | `int` | NO | — | — | — |
| `SaveMedium` | `int` | NO | — | — | — |
| `IsPayMoney` | `int` | NO | — | — | — |
| `Remark` | `text` | NO | — | — | — |
| `IsSendNoticeMail` | `int` | NO | — | — | — |
| `SampleStatus` | `int` | NO | — | — | — |
| `notification_subscribe` | `tinyint` | YES | — | 0 | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `DoctorName` | `varchar(100)` | NO | — | — | — |
| `MarketingName` | `varchar(100)` | NO | — | — | — |
| `CreateSampleId` | `int` | NO | — | — | — |

### `lin_lb_noticemail`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `IID` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `HospId` | `int` | NO | — | — | — |
| `PayMonth` | `varchar(50)` | NO | — | — | — |
| `IsSendMail` | `int` | NO | — | — | — |
| `Remark` | `varchar(50)` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `UpdateDt` | `datetime` | NO | — | — | — |

### `lin_lb_notification`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int` | NO | PRI | — | auto_increment |
| `order_id` | `int` | NO | MUL | — | — |
| `hospital_id` | `int` | NO | MUL | — | — |
| `old_status` | `int` | YES | — | — | — |
| `new_status` | `int` | NO | — | — | — |
| `animal_name` | `varchar(250)` | YES | — | — | — |
| `master_name` | `varchar(250)` | YES | — | — | — |
| `notification_message` | `text` | YES | — | — | — |
| `operator_id` | `int` | YES | — | — | — |
| `operation_time` | `datetime` | NO | MUL | — | — |
| `wechat_sent_status` | `tinyint` | NO | — | 0 | — |
| `wechat_message_id` | `varchar(100)` | YES | — | — | — |
| `error_message` | `text` | YES | — | — | — |
| `createtime` | `int unsigned` | YES | — | — | — |
| `updatetime` | `int unsigned` | YES | — | — | — |

### `lin_lb_notification_log`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int` | NO | PRI | — | auto_increment |
| `notification_id` | `int` | NO | MUL | — | — |
| `order_id` | `int` | NO | MUL | — | — |
| `hospital_id` | `int` | NO | — | — | — |
| `user_id` | `int` | YES | — | — | — |
| `openid` | `varchar(100)` | YES | MUL | — | — |
| `template_id` | `varchar(100)` | YES | — | — | — |
| `message_id` | `varchar(100)` | YES | — | — | — |
| `send_status` | `tinyint` | NO | MUL | 0 | — |
| `error_code` | `varchar(50)` | YES | — | — | — |
| `error_message` | `text` | YES | — | — | — |
| `send_time` | `datetime` | YES | MUL | — | — |
| `retry_count` | `int` | YES | — | 0 | — |
| `createtime` | `int unsigned` | YES | — | — | — |
| `updatetime` | `int unsigned` | YES | — | — | — |

### `lin_lb_paylog`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `PayLogId` | `int unsigned` | NO | PRI | — | auto_increment |
| `BuyMail` | `varchar(250)` | NO | — | — | — |
| `BuyerId` | `varchar(250)` | NO | — | — | — |
| `TradeNoAli` | `varchar(50)` | NO | — | — | — |
| `TradeNoUser` | `varchar(50)` | NO | — | — | — |
| `TotalFee` | `decimal(18,2)` | NO | — | — | — |
| `TradeStatus` | `varchar(250)` | NO | — | — | — |
| `Subject` | `varchar(250)` | NO | — | — | — |
| `TradeBody` | `varchar(250)` | NO | — | — | — |
| `Exterface` | `varchar(250)` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |

### `lin_lb_payorderdetail`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `IID` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `MainOrderId` | `varchar(50)` | NO | — | — | — |
| `UserNo` | `varchar(50)` | NO | — | — | — |
| `DetailId` | `bigint` | NO | — | — | — |
| `ShowMoney` | `decimal(18,4)` | NO | — | — | — |
| `PayMoney` | `decimal(18,4)` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `UpdateDt` | `datetime` | NO | — | — | — |

### `lin_lb_payordermain`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `IID` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `OrderId` | `varchar(100)` | NO | — | — | — |
| `Md5Id` | `varchar(100)` | NO | — | — | — |
| `AliTradeId` | `varchar(200)` | NO | — | — | — |
| `HostId` | `int` | NO | — | — | — |
| `PayMonth` | `varchar(50)` | NO | — | — | — |
| `CHKMoney` | `decimal(18,4)` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `UpdateDt` | `datetime` | NO | — | — | — |
| `IsSuccess` | `int` | NO | — | — | — |
| `Remark` | `text` | NO | — | — | — |

### `lin_lb_pet`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `animal_name` | `varchar(100)` | NO | — | — | — |
| `animal_name_en` | `varchar(100)` | YES | — | — | — |
| `info_animal_id` | `int` | YES | MUL | — | — |
| `info_variety_id` | `int` | YES | — | — | — |
| `dict_sex_id` | `int` | YES | — | — | — |
| `birthday` | `varchar(20)` | YES | — | — | — |
| `chip_no` | `varchar(50)` | YES | — | — | — |
| `owner_name` | `varchar(100)` | NO | — | — | — |
| `owner_name_en` | `varchar(100)` | YES | — | — | — |
| `info_hospital_id` | `int` | NO | MUL | — | — |
| `IsActive` | `int` | NO | MUL | 1 | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |

### `lin_monthly_bill`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `bill_no` | `varchar(64)` | NO | UNI | — | — |
| `contract_id` | `bigint` | NO | MUL | — | — |
| `hospital_id` | `int` | NO | MUL | — | — |
| `laboratory_id` | `int` | NO | MUL | — | — |
| `period_start` | `date` | NO | — | — | — |
| `period_end` | `date` | NO | — | — | — |
| `currency` | `char(3)` | NO | — | CNY | — |
| `total_amount` | `decimal(18,2)` | NO | — | 0.00 | — |
| `discount_amount` | `decimal(18,2)` | NO | — | 0.00 | — |
| `due_amount` | `decimal(18,2)` | NO | — | 0.00 | — |
| `paid_amount` | `decimal(18,2)` | NO | — | 0.00 | — |
| `status` | `varchar(32)` | NO | — | — | — |
| `due_date` | `date` | NO | — | — | — |
| `bill_kind` | `varchar(16)` | NO | — | STANDARD | — |
| `supplement_no` | `int` | NO | — | 0 | — |
| `generated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `hospital_reconciled_by` | `int` | YES | — | — | — |
| `hospital_reconciled_at` | `datetime(6)` | YES | — | — | — |
| `laboratory_reconciled_by` | `int` | YES | — | — | — |
| `laboratory_reconciled_at` | `datetime(6)` | YES | — | — | — |
| `reconciled_at` | `datetime(6)` | YES | — | — | — |
| `settled_at` | `datetime(6)` | YES | — | — | — |
| `settlement_reference` | `varchar(128)` | YES | — | — | — |
| `last_overdue_reminded_at` | `datetime(6)` | YES | — | — | — |
| `version` | `int` | NO | — | 0 | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_monthly_bill_item`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `bill_id` | `bigint` | NO | MUL | — | — |
| `order_id` | `int` | NO | MUL | — | — |
| `detail_id` | `int` | NO | — | — | — |
| `contract_item_price_id` | `bigint` | YES | — | — | — |
| `description` | `varchar(512)` | YES | — | — | — |
| `quantity` | `decimal(12,2)` | NO | — | 1.00 | — |
| `unit_price` | `decimal(18,2)` | NO | — | — | — |
| `amount` | `decimal(18,2)` | NO | — | — | — |
| `discount_rate` | `decimal(7,4)` | YES | — | — | — |
| `discount_amount` | `decimal(18,2)` | NO | — | 0.00 | — |
| `final_amount` | `decimal(18,2)` | NO | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |

### `lin_notification_delivery`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `event_id` | `bigint unsigned` | NO | MUL | — | — |
| `status` | `varchar(20)` | NO | — | — | — |
| `error_code` | `varchar(50)` | NO | — | — | — |
| `error_message` | `varchar(1000)` | NO | — | — | — |
| `message_id` | `varchar(100)` | NO | — | — | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |

### `lin_notification_event`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | MUL | — | — |
| `event_type` | `varchar(40)` | NO | — | — | — |
| `business_key` | `varchar(100)` | NO | — | — | — |
| `title` | `varchar(200)` | NO | — | — | — |
| `message` | `varchar(1000)` | NO | — | — | — |
| `page_path` | `varchar(300)` | NO | — | — | — |
| `template_data` | `json` | YES | — | — | — |
| `push_status` | `varchar(20)` | NO | MUL | pending | — |
| `retry_count` | `int unsigned` | NO | — | 0 | — |
| `next_attempt_at` | `datetime` | YES | — | — | — |
| `read_at` | `datetime` | YES | — | — | — |
| `last_error` | `varchar(1000)` | NO | — | — | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |

### `lin_notification_receipt`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `user_id` | `int unsigned` | NO | PRI | — | — |
| `notification_id` | `int` | NO | PRI | — | — |
| `read_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |

### `lin_operational_config`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `config_key` | `varchar(128)` | NO | MUL | — | — |
| `config_value` | `json` | NO | — | — | — |
| `scope_type` | `varchar(32)` | NO | — | GLOBAL | — |
| `scope_id` | `int` | NO | — | 0 | — |
| `description` | `varchar(512)` | YES | — | — | — |
| `status` | `varchar(32)` | NO | MUL | ACTIVE | — |
| `version` | `int` | NO | — | 0 | — |
| `updated_by` | `int` | NO | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_order_clinical`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `order_id` | `int` | NO | UNI | — | — |
| `pet_id` | `int` | YES | MUL | — | — |
| `hospital_id` | `int` | NO | — | — | — |
| `medical_history` | `text` | YES | — | — | — |
| `clinical_symptoms` | `text` | YES | — | — | — |
| `diagnosis` | `text` | YES | — | — | — |
| `medication` | `text` | YES | — | — | — |
| `clinical_data` | `json` | YES | — | — | — |
| `created_by` | `int` | NO | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_by` | `int` | YES | — | — | — |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_order_draft`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | MUL | — | — |
| `laboratory_id` | `int unsigned` | YES | — | — | — |
| `payload` | `json` | NO | — | — | — |
| `version` | `int unsigned` | NO | — | 0 | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `updated_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |

### `lin_order_extension`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `order_id` | `int unsigned` | NO | PRI | — | — |
| `user_id` | `int unsigned` | NO | MUL | — | — |
| `laboratory_id` | `int unsigned` | NO | — | — | — |
| `user_coupon_id` | `bigint unsigned` | YES | — | — | — |
| `original_amount` | `decimal(12,2)` | NO | — | 0.00 | — |
| `discount_amount` | `decimal(12,2)` | NO | — | 0.00 | — |
| `payable_amount` | `decimal(12,2)` | NO | — | 0.00 | — |
| `review_status` | `varchar(20)` | NO | MUL | pending | — |
| `review_reason` | `varchar(500)` | NO | — | — | — |
| `reviewed_by` | `int unsigned` | YES | — | — | — |
| `reviewed_at` | `datetime` | YES | — | — | — |
| `settlement_mode` | `varchar(20)` | NO | — | cash | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `updated_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |

### `lin_order_idempotency`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `order_id` | `int` | YES | MUL | — | — |
| `user_id` | `int` | NO | MUL | — | — |
| `hospital_id` | `int` | NO | — | — | — |
| `operation` | `varchar(64)` | NO | — | — | — |
| `idempotency_key` | `varchar(128)` | NO | — | — | — |
| `request_hash` | `char(64)` | NO | — | — | — |
| `response_code` | `int` | YES | — | — | — |
| `response_body` | `json` | YES | — | — | — |
| `status` | `varchar(32)` | NO | — | — | — |
| `locked_until` | `datetime(6)` | YES | — | — | — |
| `expires_at` | `datetime(6)` | NO | MUL | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `completed_at` | `datetime(6)` | YES | — | — | — |

### `lin_order_pricing_snapshot`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `order_id` | `int` | NO | UNI | — | — |
| `contract_id` | `bigint` | YES | MUL | — | — |
| `hospital_id` | `int` | NO | — | — | — |
| `laboratory_id` | `int` | NO | — | — | — |
| `settlement_type` | `varchar(16)` | NO | — | — | — |
| `original_amount` | `decimal(18,2)` | NO | — | — | — |
| `discount_amount` | `decimal(18,2)` | NO | — | — | — |
| `payable_amount` | `decimal(18,2)` | NO | — | — | — |
| `items_json` | `json` | NO | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_profile_change`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | MUL | — | — |
| `requested_data` | `json` | NO | — | — | — |
| `previous_data` | `json` | NO | — | — | — |
| `status` | `varchar(20)` | NO | MUL | pending | — |
| `review_reason` | `varchar(500)` | NO | — | — | — |
| `reviewed_by` | `int unsigned` | YES | — | — | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `reviewed_at` | `datetime` | YES | — | — | — |

### `lin_recharge_order`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | MUL | — | — |
| `payment_no` | `varchar(64)` | NO | UNI | — | — |
| `amount` | `decimal(12,2)` | NO | — | — | — |
| `status` | `varchar(20)` | NO | — | pending | — |
| `transaction_id` | `varchar(100)` | YES | UNI | — | — |
| `request_key` | `varchar(64)` | NO | — | — | — |
| `prepay_id` | `varchar(128)` | YES | — | — | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `paid_at` | `datetime` | YES | — | — | — |

### `lin_redemption`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | MUL | — | — |
| `gift_id` | `bigint unsigned` | NO | — | — | — |
| `gift_name` | `varchar(200)` | NO | — | — | — |
| `quantity` | `int unsigned` | NO | — | — | — |
| `points_spent` | `int unsigned` | NO | — | — | — |
| `delivery_address` | `json` | NO | — | — | — |
| `request_key` | `varchar(64)` | NO | — | — | — |
| `status` | `varchar(20)` | NO | — | pending | — |
| `tracking_number` | `varchar(100)` | NO | — | — | — |
| `created_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |

### `lin_report_template`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `laboratory_id` | `int` | NO | MUL | — | — |
| `info_test_type_id` | `int` | YES | — | — | — |
| `template_name` | `varchar(128)` | NO | — | — | — |
| `template_code` | `varchar(64)` | NO | — | — | — |
| `content` | `mediumtext` | NO | — | — | — |
| `locale` | `varchar(16)` | NO | — | zh_CN | — |
| `version` | `int` | NO | — | 1 | — |
| `status` | `varchar(32)` | NO | — | — | — |
| `created_by` | `int` | NO | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_by` | `int` | YES | — | — | — |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_report_workflow`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `order_id` | `int` | NO | MUL | — | — |
| `detail_id` | `int` | NO | MUL | — | — |
| `test_result_id` | `bigint` | NO | MUL | — | — |
| `template_id` | `bigint` | YES | — | — | — |
| `template_version` | `int` | YES | — | — | — |
| `hospital_id` | `int` | NO | — | — | — |
| `laboratory_id` | `int` | NO | — | — | — |
| `status` | `varchar(32)` | NO | — | — | — |
| `pdf_file_id` | `bigint` | YES | — | — | — |
| `payment_checked` | `tinyint(1)` | NO | — | 0 | — |
| `submitted_by` | `int` | YES | — | — | — |
| `submitted_at` | `datetime(6)` | YES | — | — | — |
| `reviewed_by` | `int` | YES | — | — | — |
| `reviewed_at` | `datetime(6)` | YES | — | — | — |
| `review_note` | `varchar(1000)` | YES | — | — | — |
| `published_by` | `int` | YES | — | — | — |
| `published_at` | `datetime(6)` | YES | — | — | — |
| `legacy_synced_at` | `datetime(6)` | YES | — | — | — |
| `version` | `int` | NO | — | 0 | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_shipment`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `order_id` | `int` | NO | UNI | — | — |
| `hospital_id` | `int` | NO | — | — | — |
| `laboratory_id` | `int` | NO | MUL | — | — |
| `courier_user_id` | `int` | YES | MUL | — | — |
| `tracking_no` | `varchar(64)` | NO | UNI | — | — |
| `status` | `varchar(32)` | NO | — | — | — |
| `pickup_address` | `varchar(512)` | YES | — | — | — |
| `expected_arrived_at` | `datetime(6)` | YES | — | — | — |
| `assigned_at` | `datetime(6)` | YES | — | — | — |
| `picked_up_at` | `datetime(6)` | YES | — | — | — |
| `delivered_at` | `datetime(6)` | YES | — | — | — |
| `signed_by` | `int` | YES | — | — | — |
| `signed_at` | `datetime(6)` | YES | — | — | — |
| `exception_type` | `varchar(32)` | YES | — | — | — |
| `exception_note` | `varchar(1000)` | YES | — | — | — |
| `exception_at` | `datetime(6)` | YES | — | — | — |
| `version` | `int` | NO | — | 0 | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_sms`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `event` | `varchar(30)` | YES | — | — | — |
| `mobile` | `varchar(20)` | YES | — | — | — |
| `code` | `varchar(10)` | YES | — | — | — |
| `times` | `int unsigned` | NO | — | 0 | — |
| `ip` | `varchar(30)` | YES | — | — | — |
| `createtime` | `int unsigned` | YES | — | 0 | — |

### `lin_test_result`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `lab_task_id` | `bigint` | NO | MUL | — | — |
| `order_id` | `int` | NO | MUL | — | — |
| `detail_id` | `int` | NO | MUL | — | — |
| `laboratory_id` | `int` | NO | — | — | — |
| `batch_no` | `varchar(64)` | NO | — | — | — |
| `is_retest` | `tinyint(1)` | NO | — | 0 | — |
| `result_status` | `varchar(32)` | NO | — | — | — |
| `result_items` | `json` | NO | — | — | — |
| `reference_snapshot` | `json` | YES | — | — | — |
| `instrument_data` | `json` | YES | — | — | — |
| `entered_by` | `int` | NO | — | — | — |
| `entered_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `reviewed_by` | `int` | YES | — | — | — |
| `reviewed_at` | `datetime(6)` | YES | — | — | — |
| `version` | `int` | NO | — | 0 | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_user`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `group_id` | `int unsigned` | NO | — | 0 | — |
| `username` | `varchar(32)` | YES | MUL | — | — |
| `nickname` | `varchar(50)` | YES | — | — | — |
| `password` | `varchar(32)` | YES | — | — | — |
| `salt` | `varchar(30)` | YES | — | — | — |
| `email` | `varchar(100)` | YES | MUL | — | — |
| `mobile` | `varchar(11)` | YES | MUL | — | — |
| `avatar` | `varchar(255)` | YES | — | — | — |
| `level` | `tinyint unsigned` | NO | — | 0 | — |
| `gender` | `tinyint unsigned` | NO | — | 0 | — |
| `birthday` | `date` | YES | — | — | — |
| `bio` | `varchar(100)` | YES | — | — | — |
| `money` | `decimal(10,2)` | NO | — | 0.00 | — |
| `score` | `int` | NO | — | 0 | — |
| `successions` | `int unsigned` | NO | — | 1 | — |
| `maxsuccessions` | `int unsigned` | NO | — | 1 | — |
| `prevtime` | `int` | YES | — | — | — |
| `logintime` | `int` | YES | — | — | — |
| `loginip` | `varchar(50)` | YES | — | — | — |
| `loginfailure` | `tinyint unsigned` | NO | — | 0 | — |
| `joinip` | `varchar(50)` | YES | — | — | — |
| `jointime` | `int` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |
| `token` | `varchar(50)` | YES | — | — | — |
| `status` | `varchar(30)` | YES | — | — | — |
| `verification` | `varchar(255)` | YES | — | — | — |
| `openid` | `varchar(100)` | YES | UNI | — | — |
| `hospital_id` | `int unsigned` | YES | — | 0 | — |

### `lin_user_coupon`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | MUL | — | — |
| `coupon_id` | `bigint unsigned` | NO | — | — | — |
| `status` | `varchar(20)` | NO | — | available | — |
| `order_id` | `int unsigned` | YES | — | — | — |
| `issued_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `used_at` | `datetime` | YES | — | — | — |

### `lin_user_group`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `name` | `varchar(50)` | YES | — | — | — |
| `rules` | `text` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |
| `status` | `enum('normal','hidden')` | YES | — | — | — |

### `lin_user_money_log`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | — | 0 | — |
| `money` | `decimal(10,2)` | NO | — | 0.00 | — |
| `before` | `decimal(10,2)` | NO | — | 0.00 | — |
| `after` | `decimal(10,2)` | NO | — | 0.00 | — |
| `memo` | `varchar(255)` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |

### `lin_user_preferences`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `user_id` | `int unsigned` | NO | PRI | — | — |
| `order_notifications` | `tinyint` | NO | — | 1 | — |
| `report_notifications` | `tinyint` | NO | — | 1 | — |
| `invoice_notifications` | `tinyint` | NO | — | 1 | — |
| `news_notifications` | `tinyint` | NO | — | 0 | — |
| `preferred_laboratory_id` | `int unsigned` | YES | — | — | — |
| `updated_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |

### `lin_user_role`（新增工作流表）

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `user_id` | `int` | NO | MUL | — | — |
| `role_code` | `varchar(32)` | NO | — | — | — |
| `hospital_id` | `int` | YES | MUL | — | — |
| `laboratory_id` | `int` | YES | MUL | — | — |
| `scope_type` | `varchar(32)` | NO | — | GLOBAL | — |
| `scope_id` | `int` | NO | — | 0 | — |
| `is_active` | `tinyint(1)` | NO | — | 1 | — |
| `created_by` | `int` | YES | — | — | — |
| `created_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| `updated_at` | `datetime(6)` | NO | — | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

### `lin_user_rule`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `pid` | `int` | YES | — | — | — |
| `name` | `varchar(50)` | YES | — | — | — |
| `title` | `varchar(50)` | YES | — | — | — |
| `remark` | `varchar(100)` | YES | — | — | — |
| `ismenu` | `tinyint(1)` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |
| `weigh` | `int` | YES | — | 0 | — |
| `status` | `enum('normal','hidden')` | YES | — | — | — |

### `lin_user_score_log`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int unsigned` | NO | PRI | — | auto_increment |
| `user_id` | `int unsigned` | NO | — | 0 | — |
| `score` | `int` | NO | — | 0 | — |
| `before` | `int` | NO | — | 0 | — |
| `after` | `int` | NO | — | 0 | — |
| `memo` | `varchar(255)` | YES | — | — | — |
| `createtime` | `int` | YES | — | — | — |

### `lin_user_token`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `token` | `varchar(50)` | NO | PRI | — | — |
| `user_id` | `int unsigned` | NO | — | 0 | — |
| `createtime` | `int` | YES | — | — | — |
| `expiretime` | `int` | YES | — | — | — |

### `lin_version`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `int` | NO | PRI | — | auto_increment |
| `oldversion` | `varchar(30)` | YES | — | — | — |
| `newversion` | `varchar(30)` | YES | — | — | — |
| `packagesize` | `varchar(30)` | YES | — | — | — |
| `content` | `varchar(500)` | YES | — | — | — |
| `downloadurl` | `varchar(255)` | YES | — | — | — |
| `enforce` | `tinyint unsigned` | NO | — | 0 | — |
| `createtime` | `int` | YES | — | — | — |
| `updatetime` | `int` | YES | — | — | — |
| `weigh` | `int` | NO | — | 0 | — |
| `status` | `varchar(30)` | YES | — | — | — |

### `lin_wechat_payment`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | — | auto_increment |
| `payment_no` | `varchar(64)` | NO | UNI | — | — |
| `order_id` | `int` | NO | MUL | — | — |
| `user_id` | `int` | NO | MUL | — | — |
| `hospital_id` | `int` | NO | — | — | — |
| `amount` | `decimal(10,2)` | NO | — | — | — |
| `total_fee` | `int` | NO | — | — | — |
| `status` | `tinyint` | NO | MUL | 0 | — |
| `transaction_id` | `varchar(64)` | YES | MUL | — | — |
| `prepay_id` | `varchar(64)` | YES | — | — | — |
| `pay_type` | `varchar(20)` | YES | — | JSAPI | — |
| `openid` | `varchar(128)` | YES | — | — | — |
| `trade_type` | `varchar(20)` | YES | — | — | — |
| `trade_state` | `varchar(20)` | YES | — | — | — |
| `paid_at` | `datetime` | YES | — | — | — |
| `create_at` | `datetime` | NO | — | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| `update_at` | `datetime` | YES | — | — | on update CURRENT_TIMESTAMP |
| `notify_time` | `datetime` | YES | — | — | — |
| `refund_id` | `varchar(64)` | YES | — | — | — |
| `refund_amount` | `decimal(10,2)` | YES | — | — | — |
| `refund_status` | `varchar(20)` | YES | — | — | — |
| `body` | `varchar(255)` | YES | — | — | — |
| `detail` | `varchar(255)` | YES | — | — | — |
| `attach` | `varchar(255)` | YES | — | — | — |
| `out_trade_no` | `varchar(64)` | YES | — | — | — |

### `listsupplyhistory`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `ListSupplyHistoryId` | `int` | NO | PRI | — | — |
| `ListSupplyMaterialId` | `int` | NO | — | — | — |
| `InfoHospitalId` | `int` | NO | — | — | — |
| `DictOutTypeId` | `int` | NO | — | — | — |
| `Count` | `int` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `IsActive` | `int` | NO | — | — | — |
| `IsPay` | `int` | NO | — | — | — |

### `listsupplymaterial`

| 字段 | 类型 | 可空 | 键 | 默认值 | 额外 |
|---|---|---|---|---|---|
| `ListSupplyMaterialId` | `int` | NO | PRI | — | — |
| `SupplyName` | `varchar(50)` | NO | — | — | — |
| `Specifications` | `varchar(50)` | NO | — | — | — |
| `Unit` | `varchar(50)` | NO | — | — | — |
| `Remark` | `longtext` | NO | — | — | — |
| `CreateDt` | `datetime` | NO | — | — | — |
| `IsActive` | `int` | NO | — | — | — |

## 视图

- `vw_diagnosisdetail_all`
- `vw_diagnosisdetail_all2`
- `vw_hospmonthreport`
- `vw_hospmonthreport3_sendmaildt`
- `vw_hospmonthreport3_sendmaildt_copy`
- `vwdiagnosisdetail`
- `vwlistdiagnosisproject`

## 触发器

| 名称 | 表 | 时机 | 事件 |
|---|---|---|---|
| `trg_order_status_change` | `lin_lb_listdiagnosisproject` | AFTER | UPDATE |
