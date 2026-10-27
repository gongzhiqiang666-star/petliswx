# Task 4：签约、月结与运营收尾报告

日期：2026-10-28
分支：`feat/merge-confirmed-miniprogram`
范围：仅 Task 4；未分派、未 push，未复制参考 `backend/` 主表或迁移，未修改旧 schema、旧迁移、触发器及 Task 1 平台 DDL，未执行真实支付或连接 Mac mini。

## 交付物

- **签约与核价**：新增 `ContractService` 与 `/contracts`、`/admin/contracts`，使用 Task 1 已批准的 `lin_contract`、`lin_contract_item_price`。支持 CASH/MONTHLY、协议折扣、固定协议价优先、账期、信用额度、版本审计。既有 `CouponService` 核价接入医院/实验室有效合同，旧 API 金额继续以元返回；无合同医院保留旧价与旧医院 PayType 行为。订单明细和 `lin_order_extension` 保留价格、优惠与结算方式快照，审核月结时行锁校验信用额度。
- **月结账单**：新增 `MonthlyBillService` 与 `/bills`、`/admin/bills`，使用 `lin_monthly_bill`、`lin_monthly_bill_item`。按医院/实验室/月生成且合同锁内复查防重，保存明细定价快照和精确优惠分摊；双方分别确认、异议、统一 mock 结算、重复结算幂等，结算后旧明细转为已结并由信用占用查询排除已结账单。列表/详情/明细/欠款/逾期提醒均鉴权，普通医生不能管理医院账单，个人钱包不参与月结。
- **现结 mock 退款**：新增 `/admin/refunds/{orderId}`，仅 mock 模式、已支付现结合法状态、归属角色可操作；不发真实退款请求。已申请/开具发票明确拒绝，行锁内二次检查审计幂等；退款后旧支付标记退款、订单支付标记撤销，既有报告服务按订单结算快照拒绝现结未付访问。
- **既有运营能力**：继续使用旧 `lin_coupon`、`lin_user_coupon`、`lin_gift`、`lin_redemption`、`lin_user_score_log`、发票、资讯和通知/outbox 主表。补齐券模板 CRUD、库存配置、锁内幂等发放、积分版本化配置和调整、兑换取消退积分回库存；库存编辑不清空已发数量。资讯发布、发票申请/开具/驳回、礼品兑换/取消、消息新旧重试均写 `lin_audit_record`，发票历史 profile snapshot 不改写。
- **看板、配置、导出与打印**：`/admin/dashboard` 真实聚合旧订单、现结支付、已结算月结、医院和项目排行，现金按支付时间、月结按结算时间且不重复计钱包；`/admin/operations` 提供版本化配置、积分调整和审计；订单 CSV 导出防公式注入。送检单、Code 39 标本标签和报告提供鉴权 HTML 打印接口，保留旧 ID、中文内容并写审计；未发布及退款现结报告禁止打印。
- **管理端**：看板、签约项目、财务、运营、系统配置、订单和报告页面全部使用统一后端真实 API，金额元字段和 0 基分页一致；财务只显示统一 mock 结算，不再出现钱包扣款或“待接入”。订单提供送检单/标签打印，报告提供鉴权打印，按钮与 ADMIN/LAB_STAFF/HOSPITAL_ADMIN 等后端角色一致。

## RED / GREEN 记录

- 基线：后端 228 项、管理端 78 项均通过。
- 签约/月结：先因 `ContractService`、`MonthlyBillService` 与 API 不存在编译 RED；实现后转 GREEN。后续补零金额分摊、双方确认、重复生成、结算幂等、越权与额度边界。
- 核价接入：`CouponServiceContractTest` 先因合同字段和新构造 RED；接入后验证固定价优先、折扣精度、合同结算快照及既有订单回归。
- 退款/运营：`RefundServiceTest`、`OperationsCompensationTest`、`CouponAdminOperationsTest` 先 RED，覆盖 mock 限定、发票边界、重复请求、库存不足、兑换补偿、积分配置。锁内二次幂等与库存编辑测试分别复现并修复并发复查、已发数量清零问题。
- 审计：先验证资讯、发票、商城、消息重试缺审计 RED，再补齐；发票测试断言不重写历史快照。
- 看板/打印/管理支撑：先因服务和控制器不存在 RED；实现后覆盖真实主表聚合、CSV 注入、旧 ID、中文、Code 39、打印权限。自审补收入时间口径与未发布报告打印 RED 后修复。
- 管理端：`task4-pages.test.js` 先因旧参考接口、分金额和“待接入”RED；页面替换后修复签约页语法并将旧角色断言更新为与后端一致。
- 最终自审：无可用分派工具，按执行计划进行作者自审；修复零金额除零、统计口径和打印状态后再次全量回归。

## 最终验证

- `cd petlis-backend && mvn test`：**276 tests，0 failures，0 errors，5 skipped，BUILD SUCCESS**。5 项均为既有 `RechargeLocalDatabaseTest` 跳过，无新增跳过。
- `cd admin && npm test`：**82 tests，82 pass，0 fail**。
- `cd integration && python3 -m unittest discover -s tests -v`：**9/9 通过**；`python3 -m py_compile schema_guard.py` 通过。
- `git diff --check` 通过；旧迁移与平台 DDL 无差异。

## 审查 2 修复（2 Critical + 5 Important）

- 每项先新增反例并观察失败：当前/未来账期、生成后补单、已确认账单调整、快照缺失、越权重放、无请求体发券、未逾期/空账单提醒、目录停用和 admin 默认库存均先 RED 后 GREEN。
- 月账单禁止未结束账期；未对账账单锁内重算，已确认/已结算账单对补录订单追加审计调整并重新对账，不再静默漏单。逾期提醒要求真实到期且未结清，空账单返回明确业务错误。
- 新增独立工作流快照表 `lin_order_pricing_snapshot`（仅追加 CREATE TABLE，未执行 DDL），订单创建/重提保存 contractId、合同项目价 ID、结算和项目价格 JSON。信用占用与账项均读订单快照，不随合同医院、实验室、结算或价格修改而重算历史。
- 退款先校验订单归属再读幂等/状态；无请求体旧发券兼容入口已移除，统一要求 requestKey、库存和审计。签约核价及协议价保存都校验实验室目录和项目 active。
- admin 券列表和编辑表单读取、展示真实 totalQuantity/issuedQuantity，不再默认以 1 覆盖库存。

## 审查 2 最终验证

- `cd petlis-backend && mvn test`：**285 tests，0 failures，0 errors，5 skipped，BUILD SUCCESS**。
- `cd admin && npm test`：**83 tests，83 pass，0 fail**。
- `cd integration && python3 -m unittest discover -s tests -v`：**9/9 通过**；`python3 -m py_compile schema_guard.py` 通过。
- `git diff --check` 通过；旧迁移和旧表未改，平台 DDL 仅新增订单定价快照表及对应 schema 白名单守护。

## 注意事项

- 本阶段使用 JDBC mock、MockMvc 与管理端 Node 契约测试，没有执行真实 MySQL 全链路 HTTP、浏览器 Playwright 或微信开发者工具；这些属于 Task 6。
- 未在数据库执行 DDL；运行环境须先按 Task 1 流程在合规副本/目标库准备已批准的平台表。
- 个人钱包与医院月结保持分离，所有退款和月结结算只执行 mock 逻辑。
