# Implementation Tasks: 预约信息填写菜单优化

**Feature Branch**: `001-appointment-form-optimization`
**Date**: 2025-12-30
**Status**: Ready for Implementation
**Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

---

## Task Summary

- **Total Tasks**: 52
- **User Story 1** (P1): 12 tasks - 年龄和体重输入优化
- **User Story 2** (P1): 10 tasks - 检测项目分页加载
- **User Story 3** (P2): 8 tasks - 样本类型选择器
- **User Story 4** (P2): 9 tasks - 订单预览页面
- **User Story 5** (P1): 9 tasks - 订单提交状态和报告编号
- **Setup Tasks**: 4 tasks
- **Foundational Tasks**: 0 tasks (无需额外基础设施)
- **Polish Tasks**: 0 tasks

---

## Dependencies & Execution Order

```
Setup (Phase 1)
  ↓
[US1] + [US2] + [US5] (P1 - 可并行开发)
  ↓
[US3] + [US4] (P2 - 依赖US1完成的数据结构)
```

**Independent Stories**: US1, US2, US5 可以完全独立并行开发
**Story Dependencies**: US3, US4 需要US1完成的age字段数据结构

**MVP Scope**: US1 + US5 - 实现核心的年龄输入和订单提交流程

---

## Phase 1: Setup & Infrastructure

**Goal**: 创建验证注解和工具类，为用户故事实现奠定基础

**Independent Test Criteria**: AgeValidator注解能正确验证各种年龄格式

### Implementation Tasks

- [X] T001 [P] 创建年龄格式验证注解 @AgeFormat 在 petlis-backend/src/main/java/com/petlis/common/validation/AgeFormat.java
- [X] T002 [P] 创建年龄验证器 AgeValidator 在 petlis-backend/src/main/java/com/petlis/common/validation/AgeValidator.java
- [X] T003 [P] 创建报告编号生成工具 ReportNOGenerator 在 petlis-backend/src/main/java/com/petlis/util/ReportNOGenerator.java
- [X] T004 [P] 在 pom.xml 确认 jakarta.validation-api 依赖存在

---

## Phase 2: User Story 1 - 优化年龄和体重输入方式 (P1)

**Goal**: 用户可以使用双选择器选择年龄（年/月），体重字段改为非必填，年龄数据以"Y/M"格式存储

**User Story**: 用户在填写宠物检测预约信息时，需要能够灵活输入宠物的年龄信息，并可以选择性地提供体重数据。

**Independent Test Criteria**:
1. 选择年龄"3岁7个月"，系统存储为"3Y7M"格式
2. 不填写体重字段，系统允许继续操作
3. 填写体重"15kg"，系统正确保存
4. 选择年龄"5个月"，系统存储为"5M"格式

### Backend Tasks

- [X] T005 [P] [US1] 修改 OrderCreateRequest 添加 age 字段验证注解 @AgeFormat 在 petlis-backend/src/main/java/com/petlis/dto/request/OrderCreateRequest.java
- [X] T006 [P] [US1] 修改 OrderCreateRequest 将 animalWeight 字段改为可选（移除@NotNull）在 petlis-backend/src/main/java/com/petlis/dto/request/OrderCreateRequest.java
- [X] T007 [US1] 修改 OrderServiceImpl 在 createOrder 方法中添加年龄格式验证和狂犬检测特殊验证在 petlis-backend/src/main/java/com/petlis/service/impl/OrderServiceImpl.java

### Frontend Tasks

- [X] T008 [P] [US1] 创建年龄选择器组件 age-picker 在 miniprogram/components/age-picker/age-picker.js
- [X] T009 [P] [US1] 创建年龄选择器组件 WXML 模板在 miniprogram/components/age-picker/age-picker.wxml
- [X] T010 [P] [US1] 创建年龄选择器组件样式在 miniprogram/components/age-picker/age-picker.wxss
- [X] T011 [P] [US1] 创建年龄选择器组件配置文件在 miniprogram/components/age-picker/age-picker.json
- [X] T012 [P] [US1] 修改 order-create 页面引入 age-picker 组件在 miniprogram/pages/order-create/order-create.json
- [X] T013 [P] [US1] 修改 order-create 页面 WXML 使用 age-picker 组件替换原有年龄输入在 miniprogram/pages/order-create/order-create.wxml
- [X] T014 [P] [US1] 修改 order-create 页面 JS 添加年龄格式化逻辑 formatAge() 在 miniprogram/pages/order-create/order-create.js
- [X] T015 [P] [US1] 修改 order-create 页面 WXML 移除体重字段的必填标记在 miniprogram/pages/order-create/order-create.wxml
- [X] T016 [US1] 修改 order-create 页面 JS 移除体重字段的必填验证逻辑在 miniprogram/pages/order-create/order-create.js

