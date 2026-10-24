# Implementation Plan: [FEATURE]

**Branch**: `004-bugfixes-and-enhancements` | **Date**: 2025-12-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-bugfixes-and-enhancements/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

修复订单列表和详情页数据显示问题，实现订单分页加载，修复宠物编辑页面预填充，调整检测报告显示格式，实现新增宠物后自动选择，显示用户所属医疗机构，为支付模块预留接口，并实现MySQL触发器和微信通知系统。这是一个前后端协同的功能增强，涉及微信小程序前端、Spring Boot后端和MySQL数据库的多层修改。

## Technical Context

**Language/Version**:
- Frontend: JavaScript (ES6+) for WeChat Mini-Program
- Backend: Java 17 with Spring Boot 3.x

**Primary Dependencies**:
- Frontend: WeChat Mini-Program Framework, wx API
- Backend: Spring Boot Starter Web, Spring Data JPA, MySQL Connector, JWT, WeChat Java SDK

**Storage**: MySQL 8.0 (utf8mb4 encoding)

**Testing**:
- Frontend: WeChat Developer Tools manual testing
- Backend: JUnit 5, integration tests with @SpringBootTest

**Target Platform**:
- Frontend: WeChat Mini-Program (iOS/Android)
- Backend: Java 17+ server (Linux/macOS/Windows)

**Project Type**: web (mobile + backend)

**Performance Goals**:
- API response time < 200ms (p95)
- Infinite scroll load time < 2 seconds
- Page load time < 1 second for "My Page" hospital info

**Constraints**:
- Must use existing RESTful API structure
- Must not modify backend field mappings (backend should match database)
- Payment module must be stubbed with interface预留
- WeChat notifications must use existing WeChat infrastructure

**Scale/Scope**:
- ~20 pages in mini-program
- ~50 REST API endpoints
- ~10 database tables
- 7 user stories, 28 functional requirements

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

根据项目宪章（constitution.md），本功能必须遵守以下约束：

### ✅ 必须遵守的规则（MUST）

1. **字段命名规范**: 后端字段命名必须与数据库匹配，使用数据库字段名（snake_case），不得在前端进行字段映射调整
   - 评估: 本功能仅修改前端显示逻辑和后端API返回数据，不涉及字段重命名
   - 状态: PASS

2. **项目职责分工**: 宠物LIS是Figma Make生成的参考代码，实际以微信小程序（miniprogram）为准
   - 评估: 本次开发基于miniprogram目录，不修改宠物LIS参考代码
   - 状态: PASS

3. **编码规范**: 所有代码使用UTF-8编码
   - 评估: 项目已使用UTF-8编码
   - 状态: PASS

4. **全局语言**: 项目全局使用中文进行交流和文档编写
   - 评估: 本次计划文档使用中文
   - 状态: PASS

5. **RESTful API设计**: 遵循RESTful规范，统一响应格式（code/message/data）
   - 评估: 后端API设计遵循现有规范
   - 状态: PASS

6. **分层架构**: Controller → Service → Repository → Entity
   - 评估: 后端代码遵循分层架构
   - 状态: PASS

### ⚠️ 需要注意的约束（SHOULD）

1. **支付模块预留**: 支付功能暂不实现，但需要在数据结构和流程上预留接口
   - 评估: FR-018至FR-22已明确要求预留接口，P3优先级
   - 状态: PASS - 按设计预留，不实际开发支付功能

2. **MySQL触发器**: 订单状态变化时使用触发器自动记录通知（FR-028）
   - 评估: 需要在数据库层面实现触发器
   - 状态: PASS - Phase 1设计时包含触发器DDL

3. **微信通知**: 使用现有微信基础设施（企业微信或小程序订阅消息）
   - 评估: 假设已有基础设施，本功能仅对接
   - 状态: PASS

4. **数据库访问**: 通过MCP工具（dbhub）查询和验证数据库结构
   - 评估: Phase 0研究阶段需要查询现有表结构
   - 状态: PASS - 将使用dbhub查询

### ❌ 不允许的操作（MUST NOT）

1. **不得破坏现有功能**: 订单、宠物、用户等核心功能的API不能破坏性修改
   - 评估: 仅增强显示和添加功能，不修改现有API签名
   - 状态: PASS

