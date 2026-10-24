# 快速启动检查清单

## 🔴 紧急修复：端口配置问题

**已修复**: `miniprogram/config/config.js`中的端口从8081改为8080

---

## ✅ 启动前检查清单

### 1. 后端服务检查

```bash
# 检查后端服务是否运行
curl http://localhost:8080/api/dictionary/animals

# 预期响应
{
  "code": 200,
  "message": "操作成功",
  "data": [...]
}
```

**如果服务未运行**：
```bash
cd /Users/gong/claudelife/PETLIS/petlis-backend
# 启动后端服务（根据你的启动方式）
mvn spring-boot:run
# 或者
./gradlew bootRun
# 或者
java -jar target/petlis-backend-1.0.0.jar
```

### 2. 微信开发者工具设置

1. **打开项目**：`miniprogram` 目录
2. **点击"编译"按钮**
3. **检查控制台**：
   - 应该看到：`App Launch`
   - 不应该有红色错误信息

### 3. 调试器检查（Console标签）

打开调试器，查看是否有以下错误：

**正常情况**：
```
[Request] GET http://localhost:8080/api/dictionary/animals
[Response] [...]
```

**错误情况1：服务未启动**
```
[Request Error] Request failed
```
→ **解决**：启动后端服务

**错误情况2：端口错误**
```
[Request Error] connect ECONNREFUSED 127.0.0.1:8080
```
→ **解决**：确认后端在8080端口运行

**错误情况3：跨域问题**
```
[Request Error] Access-Control-Allow-Origin
```
→ **解决**：后端需要配置CORS允许小程序域名

### 4. 网络请求检查（Network标签）

在调试器Network标签中：

1. **查看请求列表**
   - 应该看到API请求
   - 状态码应该是200

2. **查看请求详情**
   - 点击某个请求
   - 查看Response内容

### 5. 页面跳转测试

**测试步骤**：
1. 启动小程序后应该进入首页（home页面）
2. 点击底部Tab切换到"宠物"页面
3. 点击底部Tab切换到"订单"页面
4. 点击底部Tab切换到"我的"页面

**如果某个页面卡死**：
- 查看Console中该页面的日志
- 检查该页面的.js文件是否有API调用错误

---

## 🐛 常见问题排查

### 问题1: "模拟器长时间没有响应"

**原因**：
1. 后端服务未启动
2. API端口配置错误
3. 某个页面有死循环
4. API调用卡死

**解决步骤**：

#### 步骤1: 确认后端运行
```bash
# 测试后端API
curl http://localhost:8080/api/dictionary/animals

# 如果返回数据，说明后端正常
# 如果报错，需要启动后端服务
```

#### 步骤2: 检查小程序控制台
打开微信开发者工具的Console标签，查看错误信息

#### 步骤3: 临时简化测试
修改`app.json`，将启动页改为简单页面：

```json
"pages": [
  "pages/pets/pets",  // 改为宠物列表页启动
  "pages/home/home",
  ...
]
```

#### 步骤4: 清除缓存
点击微信开发者工具的"工具" → "清除缓存" → "全部清除"

### 问题2: 某个页面白屏

**原因**：页面JS文件有错误

**解决**：
1. 打开Console查看错误
2. 检查对应页面的.js文件
3. 确保所有API调用都有try-catch

### 问题3: 数据不显示

**原因**：API返回数据格式不对或字段名不匹配

**解决**：
1. 在Console中打印API返回数据
2. 使用Postman测试API
3. 检查字段名是否使用驼峰命名

### 问题4: "登录已过期"

**原因**：Token无效或过期

**解决**：
1. 清除Storage
2. 重新登录
3. 检查后端JWT配置

---

## 📊 性能检查

### 检查API请求数量

在Network标签中查看请求列表：
- 正常：页面加载时2-5个请求
- 异常：几十个请求（可能是死循环）

### 检查页面加载时间

- 正常：< 2秒
- 慢：> 5秒（需要优化）

---

## 🔧 开发建议

### 1. 逐步测试

不要一次性测试所有功能，按以下顺序：

1. 先测试首页（home）能否正常显示
2. 再测试宠物列表能否加载
3. 然后测试"我的"页面能否加载
4. 最后测试预约检测流程

### 2. 使用Mock数据

如果后端API有问题，可以先使用Mock数据测试：

```javascript
// 在页面JS中
async loadPets() {
  // 临时使用Mock数据
  const mockData = [
    { id: 1, animalName: '小花', animalType: '猫', dictSexId: 2 }
  ];
  this.setData({ pets: mockData });
}
```

### 3. 分段调试

在关键位置添加console.log：

```javascript
onLoad() {
  console.log('[PageName] 页面加载开始');
  
  this.loadData();
  
  console.log('[PageName] 页面加载结束');
}
```

---

## 📝 日志规范

所有console.log都应该加上标签前缀：

```javascript
// 好的做法
console.log('[PetList] 加载宠物列表:', pets);
console.log('[OrderTestItems] 切换分类:', categoryId);
console.error('[Request] API请求失败:', error);

// 不好的做法
console.log(pets);
console.error(error);
```

这样可以快速定位问题来源。

---

## ✅ 成功标志

当所有检查都通过，你应该看到：

1. ✅ 控制台没有红色错误
2. ✅ 网络请求返回200状态码
3. ✅ 首页正常显示
4. ✅ 可以切换Tab页面
5. ✅ 宠物列表能加载（如果后端有数据）
6. ✅ 点击宠物能看到详情

---

## 🆘 紧急恢复

如果所有方法都无法解决：

### 选项1: 回退到最小版本

只保留核心页面，删除所有新页面：

```json
"pages": [
  "pages/home/home",
  "pages/pets/pets",
  "pages/orders/orders",
  "pages/mine/mine"
]
```

### 选项2: 使用官方示例

创建一个新的微信小程序项目，确认工具本身没问题。

### 选项3: 联系支持

提供以下信息：
- 微信开发者工具版本
- 操作系统版本
- 控制台错误截图
- Network标签截图
