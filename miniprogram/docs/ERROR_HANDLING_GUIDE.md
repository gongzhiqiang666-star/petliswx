# 错误处理系统使用指南

## 📋 概述

本小程序已集成统一的错误处理系统，提供友好的用户错误提示，提升用户体验。

---

## 🎯 功能特性

### 1. **多种错误类型识别**
- ✅ 网络连接失败 (ERR_CONNECTION_REFUSED)
- ✅ 请求超时
- ✅ 服务器错误 (500)
- ✅ 认证失败 (401)
- ✅ 权限不足 (403)
- ✅ 资源不存在 (404)
- ✅ 业务错误

### 2. **三种显示模式**
- **Toast**：轻量级提示，适合临时错误
- **Modal**：弹窗提示，适合需要用户确认的错误
- **Full Page**：完整错误页面，适合严重错误

### 3. **友好的错误提示**
- 📡 图标化展示
- 💡 详细说明和建议
- 🔄 一键重试
- 📞 联系客服入口

---

## 🚀 快速使用

### 方式一：在页面中捕获错误

```javascript
// pages/order-select-pet/order-select-pet.js
const request = require('../../utils/request.js')
const errorHandler = require('../../utils/error-handler.js')

Page({
  async loadPets() {
    try {
      const pets = await request.get('/api/pets')
      this.setData({ pets })
    } catch (error) {
      console.error('加载宠物列表失败:', error)

      // 显示友好的错误提示
      errorHandler.handle(error, {
        showType: 'toast',  // 使用toast提示
        onRetry: () => this.loadPets()  // 提供重试回调
      })
    }
  }
})
```

### 方式二：使用 request.showError()

```javascript
Page({
  async loadPets() {
    try {
      const pets = await request.get('/api/pets')
      this.setData({ pets })
    } catch (error) {
      // 使用request的showError方法
      request.showError(error)
    }
  }
})
```

### 方式三：显示完整错误页面

```javascript
Page({
  async loadPets() {
    try {
      const pets = await request.get('/api/pets')
      this.setData({ pets })
    } catch (error) {
      // 显示完整错误页面（适合严重错误）
      errorHandler.handle(error, {
        showType: 'full',
        onRetry: () => this.loadPets(),
        showSecondary: true  // 显示"返回首页"按钮
      })
    }
  }
})
```

### 方式四：使用Modal弹窗

```javascript
Page({
  async loadPets() {
    try {
      const pets = await request.get('/api/pets')
      this.setData({ pets })
    } catch (error) {
      // 使用Modal弹窗，提供重试选项
      errorHandler.handle(error, {
        showType: 'modal',
        onRetry: () => this.loadPets()
      })
    }
  }
})
```

---

## 📖 API文档

### errorHandler.handle(error, options)

**参数说明：**
- `error` (Error): 错误对象
- `options` (Object): 处理选项
  - `showType` (String): 显示类型 - `'toast'` | `'modal'` | `'full'`，默认 `'toast'`
  - `customMessage` (String): 自定义错误消息
  - `onRetry` (Function): 重试回调函数
  - `onError` (Function): 错误回调函数
  - `showSecondary` (Boolean): 是否显示次要按钮（返回首页）

**示例：**

```javascript
errorHandler.handle(error, {
  showType: 'modal',
  customMessage: '加载宠物信息失败',
  onRetry: () => {
    console.log('用户点击了重试')
    this.loadData()
  },
  onError: (errorType, error) => {
    console.log('错误类型:', errorType)
    // 可以在这里做错误统计
  }
})
```

### 快捷方法

```javascript
// 网络错误
errorHandler.showNetworkError()

// 超时错误
errorHandler.showTimeoutError()

// 服务器错误
errorHandler.showServerError()
```

---

## 🎨 错误页面自定义

### 修改错误提示文案

在 `utils/error-handler.js` 中修改 `ErrorMessages` 对象：

```javascript
const ErrorMessages = {
  [ErrorType.NETWORK_ERROR]: {
    title: '网络连接失败',
    message: '请检查网络设置后重试',
    icon: '📡',
    suggest: '建议：\n• 检查WiFi或移动网络是否开启\n• 尝试切换网络环境\n• 稍后再试'
  },
  // ... 其他错误类型
}
```

### 修改错误页面样式

在 `pages/error-page/error-page.wxss` 中修改样式：

```css
/* 修改背景色 */
.page {
  background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
}

/* 修改图标大小 */
.error-icon {
  font-size: 96rpx;
}
```

---

## 🔧 实际应用示例

### 示例1：列表页面加载失败

```javascript
Page({
  data: {
    pets: [],
    loading: false
  },

  onLoad() {
    this.loadPets()
  },

  async loadPets() {
    this.setData({ loading: true })

    try {
      const pets = await request.get('/api/pets')
      this.setData({ pets, loading: false })
    } catch (error) {
      this.setData({ loading: false })

      // 显示toast提示
      errorHandler.handle(error, {
        showType: 'toast',
        onRetry: () => this.loadPets()
      })
    }
  }
})
```

### 示例2：表单提交失败

