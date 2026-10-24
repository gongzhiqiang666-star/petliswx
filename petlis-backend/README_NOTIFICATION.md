# 📢 消息通知系统 - 文档索引

## 📚 文档列表

### 1. 配置指南
**文件**: [NOTIFICATION_CONFIG_GUIDE.md](./NOTIFICATION_CONFIG_GUIDE.md)
**用途**: 完整的配置说明文档，包含所有配置项、部署步骤和故障排查

**包含内容**：
- 数据库表结构说明
- Flyway migration脚本说明
- 后端配置文件详解
- 微信订阅消息配置步骤
- 开发/生产环境部署指南
- Docker部署示例
- 配置验证方法
- 监控和日志说明
- 常见问题解答
- 配置检查清单

### 2. 测试指南
**文件**: [NOTIFICATION_TEST_GUIDE.md](./NOTIFICATION_TEST_GUIDE.md)
**用途**: 完整的功能测试文档，包含测试步骤和验证方法

**包含内容**：
- 数据库和触发器测试
- 后端API接口测试
- 定时任务测试
- 前端功能测试
- 端到端集成测试
- 性能测试方法
- 故障排查指南
- 测试检查清单
- 快速验证脚本

### 3. 环境变量配置
**文件**: [.env.example](./.env.example)
**用途**: 环境变量配置模板

**使用方法**：
```bash
# 1. 复制模板文件
cp .env.example .env

# 2. 编辑配置文件，填写真实的配置值
vim .env

# 3. 加载环境变量
source .env

# 4. 启动应用
mvn spring-boot:run
```

**配置项说明**：
- 数据库连接配置
- 微信小程序配置（AppID、AppSecret）
- 微信通知配置（模板ID、开关）
- 应用服务器配置
- 日志和文件存储配置

### 4. 配置检查脚本
**文件**: [check-config.sh](./check-config.sh)
**用途**: 自动化配置检查工具

**使用方法**：
```bash
# 在项目根目录执行
./check-config.sh
```

**检查内容**：
- ✅ 环境变量文件是否配置
- ✅ 必需的环境变量是否设置
- ✅ 应用配置文件是否完整
- ✅ Flyway配置是否正确
- ✅ Migration脚本是否齐全
- ✅ Maven依赖是否完整
- ✅ Java代码文件是否存在
- ✅ 数据库连接是否正常
- ✅ 数据库表是否创建成功
- ✅ 触发器是否正常工作

---

## 🚀 快速开始

### 开发环境部署

**步骤1：配置环境变量**
```bash
cd /Users/gong/claudelife/PETLIS/petlis-backend
cp .env.example .env
vim .env  # 填写配置
```

**步骤2：验证配置**
```bash
./check-config.sh
```

**步骤3：启动应用**
```bash
mvn clean install
mvn spring-boot:run
```

**步骤4：测试功能**
参考 [NOTIFICATION_TEST_GUIDE.md](./NOTIFICATION_TEST_GUIDE.md)

### 生产环境部署

**步骤1：准备生产配置**
```bash
# 编辑生产环境配置
vim src/main/resources/application-prod.yml

# 设置环境变量
export WECHAT_APPID="your_appid"
export WECHAT_SECRET="your_secret"
export WECHAT_NOTIFICATION_TEMPLATE_ID="your_template_id"
```

**步骤2：备份数据库**
```bash
mysqldump -u user -p laboklin > backup_$(date +%Y%m%d).sql
```

**步骤3：部署应用**
```bash
mvn clean package -DskipTests
java -jar target/petlis-backend-1.0.0.jar --spring.profiles.active=prod
```

**步骤4：验证部署**
```bash
# 健康检查
curl http://localhost:8082/api/actuator/health

# 检查日志
tail -f logs/petlis-backend-prod.log | grep -i "notification"
```

---

## 📋 核心配置项速查

### 微信通知配置

**开发环境** (`application-dev.yml`):
```yaml
wechat:
  notification:
    enabled: false  # 开发环境关闭
    template-id: ""
```

**生产环境** (`application-prod.yml`):
```yaml
wechat:
  notification:
    enabled: true   # 生产环境启用
    template-id: "YOUR_TEMPLATE_ID"
```

### Flyway配置

**application.yml** (所有环境):
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    table: flyway_schema_history
```

### 数据库配置

**开发环境**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/laboklin
    username: root
    password: your_password
```

**生产环境** (使用环境变量):
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

---

## 📖 Migration脚本说明

| 版本 | 文件名 | 描述 | 执行顺序 |
|------|--------|------|----------|
| V1 | V1__create_notification_table.sql | 创建通知记录表 | 1 |
| V2 | V2__create_order_status_trigger.sql | 创建触发器 | 2 |
| V3 | V3__add_notification_details.sql | 添加宠物和主人字段 | 3 |
| V4 | V4__create_notification_log_table.sql | 创建通知日志表 | 4 |
| V5 | V5__add_order_subscribe_field.sql | 添加订阅字段 | 5 |
| V6 | V6__update_order_status_trigger.sql | 更新触发器生成详细消息 | 6 |

**重要提示**：
- Migration脚本按版本号顺序执行
- V6会覆盖V2的触发器，这是正常的
- 如需回滚，手动执行SQL回滚语句

---

## 🔧 故障排查快速链接

| 问题类型 | 文档 | 章节 |
|---------|------|------|
| Flyway迁移失败 | [配置指南](./NOTIFICATION_CONFIG_GUIDE.md) | 七、常见问题 |
| 微信推送失败 | [配置指南](./NOTIFICATION_CONFIG_GUIDE.md) | 七、常见问题 |
| 触发器不工作 | [测试指南](./NOTIFICATION_TEST_GUIDE.md) | 七、常见问题 |
| 配置验证 | [测试指南](./NOTIFICATION_TEST_GUIDE.md) | 一、数据库测试 |
| 性能优化 | [配置指南](./NOTIFICATION_CONFIG_GUIDE.md) | 七、常见问题 |

---

## ✅ 部署前检查清单

使用 [check-config.sh](./check-config.sh) 脚本自动检查，或手动验证以下项目：

### 配置文件
- [ ] `.env` 文件已创建并填写完整
- [ ] `application.yml` 包含Flyway配置
- [ ] `application-prod.yml` 配置正确
- [ ] `.env.example` 已更新为最新模板

### 依赖和脚本
- [ ] `pom.xml` 包含Flyway依赖
- [ ] 所有Migration脚本存在（V1-V6）
- [ ] Java代码文件完整
- [ ] check-config.sh 可执行

### 数据库
- [ ] 数据库备份已完成
- [ ] Flyway历史表存在
- [ ] 通知表已创建
- [ ] 触发器正常工作

### 微信配置
- [ ] 微信AppID和AppSecret已配置
- [ ] 订阅消息模板已创建
- [ ] template-id已配置
- [ ] 前端订阅消息代码已实现

---

## 📞 技术支持

**文档维护**: PETLIS Team
**最后更新**: 2026-01-03
**版本**: 2.0.0

**问题反馈**:
- GitHub Issues: https://github.com/your-org/petlis-backend/issues
- 邮件: support@petlis.com

---

## 📝 文档更新记录

| 日期 | 版本 | 更新内容 |
|------|------|----------|
| 2026-01-03 | 2.0.0 | 添加详细通知消息、通知日志、Flyway配置 |
| 2025-12-28 | 1.0.0 | 初始版本，基础通知功能 |

---

**🎉 开始使用消息通知系统吧！**

如有任何问题，请参考对应文档或联系技术支持。
