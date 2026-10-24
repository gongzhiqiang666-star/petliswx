# 消息通知系统 - 配置指南

## 📋 概述

本文档提供消息通知系统的完整配置说明，包括数据库配置、微信通知配置、Flyway数据库迁移配置等。

**Feature**: 004-bugfixes-and-enhancements
**Date**: 2026-01-03
**Version**: 2.0.0

---

## 🗄️ 一、数据库配置

### 1.1 数据库Migration脚本

通知系统使用Flyway进行数据库版本管理，所有migration脚本位于：

```
petlis-backend/src/main/resources/db/migration/
```

**Migration文件列表**：

| 文件名 | 版本 | 描述 | 依赖 |
|--------|------|------|------|
| V1__create_notification_table.sql | 1 | 创建通知记录表 | 无 |
| V2__create_order_status_trigger.sql | 2 | 创建订单状态变化触发器 | V1 |
| V3__add_notification_details.sql | 3 | 添加宠物和主人信息字段 | V1 |
| V4__create_notification_log_table.sql | 4 | 创建通知发送日志表 | 无 |
| V5__add_order_subscribe_field.sql | 5 | 添加订阅同意字段 | 无 |
| V6__update_order_status_trigger.sql | 6 | 更新触发器生成详细消息 | V1, V3 |

### 1.2 数据库表结构

#### 1.2.1 lin_lb_notification（通知记录表）

**用途**：存储订单状态变化的通知记录

**表结构**：
```sql
CREATE TABLE lin_lb_notification (
  id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL COMMENT '订单ID',
  hospital_id INT NOT NULL COMMENT '医院ID',
  old_status INT COMMENT '原状态',
  new_status INT NOT NULL COMMENT '新状态',
  animal_name VARCHAR(250) COMMENT '宠物名称',
  master_name VARCHAR(250) COMMENT '主人名称',
  notification_message TEXT COMMENT '通知消息内容',
  operator_id INT COMMENT '操作人ID',
  operation_time DATETIME NOT NULL COMMENT '操作时间',
  wechat_sent_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待发送 1-已发送 2-失败',
  wechat_message_id VARCHAR(100) COMMENT '微信消息ID',
  error_message TEXT COMMENT '错误信息',
  createtime INT UNSIGNED COMMENT '创建时间戳',
  updatetime INT UNSIGNED COMMENT '更新时间戳',
  INDEX idx_order_id (order_id),
  INDEX idx_hospital_id (hospital_id),
  INDEX idx_operation_time (operation_time),
  INDEX idx_hospital_status (hospital_id, wechat_sent_status)
);
```

**状态码说明**：
| 状态码 | 说明 |
|--------|------|
| -3 | 已取消 |
| -2 | 草稿 |
| 0 | 待审核 |
| 1 | 已审核 |
| 2 | 检测中 |
| 3 | 检测完成 |

#### 1.2.2 lin_lb_notification_log（通知发送日志表）

**用途**：记录每次微信推送的详细日志

**表结构**：
```sql
CREATE TABLE lin_lb_notification_log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  notification_id INT NOT NULL COMMENT '通知记录ID',
  order_id INT NOT NULL COMMENT '订单ID',
  hospital_id INT NOT NULL COMMENT '医院ID',
  user_id INT COMMENT '用户ID',
  openid VARCHAR(100) COMMENT '微信OpenID',
  template_id VARCHAR(100) COMMENT '消息模板ID',
  message_id VARCHAR(100) COMMENT '微信消息ID',
  send_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待发送 1-成功 2-失败',
  error_code VARCHAR(50) COMMENT '错误码',
  error_message TEXT COMMENT '错误信息',
  send_time DATETIME COMMENT '发送时间',
  retry_count INT DEFAULT 0 COMMENT '重试次数',
  createtime INT UNSIGNED,
  updatetime INT UNSIGNED,
  INDEX idx_notification_id (notification_id),
  INDEX idx_order_id (order_id),
  INDEX idx_openid (openid),
  INDEX idx_send_status (send_status)
);
```

#### 1.2.3 lin_lb_listdiagnosisproject（订单表）新增字段

