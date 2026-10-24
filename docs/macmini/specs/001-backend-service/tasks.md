# Tasks: PETLIS后端API服务

**Input**: Design documents from `/specs/001-backend-service/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/

**Tests**: 本项目不包含自动化测试任务,测试通过手动验证和Swagger UI完成

**Organization**: 任务按用户故事分组,确保每个故事可以独立实现和测试

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行运行(不同文件,无依赖)
- **[Story]**: 任务所属用户故事(US1, US2, US3等)
- 包含精确的文件路径

## Path Conventions

本项目采用Spring Boot标准结构:
- 源代码: `petlis-backend/src/main/java/com/petlis/`
- 测试: `petlis-backend/src/test/java/com/petlis/`
- 资源: `petlis-backend/src/main/resources/`

---

## Phase 1: Setup (共享基础设施)

**Purpose**: 项目初始化和基本结构搭建

- [X] T001 创建项目根目录petlis-backend和标准Maven目录结构
- [X] T002 创建Maven pom.xml,定义Java 17、Spring Boot 3.4、Spring Data JPA、MySQL驱动、Lombok、MapStruct、Springdoc OpenAPI、Hutool等依赖
- [X] T003 [P] 创建application.yml主配置文件,配置端口、应用名称、JPA设置
- [X] T004 [P] 创建application-dev.yml开发环境配置,配置MySQL连接、日志级别
- [X] T005 [P] 创建application-prod.yml生产环境配置,配置生产数据库、性能优化参数
- [X] T006 [P] 创建logback-spring.xml日志配置文件,定义日志格式和文件输出
- [X] T007 创建主启动类PetlisApplication.java在src/main/java/com/petlis/
- [X] T008 [P] 创建通用异常类BusinessException.java在src/main/java/com/petlis/common/exception/
- [X] T009 [P] 创建ResourceNotFoundException.java在src/main/java/com/petlis/common/exception/
- [X] T010 [P] 创建AuthenticationException.java在src/main/java/com/petlis/common/exception/
- [X] T011 [P] 创建Result.java统一响应对象在src/main/java/com/petlis/common/response/
- [X] T012 [P] 创建ResultCode.java响应码接口在src/main/java/com/petlis/common/response/
- [X] T013 [P] 创建ResultCodeEnum.java响应码枚举在src/main/java/com/petlis/common/enums/
- [X] T014 [P] 创建GlobalExceptionHandler.java全局异常处理器在src/main/java/com/petlis/common/exception/,使用@ControllerAdvice
- [X] T015 [P] 创建WebConfig.java配置类在src/main/java/com/petlis/config/,配置CORS、拦截器
- [X] T016 [P] 创建SwaggerConfig.java配置Swagger UI在src/main/java/com/petlis/config/

---

## Phase 2: Foundational (阻塞性前置条件)

**Purpose**: 所有用户故事依赖的核心基础设施

**⚠️ CRITICAL**: 在此阶段完成前,不能开始任何用户故事的开发

### 基础实体

- [X] T017 [P] 创建BaseEntity.java基础实体类在src/main/java/com/petlis/entity/base/,包含ID和创建时间
- [X] T018 [P] 创建ActivableEntity.java可激活实体类在src/main/java/com/petlis/entity/base/,包含IsActive字段

### JPA实体 - 字典表

- [X] T019 [P] 创建InfoAnimal.java实体类在src/main/java/com/petlis/entity/,映射lin_lb_infoanimal表
- [X] T020 [P] 创建InfoVariety.java实体类在src/main/java/com/petlis/entity/dict/,映射lin_lb_infovariety表,注意主键为iid
- [X] T021 [P] 创建DictSex.java实体类在src/main/java/com/petlis/entity/dict/,映射lin_lb_dictsex表
- [X] T022 [P] 创建InfoSampleType.java实体类在src/main/java/com/petlis/entity/dict/,映射lin_lb_infosampletype表
- [X] T023 [P] 创建InfoTestType.java实体类在src/main/java/com/petlis/entity/dict/,映射lin_lb_infotesttype表
- [X] T024 [P] 创建InfoTestTypePrice.java实体类在src/main/java/com/petlis/entity/dict/,映射lin_lb_infotesttype_price表
- [X] T025 [P] 创建City.java实体类在src/main/java/com/petlis/entity/dict/,映射lin_lb_city表
- [X] T026 [P] 创建InfoHospital.java实体类在src/main/java/com/petlis/entity/dict/,映射lin_lb_infohospital表

### JPA实体 - 核心业务表

- [X] T027 [P] 创建User.java实体类在src/main/java/com/petlis/entity/business/,映射lin_user表,配置InfoHospital关联
- [X] T028 [P] 创建ListDiagnosisProject.java实体类在src/main/java/com/petlis/entity/business/,映射lin_lb_listdiagnosisproject表,配置所有关联关系
- [X] T029 [P] 创建DiagnosisDetail.java实体类在src/main/java/com/petlis/entity/business/,映射lin_lb_diagnosisdetail表
- [X] T030 [P] 创建DiagnosisDetailInfo.java实体类在src/main/java/com/petlis/entity/business/,映射lin_lb_diagnosisdetail_info表

### Repository层

- [X] T031 [P] 创建UserRepository接口在src/main/java/com/petlis/repository/,添加findByOpenid和findByHospitalId方法
- [X] T032 [P] 创建InfoAnimalRepository接口在src/main/java/com/petlis/repository/
- [X] T033 [P] 创建InfoVarietyRepository接口在src/main/java/com/petlis/repository/
- [X] T034 [P] 创建DictSexRepository接口在src/main/java/com/petlis/repository/
- [X] T035 [P] 创建InfoSampleTypeRepository接口在src/main/java/com/petlis/repository/
- [X] T036 [P] 创建InfoTestTypeRepository接口在src/main/java/com/petlis/repository/
- [X] T037 [P] 创建InfoTestTypePriceRepository接口在src/main/java/com/petlis/repository/,添加findByInfoTestTypeid方法
- [X] T038 [P] 创建InfoHospitalRepository接口在src/main/java/com/petlis/repository/
- [X] T039 [P] 创建CityRepository接口在src/main/java/com/petlis/repository/
- [X] T040 [P] 创建ListDiagnosisProjectRepository接口在src/main/java/com/petlis/repository/,添加findByHospitalId和findBySampleStatus方法
- [X] T041 [P] 创建DiagnosisDetailRepository接口在src/main/java/com/petlis/repository/,添加findByListDiagnosisProjectId方法
- [X] T042 [P] 创建DiagnosisDetailInfoRepository接口在src/main/java/com/petlis/repository/

### 基础设施 - 安全认证

- [X] T043 创建JwtUtil.java工具类在src/main/java/com/petlis/common/util/,实现JWT生成和验证
- [X] T044 创建WechatUtil.java工具类在src/main/java/com/petlis/common/util/,实现微信code换取openid
- [X] T045 创建SecurityConfig.java安全配置在src/main/java/com/petlis/config/,配置JWT认证过滤器
- [X] T046 创建JwtAuthenticationFilter.java JWT认证过滤器在src/main/java/com/petlis/common/filter/

### 基础设施 - DTO和映射器

- [X] T047 [P] 创建WechatLoginRequest.java DTO在src/main/java/com/petlis/dto/request/
- [X] T048 [P] 创建WechatLoginResponse.java DTO在src/main/java/com/petlis/dto/response/
- [X] T049 [P] 创建OrderStatusEnum.java枚举在src/main/java/com/petlis/common/enums/,定义订单状态常量
- [X] T050 [P] 创建DateUtil.java工具类在src/main/java/com/petlis/common/util/,处理日期时间转换

**Checkpoint**: 基础设施完成 - 用户故事实现现在可以并行开始

---

## Phase 3: User Story 1 - 微信用户认证与医院关联 (Priority: P1) 🎯 MVP

**Goal**: 实现微信小程序用户登录,验证身份并关联医院,返回JWT token

**Independent Test**: 使用微信小程序模拟登录,获取token,通过token访问受保护接口,验证数据权限隔离

### 实现任务

- [X] T051 [US1] 创建AuthService.java接口在src/main/java/com/petlis/service/,定义wechatLogin、refreshToken、logout方法签名
- [X] T052 [US1] 创建AuthServiceImpl.java实现类在src/main/java/com/petlis/service/impl/,实现微信登录逻辑:
  - 调用微信API换取openid
  - 查询或创建User记录
  - 关联默认医院(hospitalId=1)
  - 生成JWT token
- [X] T053 [US1] 创建AuthController.java控制器在src/main/java/com/petlis/controller/api/,实现POST /api/auth/wechat/login接口
- [X] T054 [US1] 创建POST /api/auth/refresh接口,刷新token
- [X] T055 [US1] 创建POST /api/auth/logout接口,登出(可选实现)
- [X] T056 [US1] 在SecurityConfig中配置/api/auth/**路径白名单,允许匿名访问
- [X] T057 [US1] 在JwtAuthenticationFilter中添加用户信息提取和权限验证逻辑,基于hospitalId实现数据隔离
- [X] T058 [US1] 在所有Repository查询中添加hospitalId过滤条件,确保数据权限隔离
- [X] T059 [US1] 添加日志记录登录操作、token刷新操作

**Checkpoint**: 此时用户故事1应完全功能且可独立测试 - 用户可以通过微信登录获取token并访问受保护资源

---

## Phase 4: User Story 2 - 宠物档案管理 (Priority: P1)

**Goal**: 实现宠物档案CRUD,支持搜索、分页、必填项验证

**Independent Test**: 创建宠物、查询列表、搜索宠物、更新信息、删除宠物,所有操作正常且数据隔离正确

### DTO定义

- [X] T060 [P] [US2] 创建PetCreateRequest.java DTO在src/main/java/com/petlis/dto/request/,包含所有必填字段验证注解
- [X] T061 [P] [US2] 创建PetUpdateRequest.java DTO在src/main/java/com/petlis/dto/request/
- [X] T062 [P] [US2] 创建PetQueryRequest.java DTO在src/main/java/com/petlis/dto/request/,包含搜索和分页参数
- [X] T063 [P] [US2] 创建PetResponse.java DTO在src/main/java/com/petlis/dto/response/
- [X] T064 [P] [US2] 创建PetVO.java视图对象在src/main/java/com/petlis/vo/
- [X] T065 [P] [US2] 创建PetMapper.java MapStruct映射器在src/main/java/com/petlis/mapper/

### 实体扩展

- [X] T066 [US2] 修改User实体,添加与宠物档案的关联关系(如果需要)

### Repository

- [X] T067 [US2] 创建PetRepository接口在src/main/java/com/petlis/repository/,添加:
  - findByHospitalIdAndAnimalNameContaining方法(按医院和名称模糊查询)
  - findByHospitalId方法(按医院查询所有宠物)
  - countByHospitalId方法(统计宠物数量)

### Service层

- [X] T068 [US2] 创建PetService.java接口在src/main/java/com/petlis/service/,定义create、update、delete、query、search方法
- [X] T069 [US2] 创建PetServiceImpl.java实现类在src/main/java/com/petlis/service/impl/,实现:
  - create: 创建宠物档案,验证必填项,关联当前用户医院
  - update: 更新宠物信息,验证权限
  - delete: 逻辑删除宠物,设置isActive=0
  - query: 分页查询,按医院隔离
  - search: 按中英文名模糊搜索
  - 验证逻辑:必填字段检查、数据长度检查

### Controller层

- [X] T070 [US2] 创建PetController.java控制器在src/main/java/com/petlis/controller/api/,实现:
  - POST /api/pets - 创建宠物档案
  - GET /api/pets - 查询宠物列表(分页)
  - GET /api/pets/search?name=xxx - 搜索宠物
  - GET /api/pets/{id} - 获取宠物详情
  - PUT /api/pets/{id} - 更新宠物档案
  - DELETE /api/pets/{id} - 删除宠物(逻辑删除)
- [X] T071 [US2] 在PetController中添加@Valid注解进行参数校验
- [X] T072 [US2] 在PetController中添加异常处理,返回统一错误响应

### 日志和验证

- [X] T073 [US2] 添加宠物操作日志记录(创建、更新、删除)
- [X] T074 [US2] 使用Spring Validation验证必填字段:中文名、英文名、物种、性别、生日、畜主中英文名
- [X] T075 [US2] 测试数据权限:用户A不能查看/修改用户B医院的宠物

**Checkpoint**: 此时用户故事1和2都应独立可用 - 宠物档案管理完整实现

---

## Phase 5: User Story 3 - 检测申请与项目管理 (Priority: P1)

**Goal**: 实现检测申请创建、状态流转、明细管理、价格计算

**Independent Test**: 创建检测申请、添加检测项目、查询订单列表、查看详情、更新状态,状态流转正确,价格计算准确

### DTO定义

- [ ] T076 [P] [US3] 创建OrderCreateRequest.java DTO在src/main/java/com/petlis/dto/request/,包含宠物信息、采样信息、检测项列表
- [ ] T077 [P] [US3] 创建OrderUpdateRequest.java DTO在src/main/java/com/petlis/dto/request/
- [ ] T078 [P] [US3] 创建OrderQueryRequest.java DTO在src/main/java/com/petlis/dto/request/,包含状态筛选、时间范围
- [ ] T079 [P] [US3] 创建OrderResponse.java DTO在src/main/java/com/petlis/dto/response/
- [ ] T080 [P] [US3] 创建OrderDetailResponse.java DTO在src/main/java/com/petlis/dto/response/,包含主表和明细表信息
- [ ] T081 [P] [US3] 创建OrderVO.java视图对象在src/main/java/com/petlis/vo/
- [ ] T082 [P] [US3] 创建OrderMapper.java MapStruct映射器在src/main/java/com/petlis/mapper/

### Service层

- [ ] T083 [US3] 创建OrderService.java接口在src/main/java/com/petlis/service/,定义:
  - createOrder: 创建检测申请
  - addOrderItem: 添加检测项目
  - removeOrderItem: 删除检测项目
  - updateOrder: 更新申请信息
  - updateOrderStatus: 更新申请状态
  - queryOrders: 分页查询申请列表
  - getOrderDetail: 获取申请详情
  - calculateTotalFee: 计算总费用

- [ ] T084 [US3] 创建OrderServiceImpl.java实现类在src/main/java/com/petlis/service/impl/,实现:
  - createOrder:创建ListDiagnosisProject记录,自动生成UserNO(55550-前缀),初始状态-2(草稿)
  - addOrderItem:创建DiagnosisDetail记录,从InfoTestTypePrice获取价格,计算OrginalPrice和TestPrice,生成GenterNO
  - 验证狂犬检测(InfoTestTypeId=1225)时检查ChipNO和Birthday是否已填写
  - 验证DetailRemark必填(狂犬检测时)
  - updateOrder:更新申请信息,验证权限(只能操作本院订单)
  - updateOrderStatus:实现状态流转逻辑:-2(草稿)→0(待审核)→2(等待报告)→3(已完成),以及1(审核不通过)和4(已取消)
  - queryOrders:按医院、状态、时间范围分页查询
  - getOrderDetail:关联查询主表和明细表,返回完整信息
  - calculateTotalFee:汇总所有DiagnosisDetail的TestPrice,更新ListDiagnosisProject.TestFee

### Controller层

- [ ] T085 [US3] 创建OrderController.java控制器在src/main/java/com/petlis/controller/api/,实现:
  - POST /api/orders - 创建检测申请
  - GET /api/orders - 查询申请列表(支持status、startDate、endDate参数筛选)
  - GET /api/orders/{id} - 获取申请详情
  - PUT /api/orders/{id} - 更新申请信息
  - PUT /api/orders/{id}/status - 更新申请状态
  - POST /api/orders/{id}/items - 添加检测项目
  - DELETE /api/orders/{id}/items/{itemId} - 删除检测项目

- [ ] T086 [US3] 在OrderController中添加@Valid注解进行参数校验
- [ ] T087 [US3] 在OrderController中添加事务管理注解@Transactional,确保数据一致性
- [ ] T088 [US3] 实现异常处理:订单不存在、状态不允许更新、权限不足等

### Repository扩展

- [ ] T089 [US3] 在ListDiagnosisProjectRepository中添加findByHospitalIdAndSampleStatus方法
- [ ] T090 [US3] 在ListDiagnosisProjectRepository中添加findByHospitalIdAndCreateDtBetween方法(时间范围查询)

### 业务逻辑

- [ ] T091 [US3] 实现UserNO自动生成逻辑:查询最大ID+1,格式"55550-{id}"
- [ ] T092 [US3] 实现GenterNO生成逻辑:格式"{orderId}-{序号}"
- [ ] T093 [US3] 实现价格查询逻辑:从InfoTestTypePrice表获取,支持城市价格,默认使用cityId=0的价格
- [ ] T094 [US3] 实现狂犬检测特殊验证:检测项ID=1225时,验证ChipNO、Birthday、DetailRemark必填
- [ ] T095 [US3] 实现状态机验证:状态流转必须符合规则,不允许跳过中间状态

### 日志和审计

- [ ] T096 [US3] 添加订单创建、状态变更日志
- [ ] T097 [US3] 记录操作人、操作时间、IP地址到lin_admin_log表

**Checkpoint**: 此时用户故事1、2、3都应独立可用 - 核心业务流程完整实现

---

## Phase 6: User Story 4 - 检测报告查询与展示 (Priority: P2)

**Goal**: 实现报告查询、筛选、PDF下载

**Independent Test**: 查询报告列表、按宠物筛选、按状态筛选、下载PDF报告

### DTO定义

- [X] T098 [P] [US4] 创建ReportQueryRequest.java DTO在src/main/java/com/petlis/dto/request/
- [X] T099 [P] [US4] 创建ReportResponse.java DTO在src/main/java/com/petlis/dto/response/
- [X] T100 [P] [US4] 创建ReportVO.java视图对象在src/main/java/com/petlis/vo/
- [X] T101 [P] [US4] 创建ReportMapper.java MapStruct映射器在src/main/java/com/petlis/mapper/

### Repository扩展

- [X] T102 [US4] 在DiagnosisDetailRepository中添加findByListDiagnosisProjectIdAndIsCheckPass方法,过滤isCheckPass=0的有效记录
- [X] T103 [US4] 在DiagnosisDetailRepository中添加findByListDiagnosisProjectIdInAndDiagnosisStatus方法,支持批量查询

### Service层

- [X] T104 [US4] 创建ReportService.java接口在src/main/java/com/petlis/service/,定义:
  - queryReports: 查询报告列表
  - getReportDetail: 获取报告详情
  - downloadReportPdf: 下载PDF报告

- [X] T105 [US4] 创建ReportServiceImpl.java实现类在src/main/java/com/petlis/service/impl/,实现:
  - queryReports:关联查询ListDiagnosisProject和DiagnosisDetail,按医院、宠物、状态筛选
  - getReportDetail:查询DiagnosisDetailInfo获取reportResult和mailReport
  - downloadReportPdf:根据PDFHttpUrl返回文件流,或返回URL供前端下载
  - 状态判断:diagnosisStatus=0时显示"检测中",!=0时显示"已完成"

### Controller层

- [X] T106 [US4] 创建ReportController.java控制器在src/main/java/com/petlis/controller/api/,实现:
  - GET /api/reports - 查询报告列表(支持petId、status参数筛选)
  - GET /api/reports/{id} - 获取报告详情
  - GET /api/reports/{id}/download - 下载PDF报告

### 文件服务

- [X] T107 [US4] 创建FileService.java文件服务接口在src/main/java/com/petlis/service/,处理文件上传下载
- [X] T108 [US4] 创建FileServiceImpl.java实现类,支持本地文件系统或对象存储
- [X] T109 [US4] 配置静态资源映射,允许访问PDF报告文件

**Checkpoint**: 此时用户故事1-4都应独立可用 - 报告查询功能完整实现

---

## Phase 7: User Story 5 - 数据字典与配置管理 (Priority: P2)

**Goal**: 实现字典数据接口,支持前端动态渲染表单

**Independent Test**: 调用所有字典接口,返回正确的启用数据,品种可以按物种筛选

### DTO定义

- [X] T110 [P] [US5] 创建DictionaryResponse.java通用字典响应DTO在src/main/java/com/petlis/dto/response/
- [X] T111 [P] [US5] 创建AnimalResponse.java、VarietyResponse.java、SexResponse.java等具体响应DTO

### Service层

- [X] T112 [US5] 创建DictionaryService.java接口在src/main/java/com/petlis/service/,定义:
  - getAnimals: 获取动物物种列表
  - getVarieties: 获取品种列表(支持按物种筛选)
  - getSexes: 获取性别列表
  - getSampleTypes: 获取样本类型列表
  - getTestTypes: 获取检测项目列表
  - getTestTypePrice: 获取检测项目价格
  - getCities: 获取城市列表
  - getHospitals: 获取医院列表

- [X] T113 [US5] 创建DictionaryServiceImpl.java实现类在src/main/java/com/petlis/service/impl/,实现:
  - 所有方法只返回IsActive=1的启用数据
  - getVarieties支持infoAnimalId参数筛选对应品种
  - getTestTypePrice支持cityId参数获取对应城市价格
  - 使用JPA Specification或@Query注解实现动态查询

### Controller层

- [X] T114 [US5] 创建DictionaryController.java控制器在src/main/java/com/petlis/controller/api/,实现:
  - GET /api/dictionary/animals - 获取动物物种列表
  - GET /api/dictionary/varieties?animalId={id} - 获取品种列表
  - GET /api/dictionary/sexes - 获取性别列表
  - GET /api/dictionary/sample-types - 获取样本类型列表
  - GET /api/dictionary/test-types - 获取检测项目列表
  - GET /api/dictionary/test-types/{id}/price?cityId={cityId} - 获取检测项目价格
  - GET /api/dictionary/cities - 获取城市列表
  - GET /api/dictionary/hospitals - 获取医院列表(可选,管理后台使用)

### 缓存优化

- [X] T115 [US5] 在字典接口添加@Cacheable注解,使用Spring Cache缓存字典数据
- [X] T116 [US5] 配置Caffeine缓存,设置合理的过期时间(如1小时)

**Checkpoint**: 此时用户故事1-5都应独立可用 - 字典管理功能完整实现

---

## Phase 8: User Story 6 - 用户信息管理 (Priority: P3)

**Goal**: 实现用户信息查询和更新

**Independent Test**: 查看当前用户信息、更新昵称和头像

### DTO定义

- [X] T117 [P] [US6] 创建UserUpdateRequest.java DTO在src/main/java/com/petlis/dto/request/
- [X] T118 [P] [US6] 创建UserInfoResponse.java DTO在src/main/java/com/petlis/dto/response/

### Service层

- [X] T119 [US6] 在UserService接口中添加getUserProfile、updateUserProfile方法
- [X] T120 [US6] 在UserServiceImpl中实现:
  - getUserProfile:从JWT token获取当前用户信息,查询User和关联的InfoHospital
  - updateUserProfile:更新昵称、头像等字段

### Controller层

- [X] T121 [US6] 创建UserController.java控制器在src/main/java/com/petlis/controller/api/,实现:
  - GET /api/user/profile - 获取当前用户信息
  - PUT /api/user/profile - 更新用户信息
  - POST /api/user/avatar - 上传头像(可选实现)

### 文件上传

- [X] T122 [US6] 实现文件上传接口,支持图片上传到本地或OSS
- [X] T123 [US6] 配置文件上传大小限制和类型限制
- [X] T124 [US6] 实现头像URL存储和返回

**Checkpoint**: 所有6个用户故事都应独立可用 - 完整功能实现

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 跨故事的改进和优化

### 性能优化

- [X] T125 [P] 优化数据库查询,使用@EntityGraph解决N+1问题
- [X] T126 [P] 在常用查询字段上添加数据库索引(如果未创建)
- [X] T127 [P] 配置JPA二级缓存,提升字典数据查询性能
- [X] T128 实现分页查询优化,避免count查询(如果数据量大)

### 安全加固

- [X] T129 [P] 实现SQL注入防护验证(确保使用JPA参数化查询)
- [X] T130 [P] 实现XSS防护,对输入字符串进行转义
- [X] T131 [P] 配置HTTPS重定向(生产环境)
- [X] T132 实现请求频率限制,防止API滥用

### 日志和监控

- [X] T133 [P] 添加操作审计日志到所有关键接口
- [X] T134 [P] 配置日志级别,生产环境禁用DEBUG日志
- [X] T135 添加性能监控日志,记录慢查询(>300ms)

### 文档和部署

- [X] T136 [P] 生成Swagger API文档,确保所有接口有完整注释
- [X] T137 [P] 创建README.md,包含项目简介、快速启动、API文档链接
- [X] T138 [P] 创建Dockerfile,支持容器化部署
- [X] T139 创建docker-compose.yml,包含MySQL和应用服务
- [X] T140 创建数据库初始化SQL脚本(可选)

### 测试验证

- [ ] T141 使用Swagger UI测试所有接口
- [ ] T142 手动测试完整业务流程:登录→创建宠物→创建订单→查看报告
- [ ] T143 验证数据权限隔离:不同医院用户数据互不可见
- [ ] T144 验证异常处理:所有错误返回统一格式
- [ ] T145 性能测试:使用JMeter或ab工具测试并发请求

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 - 可立即开始
- **Foundational (Phase 2)**: 依赖Setup完成 - 阻塞所有用户故事
- **User Stories (Phase 3-8)**: 全部依赖Foundational阶段完成
  - 用户故事可以并行开发(如果有多人)
  - 或按优先级顺序开发(P1 → P2 → P3)
- **Polish (Phase 9)**: 依赖所有需要的用户故事完成

### User Story Dependencies

- **User Story 1 (P1) - 认证**: Foundational完成后可开始 - 无其他故事依赖
- **User Story 2 (P2) - 宠物管理**: Foundational完成后可开始 - 依赖US1的认证机制
- **User Story 3 (P1) - 检测申请**: Foundational完成后可开始 - 依赖US1认证、US2宠物
- **User Story 4 (P2) - 报告查询**: Foundational完成后可开始 - 依赖US3订单
- **User Story 5 (P2) - 字典管理**: Foundational完成后可开始 - 无其他故事依赖
- **User Story 6 (P3) - 用户信息**: Foundational完成后可开始 - 依赖US1认证

### Within Each User Story

- 实体定义 → Repository → Service → Controller
- Service层依赖Repository和实体
- Controller层依赖Service和DTO
- 测试在每个故事完成后进行

### Parallel Opportunities

- Setup阶段所有标记[P]的任务可并行
- Foundational阶段所有标记[P]的任务可并行(阶段内)
- Foundational完成后,所有用户故事可并行(如果团队允许)
- 每个用户故事内标记[P]的DTO定义任务可并行
- 不同用户故事可由不同团队成员并行开发

---

## Parallel Example: User Story 2 (宠物管理)

```bash
# 并行创建所有DTO:
Task T060: "创建PetCreateRequest.java DTO"
Task T061: "创建PetUpdateRequest.java DTO"
Task T062: "创建PetQueryRequest.java DTO"
Task T063: "创建PetResponse.java DTO"
Task T064: "创建PetVO.java视图对象"
Task T065: "创建PetMapper.java映射器"

