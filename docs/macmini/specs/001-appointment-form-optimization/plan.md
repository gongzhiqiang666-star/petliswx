# Implementation Plan: 预约信息填写菜单优化

**Branch**: `001-appointment-form-optimization` | **Date**: 2025-12-30 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-appointment-form-optimization/spec.md`

## Summary

优化宠物检测预约信息填写流程，包括：年龄输入改为双选择器（年/月）并转换为"Y/M"格式存储、体重字段改为非必填、检测项目选择页面实现分页加载（每页20条）+ 实时搜索（300ms防抖）、样本类型使用独立页面选择器、订单预览页面完整展示所有字段（空值显示"未填写"）、订单提交时更新状态为待审核并生成德国报告编号（RAB{年}{订单ID}）。

技术方案：微信小程序前端实现UI优化，Spring Boot后端实现业务逻辑、数据验证和报告编号生成。

## Technical Context

**Language/Version**:
- 前端：微信小程序（WXML、WXSS、JavaScript）
- 后端：Java 17

**Primary Dependencies**:
- 前端：微信小程序原生框架、wx API
- 后端：Spring Boot 3.4.1、Spring Data JPA、Spring Validation、Lombok、MapStruct

**Storage**:
- MySQL 8.0+ (utf8mb4字符集)
- 表：lin_lb_diagnosisproject（订单主表）、lin_lb_diagnosisdetail（明细表）、lin_lb_sampletype（样本类型）、lin_lb_infotesttype（检测项目）、lin_lb_infotesttype_price（价格表）

**Testing**:
- 前端：微信开发者工具人工测试
- 后端：JUnit 5、Spring Boot Test、集成测试

**Target Platform**:
- 前端：微信小程序（iOS/Android）
- 后端：Java 17+ server (Tomcat)

**Project Type**: web (小程序前端 + Spring Boot后端)

**Performance Goals**:
- 检测项目初始加载时间 < 1秒
- 分页加载响应时间 < 500毫秒
- 搜索防抖延迟 300ms
- 实时搜索响应时间 < 300ms

**Constraints**:
- 后端必须遵循RESTful API设计（constitution原则I）
- 严格分层架构：Controller → Service → DAO（constitution原则II）
- 所有输入数据必须验证（constitution原则III）
- 禁止修改数据库结构来适配前端（constitution原则VI）
- 金额字段使用BigDecimal（constitution原则IV）
- 日期类型使用LocalDate/LocalDateTime（constitution原则IV）

**Scale/Scope**:
- 检测项目数量：预计100-500条
- 并发用户：预计100-500
- 涉及页面：5个（预约填写、检测项目选择、样本类型选择、预览、已有页面修改）
- 涉及API：3-5个后端接口

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Design Evaluation

| Principle | Status | Notes |
|-----------|--------|-------|
| I. RESTful API 优先设计 | ✅ PASS | 所有后端接口将使用标准REST风格（GET/POST）、统一响应格式 |
| II. 分层架构与职责分离 | ✅ PASS | 遵循Controller → Service → DAO三层架构，使用DTO/VO分离 |
| III. 数据验证与安全性 | ✅ PASS | 使用@Valid/@Validated校验，年龄范围验证（0-100岁），狂犬检测特殊验证 |
| IV. 数据库规范与一致性 | ✅ PASS | 使用JPA实体映射，金额BigDecimal，日期LocalDate，逻辑删除IsActive |
| V. 异常处理与日志记录 | ✅ PASS | 使用@ControllerAdvice统一异常处理，关键操作记录日志 |
| VI. 微信小程序适配性 | ✅ PASS | 后端提供REST API，不修改数据库结构适配前端，数据来源于后端服务 |
| VII. 代码质量与文档规范 | ✅ PASS | 添加Javadoc注释，遵循阿里巴巴Java开发手册 |

**Overall Status**: ✅ **ALL GATES PASSED** - 可以进入Phase 0研究阶段

### Post-Design Evaluation

**Phase 1设计完成后重新评估**:

| Principle | Status | Notes |
|-----------|--------|-------|
| I. RESTful API 优先设计 | ✅ PASS | API契约已定义，使用标准GET/POST，统一响应格式Result<T> |
| II. 分层架构与职责分离 | ✅ PASS | Controller → Service → Repository三层架构清晰，DTO用于数据传输 |
| III. 数据验证与安全性 | ✅ PASS | 使用@AgeFormat自定义注解验证年龄，狂犬检测特殊验证，防SQL注入（JPA） |
| IV. 数据库规范与一致性 | ✅ PASS | 无数据库结构变更，使用已有表，金额BigDecimal，日期LocalDate |
| V. 异常处理与日志记录 | ✅ PASS | 使用BusinessException统一异常，关键操作记录日志（订单创建、状态变更） |
| VI. 微信小程序适配性 | ✅ PASS | 后端提供REST API，不修改数据库结构，前端适配微信UI规范 |
| VII. 代码质量与文档规范 | ✅ PASS | 添加Javadoc注释，遵循命名规范，代码行长度控制 |

**Overall Status**: ✅ **ALL GATES PASSED (Post-Design)** - 设计符合所有宪章原则，可以进入实施阶段

## Project Structure

### Documentation (this feature)

```text
specs/001-appointment-form-optimization/
├── plan.md              # 本文件 (/speckit.plan命令输出)
├── research.md          # Phase 0输出 - 技术调研
├── data-model.md        # Phase 1输出 - 数据模型
├── quickstart.md        # Phase 1输出 - 快速开始指南
├── contracts/           # Phase 1输出 - API契约
│   ├── order-api.yaml   # 订单相关API
│   └── dictionary-api.yaml # 数据字典API
└── tasks.md             # Phase 2输出 (/speckit.tasks命令 - 非本文件生成)
```

### Source Code (repository root)

```text
# 前端：微信小程序
miniprogram/
├── pages/
│   ├── order-create/              # 预约信息填写页面（修改）
│   │   ├── order-create.js
│   │   ├── order-create.wxml
│   │   └── order-create.wxss
│   ├── order-test-items/          # 检测项目选择页面（修改）
│   │   ├── order-test-items.js
│   │   ├── order-test-items.wxml
│   │   └── order-test-items.wxss
│   ├── sample-type-select/        # 样本类型独立选择页面（新建）
│   │   ├── sample-type-select.js
│   │   ├── sample-type-select.wxml
│   │   └── sample-type-select.wxss
│   └── order-preview/             # 订单预览页面（修改）
│       ├── order-preview.js
│       ├── order-preview.wxml
│       └── order-preview.wxss
├── components/                     # 可复用组件
│   └── age-picker/                # 年龄选择器组件（新建）
│       ├── age-picker.js
│       ├── age-picker.wxml
│       └── age-picker.wxss
└── api/                           # API封装
    └── index.js                   # 已有，可能需要扩展

