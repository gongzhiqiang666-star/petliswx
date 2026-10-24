# Tasks: PETLIS Bug Fixes and Enhancements

**Feature Branch**: `004-bugfixes-and-enhancements`
**Date**: 2025-12-28
**Input**: Design documents from `/specs/004-bugfixes-and-enhancements/`

**Prerequisites**:
- plan.md ✅
- spec.md ✅
- research.md ✅
- data-model.md ✅
- contracts/ ✅
- quickstart.md ✅

**Tests**: Manual testing specified in spec (no automated test tasks required)

**Organization**: Tasks grouped by 7 user stories to enable independent implementation and testing

---

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (US1-US7) this task belongs to
- All tasks include exact file paths

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database schema initialization and project preparation

- [ ] T001 Create notification table lin_lb_notification in MySQL database
- [ ] T002 Create MySQL trigger trg_diagnosis_status_change for order status changes
- [ ] T003 Verify existing database tables and indexes using dbhub MCP
- [ ] T004 Update petlis-backend/pom.xml dependencies if needed
- [ ] T005 Review miniprogram/project.config.json for API base URL configuration

**Checkpoint**: Database and project structure ready for development

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core backend entities that all user stories depend on

**⚠️ CRITICAL**: No user story implementation can begin until this phase is complete

- [ ] T006 [P] Create NotificationRecord entity in petlis-backend/src/main/java/com/petlis/entity/business/NotificationRecord.java
- [ ] T007 [P] Create NotificationRepository interface in petlis-backend/src/main/java/com/petlis/repository/NotificationRepository.java
- [ ] T008 [P] Create NotificationService interface in petlis-backend/src/main/java/com/petlis/service/NotificationService.java
- [ ] T009 [P] Create NotificationServiceImpl in petlis-backend/src/main/java/com/petlis/service/impl/NotificationServiceImpl.java
- [ ] T010 Create NotificationController in petlis-backend/src/main/java/com/petlis/controller/api/NotificationController.java
- [ ] T011 Update OrderStatusEnum in petlis-backend/src/main/java/com/petlis/common/enums/OrderStatusEnum.java (verify all states defined)

**Checkpoint**: Backend notification infrastructure ready - user story implementation can now begin

---

## Phase 3: User Story 1 - 修复订单详情和数据展示问题 (Priority: P1) 🎯 MVP

**Goal**: 修复订单列表和详情页数据显示Bug，确保用户能正确查看订单信息

**Independent Test**: 打开订单列表页面，点击任意订单进入详情页，验证数据和详情是否正确显示

### Implementation for User Story 1

- [ ] T012 [P] [US1] Review OrderController in petlis-backend/src/main/java/com/petlis/controller/api/OrderController.java - verify list() method supports pagination
- [ ] T013 [P] [US1] Review OrderService in petlis-backend/src/main/java/com/petlis/service/OrderService.java - verify data mapping completeness
- [ ] T014 [P] [US1] Review OrderResponse/OrderDetailResponse DTOs in petlis-backend/src/main/java/com/petlis/dto/response/ - verify all required fields present
- [ ] T015 [US1] Update OrderService.getList() to include pet details (animalName, chipNo) in response
- [ ] T016 [US1] Update OrderService.getDetail() to include complete order data (pet, sampleInfo, testItems, hospital)
- [ ] T017 [US1] Update OrderMapper in petlis-backend/src/main/java/com/petlis/mapper/OrderMapper.java - verify entity-to-DTO mapping
- [ ] T018 [P] [US1] Update miniprogram/pages/orders/orders.wxml - verify order card displays pet.animalName
- [ ] T019 [P] [US1] Update miniprogram/pages/orders/orders.wxml - verify order card displays diagnosisStatus
- [ ] T020 [P] [US1] Update miniprogram/pages/order-detail/order-detail.wxml - verify all order fields display correctly
- [ ] T021 [US1] Add empty state UI in miniprogram/pages/orders/orders.wxml - show "暂无订单" illustration + "创建订单" button
- [ ] T022 [US1] Add error handling in miniprogram/pages/orders/orders.js - show toast "加载失败" on API error

