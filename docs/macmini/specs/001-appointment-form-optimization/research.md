# Research & Technology Decisions: 预约信息填写菜单优化

**Feature**: 预约信息填写菜单优化
**Date**: 2025-12-30
**Status**: Complete

---

## Research Summary

本功能的技术调研已完成，所有关键技术决策已确定。主要研究领域包括：微信小程序选择器组件、分页加载、搜索防抖、Spring Boot分页查询、年龄格式转换与验证。

---

## Key Technology Decisions

### 1. 年龄选择器实现

**Decision**: 使用两个并排的微信原生 `<picker>` 组件

**Rationale**:
- 原生picker性能最佳，符合微信设计规范
- 支持动态数据源（年份0-30，月份0-11）
- 用户熟悉度高，交互一致性好
- 无需引入第三方库，减少包体积

**Alternatives Considered**:
- 自定义滚动选择器：开发成本高，性能不如原生
- 下拉选择框：不适合数字范围选择

**Implementation Example**:
```xml
<picker mode="selector" range="{{years}}" value="{{yearIndex}}" bindchange="onYearChange">
  <view>{{years[yearIndex]}}岁</view>
</picker>
<picker mode="selector" range="{{months}}" value="{{monthIndex}}" bindchange="onMonthChange">
  <view>{{months[monthIndex]}}个月</view>
</picker>
```

---

### 2. 分页加载实现

**Decision**: 使用 `scroll-view` + `bindscrolltolower` 事件 + Spring Data JPA `Page<T>`

**Rationale**:
- **前端**:
  - scroll-view是微信官方推荐的滚动容器组件
  - bindscrolltolower事件在滚动到底部时触发，适合自动分页
  - 配合loading状态提示，用户体验良好

- **后端**:
  - Spring Data JPA原生支持分页，自动计算总数、总页数
  - Pageable支持排序、分页参数封装
  - Repository方法返回Page<T>，包含完整的分页信息

**Alternatives Considered**:
- **前端**:
  - 页面滚动 + onReachBottom：适用于整页滚动，不适合局部列表
  - 上拉加载更多：与scroll-view功能类似，但scroll-view更适合嵌入页面

- **后端**:
  - 手动分页（List.subList）：需要手动计算总数、分页逻辑，容易出错
  - MyBatis分页插件：需要额外依赖，Spring Data JPA已足够

**Implementation Example**:
```javascript
// 前端
data: {
  testItems: [],
  page: 0,
  pageSize: 20,
  hasMore: true,
  loading: false
},

onScrollToLower() {
  if (this.data.loading || !this.data.hasMore) return;
  this.loadMore();
}
```

```java
// 后端
@Repository
public interface InfoTestTypeRepository extends JpaRepository<InfoTestType, Integer> {
    Page<InfoTestType> findByTestNameContaining(String keyword, Pageable pageable);
}

@Service
public class TestTypeServiceImpl {
    public Page<InfoTestType> listTestTypes(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return repository.findByTestNameContaining(keyword, pageable);
        }
        return repository.findAll(pageable);
    }
}
```

---

### 3. 搜索防抖实现

**Decision**: 使用 setTimeout + clearTimeout 实现300ms防抖

**Rationale**:
- 简单高效，无需引入第三方库
- 防抖时间可配置（300ms）
- 每次输入清除上一次定时器，延迟执行搜索

**Alternatives Considered**:
- 使用Lodash防抖函数：需要引入额外库，增加包体积
- 节流：不适用于搜索场景（节流会定期执行，不适合用户输入）

