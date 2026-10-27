# Task 2：统一角色与 Web 管理端基础报告

日期：2026-10-27
分支：`feat/merge-confirmed-miniprogram`
范围：仅 Task 2；未分派、未 push；未复制参考 `backend/` 的主表或迁移，未修改旧表、旧迁移、触发器及 Task 1 平台 DDL。

## 交付物

- 保留既有 JWT、密码/微信登录、`lin_user`、`code=200`、登录失败锁定和 credential version 会话撤销。
- 使用 Task 1 `lin_user_role` 实现 `DOCTOR/HOSPITAL_ADMIN/LAB_STAFF/COURIER/ADMIN` 多角色及医院、实验室归属。角色在每次请求时从数据库读取，不信任客户端角色，不把用户 ID 硬编码为长期权限；旧 `app.review.admin-user-ids` 仅保留 reviewer 兼容。
- 新增 `CurrentActor/DataScope/AccessService/UserRoleService`：支持角色组合、ADMIN 全量、医院角色本院、实验人员本实验室、快递员仅本人分配及无映射旧医生本院兼容；`AdminService` 管理边界实际调用 `AccessService.requireAdmin`。
- 旧 `ROLE_USER` 仅授予映射 DOCTOR 或无映射旧医生；纯 COURIER/LAB_STAFF 不再访问 pets/orders/reports/wallet 等旧医生接口。非医生员工使用 staff JWT，无医院也可登录并保留其工作角色，DOCTOR/HOSPITAL_ADMIN 仍要求医院和城市。
- 停用、注册/资料驳回及角色变更撤销旧会话；资料驳回通过最小 `lin_doctor_profile` upsert 递增 credential version，无 profile 的旧医生也会在驳回后立即失去旧 token。注册和资料变更的旧 `/review` 路径同样在 service 层禁止自审，不能绕过 AdminService。
- 医院停用立即阻断该院用户登录和旧 token；医院停启均递增全院用户 credential version，重新启用不会复活旧 token。
- 新增真实 `/admin/users`、`/admin/hospitals`、角色分配/移除、停用启用和审核 API；复用既有 `/review/registrations`、`/review/profile-changes`、`/review/orders`、`/review/invoices` 与发票开具/驳回。全部返回旧 `Result` 和 0 基分页。
- 移植 `admin/` 全部源文件，统一密码登录使用 `account/password`，采用服务端返回的用户和角色；API 仅接受 `code=200`，分页统一 0 基，既有审核金额按元显示。
- 管理端真实可用登录、审核、用户医院、角色归属和停用启用；REVIEWER 不显示 ADMIN 专属医院启停，角色弹窗支持删除并在二次确认后调用真实 DELETE。完整订单、物流、报告等后续模块明确显示“模块待接入”，不展示模拟数据；财务发票复用真实审核接口。
- `admin/README.md` 已改为统一 `petlis-backend` 启动和角色来源说明，不再引用旧后台或虚构 dev 账号。

## RED / GREEN 记录

- 基线：后端 114 项（原 103 + Task 1 新增 11），0 失败、5 跳过；管理端源 62 项全通过。
- 角色/访问范围：先写 `AccessServiceTest`、`UserRoleServiceTest`、`UnifiedRoleSessionTest`，首次因目标类不存在编译 RED；实现后定向通过。角色移除测试修正 mock 返回值后，与 SessionPolicy 合计 27 项 GREEN。
- 管理 API：先写 `AdminServiceTest`、`AdminControllerTest`，首次因 AdminService/AdminController 不存在编译 RED；实现后覆盖 `code=200`、0 基分页、DOCTOR 越权、ADMIN/legacy reviewer、角色操作员来源等并 GREEN。
- 管理端合并契约：先在空 worktree 运行，因 `src/api.js` 不存在 RED；移植后新测试因缺少 `isModulePending` 等继续 RED。实现统一登录、`code=200`、元金额、分页和待接入后转 GREEN。
- 页面契约：审核真实列表、角色维护、0 基页码首次 3 项 RED；实现后转 GREEN。分页控件先以 1 基显示失败，再改为 0 基内部值和 1 基展示。
- 本地审查后新增财务/待接入页面守护，财务仍调用旧接口、物流未明确待接入时 RED；修正后转 GREEN。
- 订单审核提交时间守护先移除实现并确认 RED（缺少 `p.CreateDt AS submittedAt`），恢复后转 GREEN。
- 审查 1 Critical + 4 Important 修复均先复现：staff 类型缺失及 LAB/COURIER 旧医生接口越权 RED；两个旧审核自审和资料驳回未撤销 3 项 RED；医院登录/旧 token/停启撤销 4 项 RED；REVIEWER 医院菜单与角色删除确认 RED。逐项修复后定向及全量均 GREEN。
- 复审无 profile 缺口使用真实 `ReviewService + SessionService + JWT` 串联测试，先复现资料驳回后旧 token 仍有效，再以同一 `@Transactional` 方法内最小 profile upsert 修复并转 GREEN；未改任何 schema。

## 最终验证

- `cd petlis-backend && mvn test`：**152 tests，0 failures，0 errors，5 skipped，BUILD SUCCESS**。152 = 原 103 + Task 1 新增 11 + Task 2 及审查修复新增 38；5 个跳过仍为既有本地数据库测试。
- `cd admin && npm test`：**76 tests，76 pass，0 fail**。
- `cd integration && python3 -m unittest discover -s tests -v`：**9/9 通过**；`python3 -m py_compile schema_guard.py` 通过。
- `git diff --check` 通过；旧迁移和平台 DDL 无差异。

## 注意事项

- 运行前必须在合规数据库副本/目标环境准备 Task 1 `lin_user_role` 等平台表并分配真实 ADMIN；本任务没有执行 DDL、连接原库或创建演示管理员。
- 本阶段使用 MockMvc、JDBC mock 和管理端 Node 契约测试，未执行真实 MySQL 跨角色 HTTP 或浏览器端到端登录；这些留待 Task 6 隔离环境验证。
- 完整订单、物流、报告、看板、签约月结、运营及系统配置等待 Task 3/4，页面会明确提示待接入。