**新增字段**：
```sql
ALTER TABLE lin_lb_listdiagnosisproject
ADD COLUMN notification_subscribe TINYINT DEFAULT 0 COMMENT '是否订阅消息通知 0-未订阅 1-已订阅'
AFTER SampleStatus;
```

### 1.3 触发器说明

**触发器名称**：`trg_order_status_change`

**触发时机**：当 `lin_lb_listdiagnosisproject` 表的 `SampleStatus` 字段更新时

**功能**：
- 检测状态变化
- 自动创建通知记录
- 填充宠物和主人信息
- 生成人性化通知消息

**消息格式**：
```
您的宠物「宠物名称」（主人：主人名称）的检测订单状态已更新为：状态名称
```

---

## ⚙️ 二、后端配置

### 2.1 application.yml 配置

在 `src/main/resources/application.yml` 中添加Flyway配置：

```yaml
spring:
  # Flyway配置
  flyway:
    enabled: true                              # 启用Flyway
    baseline-on-migrate: true                  # 对非空数据库创建基线
    baseline-version: 0                        # 基线版本
    locations: classpath:db/migration          # Migration脚本位置
    table: flyway_schema_history               # 历史记录表名
    encoding: UTF-8                            # 文件编码
    validate-on-migrate: true                  # 迁移前验证
```

### 2.2 application-dev.yml 配置（开发环境）

```yaml
# 微信通知配置
wechat:
  notification:
    enabled: false                              # 是否启用微信通知（开发环境默认关闭）
    template-id: ""                             # 微信订阅消息模板ID

  # 微信小程序基础配置
  appid: wx30c14324c89e9151                    # 小程序AppID
  secret: REPLACE_WITH_LOCAL_SECRET      # 小程序AppSecret
```

### 2.3 application-prod.yml 配置（生产环境）

```yaml
# 微信通知配置
wechat:
  notification:
    enabled: true                               # 生产环境启用
    template-id: "YOUR_TEMPLATE_ID"            # 配置真实的模板ID

  # 微信小程序基础配置
  appid: ${WECHAT_APPID}                       # 从环境变量读取
  secret: ${WECHAT_SECRET}                     # 从环境变量读取
```

**环境变量设置**：
```bash
export WECHAT_APPID="wx30c14324c89e9151"
export WECHAT_SECRET="your_production_secret"
```

### 2.4 pom.xml 依赖配置

确保已添加Flyway依赖：

```xml
<!-- Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

---

## 🔔 三、微信订阅消息配置

### 3.1 微信小程序后台配置

#### 步骤1：登录微信小程序后台

访问：https://mp.weixin.qq.com/

#### 步骤2：创建订阅消息模板

**路径**：功能 → 订阅消息 → 模板库 → 选择模板

**推荐模板字段**：

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| thing1 | thing | 订单编号 | 55550-23777 |
| thing2 | thing | 宠物名称 | 劈柴 |
| thing3 | thing | 订单状态 | 已审核 |
| date4 | date | 状态变化时间 | 2026-01-03 09:08 |

**模板示例**：
```
订单编号：{{thing1}}
宠物名称：{{thing2}}
订单状态：{{thing3}}
状态变化时间：{{date4}}
```

#### 步骤3：获取模板ID

创建成功后，会生成一个模板ID，格式类似：`ABC123XYZ456`

将此ID配置到 `application-prod.yml` 的 `template-id` 字段。

### 3.2 前端订阅消息申请

在用户提交订单时，引导用户订阅消息：

```javascript
// pages/order-preview/order-preview.js
async submitOrder() {
  // 1. 请求用户订阅消息
  try {
    await wx.requestSubscribeMessage({
      tmplIds: ['YOUR_TEMPLATE_ID'],
      success: (res) => {
        if (res['YOUR_TEMPLATE_ID'] === 'accept') {
          // 用户同意订阅，提交订单时标记
          this.submitOrderWithSubscribe(true);
        } else {
          // 用户拒绝订阅
          this.submitOrderWithSubscribe(false);
        }
      }
    });
  } catch (error) {
    // 用户拒绝或授权失败
    console.error('订阅消息申请失败', error);
    this.submitOrderWithSubscribe(false);
  }
}

