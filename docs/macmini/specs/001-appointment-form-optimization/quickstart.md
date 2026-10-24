# Quick Start: 预约信息填写菜单优化

**Feature**: 预约信息填写菜单优化
**Date**: 2025-12-30
**Branch**: `001-appointment-form-optimization`

---

## Overview

本指南帮助开发者快速理解并实施预约信息填写菜单优化功能。主要包含以下内容：
- 年龄输入改为双选择器（年/月）并转换为"Y/M"格式
- 体重字段改为非必填
- 检测项目选择页面实现分页加载（每页20条）+ 实时搜索（300ms防抖）
- 样本类型使用独立页面选择器
- 订单预览页面完整展示所有字段（空值显示"未填写"）
- 订单提交时更新状态为待审核并生成德国报告编号

---

## Prerequisites

### Development Environment

**前端开发**:
- 微信开发者工具（最新稳定版）
- 微信小程序AppID（开发或正式）

**后端开发**:
- JDK 17+
- Maven 3.8+
- IDE: IntelliJ IDEA / Eclipse
- MySQL 8.0+

### Dependencies

**后端（pom.xml已包含）**:
```xml
<!-- Spring Boot 3.4.1 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- MySQL驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
</dependency>
```

**前端（微信小程序原生，无额外依赖）**:
- 微信小程序基础库 2.0+

---

## Project Structure

```
PETLIS/
├── miniprogram/                   # 微信小程序前端
│   ├── pages/
│   │   ├── order-create/         # 预约信息填写页面（修改）
│   │   ├── order-test-items/     # 检测项目选择页面（修改）
│   │   ├── sample-type-select/   # 样本类型选择页面（新建）
│   │   └── order-preview/        # 订单预览页面（修改）
│   ├── components/
│   │   └── age-picker/           # 年龄选择器组件（新建）
│   └── api/
│       └── index.js              # API封装
│
├── petlis-backend/               # Spring Boot后端
│   ├── src/main/java/com/petlis/
│   │   ├── controller/api/       # API控制器
│   │   ├── service/              # 业务服务层
│   │   ├── repository/           # 数据访问层
│   │   ├── entity/               # JPA实体
│   │   ├── dto/                  # 数据传输对象
│   │   ├── common/               # 通用组件
│   │   └── util/                 # 工具类
│   └── src/main/resources/
│       └── application.yml       # 配置文件
│
└── specs/001-appointment-form-optimization/  # 本功能的规格文档
    ├── spec.md                   # 功能规格
    ├── plan.md                   # 实施计划
    ├── data-model.md             # 数据模型
    ├── quickstart.md             # 本文件
    └── contracts/                # API契约
        ├── order-api.yaml
        └── dictionary-api.yaml
```

---

## Implementation Steps

### Phase 1: 后端开发

#### Step 1.1: 修改 OrderCreateRequest DTO

**文件**: `petlis-backend/src/main/java/com/petlis/dto/request/OrderCreateRequest.java`

**改动**:
1. 将 `animalWeight` 字段改为非必填
2. 添加 `age` 字段验证注解（自定义@AgeFormat）
3. 确保包含 `diagnosisDetails` 字段

```java
@Data
public class OrderCreateRequest {
    @NotBlank(message = "宠物姓名不能为空")
    @Size(max = 100, message = "宠物姓名长度不能超过100")
    private String animalName;

    @NotNull(message = "物种不能为空")
    private Integer animalTypeId;

    @NotNull(message = "性别不能为空")
    private Integer dictSexId;

    // 新增：年龄格式验证
    @NotBlank(message = "年龄不能为空")
    @AgeFormat(message = "年龄格式错误，正确格式如：3Y7M、5M、3Y")
    private String age;

    // 改为可选
    @Size(max = 20, message = "体重长度不能超过20")
    private String animalWeight;  // 可选

    private String chipNO;

    private LocalDate birthday;  // 狂犬检测必填

    @NotNull(message = "样本类型不能为空")
    private Integer infoSampleTypeId;

    @NotNull(message = "采样日期不能为空")
    private LocalDate collectDt;

    @NotBlank(message = "畜主姓名不能为空")
    @Size(max = 100, message = "畜主姓名长度不能超过100")
    private String ownerName;

    @Size(max = 100, message = "送检医师长度不能超过100")
    private String sendDoctor;

    private String remark;  // 狂犬检测必填

    @NotEmpty(message = "检测项目不能为空")
    private List<DiagnosisDetailItem> diagnosisDetails;

    @Data
    public static class DiagnosisDetailItem {
        @NotNull(message = "检测项目ID不能为空")
        private Integer infoTestTypeId;
    }
}
```

