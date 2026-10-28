# 确认版小程序与全功能后台合并设计

## 目标与约束

以 `petliswx/001-vet-lab-system@70de173` 为基线，确认版小程序为唯一小程序，统一 `petlis-backend/` 为唯一运行后台，吸收此前 `backend/` 的物流、实验室、报告、财务、权限与 `admin/` 管理能力，覆盖根目录功能规划 Excel。

共用现有 MySQL 单库和一套 `lin_*` 主表。已有表、触发器、历史迁移不修改、不重放、不回填；禁止双写及第二套同义主表。所有数据库验证只在隔离副本，禁止写 Mac mini 原库。新能力只新增缺失的独立工作流表，DDL 默认关闭。

## 方案

保留既有后台宿主及小程序契约，按模块移植此前后台实现。反向替换宿主会重写所有旧 ID、支付、分页和 `lin_*` 数据访问；双后台会双写，均排除。

- 小程序成功 `code=200`、`/api`、0 基分页、Spring Page/`items` 两种结构、元单位金额和旧订单/报告 ID 不变。
- 管理端通过适配层使用同一 `Result` 契约，不能为管理端改变小程序响应。
- 旧 `SampleStatus` 保留粗粒度状态；细物流、检测、报告状态存新工作流表。
- 金额内部用 `BigDecimal`/整数分，边界显式转换；不使用 `Double` 做新财务计算。
- 新表外键列匹配旧 ID 类型；跨新旧表外键单独评审，不盲目添加。

## 权威模型

用户 `lin_user`、医院 `lin_lb_infohospital`、宠物 `lin_lb_pet`、订单 `lin_lb_listdiagnosisproject`、检测明细 `lin_lb_diagnosisdetail` 保持唯一。既有实验室、优惠券、充值、商城、发票、资讯、消息 outbox 继续复用。

可新增：`lin_user_role`、`lin_shipment`、`lin_lab_task`、`lin_test_result`、`lin_report_workflow`、`lin_report_template`、`lin_order_clinical`、`lin_order_idempotency`、`lin_contract`、`lin_contract_item_price`、`lin_monthly_bill`、`lin_monthly_bill_item`、`lin_file`、`lin_audit_record`、`lin_operational_config`。执行前必须核验副本不存在；只 `CREATE TABLE`，不改旧表。

## 功能合并

1. 保留确认版 38 页、五 Tab、全部组件资源和真实下单/支付/报告/商城/发票/资讯/消息功能。
2. 保留既有 JWT、会话撤销、短信/微信适配；新增独立角色映射和资源权限，reviewer 兼容管理审核。
3. 新增物流配送、实验任务、结果批次、退样复检、报告审核发布及中文 PDF。发布同步旧明细与旧订单，继续走鉴权下载。
4. 新增签约定价、月结账单、对账结算、信用额度、欠款逾期；个人钱包与医院月结严格分离。
5. 保留既有支付充值、优惠券、积分、商城、发票、资讯、消息，移植管理、退款、取消补偿、审计和看板。
6. 移植 Web 管理端，增加打印与数据导出。
7. 小程序只适配环境地址、会话缺陷并补宠物病史、优惠券详情、账单、物流缺口；视觉遵循确认版设计系统。

## 验收

每模块先失败测试再实现，运行原 103 项及新增后端、管理端、小程序测试。MySQL 8.4 副本验证 DDL 和跨角色全链路，确认版真实请求模块进入真实 HTTP。验证越权、幂等、并发、旧 schema 不变、中文 PDF 和报告支付权限。

真实微信登录、支付、短信、订阅消息只用 mock 验证；无合法凭据及开发者工具成功编译证据时不宣称实网/真机通过。提交与 PR 全中文，目标分支 `001-vet-lab-system`。