submitOrderWithSubscribe(subscribed) {
  // 提交订单，携带订阅状态
  api.orders.create({
    ...this.data.orderInfo,
    notification_subscribe: subscribed ? 1 : 0
  });
}
```

### 3.3 订阅消息限制说明

**微信官方限制**：
1. **一次性订阅**：用户订阅一次，只能发送一条消息
2. **长期订阅**：需要向微信申请，仅特定行业可申请
3. **推送时机**：必须在用户触发行为后推送（如订单状态变化）

**推荐方案**：
- 在订单提交时申请订阅
- 每次状态变化发送一条通知
- 引导用户在每次提交订单时授权

---

## 🚀 四、部署步骤

### 4.1 开发环境部署

#### 步骤1：配置数据库

```bash
# 1. 确保MySQL服务运行
mysql.server status

# 2. 创建数据库（如果不存在）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS laboklin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

#### 步骤2：配置application-dev.yml

确保配置正确：
```yaml
wechat:
  notification:
    enabled: false  # 开发环境可以关闭微信推送
```

#### 步骤3：启动后端服务

```bash
cd /Users/gong/claudelife/PETLIS/petlis-backend

# 清理并编译
mvn clean install

# 启动服务
mvn spring-boot:run

# 或者使用jar包启动
java -jar target/petlis-backend-1.0.0.jar --spring.profiles.active=dev
```

#### 步骤4：验证Flyway迁移

```bash
# 检查日志，应该看到类似输出
# Flyway Community Edition 9.x.x by Redgate
# Database: jdbc:mysql://localhost:3306/laboklin (MySQL 8.0)
# Successfully validated 6 migrations
```

#### 步骤5：验证数据库表

```sql
-- 检查表是否创建成功
SHOW TABLES LIKE '%notification%';

-- 检查触发器
SHOW TRIGGERS LIKE '%order%';

-- 检查Flyway历史
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### 4.2 生产环境部署

#### 步骤1：环境变量配置

创建 `.env` 文件或设置系统环境变量：

```bash
# 微信配置
export WECHAT_APPID="wx30c14324c89e9151"
export WECHAT_SECRET="your_production_secret_here"

# 数据库配置
export DB_HOST="production-db-host"
export DB_PORT="3306"
export DB_NAME="laboklin"
export DB_USER="production_user"
export DB_PASSWORD="secure_password"
```

#### 步骤2：配置application-prod.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=true&serverTimezone=Asia/Shanghai
    username: ${DB_USER}
    password: ${DB_PASSWORD}

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    table: flyway_schema_history

wechat:
  appid: ${WECHAT_APPID}
  secret: ${WECHAT_SECRET}
  notification:
    enabled: true
    template-id: "YOUR_PRODUCTION_TEMPLATE_ID"
```

#### 步骤3：数据库备份

```bash
# 部署前务必备份数据库
mysqldump -u ${DB_USER} -p ${DB_NAME} > backup_$(date +%Y%m%d_%H%M%S).sql
```

#### 步骤4：构建和部署

```bash
# 1. 编译打包
mvn clean package -DskipTests

# 2. 上传到服务器
scp target/petlis-backend-1.0.0.jar user@server:/path/to/app/

# 3. 在服务器上启动
nohup java -jar petlis-backend-1.0.0.jar --spring.profiles.active=prod > app.log 2>&1 &
```

#### 步骤5：验证部署

```bash
# 检查服务健康状态
curl http://localhost:8082/api/actuator/health

# 检查Flyway迁移状态
curl http://localhost:8082/api/actuator/flyway

# 查看应用日志
tail -f logs/petlis-backend-prod.log | grep -i "notification\|flyway"
```

### 4.3 Docker部署（可选）

#### Dockerfile示例

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# 复制jar包
COPY target/petlis-backend-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8082

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

#### docker-compose.yml示例