#### Step 1.2: 创建年龄格式验证注解

**文件**: `petlis-backend/src/main/java/com/petlis/common/validation/AgeFormat.java` (新建)

```java
package com.petlis.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AgeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AgeFormat {
    String message() default "年龄格式错误";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**文件**: `petlis-backend/src/main/java/com/petlis/common/validation/AgeValidator.java` (新建)

```java
package com.petlis.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class AgeValidator implements ConstraintValidator<AgeFormat, String> {
    private static final Pattern AGE_PATTERN =
        Pattern.compile("^(\\d+)Y(\\d+)M$|^(\\d+)Y$|^(\\d+)M$");

    @Override
    public boolean isValid(String age, ConstraintValidatorContext context) {
        if (age == null || age.trim().isEmpty()) {
            return false;
        }

        var matcher = AGE_PATTERN.matcher(age);
        if (!matcher.matches()) {
            return false;
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
        return totalMonths > 0 && totalMonths <= 1200;
    }
}
```

#### Step 1.3: 修改 OrderServiceImpl

**文件**: `petlis-backend/src/main/java/com/petlis/service/impl/OrderServiceImpl.java`

**改动点**:
1. createOrder方法中添加年龄验证（已有@Valid自动验证，此处无需额外代码）
2. 确保SampleStatus设置为0（待审核）
3. 生成ReportNO报告编号
4. 狂犬检测特殊验证

```java
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private DiagnosisProjectRepository projectRepository;

    @Autowired
    private DiagnosisDetailRepository detailRepository;

    @Autowired
    private InfoTestTypePriceRepository priceRepository;

    @Override
    @Transactional
    public Integer createOrder(OrderCreateRequest request) {
        // 1. 创建订单主表
        DiagnosisProject project = new DiagnosisProject();
        // ... 映射基本字段 ...
        project.setAge(request.getAge());  // 直接保存，格式已验证
        project.setAnimalWeight(request.getAnimalWeight());  // 可以为null

        // 2. 设置订单状态为待审核
        project.setSampleStatus(OrderStatusEnum.PENDING_REVIEW.getCode());  // 0

        // 3. 保存订单（获取自增ID）
        DiagnosisProject saved = projectRepository.save(project);

        // 4. 生成报告编号
        String reportNO = generateReportNO(saved.getIid());
        saved.setReportNO(reportNO);
        projectRepository.save(saved);

        // 5. 狂犬检测特殊验证
        for (DiagnosisDetailItem item : request.getDiagnosisDetails()) {
            if (item.getInfoTestTypeId().equals(1225)) {  // 狂犬检测ID
                if (saved.getChipNO() == null || saved.getChipNO().trim().isEmpty()) {
                    throw new BusinessException("狂犬检测必须填写芯片号");
                }
                if (saved.getBirthday() == null) {
                    throw new BusinessException("狂犬检测必须填写出生日期");
                }
                if (saved.getRemark() == null || saved.getRemark().trim().isEmpty()) {
                    throw new BusinessException("狂犬检测必须在备注中填写快递单号");
                }
            }
        }

        // 6. 保存检测明细（从价格表获取价格）
        for (DiagnosisDetailItem item : request.getDiagnosisDetails()) {
            // 查询价格
            List<InfoTestTypePrice> prices =
                priceRepository.findByInfoTestTypeid(item.getInfoTestTypeId().longValue());
            InfoTestTypePrice priceInfo = prices.stream()
                .filter(p -> p.getIsActive() != null && p.getIsActive().equals(1))
                .findFirst()
                .orElseThrow(() -> new BusinessException("检测项目价格未启用"));

            // 创建明细
            DiagnosisDetail detail = new DiagnosisDetail();
            detail.setDiagnosisProjectId(saved.getIid());
            detail.setInfoTestTypeId(item.getInfoTestTypeId());
            detail.setTestPrice(BigDecimal.valueOf(priceInfo.getPrice()));
            detail.setOrginalPrice(BigDecimal.valueOf(priceInfo.getPrice()));
            detail.setOrginalDiscount(BigDecimal.valueOf(10));  // 固定折扣10
            detail.setSendMailDt(getDefaultDate());  // 1900-01-01 00:00:00
            detailRepository.save(detail);
        }

        return saved.getIid();
    }

    /**
     * 生成德国报告编号
     * 格式：RAB{年}{订单ID}
     * 例如：RAB20251
     */
    private String generateReportNO(Integer orderId) {
        String year = String.valueOf(java.time.Year.now().getValue());
        return String.format("RAB%s%d", year, orderId);
    }

    private LocalDateTime getDefaultDate() {
        return LocalDateTime.of(1900, 1, 1, 0, 0, 0);
    }
}
```

#### Step 1.4: 修改 Repository 支持分页

**文件**: `petlis-backend/src/main/java/com/petlis/repository/InfoTestTypeRepository.java`

**改动**: 添加分页查询方法

```java
@Repository
public interface InfoTestTypeRepository extends JpaRepository<InfoTestType, Integer> {
    // 支持按名称模糊查询 + 分页
    Page<InfoTestType> findByTestNameContainingAndIsActive(
        String keyword,
        Integer isActive,
        Pageable pageable
    );

