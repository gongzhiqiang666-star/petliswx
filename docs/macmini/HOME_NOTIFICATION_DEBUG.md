# 首页消息通知调试指南

## 📋 问题描述

- ✅ "我的"菜单下的消息通知正常显示
- ❌ 首页的消息通知区域为空

## 🔍 调试步骤

### 步骤1：查看控制台日志

1. 打开微信开发者工具
2. 进入小程序首页
3. 打开"调试器" → "Console"标签
4. 查找以`[Home]`开头的日志

**预期看到的日志**：
```
[Home] 开始加载通知...
[Request] GET http://localhost:8082/api/notifications/hospitals/my?pageNum=0&pageSize=5
[Response] {...}
[Home] API返回结果: {...}
[Home] 提取的通知数据: [...]
[Home] 开始格式化通知，数量: 5
[Home] 格式化后的活动数据: [...]
[Home] 首页通知加载完成，数量: 5
```

### 步骤2：检查API返回数据

在Console中查看`[Home] API返回结果`的详细内容。

**预期看到的数据结构**：
```javascript
{
  content: [
    {
      id: 5,
      orderId: 23777,
      animalName: "劈柴",
      masterName: "张惠妹",
      notificationMessage: "您的宠物「劈柴」...",
      newStatus: 1,
      operationTime: "2026-01-03T01:08:01"
    }
  ],
  totalElements: 5,
  last: true
}
```

### 步骤3：常见问题排查

#### 问题A：API请求失败

**症状**：Console显示错误日志，如`[Home] 加载通知失败: ...`

**可能原因**：
1. 后端服务未启动
2. Token过期
3. 网络连接问题

**解决方案**：
```bash
# 1. 检查后端服务
curl http://localhost:8082/api/actuator/health

# 2. 如果服务未运行，启动它
cd /Users/gong/claudelife/PETLIS/petlis-backend
mvn spring-boot:run

# 3. 在小程序中重新登录获取新token
```

#### 问题B：API返回空数组

**症状**：日志显示`[Home] 提取的通知数据: []`

**可能原因**：
1. 数据库中没有通知记录
2. 通知记录属于其他医院
3. 用户未登录

**解决方案**：
```sql
-- 1. 检查数据库中是否有通知
SELECT COUNT(*) FROM lin_lb_notification;

-- 2. 查看当前用户医院ID的通知
SELECT
    COUNT(*) as count
FROM lin_lb_notification
WHERE hospital_id = 3;  -- 替换为你的医院ID

-- 3. 如果没有数据，创建测试通知
UPDATE lin_lb_listdiagnosisproject
SET SampleStatus = 2
WHERE ListDiagnosisProjectId = 23775
LIMIT 1;
```

#### 问题C：数据格式不匹配

**症状**：日志显示`[Home] 格式化通知，数量: 5`，但界面为空

**可能原因**：
1. `setData`调用失败
2. 页面渲染问题
3. 数据绑定错误

**解决方案**：
```javascript
// 在home.js的onLoad中添加测试数据
onLoad() {
    // 先用测试数据验证UI
    this.setData({
        recentActivities: [
            {
                id: '1',
                type: 'notification',
                title: '测试通知',
                description: '这是一条测试通知',
                time: '刚刚',
                orderId: 23777
            }
        ],
        isEmpty: false
    })

    // 然后加载真实数据
    // this.loadRecentNotifications()
}
```

#### 问题D：返回数据结构不对

**症状**：`[Home] API返回结果`显示的数据格式不符合预期

**检查方法**：
```javascript
// 在Console中手动执行
api.notifications.getMyHospitalNotifications({
    pageNum: 0,
    pageSize: 5
}).then(res => {
    console.log('完整响应:', res)
    console.log('是否为数组:', Array.isArray(res))
    console.log('有content属性:', res && 'content' in res)
    console.log('content值:', res && res.content)
})
```

**可能的数据结构**：
```javascript
// 情况1：直接返回数组
[{id: 1, ...}, {id: 2, ...}]

// 情况2：分页对象
{
    content: [{id: 1, ...}, {id: 2, ...}],
    totalElements: 5
}

// 情况3：嵌套在data中
{
    data: {
        content: [...]
    }
}
```

