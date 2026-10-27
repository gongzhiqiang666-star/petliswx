# Task 5 合并报告

## 状态

已完成确认版小程序缺口补齐，保留原 38 个页面、5 个 Tab、组件、assets 和既有下单、草稿、支付轮询、鉴权 PDF 流程；新增 3 个账单页面，未使用根目录简化小程序覆盖。

## 实现

- `config/config.js` 按 develop/trial/release 与 ext 配置生成 `/api` 地址，开发默认 `127.0.0.1`，生产必须显式配置；mock 支付仅限开发环境。
- refresh/logout 使用 Bearer；mine 退出先请求服务端登出，再清本地；401 清理和跳转逻辑未改。
- 新增 `/pets/{id}/health`，复用 `lin_order_clinical`、旧订单和旧检测明细，按 petId 返回病史及 0 基分页报告；页面去除静态疫苗、体检、医疗假数据并加入口、空态、重试和分页。
- 优惠券底部弹层展示金额、门槛、实验室、有效期、状态和预留说明，未改下单抵扣。
- 新增账单列表、详情对账、欠款页；使用 `/bills` 真实接口和 `/user/roles` 真实角色，HOSPITAL_ADMIN 专属入口和操作，DOCTOR 直接访问也不请求账单。
- 订单列表和详情复用 `/logistics` 展示时间线、预计到达、异常；未增加医生内部操作。
- 帮助反馈改为消息中心和真实医院联系电话的明确说明，不伪造提交成功。
- 更新 `docs/FUNCTION-MATRIX.md` 的 F02、F07、F13、F15、F25、F26、F27、F39。

## 审查修复

- 对账状态机仅允许待对账/异议操作；RECONCILED、OVERDUE、SETTLED 不可 DISPUTE 倒退，前端仅合法状态显示操作。
- 账单详情自动续页加载全部明细，欠款列表支持 onReachBottom；明细未完整或续页失败时禁止确认对账。
- `/user/roles` 失败时账户页安全隐藏账单入口但保留旧导航和 `/account` 加载；账单页显示角色错误且重试仍先校验角色。
- 服务端登出失败保留 Token 和本地会话，成功后才清理；mine 不再假提示成功。
- 所有账单状态使用中文标签和确认版 badge 样式。
- 目标后端补充独立 `mvnw`，测试不再引用旧 `backend/`。

## TDD 与验证

先写并观察到环境、认证、病史、券详情、账单、物流、权限、状态倒退、明细节流、角色失败和登出失败等测试失败，再实现并复测通过。

- 后端：在 `petlis-backend/` 执行 `./mvnw -q test`，294 tests，0 failures，0 errors，5 skipped。
- Admin：在 `admin/` 执行 `npm test`，83/83 通过。
- 小程序：在 `miniprogram/` 执行 `npm test`，25/25 通过，覆盖原 38 路由/5 Tab、事件绑定、环境/认证、病史/账单/物流/券详情、权限入口、完整分页、状态机、登出顺序和核心模块保留。
- 小程序：在 `miniprogram/` 执行 `npm run check`，68 个 JS 语法、52 个 JSON 解析全部通过。
- `git diff --check` 通过；未修改 schema、触发器或迁移，未执行真实支付。

## 限制与关注

- 未执行微信开发者工具编译或真机测试；生产 baseURL 需在 `ext.json` 配置。
- 健康接口依赖 Task 3 的 `lin_order_clinical` 工作流表；旧订单未关联 petId 的历史记录不会被推测关联。
- 后端验证使用单元/WebMvc 测试，本次未启动隔离 MySQL 做真实 HTTP 验证。
