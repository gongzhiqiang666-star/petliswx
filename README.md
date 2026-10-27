# Petlis 微信小程序、统一管理端与后端

本仓库以确认版原生微信小程序、`admin/` 管理端和唯一 `petlis-backend/` 为运行应用。根目录 `backend/` 与 `demo/` 仅作历史参考，不参与合并运行。所有业务继续以既有 `lin_*` 主表为准，不建立第二套用户、订单或报告主数据。

## 运行组件

- `petlis-backend/`：Java 17、Spring Boot 3.4、MySQL，默认端口 `8082`，接口上下文 `/api`。
- `admin/`：原生 Web 管理端，默认端口 `4173`，把 `/api` 代理到同一后端。
- `miniprogram/`：确认版 38 页加 3 个账单页面，共 41 个注册页面、5 个 Tab。
- `integration/`：schema 守护、隔离 MySQL 8.4 全链路及跨端验收。

## 单一后端与数据库启动

需要 JDK 17+、Maven、Node 20+ 和已准备的 MySQL 数据库。数据库密码与 JWT 密钥只从环境变量读取。

```bash
cd petlis-backend
cp .env.example .env
# 编辑 .env，确认 DB_URL 指向本机副本，不使用 Mac mini 或共享原库。
set -a
. ./.env
set +a
export DB_MIGRATIONS_ENABLED=false
./mvnw spring-boot:run
```

Flyway 旧迁移默认关闭。`src/main/resources/db/platform/` 只包含已批准的新工作流表 DDL，**不在启动时自动执行**；只有获得明确批准后，才能在隔离副本手工执行。禁止 ALTER/DROP 旧表、修改旧触发器、回填旧数据或执行真实支付。

另开终端启动统一管理端：

```bash
cd admin
API_ORIGIN=http://127.0.0.1:8082 npm start
```

在微信开发者工具导入 `miniprogram/`。开发环境默认请求 `http://127.0.0.1:8082/api`；其他环境通过 `ext.json` 的 `baseURL` 配置，生产环境必须显式配置。仅开发环境允许 mock 支付。

## 测试

```bash
# 后端全量
cd petlis-backend && ./mvnw test

# 管理端
cd admin && npm test

# 小程序逻辑、41 页/5 Tab 与静态检查
cd miniprogram && npm test && npm run check

# Schema 守护
cd integration && python3 -m unittest discover -s tests -v

# 可重复 MySQL 8.4 全链路（默认仅 127.0.0.1:13318）
cd integration && ./run-merge-e2e.sh
```

`run-merge-e2e.sh` 每次创建独立临时 MySQL 8.4 副本和 39 张合成旧表，保存执行新表 DDL 前后的完整 schema 快照，重复执行批准的新表 DDL，启动唯一后端和管理端，并运行真实 HTTP、小程序 `utils/request.js`/page/service 与 Playwright 浏览器验收。脚本关闭启动的进程；日志、JSON、PDF、截图及微信 CLI 结果保存在 `integration/results/`。可用 `PETLIS_MERGE_MYSQL_PORT`、`PETLIS_MERGE_BACKEND_PORT`、`PETLIS_MERGE_ADMIN_PORT` 和 `PLAYWRIGHT_MODULE` 调整本机环境。

微信开发者工具已安装时可复验编译：

```bash
/Applications/wechatwebdevtools.app/Contents/MacOS/cli preview \
  --project "$PWD/miniprogram" \
  --qr-output "$PWD/integration/results/wechat-preview.png"
```

真实微信、短信、支付和生产数据库不属于本地 mock 验收范围。逐项实现与实测状态见 `docs/FUNCTION-MATRIX.md`，Task 6 证据与限制见 `.merge-reports/task-6-report.md`。
