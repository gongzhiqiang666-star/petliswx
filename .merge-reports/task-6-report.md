# Task 6：跨端集成验收报告

日期：2026-10-28
分支：`feat/merge-confirmed-miniprogram`
范围：唯一 `petlis-backend`、统一管理端、确认版小程序与隔离 MySQL 8.4。未分派、未连接 Mac mini、未修改旧 schema/旧迁移/旧触发器、未执行真实支付、未 push、未创建 PR。

## 交付

- `integration/run-merge-e2e.sh`：可重复初始化本机 MySQL 8.4.13 临时副本，构造 39 张合成旧表、基础数据和 1 个旧触发器，保存完整 schema 基线，连续两次手工执行批准的 16 张平台新表 DDL，再启动唯一后端和 admin。结束后关闭进程。
- `integration/merge-e2e.js`：真实 HTTP 跨角色主流程与负例；Node 只模拟 `wx` 传输、存储和导航，实际加载 `miniprogram/utils/request.js`、`api/index.js` 及相关页面。真实 `pages/login/login.js submit()` 获取会话，不是单独 fetch 冒充小程序。
- `integration/db/legacy-fixture.sql`、`platform-seed.sql`：仅合成隔离数据；旧基线不包含平台表。
- `integration/tests/test_merge_e2e_contract.py`：入口、隔离连接、真实小程序/Playwright 驱动守护；初跑因文件缺失 RED，实现后 GREEN。
- `integration/results/`：`merge-e2e.json`、schema 快照与校验日志、后端/admin 日志、8 张浏览器截图、PDF、中文提取文本、微信 CLI 日志和预览图。运行产物按需 gitignore。
- `README.md` 统一单后端/单库启动和全部测试入口；`docs/FUNCTION-MATRIX.md` 逐项登记 F01–F42 实现和实测状态，未完整实测子项标“部分”。

## 实测主流程

`integration/run-merge-e2e.sh` 最终退出 0，`merge-e2e.json.status=PASS`，10 个主步骤全部 PASS：

1. 真实资质 multipart 上传、医生注册、未审拒登、审核、密码登录。
2. 多实验室项目、CASH/MONTHLY 合同、固定协议价 75 元；优惠券发放重放及 65 元核价。
3. 小程序真实登录页、宠物档案、草稿、送检、记录、订单创建重放；审核后 mock 支付，重复交易号回调拒绝且旧支付记录只入账一次。
4. 配送分配/取件/运输/送达/扫码签收及重复签收；实验任务分配、结果、退样、复检继承人员设备、新批次。
5. 未发布报告无 PDF 且下载拒绝；审核发布、重复发布、鉴权下载；`pdftotext` 实际提取出“中文摘要：指标稳定”“中文结论：未见异常”和中文指标。
6. 未支付、月结、已开票退款边界；另一现结订单成功 mock 退款并幂等重放。
7. 发票抬头、申请、开具。
8. 历史月账单生成、重复生成、双方对账、统一 mock 结算及重放；补录订单生成 SUPPLEMENT 并独立对账结算；小程序账单查询。
9. 积分调整、商城重复兑换、资讯发布/互动、消息列表；报告发布事件按业务键计数恰好 1。
10. 送检单/标本标签/中文报告打印、看板、跨医院和员工越权、41 注册页面和 5 Tab。

最终 `schema-final.log` 再次确认：39 张旧表的表选项、列、索引、外键、CHECK 约束和触发器均未变化，且无非白名单新增表。

## 小程序与管理端

- 实际加载页面：`login`、`pets`、`order-preview`、`orders`、`order-detail`、`report-list`、`pet-health`、`bills`、`coupons`；通过确认版 request/service 覆盖登录、档案、送检、记录、物流、报告、病史、账单、优惠券。
- Playwright 使用本机 Chrome 真实浏览器登录，执行注册审核和订单审核，并进入物流、实验、报告、财务、运营和打印相关页面。保存 8 张截图；`pageerror=[]`，意外 console error=0。仅记录一条已明确忽略的静态 `favicon.ico` 404，不属于业务错误。
- `app.json` 实测 41 个注册页面、5 个 Tab；静态检查 68 个 JS、52 个 JSON 全通过。

## 集成发现的问题与 TDD 修复

- `PrintService.report` 使用 `i.DiagnosisdetailId`，与既有实体和发布写入的 `diagnosisdetail_id` 不一致，真实 MySQL 打印返回 500。先新增 `reportPrintUsesTheExistingLowercaseDetailInfoKey`，观察 Mockito SQL 不匹配 RED；改为既有小写列名后定向及全链路 GREEN。
- `BusinessNotifications.article` 的 `INSERT…SELECT … ON DUPLICATE KEY UPDATE id=id` 在真实 MySQL 中列名歧义。先新增资讯 outbox 守护 RED；最终使用 `LAST_INSERT_ID(lin_notification_event.id)` 明确目标表，真实发布及通知去重 GREEN。
- 功能矩阵增加 42 行实测状态后，原守护把两张表合计为 84 而 RED；将需求计数限定原矩阵区，同时新增状态区也必须 42 行，最终 GREEN。

## 最终验证

| 项目 | 结果 |
|---|---|
| `petlis-backend ./mvnw test` | **294 tests，0 failures，0 errors，5 skipped，BUILD SUCCESS**；5 项均为既有本地数据库测试 |
| `admin npm test` | **83/83 PASS** |
| `miniprogram npm test` | **25/25 PASS** |
| `miniprogram npm run check` | **68 JS、52 JSON PASS** |
| `integration python3 -m unittest discover -s tests -v` | **11/11 PASS**（原 schema 9 + 集成入口 2） |
| `integration/run-merge-e2e.sh` | **PASS，退出 0**；MySQL 8.4.13，39 旧表 schema 前后不变 |
| `integration/run-mysql-publish-concurrency.sh` | **PASS，退出 0**；MySQL 8.4.13 / REPEATABLE-READ 并发发布专项 |
| 微信 DevTools CLI | `open` 与 `preview` 均成功；AppID `wx30c14324c89e9151`，preview 总大小 1,304,106 bytes，日志和 QR 已保存 |
| `git diff --check` | PASS |

## Concerns / 未宣称通过

- 真实微信登录/绑定、真实短信、订阅消息、真实支付/退款、生产数据库与真机未验收；全部外部动作均为合成或 mock。
- F02、F03、F04、F06、F10、F12、F17、F22、F27、F28、F29、F33、F35、F36、F38 的部分子项仍仅由单元/MVC/Node 测试覆盖，详见功能矩阵，未标为全量实测通过。
- 集成旧结构为 39 张合成旧表，不是 Mac mini 全量脱敏结构副本；未读取或修改原库。
- 浏览器 favicon 404 已单列；无意外 console/pageerror。
