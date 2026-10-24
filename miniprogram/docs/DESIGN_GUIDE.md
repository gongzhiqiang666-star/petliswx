# PETLIS 微信小程序设计系统

## 🎨 经典商务蓝风格

> 最后更新：2026-01-01
> 适用范围：所有新增页面和组件

---

## 📐 核心设计原则

- **专业** - 经典蓝色传达信任和专业感
- **现代** - 渐变和阴影营造现代感
- **清晰** - 统一的间距和对齐
- **友好** - 适度的圆角和动画
- **一致** - 所有页面遵循相同规范

---

## 🎨 颜色系统

### 主色调

```css
/* 主蓝色 */
--primary-blue: #2563EB;
--primary-blue-dark: #1D4ED8;

/* 背景色 */
--bg-page: #F8FAFC;          /* 页面背景 */
--bg-card: #FFFFFF;          /* 卡片背景 */
--bg-hover: #F1F5F9;         /* 悬停背景 */

/* 文字色 */
--text-primary: #1E293B;     /* 主要文字 */
--text-secondary: #64748B;   /* 次要文字 */
--text-tertiary: #94A3B8;    /* 辅助文字 */
--text-placeholder: #CBD5E1; /* 占位文字 */

/* 边框色 */
--border-light: #E2E8F0;
--border-lighter: #F1F5F9;
```

### 功能色

```css
/* 状态色 */
--color-success: #059669;    /* 成功 - 绿色 */
--color-warning: #F59E0B;    /* 警告 - 橙色 */
--color-error: #EF4444;      /* 错误 - 红色 */
--color-info: #2563EB;       /* 信息 - 蓝色 */

/* 特殊背景 */
--bg-primary-light: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
--bg-success: #D1FAE5;
--bg-warning: #FEF3C7;
--bg-error: #FEE2E2;
```

---

## 📏 间距系统

### 全局间距标准

```css
/* 主要间距 */
--spacing-xs: 8rpx;
--spacing-sm: 12rpx;
--spacing-md: 16rpx;
--spacing-lg: 24rpx;    /* 全局统一左右间距 */
--spacing-xl: 32rpx;
--spacing-2xl: 48rpx;
```

### 布局规范

```css
/* 页面容器 */
.page {
  background-color: var(--bg-page);
  min-height: 100vh;
}

/* 内容容器 - 不使用左右padding */
.content {
  padding-bottom: XXrpx;  /* 根据需要设置底部padding */
}

/* 卡片/列表项 - 统一左右间距 */
.card, .list-item {
  margin: 0 24rpx 16rpx;  /* 上0 右24 下16 左0 */
}

/* 搜索栏/Tabs */
.search-bar, .tabs {
  margin: 16rpx 24rpx;     /* 或 padding: 0 24rpx */
}
```

---

## 🔲 圆角系统

```css
--radius-sm: 8rpx;        /* 小元素 */
--radius-md: 12rpx;       /* 按钮、标签 */
--radius-lg: 16rpx;       /* 卡片 */
--radius-xl: 24rpx;       /* 大卡片 */
--radius-2xl: 32rpx;      /* 特别大的卡片 */
--radius-full: 48rpx;     /* 搜索栏、圆形按钮 */
```

---

## 🌈 阴影系统

```css
/* 微妙阴影 - 卡片默认 */
--shadow-sm: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

/* 中等阴影 - 按钮、交互元素 */
--shadow-md: 0 4rpx 12rpx rgba(37, 99, 235, 0.15);

/* 强阴影 - 浮动元素 */
--shadow-lg: 0 8rpx 24rpx rgba(99, 102, 241, 0.4);

/* 特强阴影 - 弹窗 */
--shadow-xl: 0 32rpx 96rpx rgba(0, 0, 0, 0.12);
```

---

## 🎯 组件样式规范

### 1. 卡片 (Card)

