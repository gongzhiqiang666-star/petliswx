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