2. **不得硬编码数据**: 订单、宠物、医院等数据必须从API动态加载
   - 评估: 本功能核心就是修复数据显示问题
   - 状态: PASS

### Gate Result: ✅ PASSED

所有宪章约束均已评估通过，可以进入Phase 0研究阶段。

## Project Structure

### Documentation (this feature)

```text
specs/004-bugfixes-and-enhancements/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── order-api.md
│   ├── pet-api.md
│   ├── user-api.md
│   └── notification-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

**Structure Decision**: This is a web application with WeChat mini-program frontend and Spring Boot backend.

```text
# Frontend: WeChat Mini-Program
miniprogram/
├── pages/
│   ├── order-list/              # 订单列表（分页加载）
│   ├── order-detail/            # 订单详情（数据显示）
│   ├── pet-detail/              # 宠物详情（报告显示）
│   ├── pet-edit/                # 宠物编辑（预填充）
│   ├── order-select-pet/        # 预约检测-选择宠物（自动选择）
│   ├── profile/                 # 我的页面（医院信息）
│   └── sample-type-select/      # 样本类型选择（已实现）
├── services/
│   ├── order.js                 # 订单API服务
│   ├── pet.js                   # 宠物API服务
│   └── user.js                  # 用户API服务
└── utils/
    ├── api.js                   # API基础配置
    └── pagination.js            # 分页工具

# Backend: Spring Boot Application
petlis-backend/
├── src/
│   └── main/
│       ├── java/com/petlis/
│       │   ├── controller/
│       │   │   ├── OrderController.java          # 订单API
│       │   │   ├── PetController.java            # 宠物API
│       │   │   ├── UserController.java           # 用户API
│       │   │   └── NotificationController.java   # 通知API
│       │   ├── service/
│       │   │   ├── OrderService.java
│       │   │   ├── PetService.java
│       │   │   ├── UserService.java
│       │   │   └── NotificationService.java
│       │   ├── repository/
│       │   │   ├── OrderRepository.java
│       │   │   ├── PetRepository.java
│       │   │   ├── UserRepository.java
│       │   │   └── NotificationRepository.java
│       │   ├── entity/
│       │   │   ├── Order.java
│       │   │   ├── Pet.java
│       │   │   ├── User.java
│       │   │   ├── Hospital.java
│       │   │   ├── Report.java
│       │   │   └── NotificationRecord.java
│       │   └── dto/
│       │       ├── request/
│       │       │   └── CreateOrderRequest.java
│       │       └── response/
│       │           ├── OrderResponse.java
│       │           └── PetDetailResponse.java
│       └── resources/
│           ├── application.yml
│           └── db/
│               └── migration/
│                   ├── V1__create_notification_table.sql
│                   ├── V2__add_order_status_trigger.sql
│                   └── V3__alter_user_add_customer_type.sql
└── tests/
    ├── unit/
    └── integration/

# Database: MySQL
mysql/
└── petlis_db/
    ├── info_order              # 订单表
    ├── info_pet                # 宠物表
    ├── info_user               # 用户表
    ├── info_hospital           # 医院表
    ├── info_report             # 检测报告表
    └── info_notification       # 通知记录表（新增）
