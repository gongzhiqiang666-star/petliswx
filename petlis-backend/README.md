# PETLIS Backend API

宠物实验室信息系统后端服务,为微信小程序提供完整的API接口。

## 技术栈

- **语言**: Java 17
- **框架**: Spring Boot 3.4.1
- **数据访问**: Spring Data JPA
- **数据库**: MySQL 8.0+
- **安全**: Spring Security + JWT
- **文档**: Springdoc OpenAPI 3
- **工具**: Lombok, MapStruct, Hutool, Apache Commons Text
- **缓存**: JPA二级缓存(Ehcache) + Caffeine

## 核心特性

- ✅ 微信小程序登录认证
- ✅ JWT Token认证机制
- ✅ 医院数据权限隔离
- ✅ 统一异常处理和响应格式
- ✅ 操作审计日志
- ✅ XSS攻击防护
- ✅ 请求频率限制(60次/分钟)
- ✅ SQL注入防护(参数化查询)
- ✅ 性能监控(慢查询告警>300ms)
- ✅ 数据库索引优化
- ✅ JPA二级缓存(字典数据)e

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 配置数据库

1. 创建数据库:
```sql
CREATE DATABASE laboklin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 导入数据库表结构和初始化数据:
```bash
# 导入表结构
mysql -u root -p laboklin < src/main/resources/db/schema.sql

# 导入示例数据(可选,仅开发环境)
mysql -u root -p laboklin < src/main/resources/db/demo-data.sql

# 执行性能优化索引
mysql -u root -p laboklin < src/main/resources/db/migration/V1.0.1__add_indexes.sql
```

3. 配置数据库连接(编辑 `src/main/resources/application-dev.yml`):
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/laboklin?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

数据库脚本详细说明请参考: `src/main/resources/db/README.md`

### 运行应用

```bash
# 编译项目
mvn clean package

# 运行应用
java -jar target/petlis-backend-1.0.0.jar

# 或使用Maven直接运行
mvn spring-boot:run
```

应用启动后访问:
- API地址: http://localhost:8080/api
- Swagger文档: http://localhost:8080/api/swagger-ui.html

## 项目结构

```
petlis-backend/
├── src/main/java/com/petlis/
│   ├── PetlisApplication.java          # 主启动类
│   ├── common/                         # 通用组件
│   │   ├── exception/                   # 异常定义
│   │   ├── response/                    # 统一响应封装
│   │   ├── enums/                       # 枚举定义
│   │   ├── filter/                      # 过滤器(JWT认证)
│   │   └── util/                        # 工具类
│   ├── config/                         # 配置类
│   ├── entity/                         # JPA实体类
│   │   ├── base/                        # 基础实体
│   │   └── dict/                        # 字典实体
│   ├── repository/                     # 数据访问层
│   ├── service/                        # 服务层
│   │   └── impl/                        # 服务实现
│   ├── controller/                     # 控制器层
│   │   └── api/                        # API接口
│   ├── dto/                            # 数据传输对象
│   │   ├── request/                    # 请求DTO
│   │   └── response/                   # 响应DTO
│   ├── vo/                             # 视图对象
│   └── mapper/                         # MapStruct映射器
└── src/main/resources/
    ├── application.yml                 # 主配置
    ├── application-dev.yml             # 开发环境
    ├── application-prod.yml            # 生产环境
    └── logback-spring.xml              # 日志配置
