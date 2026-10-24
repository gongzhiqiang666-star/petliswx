# Petlis 微信小程序与后端

本仓库包含从 Mac mini 现有工作目录导入的小程序、后端源码及参考设计。导入日期：2026-10-24。

## 目录

- `miniprogram/`：原生微信小程序（JavaScript、WXML、WXSS）。
- `petlis-backend/`：Java 17、Spring Boot 3.4.1、Maven 后端，使用 MySQL。
- `宠物LIS/`：原有 React / Figma Make 参考项目；实际业务以前述小程序为准。
- `specs/`：GitHub 仓库原有规划，部分技术版本早于当前源码。
- `docs/macmini/`：Mac mini 原有项目文档及规划归档，供参考，内容不代表已验证的运行状态。

## 小程序开发

1. 在微信开发者工具中导入 `miniprogram/`。
2. 设置有权限的 AppID 或测试 AppID；个人工具设置保存在被忽略的 `project.private.config.json` 中。
3. 按后端实际地址调整 `miniprogram/config/config.js`。导入源码仍保留原有接口地址，尚未切换到新局域网地址。
4. 编译后通过模拟器和真机检查核心业务流程。

## 后端开发

需要 JDK 17、Maven 和可用的 MySQL。数据库仍由 Mac mini 提供，本次导入不包含数据库内容。

```bash
cd petlis-backend
cp .env.example .env
# 编辑 .env，填入数据库密码以及独立生成的 JWT_SECRET 等配置。
# 确认 .env 内容后导出变量；Spring Boot 默认不会自动读取此文件。
set -a
. ./.env
set +a
mvn test
mvn spring-boot:run
```

`.env` 采用 Shell 格式，含空格或特殊符号的值请使用引号。默认后端端口为 `8082`，上下文为 `/api`。

**启动后端会按现有配置执行 Flyway 迁移。首次启动前应备份数据库、核对连接目标和迁移 SQL；本次导入没有启动后端，也没有执行数据库迁移。**

## 凭证与导入说明

- 数据库密码、JWT 签名密钥、微信密钥及支付密钥改为环境变量引用。
- 未纳入私有工具配置、日志、构建产物、编辑器备份及数据库备份；源代码中的 SQL 迁移仍保留。
- Mac mini 原目录、数据库、SSH 配置和 xsf 项目均未修改。
- GitHub 已有的 `specs/`、`.specify/` 和命令模板保留；原有 Mac mini 规划另存至 `docs/macmini/specs/`。
- 导入时已检查 JavaScript / Shell 语法、JSON / XML 格式。后端测试和微信实际编译尚未完成，历史测试报告不能作为本次验证结果。