```css
.card {
  margin: 0 24rpx 16rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: var(--shadow-sm);
  border: 1rpx solid #F1F5F9;
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.header-line {
  width: 6rpx;
  height: 32rpx;
  background: #2563EB;
  margin-right: 16rpx;
  border-radius: 3rpx;
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1E293B;
}
```

### 2. 按钮 (Button)

```css
/* 主按钮 - 蓝色渐变 */
.btn-primary {
  padding: 20rpx 32rpx;
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
  color: #FFFFFF;
  border: none;
  border-radius: 12rpx;
  font-size: 28rpx;
  font-weight: 500;
  box-shadow: var(--shadow-md);
  transition: all 0.3s;
}

.btn-primary::after {
  border: none;
}

.btn-primary:active {
  opacity: 0.9;
  transform: scale(0.98);
}

/* 次要按钮 - 白底蓝边 */
.btn-secondary {
  padding: 20rpx 32rpx;
  background: #FFFFFF;
  color: #2563EB;
  border: 2rpx solid #2563EB;
  border-radius: 12rpx;
  font-size: 28rpx;
  font-weight: 500;
}

.btn-secondary:active {
  background: #EFF6FF;
}
```

### 3. 输入框 (Input)

```css
.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  background: #FFFFFF;
  border: 2rpx solid #E2E8F0;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #1E293B;
  box-sizing: border-box;
  transition: all 0.3s;
}

.form-input:focus {
  border-color: #2563EB;
  box-shadow: 0 0 0 6rpx rgba(37, 99, 235, 0.1);
}

.form-input::placeholder {
  color: #CBD5E1;
}
```

### 4. 搜索栏 (Search Bar)

```css
.search-bar {
  margin: 16rpx 24rpx;
  height: 80rpx;
  background: #FFFFFF;
  border-radius: 48rpx;
  border: 2rpx solid #E2E8F0;
  display: flex;
  align-items: center;
  padding: 0 32rpx;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s;
}

.search-bar:focus-within {
  border-color: #2563EB;
  box-shadow: 0 0 0 6rpx rgba(37, 99, 235, 0.1);
}

.search-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 12rpx;
  opacity: 0.5;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
}
```

### 5. Tabs 标签页

```css
.tabs {
  display: flex;
  background: #FFFFFF;
  border-bottom: 1rpx solid #E2E8F0;
  padding: 0 24rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: #64748B;
  position: relative;
  transition: all 0.3s;
  font-weight: 500;
}

.tab-item.active {
  color: #2563EB;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 4rpx;
  background: #2563EB;
  border-radius: 2rpx;
}
```

### 6. 列表项 (List Item)

```css
.list-item {
  margin: 0 24rpx 16rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: var(--shadow-sm);
  border: 1rpx solid #F1F5F9;
  transition: all 0.3s;
}

.list-item:active {
  box-shadow: var(--shadow-md);
  background: #F8FAFC;
}
```

### 7. 状态标签 (Status Badge)

```css
.status-badge {
  display: inline-block;
  padding: 8rpx 20rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  font-weight: 500;
}

/* 成功状态 */
.status-success {
  background: #D1FAE5;
  color: #059669;
}

/* 警告状态 */
.status-warning {
  background: #FEF3C7;
  color: #D97706;
}

/* 错误状态 */
.status-error {
  background: #FEE2E2;
  color: #DC2626;
}

/* 信息状态 */
.status-info {
  background: #DBEAFE;
  color: #2563EB;
}
```

### 8. 空状态 (Empty State)

```css
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 128rpx 24rpx;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;
}

.empty-text {
  font-size: 30rpx;
  color: #64748B;
  margin-bottom: 48rpx;
  text-align: center;
}
```

---

## 📱 排版系统

### 字体大小

