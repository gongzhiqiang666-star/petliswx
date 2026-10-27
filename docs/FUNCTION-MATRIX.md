# 功能合并矩阵

来源：`宠物医院实验室小程序功能规划.xlsx` 的“现有功能模块”和“缺失补充功能”工作表。F01–F42 与 Excel 的 42 个功能主项一一对应；每行“需求说明”保留该行全部子项，不以模块名代替功能验收。

决策含义：**保留**表示确认版已有入口与契约必须继续工作；**移植**表示从旧 `backend/`、`admin/` 吸收能力到唯一 `petlis-backend/` 与统一管理端；**新增**表示确认版缺少的目标能力。下表是合并目标与验收分工，不表示 Task 1 已实现后续模块。所有接口保持 `/api`、成功 `code=200`、0 基分页、元单位金额和旧 ID；所有支付、微信、短信只用 mock 验收。

| 编号 | 模块 | 功能 | 需求说明（Excel 全量子项） | 处理决策 | 前端位置 | 后端位置 | 验收标准 | 实现负责 | 验收负责 |
|---|---|---|---|---|---|---|---|---|---|
| F01 | 用户模块 | 注册 | 医院选择、医生资质上传、后台审核、账号归属绑定、验证码密码校验、微信授权绑定；账号归医生所有 | 保留+移植 | `miniprogram/pages/register/`；`admin/src/` 审核页 | `controller/api/AuthController`、`DoctorAuthController`、`QualificationController`；统一角色服务 | 必须选医院；mock 验证码/密码校验与微信绑定有效；资质仅授权审核员可见；审核前后归属一致且未审账号不能登录 | Task 2 | Task 6：注册、审核、越权及重复绑定 |
| F02 | 用户模块 | 登录 | 账号密码登录、记住密码、登录锁定、Token会话管理、微信快捷登录 | 保留 | `miniprogram/pages/login/`、`mine/`、`api/index.js`、`utils/request.js` | `AuthController`、`AuthServiceImpl`、`SessionService`、JWT 过滤器 | 密码与微信 mock 登录成功均 `code=200`；记住密码不存明文；连续失败锁定；撤销/登出后旧 Token 不可刷新；refresh/logout 携带 Bearer，退出先撤销服务端会话 | Task 2/5 | Task 1 契约测试；Task 5 认证头测试；Task 6 会话负例 |
| F03 | 用户中心 | 我的 | 个人信息查看、头像职称医院修改、资料重审、修改记录留存、消息通知开关配置 | 保留+移植 | `pages/mine/`、`profile/`、`preferences/` | `UserController`、`QualificationStorage`；`lin_audit_record`、`lin_file` | 修改持久化且归属校验通过；关键资料变更触发重审；修改前后快照留痕；通知开关生效 | Task 2/4 | Task 6：资料、重审、审计与开关 |
| F04 | 用户中心 | 我的积分 | 积分总额展示、收支明细、分类筛选、分页导出 | 保留+移植 | `pages/account/`；`admin/src/` 导出 | `AccountController`、`AccountService`；管理导出适配 | 总额与流水一致；分类与 0 基分页正确；导出范围同筛选且无跨医院数据 | Task 4 | Task 6：汇总、筛选、分页、导出权限 |
| F05 | 用户中心 | 积分商城 | 礼品列表详情、积分兑换、兑换记录、库存校验、收货地址 | 保留+移植 | `pages/mall/`；`admin/src/` 礼品管理 | `MallController`、`MallService`；既有商城主表与幂等服务 | 详情/列表一致；重复兑换幂等；积分与库存原子扣减、不足失败；记录含有效收货地址 | Task 4 | Task 6：重复、并发、不足及归属 |
| F06 | 用户中心 | 钱包 | 余额展示、充值对接支付、充值流水、收支明细、医生账号绑定；钱包归医生 | 保留 | `pages/recharge/`、`transactions/` | `RechargeController`、`RechargeService`、`PaymentServiceImpl` | mock 充值成功才入账；余额=流水汇总；医生维度隔离；重复回调不重复入账，与医院月结分离 | Task 4 | Task 1 支付回归；Task 6 幂等与隔离 |
| F07 | 用户中心 | 我的优惠券 | 券分类展示、详情、下单抵扣校验、有效期规则 | 保留+新增 | `pages/coupons/` 底部详情弹层、`order-preview/` | `CouponController`、`CouponService`、订单计价服务 | 分类和详情一致；详情展示门槛、实验室、有效期和状态；下单再次服务端校验归属、门槛和有效期，金额以元返回，预留抵扣不变 | Task 5 | Task 5 券详情适配；Task 6：边界时间、抵扣与越权 |
| F08 | 送检中心 | 我要送检 | 多实验室切换、独立检测项目库、宠物信息录入、送检单草稿、提交审核、审核后支付、计价规则、送检节点微信模板消息 | 保留+新增 | `pages/order-*/`、`drafts/`、`pets/` | `OrderController`、`DraftController`、`DictionaryController`、计价/通知服务；`lin_order_clinical`、`lin_order_idempotency` | 切换实验室仅见该项目库和价格；草稿可恢复；提交幂等且先审后付；金额复算一致；每节点仅一条 outbox 通知 | Task 3/4/5 | Task 6：全链路、计价、幂等、通知唯一 |
| F09 | 送检中心 | 送检记录 | 多状态筛选、列表分页、详情、报告预览下载、待支付唤起支付、驳回重提、报告生成微信消息 | 保留+移植 | `pages/orders/`、`order-detail/`、`report-view/` | `OrderController`、`ReportController`、`PdfController`；发布同步旧订单/明细 | 筛选和 0 基分页准确；旧 order/detail ID 不变；待审不提前支付；驳回可重提；付费/归属不足无 PDF URL，发布仅一条通知 | Task 3/4 | Task 1 契约；Task 6 权限、状态与通知 |
| F10 | 送检中心 | 发票管理 | 开票信息维护、送检单申请开票、开票记录、电子发票下载、开票完成微信通知 | 保留+移植 | `pages/invoices/`；`admin/src/` 发票处理 | `InvoiceController`、`InvoiceService`、`FileService` | 信息归属正确；仅合格订单可申请；状态流转与记录一致；发票鉴权下载；完成通知不重复 | Task 4 | Task 6：申请、审核、下载与越权 |
| F11 | 资讯中心 | 资讯中心 | 资讯分类列表、图文详情、直播倒计时、活动弹窗、点赞收藏、新资讯/直播预告微信推送 | 保留+移植 | `pages/articles/`、`article-detail/`、`home/` | `ArticleController`、`ArticleService`、通知服务 | 分类/详情/倒计时一致；弹窗频次可控；点赞收藏幂等；发布和预告各产生一次合规 mock 推送 | Task 4 | Task 6：展示、交互、通知去重 |
| F12 | 消息通知 | 微信小程序消息通知 | OpenID绑定、模板配置、全业务推送规则、消息中心已读/未读、推送开关、失败重试、消息日志 | 保留+移植 | `pages/notifications/`、`preferences/`；`admin/src/` 模板日志 | `NotificationController`、`InboxController`、`NotificationServiceImpl`、既有 outbox/log | OpenID 不泄露；规则与开关生效；已读状态持久化；重试幂等；失败原因留日志且不重复业务事件 | Task 4 | Task 6：规则、已读、重试与日志 |
| F13 | 通用基础 | 全局公共配套 | 公共组件、支付SDK、文件上传下载、消息推送底层、权限、埋点、版本更新、异常捕获 | 保留+新增 | `miniprogram/components/`、`config/`、`utils/`、`app.js`、`tests/` | `FileController`、支付/通知适配、统一安全与 `lin_audit_record`、`lin_operational_config` | 确认版 38 页五 Tab 和资源不回归；环境地址无局域网硬编码；仅开发环境 mock 支付；文件鉴权；权限与审计完整；版本/异常不泄露敏感信息 | Task 2/4/5 | Task 5 Node/JS/JSON 检查；Task 1/6：契约、安全与路由回归 |
| F14 | 配送/物流 | 标本揽收 | 医院下单后快递/骑手上门取件 | 新增 | `admin/src/` 配送页；`miniprogram/` 物流时间线 | `ShipmentController/Service`；`lin_shipment` | 审核订单可分配配送员；仅被分配者可揽收；状态和时间线持久化，非法跃迁拒绝 | Task 3 | Task 6：分配、揽收、越权、状态机 |
| F15 | 配送/物流 | 配送追踪 | 标本在途状态、预计到达时间 | 新增 | `miniprogram/pages/orders/`、`order-detail/` 只读物流时间线；`admin/src/` 配送页 | `/logistics` 归属查询 API；`lin_shipment.tracking_no/status/expected_arrived_at` | 在途节点顺序、追踪号和预计到达时间一致；异常可读；仅本医院/授权角色可见；旧订单 ID 不变，医生无内部操作按钮 | Task 3/5 | Task 5 物流适配；Task 6：时间线与跨医院越权 |
| F16 | 配送/物流 | 实验室签收确认 | 实验室扫码/手动确认收样 | 新增 | `admin/src/` 收样页 | 收样 API；`lin_shipment`、`lin_lab_task` | 有效码与手动确认均只能签收一次；签收生成/更新检测任务；重复请求幂等且不重复通知 | Task 3 | Task 6：扫码、重复签收与任务唯一 |
| F17 | 配送/物流 | 异常处理 | 标本破损、丢失、超时未送达等 | 新增 | `admin/src/` 异常处理；小程序状态展示 | 物流异常 API；`lin_shipment`、`lin_audit_record` | 破损/丢失/超时原因必填并留痕；异常可触发退样/退款流程；未授权角色不能处理 | Task 3/4 | Task 6：三类异常、审计与后续退款 |
| F18 | 实验室端 | 标本签收管理 | 接收标本、核对送检单 | 新增 | `admin/src/` 实验室收样 | `LabTaskController/Service`；`lin_lab_task`、旧订单/明细只读核验 | 签收时订单、标本、实验室一致；不匹配拒绝；重复接收幂等并保留操作者时间 | Task 3 | Task 6：匹配、重复与跨实验室 |
| F19 | 实验室端 | 检测任务分配 | 标本分配到具体检测人员/设备 | 新增 | `admin/src/` 任务分配 | 任务分配 API；`lin_lab_task.assigned_to/equipment_code` | 仅本实验室任务可分配；人员/设备与状态可见；改派留版本和审计，非法任务状态拒绝 | Task 3 | Task 6：分配、改派、并发与范围权限 |
| F20 | 实验室端 | 检测结果录入 | 各项指标结果录入 | 新增 | `admin/src/` 结果批次录入 | `TestResultController/Service`；`lin_test_result` | 指标、单位、参考范围与结果完整；批次可追溯；仅分配人员可编辑，已审核批次只读 | Task 3 | Task 6：字段、批次、权限与并发 |
| F21 | 实验室端 | 报告生成与审核 | 生成报告、内部审核、发布 | 新增+保留旧下载 | `admin/src/` 报告审核；`miniprogram/pages/report-*` | `ReportWorkflowController/Service`；`lin_report_workflow`、`lin_file`；同步旧订单/明细 | 中文 PDF 可打开；未审/未支付/越权不可发布或下载；发布同步旧明细与订单；重复发布幂等且通知一次 | Task 3 | Task 6：PDF、支付、归属、同步与幂等 |
| F22 | 实验室端 | 报告模板管理 | 不同检测项目的报告模板 | 新增 | `admin/src/` 模板管理 | 模板 API；`lin_report_template` | 模板按实验室/项目隔离；版本与启停用明确；发布报告固定所用模板版本，不能篡改历史报告 | Task 3 | Task 6：选择、版本、隔离与历史一致 |
| F23 | 实验室端 | 退样/复检处理 | 标本不合格退回、申请复检 | 新增 | `admin/src/` 退样复检；小程序状态展示 | 退样/复检 API；`lin_lab_task`、`lin_test_result`、物流/审计表 | 不合格原因留痕；退样可关联退款；复检生成新任务/批次而不覆盖旧结果；权限与幂等有效 | Task 3/4 | Task 6：退样、复检、新旧结果与退款 |
| F24 | 账单与结算 | 现结流程 | 审核通过后即时支付 | 保留 | `pages/order-detail/`、`utils/payment.js` | `OrderController`、`PaymentController`、`PaymentServiceImpl` | 审核前 `needPayment=false`，审核后 mock 支付；支付回调验签验额；报告支付权限与旧契约不变 | Task 4 | Task 1 契约；Task 6 回调与支付权限 |
| F25 | 账单与结算 | 月结流程 | 挂账消费、月末出账、对账确认、统一结算 | 新增 | `admin/src/` 月结；`miniprogram/pages/bills/`、`bill-detail/` | `Contract/BillController/Service`；`lin_contract`、`lin_monthly_bill*` | 月结订单不唤起现结；挂账额度校验；月末汇总准确；医院管理员可对账，双方确认后统一 mock 结算，状态不可倒退 | Task 4/5 | Task 5 账单 API/权限测试；Task 6：周期、对账、结算、重复请求 |
| F26 | 账单与结算 | 账单生成 | 单次账单/月度汇总账单 | 新增 | `miniprogram/pages/bills/` 列表、`bill-detail/` 明细；`admin/src/` 账单 | 账单生成 API；`lin_monthly_bill`、`lin_monthly_bill_item` | 单订单与月汇总明细金额一致；0 基分页；同一周期重复生成幂等；跨医院订单不混入，元/分转换无精度误差 | Task 4/5 | Task 5 数据适配；Task 6：汇总、幂等、精度与隔离 |
| F27 | 账单与结算 | 欠款管理 | 月结客户未结账单、逾期提醒 | 新增 | `miniprogram/pages/bill-arrears/`、`bills/`；`admin/src/` 欠款 | 欠款查询/提醒服务；`lin_monthly_bill`、通知 outbox | 未结余额和逾期天数准确；到期边界正确；仅医院管理员可见；提醒按规则去重，结算后不再提醒 | Task 4/5 | Task 5 欠款权限入口；Task 6：余额、边界、提醒与结算 |
| F28 | 账单与结算 | 账期配置 | 月结客户账期天数、信用额度 | 新增 | `admin/src/` 签约配置 | 签约配置 API；`lin_contract.billing_cycle_days/credit_amount` | 仅授权管理员配置；账期影响 due_date；超额挂账拒绝；修改有版本和审计且不改历史账单 | Task 4 | Task 6：超额、并发、历史与权限 |
| F29 | 医院签约与定价 | 医院入驻签约 | 医院与实验室建立合作关系 | 新增 | `admin/src/` 签约管理 | `ContractController/Service`；`lin_contract` | 双方 ID 与有效期完整；同效期重复签约拒绝；停用后不可新增挂账，历史订单仍可查 | Task 2/4 | Task 6：签约、重复、停用与历史 |
| F30 | 医院签约与定价 | 结算方式配置 | 签约时设定“现结”或“月结” | 新增 | `admin/src/` 签约管理；小程序支付分支 | `lin_contract.settlement_type`、订单计价/支付服务 | 仅允许 CASH/MONTHLY；订单固定签约结算方式；现结/月结分支互不串用，个人钱包不受影响 | Task 4 | Task 6：两种流程与切换边界 |
| F31 | 医院签约与定价 | 检测项目定价 | 不同实验室、不同项目价格体系 | 新增+保留旧字典 | `admin/src/` 定价；`pages/order-test-items/` | `lin_contract_item_price`、既有项目价格服务 | 实验室/项目命中唯一有效价；服务端计价与展示一致；无有效价格拒单；旧 ID 不变 | Task 4 | Task 6：价格命中、缺失与精度 |
| F32 | 医院签约与定价 | 协议折扣 | 不同医院折扣/优惠协议 | 新增 | `admin/src/` 定价；订单预览 | `lin_contract_item_price.discount_rate/agreed_price`、计价服务 | 医院协议折扣仅作用其订单；范围和有效期正确；折扣/固定价优先级明确；金额精确保留 | Task 4 | Task 6：跨院、有效期、优先级与精度 |
| F33 | 医院签约与定价 | 月结信用额度 | 月结客户额度上限管理 | 新增 | `admin/src/` 签约和欠款 | `lin_contract.credit_amount`、挂账服务 | 可用额度=额度-未结占用；并发挂账不超额；结算释放额度；调额留审计 | Task 4 | Task 6：并发、结算、调额与越权 |
| F34 | 后台管理端 | 审核管理 | 医院入驻审核、医生资质审核 | 移植+新增 | `admin/src/` 审核页 | 审核 API、`ReviewController`、角色权限与审计 | reviewer/admin 可审且普通医生不可；审核意见、状态、操作者留痕；重复审核幂等，结果同步登录权限 | Task 2 | Task 6：两类审核、权限与重复操作 |
| F35 | 后台管理端 | 用户管理 | 医院账号、医生账号管理 | 移植 | `admin/src/` 用户页 | 用户管理 API、`lin_user`、`lin_user_role` | 列表 0 基分页且管理端 `code=200`；账号启停和角色变更生效；撤销会话；不创建重复 users 主表 | Task 2 | Task 1 契约；Task 6 用户、角色、会话 |
| F36 | 后台管理端 | 检测项目管理 | 各实验室项目增删改、上下架 | 移植 | `admin/src/` 项目页 | 项目管理 API、既有 `lin_lb_infotesttype`/价格主表 | 仅本实验室可管理；增删改与上下架生效；历史订单保留原价与名称快照，不重复主表 | Task 4 | Task 6：CRUD、范围与历史订单 |
| F37 | 后台管理端 | 数据统计看板 | 送检量、收入、医院排名等 | 移植+新增 | `admin/src/` 看板 | dashboard 聚合 API、统一旧订单/支付/账单主数据 | 指标口径和时间筛选一致；收入不重复计钱包与月结；医院排名正确；角色仅见授权范围 | Task 4 | Task 6：合成数据聚合、筛选与隔离 |
| F38 | 后台管理端 | 运营配置 | 优惠券发放、积分规则、资讯发布 | 移植+新增 | `admin/src/` 运营页 | 运营 API、`CouponService`、`ArticleService`；`lin_operational_config` | 发放幂等且有效期合法；积分规则版本化；资讯发布触发一次通知；配置修改留操作者和版本 | Task 4 | Task 6：发放、规则、发布与审计 |
| F39 | 其他细节 | 宠物档案 | 宠物基本信息、病史、既往送检记录 | 保留+新增 | `pages/pets/`、`pet-detail/` 健康入口、`pet-health/` 真实病史与报告空态 | `PetController`、`PetHealthService`；`lin_order_clinical`、旧订单/明细只读关联 | 基本资料归属隔离；按 petId 加载真实病史；既往报告来自旧订单明细且 ID 不变；0 基分页契约稳定，无静态假数据 | Task 5 | Task 5 服务/Controller/页面测试；Task 1 契约；Task 6 病史、记录与越权 |
| F40 | 其他细节 | 打印功能 | 送检单打印、标本标签打印、报告打印 | 移植+新增 | `admin/src/` 打印入口与打印视图 | print/export API、PDF/文件服务、角色权限与 `lin_audit_record` | 三类打印内容含正确旧 ID；标签可扫码；中文报告不乱码；仅授权角色可打印并留审计，无公开文件 URL | Task 4 | Task 6：三模板、扫码、中文与权限 |
| F41 | 其他细节 | 退款流程 | 支付后退款、标本不合格退款等 | 移植+新增 | `admin/src/` 退款审核；小程序状态展示 | refund API、mock 支付网关、订单/账单服务、`lin_audit_record` | 仅已付且符合条件的订单可退；金额不超实付；重复退款幂等；月结/现结补偿分离；越权拒绝并留审计 | Task 4 | Task 6：条件、金额、重复、权限与月结 |
| F42 | 其他细节 | 角色权限 | 医生、医院管理员、实验室人员、配送员、平台管理员权限 | 新增+保留 JWT | `admin/src/` 按角色动作；小程序保持医生流程 | `lin_user_role`、资源授权服务、安全过滤器；兼容 reviewer | 五角色最小权限；跨医院/实验室/配送任务均拒绝；撤销用户 Token 失效；管理响应不改变小程序契约 | Task 2 | Task 6：全角色正例、越权与会话撤销 |

## 全局验收与数据边界

- 唯一主数据：`lin_user`、`lin_lb_infohospital`、`lin_lb_pet`、`lin_lb_listdiagnosisproject`、`lin_lb_diagnosisdetail`；禁止 `users`、`test_orders` 等重复主表、双写、旧迁移重放和旧数据回填。
- 新工作流表只位于 `petlis-backend/src/main/resources/db/platform/`，默认不执行；仅在显式确认的隔离 MySQL 副本手工运行，并用 `integration/schema_guard.py` 比较执行前后旧表列、索引、触发器。
- 小程序确认版 38 页、五 Tab、组件和资源全量回归；62 个 JS 语法和全部 JSON 解析检查通过。管理端通过适配层复用 `Result` 与 0 基分页。
- 财务使用 `BigDecimal`/整数分，接口边界保持元；退款、支付、 redemption、账单、签收和发布均验证幂等、归属与并发。
- Task 6 按 F01–F42 逐项合成数据验收；真实微信、支付、短信、订阅消息不纳入无凭据通过声明。
