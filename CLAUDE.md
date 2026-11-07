# PETLISWX Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-10-22

## Active Technologies
- Java 1.8 (后端), JavaScript ES6+ (小程序前端) + SpringBoot 1.5.22, Spring Security, MyBatis, WeUI组件库 (001-vet-lab-system)

## Project Structure
```
src/
tests/
```

## Commands
npm test && npm run lint

## Code Style
Java 1.8 (后端), JavaScript ES6+ (小程序前端): Follow standard conventions

## Recent Changes
- 001-vet-lab-system: Added Java 1.8 (后端), JavaScript ES6+ (小程序前端) + SpringBoot 1.5.22, Spring Security, MyBatis, WeUI组件库

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
- 本项目的github地址是：https://github.com/gongzhiqiang666-star/petliswx.git

## 当前源码（2026-10-24 导入）

- 实际目录为 `miniprogram/`、`petlis-backend/`、`宠物LIS/`。
- 实际后端版本为 Java 17 / Spring Boot 3.4.1；上文 Java 8 / Spring Boot 1.5 是历史规划。
- 后端验证命令为 `cd petlis-backend && mvn test`；小程序未配置 npm test / lint 脚本。
- 前后端接口字段以数据库映射和现有后端契约为准。
- 凭证仅放本地环境变量，不提交 .env、日志、数据库备份和私钥。
- 未经明确安排，不操作 xsf 项目或改动它的 SSH 设置。

## 数据库铁律（必须遵守）

- **任何表结构、触发器、视图、字段类型/长度/精度、索引、外键、字符集变更，必须先经用户明确同意；未批准不得执行。**
- 数据库现状：88 表（72 旧表 + 16 新增工作流表）、7 视图、1 触发器 `trg_order_status_change`；旧表/视图/触发器只读不改。完整清单见 `docs/DATABASE-SCHEMA.md`。
- 只允许在隔离 MySQL 副本操作（`127.0.0.1 / localhost`、端口非 3306、`--isolated-copy`）；禁止写 Mac mini 原库或远程库。
- 禁止 `DROP / TRUNCATE`、批量删改旧表、全量测试 fixture、重放旧 Flyway 迁移；启动设 `DB_MIGRATIONS_ENABLED=false`。
- 金额用 `decimal` + `BigDecimal`/整数分（禁止 `Double`）；`SampleStatus / PayType / PayStatus2 / IsPayMoney` 保持整数语义；`UNIX_TIMESTAMP` 整数秒与 `datetime` 时间字段不得混改。
- 新增表只允许 `db/platform/` 下的平台工作流表，执行前备份并用 `integration/schema_guard.py` 前后对比。
- 变更申请需附：目的、影响对象、SQL、回滚方案、兼容性影响、隔离副本验证结果。详见 `AGENTS.md`「数据库变更管控」。
