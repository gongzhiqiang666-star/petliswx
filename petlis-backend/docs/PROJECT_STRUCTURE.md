# PETLIS 检测申请项目结构说明

## 项目路径
```
/Users/gong/claudelife/PETLIS/petlis-backend
```

## 完整目录结构
```
src/main/java/com/petlis/
├── common/
│   ├── annotation/          # 自定义注解
│   ├── aspect/              # AOP切面
│   ├── enums/               # 枚举类
│   │   └── OrderStatusEnum.java (订单状态枚举)
│   ├── exception/           # 异常类
│   │   ├── BusinessException.java
│   │   └── ResourceNotFoundException.java
│   ├── filter/              # 过滤器
│   │   └── JwtAuthenticationFilter.java
│   ├── response/            # 统一响应格式
│   │   ├── Result.java
│   │   └── ResultCode.java
│   ├── security/            # 安全相关
│   └── util/                # 工具类
│       ├── DateUtil.java
│       ├── JwtUtil.java
│       └── WechatUtil.java
├── config/                  # 配置类
│   ├── AppConfig.java
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   └── WebConfig.java
├── controller/              # 控制器
│   └── api/
│       ├── AuthController.java
│       └── OrderController.java (检测申请控制器)
├── dto/                     # 数据传输对象
│   ├── request/             # 请求DTO
│   │   ├── OrderCreateRequest.java (创建检测申请请求)
│   │   ├── OrderUpdateRequest.java (更新检测申请请求)
│   │   ├── OrderQueryRequest.java (查询检测申请请求)
│   │   ├── PetCreateRequest.java
│   │   ├── PetQueryRequest.java
│   │   ├── PetUpdateRequest.java
│   │   └── WechatLoginRequest.java
│   └── response/            # 响应DTO
│       ├── OrderDetailResponse.java (检测申请详情响应)
│       ├── OrderResponse.java (检测申请响应)
│       ├── PetResponse.java
│       └── WechatLoginResponse.java
├── entity/                  # 实体类
│   ├── base/                # 基础实体
│   │   ├── ActivableEntity.java
│   │   └── BaseEntity.java
│   ├── business/            # 业务实体
│   │   ├── DiagnosisDetail.java (检测明细实体)
│   │   ├── DiagnosisDetailInfo.java (检测报告内容实体)
│   │   ├── ListDiagnosisProject.java (检测申请主表实体)
│   │   └── User.java (用户实体)
│   └── dict/                # 字典实体
│       ├── City.java
│       ├── DictSex.java
│       ├── InfoAnimal.java
│       ├── InfoHospital.java
│       ├── InfoSampleType.java
│       ├── InfoTestType.java
│       ├── InfoTestTypePrice.java (检测项目价格实体)
│       └── InfoVariety.java
├── mapper/                  # 数据转换器
│   ├── OrderMapper.java (订单数据转换Mapper)
│   └── PetMapper.java
├── repository/              # 数据访问层
│   ├── DiagnosisDetailInfoRepository.java
│   ├── DiagnosisDetailRepository.java (检测明细Repository)
│   ├── InfoTestTypePriceRepository.java
│   ├── ListDiagnosisProjectRepository.java (检测申请Repository)
│   └── ... (其他Repository)
├── service/                 # 服务层接口
│   ├── AuthService.java
│   ├── OrderService.java (检测申请Service接口)
│   └── PetService.java
├── service/impl/            # 服务层实现
│   ├── AuthServiceImpl.java
│   ├── OrderServiceImpl.java (检测申请Service实现)
│   └── PetServiceImpl.java
├── vo/                      # 视图对象
│   ├── OrderVO.java (检测申请视图对象)
│   └── PetVO.java
└── PetlisApplication.java   # 主启动类
```

## 本次实现新增文件

### 1. DTO层 (5个文件)
- `/src/main/java/com/petlis/dto/request/OrderCreateRequest.java`
- `/src/main/java/com/petlis/dto/request/OrderUpdateRequest.java`
- `/src/main/java/com/petlis/dto/request/OrderQueryRequest.java`
- `/src/main/java/com/petlis/dto/response/OrderResponse.java`
- `/src/main/java/com/petlis/dto/response/OrderDetailResponse.java`