**Checkpoint**: 订单列表和详情页显示实际数据，用户可以正常查看订单信息

---

## Phase 4: User Story 2 - 实现订单列表分页加载 (Priority: P1)

**Goal**: 实现订单列表分页加载，默认显示5条，支持下拉加载更多

**Independent Test**: 创建超过5个订单，然后下拉滚动验证是否加载更多订单

### Implementation for User Story 2

- [ ] T023 [P] [US2] Review OrderController.getList() in petlis-backend - verify pageNum/pageSize parameters
- [ ] T024 [P] [US2] Update OrderService.getList() to return paginated response (total, pageNum, pageSize, list)
- [ ] T025 [US2] Update miniprogram/pages/orders/orders.js data - change pageSize from 100 to 5
- [ ] T026 [US2] Add pageNum and hasMore state in miniprogram/pages/orders/orders.js
- [ ] T027 [US2] Implement loadMore() function in miniprogram/pages/orders/orders.js - fetch next page
- [ ] T028 [US2] Add onReachBottom() handler in miniprogram/pages/orders/orders.js - trigger loadMore when scrolling to bottom
- [ ] T029 [US2] Add loading state in miniprogram/pages/orders/orders.wxml - show "加载中..." text when loading
- [ ] T030 [US2] Add "没有更多了" message in miniprogram/pages/orders/orders.wxml - show when hasMore=false
- [ ] T031 [US2] Add loading prevention flag in miniprogram/pages/orders/orders.js - prevent duplicate requests

**Checkpoint**: 订单列表支持分页加载，用户可以滚动查看所有历史订单

---

## Phase 5: User Story 3 - 修复宠物编辑和检测报告显示 (Priority: P1)

**Goal**: 修复宠物编辑页面预填充，调整检测报告显示为申请时间和状态

**Independent Test**: 打开宠物详情页，点击编辑按钮验证表单预填充；查看检测报告列表验证显示内容

### Implementation for User Story 3

- [ ] T032 [P] [US3] Review PetController.getDetail() in petlis-backend/src/main/java/com/petlis/controller/api/PetController.java
- [ ] T033 [P] [US3] Review PetResponse DTO in petlis-backend/src/main/java/com/petlis/dto/response/PetResponse.java
- [ ] T034 [P] [US3] Review PetService.getDetail() in petlis-backend/src/main/java/com/petlis/service/PetService.java
- [ ] T035 [US3] Verify Pet entity includes all required fields (animalName, animalNameEn, birthday, chipNo, ownerName, etc.)
- [ ] T036 [P] [US3] Review ReportController in petlis-backend/src/main/java/com/petlis/controller/api/ReportController.java
- [ ] T037 [P] [US3] Review ReportService.getPetReports() in petlis-backend/src/main/java/com/petlis/service/ReportService.java
- [ ] T038 [US3] Update ReportResponse DTO in petlis-backend/src/main/java/com/petlis/dto/response/ReportResponse.java - add applicationTime field
- [ ] T039 [US3] Update ReportService.getPetReports() to return applicationTime (createDt) and statusLabel
- [ ] T040 [US3] Update ReportService.getPetReports() to sort by createDt DESC (最新在前)
- [ ] T041 [P] [US3] Update miniprogram/pages/pet-detail/pet-detail.js - add loadPetDetail() for edit mode
- [ ] T042 [P] [US3] Update miniprogram/pages/pet-detail/pet-detail.js - implement onLoad() to check for edit mode
- [ ] T043 [US3] Update miniprogram/pages/order-add-pet/order-add-pet.js - implement loadPetDetail() for pre-filling
- [ ] T044 [US3] Update miniprogram/pages/order-add-pet/order-add-pet.js - prefill all form fields from pet detail
- [ ] T045 [US3] Update miniprogram/pages/order-add-pet/order-add-pet.wxml - verify all form fields bind to data correctly
- [ ] T046 [P] [US3] Update miniprogram/pages/pet-detail/pet-detail.wxml - display applicationTime instead of other fields
- [ ] T047 [P] [US3] Update miniprogram/pages/pet-detail/pet-detail.wxml - display statusLabel for report status
- [ ] T048 [US3] Add empty state UI in miniprogram/pages/pet-detail/pet-detail.wxml - show "暂无报告" when no reports

