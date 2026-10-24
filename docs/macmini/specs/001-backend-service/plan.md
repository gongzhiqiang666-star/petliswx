# Implementation Plan: PETLIS后端API服务

**Branch**: `001-backend-service` | **Date**: 2025-12-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-backend-service/spec.md`

## Summary

为PETLIS宠物实验室信息系统开发完整的后端API服务,支持微信小程序访问。系统需要实现用户认证、宠物档案管理、检测申请与项目管理、检测报告查询、数据字典管理和用户信息管理6大核心功能。技术栈采用Java 17 + Spring Boot 3.x + Spring Data JPA + MySQL 8.0,遵循RESTful架构风格和分层设计原则。系统必须支持基于医院的数据权限控制、状态流转管理、PDF报告生成等核心业务能力。

## Technical Context

**Language/Version**: Java 17 (LTS长期支持版本)

**Primary Dependencies**:
- Spring Boot 3.4+ (核心框架)
- Spring Data JPA (数据访问)
- Spring Security + JWT (认证授权)
- Spring Validation (参数校验)
- MySQL Connector Java 8.0+ (数据库驱动)
- Lombok 1.18+ (减少样板代码)
- MapStruct 1.6+ (DTO映射)
- Springdoc OpenAPI 3 (API文档)
- Hutool 5.x (工具类库)
- itext7 (PDF生成)

**Storage**: MySQL 8.0+ (已存在的数据库laboklin,字符集utf8mb4)

**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers

**Target Platform**: Linux服务器(支持容器化部署Docker)

**Project Type**: web应用 (后端API服务)

**Performance Goals**:
- API响应时间: P95 < 500ms, P99 < 1s
- 并发支持: 100+ 并发请求
- 数据库查询: 单表查询 < 100ms, 关联查询 < 300ms

**Constraints**:
- 必须兼容现有MySQL数据库结构(47个表)
- 必须支持微信小程序登录(code换取openid)
- 必须实现基于医院的数据隔离
- 所有接口返回统一JSON格式
- 使用UTF-8编码支持中英德文

**Scale/Scope**:
- 预期用户: 100-1000个医院用户
- 数据量: 宠物档案10K+, 检测申请50K+, 检测报告50K+
- API接口: 约40-50个REST端点
- 实体类: 约20个JPA实体

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

✅ **I. RESTful API优先设计**: 所有接口遵循RESTful风格,使用标准HTTP方法,统一返回格式

✅ **II. 分层架构与职责分离**: 采用Controller → Service → Repository三层架构

✅ **III. 数据验证与安全性**: 使用Spring Validation + @Valid注解,敏感数据加密存储

✅ **IV. 数据库规范与一致性**: 使用JPA映射现有表,逻辑删除(IsActive字段),UTF-8编码

✅ **V. 异常处理与日志记录**: @ControllerAdvice统一异常处理,SLF4J记录关键操作

✅ **VI. 微信小程序适配性**: 实现微信登录接口,JWT token管理,支持微信支付回调

✅ **VII. 代码质量与文档规范**: Javadoc注释,阿里巴巴规范,Lombok减少样板代码

**结论**: 宪章检查通过,无违规项,无需复杂度跟踪。

## Project Structure

### Documentation (this feature)

```text
specs/001-backend-service/
├── plan.md              # 本文件 - 实施计划
├── spec.md              # 功能规格说明(已创建)
├── data-model.md        # 数据模型设计(JPA实体定义)
├── quickstart.md        # 快速启动指南
├── contracts/           # API契约定义
│   ├── auth-api.md           # 认证相关接口
│   ├── pet-api.md            # 宠物管理接口
│   ├── order-api.md          # 检测申请接口
│   ├── report-api.md         # 报告查询接口
│   ├── dictionary-api.md     # 数据字典接口
│   └── user-api.md           # 用户管理接口
└── tasks.md             # 任务列表(待生成)
```

### Source Code (repository root)

```text
petlis-backend/
├── src/
│   ├── main/
│   │   ├── java/com/petlis/
│   │   │   ├── PetlisApplication.java              # 启动类
│   │   │   ├── controller/                         # 控制器层
│   │   │   │   ├── api/                            # 业务API接口
│   │   │   │   │   ├── AuthController.java         # 认证接口
│   │   │   │   │   ├── PetController.java          # 宠物管理接口
│   │   │   │   │   ├── OrderController.java        # 检测申请接口
│   │   │   │   │   ├── ReportController.java       # 报告查询接口
│   │   │   │   │   ├── DictionaryController.java   # 数据字典接口
│   │   │   │   │   └── UserController.java         # 用户管理接口
│   │   │   │   └── admin/                          # 管理后台接口(后续扩展)
│   │   │   ├── service/                            # 服务层
│   │   │   │   ├── AuthService.java                # 认证服务接口
│   │   │   │   ├── PetService.java                 # 宠物服务接口
│   │   │   │   ├── OrderService.java               # 订单服务接口
│   │   │   │   ├── ReportService.java              # 报告服务接口
│   │   │   │   ├── DictionaryService.java          # 字典服务接口
│   │   │   │   ├── UserService.java                # 用户服务接口
│   │   │   │   └── impl/                           # 服务实现
│   │   │   │       ├── AuthServiceImpl.java
│   │   │   │       ├── PetServiceImpl.java
│   │   │   │       ├── OrderServiceImpl.java
│   │   │   │       ├── ReportServiceImpl.java
│   │   │   │       ├── DictionaryServiceImpl.java
│   │   │   │       └── UserServiceImpl.java
│   │   │   ├── repository/                         # 数据访问层
│   │   │   │   ├── InfoAnimalRepository.java       # 动物物种
│   │   │   │   ├── InfoVarietyRepository.java      # 品种
│   │   │   │   ├── DictSexRepository.java          # 性别字典
│   │   │   │   ├── InfoSampleTypeRepository.java   # 样本类型
│   │   │   │   ├── InfoTestTypeRepository.java     # 检测项目
│   │   │   │   ├── InfoTestTypePriceRepository.java # 检测项目价格
│   │   │   │   ├── InfoHospitalRepository.java     # 医院
│   │   │   │   ├── ListDiagnosisProjectRepository.java # 检测申请主表
│   │   │   │   ├── DiagnosisDetailRepository.java  # 检测明细
│   │   │   │   ├── DiagnosisDetailInfoRepository.java # 检测报告内容
│   │   │   │   ├── UserRepository.java             # 用户
│   │   │   │   └── CityRepository.java             # 城市
│   │   │   ├── entity/                             # JPA实体类
│   │   │   │   ├── InfoAnimal.java                 # 动物物种实体
│   │   │   │   ├── InfoVariety.java                # 品种实体
│   │   │   │   ├── DictSex.java                    # 性别字典实体
│   │   │   │   ├── InfoSampleType.java             # 样本类型实体
│   │   │   │   ├── InfoTestType.java               # 检测项目实体
│   │   │   │   ├── InfoTestTypePrice.java          # 检测项目价格实体
│   │   │   │   ├── InfoHospital.java               # 医院实体
│   │   │   │   ├── ListDiagnosisProject.java       # 检测申请主表实体
│   │   │   │   ├── DiagnosisDetail.java            # 检测明细实体
│   │   │   │   ├── DiagnosisDetailInfo.java        # 检测报告内容实体
│   │   │   │   ├── User.java                       # 用户实体
│   │   │   │   ├── City.java                       # 城市实体
│   │   │   │   └── base/                           # 基础实体
│   │   │   │       ├── BaseEntity.java             # 基础实体(ID, 创建时间)
│   │   │   │       └── ActivableEntity.java         # 可激活实体(IsActive字段)
│   │   │   ├── dto/                                # 数据传输对象
│   │   │   │   ├── request/                        # 请求DTO
│   │   │   │   │   ├── WechatLoginRequest.java     # 微信登录请求
│   │   │   │   │   ├── PetCreateRequest.java       # 创建宠物请求
│   │   │   │   │   ├── PetUpdateRequest.java       # 更新宠物请求
│   │   │   │   │   ├── PetQueryRequest.java        # 查询宠物请求
│   │   │   │   │   ├── OrderCreateRequest.java     # 创建检测申请请求
│   │   │   │   │   ├── OrderUpdateRequest.java     # 更新检测申请请求
│   │   │   │   │   ├── OrderQueryRequest.java      # 查询检测申请请求
│   │   │   │   │   ├── ReportQueryRequest.java     # 查询报告请求
│   │   │   │   │   └── UserUpdateRequest.java      # 更新用户请求
│   │   │   │   └── response/                       # 响应DTO
│   │   │   │       ├── WechatLoginResponse.java    # 微信登录响应
│   │   │   │       ├── PetResponse.java            # 宠物响应
│   │   │   │       ├── OrderResponse.java          # 检测申请响应
│   │   │   │       ├── OrderDetailResponse.java    # 检测申请详情响应
│   │   │   │       ├── ReportResponse.java         # 报告响应
│   │   │   │       ├── DictionaryResponse.java     # 字典响应
│   │   │   │       └── UserInfoResponse.java       # 用户信息响应
│   │   │   ├── vo/                                 # 视图对象
│   │   │   │   ├── PetVO.java                      # 宠物视图对象
│   │   │   │   ├── OrderVO.java                    # 订单视图对象
│   │   │   │   └── ReportVO.java                   # 报告视图对象
│   │   │   ├── common/                             # 通用组件
│   │   │   │   ├── exception/                      # 异常定义
│   │   │   │   │   ├── BusinessException.java      # 业务异常
│   │   │   │   │   ├── ResourceNotFoundException.java # 资源未找到异常
│   │   │   │   │   └── AuthenticationException.java # 认证异常
│   │   │   │   ├── response/                       # 统一响应封装
│   │   │   │   │   ├── Result.java                 # 统一响应对象
│   │   │   │   │   └── ResultCode.java             # 响应码枚举
│   │   │   │   ├── constant/                       # 常量定义
│   │   │   │   │   ├── ApiConstants.java           # API常量
│   │   │   │   │   ├── OrderStatusConstants.java   # 订单状态常量
│   │   │   │   │   └── WechatConstants.java        # 微信常量
│   │   │   │   ├── enums/                          # 枚举定义
│   │   │   │   │   ├── ResultCodeEnum.java         # 响应码枚举
│   │   │   │   │   ├── OrderStatusEnum.java        # 订单状态枚举
│   │   │   │   │   └── GenderEnum.java             # 性别枚举
│   │   │   │   ├── util/                           # 工具类
│   │   │   │   │   ├── JwtUtil.java                # JWT工具类
│   │   │   │   │   ├── WechatUtil.java             # 微信工具类
│   │   │   │   │   └── DateUtil.java               # 日期工具类
│   │   │   │   └── config/                         # 配置类
│   │   │   │       ├── AppConfig.java              # 应用配置
│   │   │   │       ├── DatabaseConfig.java         # 数据库配置
│   │   │   │       ├── SecurityConfig.java         # 安全配置(JWT)
│   │   │   │       ├── SwaggerConfig.java          # Swagger配置
│   │   │   │       └── WebConfig.java              # Web配置(CORS等)
│   │   │   └── mapper/                             # MapStruct映射器
│   │   │       ├── PetMapper.java                  # 宠物映射器
│   │   │       ├── OrderMapper.java                # 订单映射器
│   │   │       └── UserMapper.java                 # 用户映射器
│   │   └── resources/
│   │       ├── application.yml                     # 主配置文件
│   │       ├── application-dev.yml                 # 开发环境配置
│   │       ├── application-prod.yml                # 生产环境配置
│   │       ├── logback-spring.xml                  # 日志配置
│   │       └── db/
│   │           └── migration/                      # 数据库迁移脚本(可选)
│   └── test/
│       └── java/com/petlis/
│           ├── service/                            # 服务层测试
│           │   ├── AuthServiceTest.java
│           │   ├── PetServiceTest.java
│           │   ├── OrderServiceTest.java
│           │   └── ReportServiceTest.java
│           ├── controller/                         # 控制器测试
│           │   ├── PetControllerTest.java
│           │   └── OrderControllerTest.java
│           └── repository/                         # 数据访问测试
│               ├── PetRepositoryTest.java
│               └── OrderRepositoryTest.java
├── pom.xml                                         # Maven配置文件
└── Dockerfile                                      # Docker镜像构建文件

miniprogram/                                        # 微信小程序(已存在)
├── pages/
├── app.js
└── ...
```

**Structure Decision**: 选择标准的Spring Boot项目结构,采用分层架构(Controller-Service-Repository),符合Java企业级应用最佳实践。后端代码独立于小程序,通过REST API通信,支持前后端分离开发和部署。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

无需复杂度跟踪,宪章检查全部通过,无违规项。