```yaml
version: '3.8'

services:
  petlis-backend:
    build: .
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - WECHAT_APPID=${WECHAT_APPID}
      - WECHAT_SECRET=${WECHAT_SECRET}
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=laboklin
      - DB_USER=root
      - DB_PASSWORD=${DB_PASSWORD}
    depends_on:
      - mysql
    restart: always

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_PASSWORD}
      - MYSQL_DATABASE=laboklin
      - MYSQL_CHARACTER_SET_SERVER=utf8mb4
      - MYSQL_COLLATION_SERVER=utf8mb4_unicode_ci
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    restart: always

volumes:
  mysql-data:
```

---

## 🔍 五、配置验证

### 5.1 数据库验证

```sql
-- 1. 检查表是否创建
SHOW TABLES LIKE '%notification%';
-- 预期输出：lin_lb_notification, lin_lb_notification_log

-- 2. 检查触发器
SHOW TRIGGERS WHERE Trigger = 'trg_order_status_change';
-- 预期输出：一条记录

-- 3. 检查Flyway历史
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
-- 预期输出：6条记录，success都是1

-- 4. 检查新增字段
DESCRIBE lin_lb_listdiagnosisproject;
-- 预期：包含notification_subscribe字段
```

### 5.2 后端服务验证

```bash
# 1. 健康检查
curl http://localhost:8082/api/actuator/health
# 预期：{"status":"UP"}

# 2. 检查定时任务日志
tail -f logs/petlis-backend-dev.log | grep "定时任务"
# 预期：[定时任务] 开始扫描待发送通知

# 3. 检查通知API
curl http://localhost:8082/api/notifications/hospitals/my/unread-count \
  -H "Authorization: Bearer YOUR_TOKEN"
# 预期：{"code":200,"data":0}
```

### 5.3 功能测试

```sql
-- 测试触发器：更新订单状态
UPDATE lin_lb_listdiagnosisproject
SET SampleStatus = 1
WHERE ListDiagnosisProjectId = 23777;

-- 检查是否创建了通知记录
SELECT
    id,
    animal_name,
    master_name,
    notification_message
FROM lin_lb_notification
WHERE order_id = 23777
ORDER BY operation_time DESC
LIMIT 1;

-- 预期输出：包含完整的宠物信息、主人信息和通知消息
```

---

## 📊 六、监控和日志

### 6.1 关键日志位置

| 日志类型 | 位置 | 说明 |
|---------|------|------|
| 应用日志 | `logs/petlis-backend-{env}.log` | 主应用日志 |
| 错误日志 | `logs/error.log` | 错误日志 |
| Flyway日志 | 应用日志中 | 搜索关键词"Flyway" |
| 定时任务日志 | 应用日志中 | 搜索关键词"定时任务" |
| 微信推送日志 | 应用日志中 | 搜索关键词"微信通知" |

### 6.2 监控指标

**数据库监控**：
```sql
-- 查看通知表数据量
SELECT
    COUNT(*) as total_notifications,
    COUNT(CASE WHEN wechat_sent_status = 0 THEN 1 END) as pending,
    COUNT(CASE WHEN wechat_sent_status = 1 THEN 1 END) as sent,
    COUNT(CASE WHEN wechat_sent_status = 2 THEN 1 END) as failed
FROM lin_lb_notification;

-- 查看日志表数据量
SELECT
    COUNT(*) as total_logs,
    COUNT(CASE WHEN send_status = 1 THEN 1 END) as success,
    COUNT(CASE WHEN send_status = 2 THEN 1 END) as failed,
    AVG(CASE WHEN send_status = 1 THEN retry_count END) as avg_retry
FROM lin_lb_notification_log;
```

**应用监控**：
- 定时任务执行频率：每60秒
- 待发送通知积压数量
- 微信推送成功率
- 推送失败重试次数

### 6.3 告警规则建议

1. **推送失败率过高**：失败率 > 10% 时告警
2. **通知积压**：待发送通知 > 100 条时告警
3. **定时任务停止**：5分钟内无日志输出时告警
4. **数据库连接异常**：Flyway迁移失败时立即告警

---

## 🐛 七、常见问题

### 7.1 Flyway相关问题

**Q1: Flyway迁移失败，提示表已存在**