**Checkpoint**: 宠物编辑页面正确预填充，检测报告显示申请时间和状态

---

## Phase 6: User Story 4 - 新增宠物后自动选择和页面刷新 (Priority: P2)

**Goal**: 新增宠物后自动选中新宠物并刷新列表

**Independent Test**: 通过预约检测流程，新增一个宠物，验证是否自动选中和刷新

### Implementation for User Story 4

- [ ] T049 [P] [US4] Review PetController.create() in petlis-backend/src/main/java/com/petlis/controller/api/PetController.java
- [ ] T050 [P] [US4] Update PetService.create() to return created pet ID in response
- [ ] T051 [US4] Update PetResponse DTO in petlis-backend/src/main/java/com/petlis/dto/response/PetResponse.java - include id and createtime
- [ ] T052 [P] [US4] Update miniprogram/pages/order-add-pet/order-add-pet.js - implement onCreateSuccess() handler
- [ ] T053 [US4] Update miniprogram/pages/order-add-pet/order-add-pet.js - pass newPetId to previous page using getCurrentPages()
- [ ] T054 [US4] Update miniprogram/pages/order-select-pet/order-select-pet.js - add onShow() lifecycle method
- [ ] T055 [US4] Update miniprogram/pages/order-select-pet/order-select-pet.js - check for newPetId parameter in onShow()
- [ ] T056 [US4] Update miniprogram/pages/order-select-pet/order-select-pet.js - set selectedPetId = newPetId if found
- [ ] T057 [US4] Update miniprogram/pages/order-select-pet/order-select-pet.js - default selectedPetId to first pet (latest) if no selection
- [ ] T058 [US4] Update miniprogram/pages/order-select-pet/order-select-pet.js - loadPets() in onShow() to refresh list
- [ ] T059 [US4] Update PetService.getList() in petlis-backend - order by createtime DESC
- [ ] T060 [P] [US4] Update miniprogram/pages/order-select-pet/order-select-pet.wxml - highlight selected pet visually

**Checkpoint**: 新增宠物后自动选中新宠物，刷新页面后默认选最新

---

## Phase 7: User Story 5 - 我的页面显示用户所在医疗机构 (Priority: P2)

**Goal**: "我的"页面显示用户所属医院的正确信息

**Independent Test**: 登录后打开"我的"页面，验证医院信息是否正确

### Implementation for User Story 5

- [ ] T061 [P] [US5] Review UserController in petlis-backend/src/main/java/com/petlis/controller/api/UserController.java
- [ ] T062 [P] [US5] Add getHospitalInfo() method in UserController - returns user's hospital
- [ ] T063 [US5] Update UserService in petlis-backend/src/main/java/com/petlis/service/UserService.java - get user by hospital_id
- [ ] T064 [US5] Add UserInfoResponse.getHospitalInfo() method - query hospital by user.hospitalId
- [ ] T065 [P] [US5] Update HospitalRepository in petlis-backend/src/main/java/com/petlis/repository/InfoHospitalRepository.java - verify findById method
- [ ] T066 [P] [US5] Create/getHospitalInfo API endpoint in UserController: GET /api/user/hospital
- [ ] T067 [P] [US5] Update miniprogram/pages/mine/mine.js - add loadUserInfo() method
- [ ] T068 [US5] Update miniprogram/pages/mine/mine.js - add loadHospitalInfo() method
- [ ] T069 [US5] Update miniprogram/pages/mine/mine.wxml - display hospitalName field
- [ ] T070 [P] [US5] Update miniprogram/pages/mine/mine.wxml - display hospital adress field
- [ ] T071 [P] [US5] Update miniprogram/pages/mine/mine.wxml - display hospital tel field
- [ ] T072 [US5] Add error handling in miniprogram/pages/mine/mine.js - show "获取医院信息失败" toast on 404
- [ ] T073 [US5] Add retry button in miniprogram/pages/mine/mine.wxml - shown when hospital info load fails
- [ ] T074 [US5] Update UserInfoResponse DTO in petlis-backend/src/main/java/com/petlis/dto/response/UserInfoResponse.java - include hospital details

