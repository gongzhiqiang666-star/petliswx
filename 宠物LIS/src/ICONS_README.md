# 图标下载功能使用说明

## 功能概述

本项目已集成图标下载中心，可以查看、管理和导出项目中使用的所有 Lucide React 图标，**完全符合微信小程序TabBar图标规范**。

## 访问路径

**我的页面** → **图标下载** → 图标下载中心

## 主要功能

### 1. 图标浏览
- 📊 **统计面板**：显示图标总数、已选择数量、分类数和当前显示数
- 🔍 **搜索功能**：通过图标名称快速查找
- 🏷️ **分类筛选**：按功能分类浏览（导航、操作、功能、状态等）
- 📱 **网格展示**：清晰的卡片式图标展示

### 2. 图标选择
- ✅ **单选/多选**：点击图标卡片进行选择
- 🔘 **全选功能**：一键选择当前筛选结果的所有图标
- 💫 **视觉反馈**：选中状态有明显的视觉提示

### 3. 下载功能

#### 🎯 下载微信小程序TabBar图标（推荐）
**一键下载底部导航栏所有图标，完全符合微信小程序规范**

**规格说明：**
- 📏 尺寸：**81px × 81px**（微信小程序官方标准）
- 🎨 格式：PNG 透明背景
- ✏️ 描边：2.5px
- 🎨 未选中色：**#8C8C8D**（微信标准灰色）
- 🎨 选中色：**#615FFF**（主题紫色）

**下载文件（8个PNG）：**
```
home.png           // 首页-未选中
home-active.png    // 首页-选中
pet.png            // 宠物-未选中
pet-active.png     // 宠物-选中
order.png          // 订单-未选中
order-active.png   // 订单-选中
profile.png        // 我的-未选中
profile-active.png // 我的-选中
```

#### 下载选中图标
- 选择任意图标后批量下载
- 尺寸：81px × 81px（符合微信小程序规范）
- 格式：PNG 透明背景
- 文件名：小写字母+下划线格式

#### 下载完整列表
- 导出所有 35 个项目图标
- 尺寸：81px × 81px（符合微信小程序规范）
- 格式：PNG 透明背景

## 微信小程序TabBar配置示例

下载图标后，在微信小程序中的配置方式：

```json
{
  "tabBar": {
    "color": "#8C8C8D",
    "selectedColor": "#615FFF",
    "backgroundColor": "#ffffff",
    "borderStyle": "white",
    "list": [
      {
        "pagePath": "pages/home/index",
        "text": "首页",
        "iconPath": "images/tabbar/home.png",
        "selectedIconPath": "images/tabbar/home-active.png"
      },
      {
        "pagePath": "pages/pet/index",
        "text": "宠物",
        "iconPath": "images/tabbar/pet.png",
        "selectedIconPath": "images/tabbar/pet-active.png"
      },
      {
        "pagePath": "pages/order/index",
        "text": "订单",
        "iconPath": "images/tabbar/order.png",
        "selectedIconPath": "images/tabbar/order-active.png"
      },
      {
        "pagePath": "pages/profile/index",
        "text": "我的",
        "iconPath": "images/tabbar/profile.png",
        "selectedIconPath": "images/tabbar/profile-active.png"
      }
    ]
  }
}
```

## 项目使用的图标分类

### 导航类图标（8个）
- Home, PawPrint, ShoppingBag, User
- ArrowLeft, ArrowRight, ChevronRight, ChevronDown

### 操作类图标（7个）
- Share2, Plus, Download, Edit2, Trash2, X, LogOut

### 功能类图标（11个）
- Calendar, List, Clock, MapPin, Search, Filter
- Bell, Settings, HelpCircle, Shield, Activity

### 状态类图标（4个）
- Circle, CheckCircle, Check, AlertTriangle

### 联系类图标（2个）
- Phone, Mail

### 文档类图标（1个）
- FileText

### 信息类图标（1个）
- Building2

### 装饰类图标（1个）
- Sparkles

## 使用示例

### 单个图标导入（React）
```tsx
import { Home } from 'lucide-react';

function MyComponent() {
  return <Home className="w-6 h-6" />;
}
```

### 批量图标导入（React）
```tsx
import { 
  Home, 
  PawPrint, 
  Calendar,
  CheckCircle 
} from 'lucide-react';
```

## 技术规范

### 微信小程序TabBar图标规范 ✅

| 项目 | 规格 | 说明 |
|------|------|------|
| 尺寸 | 81px × 81px | 微信官方推荐标准尺寸 |
| 格式 | PNG | 透明背景 |
| 描边 | 2.5px | 清晰度最佳 |
| 未选中色 | #8C8C8D | 微信标准灰色 |
| 选中色 | #615FFF | 主题紫色 |
| 文件命名 | xxx.png / xxx-active.png | 小程序规范 |

### 图标库信息

- **图标库**：Lucide React (https://lucide.dev)
- **图标总数**：35个
- **分类数量**：8个
- **文件格式**：PNG（透明背景）
- **导出尺寸**：81px × 81px

## 使用流程

### 快速下载TabBar图标

```
1. 进入"我的"页面
2. 点击"图标下载"
3. 点击紫色按钮"下载微信小程序TabBar图标"
4. 确认下载
5. 获得8个PNG文件，可直接用于小程序开发
```

### 自定义下载

```
1. 使用搜索或分类筛选图标
2. 点击图标卡片进行多选
3. 点击底部"下载选中图标"按钮
4. 获得81px × 81px的PNG文件
```

## 注意事项

1. ✅ 所有图标均来自 Lucide React 官方库
2. ✅ 完全符合微信小程序TabBar图标规范
3. ✅ 下载的PNG文件包含透明背景
4. ✅ TabBar图标已按微信标准配置颜色
5. ✅ 文件命名遵循微信小程序规范
6. ⚠️ 图标样式可通过 className 自定义（React环境）
7. ⚠️ 微信小程序中直接使用PNG文件，无需导入React组件

## 文件目录建议

下载后建议的微信小程序文件目录结构：

```
miniprogram/
├── images/
│   └── tabbar/
│       ├── home.png
│       ├── home-active.png
│       ├── pet.png
│       ├── pet-active.png
│       ├── order.png
│       ├── order-active.png
│       ├── profile.png
│       └── profile-active.png
├── pages/
│   ├── home/
│   ├── pet/
│   ├── order/
│   └── profile/
└── app.json
```

## 更新日志

- **2024-12-25 v2.0**：完全适配微信小程序规范
  - ✅ 尺寸统一为 81px × 81px
  - ✅ 使用微信标准颜色（#8C8C8D / #615FFF）
  - ✅ 文件命名符合小程序规范
  - ✅ 新增一键下载TabBar图标功能
  - ✅ 透明背景PNG格式
  
- **2024-12-25 v1.0**：初始版本，支持35个图标的浏览和导出

## 技术支持

如有问题，请检查：
- 图标是否成功下载到本地
- PNG文件是否为透明背景
- 文件尺寸是否为81px × 81px
- 颜色是否符合预期

---

**宠物检测预约系统 · 图标下载中心**  
专为微信小程序开发优化 🎨✨
