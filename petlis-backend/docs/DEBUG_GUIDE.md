# 微信小程序调试指南

## 问题：模拟器长时间没有响应

### 可能的原因

1. **API请求过多或卡死** - order-test-items页面原来会为每个检测项目发送一个价格请求
2. **死循环** - 某个页面的数据处理逻辑有问题
3. **页面栈溢出** - 页面跳转逻辑有问题
4. **setData调用过多** - 频繁调用setData导致性能问题

### 已修复的问题

1. **order-test-items.js** - 移除了为每个项目单独请求价格的逻辑，直接使用API返回的价格
2. **order-preview.js** - 添加了try-catch保护，避免数据错误导致崩溃
3. **pet-add.js** - 优化了字典数据加载逻辑

### 排查步骤

#### 1. 检查控制台错误

打开微信开发者工具的调试器（Console标签），查看是否有错误信息：

```javascript
// 常见错误类型
- TypeError: Cannot read property 'xxx' of undefined
- ReferenceError: xxx is not defined
- Maximum call stack size exceeded (死循环)
```

#### 2. 检查网络请求

打开调试器的Network标签，查看API请求：
- 是否有请求一直处于pending状态
- 是否有请求返回错误
- 是否有大量重复请求

#### 3. 检查页面入口

查看app.json中的pages数组，确认第一个页面（启动页）：

```json
"pages": [
  "pages/home/home",  // 这是启动页
  ...
]
```

如果home页面有问题，会导致整个小程序卡死。

#### 4. 临时禁用某些页面

如果不确定是哪个页面的问题，可以临时在app.json中注释掉某些页面：

```json
"pages": [
  "pages/home/home",
  "pages/pets/pets",
  "pages/orders/orders",
  "pages/mine/mine"
  // 临时注释掉新页面
  // "pages/order-add-pet/order-add-pet",
  // "pages/order-test-items/order-test-items",
  // "pages/order-preview/order-preview"
]
```

#### 5. 检查app.js

查看app.js是否有问题：

```javascript
// app.js
App({
  onLaunch() {
    console.log('App Launch');
    // 如果这里有死循环，会导致整个小程序卡死
  }
});
```

### 快速修复方案

#### 方案1: 清除缓存并重新编译

1. 在微信开发者工具中，点击"工具" → "清除缓存"
2. 选择"全部清除"
3. 重新编译项目

#### 方案2: 检查API配置

查看`miniprogram/api/index.js`，确认API基础URL配置正确：

```javascript
// 检查API配置
const BASE_URL = 'http://localhost:8080/api';  // 确认后端服务已启动
```

#### 方案3: 简化启动逻辑

如果home页面有问题，可以临时修改app.json，将启动页改为简单的页面：

```json
"pages": [
  "pages/pets/pets",  // 临时改为宠物列表页作为启动页
  "pages/home/home",
  ...
]
```

### 常见问题排查

#### 问题1: 页面空白或白屏

**原因**: 页面JS文件有语法错误或运行时错误

**解决**:
1. 打开Console查看错误信息
2. 检查对应页面的.js文件
3. 确保所有API调用都有try-catch

#### 问题2: 数据不显示

**原因**: 字段名不匹配或API返回数据为空

**解决**:
1. 在Console中打印API返回数据：`console.log('Data:', data)`
2. 检查WXML中的字段名是否与JS中的data字段名一致
3. 确认API确实返回了数据

#### 问题3: 页面跳转失败

**原因**: 页面路径错误或页面未在app.json中注册

**解决**:
1. 检查页面路径是否正确
2. 确认页面已在app.json的pages数组中
3. 检查页面路径大小写是否正确

### 调试技巧

#### 1. 添加console.log

在关键位置添加console.log，追踪代码执行：

```javascript
onLoad() {
  console.log('[PageName] onLoad 开始');
  // ... 你的代码
  console.log('[PageName] onLoad 结束');
}
```

#### 2. 使用debugger

在代码中添加debugger断点：

```javascript
onLoad() {
  debugger;  // 程序会在这里暂停
  // ... 你的代码
}
```

#### 3. 分段注释

如果不确定哪段代码有问题，可以分段注释：

```javascript
async loadTestItems() {
  try {
    // 先注释掉API调用，直接返回模拟数据
    // const items = await api.dictionary.getTestTypes();
    const items = [
      { infoTestTypeId: 1, testTypeName: '血常规', testPrice: 50 },
      { infoTestTypeId: 2, testTypeName: '生化检查', testPrice: 100 }
    ];
    
    this.setData({ allItems: items });
  } catch (error) {
    console.error(error);
  }
}
```

### 性能优化建议

1. **减少setData调用频率** - 不要在循环中频繁调用setData
2. **合并数据更新** - 一次性更新多个字段
3. **使用分页加载** - 列表数据使用分页，不要一次加载全部
4. **图片懒加载** - 图片使用懒加载
5. **避免深拷贝** - 大对象使用浅拷贝

### 联系后端开发者

如果确定是后端API的问题：

1. 确认后端服务已启动
2. 确认API端口正确（8080）
3. 使用Postman测试API是否正常
4. 检查后端日志是否有错误

### 紧急恢复

如果所有方法都无法解决，可以：

1. 回退到之前工作的版本
2. 只保留核心页面，逐个添加新页面
3. 使用微信小程序官方示例代码测试

---

## 检查清单

- [ ] 控制台没有错误信息
- [ ] 网络请求正常返回
- [ ] 启动页面没有问题
- [ ] API配置正确
- [ ] 后端服务已启动
- [ ] 字段名与后端API一致
- [ ] 没有死循环
- [ ] setData调用频率合理
