# 微信消息通知模块 - 测试指南

## 📋 概述

本文档提供完整的微信消息通知模块测试方法，包括数据库、后端API、定时任务和前端功能的测试。

**Feature**: 004-bugfixes-and-enhancements
**Date**: 2026-01-03
**Version**: 1.0.0

---

## 🗄️ 一、数据库表和触发器测试

### 1.1 检查数据库表是否创建成功

**测试步骤**：
```sql
-- 1. 检查通知表是否存在
SHOW TABLES LIKE '%notification%';

-- 2. 查看表结构
DESCRIBE lin_lb_notification;

-- 3. 查看索引
SHOW INDEX FROM lin_lb_notification;
```

**预期结果**：
- ✅ 表 `lin_lb_notification` 存在
- ✅ 包含所有字段：id, order_id, hospital_id, old_status, new_status, operator_id, operation_time, wechat_sent_status, wechat_message_id, error_message, createtime, updatetime
- ✅ 索引已创建

### 1.2 测试触发器是否工作

**测试步骤**：
```sql
-- 1. 创建一个测试订单（如果不存在）
INSERT INTO lin_lb_listdiagnosisproject (
  UserNO, InfoHospitalId, SampleStatus, CreateDt
) VALUES (
  'TEST-001', 3, -2, NOW()
);

-- 2. 记录生成的订单ID
SELECT @order_id := LAST_INSERT_ID();

-- 3. 更新订单状态，应该触发触发器
UPDATE lin_lb_listdiagnosisproject
SET SampleStatus = 0
WHERE ListDiagnosisProjectId = @order_id;

-- 4. 检查是否创建了通知记录
SELECT * FROM lin_lb_notification
WHERE order_id = @order_id
ORDER BY operation_time DESC
LIMIT 1;
```

**预期结果**：
- ✅ 触发器自动在 `lin_lb_notification` 表中插入记录
- ✅ old_status = -2 (草稿)
- ✅ new_status = 0 (待审核)
- ✅ operation_time = 当前时间

---

## 🔧 二、后端API测试

### 2.1 启动后端服务

```bash
cd /Users/gong/claudelife/PETLIS/petlis-backend
mvn spring-boot:run
```

**验证服务启动**：
```bash
# 检查服务是否正常启动
curl http://localhost:8082/actuator/health

# 预期返回：{"status":"UP"} 或类似健康状态
```

### 2.2 API接口测试

#### 测试1: 获取医院通知列表

```bash
# 需要先登录获取token
TOKEN="your_token_here"

curl -X GET "http://localhost:8082/api/notifications/hospitals/my?pageNum=0&pageSize=10" \
  -H "Authorization: Bearer $TOKEN"
```

**预期结果**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [...],
    "totalElements": 0
  }
}
```

#### 测试2: 获取未读通知数量

```bash
curl -X GET "http://localhost:8082/api/notifications/hospitals/my/unread-count" \
  -H "Authorization: Bearer $TOKEN"
```

**预期结果**：
```json
{
  "code": 200,
  "message": "success",
  "data": 0
}
```

#### 测试3: 标记通知为已读

```bash
# 先通过测试1获取通知ID，然后标记为已读
NOTIFICATION_ID=1

curl -X PUT "http://localhost:8082/api/notifications/$NOTIFICATION_ID/read" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**预期结果**：
```json
{
  "code": 200,
  "message": "标记成功"
}
```

#### 测试4: 处理待发送通知（手动触发定时任务）

```bash
curl -X POST "http://localhost:8082/api/notifications/process-pending" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**预期结果**：
```json
{
  "code": 200,
  "message": "处理完成",
  "data": 0
}
```

---

## ⏰ 三、定时任务测试

### 3.1 验证定时任务是否启用

**检查日志**：
```bash
# 查看后端日志
tail -f /path/to/spring.log | grep "定时任务"
```

**预期日志输出**：
```
[定时任务] 开始扫描待发送通知
[定时任务] 找到 0 条待发送通知
```

### 3.2 手动触发定时任务测试

**测试步骤**：
1. 在数据库中手动插入一条待发送通知：
```sql
INSERT INTO lin_lb_notification (
  order_id, hospital_id, new_status, operation_time, wechat_sent_status
) VALUES (
  123, 3, 2, NOW(), 0
);
```

2. 调用处理接口：
```bash
curl -X POST "http://localhost:8082/api/notifications/process-pending" \
  -H "Authorization: Bearer $TOKEN"
