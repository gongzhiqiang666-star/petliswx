# 真实环境送检流程修复报告

## 修复内容

- `FeatureStore.insert` 仍只执行一次 INSERT；优先读取 JDBC generated keys，缺失时在同一个 Connection 执行 `SELECT LAST_INSERT_ID()`，避免 MySQL 9.3 下保存成功但无法取得主键，也不以重插作为回退。
- 旧月结订单仅当定价快照关联有效合同记录时执行信用额度校验；无合同的旧医院继续按原月结规则挂账，有合同仍调用 `reserveCredit`。
- 实验人员列表改用真实 `lin_user.username` 字段。
- 字典继续由 `InfoAnimalRepository.findAllActive()` 过滤禁用项。本机 laboklin 副本仅将猫 `InfoAnimalId=6`、犬 `InfoAnimalId=7` 的 `IsActive` 改为 1，未修改其他字典、旧表结构或触发器。
- 创建和更新订单的 `masterNameEn` 改为可选；创建、驳回重提和更新时将 null 归一为空字符串，兼容旧列 `NOT NULL` 约束。小程序同步移除必填校验和必填标记。
- 报告发布写入 `/api/pdf/view/{detailId}`；旧 PDF 服务通过报告工作流与 `lin_file` 元数据解析实际文件，保留旧 `/Content/FilePdf` 兼容。发布响应返回 `PUBLISHED`。
- report-modal 的预览与下载请求均携带 Bearer 认证。

## 自动化验证

- TDD：主键回退、无合同/有合同月结、username、仅中文畜主、null 持久化、新 PDF 路径、发布状态和 report-modal 均先观察失败，再实现并转绿。
- `petlis-backend/./mvnw test`：318 项，0 failure，0 error，5 skipped（未提供专项数据库参数的测试按设计跳过）。
- 真实 MySQL 9.3：`MySqlRealFlowCompatibilityIT` 2/2 通过；实际 `lin_lb_infotesttype` 插入回读主键且只新增一行，事务回滚；assignees 在真实 schema 执行成功。
- `admin/npm test`：81/81 通过。
- `miniprogram/npm test`：27/27 通过；`npm run check` 通过（68 JS、54 JSON）。
- `integration/python3 -m unittest discover -s tests -v`：11/11 通过。
- 隔离 MySQL 8.4 全链 `run-merge-e2e.sh`：全部 PASS；39 张旧表的选项、列、索引、外键、CHECK 约束和触发器前后均未变化，无非白名单新增表。
- `git diff --check`：通过。

## 本机 laboklin 副本 API 全链

最终唯一测试前缀：`QA_REAL_5AD726F2`。未手工补合同、定价快照或业务状态，未使用真实支付。

1. AdminCatalog 新增项目成功，项目 ID `828`，数据库仅一条对应项目。
2. 猫犬字典均可见且仅返回启用项；使用猫 id=6 创建宠物 ID `118`。
3. 仅中文畜主成功创建订单 `23842`；无合同关联的定价快照。
4. 旧月结审核成功，订单进入等待报告状态，未因无合同报 404。
5. 运单 `2` 完成揽收、运输、送达、扫码签收。
6. 实验人员接口返回 `username`；任务 `2` 分派并录入结果。
7. 报告工作流 `2` 完成生成、审核、发布；发布响应为 `PUBLISHED`，报告明细 ID `33709`。
8. 报告列表和详情可见；`PDFHttpUrl=/api/pdf/view/33709`。
9. `/reports/33709/pdf-url` 正常返回；`/reports/33709/download`、`/pdf/view/33709`、`/report-workflows/2/download` 均在医生鉴权下返回 `%PDF` 字节；匿名 `/pdf/view/33709` 被拒绝。

复现脚本：`scripts/local/test-real-flow-fix.py`。

## 剩余限制

- 验证范围为本机真实 laboklin 副本与隔离 MySQL，不连接或修改 Mac mini 原库。
- 未进行微信真机 UI 操作；report-modal 通过代码回归验证认证头，PDF 端到端通过真实 HTTP 鉴权与文件字节验证。
- 未执行真实支付、数据库结构变更或旧触发器变更。
