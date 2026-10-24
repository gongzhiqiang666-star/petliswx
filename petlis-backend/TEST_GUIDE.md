# PETLIS 后端测试指南

## 概述

本文档描述了PETLIS后端服务的测试框架和测试执行方法。

## 测试框架

- **JUnit 5**: 单元测试框架
- **Mockito**: Mock框架，用于模拟依赖
- **Spring Boot Test**: 集成测试支持
- **MockMvc**: MVC测试框架

## 测试类列表

### 1. PdfServiceTest
**路径**: `src/test/java/com/petlis/service/PdfServiceTest.java`

**测试内容**:
- ✓ PDF访问权限验证（成功/失败）
- ✓ PDF生成状态验证
- ✓ 医院隔离验证（防止跨医院访问）
- ✓ Token提取功能
- ✓ PDF预览URL生成

**关键测试用例**:
```java
testGetPdfWithAuth_Success()              // 有权限获取PDF
testGetPdfWithAuth_ReportNotFound()        // 报告不存在
testGetPdfWithAuth_PdfNotGenerated()       // PDF未生成
testGetPdfWithAuth_NoPermission()          // 无权限访问
testGetPdfPreviewUrlWithAuth_Success()     // 获取预览URL
```

### 2. PdfControllerTest
**路径**: `src/test/java/com/petlis/controller/PdfControllerTest.java`

**测试内容**:
- ✓ GET `/api/pdf/view/{detailId}` - 获取PDF文件
- ✓ GET `/api/pdf/preview-url/{detailId}` - 获取预览URL
- ✓ HTTP状态码验证
- ✓ 响应头验证
- ✓ 异常处理

**关键测试用例**:
```java
testGetPdfReport_Success()               // 成功获取PDF
testGetPdfReport_NotFound()              // 报告不存在(400)
testGetPdfReport_NoPermission()          // 无权限(403)
testGetPdfPreviewUrl_Success()           // 成功获取预览URL
```

### 3. OrderControllerTest
**路径**: `src/test/java/com/petlis/controller/OrderControllerTest.java`

**测试内容**:
- ✓ GET `/api/orders/{id}` - 获取订单详情及安全PDF URL
- ✓ GET `/api/orders` - 查询订单列表
- ✓ POST `/api/orders` - 创建订单
- ✓ PUT `/api/orders/{id}/status` - 更新订单状态
- ✓ GET `/api/orders/{id}/fee` - 计算订单费用
- ✓ 安全URL生成验证

**关键测试用例**:
```java
testGetOrderDetail_Success()              // 获取订单详情(含securePdfUrl)
testGetOrderDetail_NoAuthHeader()         // 无Authorization头
testQueryOrders_Success()                 // 查询订单列表
testCreateOrder_NonMonthlyCustomer()      // 创建订单(现结客户)
testCreateOrder_MonthlyCustomer()         // 创建订单(月结客户)
testUpdateOrderStatus_Success()           // 更新订单状态
```

### 4. ReportControllerTest
**路径**: `src/test/java/com/petlis/controller/ReportControllerTest.java`

**测试内容**:
- ✓ GET `/api/reports` - 查询报告列表及安全PDF URL
- ✓ GET `/api/reports/{id}` - 获取报告详情
- ✓ GET `/api/reports/{id}/download` - 下载PDF报告
- ✓ GET `/api/reports/{id}/pdf-url` - 获取PDF URL
- ✓ 分页功能验证
- ✓ 状态过滤验证
- ✓ 安全URL格式验证

**关键测试用例**:
```java
testQueryReports_Success()                // 查询报告列表(含securePdfUrl)
testQueryReports_EmptyList()              // 空列表
testQueryReports_WithStatusFilter()       // 状态过滤
testQueryReports_WithPagination()         // 分页
testGetReportDetail_Success()             // 获取报告详情
testDownloadReportPdf_Success()           // 下载PDF
testSecurePdfUrl_Format()                 // 安全URL格式验证
```

## 快速开始

### 方式1: 使用测试脚本