---

## Phase 3: User Story 2 - 检测项目按需加载 (P1)

**Goal**: 检测项目选择页面首次加载20条数据，滚动到底部自动加载下一批，搜索功能带300ms防抖

**User Story**: 用户在选择检测项目时，系统应该分页加载数据而不是一次性加载所有项目，以确保页面响应速度和流畅的用户体验。

**Independent Test Criteria**:
1. 首次进入页面，只显示前20条检测项目
2. 滚动到底部，自动加载下一批20条
3. 输入搜索关键词，300ms后触发搜索并显示全部匹配结果
4. 数据全部加载完毕，不再触发加载

### Backend Tasks

- [X] T017 [P] [US2] 修改 InfoTestTypeRepository 添加分页查询方法 findByTestNameContainingAndIsActive 在 petlis-backend/src/main/java/com/petlis/repository/InfoTestTypeRepository.java
- [X] T018 [P] [US2] 修改 DictionaryService 添加 listTestTypes 分页查询方法在 petlis-backend/src/main/java/com/petlis/service/DictionaryService.java
- [X] T019 [P] [US2] 修改 DictionaryServiceImpl 实现分页查询逻辑，支持关键词搜索在 petlis-backend/src/main/java/com/petlis/service/impl/DictionaryServiceImpl.java
- [X] T020 [US2] 修改 DictionaryController 添加 GET /api/dictionary/test-types 接口，支持分页和搜索参数在 petlis-backend/src/main/java/com/petlis/controller/api/DictionaryController.java

### Frontend Tasks

- [X] T021 [P] [US2] 修改 api/index.js 添加 listTestTypes 分页查询 API 方法在 miniprogram/api/index.js
- [X] T022 [P] [US2] 修改 order-test-items 页面 WXML 添加 scroll-view 组件包装检测项目列表在 miniprogram/pages/order-test-items/order-test-items.wxml
- [X] T023 [P] [US2] 修改 order-test-items 页面 JS 添加分页数据结构（page, pageSize, hasMore, loading）在 miniprogram/pages/order-test-items/order-test-items.js
- [X] T024 [P] [US2] 修改 order-test-items 页面 JS 实现 loadTestItems 分页加载方法在 miniprogram/pages/order-test-items/order-test-items.js
- [X] T025 [P] [US2] 修改 order-test-items 页面 JS 实现 onScrollToLower 滚动到底部加载更多在 miniprogram/pages/order-test-items/order-test-items.js
- [X] T026 [US2] 修改 order-test-items 页面 WXML 添加搜索框 input 组件在 miniprogram/pages/order-test-items/order-test-items.wxml
- [X] T027 [P] [US2] 修改 order-test-items 页面 JS 实现 onSearchInput 搜索输入事件（带300ms防抖）在 miniprogram/pages/order-test-items/order-test-items.js
- [X] T028 [P] [US2] 修改 order-test-items 页面 JS 实现 performSearch 执行搜索方法在 miniprogram/pages/order-test-items/order-test-items.js

---

## Phase 4: User Story 3 - 样本类型选择器优化 (P2)

**Goal**: 样本类型使用独立页面选择器，与品种选择保持一致的交互体验

**User Story**: 用户在选择样本类型时，使用与宠物建档中品种选择相同的独立页面选择器模式，以提供一致的交互体验。

**Independent Test Criteria**:
1. 点击样本类型选择框，导航到独立选择页面
2. 在独立页面选择样本类型，返回预约页面并显示选中值
3. 不选择直接返回，预约页面保持原有状态

### Frontend Tasks

- [X] T029 [P] [US3] 创建 sample-type-select 页面配置文件在 miniprogram/pages/sample-type-select/sample-type-select.json
- [X] T030 [P] [US3] 创建 sample-type-select 页面 WXML 模板（参考 breed-select 结构）在 miniprogram/pages/sample-type-select/sample-type-select.wxml
- [X] T031 [P] [US3] 创建 sample-type-select 页面 JS 逻辑在 miniprogram/pages/sample-type-select/sample-type-select.js
- [X] T032 [P] [US3] 创建 sample-type-select 页面样式文件在 miniprogram/pages/sample-type-select/sample-type-select.wxss
- [X] T033 [P] [US3] 修改 order-create 页面 WXML 将样本类型输入框改为点击跳转到 sample-type-select 页面在 miniprogram/pages/order-create/order-create.wxml
- [X] T034 [P] [US3] 修改 order-create 页面 JS 添加 onTapSampleType 方法处理样本类型选择框点击事件在 miniprogram/pages/order-create/order-create.js
- [X] T035 [P] [US3] 修改 order-create 页面 JS 在 onLoad 中从页面参数或缓存中读取样本类型选中值在 miniprogram/pages/order-create/order-create.js
- [X] T036 [US3] 修改 app.json 在 pages 数组中添加 sample-type-select 页面路径在 miniprogram/app.json