### 2. VO层 (1个文件)
- `/src/main/java/com/petlis/vo/OrderVO.java`

### 3. Mapper层 (1个文件)
- `/src/main/java/com/petlis/mapper/OrderMapper.java`

### 4. Service层 (2个文件)
- `/src/main/java/com/petlis/service/OrderService.java`
- `/src/main/java/com/petlis/service/impl/OrderServiceImpl.java`

### 5. Controller层 (1个文件)
- `/src/main/java/com/petlis/controller/api/OrderController.java`

### 6. Repository层 (2个文件扩展)
- `/src/main/java/com/petlis/repository/ListDiagnosisProjectRepository.java` (新增查询方法)
- `/src/main/java/com/petlis/repository/DiagnosisDetailRepository.java` (新增查询方法)

## 关键文件说明

### 1. OrderStatusEnum.java
定义订单状态枚举和状态流转验证逻辑:
- DRAFT(-2, "草稿")
- PENDING_REVIEW(0, "待审核")
- REVIEW_FAILED(1, "审核未通过")
- WAITING_REPORT(2, "等待报告")
- COMPLETED(3, "已完成")
- CANCELLED(4, "已取消")

### 2. ListDiagnosisProject.java
检测申请主表实体,包含:
- 宠物信息(姓名、品种、年龄、生日等)
- 采样信息(采样类型、采样部位、采样日期等)
- 畜主信息(姓名、联系方式等)
- 状态和费用信息

### 3. DiagnosisDetail.java
检测明细实体,包含:
- 检测项目ID和价格信息
- 支付状态
- 诊断状态

### 4. InfoTestTypePrice.java
检测项目价格实体,用于获取价格信息

### 5. OrderService.java
定义8个核心业务方法:
- createOrder: 创建检测申请
- addOrderItem: 添加检测项目
- removeOrderItem: 删除检测项目
- updateOrder: 更新申请信息
- updateOrderStatus: 更新申请状态
- queryOrders: 查询申请列表
- getOrderDetail: 获取申请详情
- calculateTotalFee: 计算申请总费用

### 6. OrderController.java
提供8个RESTful API端点:
- POST /api/orders - 创建检测申请
- GET /api/orders - 查询申请列表
- GET /api/orders/{id} - 获取申请详情
- PUT /api/orders/{id} - 更新申请信息
- PUT /api/orders/{id}/status - 更新申请状态
- POST /api/orders/{id}/items - 添加检测项目
- DELETE /api/orders/{id}/items/{itemId} - 删除检测项目
- GET /api/orders/{id}/fee - 计算申请总费用

## 数据库表依赖

### 主表
- `lin_lb_listdiagnosisproject` (检测申请主表)

### 从表
- `lin_lb_diagnosisdetail` (检测明细表)
- `lin_lb_diagnosisdetail_info` (检测报告内容表)

### 字典表
- `lin_lb_infotesttype_price` (检测项目价格表)
- `lin_lb_infoanimal` (动物物种表)
- `lin_lb_infosampletype` (采样类型表)
- `lin_lb_infovariety` (品种表)
- `lin_lb_dictsex` (性别字典表)
- `lin_lb_infohospital` (医院表)
- `lin_lb_city` (城市表)

## 技术栈
- Java 17
- Spring Boot 3.x
- Spring Data JPA
- Jakarta Validation
- Lombok
- Swagger/OpenAPI 3.0
- MySQL 8.0

## 编码规范
- 所有文件使用UTF-8编码
- 所有public方法添加中文Javadoc
- 使用Lombok简化代码
- 遵循RESTful设计规范
- 统一使用Result响应格式

## 下一步工作
1. 集成JWT认证,从SecurityContext获取当前用户信息
2. 添加操作日志功能
3. 编写单元测试和集成测试
4. 性能优化(缓存、分页优化)
5. 前后端联调测试