```

## API接口

### 认证相关
- `POST /api/auth/wechat/login` - 微信小程序登录
- `POST /api/auth/refresh` - 刷新Token
- `POST /api/auth/logout` - 登出

### 宠物管理
- `POST /api/pets` - 创建宠物档案
- `GET /api/pets` - 查询宠物列表
- `GET /api/pets/search?name=xxx` - 搜索宠物
- `GET /api/pets/{id}` - 获取宠物详情
- `PUT /api/pets/{id}` - 更新宠物档案
- `DELETE /api/pets/{id}` - 删除宠物(逻辑删除)

### 检测申请
- `POST /api/orders` - 创建检测申请
- `GET /api/orders` - 查询申请列表
- `GET /api/orders/{id}` - 获取申请详情
- `PUT /api/orders/{id}` - 更新申请信息
- `PUT /api/orders/{id}/status` - 更新申请状态
- `POST /api/orders/{id}/items` - 添加检测项目
- `DELETE /api/orders/{id}/items/{itemId}` - 删除检测项目

### 报告查询
- `GET /api/reports` - 查询报告列表
- `GET /api/reports/{id}` - 获取报告详情
- `GET /api/reports/{id}/download` - 下载PDF报告

### 数据字典
- `GET /api/dictionary/animals` - 获取动物物种列表
- `GET /api/dictionary/varieties?animalId={id}` - 获取品种列表
- `GET /api/dictionary/sexes` - 获取性别列表
- `GET /api/dictionary/sample-types` - 获取样本类型列表
- `GET /api/dictionary/test-types` - 获取检测项目列表
- `GET /api/dictionary/cities` - 获取城市列表

### 用户管理
- `GET /api/user/profile` - 获取当前用户信息
- `PUT /api/user/profile` - 更新用户信息
- `POST /api/user/avatar` - 上传头像

## 统一响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

### 失败响应
```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": null
}
```

## 认证方式

除 `/api/auth/**` 外的所有接口需要在请求头中携带JWT Token:

```
Authorization: Bearer {jwt_token}
```

## Docker部署

### 使用Docker Compose(推荐)

```bash
# 启动所有服务(MySQL + Backend)
docker-compose up -d

# 查看日志
docker-compose logs -f backend

# 停止服务
docker-compose down
```

### 单独使用Docker

```bash
# 构建镜像
docker build -t petlis-backend:1.0.0 .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -e DB_USERNAME=petlis \
  -e DB_PASSWORD=your_password \
  -e JWT_SECRET=your-secret-key \
  -e WECHAT_APPID=your-appid \
  -e WECHAT_SECRET=your-secret \
  petlis-backend:1.0.0
```

### 环境变量说明

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| DB_USERNAME | 数据库用户名 | petlis | 是 |
| DB_PASSWORD | 数据库密码 | - | 是 |
| JWT_SECRET | JWT密钥 | - | 是 |
| JWT_EXPIRATION | Token过期时间(毫秒) | 604800000 | 否 |
| WECHAT_APPID | 微信小程序AppID | - | 是 |
| WECHAT_SECRET | 微信小程序Secret | - | 是 |
| SPRING_PROFILES_ACTIVE | 运行环境 | dev | 否 |

## 部署说明

### 生产环境配置

1. 修改 `application-prod.yml` 中的配置:
   - 数据库连接信息
   - 文件上传路径
   - JWT密钥(从环境变量读取)

2. 设置环境变量:
```bash
export DB_USERNAME=petlis
export DB_PASSWORD=your_secure_password
export JWT_SECRET=your_jwt_secret_at_least_32_chars
export WECHAT_APPID=your_wechat_appid
export WECHAT_SECRET=your_wechat_secret
```

3. 使用生产配置启动:
```bash
java -jar petlis-backend-1.0.0.jar --spring.profiles.active=prod
```

### 安全建议

- ✅ 修改默认JWT密钥
- ✅ 使用强密码
- ✅ 配置HTTPS(生产环境)
- ✅ 定期更新依赖版本
- ✅ 配置防火墙规则
- ✅ 启用请求频率限制
- ✅ 定期备份数据库

### 监控建议

- 监控应用日志(`logs/petlis-backend.log`)
- 监控慢查询(>300ms会记录WARN日志)
- 监控请求频率(429错误)
- 定期检查磁盘空间
- 监控JVM内存使用情况

## 开发指南

### 代码规范
- 遵循阿里巴巴Java开发手册
- 所有public方法必须添加中文Javadoc注释
- 使用Lombok减少样板代码
- 使用MapStruct进行DTO/VO映射
- 统一异常处理
- UTF-8编码

### 数据库规范
- 所有表包含IsActive字段(0=禁用,1=启用)
- 使用逻辑删除,不使用物理DELETE
- 金额字段使用BigDecimal
- 日期时间统一使用Unix时间戳(秒)
- 使用参数化查询防止SQL注入

### 性能优化
- 字典数据使用JPA二级缓存
- 常用查询字段已添加索引
- 使用@OperationLog记录关键操作
- PerformanceLogAspect监控慢查询

### 提交规范
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 重构
- test: 测试相关
- chore: 构建/工具链相关

## 常见问题

### Q: 如何配置微信小程序信息?

A: 编辑 `application-dev.yml`,配置以下项:
```yaml
app:
  wechat:
    appid: your-wechat-appid
    secret: REPLACE_WITH_LOCAL_SECRET
```

生产环境建议使用环境变量:
```bash
export WECHAT_APPID=your_appid
export WECHAT_SECRET=your_secret
```

### Q: 如何查看API文档?

A: 启动应用后访问 http://localhost:8080/api/swagger-ui.html

**注意**: 生产环境默认关闭Swagger文档,可在 `application-prod.yml` 中开启。

### Q: 如何调试SQL?

A: 在 `application-dev.yml` 中设置:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Q: 如何解决请求频率限制问题?

A: 默认限制为60次/分钟,如需调整请修改 `RateLimitFilter.java`:
```java
private static final int RATE_LIMIT = 60; // 修改此值
```

### Q: 如何查看性能问题?

A: 查看日志中的性能警告:
```bash
grep "性能警告" logs/petlis-backend-dev.log
```

执行时间超过300ms的方法会记录WARN日志。

### Q: 数据库连接失败怎么办?

A: 检查以下项:
1. MySQL服务是否启动
2. 数据库名称、用户名、密码是否正确
3. MySQL用户权限是否足够
4. 防火墙是否允许连接

## 文档索引

- [API使用示例](API_USAGE_EXAMPLES.md)
- [API测试指南](API测试指南.md)
- [项目结构说明](PROJECT_STRUCTURE.md)
- [验收清单](ACCEPTANCE_CHECKLIST.md)
- [实现总结](IMPLEMENTATION_SUMMARY.md)
- [SQL注入防护验证报告](docs/SQL注入防护验证报告.md)
- [数据库脚本说明](src/main/resources/db/README.md)
- [用户模块文档](USER_MODULE_README.md)
- [检测报告模块文档](检测报告查询展示实现总结.md)

## 技术支持

如有问题请联系: PETLIS开发团队

## 许可证

Copyright © 2024 PETLIS Team. All rights reserved.