---

## Phase 5: User Story 4 - 订单预览信息完整展示 (P2)

**Goal**: 订单预览页面显示所有字段标签，空值字段显示"未填写"（灰色文本），确保信息完整性

**User Story**: 用户在订单预览页面能够看到完整的订单信息，包括所有已填写的字段，以便在提交前进行核对。

**Independent Test Criteria**:
1. 预览页面显示所有宠物基本信息（姓名、品种、性别、年龄、体重）
2. 预览页面显示样本信息（样本类型、采样日期）
3. 预览页面显示配送信息（畜主姓名、送检医师）
4. 预览页面显示检测项目及价格
5. 所有字段正确赋值，空值显示"未填写"（灰色）

### Frontend Tasks

- [X] T037 [P] [US4] 修改 order-preview 页面 WXML 添加体重字段展示（使用条件渲染显示"未填写"）在 miniprogram/pages/order-preview/order-preview.wxml
- [X] T038 [P] [US4] 修改 order-preview 页面 WXML 添加年龄字段展示在 miniprogram/pages/order-preview/order-preview.wxml
- [X] T039 [P] [US4] 修改 order-preview 页面 WXML 添加样本类型字段展示（带空值处理）在 miniprogram/pages/order-preview/order-preview.wxml
- [X] T040 [P] [US4] 修改 order-preview 页面 WXML 添加送检医师字段展示（带空值处理）在 miniprogram/pages/order-preview/order-preview.wxml
- [X] T041 [P] [US4] 修改 order-preview 页面 JS 在 onLoad 中添加空值字段处理逻辑在 miniprogram/pages/order-preview/order-preview.js
- [X] T042 [P] [US4] 修改 order-preview 页面 JS 添加 formatFieldValue 方法统一处理空值显示在 miniprogram/pages/order-preview/order-preview.js
- [X] T043 [P] [US4] 修改 order-preview 页面 WXML 确保所有字段标签都显示在 miniprogram/pages/order-preview/order-preview.wxml
- [X] T044 [P] [US4] 修改 order-preview 页面 WXML 为空值字段添加 empty class 用于灰色样式在 miniprogram/pages/order-preview/order-preview.wxml
- [X] T045 [P] [US4] 修改 order-preview 页面 WXSS 添加 .value.empty 样式规则（颜色 #999）在 miniprogram/pages/order-preview/order-preview.wxss

---

## Phase 6: User Story 5 - 订单提交时更新状态和生成报告编号 (P1)

**Goal**: 订单提交时将 SampleStatus 从 -2 更新为 0（待审核），并生成格式为"RAB{年}{订单ID}"的报告编号

**User Story**: 用户在预览页面点击"提交订单"按钮后，系统将订单状态从草稿更新为待审核，并生成唯一的德国报告编号。

**Independent Test Criteria**:
1. 提交订单成功，数据库中 SampleStatus 为 0（待审核）
2. 系统生成格式为"RAB20251"的报告编号
3. 查询订单详情，ReportNO 字段包含生成的报告编号
4. 同一年创建多个订单，每个订单拥有唯一的报告编号

### Backend Tasks

- [X] T046 [P] [US5] 修改 OrderServiceImpl 在 createOrder 方法中设置 SampleStatus 为 0（待审核）在 petlis-backend/src/main/java/com/petlis/service/impl/OrderServiceImpl.java
- [X] T047 [P] [US5] 修改 OrderServiceImpl 在 createOrder 方法中调用 ReportNOGenerator 生成报告编号在 petlis-backend/src/main/java/com/petlis/service/impl/OrderServiceImpl.java
- [X] T048 [P] [US5] 修改 OrderServiceImpl 在保存订单后将生成的 ReportNO 写入数据库在 petlis-backend/src/main/java/com/petlis/service/impl/OrderServiceImpl.java
- [X] T049 [US5] 修改 OrderServiceImpl 添加报告编号生成失败时的异常处理和事务回滚在 petlis-backend/src/main/java/com/petlis/service/impl/OrderServiceImpl.java

### Frontend Tasks

- [X] T050 [P] [US5] 修改 order-preview 页面 JS 在 onSubmit 方法中移除 sampleStatus: -2 字段在 miniprogram/pages/order-preview/order-preview.js
- [X] T051 [P] [US5] 修改 order-preview 页面 JS 在提交成功后显示订单号和报告编号在 miniprogram/pages/order-preview/order-preview.js
- [X] T052 [US5] 修改 order-preview 页面 JS 添加提交失败时的错误处理和用户提示在 miniprogram/pages/order-preview/order-preview.js

---

## Parallel Execution Opportunities