# 后端：Spring Boot
petlis-backend/
├── src/main/java/com/petlis/
│   ├── controller/api/
│   │   ├── OrderController.java           # 已有，可能需要修改
│   │   └── DictionaryController.java      # 已有，可能需要扩展
│   ├── service/
│   │   ├── OrderService.java              # 已有，需要修改
│   │   ├── impl/OrderServiceImpl.java     # 已有，需要修改
│   │   └── DictionaryService.java         # 已有，可能需要扩展
│   ├── repository/
│   │   ├── DiagnosisProjectRepository.java    # 已有
│   │   ├── DiagnosisDetailRepository.java     # 已有
│   │   └── InfoTestTypeRepository.java        # 已有
│   ├── entity/
│   │   ├── DiagnosisProject.java             # 已有
│   │   ├── DiagnosisDetail.java              # 已有
│   │   ├── InfoTestType.java                 # 已有
│   │   └── InfoTestTypePrice.java            # 已有
│   ├── dto/
│   │   ├── request/
│   │   │   └── OrderCreateRequest.java       # 已有，需要修改
│   │   └── response/
│   │       ├── OrderResponse.java            # 已有
│   │       └── TestTypePageResponse.java     # 新建（分页响应）
│   ├── common/
│   │   ├── exception/BusinessException.java  # 已有
│   │   ├── response/Result.java              # 已有
│   │   └── enums/OrderStatusEnum.java        # 已有
│   └── util/
│       └── ReportNOGenerator.java            # 新建（报告编号生成工具）
└── src/main/resources/
    └── application.yml
```

**Structure Decision**: 采用Option 2 (Web application) - 微信小程序前端 + Spring Boot后端。前端负责UI交互和表单验证，后端负责业务逻辑、数据持久化和API接口。前后端通过REST API通信，使用标准JSON格式。

## Complexity Tracking

> 本功能不违反宪章原则，无需填写此表。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

---

## Phase 0: Research & Technology Decisions

### Research Tasks

#### 1. 微信小程序选择器组件最佳实践

**Research Question**: 如何在微信小程序中实现两个并排的独立选择器（年龄年份0-30，月份0-11）？

**Findings**:
- **Decision**: 使用微信原生 `<picker>` 组件，mode="selector"，两个picker并排布局
- **Rationale**:
  - 原生picker性能最佳，符合微信设计规范
  - 支持动态数据源（年份0-30，月份0-11）
  - 用户熟悉度高，交互一致性好
- **Alternatives Considered**:
  - 自定义滚动选择器：开发成本高，性能不如原生
  - 下拉选择框：不适合数字范围选择

**Implementation Notes**:
```xml
<picker mode="selector" range="{{years}}" value="{{yearIndex}}" bindchange="onYearChange">
  <view>{{years[yearIndex]}}岁</view>
