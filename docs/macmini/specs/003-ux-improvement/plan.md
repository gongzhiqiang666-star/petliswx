# Implementation Plan: PETLIS UX体验优化

**Branch**: `003-ux-improvement` | **Date**: 2025-12-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-ux-improvement/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

优化PETLIS宠物实验室信息系统微信小程序的用户体验,主要改进包括:宠物选择优化(搜索/分页/自动计算年龄)、物种选择独立页面、性别选择修正、表单布局优化、报告查看功能增强。项目以微信小程序前端改进为主,依赖现有后端API,重点提升易用性和操作效率。

## Technical Context

**Language/Version**: 微信小程序框架 (JavaScript/TypeScript), WXML, WXSS
**Primary Dependencies**:
- 微信小程序基础库 2.x+
- 现有后端 API 服务 (Java Spring Boot)
- 需要确认: 小程序 UI 组件库 (WEUI/Vant Weapp/自定义组件)

**Storage**:
- 数据存储: MySQL 8.0+ (通过后端 API 访问)
- 本地缓存: wx.setStorageSync 用于排序偏好、历史记录
- 不涉及前端本地数据库

**Testing**:
- 微信开发者工具内置测试
- 手动测试为主
- 单元测试: NEEDS CLARIFICATION (是否引入 Jest/Miniprogram Simulate)

**Target Platform**: 微信小程序 (iOS + Android)
**Project Type**: mobile (微信小程序前端项目)

**Performance Goals**:
- 页面加载时间 < 2秒
- 搜索响应时间 < 1秒 (本地搜索 < 500ms, 远程搜索 < 1s)
- 动画帧率 >= 60fps
- 分页加载 < 500ms

**Constraints**:
- 微信小程序包大小限制 (主包 < 2MB, 总包 < 20MB)
- 不许通过修改数据库来满足前台需求,小程序和后端服务是配合关系
- 后端 API 已存在,需确认是否满足新需求
- 必须使用 UTF-8 编码

**Scale/Scope**:
- 宠物档案数量 < 1000
- 物种数量 < 500
- 品种数量 < 2000
- 报告数量 < 10000
- 影响页面: 宠物档案列表、预约检测、宠物建档、报告列表、报告详情等约 7-10 个页面

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. RESTful API 优先设计 - ✅ 适用 (前端调用)

**状态**: 本功能主要是前端改进,但需要确认后端 API 是否符合 RESTful 规范

**检查点**:
- 后端 API 是否使用 `/api` 前缀
- 是否返回标准 JSON 格式 (code, message, data)
- 是否使用标准 HTTP 状态码
- 性别字典 API 是否为 GET /api/dict/sex

**需确认**: 现有后端 API 是否完全满足需求,是否需要新增或修改接口

### II. 分层架构与职责分离 - ⚠️ 部分适用

**状态**: 本功能主要在前端实现,但需确保后端 API 调用遵循分层架构

**检查点**:
- 前端调用后端 API 接口,不应直接访问数据库
- 前端不应包含业务逻辑,业务逻辑应在后端 Service 层
- 年龄计算: 应在前端展示时计算(基于当前日期),还是后端提供?
  - **建议**: 前端实时计算(基于生日),避免频繁请求后端

### III. 数据验证与安全性 - ✅ 适用

**状态**: 前端必须进行数据验证,但不能替代后端验证

**检查点**:
- 前端表单验证: 必填项、长度、格式
- 性别选择必须通过后端字典 API 获取
- 搜索输入必须防注入处理(即使在前端)
- API 调用失败时的错误处理和用户提示

### IV. 数据库规范与一致性 - ⚠️ 间接相关

**状态**: 前端不直接访问数据库,但需确保数据展示一致性

**检查点**:
- 禁止通过修改数据库来满足前台需求 ✅ (符合规范)
- 所有数据通过后端 API 获取
- 逻辑删除(IsActive)字段只显示 IsActive=1 的数据
- 日期格式统一使用 ISO 格式

### V. 异常处理与日志记录 - ✅ 适用

**状态**: 前端需要处理 API 异常,但不记录后端日志

**检查点**:
- API 调用失败时显示友好错误提示
- 自动填充失败时的降级方案(允许手动输入)
- 前端错误日志可以使用 console.error 或上报到监控平台
- 关键操作失败时的重试机制

### VI. 微信小程序适配性 - ✅ 核心适用

**状态**: 本功能专门针对微信小程序 UX 优化

**检查点**:
- 不许通过修改数据库数据来满足前台展示要求 ✅
- 小程序和后端服务是配合关系 ✅
- 如果需要调整后端要严谨,理论上后端服务只负责与数据库通信满足需求
- 接口响应数据大小优化,避免超过小程序请求限制
- 支持数据字典接口,动态渲染表单选项
- 年龄计算使用前端实时计算,减少 API 请求

