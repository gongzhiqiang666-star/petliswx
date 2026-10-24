# 🔧 服务重启和功能验证指南

## 问题描述

消息通知内容没有显示宠物名称、主人名称等详细信息。

## 根本原因

1. ✅ **数据库层已修复**：触发器正常生成详细消息
2. ✅ **后端DTO已修复**：NotificationResponse已添加新字段
3. ❌ **后端服务未重启**：需要重启加载新代码
4. ❌ **前端未重新编译**：需要刷新小程序

---

## 📋 完整解决步骤

### 步骤1：重启后端服务

**方法A：使用脚本（推荐）**
```bash
cd /Users/gong/claudelife/PETLIS/petlis-backend
./restart.sh
```

**方法B：手动重启**
```bash
# 1. 停止现有服务
pkill -f "petlis-backend"

# 2. 等待进程完全停止
sleep 2

# 3. 重新编译
mvn clean install -DskipTests

# 4. 启动服务
mvn spring-boot:run
```

**验证服务启动**
```bash
# 检查健康状态
curl http://localhost:8082/api/actuator/health

# 预期返回：{"status":"UP"}
```

### 步骤2：刷新前端小程序

1. **完全关闭**微信开发者工具（不是最小化）
2. **重新打开**开发者工具
3. **点击"编译"**（或按 Ctrl+B / Cmd+B）
4. **清除缓存**（推荐）：
   - 工具栏 → 清除缓存 → 清除全部缓存
5. **再次编译**

### 步骤3：创建新的测试数据

```sql
-- 进入MySQL
mysql -u root -p laboklin

-- 更新订单状态，触发新通知
UPDATE lin_lb_listdiagnosisproject
SET SampleStatus = 2
WHERE ListDiagnosisProjectId = 23775
LIMIT 1;

-- 验证通知已创建（应该有animalName、masterName、notificationMessage）
SELECT
    id,
    animal_name,
    master_name,
    notification_message
FROM lin_lb_notification
WHERE order_id = 23775
ORDER BY operation_time DESC
LIMIT 1;
```

### 步骤4：验证前端显示

**在首页查看**：
1. 进入小程序首页
2. 查看下方"消息通知"部分
3. 应该看到最近5条通知
4. 点击"更多"跳转到完整通知列表

**在通知列表查看**：
1. 进入"我的"页面
2. 点击"消息通知"
3. 应该看到详细的通知内容

**预期显示效果**：
```
订单状态更新
您的宠物「劈柴」（主人：张惠妹）的检测订单状态已更新为：已审核
宠物：劈柴 | 主人：张惠妹
09:08
```

---

## 🔍 故障排查

### 问题1：后端服务启动失败

**检查方法**：
```bash
# 查看启动日志
tail -f /Users/gong/claudelife/PETLIS/petlis-backend/logs/petlis-backend-dev.log
```

**常见错误**：
- **端口被占用**：`kill -9 $(lsof -ti:8082)`
- **数据库连接失败**：检查MySQL是否运行
- **依赖下载失败**：`mvn clean install` 重新下载

### 问题2：API返回数据没有新字段

**验证API**：
```bash
# 获取通知列表（需要token）
curl "http://localhost:8082/api/notifications/hospitals/my?pageNum=0&pageSize=5" \
  -H "Authorization: Bearer YOUR_TOKEN" | jq '.data.content[0]'
```

**预期JSON**：
```json
{
  "id": 5,
  "orderId": 23777,
  "animalName": "劈柴",
  "masterName": "张惠妹",
  "notificationMessage": "您的宠物「劈柴」（主人：张惠妹）的检测订单状态已更新为：已审核",
  ...
}
```

**如果没有这些字段**：
- 检查NotificationResponse.java是否包含这些字段
- 重新编译：`mvn clean compile`
- 重启服务

### 问题3：前端仍然显示旧数据

**解决方法**：
1. 创建新的通知记录（更新订单状态）
2. 旧通知记录的这些字段是null，这是正常的
3. 新的通知会包含完整信息

### 问题4：小程序编译报错

**解决方法**：
```bash
# 在微信开发者工具中
1. 工具 → 构建npm
2. 清除缓存 → 清除全部缓存
3. 重新编译
```

---

## ✅ 验证清单

完成以下步骤确认功能正常：

### 后端验证
- [ ] 服务正常启动（health检查通过）
- [ ] Flyway迁移成功（6个版本）
- [ ] NotificationResponse包含新字段
- [ ] API返回包含animalName、masterName、notificationMessage

### 数据库验证
- [ ] lin_lb_notification表包含animalName、masterName、notificationMessage字段
- [ ] 触发器正常工作（更新订单状态时创建通知）
- [ ] 新通知包含完整信息

### 前端验证
- [ ] 首页显示最近5条通知
- [ ] 通知列表页面显示详细信息
- [ ] 点击通知跳转到订单详情
- [ ] 未读通知显示红点

---

## 🎯 快速验证命令

**一键测试脚本**：
```bash
#!/bin/bash
echo "=== 验证通知功能 ==="

# 1. 检查后端服务
echo "1. 检查服务状态..."
curl -s http://localhost:8082/api/actuator/health | jq '.status'

# 2. 创建测试通知
echo "2. 创建测试通知..."
mysql -u root -pREPLACE_WITH_LOCAL_SECRET laboklin << EOF
UPDATE lin_lb_listdiagnosisproject
SET SampleStatus = 3
WHERE ListDiagnosisProjectId = 23777
LIMIT 1;
EOF

# 3. 查询新通知
echo "3. 查询通知内容..."
mysql -u root -pREPLACE_WITH_LOCAL_SECRET laboklin -e "
SELECT
    CONCAT('宠物: ', animal_name, ', 主人: ', master_name) as info,
    notification_message
FROM lin_lb_notification
WHERE order_id = 23777
ORDER BY operation_time DESC
LIMIT 1;
"

echo "=== 验证完成 ==="
```

---

## 📞 技术支持

如果以上步骤都完成还是看不到通知详情：

1. **查看后端日志**：
   ```bash
   tail -100 logs/petlis-backend-dev.log | grep -i "notification"
   ```

2. **查看前端控制台**：
   - 打开微信开发者工具
   - Console标签查看错误

3. **检查数据库**：
   ```sql
   -- 查看最新的通知记录
   SELECT * FROM lin_lb_notification ORDER BY operation_time DESC LIMIT 5;
   ```

4. **联系开发团队**：
   - 提供错误日志
   - 提供数据库查询结果
   - 说明执行了哪些步骤

---

**更新时间**：2026-01-03
**版本**：1.0.0