**Checkpoint**: "我的"页面显示用户所属医院的名称、地址、联系电话

---

## Phase 8: User Story 6 - 申请支付流程（为支付模块预留接口） (Priority: P3)

**Goal**: 为支付模块预留接口，暂不实现实际支付功能

**Independent Test**: 提交不同类型的申请，验证流程分支（月结无需支付，次结显示提示）

### Implementation for User Story 6

- [ ] T075 [P] [US6] Review OrderController.create() in petlis-backend/src/main/java/com/petlis/controller/api/OrderController.java
- [ ] T076 [US6] Update OrderService.create() to check hospital.PayType (0=月结, 1=次结)
- [ ] T077 [US6] Add logic in OrderService.create() - if PayType=0 (月结), set status to 0 (待审核)
- [ ] T078 [US6] Add logic in OrderService.create() - if PayType=1 (次结), set status to -2 (草稿/待支付)
- [ ] T079 [P] [US6] Create payment stub endpoint in OrderController: POST /api/payments/create
- [ ] T080 [US6] Update payment stub to return 501 "支付功能开发中，请暂时使用其他支付方式"
- [ ] T081 [P] [US6] Add payment flow placeholder in miniprogram - detect PayType and show message if 次结
- [ ] T082 [US6] Update frontend to display "支付功能开发中" message for 次结 customers
- [ ] T083 [US6] Document payment interface contract in docs/payment-api-guide.md (future reference)

**Checkpoint**: 支付接口已预留，月结客户正常流程，次结客户看到提示信息

---

## Phase 9: User Story 7 - 订单状态变化通知 (Priority: P3)

**Goal**: 使用MySQL触发器记录状态变化，预留微信通知接口

**Independent Test**: 修改订单状态，验证通知是否记录到通知表

### Implementation for User Story 7

- [ ] T084 [P] [US7] Verify MySQL trigger trg_diagnosis_status_change created in Phase 1
- [ ] T085 [P] [US7] Test trigger by updating DiagnosisStatus in lin_lb_diagnosisdetail table
- [ ] T086 [US7] Query lin_lb_notification table to verify trigger created record
- [ ] T087 [US7] Update NotificationService in petlis-backend/src/main/java/com/petlis/service/NotificationService.java
- [ ] T088 [US7] Add getPendingNotifications() method in NotificationService - query wechat_sent_status=0
- [ ] T089 [US7] Add updateNotificationStatus() method in NotificationService - update wechat_sent_status
- [ ] T090 [US7] Add getNotificationHistory() method in NotificationService - query by orderId
- [ ] T091 [P] [US7] Add GET /api/notifications/pending endpoint in NotificationController
- [ ] T092 [P] [US7] Add PUT /api/notifications/{id}/status endpoint in NotificationController
- [ ] T093 [P] [US7] Add GET /api/notifications/history endpoint in NotificationController
- [ ] T094 [US7] Document WeChat notification integration in docs/wechat-notification-integration.md (future work)
- [ ] T095 [US7] Add scheduled task placeholder in petlis-backend for sending WeChat notifications (commented out)

**Checkpoint**: 订单状态变化自动记录到通知表，微信通知接口已预留

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements, testing, and documentation

