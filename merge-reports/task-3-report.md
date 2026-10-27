# Task 3：物流、实验室与报告工作流合并报告

## 状态

已完成统一后台真实模块及管理端接入。实现仅使用 Task 1 新增的独立工作流表和既有旧订单、检测明细、报告内容、通知 outbox；未修改或重放旧表结构、旧迁移、触发器，未连接 Mac mini。

## 实现内容

- `/logistics`：仅审核通过订单可由管理员创建和分配运单；快递员仅能操作本人运单；实验人员仅能签收本实验室运单；医院与医生按归属读取。完整状态、预计到达、异常与审计时间线持久化，签收创建初始实验任务。重复签收幂等返回，扫码签收核验旧订单 `UserNO`。
- `/lab/tasks`：仅 `assigned_to` 人员可录入结果；完整项目校验、不可变批次、退样和复检新任务。退样作废旧结果并取消未发布报告；同请求复检返回原任务，变更原因冲突。
- `/report-workflows`：按旧 `InfoTestTypeId` 选择版本化模板，生成、审核、发布及鉴权下载。每个明细 PDF 和旧正文只含本项目结果，支持多行摘要、结论和模板自动换行。全部有效明细发布后才同步整单 `SampleStatus=3` 和任务 `COMPLETED`；单明细驳回不倒退共享任务；发布明细 `DiagnosisStatus=2`。
- 报告支付与权限：发布、列表、确认版 GET 和 PDF 均优先使用 `lin_order_extension.settlement_mode` 快照，缺失才回退医院旧规则；现结未支付不可见，月结不受医院当前 `PayType` 变化影响。
- PDF 契约：旧 `PDFHttpUrl` 保存绝对存储地址；`/reports/{id}/pdf-url` 返回带 token 的绝对 `/api/pdf/view/{id}`，`/reports/{id}/download` 与 `/pdf/view/{id}` 统一走鉴权本地存储解析，report-modal 查询 token 可用。
- 幂等与通知：重复发布幂等返回且无新增写入/通知；订单创建幂等先于草稿校验，响应丢失重放返回原 orderId。发布通知使用稳定 `orderId:published`，触发器桥复用同键，无 10 秒窗口依赖；桥接导入前识别同订单主动发布事件并跳过 legacy 重复。
- 并发恢复：事务首读使用联接当前读且仅锁共享订单（`FOR UPDATE OF o`），任务仅锁任务，聚合计数使用 `FOR UPDATE` 当前读；角色等普通查询不得提前建立 RR 快照。首次发布和已发布重试共用聚合完成路径，避免 workflow 与订单锁顺序环；重试可补齐缺失状态，已完整时不重复写入或通知。
- 兼容与文件：物流、实验、报告路径均兼容 `CreateSampleId=null` 并回退本院用户。`QUALIFICATION` 仅本人、授权 reviewer 或 ADMIN 可下载；文件仍仅存 `lin_file` 元数据并校验魔数、大小和真实路径。
- admin：物流、实验、报告页面均接真实 API，保持 `code=200` 和 0 基分页。

## 验证

- 12 项审查均先添加并运行失败反例，再实现并运行对应绿色回归。
- 后端全量：230 项，0 失败，0 错误，5 跳过；原 152 项保持通过。
- MySQL 8.4 专项：在隔离 MySQL 8.4.13、REPEATABLE-READ 副本中，两事务首次并发发布最后两个明细即得到整单 `SampleStatus=3` 和共享任务 `COMPLETED`，未依赖重试补偿；测试脚本 `integration/run-mysql-publish-concurrency.sh` 自动初始化、关闭并清理临时实例。
- schema：`PlatformMigrationGuardTest` 9 项全部通过。
- admin：`npm test` 78 项全部通过。
- 中文 PDF：实际生成并提取文本，覆盖中文、多行摘要/结论/模板及每明细独立结果。
- `git diff --check` 通过；DDL、旧迁移与 `integration/db` 无差异。

## 验证边界 / concerns

- 未修改旧 schema/触发器，仅在隔离 MySQL 8.4 副本创建最小测试表；未连接外部 MySQL 或 Mac mini。完整跨角色业务全链路仍属 Task 6 集成验收范围。
- 当前验证为后端单元/MVC 契约测试、真实 PDF 文件测试和管理端 Node 测试；未宣称真实浏览器或生产数据库端到端通过。
