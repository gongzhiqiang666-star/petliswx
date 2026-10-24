# 错误处理系统 - 快速应用示例

## 针对当前 order-select-pet 页面的网络错误优化

### 当前代码（不友好）

```javascript
// pages/order-select-pet/order-select-pet.js
async loadPets() {
  this.setData({ loading: true })

  try {
    const res = await request.get('/api/pets', {
      hospital_id: hospitalId
    })
    this.setData({
      pets: res,
      filteredPets: res,
      loading: false
    })
  } catch (error) {
    console.error('[OrderSelectPet] 加载宠物列表失败:', error)
    this.setData({ loading: false })
    // ❌ 只在控制台输出，用户无感知
  }
}
```

### 优化后的代码（友好提示）

```javascript
// pages/order-select-pet/order-select-pet.js
const errorHandler = require('../../utils/error-handler.js')

Page({
  async loadPets() {
    this.setData({ loading: true })

    try {
      const res = await request.get('/api/pets', {
        hospital_id: hospitalId
      })
      this.setData({
        pets: res,
        filteredPets: res,
        loading: false
      })
    } catch (error) {
      console.error('[OrderSelectPet] 加载宠物列表失败:', error)
      this.setData({ loading: false })

      // ✅ 显示友好的错误提示
      errorHandler.handle(error, {
        showType: 'toast',  // 使用toast提示
        onRetry: () => this.loadPets()  // 提供重试功能
      })
    }
  }
})
```

---

## 更多应用场景

### 场景1：订单列表加载失败

**文件：pages/orders/orders.js**

```javascript
const errorHandler = require('../../utils/error-handler.js')

Page({
  async loadOrders(refresh = false) {
    if (refresh) {
      this.setData({ refreshing: true })
    } else if (!this.data.hasMore) {
      return
    }

    this.setData({ loadingMore: true })

    try {
      const res = await request.get('/api/orders', {
        page: this.data.currentPage,
        page_size: this.data.pageSize
      })

      // 处理数据...
      this.setData({ loadingMore: false, refreshing: false })
    } catch (error) {
      console.error('[Orders] 加载订单失败:', error)
      this.setData({ loadingMore: false, refreshing: false })

      // 显示友好的错误提示
      errorHandler.handle(error, {
        showType: 'toast',
        onRetry: () => this.loadOrders(refresh)
      })
    }
  }
})
```

### 场景2：表单提交失败

**文件：pages/order-add-pet/order-add-pet.js**

```javascript
const errorHandler = require('../../utils/error-handler.js')

Page({
  async onSubmit() {
    if (!this.validateForm()) {
      return
    }

    wx.showLoading({ title: '提交中...' })

    try {
      await request.post('/api/orders', this.formData)
      wx.hideLoading()

      wx.showToast({
        title: '提交成功',
        icon: 'success'
      })

      // 跳转到订单详情
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/order-detail/order-detail'
        })
      }, 1500)
    } catch (error) {
      wx.hideLoading()
      console.error('[OrderAdd] 提交订单失败:', error)

      // 使用Modal弹窗，提供重试选项
      errorHandler.handle(error, {
        showType: 'modal',
        customMessage: '订单提交失败',
        onRetry: () => this.onSubmit()
      })
    }
  }
})
```

### 场景3：详情页面数据加载失败

**文件：pages/order-detail/order-detail.js**

```javascript
const errorHandler = require('../../utils/error-handler.js')

Page({
  async loadOrderDetail() {
    this.setData({ loading: true })

    try {
      const orderId = this.data.orderId
      const detail = await request.get(`/api/orders/${orderId}`)

      this.setData({
        orderData: detail,
        loading: false,
        error: null
      })
    } catch (error) {
      console.error('[OrderDetail] 加载订单详情失败:', error)
      this.setData({
        loading: false,
        error: true
      })

      // 显示完整错误页面
      errorHandler.handle(error, {
        showType: 'full',
        customMessage: '订单详情加载失败',
        showSecondary: true,  // 显示"返回首页"按钮
        onRetry: () => this.loadOrderDetail()
      })
    }
  }
})
```

### 场景4：上传文件失败

**文件：任何需要上传文件的页面**

```javascript
const errorHandler = require('../../utils/error-handler.js')

Page({
  async uploadFile(filePath) {
    wx.showLoading({ title: '上传中...' })

    try {
      await request.uploadFile('/api/upload', filePath)

      wx.hideLoading()
      wx.showToast({ title: '上传成功', icon: 'success' })
    } catch (error) {
      wx.hideLoading()
      console.error('[Upload] 上传失败:', error)

      // 根据错误类型选择提示方式
      if (error.message.includes('网络')) {
        errorHandler.showNetworkError()
      } else if (error.message.includes('超时')) {
        errorHandler.showTimeoutError()
      } else {
        errorHandler.handle(error, {
          showType: 'toast',
          customMessage: '文件上传失败'
        })
      }
    }
  }
})
```

---

## 全局错误处理（可选）

### 在 app.js 中添加全局错误监听