- [ ] T096 [P] Test all P1 user stories (US1, US2, US3) end-to-end in WeChat Developer Tools
- [ ] T097 [P] Test all P2 user stories (US4, US5) end-to-end in WeChat Developer Tools
- [ ] T098 [P] Test P3 user stories (US6, US7) - verify stub interfaces work correctly
- [ ] T099 [P] Verify all API responses use unified format {code, message, data}
- [ ] T100 [P] Verify all error cases show toast messages "加载失败"
- [ ] T101 [P] Verify all empty states show illustration + message + action button
- [ ] T102 [P] Performance test - verify order list API < 200ms (p95)
- [ ] T103 [P] Performance test - verify infinite scroll load time < 2 seconds
- [ ] T104 [P] Performance test - verify "My Page" hospital info load < 1 second
- [ ] T105 Update miniprogram/utils/api.js - verify BASE_URL points to correct backend
- [ ] T106 Code review - verify all backend field mappings match database (no camelCase in DB layer)
- [ ] T107 Update README.md in project root with feature summary
- [ ] T108 Run quickstart.md validation checklist from specs/004-bugfixes-and-enhancements/quickstart.md
- [ ] T109 Create feature completion report in docs/004-completion-report.md
- [ ] T110 Update CHANGELOG.md with new features and bug fixes

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion (T001-T005) - BLOCKS all user stories
- **User Stories (Phase 3-9)**: All depend on Foundational phase completion (T006-T011)
  - User stories can proceed sequentially in priority order (P1 → P2 → P3)
  - Or in parallel if team capacity allows (see Parallel Team Strategy below)
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1) - 订单数据显示**: Can start after Foundational - No dependencies on other stories
- **User Story 2 (P1) - 订单分页加载**: Can start after Foundational - Extends US1 but independently testable
- **User Story 3 (P1) - 宠物编辑和报告**: Can start after Foundational - Independent of orders stories
- **User Story 4 (P2) - 自动选择宠物**: Can start after Foundational - Depends on Pet entity (US3)
- **User Story 5 (P2) - 医院信息显示**: Can start after Foundational - Independent
- **User Story 6 (P3) - 支付接口**: Can start after Foundational - Integrates with US1 (orders)
- **User Story 7 (P3) - 通知系统**: Can start after Foundational - Integrates with US1 (orders)

**Critical Path**: Setup → Foundational → US1 (订单数据显示) → US2 (订单分页加载)

**Parallel Opportunities**: US3, US4, US5 can all proceed in parallel after Foundational phase

### Within Each User Story

- Backend tasks (Controller/Service/Repository updates) can run in parallel if different files
- Frontend tasks (different pages) can run in parallel
- Backend for a story should complete before frontend integration for that story
- Each story complete before moving to next priority level

---

## Parallel Example: Foundational Phase (Phase 2)

```bash
# Launch all entity/repository/service creation in parallel:
Task T006: "Create NotificationRecord entity in petlis-backend/src/main/java/com/petlis/entity/business/NotificationRecord.java"
Task T007: "Create NotificationRepository interface in petlis-backend/src/main/java/com/petlis/repository/NotificationRepository.java"
Task T008: "Create NotificationService interface in petlis-backend/src/main/java/com/petlis/service/NotificationService.java"
```

---

## Parallel Example: User Story 1 (Phase 3)

```bash
# Launch all backend reviews in parallel:
Task T012: "Review OrderController in petlis-backend/src/main/java/com/petlis/controller/api/OrderController.java"
Task T013: "Review OrderService in petlis-backend/src/main/java/com/petlis/service/OrderService.java"
Task T014: "Review OrderResponse/OrderDetailResponse DTOs in petlis-backend/src/main/java/com/petlis/dto/response/"

# Launch all frontend UI updates in parallel:
Task T018: "Update miniprogram/pages/orders/orders.wxml - verify order card displays pet.animalName"
Task T019: "Update miniprogram/pages/orders/orders.wxml - verify order card displays diagnosisStatus"
Task T020: "Update miniprogram/pages/order-detail/order-detail.wxml - verify all order fields display correctly"
```

---

## Parallel Example: User Story 3 (Phase 5)

```bash
# Launch all PetService/ReportService reviews in parallel:
Task T032: "Review PetController.getDetail() in petlis-backend/src/main/java/com/petlis/controller/api/PetController.java"
Task T033: "Review PetResponse DTO in petlis-backend/src/main/java/com/petlis/dto/response/PetResponse.java"
Task T034: "Review PetService.getDetail() in petlis-backend/src/main/java/com/petlis/service/PetService.java"

# Launch all frontend pet-detail updates in parallel:
Task T041: "Update miniprogram/pages/pet-detail/pet-detail.js - add loadPetDetail() for edit mode"
Task T042: "Update miniprogram/pages/pet-detail/pet-detail.js - implement onLoad() to check for edit mode"
```

---

## Implementation Strategy

### MVP First (User Stories 1-3 Only - P1 Priority)