```

3. 检查通知状态是否更新：
```sql
SELECT * FROM lin_lb_notification WHERE order_id = 123;
```

**预期结果**：
- ✅ `wechat_sent_status` 从 0 变为 1 或 2
- ✅ 如果配置了微信通知，状态为 1
- ✅ 如果未配置，状态为 2，`error_message` = "微信通知功能未启用或模板ID未配置"

---

## 📱 四、前端功能测试

### 4.1 页面访问测试

**测试步骤**：
1. 打开微信开发者工具
2. 登录小程序
3. 访问路径：`pages/notifications/notifications`

**预期结果**：
- ✅ 页面正常加载
- ✅ 显示"暂无通知"空状态
- ✅ 下拉刷新功能正常
- ✅ 滚动到底部加载更多功能正常

### 4.2 未读通知显示测试

**测试步骤**：
1. 在"我的"页面查看消息通知入口
2. 检查是否显示未读数量徽标

**预期结果**：
- ✅ 如果有未读通知，显示红色徽标
- ✅ 徽标数字正确（超过99显示99+）
- ✅ 点击跳转到通知列表页面

### 4.3 通知点击跳转测试

**测试步骤**：
1. 确保有通知记录
2. 点击通知项
3. 验证跳转和已读标记

**预期结果**：
- ✅ 跳转到订单详情页
- ✅ 通知标记为已读（红点消失）
- ✅ 未读数量减少

---

## 🧪 五、完整功能测试流程

### 5.1 端到端测试

**步骤1: 创建订单**
```bash
# 通过小程序创建一个新订单
# 路径：首页 -> 预约检测 -> 选择宠物 -> 填写信息 -> 提交
```

**步骤2: 更改订单状态**
```sql
-- 将订单从草稿改为待审核
UPDATE lin_lb_listdiagnosisproject
SET SampleStatus = 0
WHERE ListDiagnosisProjectId = <刚创建的订单ID>;
```

**步骤3: 检查通知记录**
```sql
-- 检查是否自动创建了通知记录
SELECT * FROM lin_lb_notification
WHERE order_id = <订单ID>
ORDER BY operation_time DESC;
```

**步骤4: 等待定时任务处理**
```bash
# 等待1分钟（定时任务每分钟执行一次）
# 或手动触发处理接口
```

**步骤5: 查看前端通知**
- 刷新小程序"我的"页面
- 查看未读通知数量
- 点击进入通知列表

**预期结果**：
- ✅ 触发器自动创建通知记录
- ✅ 定时任务处理通知
- ✅ 前端显示通知列表
- ✅ 未读数量正确
- ✅ 点击通知跳转到订单详情

---

## 📊 六、性能测试

### 6.1 并发测试

**使用JMeter或类似工具**：

```bash
# 测试获取未读通知数量接口
# 并发用户数: 100
# 循环次数: 10
# 目标: 验证接口在高并发下的稳定性
```

### 6.2 数据量测试

**大数据量测试**：
```sql
-- 插入1000条测试通知
DELIMITER $$
CREATE PROCEDURE insert_test_notifications()
BEGIN
  DECLARE i INT DEFAULT 1;
  WHILE i <= 1000 DO
    INSERT INTO lin_lb_notification (
      order_id, hospital_id, new_status, operation_time, wechat_sent_status
    ) VALUES (
      i, 3, 2, DATE_SUB(NOW(), INTERVAL i MINUTE), 0
    );
    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;

CALL insert_test_notifications();
```

**前端分页加载测试**：
- 验证分页加载是否流畅
- 检查是否有内存泄漏
- 确认加载速度可接受

---

## 🐛 七、常见问题排查

### 7.1 触发器未生效

**检查方法**：
```sql
-- 查看触发器是否存在
SHOW TRIGGERS LIKE '%order%';

-- 查看触发器定义
SHOW CREATE TRIGGER trg_order_status_change;
```

**解决方案**：
- 删除旧触发器：`DROP TRIGGER IF EXISTS trg_order_status_change;`
- 重新创建触发器：执行 `V2__create_order_status_trigger.sql`

### 7.2 定时任务未执行

**检查方法**：
```bash
# 查看Spring Boot启动日志
grep -i "scheduled" /path/to/spring.log