### VII. 代码质量与文档规范 - ✅ 适用

**状态**: 前端代码也需遵循代码质量规范

**检查点**:
- 组件命名规范: 大驼峰(PascalCase)
- 方法和变量: 小驼峰(camelCase)
- 常量: 全大写下划线分隔(UPPER_SNAKE_CASE)
- 复杂 UI 逻辑添加注释
- 单个组件长度不超过 500 行
- 控制流嵌套不超过 3 层

### Gate 评估结果

✅ **通过** - 本功能主要在前端实现,遵循项目宪章的核心原则:

1. **不修改数据库** - 所有数据通过后端 API 获取和修改
2. **后端服务独立** - 小程序和后端服务是配合关系,后端只负责与数据库通信
3. **遵循分层架构** - 前端调用 API,不直接访问数据库
4. **符合小程序规范** - 针对微信小程序特性优化

**需要 Phase 0 研究的未知项**:
1. 现有后端 API 是否完全满足新功能需求(搜索、分页、排序)?
2. 是否需要新增后端 API 接口?
3. 性别字典 API 当前是否存在且返回完整选项(未知/公/母/其他)?
4. 年龄计算应该在前端还是后端实现?
5. 当前小程序使用什么 UI 组件库?
6. 是否需要引入单元测试框架?

## Project Structure

### Documentation (this feature)

```text
specs/003-ux-improvement/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

**结构决策**: 本项目是微信小程序前端项目,采用微信小程序标准目录结构

```text
miniprogram/                          # 微信小程序根目录
├── api/                              # API 接口封装
│   └── *.js                          # API 请求函数
├── assets/                           # 静态资源
│   └── images/                       # 图片资源
├── config/                           # 配置文件
│   └── config.js                     # 应用配置
├── styles/                           # 全局样式
│   └── *.wxss                        # 通用样式文件
├── utils/                            # 工具函数
│   ├── util.js                       # 通用工具函数
│   └── age-calculator.js             # 年龄计算工具(新增)
├── pages/                            # 页面目录
│   ├── home/                         # 首页
│   ├── pets/                         # 宠物档案列表(需修改)
│   ├── pet-add/                      # 宠物建档(需修改)
│   ├── pet-detail/                   # 宠物详情(需修改)
│   ├── order-select-pet/             # 宠物选择页面(需大幅修改)
│   ├── breed-select/                 # 品种选择(可能需调整)
│   ├── orders/                       # 订单列表
│   ├── order-add-pet/                # 预约检测(需修改)
│   ├── order-test-items/             # 检测项目选择
│   ├── order-preview/                # 订单预览
│   ├── report-list/                  # 报告列表(需修改)
│   ├── report-view/                  # 报告查看(需修改)
│   ├── mine/                         # 我的
│   └── ...                           # 其他页面
├── components/                       # 自定义组件(可能新增)
│   ├── search-input/                 # 搜索输入组件(新增)
│   ├── pagination/                   # 分页组件(新增)
│   ├── pet-card/                     # 宠物卡片组件(新增)
│   └── species-selector/             # 物种选择页面(新增)
├── app.js                            # 小程序逻辑
├── app.json                          # 小程序配置
└── app.wxss                          # 全局样式

petlis-backend/                       # 后端服务(可能需要 API 调整)
└── src/main/java/com/petlis/
    ├── controller/api/               # API 控制器
    │   ├── PetController.java        # 宠物相关 API
    │   ├── DictController.java       # 字典 API
    │   └── ReportController.java     # 报告 API
    └── ...
```

**影响的页面和组件**:
- **需要修改的页面** (7个):
  1. `pages/pets/` - 宠物档案列表(添加搜索、分页)
  2. `pages/pet-add/` - 宠物建档(调整表单布局、性别选择)
  3. `pages/order-select-pet/` - 宠物选择(搜索、分页、年龄计算)
  4. `pages/order-add-pet/` - 预约检测(自动填充畜主信息)
  5. `pages/report-list/` - 报告列表(排序、搜索、分页)
  6. `pages/report-view/` - 报告查看(使用真实 API、合并按钮)
  7. `pages/breed-select/` - 可能需要改为物种选择独立页面

- **可能新增的组件** (4个):
  1. `components/search-input/` - 搜索输入组件(带防抖)
  2. `components/pagination/` - 分页组件
  3. `components/pet-card/` - 宠物卡片组件(显示年龄)
  4. `components/species-selector/` - 物种选择页面

- **新增工具函数** (1个):
  1. `utils/age-calculator.js` - 年龄计算函数

## Complexity Tracking

> 本功能没有违反宪章的项,无需填写此表

本功能主要在前端实现,遵循项目宪章的所有核心原则:
- 不修改数据库,所有数据通过后端 API
- 后端服务独立,只负责与数据库通信
- 前端不包含业务逻辑,只负责展示和交互