```

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

无违规范例。所有宪章约束均已通过评估，本功能无需额外的复杂度justify。

---

## Phase 0: Research and Documentation

*OBJECTIVE: Resolve technical unknowns, verify assumptions, document existing architecture*

### Research Tasks

1. **验证数据库表结构**
   - 查询现有表结构: info_order, info_pet, info_user, info_hospital, info_report
   - 验证字段名是否符合snake_case规范
   - 检查是否有notification_record表，如无需要创建
   - 验证订单状态字段（order_status）的可能值

2. **验证现有API端点**
   - 检查后端是否已有订单列表、订单详情、宠物详情、用户信息等API
   - 验证API响应格式是否为{code, message, data}结构
   - 检查是否支持分页查询（pageNum, pageSize）

3. **验证微信通知基础设施**
   - 确认是否已有微信企业号或小程序订阅消息配置
   - 确认发送通知的SDK或API接口

4. **验证用户token中的hospitalId**
   - 检查JWT token payload中是否包含hospitalId字段
   - 验证用户表中的hospital_id字段

5. **验证宠物表的时间字段**
   - 检查info_pet表是否有createtime或updatetime字段
   - 用于判断"最新创建的宠物"

### Research Output

✅ **Completed**: `research.md` created with findings from all research tasks.

---

## Phase 1: Data Model and API Design

*OBJECTIVE: Design data models, API contracts, and generate quickstart guide*

### Design Artifacts

#### 1. Data Model (`data-model.md`) ✅

**核心实体设计**:
- ✅ DiagnosisDetail (订单/检测申请) - `lin_lb_diagnosisdetail`
- ✅ Pet (宠物) - `lin_lb_pet`
- ✅ User (用户) - `lin_user`
- ✅ Hospital (医院) - `lin_lb_infohospital`
- ✅ NotificationRecord (通知记录) - `lin_lb_notification` (NEW)
- ✅ SampleType (样本类型) - `lin_lb_infosampletype`
- ✅ TestType (检测项目) - `lin_lb_infotesttype`

**数据库表设计**:
- ✅ 新增 `lin_lb_notification` 表 (通知记录表)
- ✅ MySQL触发器 `trg_diagnosis_status_change` (订单状态变化触发器)
- ✅ 索引设计优化

**关键发现**:
- 订单状态枚举: -2(草稿), 0(待审核), 1(审核不通过), 2(等待报告), 3(已完成), 4(已取消)
- 支付状态: PayStatus2 (0=未支付, 1=已支付)
- 医院支付类型: PayType (0=月结, 1=次结)

#### 2. API Contracts (`contracts/`) ✅

**已创建的API契约文档**:

1. ✅ `contracts/order-api.md`
   - GET /api/orders/list - 订单列表（分页）
   - GET /api/orders/{orderId} - 订单详情
   - 前端调用示例（分页加载）
   - 数据验证规则

2. ✅ `contracts/pet-api.md`
   - GET /api/pets/{petId} - 宠物详情（用于预填充）
   - GET /api/pets/list - 宠物列表
   - GET /api/pets/{petId}/reports - 检测报告列表
   - POST /api/pets/create - 创建宠物（返回ID用于自动选择）
   - PUT /api/pets/{petId} - 更新宠物
   - 前端调用示例（编辑预填充、新增自动选择）

3. ✅ `contracts/user-api.md`
   - GET /api/user/hospital - 获取用户所属医院信息
   - 前端调用示例（我的页面显示）

4. ✅ `contracts/notification-api.md` (P3优先级)
   - GET /api/notifications/pending - 获取待发送通知
   - PUT /api/notifications/{id}/status - 更新发送状态
   - GET /api/notifications/history - 通知历史
   - POST /api/payments/create - 支付接口预留

#### 3. Quickstart Guide (`quickstart.md`) ✅

**开发环境设置**:
- ✅ 数据库DDL (通知表、触发器)
- ✅ 后端启动步骤
- ✅ 前端开发工具配置
- ✅ API测试示例

**开发指南**:
- ✅ 按优先级排序的开发任务 (P1 → P2 → P3)
- ✅ 功能测试用例
- ✅ 性能测试基准
- ✅ 调试技巧和常见问题

**部署检查清单**:
- ✅ 数据库验证
- ✅ 后端验证
- ✅ 前端验证
- ✅ 测试验收标准

---

## Phase 1 Re-evaluation: Constitution Check

*GATE: Re-check after Phase 1 design to ensure compliance*

### ✅ 宪章约束复审结果

| 约束 | Phase 0评估 | Phase 1设计 | 状态 |
|------|------------|------------|------|
| 字段命名规范 | PASS | 使用@Column注解明确映射 | ✅ PASS |
| 项目职责分工 | PASS | 基于miniprogram开发 | ✅ PASS |
| UTF-8编码 | PASS | 所有文件UTF-8 | ✅ PASS |
| 全局中文 | PASS | 文档和注释中文 | ✅ PASS |
| RESTful API | PASS | 遵循统一响应格式 | ✅ PASS |
| 分层架构 | PASS | Controller → Service → Repository | ✅ PASS |
| 支付模块预留 | PASS | 仅设计接口，不实现功能 | ✅ PASS |
| MySQL触发器 | PASS | 已设计触发器DDL | ✅ PASS |
| 不破坏现有功能 | PASS | 仅增强，不修改签名 | ✅ PASS |

### 最终结论: ✅ ALL PASSED

所有Phase 1设计产物完全符合项目宪章要求，可以进入Phase 2（任务分解）。

---
