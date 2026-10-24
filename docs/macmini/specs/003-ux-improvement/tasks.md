# Tasks: PETLIS UX体验优化

**Input**: Design documents from `/specs/003-ux-improvement/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-spec.yaml

**Tests**: 本项目为前端UX优化,未要求TDD,因此不包含自动化测试任务。测试将通过手动验证完成。

**Organization**: 任务按用户场景分组,以实现每个场景的独立实施和测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行 (不同文件,无依赖关系)
- **[Story]**: 任务所属的用户场景 (US1, US2, US3, US4)
- 包含精确的文件路径

## Path Conventions

本项目是微信小程序前端项目:
- **前端代码**: `miniprogram/`
- **工具函数**: `miniprogram/utils/`
- **组件**: `miniprogram/components/`
- **页面**: `miniprogram/pages/`
- **样式**: `miniprogram/styles/`, `miniprogram/app.wxss`
- **API层**: `miniprogram/api/`

---

## Phase 1: Setup (共享基础设施)

**Purpose**: 项目初始化和基础结构

- [ ] T001 执行数据库准备脚本,启用性别字典选项 (UPDATE lin_lb_dictsex SET IsActive = 1 WHERE DictSexId IN (1, 2, 6, 8))
- [ ] T002 验证性别字典数据,确认返回4条记录 (使用MySQL客户端或dbhub MCP)

**Checkpoint**: 数据库准备完成,性别字典API返回完整选项

---

## Phase 2: Foundational (阻塞性前置条件)

**Purpose**: 所有用户场景依赖的核心基础设施

**⚠️ CRITICAL**: 此阶段完成前,任何用户场景工作无法开始

- [ ] T003 创建年龄计算工具函数 `miniprogram/utils/age-calculator.js`,实现 calculateAge(birthday) 函数,支持 "X岁Y个月" / "X个月" / "未知" 三种格式
- [ ] T004 [P] 在 `miniprogram/app.wxss` 添加全局页面布局样式 (`.page`, `.page-content` 类),确保内容居中对齐,最大宽度750px
- [ ] T005 [P] 创建搜索输入组件目录结构 `miniprogram/components/search-input/`,包含 .js, .wxml, .wxss, .json 四个文件
- [ ] T006 [P] 创建分页组件目录结构 `miniprogram/components/pagination/`,包含 .js, .wxml, .wxss, .json 四个文件
- [ ] T007 [P] 创建宠物卡片组件目录结构 `miniprogram/components/pet-card/`,包含 .js, .wxml, .wxss, .json 四个文件

**Checkpoint**: 基础设施就绪 - 用户场景实施可以并行开始

---

## Phase 3: User Story 1 - 预约检测宠物选择优化 (Priority: P1) 🎯 MVP

**Goal**: 实现宠物选择页面的搜索、分页、年龄计算和畜主信息自动填充功能

**Independent Test**:
1. 打开宠物选择页面 (`pages/order-select-pet/order-select-pet`)
2. 输入"小白"或"张三"进行搜索,验证结果正确过滤
3. 验证宠物卡片显示年龄 (基于生日实时计算)
4. 选择宠物,验证畜主信息自动填充到预约表单
5. 验证分页功能正常工作 (上一页/下一页/页码显示)

### Implementation for User Story 1

- [ ] T008 [P] [US1] 实现搜索输入组件逻辑 `miniprogram/components/search-input/search-input.js`,包含防抖功能 (400ms延迟),触发 search 事件
- [ ] T009 [P] [US1] 实现搜索输入组件样式 `miniprogram/components/search-input/search-input.wxss`,包含搜索图标、输入框、清除按钮样式
- [ ] T010 [P] [US1] 实现搜索输入组件模板 `miniprogram/components/search-input/search-input.wxml`,包含搜索图标、输入框、清除按钮结构
- [ ] T011 [P] [US1] 配置搜索输入组件 `miniprogram/components/search-input/search-input.json`,设置为通用组件
- [ ] T012 [P] [US1] 实现分页组件逻辑 `miniprogram/components/pagination/pagination.js`,包含上一页/下一页/总页数计算,触发 change 事件
- [ ] T013 [P] [US1] 实现分页组件样式 `miniprogram/components/pagination/pagination.wxss`,包含按钮、页码信息样式
- [ ] T014 [P] [US1] 实现分页组件模板 `miniprogram/components/pagination/pagination.wxml`,包含上一页按钮、页码显示、下一页按钮
- [ ] T015 [P] [US1] 配置分页组件 `miniprogram/components/pagination/pagination.json`,设置为通用组件
- [ ] T016 [P] [US1] 实现宠物卡片组件逻辑 `miniprogram/components/pet-card/pet-card.js`,集成年龄计算工具,显示宠物姓名、品种、主人、年龄、性别
- [ ] T017 [P] [US1] 实现宠物卡片组件样式 `miniprogram/components/pet-card/pet-card.wxss`,包含选中状态样式、卡片布局、头像、信息展示样式
- [ ] T018 [P] [US1] 实现宠物卡片组件模板 `miniprogram/components/pet-card/pet-card.wxml`,包含宠物头像、姓名、品种、年龄、性别、主人信息
- [ ] T019 [P] [US1] 配置宠物卡片组件 `miniprogram/components/pet-card/pet-card.json`,设置为通用组件
- [ ] T020 [US1] 修改宠物选择页面逻辑 `miniprogram/pages/order-select-pet/order-select-pet.js`,集成搜索、分页组件,实现宠物搜索API调用 (支持 name 和 masterName 参数),处理搜索结果和空状态
- [ ] T021 [US1] 修改宠物选择页面模板 `miniprogram/pages/order-select-pet/order-select-pet.wxml`,使用 search-input、pet-card、pagination 组件,添加空状态提示 ("未找到匹配宠物" + "清空搜索"按钮)
- [ ] T022 [US1] 修改宠物选择页面样式 `miniprogram/pages/order-select-pet/order-select-pet.wxss`,应用全局 `.page` 和 `.page-content` 类,确保布局居中
- [ ] T023 [US1] 修改预约检测页面逻辑 `miniprogram/pages/order-add-pet/order-add-pet.js`,实现畜主信息自动填充功能,从选中的宠物对象提取 masterName、masterName_en、masterPhone 字段,处理API失败场景并显示友好提示
- [ ] T024 [US1] 修改预约检测页面模板 `miniprogram/pages/order-add-pet/order-add-pet.wxml`,在畜主信息字段添加"自动填充"视觉提示 (如图标或文本说明)

**Checkpoint**: 此时,用户场景1应该完全功能正常并可独立测试

---

## Phase 4: User Story 2 - 物种选择独立页面 (Priority: P2)

**Goal**: 创建物种选择独立页面,支持搜索和分页

**Independent Test**:
1. 在宠物建档页面,点击"物种"选择框
2. 验证跳转到物种选择独立页面
3. 输入"猫"进行搜索,验证结果实时过滤
4. 验证分页功能正常 (每页20条)
5. 选择某个物种,验证返回后物种信息正确填充

### Implementation for User Story 2

- [ ] T025 [P] [US2] 在 `miniprogram/app.json` 添加物种选择页面路由 ("pages/species-select/species-select")
- [ ] T026 [P] [US2] 创建物种选择页面目录 `miniprogram/pages/species-select/`,包含 .js, .wxml, .wxss, .json 四个文件
- [ ] T027 [US2] 实现物种选择页面逻辑 `miniprogram/pages/species-select/species-select.js`,集成 search-input 和 pagination 组件,调用 `GET /api/dictionary/animals` API (支持 name、pageNum、pageSize 参数),处理搜索和分页逻辑
- [ ] T028 [US2] 实现物种选择页面模板 `miniprogram/pages/species-select/species-select.wxml`,使用 search-input 组件,显示物种列表 (包含中英文名),使用 pagination 组件,添加选中状态视觉反馈
- [ ] T029 [US2] 实现物种选择页面样式 `miniprogram/pages/species-select/species-select.wxss`,应用全局布局样式,设计物种列表项样式 (点击区域、选中高亮、文本排版)
- [ ] T030 [US2] 配置物种选择页面 `miniprogram/pages/species-select/species-select.json`,设置页面标题为"选择物种"
- [ ] T031 [US2] 修改宠物建档页面逻辑 `miniprogram/pages/pet-add/pet-add.js`,将物种选择从 picker 改为跳转到物种选择页面,接收选择结果并更新表单字段
- [ ] T032 [US2] 修改宠物建档页面模板 `miniprogram/pages/pet-add/pet-add.wxml`,将物种选择框改为可点击的按钮,显示选中的物种名称,点击后跳转到物种选择页面

**Checkpoint**: 此时,用户场景1和2都应该独立工作正常

---

## Phase 5: User Story 3 - 宠物建档表单优化 (Priority: P2)

**Goal**: 修正性别选择选项,优化表单字段布局

**Independent Test**:
1. 打开宠物建档页面
2. 验证表单字段顺序: 基本信息 → 主人信息 → 其他信息
3. 点击"性别"选择框,验证显示4个选项 (不详、雄性、雌性、已绝育)
4. 验证默认选择为"不详"
5. 选择性别后,验证正确显示选中的选项
6. 验证必填项验证正常工作

### Implementation for User Story 3

- [ ] T033 [P] [US3] 验证性别字典API `GET /api/dictionary/sexes` 返回4条记录 (使用API测试工具或console.log),如不返回4条则通知后端更新数据库
- [ ] T034 [US3] 修改宠物建档页面逻辑 `miniprogram/pages/pet-add/pet-add.js`,调用性别字典API获取选项,设置默认值为 DictSexId=6 (不详),实现性别选择器change事件处理
- [ ] T035 [US3] 修改宠物建档页面模板 `miniprogram/pages/pet-add/pet-add.wxml`,调整表单字段顺序为: 基本信息 (姓名/英文名/物种/品种/性别/生日/芯片号) → 主人信息 (姓名/英文名/电话/地址) → 其他信息 (备注),将性别picker改为绑定到API返回的性别列表
- [ ] T036 [US3] 修改宠物建档页面样式 `miniprogram/pages/pet-add/pet-add.wxss`,应用全局布局样式,优化表单字段间距和分组 (基本信息、主人信息使用视觉分隔)
- [ ] T037 [US3] 修改宠物详情页面 (如果存在) `miniprogram/pages/pet-detail/pet-detail.js`,同步更新性别显示逻辑,确保正确显示性别名称
- [ ] T038 [US3] 应用全局布局样式到所有相关页面 `miniprogram/pages/pets/pets.wxml`, `miniprogram/pages/pet-add/pet-add.wxml`, `miniprogram/pages/order-add-pet/order-add-pet.wxml`,确保最外层使用 `.page` 类,内容区域使用 `.page-content` 类

**Checkpoint**: 所有用户场景(1, 2, 3)现在都应该独立功能正常

---

## Phase 6: User Story 4 - 报告查看优化 (Priority: P1)

**Goal**: 优化报告列表,支持排序、搜索、分页,使用真实API数据显示报告详情

**Independent Test**:
1. 打开报告列表页面
2. 验证排序选项卡显示 (申请时间/发布时间)
3. 切换排序方式,验证列表正确排序
4. 输入宠物姓名搜索,验证结果实时过滤
5. 验证分页功能正常
6. 点击"查看详情"按钮,验证弹窗显示真实API数据 (宠物信息、检测项目、检测结果)
7. 验证只保留一个"查看详情"按钮,移除重复的"查看完整报告"按钮

### Implementation for User Story 4

- [ ] T039 [P] [US4] 实现排序选项卡组件 (可选,或直接在页面实现) `miniprogram/components/sort-tabs/` 或内联在 report-list 页面,包含申请时间/发布时间两个选项,支持升序/降序切换,触发 sort-change 事件
- [ ] T040 [P] [US4] 创建报告详情弹窗组件 `miniprogram/components/report-modal/`,包含 .js, .wxml, .wxss, .json 文件
- [ ] T041 [P] [US4] 实现报告详情弹窗逻辑 `miniprogram/components/report-modal/report-modal.js`,接收报告数据作为属性,显示宠物信息 (姓名、品种、年龄、性别、生日),检测项目列表,检测结果概要,报告日期、状态,备注信息
- [ ] T042 [P] [US4] 实现报告详情弹窗样式 `miniprogram/components/report-modal/report-modal.wxss`,居中显示,宽度90%最大600px,遮罩层,关闭按钮,信息层次清晰 (使用标题、分段、列表)
- [ ] T043 [P] [US4] 实现报告详情弹窗模板 `miniprogram/components/report-modal/report-modal.wxml`,包含宠物信息区域、检测项目列表 (最多显示3项,超过3项显示"查看更多")、检测结果概要、报告信息区域
- [ ] T044 [P] [US4] 配置报告详情弹窗组件 `miniprogram/components/report-modal/report-modal.json`,设置为通用组件
- [ ] T045 [US4] 修改报告列表页面逻辑 `miniprogram/pages/report-list/report-list.js`,集成 search-input、pagination、sort-tabs (或内联排序)、report-modal 组件,实现排序API调用 (GET /api/reports?sortBy=applyTime/publishTime&sortOrder=asc/desc),保存排序偏好到本地缓存 (wx.setStorageSync)
- [ ] T046 [US4] 修改报告列表页面模板 `miniprogram/pages/report-list/report-list.wxml`,使用 search-input 组件,显示排序选项卡,使用 pagination 组件,在报告卡片中只保留"查看详情"按钮 (移除"查看完整报告"按钮),点击后弹出 report-modal
- [ ] T047 [US4] 修改报告列表页面样式 `miniprogram/pages/report-list/report-list.wxss`,应用全局布局样式,设计排序选项卡样式 (选中状态高亮),优化报告卡片布局,确保弹窗遮罩层正确显示
- [ ] T048 [US4] 修改报告查看页面 (如果存在独立页面) `miniprogram/pages/report-view/report-view.js` 或移除该页面 (如果功能已合并到 report-list 的弹窗中),确保使用真实API数据 (`GET /api/reports/:id`)
- [ ] T049 [US4] 在 `miniprogram/app.json` 中注册 report-modal 组件为全局组件 (可选,或在需要使用的页面中引入)

**Checkpoint**: 所有用户场景(1, 2, 3, 4)现在都应该独立功能正常

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 跨多个用户场景的改进和优化

- [ ] T050 [P] 应用全局布局样式到所有遗留页面,检查所有 `miniprogram/pages/*/` 目录,确保每个页面的 WXML 最外层使用 `.page` 类,内容区域使用 `.page-content` 类
- [ ] T051 [P] 优化所有页面的加载性能,添加加载状态指示器 (loading spinner),优化图片加载 (使用懒加载),验证页面加载时间 < 2秒
- [ ] T052 [P] 代码清理和优化,移除调试代码 (console.log),统一代码格式 (使用Prettier),检查并修复ESLint警告
- [ ] T053 [P] 组件样式统一,review所有自定义组件的样式,确保颜色、字体、间距与全局样式一致,复用全局样式类
- [ ] T054 验证所有搜索功能的防抖机制,测试所有搜索输入组件,确认防抖时间为400ms,输入停止后才触发搜索
- [ ] T055 验证所有年龄计算的一致性,检查所有使用年龄计算的页面和组件,确保都使用 `age-calculator.js` 工具函数,测试边界情况 (无生日、今天生日、去年生日)
- [ ] T056 验证所有空状态处理,检查所有列表页面,确认空状态显示友好提示 ("未找到匹配{宠物/报告/物种}") + 提供"清空搜索"或"返回"按钮
- [ ] T057 运行手动测试清单,使用 `quickstart.md` 中的测试清单,逐项验证所有功能正常工作,记录并修复发现的问题
- [ ] T058 [P] 在真机上测试,在iOS和Android设备上测试所有功能,验证布局、交互、性能符合预期
- [ ] T059 准备发布,清理调试代码,移除未使用的组件和资源,检查包大小 (主包 < 2MB),准备发布说明

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 - 可以立即开始
- **Foundational (Phase 2)**: 依赖Setup完成 - 阻塞所有用户场景
- **User Stories (Phase 3-6)**: 都依赖Foundational阶段完成
  - 用户场景可以并行进行 (如果有人力)
  - 或按优先级顺序依次进行 (P1 → P2)
- **Polish (Phase 7)**: 依赖所有期望完成的用户场景

### User Story Dependencies

- **User Story 1 (场景1 - P1)**: Foundational完成后可开始 - 无其他场景依赖
- **User Story 2 (场景2 - P2)**: Foundational完成后可开始 - 可能与US1集成(如物种选择在宠物建档中使用),但应独立可测
- **User Story 3 (场景3 - P2)**: Foundational完成后可开始 - 无其他场景依赖
- **User Story 4 (场景4 - P1)**: Foundational完成后可开始 - 无其他场景依赖

### Within Each User Story

- 组件实现可以并行 (标记为 [P] 的任务)
- 页面逻辑依赖组件完成
- 页面模板依赖页面逻辑
- 场景完成前验证独立测试标准

### Parallel Opportunities

- Setup阶段: 所有任务可以并行执行
- Foundational阶段: T004, T005, T006, T007 可以并行 (不同组件目录)
- User Story 1: T008-T011 (搜索组件), T012-T015 (分页组件), T016-T019 (宠物卡片组件) 可以各自内部并行
- User Story 2: T025, T026 可以并行 (页面路由和目录创建)
- User Story 3: T033 可以独立进行 (API验证)
- User Story 4: T039-T044 (排序选项卡和报告弹窗组件) 可以并行
- Polish阶段: T050, T051, T052, T053 可以并行 (不同页面/文件)

---

## Parallel Example: User Story 1

```bash
# 并行创建所有组件目录结构:
Task: "创建搜索输入组件目录结构 miniprogram/components/search-input/"
Task: "创建分页组件目录结构 miniprogram/components/pagination/"
Task: "创建宠物卡片组件目录结构 miniprogram/components/pet-card/"

# 并行实现搜索输入组件的所有文件:
Task: "实现搜索输入组件逻辑 miniprogram/components/search-input/search-input.js"
Task: "实现搜索输入组件样式 miniprogram/components/search-input/search-input.wxss"
Task: "实现搜索输入组件模板 miniprogram/components/search-input/search-input.wxml"

# 并行实现分页组件的所有文件:
Task: "实现分页组件逻辑 miniprogram/components/pagination/pagination.js"
Task: "实现分页组件样式 miniprogram/components/pagination/pagination.wxss"
Task: "实现分页组件模板 miniprogram/components/pagination/pagination.wxml"
```

---

## Implementation Strategy

### MVP First (仅用户场景1)

1. 完成 Phase 1: Setup (数据库准备)
2. 完成 Phase 2: Foundational (年龄计算工具、全局样式、组件目录) ⚠️ **CRITICAL**
3. 完成 Phase 3: User Story 1 (宠物选择优化)
4. **STOP and VALIDATE**: 独立测试用户场景1
5. 如果准备就绪,部署/演示 MVP

### Incremental Delivery (增量交付)

1. 完成 Setup + Foundational → 基础就绪
2. 添加 User Story 1 → 独立测试 → 部署/演示 (MVP!) ✨
3. 添加 User Story 4 → 独立测试 → 部署/演示 (报告优化)
4. 添加 User Story 2 → 独立测试 → 部署/演示 (物种选择)
5. 添加 User Story 3 → 独立测试 → 部署/演示 (表单优化)
6. 完成 Polish → 最终发布
7. 每个场景都增加价值,不破坏已有场景

### Parallel Team Strategy (并行团队策略)

多名开发者时:

1. 团队一起完成 Setup + Foundational
2. Foundational 完成后:
   - 开发者 A: User Story 1 (宠物选择优化)
   - 开发者 B: User Story 4 (报告查看优化) - 两个P1优先级并行
   - 开发者 C: User Story 2 (物种选择) - P2优先级
3. 场景完成后独立集成测试
4. 所有场景完成后一起做 Polish

---

## Notes

- [P] 任务 = 不同文件,无依赖,可并行
- [Story] 标签 = 将任务映射到特定用户场景,便于追溯
- 每个用户场景应该独立可完成和可测试
- 每个任务或逻辑组后提交代码
- 在任何检查点停止以独立验证场景
- 避免: 模糊的任务、相同文件冲突、破坏独立性的跨场景依赖
- **测试策略**: 本项目为前端UX优化,采用手动测试验证,参考 `quickstart.md` 中的测试清单
- **性能目标**: 页面加载 < 2秒, 搜索响应 < 1秒, 分页加载 < 500ms
- **数据库准备**: Phase 1 必须完成,否则性别选择无法正常工作