```javascript
// app.js
const errorHandler = require('./utils/error-handler.js')

App({
  onLaunch() {
    // 监听全局错误
    wx.onError((error) => {
      console.error('[Global Error]', error)

      // 只在生产环境显示友好提示
      if (process.env.NODE_ENV === 'production') {
        errorHandler.handle(error, {
          showType: 'toast'
        })
      }
    })

    // 监听未处理的Promise错误
    wx.onUnhandledRejection((res) => {
      console.error('[Unhandled Rejection]', res)

      if (process.env.NODE_ENV === 'production') {
        errorHandler.handle(res.reason, {
          showType: 'toast'
        })
      }
    })
  }
})
```

---

## 统一的页面错误处理模板

创建一个页面基类或混入：

```javascript
// mixins/page-error-handler.js
const errorHandler = require('../../utils/error-handler.js')

module.exports = {
  /**
   * 安全的数据加载方法
   * @param {Function} loadFn 加载函数
   * @param {Object} options 选项
   */
  async safeLoad(loadFn, options = {}) {
    const {
      loadingKey = 'loading',
      errorKey = 'error',
      showType = 'toast',
      customMessage = '',
      onSuccess = null,
      onError = null
    } = options

    this.setData({ [loadingKey]: true, [errorKey]: null })

    try {
      const result = await loadFn()

      this.setData({
        [loadingKey]: false
      })

      if (onSuccess) {
        onSuccess(result)
      }

      return result
    } catch (error) {
      console.error('[Page Error]', error)

      this.setData({
        [loadingKey]: false,
        [errorKey]: error
      })

      errorHandler.handle(error, {
        showType,
        customMessage,
        onRetry: () => this.safeLoad(loadFn, options)
      })

      if (onError) {
        onError(error)
      }

      throw error
    }
  }
}
```

### 在页面中使用混入

```javascript
// pages/order-select-pet/order-select-pet.js
const pageErrorHandler = require('../../mixins/page-error-handler.js')

const errorHandler = require('../../utils/error-handler.js')
const request = require('../../utils/request.js')

Page({
  ...pageErrorHandler,  // 混入错误处理方法

  onLoad() {
    this.loadPets()
  },

  loadPets() {
    // 使用安全加载方法
    this.safeLoad(
      () => request.get('/api/pets', {
        hospital_id: wx.getStorageSync('hospitalId')
      }),
      {
        loadingKey: 'loading',
        errorKey: 'error',
        showType: 'toast',
        customMessage: '加载宠物列表失败',
        onSuccess: (res) => {
          this.setData({
            pets: res,
            filteredPets: res
          })
        }
      }
    )
  }
})
```

---

## 最佳实践总结

### ✅ 推荐做法

1. **所有网络请求都包裹在 try-catch 中**
2. **使用 errorHandler.handle() 统一处理错误**
3. **提供重试回调**
4. **根据错误严重程度选择合适的显示模式**
5. **在控制台输出详细错误日志便于调试**

### ❌ 避免的做法

1. **catch 块中留空**
2. **只在控制台输出错误，用户无感知**
3. **使用硬编码的错误提示**
4. **不提供重试机制**
5. **使用 alert() 等原生提示**

---

## 测试清单

在开发时，请测试以下场景：

- [ ] 网络关闭时访问页面
- [ ] 网络超时（设置短超时时间）
- [ ] 服务器返回500错误
- [ ] 服务器返回404错误
- [ ] 认证失败（401）
- [ ] 权限不足（403）
- [ ] 数据格式错误

---

## 完整示例：重构 order-select-pet 页面

```javascript
// pages/order-select-pet/order-select-pet.js
const request = require('../../utils/request.js')
const errorHandler = require('../../utils/error-handler.js')

Page({
  data: {
    pets: [],
    filteredPets: [],
    loading: false,
    error: null
  },

  onLoad() {
    this.loadPets()
  },

  /**
   * 加载宠物列表
   */
  async loadPets() {
    this.setData({ loading: true, error: null })

    try {
      const hospitalId = wx.getStorageSync('hospitalId')
      const res = await request.get('/api/pets', {
        hospital_id: hospitalId
      })

      this.setData({
        pets: res,
        filteredPets: res,
        loading: false
      })

      console.log('[OrderSelectPet] 加载成功，宠物数量:', res.length)
    } catch (error) {
      console.error('[OrderSelectPet] 加载失败:', error)

      this.setData({
        loading: false,
        error: error.message
      })

      // 显示友好的错误提示
      errorHandler.handle(error, {
        showType: 'toast',
        customMessage: '加载宠物列表失败',
        onRetry: () => this.loadPets()
      })
    }
  },

  /**
   * 重试加载
   */
  onRetry() {
    this.loadPets()
  },

  // ... 其他方法
})
```

这样，当网络连接失败时，用户会看到：

```
┌─────────────────────┐
│  📡 网络连接失败     │
└─────────────────────┘
```

并且可以点击"重试"按钮重新加载数据，大大提升了用户体验！