1. Complete Phase 1: Setup (T001-T005)
2. Complete Phase 2: Foundational (T006-T011) ⚠️ CRITICAL
3. Complete Phase 3: User Story 1 - 订单数据显示 (T012-T022)
4. Complete Phase 4: User Story 2 - 订单分页加载 (T023-T031)
5. Complete Phase 5: User Story 3 - 宠物编辑和报告 (T032-T048)
6. **STOP and VALIDATE**: Test all P1 stories independently
7. Deploy/demo if ready (P1 fixes critical bugs)

### Incremental Delivery (Recommended)

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 (P1) → Test independently → Deploy/Demo (Fixes critical order display bugs!)
3. Add User Story 2 (P1) → Test independently → Deploy/Demo (Adds pagination)
4. Add User Story 3 (P1) → Test independently → Deploy/Demo (Fixes pet editing)
5. Add User Story 4 (P2) → Test independently → Deploy/Demo (UX improvement)
6. Add User Story 5 (P2) → Test independently → Deploy/Demo (Shows hospital info)
7. Add User Story 6 (P3) → Test independently → Deploy/Demo (Payment stub)
8. Add User Story 7 (P3) → Test independently → Deploy/Demo (Notification logging)
9. Complete Polish phase → Final release

Each story adds value without breaking previous stories!

### Parallel Team Strategy

With 2-3 developers after Foundational phase:

**Sprint 1** (P1 - Critical Bug Fixes):
- Developer A: User Story 1 (订单数据显示) - Frontend focused
- Developer B: User Story 2 (订单分页加载) - Frontend focused
- Developer C: User Story 3 (宠物编辑和报告) - Full stack

**Sprint 2** (P2 - UX Improvements):
- Developer A: User Story 4 (自动选择宠物) - Frontend
- Developer B: User Story 5 (医院信息显示) - Full stack

**Sprint 3** (P3 - Future Features):
- Developer A: User Story 6 (支付接口) - Backend stub
- Developer B: User Story 7 (通知系统) - Backend + DB

---

## Task Summary

**Total Tasks**: 110
**By Phase**:
- Phase 1 (Setup): 5 tasks
- Phase 2 (Foundational): 6 tasks
- Phase 3 (US1 - P1): 11 tasks
- Phase 4 (US2 - P1): 9 tasks
- Phase 5 (US3 - P1): 17 tasks
- Phase 6 (US4 - P2): 12 tasks
- Phase 7 (US5 - P2): 14 tasks
- Phase 8 (US6 - P3): 9 tasks
- Phase 9 (US7 - P3): 12 tasks
- Phase 10 (Polish): 15 tasks

**By User Story**:
- US1 (订单数据显示 - P1): 11 tasks
- US2 (订单分页加载 - P1): 9 tasks
- US3 (宠物编辑和报告 - P1): 17 tasks
- US4 (自动选择宠物 - P2): 12 tasks
- US5 (医院信息显示 - P2): 14 tasks
- US6 (支付接口 - P3): 9 tasks
- US7 (通知系统 - P3): 12 tasks

**Parallelizable Tasks**: ~67% (74/110 tasks marked [P])

---

## Notes

- [P] tasks = different files, no dependencies, can run in parallel
- [US1-US7] labels map tasks to user stories for traceability
- Each user story is independently completable and testable
- Manual testing specified (no automated test tasks)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All file paths are absolute and specific
- Backend follows: Controller → Service → Repository → Entity
- Frontend follows: pages/ for UI, services/ for API calls
- All tasks follow strict checklist format: `- [ ] [ID] [P?] [Story?] Description with file path`

---

## MVP Scope Recommendation

**Minimum Viable Product** = Phase 1 + Phase 2 + Phase 3 + Phase 4 + Phase 5

**Rationale**:
- US1, US2, US3 are all P1 (critical bug fixes)
- Fix order display issues (US1)
- Add pagination (US2)
- Fix pet editing and reports (US3)
- These 3 stories address the most critical user pain points
- Can be delivered and tested independently

**Post-MVP**:
- US4 and US5 add UX improvements (P2)
- US6 and US7 add future features (P3)

---

**Generated**: 2025-12-28
**Ready to execute**: Yes - all tasks are specific and actionable