代码已经处理了所有这些情况，应该能正确提取数据。

### 步骤4：验证数据绑定

在首页Page的`data`中检查数据：

```javascript
// 在微信开发者工具的Console中执行
getCurrentPages()[0].data
```

查看`recentActivities`和`isEmpty`的值。

**预期**：
```javascript
{
    recentActivities: [...], // 有5个元素
    isEmpty: false
}
```

### 步骤5：强制刷新页面

1. 在微信开发者工具中点击"编译"
2. 或按快捷键：Ctrl+B (Windows) / Cmd+B (Mac)
3. 查看Console日志是否有变化
4. 检查首页是否显示通知

---

## 🛠️ 临时测试方案

如果调试后还是无法显示，可以先用静态数据测试UI：

**修改home.js**：
```javascript
onLoad() {
    console.log('[Home] 页面加载')

    // 临时使用静态数据测试UI
    this.setData({
        recentActivities: [
            {
                id: '1',
                type: 'notification',
                title: '您的宠物「劈柴」（主人：张惠妹）的检测订单状态已更新为：已审核',
                description: '订单状态已更新',
                time: '刚刚',
                orderId: 23777
            },
            {
                id: '2',
                type: 'notification',
                title: '您的宠物「芹菜」（主人：李明）的检测订单状态已更新为：检测中',
                description: '订单状态已更新',
                time: '2小时前',
                orderId: 23775
            }
        ],
        isEmpty: false
    })

    // 注释掉API调用，先测试UI
    // this.loadRecentNotifications()
}
```

如果静态数据能显示，说明UI没问题，问题在API调用。

---

## 📊 后端API验证

### 使用curl测试API

```bash
# 1. 获取token（先登录）
curl -X POST "http://localhost:8082/api/auth/wechat/login" \
  -H "Content-Type: application/json" \
  -d '{"code": "test_code"}'

# 2. 使用token获取通知列表
curl "http://localhost:8082/api/notifications/hospitals/my?pageNum=0&pageSize=5" \
  -H "Authorization: Bearer YOUR_TOKEN" | jq '.'
```

**预期返回**：
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 5,
        "orderId": 23777,
        "animalName": "劈柴",
        "masterName": "张惠妹",
        "notificationMessage": "您的宠物「劈柴」...",
        "newStatus": 1,
        "operationTime": "2026-01-03T01:08:01"
      }
    ],
    "totalElements": 5
  }
}
```

---

## ✅ 验证清单

使用以下清单逐步排查：

- [ ] 打开微信开发者工具Console
- [ ] 查找`[Home]`开头的日志
- [ ] 确认API请求已发送（有`[Request]`日志）
- [ ] 确认API有响应（有`[Response]`日志）
- [ ] 检查返回数据结构是否正确
- [ ] 检查`recentActivities`是否被正确设置
- [ ] 检查`isEmpty`是否为false
- [ ] 检查WXML中的数据绑定是否正确
- [ ] 尝试使用静态数据测试UI

---

## 🎯 快速修复建议

根据日志输出选择修复方案：

### 情况1：没有看到`[Home]`日志
→ 页面onLoad未执行，检查页面配置

### 情况2：看到`[Request]`但没有`[Response]`
→ API请求失败，检查网络和服务

### 情况3：看到`API返回结果: {}`（空对象）
→ 后端返回空数据，检查数据库

### 情况4：看到`提取的通知数据: []`（空数组）
→ 数据提取逻辑有问题，检查返回结构

### 情况5：看到`格式化通知，数量: 0`
→ 没有通知数据，创建测试通知

### 情况6：看到`首页通知加载完成，数量: 5`但界面为空
→ 数据绑定或渲染问题，使用静态数据测试

---

## 📞 下一步行动

1. **打开Console查看日志**
2. **根据日志输出定位问题**
3. **按照上述方案修复**
4. **如果还是无法解决，提供以下信息**：
   - Console完整日志截图
   - `getCurrentPages()[0].data`的输出
   - API响应的完整数据

---

**更新时间**: 2026-01-03
**版本**: 1.0.0
