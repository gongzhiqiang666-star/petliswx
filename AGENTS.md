# Petlis 项目约定

## 提交与 PR 规范

- 所有提交信息（commit message）和 PR 标题、正文一律使用中文。
- 提交类型前缀可使用 Conventional Commits 的英文缩写（feat、fix、refactor、docs、test、chore、build、ci、perf），主题和正文必须中文。
- PR 正文使用 `## 概述`、`## 验证`、`## 风险` 三个固定小节，内容简洁、事实清楚。
- 提交前运行可用的静态检查和客户端逻辑检查；通过后再提交。

## 其他项目约定

- 页面与组件视觉、间距、图标、表单、按钮、空态等统一遵循 `docs/DESIGN-SYSTEM.md`。
- 小程序仅使用 `assets/icons/brand-*` 图标，不使用 emoji 或文字箭头充当图标。
- 选择类交互优先当前页底部弹层，非必要不跳新页面。
- 不执行数据库结构变更（CREATE/ALTER/DROP）前必须先获得用户批准；本机只使用 MySQL 副本，禁止写 Mac mini 原库。
- 不使用真实支付；本地开发仅使用 mock 支付与合成测试数据。

## 新环境启动与小程序联调

### 启动顺序与安全边界

1. 准备 JDK 17+、Maven、Node 20+、MySQL 和微信开发者工具。先确认代码分支及 `petlis-backend/`、`admin/`、`miniprogram/` 三个应用来源一致，不启动历史 `backend/` 形成双后台。
2. 数据库只使用当前机器上的 MySQL 副本，禁止连接或修改 Mac mini 原库。保留既有 `lin_*` 主表和数据，启动时设置 `DB_MIGRATIONS_ENABLED=false`，禁止重放旧 Flyway 迁移、修改旧表或旧触发器。
3. `db/platform/` 的新工作流表 DDL 只能在获得明确批准后执行，先备份并核对目标库，执行前后使用 `integration/schema_guard.py` 验证旧结构未变。数据库凭据只从私有环境配置注入，不写入仓库、日志或 PR。
4. 启动统一后端：`source "$HOME/.local/share/petlis/mysql/backend.env"` 后运行 `petlis-backend/mvnw spring-boot:run`，默认接口为 `http://127.0.0.1:8082/api`。实际以本机私有配置为准。
5. 启动 PC 管理端：`API_ORIGIN=http://127.0.0.1:8082 npm start`，目录为 `admin/`，默认地址 `http://127.0.0.1:4173`。
6. 在微信开发者工具导入 `miniprogram/`。不要使用旧预览二维码判断最新代码；每次改配置后重新执行 `cli preview` 并扫描新二维码。

### 模拟器、手机和 HTTPS 配置

- 模拟器可使用 `http://127.0.0.1:8082/api`；手机中的 `127.0.0.1` 指向手机自身，不能指向电脑。
- 局域网方式：后端必须监听 `0.0.0.0`，将地址改为 `http://<电脑局域网IP>:8082/api`，并确认手机与电脑处于同一网段。IP 可能随网络变化，不要把某台机器的 IP 当作团队统一配置。
- HTTPS 方式：在获得负责人同意后，可临时执行 `cloudflared tunnel --url http://127.0.0.1:8082 --no-autoupdate`，将生成的随机 HTTPS 地址配置为 `<隧道地址>/api`。隧道会让 API 在公网可访问，真实数据库环境必须控制使用时长，联调结束立即关闭，不得提交临时域名。
- `miniprogram/ext.json` 的 `baseURL` 可用于环境配置，但本次实测某些预览包没有采用该值。真机联调时必须同时检查 `miniprogram/config/config.js` 的开发环境默认地址，确保不再回退到 `127.0.0.1` 或其他旧地址。机器相关地址应进入本地私有配置，不能作为团队默认值提交。
- 开发者工具路径：`设置 → 项目设置 → 本地设置`，开发联调可勾选“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”。这只影响开发工具，不会替手机开启调试。
- 手机预览页还必须单独执行：`右上角 … → 开发调试 → 打开调试`。正式发布不能使用此豁免，生产必须配置合法 HTTPS 域名和微信服务器域名白名单。

### 故障排查顺序

1. 先查实际请求地址，不要先改代码。`request:fail-102 net:ERR_CONNECTION_REFUSED`、`errno:600001` 优先检查是否仍请求 `127.0.0.1`、旧局域网 IP 或旧预览包。
2. 电脑分别执行 `curl http://127.0.0.1:8082/api/auth/capabilities` 和 `curl http://<局域网IP>:8082/api/auth/capabilities`，预期 HTTP 200。再检查监听端口：`lsof -nP -iTCP:8082 -sTCP:LISTEN`。
3. 手机浏览器访问同一个 API 地址。能返回 200/JSON 说明 Wi-Fi、路由和后端基本可达；不能访问时检查 IP 是否变化、手机代理/VPN、路由器设备隔离和电脑防火墙。同一 Wi-Fi 不等于设备间一定可访问。
4. 手机浏览器可访问但小程序失败时，依次核对：最新预览二维码、手机调试模式、`config.js` 与 `ext.json` 的实际生效值、HTTPS 证书和完整错误文字。电脑开发者工具显示 0 Errors 不能代替手机错误证据。
5. 隧道场景先用电脑访问 `<隧道地址>/api/auth/capabilities`，确认 200 后再生成二维码。隧道只代理本机 `8082`，MySQL 不需要也不应对外开放。
6. 不连续盲改配置。每次只改一个变量，记录最新错误；错误变化说明问题层级已变化，必须按新错误继续定位。

### 新环境验收清单

```bash
cd petlis-backend && ./mvnw test
cd admin && npm test
cd miniprogram && npm test && npm run check
cd integration && python3 -m unittest discover -s tests -v
```

- PC：打开 `http://127.0.0.1:4173`，使用团队分配的开发账号登录并检查真实列表。
- 小程序：登录页加载成功后，再验证宠物、送检、报告等只读流程；支付只使用 mock。
- 真实库启动后核对用户、医院、订单数量与启动前一致，不执行清理脚本、`DROP`、`TRUNCATE` 或全量测试 fixture。
- 联调结束关闭临时隧道、后端演示写操作和不再需要的进程；保留 MySQL 的真实数据与结构。
