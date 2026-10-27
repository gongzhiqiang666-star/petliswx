# Task 3：物流、实验室与报告工作流合并报告

## 状态

已完成统一后台真实模块及管理端接入。实现仅使用 Task 1 新增的独立工作流表和既有旧订单、检测明细、报告内容、通知 outbox；未修改或重放旧表结构、旧迁移、触发器，未连接 Mac mini。

## 实现内容

- `/logistics`：管理员创建和分配运单；快递员仅能操作本人运单；实验人员仅能签收本实验室运单；医院与医生按归属读取。完整支持 `PENDING_PICKUP → PICKED_UP → IN_TRANSIT → DELIVERED → RECEIVED`、预计到达、异常与审计时间线，签收同事务创建初始实验任务。
- `/lab/tasks`：本实验室人员和设备分配、完整项目结果录入、数值范围异常判断、定性结果显式异常标记、不可变批次、退样和复检新任务。退样作废旧结果并取消未发布报告，复检必须重新录入新批次。
- `/report-workflows`：按旧 `InfoTestTypeId` 和实验室选择版本化模板，生成、审核、发布及鉴权下载。PDFBox 生成包含全部项目模板和结果的中文 PDF。发布事务同步 `lin_lb_diagnosisdetail_info`、`PDFHttpUrl`、`DiagnosisStatus` 和旧订单 `SampleStatus=3`，保留确认版报告 GET/PDF 路径。
- 报告权限：现结未支付禁止发布且医院不可见；月结无需现付；未发布及外院不可见；作废结果不能发布；重复生成和非法状态被拒绝。
- 订单扩展：旧订单创建接入 `lin_order_clinical` 与 `lin_order_idempotency`，支持 `Idempotency-Key`，不创建第二套订单。
- 通知：复用 `lin_notification_event` 和既有 outbox。物流与实验事件按业务键去重并兼容无创建人的旧订单；报告发布复用 `notifications.order(..., 3)` 的 `legacy:{id}` 键，避免与触发器通知桥重复。
- 文件：仅新增 `lin_file` 元数据，随机文件名、PDF/PNG/JPEG 魔数、大小、摘要、归属鉴权、报告专用权限、规范化及真实路径检查。
- admin：物流、实验、报告页面均接真实 API，移除“待接入”；严格按角色展示创建、分配、快递操作、签收、结果、审核、发布与下载按钮；保持 `code=200` 和 0 基分页。

## 验证

- 后端全量：195 项，0 失败，0 错误，5 跳过；原 152 项保持通过。
- schema：`PlatformMigrationGuardTest` 9 项全部通过。
- admin：`npm test` 78 项全部通过。
- 中文 PDF：通过 PDFBox 实际生成并提取文本，校验中文标题、摘要、结论、全部模板和结果。
- `git diff --check` 通过；DDL、旧迁移与 `integration/db` 无差异。

## 验证边界 / concerns

- 本次未执行任何 DDL，未连接外部 MySQL 或 Mac mini；真实 MySQL 副本上的跨角色全链路仍属 Task 6 集成验收范围。
- 当前验证为后端单元/MVC 契约测试、真实 PDF 文件测试和管理端 Node 测试；未宣称真实浏览器或生产数据库端到端通过。