### Within User Story 1 (Age & Weight Input)
- **Backend并行**: T005, T006 可同时开发（都是DTO修改）
- **Frontend并行**: T008-T011 可同时开发（age-picker组件的4个文件）
- **Frontend并行**: T012, T013, T015 可同时开发（页面修改，不同文件）

### Within User Story 2 (Test Items Pagination)
- **Backend并行**: T017-T019 可同时开发（Repository、Service接口、Service实现）
- **Frontend并行**: T022, T023, T026 可同时开发（WXML、JS数据结构、WXML搜索框）

### Within User Story 3 (Sample Type Selector)
- **完全并行**: T029-T032 可同时开发（sample-type-select页面的4个文件）
- **并行修改**: T033-T035 可同时开发（都是order-create页面修改，不同方法）

### Across User Stories (Setup完成后的独立并行)
- **完全独立的Story**: US1, US2, US5 可以完全独立并行开发
- **前端-后端分离**: 每个Story的前后端任务可以并行开发（先定接口契约）

---

## Implementation Strategy

### MVP Scope (最小可行产品)

**推荐MVP**: Phase 1 (Setup) + Phase 2 (US1) + Phase 6 (US5)

**理由**:
- US1 实现核心的年龄输入和数据验证
- US5 实现订单提交流程和报告编号生成
- 两者结合可以完成完整的预约订单创建流程
- US2, US3, US4 是性能和体验优化，可以在MVP后迭代

**MVP交付时间**: 预计 8-12 小时

### Incremental Delivery (增量交付)

**第1次增量** (Week 1):
- Setup (T001-T004)
- US1 完整实现 (T005-T016)
- US5 完整实现 (T046-T052)
- **交付物**: 可用的预约订单创建功能

**第2次增量** (Week 2):
- US2 完整实现 (T017-T028)
- **交付物**: 检测项目分页加载和搜索优化

**第3次增量** (Week 3):
- US3 完整实现 (T029-T036)
- US4 完整实现 (T037-T045)
- **交付物**: 完整优化的预约信息填写流程

### Risk Mitigation

**技术风险**:
- 风险：年龄格式验证可能遗漏边界情况
  - 缓解：T002编写全面的单元测试，覆盖各种格式

- 风险：分页加载在大量数据时性能下降
  - 缓解：确保数据库索引建立，使用EXPLAIN分析查询计划

- 风险：报告编号生成并发冲突
  - 缓解：使用数据库唯一索引约束，事务回滚机制

**业务风险**:
- 风险：用户不熟悉新的年龄选择方式
  - 缓解：UI上添加使用提示和示例

- 风险：历史数据的Age字段格式不符合新规则
  - 缓解：查询时添加兼容处理，逐步迁移历史数据

---

## Testing Strategy

### Backend Testing

**单元测试** (JUnit 5):
- T002: AgeValidator 测试各种年龄格式和边界值
- T003: ReportNOGenerator 测试报告编号生成逻辑
- T019: DictionaryServiceImpl 分页查询测试
- T047-T049: OrderServiceImpl 订单创建和报告编号测试

**集成测试** (Spring Boot Test + MockMvc):
- POST /api/orders: 创建订单并验证 SampleStatus 和 ReportNO
- GET /api/dictionary/test-types: 分页查询和搜索功能

**测试覆盖率目标**: > 80%

### Frontend Testing

**人工测试** (微信开发者工具):
- 每个User Story的Acceptance Scenarios全部验证
- 边界情况测试（如年龄输入极限值）
- 错误处理测试（如网络错误、服务器错误）

**测试检查清单** (详见 quickstart.md):
- 年龄选择器功能测试
- 分页加载功能测试
- 搜索防抖功能测试
- 样本类型选择器测试
- 订单预览页面测试
- 订单提交流程测试

---

## Task Format Validation

✅ **所有任务遵循标准格式**: `- [ ] [TaskID] [P?] [Story?] Description with file path`

✅ **Task ID**: T001-T052 连续编号

✅ **P标记**: 并行任务已标记[P]

✅ **Story标签**: Setup阶段无标签，User Story阶段有[US1]-[US5]标签

✅ **文件路径**: 每个任务都包含明确的文件路径

✅ **可执行性**: 每个任务描述清晰具体，可以直接执行

---

## Next Steps

1. **开始实施**: 按照Phase顺序执行任务，或选择MVP scope (Phase 1+2+6)
2. **分支策略**: 在 `001-appointment-form-optimization` 分支上开发
3. **代码审查**: 每完成一个Phase提交一次Pull Request
4. **测试验证**: 完成每个User Story后执行独立测试
5. **部署上线**: 全部任务完成后合并到主分支

**Estimated Total Time**: 20-30 小时（包含开发和测试）

---

**End of Tasks**