**解决方案**：
```sql
-- 检查Flyway历史
SELECT * FROM flyway_schema_history;

-- 如果表已创建但历史记录缺失，手动添加记录
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (1, '1', 'create notification table', 'SQL', 'V1__create_notification_table.sql', NULL, 'root', NOW(), 100, 1);
```

**Q2: 触发器创建失败**

**解决方案**：
```sql
-- 检查触发器权限
SHOW GRANTS FOR CURRENT_USER;

-- 确保有CREATE TRIGGER权限
GRANT CREATE ROUTE ON laboklin.* TO 'your_user'@'localhost';

-- 删除旧触发器重新创建
DROP TRIGGER IF EXISTS trg_order_status_change;
-- 然后执行V6脚本
```

### 7.2 微信推送相关问题

**Q1: 微信推送返回错误码43101**

**原因**：用户拒绝接收订阅消息

**解决方案**：
- 检查用户是否授权
- 引导用户在小程序设置中开启通知
- 下次订单提交时重新请求订阅

**Q2: 微信推送返回错误码40037**

**原因**：模板ID不存在或配置错误

**解决方案**：
- 确认 `template-id` 配置正确
- 检查微信小程序后台模板是否审核通过
- 确认模板属于正确的小程序

**Q3: access_token获取失败**

**解决方案**：
```bash
# 验证appid和secret配置
curl "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={APPID}&secret={SECRET}"

# 检查返回的errcode
# 0: 成功
# 40013: 不合法的AppID
# 40125: 不合法的secret
```

### 7.3 性能问题

**Q1: 通知表数据量过大，查询慢**

**解决方案**：
```sql
-- 定期清理旧数据（保留6个月）
DELETE FROM lin_lb_notification
WHERE operation_time < DATE_SUB(NOW(), INTERVAL 6 MONTH);

DELETE FROM lin_lb_notification_log
WHERE send_time < DATE_SUB(NOW(), INTERVAL 6 MONTH);

-- 添加索引优化查询
CREATE INDEX idx_operation_time ON lin_lb_notification(operation_time);
```

**Q2: 定时任务处理慢**

**解决方案**：
```java
// 在NotificationScheduler中调整批量大小
@Scheduled(fixedRate = 60000)
public void processPendingNotifications() {
    // 每次最多处理100条
    Pageable pageable = PageRequest.of(0, 100);
    // ...
}
```

---

## 📝 八、配置检查清单

### 部署前检查

- [ ] 数据库备份已完成
- [ ] 环境变量已配置
- [ ] Flyway配置正确（enabled: true）
- [ ] 微信AppID和AppSecret已配置
- [ ] 订阅消息模板已创建并审核通过
- [ ] template-id已配置
- [ ] pom.xml包含Flyway依赖
- [ ] application.yml包含Flyway配置
- [ ] 订单表包含notification_subscribe字段
- [ ] 前端订阅消息代码已实现

### 部署后验证

- [ ] Flyway迁移成功执行（6个版本）
- [ ] 数据库表创建成功
- [ ] 触发器正常工作
- [ ] 后端服务正常启动
- [ ] 定时任务日志正常输出
- [ ] 健康检查接口返回UP
- [ ] 通知API接口可访问
- [ ] 更新订单状态能触发通知
- [ ] 通知消息包含宠物和主人信息
- [ ] 前端通知列表正常显示

---

## 📚 九、相关文档

- [NOTIFICATION_TEST_GUIDE.md](./NOTIFICATION_TEST_GUIDE.md) - 测试指南
- [SYSTEM_DESIGN_SPEC.md](../SYSTEM_DESIGN_SPEC.md) - 系统设计文档
- [微信订阅消息开发文档](https://developers.weixin.qq.com/miniprogram/dev/api-backend/open-api/subscribe-message/subscribeMessage.send.html)
- [Flyway官方文档](https://flywaydb.org/documentation/)

---

## 📞 十、技术支持

**开发团队**：PETLIS Team
**版本**：2.0.0
**最后更新**：2026-01-03

**问题反馈**：
- GitHub Issues: https://github.com/your-org/petlis-backend/issues
- 邮件: support@petlis.com

---

**文档版本**: 1.0.0
**最后更新**: 2026-01-03