```css
--text-xs: 22rpx;   /* 辅助说明 */
--text-sm: 24rpx;   /* 小标签 */
--text-base: 26rpx; /* 正文 */
--text-md: 28rpx;   /* 正文大 */
--text-lg: 30rpx;   /* 小标题 */
--text-xl: 32rpx;   /* 标题 */
--text-2xl: 36rpx;  /* 大标题 */
--text-3xl: 48rpx;  /* 页面标题 */
--text-4xl: 60rpx;  /* 特大标题 */
```

### 字重

```css
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;
```

---

## 🎭 动画与交互

### 过渡动画

```css
/* 标准过渡 */
transition: all 0.3s;

/* 快速过渡 */
transition: all 0.2s;

/* 慢速过渡 */
transition: all 0.5s;
```

### 交互反馈

```css
/* 点击反馈 */
.element:active {
  opacity: 0.9;
  transform: scale(0.98);
}

/* 悬停反馈 */
.element:hover {
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}
```

### 加载动画

```css
/* 旋转动画 */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-spinner {
  width: 32rpx;
  height: 32rpx;
  border: 3rpx solid #F1F5F9;
  border-top-color: #2563EB;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 脉冲动画 */
@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.4);
  }
  50% {
    box-shadow: 0 0 0 10rpx rgba(37, 99, 235, 0);
  }
}
```

---

## 📄 页面布局模板

### 列表页面模板

```css
/* 页面容器 */
.page {
  background-color: #F8FAFC;
  min-height: 100vh;
}

/* Tabs */
.tabs {
  display: flex;
  justify-content: space-between;
  background: #FFFFFF;
  border-bottom: 1rpx solid #E2E8F0;
  padding: 0 24rpx;
}

/* 列表容器 */
.list-container {
  height: calc(100vh - 200rpx);
}

/* 列表项 */
.list-item {
  margin: 0 24rpx 16rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: var(--shadow-sm);
}
```

### 详情页面模板

```css
/* 头部 */
.header {
  padding: 96rpx 24rpx 64rpx;
  background: linear-gradient(135deg, #6366f1 0%, #9333ea 100%);
  position: relative;
}

/* 内容 */
.content {
  padding: 0 24rpx;
  height: calc(100vh - 400rpx);
}

/* 卡片 */
.card {
  margin: 0 0 24rpx 0;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}
```

### 表单页面模板

```css
/* 内容 */
.content {
  flex: 1;
  padding: 0 24rpx 120rpx;
}

/* 卡片 */
.card {
  background: white;
  border-radius: 16rpx;
  padding: 30rpx;
  margin: 0 0 30rpx 0;
}

/* 表单项 */
.form-group {
  margin-bottom: 30rpx;
}

.form-label {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 16rpx;
}

.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 8rpx;
}
```

---

## ✅ 页面开发检查清单

新增页面时，请确保：

- [ ] 所有卡片/列表项使用 `margin: 0 24rpx XXrpx`
- [ ] 搜索栏/Tabs 使用 `margin: XXrpx 24rpx` 或 `padding: 0 24rpx`
- [ ] 容器不使用左右padding
- [ ] 卡片使用 16rpx 圆角
- [ ] 按钮使用蓝色渐变背景
- [ ] 输入框高度 80rpx，圆角 12rpx
- [ ] 文字颜色使用 `#1E293B` (主) 和 `#64748B` (次)
- [ ] 背景色使用 `#F8FAFC` (页面) 和 `#FFFFFF` (卡片)
- [ ] 添加点击反馈 `:active { opacity: 0.9; transform: scale(0.98); }`
- [ ] 使用统一的阴影 `var(--shadow-sm)` 或 `var(--shadow-md)`

---

## 🎨 渐变参考

```css
/* 主按钮渐变 */
gradient-primary: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);

/* 浅蓝背景渐变 */
gradient-light: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);

/* 紫蓝渐变（特殊场景） */
gradient-purple: linear-gradient(135deg, #6366f1 0%, #9333ea 100%);
```

---

## 📞 联系方式

如有设计疑问，请参考本设计系统或咨询设计团队。

**文档版本**: v1.0
**最后更新**: 2026-01-01