    // 查询所有启用的检测项目（分页）
    Page<InfoTestType> findByIsActive(Integer isActive, Pageable pageable);
}
```

#### Step 1.5: 修改 DictionaryController

**文件**: `petlis-backend/src/main/java/com/petlis/controller/api/DictionaryController.java`

**改动**: 添加分页查询接口

```java
@RestController
@RequestMapping("/dictionary")
public class DictionaryController {

    @Autowired
    private InfoTestTypeService testTypeService;

    /**
     * 分页查询检测项目列表
     * GET /api/dictionary/test-types?keyword=血&page=0&size=20
     */
    @GetMapping("/test-types")
    public Result<Page<InfoTestType>> listTestTypes(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("iid").ascending());
        Page<InfoTestType> result = testTypeService.listTestTypes(keyword, pageable);
        return Result.success(result);
    }

    // ... 其他已有方法 ...
}
```

#### Step 1.6: 测试后端API

```bash
# 启动后端服务
cd petlis-backend
mvn spring-boot:run

# 测试创建订单（基础订单）
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "animalName": "旺财",
    "animalTypeId": 1,
    "dictSexId": 1,
    "age": "3Y7M",
    "animalWeight": "15kg",
    "infoSampleTypeId": 1,
    "collectDt": "2025-12-30",
    "ownerName": "张三",
    "remark": "备注",
    "diagnosisDetails": [
      {"infoTestTypeId": 1223}
    ]
  }'

# 测试分页查询检测项目
curl "http://localhost:8081/api/dictionary/test-types?page=0&size=20"