```javascript
Page({
  async onSubmit() {
    wx.showLoading({ title: '提交中...' })

    try {
      await request.post('/api/order', this.formData)
      wx.hideLoading()
      wx.showToast({ title: '提交成功' })

      // 跳转到订单详情
      wx.navigateTo({
        url: '/pages/order-detail/order-detail'
      })
    } catch (error) {
      wx.hideLoading()

      // 使用Modal弹窗显示错误
      errorHandler.handle(error, {
        showType: 'modal',
        customMessage: '订单提交失败，请重试',
        onRetry: () => this.onSubmit()
      })
    }
  }
})
```

### 示例3：严重错误跳转错误页面

```javascript
Page({
  async initPage() {
    try {
      // 关键数据加载
      const config = await request.get('/api/config')

      if (!config) {
        throw new Error('系统配置加载失败')
      }

      this.setData({ config })
    } catch (error) {
      // 严重错误，跳转到错误页面
      errorHandler.handle(error, {
        showType: 'full',
        customMessage: '系统初始化失败，无法继续',
        showSecondary: true,
        onRetry: () => this.initPage()
      })
    }
  }
})
```

---

## 📱 效果展示

### Toast提示效果
```
┌─────────────────────┐
│  📡 网络连接失败     │
└─────────────────────┘
```

### Modal弹窗效果
```
┌─────────────────────────┐
│   网络连接失败          │
├─────────────────────────┤
│  请检查网络设置后重试   │
│                         │
│  建议：                 │
│  • 检查WiFi或移动网络   │
│  • 尝试切换网络环境     │
│  • 稍后再试             │
├─────────────────────────┤
│    [关闭]  [重试]       │
└─────────────────────────┘
```

### 完整错误页面效果
```
    ┌─────────────────┐
    │      📡         │
    │  网络连接失败    │
    │请检查网络设置    │
    │                 │
    │ 建议：           │
    │ • 检查WiFi...    │
    │                 │
    │ [查看详情 v]     │
    │                 │
    │ [  重新加载  ]   │
    │ [  返回首页  ]   │
    │ [  联系客服  ]   │
    │                 │
    │ 如问题持续...     │
    └─────────────────┘
```

---

## 💡 最佳实践

### 1. **选择合适的显示模式**

| 错误场景 | 推荐模式 | 说明 |
|---------|---------|------|
| 列表加载失败 | toast | 轻量级，不打断用户 |
| 表单提交失败 | modal | 需要用户确认重试 |
| 系统初始化失败 | full | 严重错误，需要专门页面 |
| 网络超时 | toast | 临时性错误，快速提示 |
| 认证失败 | modal | 特殊处理，引导重新登录 |

### 2. **提供重试机制**

```javascript
// ✅ 推荐：提供重试回调
errorHandler.handle(error, {
  onRetry: () => this.loadData()
})

// ❌ 不推荐：让用户手动返回重试
```

### 3. **自定义错误消息**

```javascript
// ✅ 推荐：提供具体的错误信息
errorHandler.handle(error, {
  customMessage: '加载宠物信息失败'
})

// ❌ 不推荐：使用通用错误消息
```

### 4. **错误统计**

```javascript
errorHandler.handle(error, {
  onError: (errorType, error) => {
    // 上报错误到统计平台
    wx.reportEvent('error', {
      type: errorType,
      page: getCurrentPages().pop().route,
      message: error.message
    })
  }
})
```

---

## 🔍 调试技巧

### 1. **查看错误日志**

所有错误都会在控制台输出，包含：
- `[Request Error]` - 网络请求错误
- `[ErrorHandler]` - 错误处理器日志
- `[ErrorPage]` - 错误页面日志

### 2. **模拟网络错误**

在微信开发者工具中：
1. 打开 "Network" 面板
2. 勾选 "Offline" 模拟离线状态
3. 测试错误提示效果

### 3. **测试不同错误类型**

```javascript
// 测试网络错误
errorHandler.handle(new Error('request:fail'))

// 测试超时错误
errorHandler.handle(new Error('request timeout'))

// 测试认证错误
errorHandler.handle(new Error('登录已过期'))
```

---

## 📚 相关文件

- `/utils/error-handler.js` - 错误处理器
- `/utils/request.js` - 请求封装
- `/pages/error-page/` - 错误页面组件
- `/config/config.js` - 配置文件

---

## ❓ 常见问题

### Q1: 如何自定义错误图标？

修改 `utils/error-handler.js` 中的 `ErrorMessages` 对象，修改 `icon` 字段：

```javascript
icon: '📡',  // 使用emoji
// 或
icon: '/assets/icons/error-icon.png'  // 使用图片
```

### Q2: 错误页面如何返回上一页并重试？

错误页面会自动检测上一页是否有 `onRetry` 方法，如果有会自动调用。确保你的页面有这个方法：

```javascript
Page({
  onRetry() {
    this.loadData()
  }
})
```

### Q3: 如何禁用错误提示？

如果某个请求不需要自动错误提示，可以：

```javascript
try {
  const data = await request.get('/api/data')
} catch (error) {
  // 自己处理，不使用errorHandler
  console.log(error)
}
```

---

## 📞 技术支持

如有问题，请联系开发团队。