# 然后顺序执行:
Task T067: "创建PetRepository接口"
  ↓
Task T068: "创建PetService接口"
  ↓
Task T069: "创建PetServiceImpl实现类"
  ↓
Task T070: "创建PetController控制器"
  ↓
Task T071-T075: "验证、日志、权限"
```

---

## Implementation Strategy

### MVP First (仅用户故事1)

1. 完成Phase 1: Setup
2. 完成Phase 2: Foundational (关键 - 阻塞所有故事)
3. 完成Phase 3: User Story 1 (微信登录)
4. **停止并验证**: 独立测试用户故事1
5. 如果准备就绪则部署/演示

### Incremental Delivery (增量交付)

1. 完成Setup + Foundational → 基础就绪
2. 添加User Story 1 → 独立测试 → 部署/演示 (MVP!)
3. 添加User Story 2 → 独立测试 → 部署/演示
4. 添加User Story 3 → 独立测试 → 部署/演示
5. 添加User Story 4 → 独立测试 → 部署/演示
6. 添加User Story 5 → 独立测试 → 部署/演示
7. 添加User Story 6 → 独立测试 → 部署/演示
8. 每个故事增加价值且不破坏已有故事

### Parallel Team Strategy (多人并行)

如果有多个开发人员:

1. 团队共同完成Setup + Foundational
2. Foundational完成后:
   - 开发者A: User Story 1 (认证)
   - 开发者B: User Story 2 (宠物管理)
   - 开发者C: User Story 5 (字典管理)
3. 故事独立完成并集成

---

## Notes

- [P]任务 = 不同文件,无依赖,可并行
- [Story]标签将任务映射到具体用户故事,便于追溯
- 每个用户故事应可独立完成和测试
- 每完成一个任务或逻辑组后提交代码
- 在任何检查点停止以独立验证故事
- 避免:模糊任务、相同文件冲突、破坏独立性的跨故事依赖

---

## Task Summary

- **Total Tasks**: 145
- **Setup (Phase 1)**: 16 tasks
- **Foundational (Phase 2)**: 30 tasks
- **User Story 1 (Phase 3)**: 9 tasks
- **User Story 2 (Phase 4)**: 16 tasks
- **User Story 3 (Phase 5)**: 20 tasks
- **User Story 4 (Phase 6)**: 12 tasks
- **User Story 5 (Phase 7)**: 7 tasks
- **User Story 6 (Phase 8)**: 8 tasks
- **Polish (Phase 9)**: 21 tasks

**Parallel Opportunities**: 约60%的任务标记为[P],可并行执行

**Independent Test Criteria**:
- US1: 微信登录获取token,通过token访问接口,数据隔离正确
- US2: 创建、查询、搜索、更新、删除宠物,全部成功
- US3: 创建订单、添加项目、查询列表、更新状态,状态流转正确
- US4: 查询报告列表、筛选、下载PDF,全部正常
- US5: 调用所有字典接口,返回正确启用数据
- US6: 查看、更新用户信息,成功保存

**Suggested MVP Scope**: Phase 1 + Phase 2 + Phase 3 (User Story 1 - 认证)