</picker>
<picker mode="selector" range="{{months}}" value="{{monthIndex}}" bindchange="onMonthChange">
  <view>{{months[monthIndex]}}个月</view>
</picker>
```

#### 2. 微信小程序分页加载最佳实践

**Research Question**: 如何实现检测项目的分页加载（每页20条）+ 滚动到底部自动加载下一页？

**Findings**:
- **Decision**: 使用 `scroll-view` 组件 + `bindscrolltolower` 事件 + 页码参数
- **Rationale**:
  - scroll-view是微信官方推荐的滚动容器组件
  - bindscrolltolower事件在滚动到底部时触发，适合自动分页
  - 配合loading状态提示，用户体验良好
- **Alternatives Considered**:
  - 页面滚动 + onReachBottom：适用于整页滚动，不适合局部列表
  - 上拉加载更多：与scroll-view功能类似，但scroll-view更适合嵌入页面

**Implementation Notes**:
```javascript
data: {
  testItems: [],
  page: 1,
  pageSize: 20,
  hasMore: true,
  loading: false
},

onScrollToLower() {
  if (this.data.loading || !this.data.hasMore) return;
  this.loadMore();
},

async loadMore() {
  this.setData({ loading: true });
  const result = await api.testTypes.list({
    page: this.data.page,
    pageSize: this.data.pageSize
  });
  this.setData({
    testItems: [...this.data.testItems, ...result.data],
    page: this.data.page + 1,
    hasMore: result.data.length === this.data.pageSize,
    loading: false
  });
}
```

#### 3. 搜索防抖实现

**Research Question**: 如何在微信小程序中实现300ms防抖的实时搜索？

**Findings**:
- **Decision**: 使用setTimeout + clearTimeout实现防抖
- **Rationale**:
  - 简单高效，无需引入第三方库
  - 防抖时间可配置（300ms）
  - 每次输入清除上一次定时器，延迟执行搜索
- **Alternatives Considered**:
  - 使用Lodash防抖函数：需要引入额外库，增加包体积
  - 节流：不适用于搜索场景（节流会定期执行，不适合用户输入）

**Implementation Notes**:
```javascript
data: {
  searchKeyword: '',
  searchTimer: null
},

onSearchInput(e) {
  const keyword = e.detail.value;
  this.setData({ searchKeyword: keyword });

  // 清除上一次定时器
  if (this.data.searchTimer) {
    clearTimeout(this.data.searchTimer);
  }

  // 设置新的定时器，300ms后执行搜索
  this.data.searchTimer = setTimeout(() => {
    this.performSearch(keyword);
  }, 300);
},

async performSearch(keyword) {
  const result = await api.testTypes.search({ keyword });
  this.setData({ testItems: result.data });
}
```

#### 4. Spring Boot分页查询最佳实践

**Research Question**: 如何在Spring Boot后端实现高效的分页查询（支持搜索）？

**Findings**:
- **Decision**: 使用Spring Data JPA的 `Page<T>` 和 `Pageable` 接口
- **Rationale**:
  - Spring Data JPA原生支持分页，自动计算总数、总页数
  - Pageable支持排序、分页参数封装
  - Repository方法返回Page<T>，包含完整的分页信息
- **Alternatives Considered**:
  - 手动分页（List.subList）：需要手动计算总数、分页逻辑，容易出错
  - MyBatis分页插件：需要额外依赖，Spring Data JPA已足够

**Implementation Notes**:
```java
// Repository
@Repository
public interface InfoTestTypeRepository extends JpaRepository<InfoTestType, Integer> {
    // 支持按名称模糊查询 + 分页
    Page<InfoTestType> findByTestNameContaining(String keyword, Pageable pageable);
}

// Service
@Service
public class TestTypeServiceImpl implements TestTypeService {
    @Autowired
    private InfoTestTypeRepository repository;

    public Page<InfoTestType> listTestTypes(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return repository.findByTestNameContaining(keyword, pageable);
        }
        return repository.findAll(pageable);
    }
}