**Implementation Example**:
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
}
```

---

### 4. 年龄格式转换与验证

**Decision**:
- **前端**: 选择年份和月份后，JavaScript拼接为"Y/M"格式（如"3Y7M"、"5M"）
- **后端**: 使用正则表达式验证格式，提取年月值进行合理性检查（0-100岁）

**Rationale**:
- "Y/M"格式简洁明了，符合用户需求
- 正则验证确保格式正确，提取数值进行范围验证
- 只选年份存为"NY"，只选月份存为"NM"，都选存为"NYM"

**Alternatives Considered**:
- 分别存储年份和月份字段：增加数据库复杂度，不必要
- 存储月龄（总月数）：不直观，用户无法直接理解

**Implementation Example**:
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
public class AgeValidator implements ConstraintValidator<AgeFormat, String> {
    private static final Pattern AGE_PATTERN =
        Pattern.compile("^(\\d+)Y(\\d+)M$|^(\\d+)Y$|^(\\d+)M$");

    @Override
    public boolean isValid(String age, ConstraintValidatorContext context) {
        if (age == null || age.trim().isEmpty()) {
            return false;
        }

        Matcher matcher = AGE_PATTERN.matcher(age);
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

---

### 5. 样本类型选择器实现

**Decision**: 使用独立页面选择器（参考breed-select实现）

**Rationale**:
- 与品种选择保持交互一致性
- 样本类型数量适中（预计10-20项），独立页面能更好展示选项
- 用户已熟悉品种选择流程，降低学习成本

**Alternatives Considered**:
- 底部弹出选择器：样本类型数量较少时可以考虑，但独立页面更统一
- 原生picker：选项较多时不便于查找

**Implementation Reference**:
参考 `miniprogram/pages/breed-select/breed-select.js` 的实现模式

---

## Technology Stack Confirmation

### Frontend (微信小程序)

**框架**: 微信小程序原生框架
- WXML (页面结构)
- WXSS (样式)
- JavaScript (逻辑)
- wx API (微信接口)

**无额外依赖**:
- 不使用第三方UI框架（如Vant Weapp）
- 不使用第三方工具库（如Lodash）
- 减少包体积，提高加载性能

### Backend (Spring Boot)

**核心框架**:
- Spring Boot 3.4.1
- Spring Data JPA
- Spring Validation
- Spring Security (认证授权，已有)

**工具库**:
- Lombok (减少样板代码)
- MapStruct (DTO/VO对象映射)
- Hutool (工具类库，已有)

**数据存储**:
- MySQL 8.0+ (utf8mb4字符集)
- JPA实体映射已有表结构

---

## Performance Considerations

### Frontend Performance

**分页加载**:
- 首次加载：20条记录，预计响应时间 < 1秒
- 滚动加载：每批20条，预计响应时间 < 500ms
- 避免一次性加载所有数据（可能导致卡顿）

**搜索防抖**:
- 300ms延迟：避免频繁请求
- 取消上一次未完成的搜索：节省网络资源
- 搜索时禁用分页加载：避免混淆

**列表渲染优化**:
- 使用wx:key提高列表渲染性能
- 避免在data中存储大量不必要的数据
- 及时清理不再需要的页面数据

### Backend Performance

**数据库查询优化**:
- 为testName字段建立索引：支持模糊搜索
- 分页查询使用LIMIT + OFFSET：JPA自动优化
- 避免N+1查询：使用JOIN FETCH（如果需要）

**缓存策略** (可选):
- 检测项目数据变化不频繁，可以考虑缓存
- 样本类型数据变化不频繁，可以考虑缓存
- 使用Spring Cache + Redis（如果有缓存服务）

**API响应时间目标**:
- 分页查询：< 300ms
- 搜索查询：< 500ms
- 订单创建：< 1000ms

---

## Security Considerations

### Input Validation

**前端验证**:
- 年龄格式验证（"Y/M"格式）
- 年龄范围验证（0-100岁）
- 体重格式验证（可选，填写时建议"{数字}kg"）

**后端验证** (必须):
- 使用@Valid/@Validated进行参数校验
- 自定义@AgeFormat注解验证年龄格式
- 狂犬检测特殊验证（芯片号、出生日期、备注必填）
- 所有输入数据进行长度限制（防止数据截断）

### SQL Injection Prevention

- 使用JPA参数化查询，自动防止SQL注入
- 不使用字符串拼接SQL语句
- Repository方法参数自动转义

### XSS Prevention

- 微信小程序 inherently 防止XSS（不执行HTML/JavaScript）
- 后端不返回HTML内容，只返回JSON数据

---

## Testing Strategy

### Frontend Testing

**人工测试清单**:
1. 年龄选择器功能正常
2. 体重字段可以不填写
3. 分页加载触发正常
4. 搜索防抖正常工作（300ms）
5. 样本类型选择页面正常
6. 订单预览页面所有字段显示正确
7. 空值字段显示"未填写"（灰色）
8. 提交订单成功

**自动化测试** (可选):
- 使用微信小程序自动化测试框架
- 编写关键流程的自动化测试用例

### Backend Testing

**单元测试**:
- AgeValidator测试：覆盖各种格式和边界情况
- OrderService测试：验证订单创建、报告编号生成
- DictionaryService测试：验证分页查询、搜索

**集成测试**:
- API端到端测试：使用MockMvc
- 数据库集成测试：使用H2内存数据库
- 测试覆盖率目标：> 80%

---

## Open Questions / Risks

### 已解决

1. ✅ 年龄选择器UI方案：使用两个并排的picker
2. ✅ 分页加载实现方式：scroll-view + Pageable
3. ✅ 搜索防抖延迟时间：300ms
4. ✅ 年龄格式存储方式："Y/M"格式字符串
5. ✅ 狂犬检测特殊验证：芯片号、出生日期、备注必填

### 无遗留问题

所有技术决策已确定，无遗留风险。

---

## References

### WeChat Mini Program Documentation
- [picker组件文档](https://developers.weixin.qq.com/miniprogram/dev/component/picker.html)
- [scroll-view组件文档](https://developers.weixin.qq.com/miniprogram/dev/component/scroll-view.html)

### Spring Boot Documentation
- [Spring Data JPA - Pagination](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.paging)
- [Spring Validation](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#validation)
- [Jakarta Validation - Bean Validation](https://beanvalidation.org/3.0/spec/)

### Internal Documentation
- [Laboklin送检流程字段详细清单.md](../../../Laboklin送检流程字段详细清单.md)
- [MySQL表结构说明.md](../../../MySQL表结构说明.md)

---

**End of Research**