# 测试搜索检测项目
curl "http://localhost:8081/api/dictionary/test-types?keyword=血&page=0&size=20"
```

---

### Phase 2: 前端开发

#### Step 2.1: 创建年龄选择器组件

**文件**: `miniprogram/components/age-picker/age-picker.wxml` (新建)

```xml
<view class="age-picker">
  <view class="picker-container">
    <!-- 年份选择器 -->
    <view class="picker-item">
      <text class="picker-label">年龄</text>
      <picker
        mode="selector"
        range="{{years}}"
        value="{{yearIndex}}"
        bindchange="onYearChange">
        <view class="picker-value">
          {{years[yearIndex]}}岁
        </view>
      </picker>
    </view>

    <!-- 月份选择器 -->
    <view class="picker-item">
      <picker
        mode="selector"
        range="{{months}}"
        value="{{monthIndex}}"
        bindchange="onMonthChange">
        <view class="picker-value">
          {{months[monthIndex]}}个月
        </view>
      </picker>
    </view>
  </view>
</view>
```

**文件**: `miniprogram/components/age-picker/age-picker.js` (新建)

```javascript
Component({
  properties: {
    value: {
      type: String,
      value: ''
    }
  },

  data: {
    years: [],
    months: [],
    yearIndex: 0,
    monthIndex: 0
  },

  lifetimes: {
    attached() {
      // 初始化年份0-30
      const years = Array.from({length: 31}, (_, i) => i);
      // 初始化月份0-11
      const months = Array.from({length: 12}, (_, i) => i);

      this.setData({ years, months });

      // 如果有初始值，解析并设置
      if (this.properties.value) {
        this.parseAge(this.properties.value);
      }
    }
  },

  methods: {
    onYearChange(e) {
      this.setData({ yearIndex: parseInt(e.detail.value) });
      this.triggerAgeChange();
    },

    onMonthChange(e) {
      this.setData({ monthIndex: parseInt(e.detail.value) });
      this.triggerAgeChange();
    },

    triggerAgeChange() {
      const age = this.formatAge();
      this.triggerEvent('change', { value: age });
    },

    formatAge() {
      const years = this.data.years[this.data.yearIndex];
      const months = this.data.months[this.data.monthIndex];

      if (years > 0 && months > 0) {
        return `${years}Y${months}M`;
      } else if (years > 0) {
        return `${years}Y`;
      } else if (months > 0) {
        return `${months}M`;
      }
      return '';
    },

    parseAge(ageStr) {
      const regex = /^(\d+)Y(\d+)M$|^(\d+)Y$|^(\d+)M$/;
      const match = ageStr.match(regex);

      if (match) {
        if (match[1] !== undefined && match[2] !== undefined) {
          // "3Y7M"
          const years = parseInt(match[1]);
          const months = parseInt(match[2]);
          this.setData({
            yearIndex: years,
            monthIndex: months
          });
        } else if (match[3] !== undefined) {
          // "3Y"
          const years = parseInt(match[3]);
          this.setData({ yearIndex: years, monthIndex: 0 });
        } else if (match[4] !== undefined) {
          // "7M"
          const months = parseInt(match[4]);
          this.setData({ yearIndex: 0, monthIndex: months });
        }
      }
    }
  }
});
```

#### Step 2.2: 修改检测项目选择页面（分页+搜索）

**文件**: `miniprogram/pages/order-test-items/order-test-items.wxml` (修改)

```xml
<view class="test-items-page">
  <!-- 搜索框 -->
  <view class="search-bar">
    <input
      class="search-input"
      placeholder="搜索检测项目"
      value="{{searchKeyword}}"
      bindinput="onSearchInput" />
    <view class="clear-icon" wx:if="{{searchKeyword}}" bindtap="onClearSearch">✕</view>
  </view>

  <!-- 检测项目列表 -->
  <scroll-view
    class="test-items-list"
    scroll-y
    bindscrolltolower="onScrollToLower">
    <view
      class="test-item {{item.selected ? 'selected' : ''}}"
      wx:for="{{testItems}}"
      wx:key="iid"
      bindtap="onSelectItem"
      data-item="{{item}}">
      <view class="item-name">{{item.testName}}</view>
      <view class="item-price">¥{{item.price}}</view>
      <view class="check-icon" wx:if="{{item.selected}}">✓</view>
    </view>

    <!-- 加载状态 -->
    <view class="loading" wx:if="{{loading}}">
      <text>加载中...</text>
    </view>

    <!-- 没有更多 -->
    <view class="no-more" wx:if="{{!hasMore && testItems.length > 0}}">
      <text>没有更多了</text>
    </view>

    <!-- 空状态 -->
    <view class="empty" wx:if="{{testItems.length === 0 && !loading}}">
      <text>未找到相关检测项目</text>
    </view>
  </scroll-view>

  <!-- 底部操作栏 -->
  <view class="footer">
    <view class="selected-count">已选{{selectedCount}}项</view>
    <button class="confirm-btn" bindtap="onConfirm">确定</button>
  </view>