// Controller
@GetMapping("/test-types")
public Result<Page<InfoTestType>> listTestTypes(
    @RequestParam(required = false) String keyword,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Page<InfoTestType> result = testTypeService.listTestTypes(keyword, page, size);
    return Result.success(result);
}
```

#### 5. 年龄格式转换与验证

**Research Question**: 如何在前端将年龄选择转换为"Y/M"格式，并在后端验证年龄合理性？

**Findings**:
- **Decision**:
  - 前端：选择年份和月份后，JavaScript拼接为"Y/M"格式（如"3Y7M"、"5M"）
  - 后端：使用正则表达式验证格式，提取年月值进行合理性检查（0-100岁）
- **Rationale**:
  - "Y/M"格式简洁明了，符合用户需求
  - 正则验证确保格式正确，提取数值进行范围验证
  - 只选年份存为"NY"，只选月份存为"NM"，都选存为"NYM"
- **Alternatives Considered**:
  - 分别存储年份和月份字段：增加数据库复杂度，不必要
  - 存储月龄（总月数）：不直观，用户无法直接理解

**Implementation Notes**:
```javascript
// 前端：格式化年龄
formatAge(year, month) {
  if (year > 0 && month > 0) {
    return `${year}Y${month}M`;
  } else if (year > 0) {
    return `${year}Y`;
  } else if (month > 0) {
    return `${month}M`;
  }
  return '';
}
```

```java
// 后端：验证年龄格式
public class AgeValidator {
    private static final Pattern AGE_PATTERN = Pattern.compile("^(\\d+)Y(\\d+)M$|^(\\d+)Y$|^(\\d+)M$");

    public static void validate(String age) {
        if (age == null || age.trim().isEmpty()) {
            throw new BusinessException("年龄不能为空");
        }

        Matcher matcher = AGE_PATTERN.matcher(age);
        if (!matcher.matches()) {
            throw new BusinessException("年龄格式错误，正确格式如：3Y7M、5M、3Y");
        }

        // 提取年份和月份
        int years = 0, months = 0;
        if (matcher.group(1) != null && matcher.group(2) != null) {
            years = Integer.parseInt(matcher.group(1));
            months = Integer.parseInt(matcher.group(2));
        } else if (matcher.group(3) != null) {
            years = Integer.parseInt(matcher.group(3));
        } else if (matcher.group(4) != null) {
            months = Integer.parseInt(matcher.group(4));
        }

        // 验证范围：总月数不超过1200（100岁）
        int totalMonths = years * 12 + months;
        if (totalMonths > 1200 || totalMonths <= 0) {
            throw new BusinessException("年龄超出合理范围（0-100岁）");
        }
    }
}
```

### Research Summary

**Technology Stack Confirmed**:
- 前端：微信原生小程序框架（无额外依赖）
- 后端：Spring Boot 3.4.1 + Spring Data JPA + Spring Validation
- 数据库：MySQL 8.0+ (已有表结构不变)

**Key Decisions**:
1. 年龄选择器：两个并排的原生picker组件
2. 分页加载：scroll-view + bindscrolltolower + Pageable
3. 搜索防抖：setTimeout + clearTimeout (300ms)
4. 年龄格式：前端拼接"Y/M"，后端正则验证
5. 样本类型选择：独立页面（参考breed-select实现）

**No NEEDS CLARIFICATION items remain** - 可以进入Phase 1设计阶段。

---

## Phase 1: Design & Contracts

### Data Model

详见 [data-model.md](./data-model.md)

**Key Entities**:
1. **DiagnosisProject** (订单主表) - 修改age字段验证逻辑，添加reportNO生成
2. **DiagnosisDetail** (明细表) - 无结构变化，使用已有逻辑
3. **InfoTestType** (检测项目) - 分页查询支持
4. **InfoTestTypePrice** (价格表) - 已有，用于价格查询
5. **SampleType** (样本类型) - 已有，用于样本类型选择

**State Transitions**:
- SampleStatus: -2 (草稿) → 0 (待审核) [在提交订单时更新]

### API Contracts

详见 `/specs/001-appointment-form-optimization/contracts/` 目录：

1. **order-api.yaml** - 订单创建相关API
   - POST /api/orders - 创建订单（已存在，验证逻辑需要增强）
   - GET /api/orders/{id} - 查询订单详情（已存在）

2. **dictionary-api.yaml** - 数据字典相关API
   - GET /api/dictionary/test-types - 分页查询检测项目（新增分页参数）
   - GET /api/dictionary/sample-types - 查询样本类型列表（已存在）
   - GET /api/dictionary/test-types/search - 搜索检测项目（新增，或合并到分页接口）

### Quick Start Guide

详见 [quickstart.md](./quickstart.md)

---

**Phase 0 Complete** ✅ - Research.md已完成，所有技术决策已确定
**Phase 1 Complete** ✅ - Data model、API contracts、Quickstart已生成
**Ready for Phase 2**: 运行 `/speckit.tasks` 生成实施任务清单