# 查看定时任务相关日志
grep "定时任务" /path/to/spring.log
```

**解决方案**：
- 确保主启动类添加了 `@EnableScheduling` 注解
- 检查 `NotificationScheduler.java` 是否被Spring扫描到

### 7.3 微信通知发送失败

**检查配置**：
```yaml
# application-dev.yml
wechat:
  notification:
    enabled: true  # 确保启用
    template-id: "你的模板ID"
```

**检查方法**：
```bash
# 查看错误日志
tail -f /path/to/spring.log | grep "微信通知"
```

**常见错误**：
- `appid` 或 `appsecret` 配置错误
- 模板ID未配置或不存在
- 用户openid不存在
- 网络问题

### 7.4 前端页面空白

**检查方法**：
1. 打开微信开发者工具控制台
2. 查看是否有JavaScript错误
3. 检查API请求是否成功

**解决方案**：
- 确认后端服务已启动
- 检查token是否有效
- 验证API路径是否正确

---

## ✅ 八、测试检查清单

### 数据库层
- [ ] 表 `lin_lb_notification` 已创建
- [ ] 触发器 `trg_order_status_change` 已创建
- [ ] 触发器在订单状态变化时自动插入记录
- [ ] 索引正确创建

### 后端层
- [ ] NotificationController 所有API可访问
- [ ] NotificationService 正确处理通知
- [ ] WechatNotificationService 正确发送微信通知（如果配置）
- [ ] NotificationScheduler 定时任务正常运行
- [ ] 错误处理和日志记录正常

### 前端层
- [ ] 通知页面正常显示
- [ ] API调用成功
- [ ] 未读数量正确显示
- [ ] 点击通知正确跳转
- [ ] 下拉刷新和上拉加载更多正常
- [ ] 徽标样式正确

### 集成测试
- [ ] 订单状态变化自动创建通知
- [ ] 定时任务自动发送通知
- [ ] 前端实时显示未读数量
- [ ] 点击通知跳转到正确页面
- [ ] 通知标记为已读功能正常

---

## 📝 九、测试报告模板

**测试日期**：2026-01-03
**测试人员**：__________
**测试环境**：开发环境

### 测试结果汇总

| 模块 | 测试用例数 | 通过数 | 失败数 | 通过率 |
|------|-----------|--------|--------|--------|
| 数据库和触发器 | 5 | | | % |
| 后端API | 6 | | | % |
| 定时任务 | 2 | | | % |
| 前端功能 | 6 | | | % |
| 集成测试 | 3 | | | % |
| **总计** | **22** | | | **%** |

### 发现的问题

1. 问题描述：
   - 复现步骤：
   - 严重程度：[高/中/低]
   - 修复方案：

2. 问题描述：
   - 复现步骤：
   - 严重程度：[高/中/低]
   - 修复方案：

### 测试结论

- [ ] 测试通过，可以发布
- [ ] 存在问题，需要修复后重新测试

---

## 🚀 十、快速验证脚本

创建一个Shell脚本快速验证所有功能：

```bash
#!/bin/bash
# quick-test.sh - 快速验证通知模块

echo "=== 微信消息通知模块快速验证 ==="

echo ""
echo "1. 检查数据库表..."
mysql -u root -p"密码" -e "USE laboklin; SHOW TABLES LIKE '%notification%';"

echo ""
echo "2. 检查触发器..."
mysql -u root -p"密码" -e "USE laboklin; SHOW TRIGGERS LIKE '%order%';"

echo ""
echo "3. 测试创建通知记录..."
mysql -u root -p"密码" laboklin << 'EOF'
INSERT INTO lin_lb_listdiagnosisproject (UserNO, InfoHospitalId, SampleStatus, CreateDt)
VALUES ('AUTO-TEST', 3, -2, NOW());
EOF

echo ""
echo "4. 检查后端服务..."
curl -s http://localhost:8082/actuator/health | jq '.'

echo ""
echo "5. 测试API接口（需要token）..."
# curl -X GET "http://localhost:8082/api/notifications/hospitals/my/unread-count" \
#   -H "Authorization: Bearer $TOKEN"

echo ""
echo "=== 验证完成 ==="
```

**使用方法**：
```bash
chmod +x quick-test.sh
./quick-test.sh
```

---

**文档版本**: 1.0.0
**最后更新**: 2026-01-03
**维护者**: PETLIS Team
