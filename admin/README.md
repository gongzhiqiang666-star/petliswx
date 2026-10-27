# Petlis Web 管理后台

原生 HTML / CSS / ES Modules 管理端，无前端依赖、包安装或构建步骤。业务数据与操作通过 `petlis-backend` 真实 API 完成，不使用 localStorage 业务假数据；localStorage 仅保存登录 Token 和服务端返回的当前用户。

## 启动

先启动统一后端：

```bash
cd ../petlis-backend
mvn spring-boot:run
```

再启动管理端：

```bash
cd ../admin
npm start
```

访问 `http://127.0.0.1:4173`。静态服务器监听 `127.0.0.1:4173`，并将 `/api/**` 同源代理到 `API_ORIGIN`（默认 `http://localhost:8080`）。

```bash
PORT=4173 API_ORIGIN=http://127.0.0.1:8080 npm start
```

登录使用统一 `/api/auth/password/login`。账号仍来自 `lin_user`，不会由管理端创建演示用户或角色；`ADMIN` 等角色必须来自 `lin_user_role`。旧配置 reviewer 仅能进入审核中心，不能获得管理员全量权限。

## 测试

要求 Node.js 20 或更高版本，无需 `npm install`：

```bash
npm test
```

测试使用 Node 内置 `node:test`，覆盖统一登录、`code=200`、0 基分页、Token 持久化、权限路由、审核和用户角色表单、金额转换、待接入提示、静态服务器路径安全及 XSS escaping。

## 当前功能结构

- 真实可用：统一登录、退出、注册/资料/送检/发票审核、用户查询与停用启用、角色及医院/实验室归属维护、医院档案与启停。
- legacy reviewer 可处理既有审核；映射 ADMIN 还可使用用户、医院和角色管理。
- 发票填写真实 HTTPS 地址开具或填写原因驳回，既有订单金额按元显示。
- 数据看板、完整订单管理、物流、实验室、报告工作流、签约月结、运营和系统配置等后续模块在对应合并任务完成前显示“模块待接入”，不展示模拟数据。

核心页面位于 `src/pages/`，共享请求、状态、路由、表单适配和 UI 分别位于 `src/api.js`、`src/store.js`、`src/router.js`、`src/adapters.js`、`src/components/`。

## 安全与交互

管理端仅接受后端 `code=200`，列表请求使用 0 基页码。后续财务模块的表单仍以元输入、按整数分提交；既有 `lin_*` API 的元金额使用独立显示函数，不做分元换算。幂等操作携带唯一 `Idempotency-Key`，危险操作需要确认。动态文本进行 HTML escaping，静态服务器提供 CSP、`nosniff` 和路径穿越防护。