```bash
# 进入后端目录
cd petlis-backend

# 运行所有测试
./run-tests.sh
```

### 方式2: 使用Maven命令

```bash
# 进入后端目录
cd petlis-backend

# 运行所有测试
mvn clean test

# 运行特定测试类
mvn test -Dtest=PdfServiceTest

# 运行特定测试方法
mvn test -Dtest=PdfServiceTest#testGetPdfWithAuth_Success

# 生成测试报告
mvn clean test surefire-report:report
```

### 方式3: IDE中运行

**IntelliJ IDEA**:
1. 打开测试类
2. 点击类名旁边的绿色运行按钮
3. 或右键点击测试方法，选择"Run"

**Eclipse**:
1. 右键点击测试类
2. 选择"Run As" > "JUnit Test"

## 测试覆盖的功能

### PDF安全访问
- [x] Token验证
- [x] 医院权限验证
- [x] PDF生成状态验证
- [x] 文件读取和返回
- [x] 异常处理

### 订单管理
- [x] 订单详情查询
- [x] 订单列表查询
- [x] 订单创建
- [x] 订单状态更新
- [x] 费用计算
- [x] 安全URL生成

### 报告管理
- [x] 报告列表查询
- [x] 报告详情查询
- [x] PDF下载
- [x] PDF URL获取
- [x] 分页和过滤
- [x] 安全URL生成

## 测试数据

### Mock数据

所有测试使用Mock数据，不需要真实数据库连接:

**PdfServiceTest**:
- 临时PDF文件（在系统临时目录创建）
- Mock的DiagnosisDetail和ListDiagnosisProject实体

**OrderControllerTest**:
- Mock订单数据（ID=1, 状态=已完成）
- Mock检测项（有PDF和无PDF两种）

**ReportControllerTest**:
- Mock报告列表（有PDF和无PDF两种）
- Mock报告详情VO

## 测试报告

### 查看测试报告

**HTML格式**:
```bash
# 生成报告
mvn clean test surefire-report:report

# 打开报告
open target/site/surefire-report.html
```

**命令行输出**:
```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
[INFO] Running com.petlis.service.PdfServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Running com.petlis.controller.PdfControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Running com.petlis.controller.OrderControllerTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Running com.petlis.controller.ReportControllerTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

## 常见问题

### Q: 测试失败怎么办？

**A**: 查看错误堆栈信息，常见原因：
1. 依赖Mock未正确配置
2. 测试数据不符合预期
3. 业务逻辑修改导致测试失效

### Q: 如何调试测试？

**A**: 在IDE中设置断点，以调试模式运行测试：
- IntelliJ IDEA: 右键 > Debug
- Eclipse: 右键 > Debug As > JUnit Test

### Q: 测试太慢怎么办？

**A**: 可以：
1. 只运行特定测试类或方法
2. 跳过集成测试，只运行单元测试
3. 使用并行测试执行（需配置）

### Q: 如何添加新测试？

**A**:
1. 在相应包下创建测试类（命名规范: `XxxTest.java`）
2. 使用`@Test`注解标记测试方法
3. 使用Mockito mock依赖
4. 使用`@BeforeEach`准备测试数据
5. 使用AssertJ或JUnit assertions验证结果

## 测试最佳实践

1. **测试命名**: 使用描述性名称，如`testGetPdfWithAuth_Success()`
2. **AAA模式**: Arrange（准备）-> Act（执行）-> Assert（断言）
3. **独立性**: 每个测试应该独立运行，不依赖其他测试
4. **Mock外部依赖**: 使用Mock避免依赖真实服务
5. **测试覆盖率**: 目标是覆盖主要业务逻辑

## CI/CD集成

这些测试可以在CI/CD流水线中自动运行:

```yaml
# .github/workflows/test.yml 示例
name: Run Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run tests
        run: mvn clean test
```

## 总结

当前测试套件包含：
- **4个测试类**
- **32+个测试用例**
- **覆盖PDF安全访问、订单管理、报告管理核心功能**

运行测试确保代码质量和功能正确性！