</view>
```

**文件**: `miniprogram/pages/order-test-items/order-test-items.js` (修改)

```javascript
const api = require('../../api/index.js');

Page({
  data: {
    testItems: [],
    selectedItems: [],
    searchKeyword: '',
    page: 0,
    pageSize: 20,
    hasMore: true,
    loading: false,
    searchTimer: null
  },

  onLoad() {
    this.loadTestItems();
  },

  /**
   * 加载检测项目列表（分页）
   */
  async loadTestItems() {
    if (this.data.loading || !this.data.hasMore) return;

    this.setData({ loading: true });

    try {
      const result = await api.dictionary.listTestTypes({
        keyword: this.data.searchKeyword,
        page: this.data.page,
        size: this.data.pageSize
      });

      const newItems = result.data.content.map(item => ({
        ...item,
        selected: this.data.selectedItems.some(sel => sel.iid === item.iid)
      }));

      this.setData({
        testItems: [...this.data.testItems, ...newItems],
        page: this.data.page + 1,
        hasMore: !result.data.last,
        loading: false
      });
    } catch (error) {
      console.error('[OrderTestItems] 加载失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
      this.setData({ loading: false });
    }
  },

  /**
   * 搜索输入（带300ms防抖）
   */
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

  /**
   * 执行搜索
   */
  async performSearch(keyword) {
    this.setData({
      testItems: [],
      page: 0,
      hasMore: true
    });
    await this.loadTestItems();
  },

  /**
   * 清除搜索
   */
  onClearSearch() {
    this.setData({ searchKeyword: '' });
    this.performSearch('');
  },

  /**
   * 滚动到底部加载更多
   */
  onScrollToLower() {
    if (!this.data.searchKeyword) {
      // 仅在非搜索模式下支持分页加载
      this.loadTestItems();
    }
  },

  /**
   * 选择/取消选择检测项目
   */
  onSelectItem(e) {
    const item = e.currentTarget.dataset.item;
    const selectedItems = [...this.data.selectedItems];
    const index = selectedItems.findIndex(sel => sel.iid === item.iid);

    if (index >= 0) {
      // 已选中，取消选择
      selectedItems.splice(index, 1);
    } else {
      // 未选中，添加选择
      selectedItems.push(item);
    }

    // 更新列表项的选中状态
    const testItems = this.data.testItems.map(testItem => ({
      ...testItem,
      selected: selectedItems.some(sel => sel.iid === testItem.iid)
    }));

    this.setData({
      selectedItems,
      testItems,
      selectedCount: selectedItems.length
    });
  },

  /**
   * 确认选择
   */
  onConfirm() {
    if (this.data.selectedItems.length === 0) {
      wx.showToast({ title: '请至少选择一项', icon: 'none' });
      return;
    }

    // 将选中的项目存储到全局
    const app = getApp();
    app.globalData.selectedTestItems = this.data.selectedItems;

    wx.navigateBack();
  }
});
```

#### Step 2.3: 创建样本类型选择页面

**文件**: `miniprogram/pages/sample-type-select/sample-type-select.wxml` (新建)

```xml
<view class="sample-type-page">
  <scroll-view class="sample-type-list" scroll-y>
    <view
      class="sample-type-item {{item.iid === selectedId ? 'selected' : ''}}"
      wx:for="{{sampleTypes}}"
      wx:key="iid"
      bindtap="onSelect"
      data-item="{{item}}">
      <view class="item-name">{{item.sampleTypeName}}</view>
      <view class="item-name-en" wx:if="{{item.sampleTypeNameEn}}">
        {{item.sampleTypeNameEn}}
      </view>
      <view class="check-icon" wx:if="{{item.iid === selectedId}}">✓</view>
    </view>
  </scroll-view>
</view>
```

**文件**: `miniprogram/pages/sample-type-select/sample-type-select.js` (新建)

```javascript
const api = require('../../api/index.js');

Page({
  data: {
    sampleTypes: [],
    selectedId: null
  },

  onLoad(options) {
    // 获取已选择的样本类型ID（如果有）
    if (options.selectedId) {
      this.setData({ selectedId: parseInt(options.selectedId) });
    }

    this.loadSampleTypes();
  },

  async loadSampleTypes() {
    try {
      const result = await api.dictionary.listSampleTypes();
      this.setData({ sampleTypes: result.data });
    } catch (error) {
      console.error('[SampleTypeSelect] 加载失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  onSelect(e) {
    const item = e.currentTarget.dataset.item;

    // 存储到全局或缓存
    const pages = getCurrentPages();
    const prevPage = pages[pages.length - 2];

    if (prevPage) {
      prevPage.setData({
        'sampleInfo.infoSampleTypeId': item.iid,
        'sampleInfo.sampleTypeName': item.sampleTypeName
      });
    }

    wx.navigateBack();
  }
});
```

#### Step 2.4: 修改订单预览页面

**文件**: `miniprogram/pages/order-preview/order-preview.wxml` (修改)

```xml
<view class="order-preview-page">
  <!-- 宠物基本信息 -->
  <view class="section">
    <view class="section-title">宠物信息</view>
    <view class="info-list">
      <view class="info-item">
        <text class="label">姓名</text>
        <text class="value">{{formData.petInfo.animalName}}</text>
      </view>
      <view class="info-item">
        <text class="label">品种</text>
        <text class="value">{{formData.petInfo.infoVarietyName || '未填写'}}</text>
      </view>
      <view class="info-item">
        <text class="label">性别</text>
        <text class="value">{{formData.petInfo.genderText}}</text>
      </view>
      <view class="info-item">
        <text class="label">年龄</text>
        <text class="value">{{formData.animalInfo.age || '未填写'}}</text>
      </view>
      <view class="info-item">
        <text class="label">体重</text>
        <text class="value {{formData.animalInfo.animalWeight ? '' : 'empty'}}">
          {{formData.animalInfo.animalWeight || '未填写'}}
        </text>
      </view>
    </view>
  </view>

  <!-- 样本信息 -->
  <view class="section">
    <view class="section-title">样本信息</view>
    <view class="info-list">
      <view class="info-item">
        <text class="label">样本类型</text>
        <text class="value">{{formData.sampleInfo.sampleTypeName || '未填写'}}</text>
      </view>
      <view class="info-item">
        <text class="label">采样日期</text>
        <text class="value">{{formData.sampleInfo.collectDt || '未填写'}}</text>
      </view>
    </view>
  </view>

  <!-- 检测项目 -->
  <view class="section">
    <view class="section-title">检测项目</view>
    <view class="test-items">
      <view class="test-item" wx:for="{{formData.selectedTestItems}}" wx:key="infoTestTypeId">
        <text class="item-name">{{item.testName}}</text>
        <text class="item-price">¥{{item.price}}</text>
      </view>
    </view>
    <view class="total-price">
      <text>总计：</text>
      <text class="price">¥{{totalPrice}}</text>
    </view>
  </view>

  <!-- 底部按钮 -->
  <view class="footer">
    <button class="back-btn" bindtap="onBack">返回修改</button>
    <button class="submit-btn" bindtap="onSubmit" loading="{{submitting}}">
      提交订单
    </button>
  </view>
</view>
```

**文件**: `miniprogram/pages/order-preview/order-preview.wxss` (修改)

```css
/* 添加空值样式 */
.info-item .value.empty {
  color: #999;
}

.section {
  margin-bottom: 32rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 24rpx;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-item .label {
  font-size: 28rpx;
  color: #666;
}

.info-item .value {
  font-size: 28rpx;
  color: #333;
}

.info-item .value.empty {
  color: #999;
}
```

#### Step 2.5: 测试前端页面

```bash
# 启动微信开发者工具
# 打开 miniprogram 目录
# 编译并预览
```

---

## Testing Checklist

### 后端测试

- [ ] 创建基础订单（不含狂犬检测）- 成功
- [ ] 创建狂犬检测订单（含所有必填字段）- 成功
- [ ] 创建狂犬检测订单（缺少芯片号）- 失败，提示"狂犬检测必须填写芯片号"
- [ ] 创建狂犬检测订单（缺少出生日期）- 失败，提示"狂犬检测必须填写出生日期"
- [ ] 创建狂犬检测订单（缺少备注）- 失败，提示"狂犬检测必须在备注中填写快递单号"
- [ ] 年龄格式验证："3Y7M" - 成功
- [ ] 年龄格式验证："5M" - 成功
- [ ] 年龄格式验证："3Y" - 成功
- [ ] 年龄格式验证："200" - 失败，提示"年龄格式错误"
- [ ] 年龄范围验证："101Y" - 失败，提示"年龄超出合理范围"
- [ ] 体重为空 - 成功
- [ ] 体重有值 - 成功
- [ ] 分页查询检测项目（第1页）- 成功，返回20条
- [ ] 分页查询检测项目（滚动加载第2页）- 成功
- [ ] 搜索检测项目 - 成功，返回匹配结果
- [ ] 生成的报告编号格式正确 - "RAB20251"

### 前端测试

- [ ] 年龄选择器显示两个并排的picker
- [ ] 年龄选择器年份范围0-30
- [ ] 年龄选择器月份范围0-11
- [ ] 选择年龄后格式正确（"3Y7M"）
- [ ] 体重字段可以不填写
- [ ] 检测项目页面首次加载显示20条
- [ ] 滚动到底部自动加载下一页
- [ ] 搜索输入300ms后触发搜索
- [ ] 搜索结果正确显示
- [ ] 样本类型选择页面正常打开
- [ ] 选择样本类型后返回上一页并显示选中值
- [ ] 订单预览页面所有字段正确显示
- [ ] 空值字段显示"未填写"（灰色）
- [ ] 提交订单成功
- [ ] 订单状态更新为"待审核"
- [ ] 报告编号生成正确

---

## Common Issues

### Q1: 年龄格式验证失败

**错误**: "年龄格式错误，正确格式如：3Y7M、5M、3Y"

**解决**: 检查前端格式化逻辑是否正确，确保格式为"Y/M"（如"3Y7M"、"5M"、"3Y"）

### Q2: 分页加载不触发

**原因**: scroll-view需要设置固定高度

**解决**: 在WXSS中设置scroll-view高度：
```css
.test-items-list {
  height: calc(100vh - 200rpx);
}
```

### Q3: 搜索防抖不生效

**原因**: searchTimer没有正确清除

**解决**: 确保在每次输入时清除上一次定时器：
```javascript
if (this.data.searchTimer) {
  clearTimeout(this.data.searchTimer);
}
```

### Q4: 报告编号生成重复

**原因**: 并发创建订单时可能有冲突

**解决**: 使用数据库唯一索引约束ReportNO字段，或在报告中添加随机后缀

---

## Next Steps

1. **运行测试**: 按照 Testing Checklist 执行完整测试
2. **代码审查**: 提交代码前进行Code Review
3. **部署上线**: 先部署后端，再发布小程序
4. **用户培训**: 更新用户手册，说明新的操作流程

---

**End of Quick Start**
