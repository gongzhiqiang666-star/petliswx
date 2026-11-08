# 小程序匿名操作事件

用于 F13 的操作体验排查与事件计数。只请求配置中的 Petlis API，不使用第三方统计 SDK，不连接生产库，不开启定时任务或执行 DDL。

## 事件契约

每条事件仅允许 `id`（随机 UUID v4）、`event`、`step`、`outcome`、`environment` 五个字段。后四个字段为白名单枚举，客户端与后端均校验。

| event | step | outcome | 触发点 |
|---|---|---|---|
| order_step | pet / tests / sample / confirm | view | 四个送检步骤的页面加载 |
| order_submit | confirm | success / failure | 送检提交接口结果 |
| payment_result | checkout | success / pending / cancelled / failure | 支付工具；成功仅在服务端支付状态确认为已支付后记录 |
| report_open | report | success / failure | 报告详情加载结果 |
| registration_submit | registration | success / failure | 注册提交结果，非资质审核结果 |
| auth_result | login | success / failure | 密码或微信登录结果，不含凭据 |

环境为 develop / trial / release。开发 mock 支付事件与正式环境分开统计，不可作为实际资金到账证明。不采集账号、手机号、用户/医院/订单/宠物 ID、位置、设备标识、请求 URL、异常原文、诊断内容、模板内容或任何自由文本。事件号用于重传去重，不是设备、会话或用户标识。

## 发送与降级

- 队列仅保留最近 50 条允许字段；读取本地队列时再次校验，污染条目不会发送。
- 单批最多 20 条，仅有登录态时发送。未登录注册/登录失败事件等待后续登录补传，未再次登录可能永远不上传；不能用这些计数反推全部未登录访客或注册转化率。
- 内存登录态也可发送，不要求“记住登录”。Token 只放在本系统鉴权请求头，不进入事件和本地事件队列。
- 3 秒请求超时；失败静默保留原事件号，30 秒内退避，下次事件或应用前后台切换时重试。失败不弹窗、不清会话、不阻止送检或支付。服务端成功确认后才删除本批队列。
- `ext.json` 的 `analyticsEnabled: false` 可关闭采集；flush 时清空待传队列。没有新增任何后台定时作业。

## 接口与落盘

`POST /api/client-events`，登录鉴权，body `{ "events": [...] }`。先校验整批，含额外字段、非法枚举、非法事件号、空批或超过 20 条均拒绝。复用批准的 `lin_audit_record`：action=CLIENT_EVENT，resource_type=MINIPROGRAM_EVENT，resource_id=事件号，after_json 只存四个枚举字段；身份、机构和 IP 列不填。

重复事件号只落一次。事务显式 REPEATABLE_READ，按已存在的资源复合索引做当前读取/间隙锁，发生死锁整批回滚，由客户端使用原事件号重试；不将死锁失败当作已接收。

`GET /api/admin/client-events/summary?days=7`，仅管理员，范围 1–30 天。按数据库接收日期、事件、步骤、结果和环境分组，返回次数。管理端“数据看板 → 小程序操作事件”展示；不提供身份查询或原始诊断检索。

这些是客户端自报操作次数，不能替代服务端业务审计、独立用户数、同一用户漏斗、真实支付对账或完整注册审核数量。时间按接收时刻，离线补传不回填客户端时刻。生产大流量容量、匿名事件留存期限与归档策略仍需预发布验证，当前未运行清理作业。

## 验证与未完成项

- `npm --prefix miniprogram test`：队列边界、字段白名单、静默退避、确认后移除、污染数据拒发。
- `ClientEventServiceTest`：服务端白名单、批次边界、匿名落盘、重复拒写、管理员范围。
- `python3 scripts/local/test-real-flow-fix.py --client-events`：仅隔离库合成数据，真实 HTTP 并发重放/去重、匿名及越权拒绝、落盘身份列为空、非法批次原子拒绝、管理员汇总增量。
- Chrome 已核对开发环境合成事件在看板显示“送检步骤 / 选择宠物 / 进入 / develop / 1”。

尚待：微信模拟器/真机对各触发点的贯通验收、正式环境隐私文档、弱网压力与留存策略。短信、实际支付和微信订阅的外部验收仍按总计划推进，不因本模块测试通过而视为完成。注册申请状态权威计数见下节；用户本机关闭行为现已补充模块、页面和现有微信模拟器验证，见[使用统计本机开关验证](ANALYTICS-USER-CHOICE-VALIDATION.md)，不代表正式环境/真机全部通过。


## 注册审核业务统计（与匿名操作次数独立，2026-11-07）

新增仅ADMIN可用 `GET /api/admin/dashboard/registrations?from=YYYY-MM-DD&to=YYYY-MM-DD`，时间边界为开始含、结束不含，严格日历校验且1至366天。读取已存在 `lin_doctor_profile` 的真实申请数据，要求 `requested_hospital_id IS NOT NULL`，排除仅为撤销登录凭据而补建的资料行。返回所选提交期间内申请的当前状态计数submittedCount/pendingCount/approvedCount/rejectedCount，及不限日期的全部待审totalPending。不是历史审核操作吞吐量、注册转化率或独立用户埋点；资料行当前状态变化会改变所属提交期间的分类。历史缺少申请医院字段的记录不纳入，并非统计全部历史PHP用户。

PC看板新增独立注册审核区，复用含当天的日期筛选，注册统计与业务看板分别忽略过期成功/失败响应。数据加载失败独立显示错误，不以零伪装。仅从数据库读数据，无新增表/索引/作业。

后端390项（385通过5跳过）、PC87项通过。真实HTTP合成订单23905：同次创建待审/驳回/通过申请，统计与直接数据库查询一致，匿名/医生/实验人员拒绝、错误日历拒绝；全链报告33776发布并下载通过。证据本机私有qa/registration-summary-http.json。统计包含本机开发数据，不能用于代表生产业务量；本轮未完成原生浏览器或真机验收，F13整体仍未完成。
