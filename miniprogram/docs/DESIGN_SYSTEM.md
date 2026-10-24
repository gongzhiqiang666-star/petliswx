# PETLIS 微信小程序设计系统

> **当前使用的配色方案：方案一 - 经典商务蓝** ✅
> 最后更新：2026-01-01
> 新增页面请严格遵循此设计系统

---

## 设计原则

- **商务专业** - 适合医疗/检测行业的专业形象
- **简洁大方** - 避免过度装饰，突出内容
- **统一规范** - 间距、颜色、字体保持一致
- **易用友好** - 清晰的视觉层次，良好的可读性

---

## 全局间距规范（重要！）

```css
/* 统一左右间距：24rpx */
所有页面卡片/列表项: margin: 0 24rpx 16rpx;
搜索栏/Tabs: margin: XXrpx 24rpx 或 padding: 0 24rpx;
容器: 不使用左右padding，让子元素自己通过margin控制
```

---

## 配色方案

### 方案一：经典商务蓝（推荐）
> 适合医疗、检测行业，专业稳重

```json
{
  "主色（Primary）": "#2563EB - 蓝色",
  "辅助色（Secondary）": "#64748B - 蓝灰",
  "成功色（Success）": "#10B981 - 绿色",
  "警告色（Warning）": "#F59E0B - 橙色",
  "错误色（Error）": "#EF4444 - 红色",
  "背景色（Background）": "#F8FAFC - 浅灰",
  "卡片色（Card）": "#FFFFFF - 白色",
  "文字色（Text）": "#1E293B - 深灰",
  "次要文字（Text Secondary）": "#64748B - 中灰",
  "边框色（Border）": "#E2E8F0 - 浅灰"
}
```

### 方案二：现代紫蓝
> 科技感强，年轻化

```json
{
  "主色": "#6366F1 - 靛蓝（当前使用）",
  "辅助色": "#8B5CF6 - 紫色",
  "成功色": "#10B981",
  "警告色": "#F59E0B",
  "错误色": "#EF4444",
  "背景色": "#F9FAFB",
  "文字色": "#1F2937"
}
```

### 方案三：医疗青绿
> 医疗行业常用色，清新专业

```json
{
  "主色": "#059669 - 青绿",
  "辅助色": "#0D9488 - 青色",
  "成功色": "#10B981",
  "警告色": "#F59E0B",
  "错误色": "#EF4444",
  "背景色": "#F0FDFA",
  "文字色": "#134E4A"
}
```

---

## TabBar 配色方案

### 方案一：经典商务蓝
```json
{
  "color": "#64748B",
  "selectedColor": "#2563EB",
  "backgroundColor": "#FFFFFF",
  "borderStyle": "white"
}
```

### 方案二：现代紫蓝（当前）
```json
{
  "color": "#666666",
  "selectedColor": "#6366F1",
  "backgroundColor": "#FFFFFF",
  "borderStyle": "black"
}
```

### 方案三：医疗青绿
```json
{
  "color": "#6B7280",
  "selectedColor": "#059669",
  "backgroundColor": "#FFFFFF",
  "borderStyle": "white"
}
```

---

## 间距规范

采用 **8px 基准网格**系统：

```
页面边距：24rpx (12px)
卡片间距：16rpx (8px)
元素间距：12rpx (6px)
小间距：    8rpx  (4px)
```

### 常用间距场景

```css
/* 页面容器 */
.page-container {
  padding: 24rpx;
}

/* 卡片 */
.card {
  margin-bottom: 16rpx;
  padding: 24rpx;
  border-radius: 12rpx;
}

/* 表单元素 */
.form-group {
  margin-bottom: 24rpx;
}

.form-item {
  padding: 20rpx 24rpx;
  border-radius: 8rpx;
}

/* 按钮间距 */
.btn-group {
  gap: 12rpx;
}
```

---

## 字体规范

```css
/* 标题 */
.page-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1E293B;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #334155;
}

/* 正文 */
.body-text {
  font-size: 28rpx;
  color: #475569;
}

/* 次要文字 */
.secondary-text {
  font-size: 24rpx;
  color: #94A3B8;
}

/* 辅助说明 */
.caption-text {
  font-size: 22rpx;
  color: #CBD5E1;
}
```

---

## 组件样式规范

### 按钮

**主要按钮（Primary）**
```css
.btn-primary {
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
  color: #FFFFFF;
  padding: 20rpx 48rpx;
  border-radius: 12rpx;
  font-size: 28rpx;
  font-weight: 500;
  box-shadow: 0 4rpx 12rpx rgba(37, 99, 235, 0.15);
}
```

**次要按钮（Secondary）**
```css
.btn-secondary {
  background: #F1F5F9;
  color: #475569;
  padding: 20rpx 48rpx;
  border-radius: 12rpx;
  font-size: 28rpx;
  border: 2rpx solid #E2E8F0;
}
```

### 卡片

```css
.card {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
  border: 1rpx solid #F1F5F9;
}
```

### 输入框

```css
.input {
  background: #F8FAFC;
  border: 2rpx solid #E2E8F0;
  border-radius: 12rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #1E293B;
  transition: all 0.3s;
}

.input:focus {
  border-color: #2563EB;
  background: #FFFFFF;
  box-shadow: 0 0 0 6rpx rgba(37, 99, 235, 0.1);
}
```

---

## 推荐实施顺序

1. ✅ **确定配色方案** - 从3个TabBar配色中选择一个
2. ⬜ **更新TabBar** - 应用到app.json
3. ⬜ **创建全局样式文件** - styles/common.wxss
4. ⬜ **逐页优化** - 按页面优先级应用新样式
5. ⬜ **统一图标** - 更新图标风格一致
6. ⬜ **微调细节** - 根据实际效果调整

---

## 当前状态

- [ ] TabBar配色待确定
- [ ] 全局样式文件待创建
- [ ] 首页待优化
- [ ] 宠物列表页待优化
- [ ] 订单页待优化
- [ ] 我的页待优化
