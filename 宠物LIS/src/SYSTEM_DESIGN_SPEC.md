# 宠物检测预约和报告管理系统 - 详细设计规格书

> **版本**: v2.0  
> **更新日期**: 2024-12-26  
> **设计规范**: 微信小程序标准 / 移动端优先  
> **最大宽度**: 448px  
> **主题色**: 紫色渐变 (#6366F1 → #8B5CF6)  
> **字体**: PingFang SC

---

## 目录

1. [系统架构](#系统架构)
2. [数据库字段映射](#数据库字段映射)
3. [页面详细设计](#页面详细设计)
4. [组件库](#组件库)
5. [状态管理](#状态管理)
6. [验证规则](#验证规则)
7. [API接口](#api接口)

---

## 系统架构

### 技术栈

| 层级 | 技术选型 |
|------|---------|
| 前端框架 | React 18+ with TypeScript |
| 样式方案 | Tailwind CSS v4.0 |
| 图标库 | Lucide React (35个图标) |
| 状态管理 | React Hooks (useState, useEffect) |
| 路由管理 | 自定义页面导航 |

### 页面结构树

```
App.tsx (根组件)
├── HomePage (首页)
├── PetsPage (宠物管理)
│   ├── PetAddPage (添加宠物)
│   ├── PetEditPage (编辑宠物)
│   ├── PetDetailPage (宠物详情)
│   └── PetHealthPage (健康档案)
├── OrdersPage (订单管理)
│   ├── OrderDetailPage (订单详情)
│   ├── OrderSelectPetPage (选择宠物)
│   ├── OrderAddPetPage (新增宠物)
│   ├── OrderStep1Page (基本信息)
│   └── OrderStep2Page (选择项目)
├── ReportsPage (报告管理)
│   └── ReportViewPage (报告详情)
├── MinePage (我的)
│   ├── IconDownloadPage (图标下载)
│   └── IconExportPage (图标导出)
└── BottomNav (底部导航)
```

---

## 数据库字段映射

### 1. 宠物信息表 (tbl_InfoAnimal)

| 前端字段 | 数据库字段 | 类型 | 必填 | 说明 |
|---------|-----------|------|------|------|
| id | InfoAnimalId | string | ✓ | 宠物唯一标识 (UUID) |
| name | AnimalName | string | ✓ | 宠物名称 (1-20字符) |
| type | AnimalTypeId | string | ✓ | 宠物类型ID (关联字典表) |
| typeName | - | string | - | 宠物类型名称 (前端展示) |
| gender | DictSexId | string | ✓ | 性别ID (关联字典表) |
| genderName | - | string | - | 性别名称 (前端展示) |
| birthday | Birthday | string | ✓ | 出生日期 (YYYY-MM-DD) |
| age | - | string | - | 年龄 (前端计算) |
| chipNo | ChipNO | string | ✗ | 芯片号 (狂犬检测必填) |
| breed | Breed | string | ✗ | 品种 |
| weight | Weight | number | ✗ | 体重 (kg) |
| color | Color | string | ✗ | 毛色 |
| sterilized | IsSterilized | boolean | ✗ | 是否绝育 |
| createdAt | CreateTime | datetime | ✓ | 创建时间 |
| updatedAt | UpdateTime | datetime | ✓ | 更新时间 |

**宠物类型字典 (DictAnimalType):**
- `1001`: 犬
- `1002`: 猫
- `1003`: 其他

**性别字典 (DictSex):**
- `2001`: 公
- `2002`: 母
- `2003`: 未知

---

### 2. 订单信息表 (tbl_OrderInfo)

| 前端字段 | 数据库字段 | 类型 | 必填 | 说明 |
|---------|-----------|------|------|------|
| id | OrderId | string | ✓ | 订单唯一标识 (UUID) |
| orderNo | OrderNO | string | ✓ | 订单号 (系统生成) |
| petId | InfoAnimalId | string | ✓ | 关联宠物ID |
| petName | - | string | - | 宠物名称 (冗余) |
| status | OrderStatus | string | ✓ | 订单状态 |
| statusName | - | string | - | 状态名称 (前端展示) |
| testItems | TestItemIds | string[] | ✓ | 检测项目ID数组 |
| testItemsJson | TestItemsJson | json | ✓ | 检测项目详情JSON |
| appointmentDate | AppointmentDate | string | ✓ | 预约日期 (YYYY-MM-DD) |
| appointmentTime | AppointmentTime | string | ✓ | 预约时间 (HH:mm) |
| contactName | ContactName | string | ✓ | 联系人姓名 |
| contactPhone | ContactPhone | string | ✓ | 联系电话 (11位) |
| address | Address | string | ✓ | 详细地址 |
| province | Province | string | ✓ | 省份 |
| city | City | string | ✓ | 城市 |
| district | District | string | ✓ | 区县 |
| expressNo | ExpressNO | string | ✗ | 快递单号 (狂犬检测必填) |
| totalAmount | TotalAmount | number | ✓ | 订单总金额 |
| remark | Remark | string | ✗ | 备注说明 |
| createdAt | CreateTime | datetime | ✓ | 创建时间 |
| updatedAt | UpdateTime | datetime | ✓ | 更新时间 |

**订单状态枚举 (OrderStatus):**
- `pending`: 待确认
- `confirmed`: 已确认
- `processing`: 检测中
- `completed`: 已完成
- `cancelled`: 已取消

---

### 3. 检测项目表 (tbl_TestItem)

| 前端字段 | 数据库字段 | 类型 | 必填 | 说明 |
|---------|-----------|------|------|------|
| id | TestItemId | string | ✓ | 项目唯一标识 |
| code | ItemCode | string | ✓ | 项目代码 |
| name | ItemName | string | ✓ | 项目名称 |
| category | CategoryId | string | ✓ | 项目分类ID |
| categoryName | - | string | - | 分类名称 (前端展示) |
| price | Price | number | ✓ | 项目价格 |
| duration | Duration | number | ✗ | 检测时长 (天) |
| description | Description | string | ✗ | 项目描述 |
| isRabies | IsRabiesTest | boolean | ✓ | 是否狂犬检测 |
| isActive | IsActive | boolean | ✓ | 是否启用 |

**检测项目分类:**
- `血常规检查`: 包括白细胞、红细胞、血小板等
- `生化检查`: 包括肝功能、肾功能等
- `传染病筛查`: 包括犬瘟热、细小病毒等
- `狂犬病检测`: 特殊项目，需要芯片号和快递单号

**当前系统项目清单:**
```json
[
  {
    "id": "1",
    "name": "血常规检查",
    "price": 120,
    "category": "血常规检查",
    "isRabies": false
  },
  {
    "id": "2",
    "name": "生化全套",
    "price": 380,
    "category": "生化检查",
    "isRabies": false
  },
  {
    "id": "3",
    "name": "传染病四项",
    "price": 280,
    "category": "传染病筛查",
    "isRabies": false
  },
  {
    "id": "4",
    "name": "狂犬病抗体检测",
    "price": 350,
    "category": "狂犬病检测",
    "isRabies": true
  }
]
```

---

### 4. 报告信息表 (tbl_ReportInfo)

| 前端字段 | 数据库字段 | 类型 | 必填 | 说明 |
|---------|-----------|------|------|------|
| id | ReportId | string | ✓ | 报告唯一标识 |
| reportNo | ReportNO | string | ✓ | 报告编号 |
| orderId | OrderId | string | ✓ | 关联订单ID |
| petId | InfoAnimalId | string | ✓ | 关联宠物ID |
| petName | - | string | - | 宠物名称 (冗余) |
| testItem | TestItemName | string | ✓ | 检测项目名称 |
| testDate | TestDate | string | ✓ | 检测日期 |
| reportDate | ReportDate | string | ✓ | 报告日期 |
| status | ReportStatus | string | ✓ | 报告状态 |
| result | Result | string | ✓ | 检测结果 |
| resultData | ResultData | json | ✓ | 详细数据JSON |
| conclusion | Conclusion | string | ✗ | 检测结论 |
| suggestion | Suggestion | string | ✗ | 建议 |
| doctorName | DoctorName | string | ✗ | 检测医生 |
| reviewerName | ReviewerName | string | ✗ | 审核医生 |
| pdfUrl | PdfUrl | string | ✗ | PDF报告URL |
| createdAt | CreateTime | datetime | ✓ | 创建时间 |

**报告状态枚举 (ReportStatus):**
- `processing`: 检测中
- `completed`: 已完成
- `abnormal`: 异常

---

### 5. 用户信息表 (tbl_UserInfo)

| 前端字段 | 数据库字段 | 类型 | 必填 | 说明 |
|---------|-----------|------|------|------|
| id | UserId | string | ✓ | 用户唯一标识 |
| openId | OpenId | string | ✓ | 微信OpenID |
| nickName | NickName | string | ✗ | 昵称 |
| phone | Phone | string | ✗ | 手机号 |
| realName | RealName | string | ✗ | 真实姓名 |
| province | Province | string | ✗ | 省份 |
| city | City | string | ✗ | 城市 |
| district | District | string | ✗ | 区县 |
| address | Address | string | ✗ | 详细地址 |
| createdAt | CreateTime | datetime | ✓ | 创建时间 |
| updatedAt | UpdateTime | datetime | ✓ | 更新时间 |

---

## 页面详细设计

### 1. 首页 (HomePage)

**路由**: `/` 或 `home`  
**文件**: `/pages/HomePage.tsx`

#### 功能模块

##### 1.1 顶部欢迎区
```typescript
// 字段定义
interface WelcomeSection {
  userName: string;        // 用户名称，默认"宠物主人"
  notificationCount: number; // 未读通知数量
}
```

**UI组件:**
- 用户头像区域: 紫色Sparkles图标
- 欢迎文字: "欢迎回来"
- 通知按钮: Bell图标 + 红点徽标

##### 1.2 快捷操作卡片

**三个主要操作按钮:**

```typescript
// 按钮配置
const quickActions = [
  {
    id: 'booking',
    title: '预约检测',
    icon: Calendar,
    gradient: 'from-indigo-500 to-purple-600',
    navigation: 'order-select-pet'
  },
  {
    id: 'add-pet',
    title: '宠物建档',
    icon: Sparkles,
    gradient: 'from-violet-500 to-fuchsia-600',
    navigation: 'pet-add-from-home'
  },
  {
    id: 'reports',
    title: '查看报告',
    icon: FileText,
    gradient: 'from-emerald-500 to-teal-600',
    navigation: 'reports'
  }
];
```

**样式规范:**
- 卡片圆角: 24px (rounded-3xl)
- 按钮圆角: 16px (rounded-2xl)
- 图标尺寸: 56px容器 + 28px图标
- 内边距: 24px (p-6)
- 悬停效果: shadow-lg + scale-110 (图标)

##### 1.3 最近动态列表

```typescript
// 数据结构
interface Activity {
  id: string;
  type: 'report' | 'order' | 'appointment';
  title: string;          // 动态标题
  description: string;    // 动态描述
  time: string;          // 相对时间 (如: 2小时前)
  relatedId?: string;    // 关联ID (订单/报告)
}
```

**示例数据:**
```json
[
  {
    "id": "1",
    "type": "report",
    "title": "小花的血常规检查报告已出",
    "description": "详细报告请查看",
    "time": "2小时前"
  },
  {
    "id": "2",
    "type": "order",
    "title": "小黑的检测预约已确认",
    "description": "预约详情请查看",
    "time": "1天前"
  },
  {
    "id": "3",
    "type": "appointment",
    "title": "喵喵的年度体检即将到期",
    "description": "请尽快预约",
    "time": "3天前"
  }
]
```

**UI规范:**
- 卡片圆角: 16px (rounded-2xl)
- 图标容器: 44px (w-11 h-11)
- 图标尺寸: 20px (w-5 h-5)
- 颜色映射:
  - report: emerald (绿色)
  - order: indigo (蓝紫色)
  - appointment: orange (橙色)

#### 交互逻辑

1. **点击"预约检测"**
   - 导航到: `order-select-pet`
   - 进入订单创建流程

2. **点击"宠物建档"**
   - 导航到: `pet-add-from-home`
   - 进入宠物添加流程

3. **点击"查看报告"**
   - 导航到: `reports`
   - 查看所有报告列表

4. **点击动态卡片**
   - 根据type跳转到对应详情页

---

### 2. 宠物管理页 (PetsPage)

**路由**: `pets`  
**文件**: `/pages/PetsPage.tsx`

#### 功能模块

##### 2.1 搜索和筛选栏

```typescript
// 状态管理
interface PetsPageState {
  searchQuery: string;      // 搜索关键词
  selectedType: string;     // 选中的宠物类型 ('all' | '犬' | '猫' | '其他')
  pets: Pet[];             // 宠物列表
}
```

**UI组件:**
- 搜索框: Search图标 + 输入框
- 筛选标签: 全部 / 犬 / 猫 / 其他
- 添加按钮: 右上角Plus图标

##### 2.2 宠物列表

```typescript
// 宠物卡片数据
interface PetCard {
  id: string;              // InfoAnimalId
  name: string;            // AnimalName
  type: string;            // 宠物类型名称
  gender: string;          // 性别名称
  age: string;             // 计算年龄
  chipNo?: string;         // ChipNO (可选)
  breed?: string;          // Breed (可选)
}
```

**卡片样式:**
- 圆角: 16px (rounded-2xl)
- 图标容器: 64px (w-16 h-16)
- 右箭头: ChevronRight图标
- 间距: 12px gap

**显示逻辑:**
```typescript
// 年龄计算
function calculateAge(birthday: string): string {
  const birth = new Date(birthday);
  const now = new Date();
  const years = now.getFullYear() - birth.getFullYear();
  const months = now.getMonth() - birth.getMonth();
  
  if (years > 0) {
    return `${years}岁`;
  } else if (months > 0) {
    return `${months}个月`;
  } else {
    return '未满月';
  }
}

// 筛选逻辑
function filterPets(pets: Pet[], type: string, query: string): Pet[] {
  return pets.filter(pet => {
    const matchType = type === 'all' || pet.type === type;
    const matchQuery = query === '' || 
                      pet.name.includes(query) || 
                      pet.breed?.includes(query);
    return matchType && matchQuery;
  });
}
```

##### 2.3 空状态

**显示条件:** `pets.length === 0`

**内容:**
- 图标: PawPrint (灰色)
- 文字: "还没有宠物档案"
- 提示: "点击右上角添加按钮，为您的宠物建档吧"

#### 交互逻辑

1. **点击搜索框**
   - 聚焦输入框
   - 实时筛选列表

2. **点击类型标签**
   - 切换选中状态
   - 更新宠物列表

3. **点击添加按钮**
   - 导航到: `pet-add`

4. **点击宠物卡片**
   - 导航到: `pet-detail`
   - 传递petId参数

---

### 3. 宠物添加页 (PetAddPage)

**路由**: `pet-add` / `pet-add-from-home`  
**文件**: `/pages/PetAddPage.tsx`

#### 表单字段

```typescript
// 表单数据结构
interface PetFormData {
  // 基本信息
  name: string;              // 宠物名称 (必填, 1-20字符)
  type: string;              // 宠物类型ID (必填, 下拉选择)
  gender: string;            // 性别ID (必填, 下拉选择)
  birthday: string;          // 出生日期 (必填, YYYY-MM-DD)
  
  // 详细信息
  chipNo: string;            // 芯片号 (选填, 数字/字母)
  breed: string;             // 品种 (选填, 文本)
  weight: string;            // 体重 (选填, 数字, kg)
  color: string;             // 毛色 (选填, 文本)
  sterilized: boolean;       // 是否绝育 (选填, 开关)
}

// 初始值
const initialFormData: PetFormData = {
  name: '',
  type: '',
  gender: '',
  birthday: '',
  chipNo: '',
  breed: '',
  weight: '',
  color: '',
  sterilized: false
};
```

#### 表单验证规则

```typescript
// 验证函数
function validatePetForm(data: PetFormData): ValidationResult {
  const errors: string[] = [];
  
  // 宠物名称
  if (!data.name.trim()) {
    errors.push('请输入宠物名称');
  } else if (data.name.length > 20) {
    errors.push('宠物名称不能超过20个字符');
  }
  
  // 宠物类型
  if (!data.type) {
    errors.push('请选择宠物类型');
  }
  
  // 性别
  if (!data.gender) {
    errors.push('请选择性别');
  }
  
  // 出生日期
  if (!data.birthday) {
    errors.push('请选择出生日期');
  } else {
    const birthday = new Date(data.birthday);
    const today = new Date();
    if (birthday > today) {
      errors.push('出生日期不能晚于今天');
    }
  }
  
  // 体重 (如果填写)
  if (data.weight && (isNaN(Number(data.weight)) || Number(data.weight) <= 0)) {
    errors.push('请输入有效的体重');
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
}
```

#### UI组件

##### 3.1 表单区域

**基本信息部分:**
```html
<section title="基本信息">
  <FormField label="宠物名称*" required>
    <input type="text" maxLength="20" placeholder="请输入宠物名称" />
  </FormField>
  
  <FormField label="宠物类型*" required>
    <select>
      <option value="">请选择</option>
      <option value="1001">犬</option>
      <option value="1002">猫</option>
      <option value="1003">其他</option>
    </select>
  </FormField>
  
  <FormField label="性别*" required>
    <RadioGroup>
      <option value="2001">公</option>
      <option value="2002">母</option>
      <option value="2003">未知</option>
    </RadioGroup>
  </FormField>
  
  <FormField label="出生日期*" required>
    <input type="date" />
  </FormField>
</section>
```

**详细信息部分:**
```html
<section title="详细信息（选填）">
  <FormField label="芯片号">
    <input type="text" placeholder="如有芯片请填写" />
  </FormField>
  
  <FormField label="品种">
    <input type="text" placeholder="如: 金毛、英短" />
  </FormField>
  
  <FormField label="体重">
    <input type="number" step="0.1" placeholder="单位: kg" />
  </FormField>
  
  <FormField label="毛色">
    <input type="text" placeholder="如: 金黄色、黑白花" />
  </FormField>
  
  <FormField label="是否绝育">
    <Toggle />
  </FormField>
</section>
```

##### 3.2 底部按钮

```typescript
// 按钮状态
interface ButtonState {
  loading: boolean;    // 提交中
  disabled: boolean;   // 禁用状态 (表单未填写完整)
}
```

**两种按钮:**
1. **取消** - 灰色背景，返回上一页
2. **保存** - 紫色渐变，提交表单

#### 提交流程

```typescript
async function handleSubmit(formData: PetFormData) {
  // 1. 验证表单
  const validation = validatePetForm(formData);
  if (!validation.isValid) {
    alert(validation.errors.join('\n'));
    return;
  }
  
  // 2. 构建提交数据
  const submitData = {
    InfoAnimalId: generateUUID(),
    AnimalName: formData.name,
    AnimalTypeId: formData.type,
    DictSexId: formData.gender,
    Birthday: formData.birthday,
    ChipNO: formData.chipNo || null,
    Breed: formData.breed || null,
    Weight: formData.weight ? parseFloat(formData.weight) : null,
    Color: formData.color || null,
    IsSterilized: formData.sterilized,
    CreateTime: new Date().toISOString(),
    UpdateTime: new Date().toISOString()
  };
  
  // 3. 调用API (模拟)
  try {
    setLoading(true);
    await api.createPet(submitData);
    
    // 4. 成功反馈
    alert('宠物档案添加成功！');
    
    // 5. 返回列表页
    onNavigate('pets');
  } catch (error) {
    alert('添加失败，请重试');
  } finally {
    setLoading(false);
  }
}
```

---

### 4. 宠物编辑页 (PetEditPage)

**路由**: `pet-edit`  
**文件**: `/pages/PetEditPage.tsx`  
**参数**: `petId: string`

#### 数据加载

```typescript
// 页面初始化
useEffect(() => {
  async function loadPetData() {
    try {
      // 1. 根据petId获取宠物数据
      const pet = await api.getPetById(petId);
      
      // 2. 填充表单
      setFormData({
        name: pet.AnimalName,
        type: pet.AnimalTypeId,
        gender: pet.DictSexId,
        birthday: pet.Birthday,
        chipNo: pet.ChipNO || '',
        breed: pet.Breed || '',
        weight: pet.Weight ? pet.Weight.toString() : '',
        color: pet.Color || '',
        sterilized: pet.IsSterilized || false
      });
    } catch (error) {
      alert('加载失败');
      onNavigate('pets');
    }
  }
  
  loadPetData();
}, [petId]);
```

#### 更新流程

```typescript
async function handleUpdate(formData: PetFormData) {
  // 1. 验证表单 (同添加页)
  const validation = validatePetForm(formData);
  if (!validation.isValid) {
    alert(validation.errors.join('\n'));
    return;
  }
  
  // 2. 构建更新数据
  const updateData = {
    InfoAnimalId: petId,  // 保持原ID
    AnimalName: formData.name,
    AnimalTypeId: formData.type,
    DictSexId: formData.gender,
    Birthday: formData.birthday,
    ChipNO: formData.chipNo || null,
    Breed: formData.breed || null,
    Weight: formData.weight ? parseFloat(formData.weight) : null,
    Color: formData.color || null,
    IsSterilized: formData.sterilized,
    UpdateTime: new Date().toISOString()
  };
  
  // 3. 调用更新API
  try {
    setLoading(true);
    await api.updatePet(petId, updateData);
    
    alert('宠物信息更新成功！');
    onNavigate('pet-detail', { petId });
  } catch (error) {
    alert('更新失败，请重试');
  } finally {
    setLoading(false);
  }
}
```

**注意事项:**
- 表单结构与PetAddPage完全一致
- 增加"删除宠物"按钮 (红色，底部)
- 删除需要二次确认

---

### 5. 宠物详情页 (PetDetailPage)

**路由**: `pet-detail`  
**文件**: `/pages/PetDetailPage.tsx`  
**参数**: `petId: string`

#### 页面结构

```typescript
// 详情数据
interface PetDetail {
  // 基本信息
  id: string;
  name: string;
  type: string;
  gender: string;
  age: string;
  birthday: string;
  
  // 详细信息
  chipNo?: string;
  breed?: string;
  weight?: number;
  color?: string;
  sterilized: boolean;
  
  // 统计信息
  totalOrders: number;      // 总订单数
  totalReports: number;     // 总报告数
  lastCheckDate?: string;   // 最后检测日期
}
```

#### UI模块

##### 5.1 宠物信息卡片

```html
<div class="pet-info-card">
  <!-- 头部 -->
  <div class="header">
    <div class="icon-container">
      <PawPrint icon />
    </div>
    <div class="info">
      <h1>{name}</h1>
      <div class="meta">
        <span>{type}</span>
        <span>·</span>
        <span>{gender}</span>
        <span>·</span>
        <span>{age}</span>
      </div>
    </div>
    <button class="edit-btn" onClick={() => onNavigate('pet-edit', { petId })}>
      <Edit2 icon />
    </button>
  </div>
  
  <!-- 详细信息网格 -->
  <div class="details-grid">
    <InfoItem label="出生日期" value={birthday} />
    <InfoItem label="品种" value={breed || '-'} />
    <InfoItem label="体重" value={weight ? `${weight}kg` : '-'} />
    <InfoItem label="毛色" value={color || '-'} />
    <InfoItem label="芯片号" value={chipNo || '未录入'} />
    <InfoItem label="绝育状态" value={sterilized ? '已绝育' : '未绝育'} />
  </div>
</div>
```

##### 5.2 快捷操作

```typescript
const quickActions = [
  {
    id: 'book',
    title: '预约检测',
    icon: Calendar,
    color: 'indigo',
    onClick: () => {
      // 直接进入订单流程，预选此宠物
      onNavigate('order-step1', { petId });
    }
  },
  {
    id: 'health',
    title: '健康档案',
    icon: Activity,
    color: 'emerald',
    onClick: () => {
      onNavigate('pet-health', { petId });
    }
  }
];
```

##### 5.3 统计信息

```html
<div class="stats-section">
  <h3>统计信息</h3>
  <div class="stats-grid">
    <StatCard
      icon={ShoppingBag}
      label="总订单"
      value={totalOrders}
      color="indigo"
    />
    <StatCard
      icon={FileText}
      label="检测报告"
      value={totalReports}
      color="emerald"
    />
  </div>
  <div class="last-check">
    <Clock icon />
    <span>最后检测: {lastCheckDate || '暂无记录'}</span>
  </div>
</div>
```

##### 5.4 相关订单/报告列表

```typescript
// 最近订单 (显示最多3条)
interface RecentOrder {
  id: string;
  orderNo: string;
  testItem: string;
  date: string;
  status: string;
}

// 最近报告 (显示最多3条)
interface RecentReport {
  id: string;
  reportNo: string;
  testItem: string;
  date: string;
  result: string;
}
```

**查看更多:**
- 点击"查看全部订单" → 跳转到OrdersPage，筛选当前宠物
- 点击"查看全部报告" → 跳转到ReportsPage，筛选当前宠物

---

### 6. 宠物健康档案页 (PetHealthPage)

**路由**: `pet-health`  
**文件**: `/pages/PetHealthPage.tsx`  
**参数**: `petId: string`

#### 功能模块

##### 6.1 健康总览

```typescript
interface HealthOverview {
  petId: string;
  petName: string;
  
  // 疫苗接种记录
  vaccinations: Vaccination[];
  
  // 体检记录
  checkups: Checkup[];
  
  // 用药记录
  medications: Medication[];
  
  // 异常记录
  abnormalReports: Report[];
}

interface Vaccination {
  id: string;
  name: string;           // 疫苗名称
  date: string;           // 接种日期
  nextDate?: string;      // 下次接种日期
  batchNo?: string;       // 批次号
}

interface Checkup {
  id: string;
  date: string;
  items: string[];        // 检测项目
  result: string;         // 总体结果
  reportId?: string;      // 关联报告ID
}

interface Medication {
  id: string;
  name: string;           // 药品名称
  purpose: string;        // 用途
  startDate: string;      // 开始日期
  endDate?: string;       // 结束日期
  dosage: string;         // 剂量
}
```

##### 6.2 健康时间轴

```html
<div class="health-timeline">
  {healthEvents.map(event => (
    <TimelineItem
      date={event.date}
      type={event.type}
      title={event.title}
      description={event.description}
      status={event.status}
    />
  ))}
</div>
```

**时间轴事件类型:**
- `vaccination`: 疫苗接种
- `checkup`: 健康体检
- `medication`: 用药记录
- `abnormal`: 异常结果

##### 6.3 健康提醒

```typescript
interface HealthReminder {
  id: string;
  type: 'vaccination' | 'checkup' | 'medication';
  title: string;
  dueDate: string;
  priority: 'high' | 'medium' | 'low';
  message: string;
}

// 示例
const reminders: HealthReminder[] = [
  {
    id: '1',
    type: 'vaccination',
    title: '狂犬疫苗即将到期',
    dueDate: '2025-01-15',
    priority: 'high',
    message: '距离下次接种还有20天，请及时预约'
  },
  {
    id: '2',
    type: 'checkup',
    title: '年度体检提醒',
    dueDate: '2025-02-01',
    priority: 'medium',
    message: '建议每年进行一次全面体检'
  }
];
```

**优先级颜色:**
- `high`: 红色 (red-500)
- `medium`: 橙色 (orange-500)
- `low`: 蓝色 (blue-500)

---

### 7. 订单列表页 (OrdersPage)

**路由**: `orders`  
**文件**: `/pages/OrdersPage.tsx`

#### 功能模块

##### 7.1 状态筛选标签

```typescript
// 状态配置
const statusTabs = [
  { id: 'all', label: '全部', count: 0 },
  { id: 'pending', label: '待确认', count: 0 },
  { id: 'confirmed', label: '已确认', count: 0 },
  { id: 'processing', label: '检测中', count: 0 },
  { id: 'completed', label: '已完成', count: 0 }
];

// 状态管理
interface OrdersPageState {
  selectedStatus: string;   // 当前选中状态
  orders: Order[];          // 订单列表
}
```

**UI实现:**
```html
<div class="status-tabs">
  {statusTabs.map(tab => (
    <button
      class={selectedStatus === tab.id ? 'active' : ''}
      onClick={() => setSelectedStatus(tab.id)}
    >
      {tab.label}
      {tab.count > 0 && <span class="badge">{tab.count}</span>}
    </button>
  ))}
</div>
```

##### 7.2 订单卡片

```typescript
// 订单卡片数据
interface OrderCardData {
  id: string;                // OrderId
  orderNo: string;           // 订单号
  petName: string;           // 宠物名称
  testItems: string[];       // 检测项目名称数组
  appointmentDate: string;   // 预约日期
  appointmentTime: string;   // 预约时间
  status: string;            // 订单状态
  statusName: string;        // 状态名称
  totalAmount: number;       // 总金额
  createdAt: string;         // 创建时间
}
```

**卡片布局:**
```html
<div class="order-card">
  <!-- 顶部: 订单号 + 状态 -->
  <div class="header">
    <span class="order-no">订单号: {orderNo}</span>
    <span class="status-badge status-{status}">{statusName}</span>
  </div>
  
  <!-- 宠物信息 -->
  <div class="pet-info">
    <PawPrint icon />
    <span>{petName}</span>
  </div>
  
  <!-- 检测项目 -->
  <div class="test-items">
    <Calendar icon />
    <span>{testItems.join('、')}</span>
  </div>
  
  <!-- 预约时间 -->
  <div class="appointment">
    <Clock icon />
    <span>{appointmentDate} {appointmentTime}</span>
  </div>
  
  <!-- 底部: 金额 + 查看详情 -->
  <div class="footer">
    <div class="amount">¥{totalAmount}</div>
    <button onClick={() => onNavigate('order-detail', { orderId: id })}>
      查看详情
      <ChevronRight icon />
    </button>
  </div>
</div>
```

**状态徽章颜色:**
```css
.status-pending { background: orange; }
.status-confirmed { background: blue; }
.status-processing { background: indigo; }
.status-completed { background: green; }
.status-cancelled { background: gray; }
```

##### 7.3 列表筛选

```typescript
// 筛选函数
function filterOrders(orders: Order[], status: string): Order[] {
  if (status === 'all') {
    return orders;
  }
  return orders.filter(order => order.status === status);
}

// 排序: 最新订单在前
function sortOrders(orders: Order[]): Order[] {
  return orders.sort((a, b) => 
    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );
}
```

##### 7.4 空状态

```html
<!-- 无订单 -->
<div class="empty-state">
  <ShoppingBag icon class="large-icon gray" />
  <p>还没有订单</p>
  <button onClick={() => onNavigate('order-select-pet')}>
    立即预约
  </button>
</div>

<!-- 筛选无结果 -->
<div class="empty-state">
  <Search icon class="large-icon gray" />
  <p>暂无{statusName}订单</p>
</div>
```

---

### 8. 订单详情页 (OrderDetailPage)

**路由**: `order-detail`  
**文件**: `/pages/OrderDetailPage.tsx`  
**参数**: `orderId: string`

#### 数据结构

```typescript
interface OrderDetail {
  // 订单基本信息
  id: string;
  orderNo: string;
  status: string;
  statusName: string;
  createdAt: string;
  
  // 宠物信息
  petId: string;
  petName: string;
  petType: string;
  petGender: string;
  petAge: string;
  
  // 检测项目
  testItems: TestItemDetail[];
  totalAmount: number;
  
  // 预约信息
  appointmentDate: string;
  appointmentTime: string;
  
  // 联系信息
  contactName: string;
  contactPhone: string;
  province: string;
  city: string;
  district: string;
  address: string;
  
  // 特殊信息 (狂犬检测)
  chipNo?: string;
  expressNo?: string;
  
  // 备注
  remark?: string;
  
  // 时间轴
  timeline: OrderTimeline[];
}

interface TestItemDetail {
  id: string;
  name: string;
  category: string;
  price: number;
  isRabies: boolean;
}

interface OrderTimeline {
  time: string;
  status: string;
  description: string;
}
```

#### UI模块

##### 8.1 订单状态卡片

```html
<div class="status-card gradient-{status}">
  <div class="status-icon">
    {status === 'pending' && <Clock />}
    {status === 'confirmed' && <CheckCircle />}
    {status === 'processing' && <Activity />}
    {status === 'completed' && <CheckCircle />}
  </div>
  <div class="status-info">
    <h2>{statusName}</h2>
    <p class="order-no">订单号: {orderNo}</p>
    <p class="create-time">创建时间: {createdAt}</p>
  </div>
</div>
```

##### 8.2 宠物信息

```html
<section title="宠物信息">
  <div class="pet-card">
    <div class="pet-icon">
      <PawPrint />
    </div>
    <div class="pet-details">
      <h3>{petName}</h3>
      <p>{petType} · {petGender} · {petAge}</p>
      {chipNo && <p>芯片号: {chipNo}</p>}
    </div>
    <button onClick={() => onNavigate('pet-detail', { petId })}>
      查看详情
    </button>
  </div>
</section>
```

##### 8.3 检测项目

```html
<section title="检测项目">
  {testItems.map(item => (
    <div class="item-row">
      <div class="item-info">
        <span class="item-name">{item.name}</span>
        {item.isRabies && <span class="badge-rabies">狂犬检测</span>}
        <span class="item-category">{item.category}</span>
      </div>
      <div class="item-price">¥{item.price}</div>
    </div>
  ))}
  <div class="total-row">
    <span>合计</span>
    <span class="total-amount">¥{totalAmount}</span>
  </div>
</section>
```

##### 8.4 预约信息

```html
<section title="预约信息">
  <InfoRow icon={Calendar} label="预约日期" value={appointmentDate} />
  <InfoRow icon={Clock} label="预约时间" value={appointmentTime} />
</section>
```

##### 8.5 联系信息

```html
<section title="联系信息">
  <InfoRow icon={User} label="联系人" value={contactName} />
  <InfoRow icon={Phone} label="联系电话" value={contactPhone} />
  <InfoRow 
    icon={MapPin} 
    label="地址" 
    value={`${province}${city}${district}\n${address}`} 
  />
</section>
```

##### 8.6 快递信息 (仅狂犬检测)

```html
{hasRabiesTest && (
  <section title="快递信息">
    <InfoRow icon={Package} label="快递单号" value={expressNo || '待填写'} />
    {!expressNo && (
      <button class="btn-outline">填写快递单号</button>
    )}
  </section>
)}
```

##### 8.7 订单时间轴

```html
<section title="订单进度">
  <div class="timeline">
    {timeline.map((event, index) => (
      <div class="timeline-item">
        <div class="timeline-dot active={index === 0}" />
        <div class="timeline-content">
          <div class="timeline-time">{event.time}</div>
          <div class="timeline-status">{event.status}</div>
          <div class="timeline-desc">{event.description}</div>
        </div>
      </div>
    ))}
  </div>
</section>
```

**时间轴示例:**
```typescript
const timeline: OrderTimeline[] = [
  {
    time: '2024-12-26 14:30',
    status: '订单已完成',
    description: '检测报告已生成，可在报告页面查看'
  },
  {
    time: '2024-12-25 10:00',
    status: '检测中',
    description: '样本已送检，预计1-2个工作日出结果'
  },
  {
    time: '2024-12-24 16:00',
    status: '订单已确认',
    description: '预约已确认，请按时送检'
  },
  {
    time: '2024-12-24 15:30',
    status: '订单已创建',
    description: '订单创建成功，等待确认'
  }
];
```

##### 8.8 底部操作按钮

```typescript
// 根据订单状态显示不同按钮
function getActionButtons(status: string) {
  switch (status) {
    case 'pending':
      return [
        { label: '取消订单', type: 'outline', color: 'gray' },
        { label: '联系客服', type: 'primary', color: 'indigo' }
      ];
    
    case 'confirmed':
      return [
        { label: '修改预约', type: 'outline', color: 'indigo' },
        { label: '查看须知', type: 'primary', color: 'indigo' }
      ];
    
    case 'processing':
      return [
        { label: '查看进度', type: 'primary', color: 'indigo' }
      ];
    
    case 'completed':
      return [
        { label: '查看报告', type: 'primary', color: 'emerald' },
        { label: '再次预约', type: 'outline', color: 'indigo' }
      ];
    
    default:
      return [];
  }
}
```

---

### 9. 订单创建流程

#### 9.1 选择宠物页 (OrderSelectPetPage)

**路由**: `order-select-pet`  
**文件**: `/pages/OrderSelectPetPage.tsx`

##### 功能说明

- 显示所有宠物列表
- 选择一个宠物进入订单流程
- 可快速添加新宠物

##### UI实现

```html
<div class="page">
  <PageHeader title="选择宠物" />
  
  <!-- 宠物列表 -->
  <div class="pet-list">
    {pets.map(pet => (
      <div 
        class="pet-card selectable"
        onClick={() => handleSelectPet(pet.id)}
      >
        <div class="pet-icon">
          <PawPrint />
        </div>
        <div class="pet-info">
          <h3>{pet.name}</h3>
          <p>{pet.type} · {pet.gender} · {pet.age}</p>
        </div>
        <ChevronRight />
      </div>
    ))}
  </div>
  
  <!-- 添加新宠物 -->
  <button 
    class="btn-add-pet"
    onClick={() => onNavigate('order-add-pet')}
  >
    <Plus icon />
    添加新宠物
  </button>
</div>
```

##### 交互逻辑

```typescript
function handleSelectPet(petId: string) {
  // 导航到订单步骤1，传递petId
  onNavigate('order-step1', { petId });
}
```

---

#### 9.2 订单添加宠物页 (OrderAddPetPage)

**路由**: `order-add-pet`  
**文件**: `/pages/OrderAddPetPage.tsx`

##### 功能说明

- 在订单流程中快速添加宠物
- 表单结构与PetAddPage相同
- 添加成功后自动选中并进入订单步骤1

##### 特殊逻辑

```typescript
async function handleSubmit(formData: PetFormData) {
  // 1. 创建宠物
  const newPet = await api.createPet(formData);
  
  // 2. 自动选中并进入订单流程
  onNavigate('order-step1', { petId: newPet.id });
}
```

---

#### 9.3 订单步骤1 - 基本信息 (OrderStep1Page / SendPro1)

**路由**: `order-step1`  
**文件**: `/pages/OrderStep1Page.tsx`  
**参数**: `petId: string`

##### 表单字段

```typescript
interface OrderStep1Data {
  // 预约信息
  appointmentDate: string;   // 预约日期 (必填, YYYY-MM-DD)
  appointmentTime: string;   // 预约时间 (必填, HH:mm)
  
  // 联系信息
  contactName: string;       // 联系人 (必填, 1-20字符)
  contactPhone: string;      // 联系电话 (必填, 11位手机号)
  
  // 地址信息
  province: string;          // 省份 (必填, 下拉选择)
  city: string;              // 城市 (必填, 下拉选择)
  district: string;          // 区县 (必填, 下拉选择)
  address: string;           // 详细地址 (必填, 5-100字符)
  
  // 备注
  remark: string;            // 备注 (选填, 最多200字符)
}
```

##### 验证规则

```typescript
function validateStep1(data: OrderStep1Data): ValidationResult {
  const errors: string[] = [];
  
  // 预约日期
  if (!data.appointmentDate) {
    errors.push('请选择预约日期');
  } else {
    const appointmentDate = new Date(data.appointmentDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    if (appointmentDate < today) {
      errors.push('预约日期不能早于今天');
    }
  }
  
  // 预约时间
  if (!data.appointmentTime) {
    errors.push('请选择预约时间');
  }
  
  // 联系人
  if (!data.contactName.trim()) {
    errors.push('请输入联系人姓名');
  }
  
  // 联系电话
  if (!data.contactPhone) {
    errors.push('请输入联系电话');
  } else if (!/^1[3-9]\d{9}$/.test(data.contactPhone)) {
    errors.push('请输入正确的手机号码');
  }
  
  // 地址信息
  if (!data.province) errors.push('请选择省份');
  if (!data.city) errors.push('请选择城市');
  if (!data.district) errors.push('请选择区县');
  
  if (!data.address.trim()) {
    errors.push('请输入详细地址');
  } else if (data.address.length < 5) {
    errors.push('详细地址至少5个字符');
  } else if (data.address.length > 100) {
    errors.push('详细地址不能超过100个字符');
  }
  
  // 备注
  if (data.remark && data.remark.length > 200) {
    errors.push('备注不能超过200个字符');
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
}
```

##### UI布局

```html
<div class="page">
  <PageHeader title="预约信息" subtitle="步骤 1/2" />
  
  <!-- 进度条 -->
  <div class="progress-bar">
    <div class="progress-step active">1</div>
    <div class="progress-line" />
    <div class="progress-step">2</div>
  </div>
  
  <!-- 选中的宠物信息 -->
  <section class="selected-pet">
    <h3>宠物信息</h3>
    <div class="pet-card">
      <PawPrint icon />
      <div>
        <p>{petName}</p>
        <p class="meta">{petType} · {petGender} · {petAge}</p>
      </div>
      <button onClick={() => onNavigate('order-select-pet')}>
        更换
      </button>
    </div>
  </section>
  
  <!-- 预约信息 -->
  <section title="预约信息">
    <FormField label="预约日期*" required>
      <input 
        type="date" 
        min={today}
        value={appointmentDate}
        onChange={(e) => setAppointmentDate(e.target.value)}
      />
    </FormField>
    
    <FormField label="预约时间*" required>
      <select value={appointmentTime} onChange={(e) => setAppointmentTime(e.target.value)}>
        <option value="">请选择</option>
        <option value="09:00">09:00</option>
        <option value="10:00">10:00</option>
        <option value="11:00">11:00</option>
        <option value="14:00">14:00</option>
        <option value="15:00">15:00</option>
        <option value="16:00">16:00</option>
      </select>
    </FormField>
  </section>
  
  <!-- 联系信息 -->
  <section title="联系信息">
    <FormField label="联系人*" required>
      <input 
        type="text" 
        maxLength="20"
        placeholder="请输入联系人姓名"
        value={contactName}
        onChange={(e) => setContactName(e.target.value)}
      />
    </FormField>
    
    <FormField label="联系电话*" required>
      <input 
        type="tel" 
        maxLength="11"
        placeholder="请输入手机号"
        value={contactPhone}
        onChange={(e) => setContactPhone(e.target.value)}
      />
    </FormField>
  </section>
  
  <!-- 地址信息 -->
  <section title="送检地址">
    <FormField label="所在地区*" required>
      <div class="address-picker">
        <select value={province} onChange={handleProvinceChange}>
          <option value="">省份</option>
          {provinces.map(p => <option value={p}>{p}</option>)}
        </select>
        <select value={city} onChange={handleCityChange}>
          <option value="">城市</option>
          {cities.map(c => <option value={c}>{c}</option>)}
        </select>
        <select value={district} onChange={(e) => setDistrict(e.target.value)}>
          <option value="">区县</option>
          {districts.map(d => <option value={d}>{d}</option>)}
        </select>
      </div>
    </FormField>
    
    <FormField label="详细地址*" required>
      <textarea 
        rows="3"
        maxLength="100"
        placeholder="请输入详细地址（街道、门牌号等）"
        value={address}
        onChange={(e) => setAddress(e.target.value)}
      />
    </FormField>
  </section>
  
  <!-- 备注 -->
  <section title="备注说明（选填）">
    <FormField>
      <textarea 
        rows="4"
        maxLength="200"
        placeholder="如有特殊要求请在此说明"
        value={remark}
        onChange={(e) => setRemark(e.target.value)}
      />
      <div class="char-count">{remark.length}/200</div>
    </FormField>
  </section>
  
  <!-- 底部按钮 -->
  <div class="bottom-actions">
    <button class="btn-outline" onClick={() => onNavigate('order-select-pet')}>
      返回
    </button>
    <button class="btn-primary" onClick={handleNext}>
      下一步
    </button>
  </div>
</div>
```

##### 下一步逻辑

```typescript
function handleNext() {
  // 1. 验证表单
  const validation = validateStep1(formData);
  if (!validation.isValid) {
    alert(validation.errors.join('\n'));
    return;
  }
  
  // 2. 保存数据到临时状态
  setOrderDraft({
    petId,
    step1Data: formData
  });
  
  // 3. 进入步骤2
  onNavigate('order-step2', { petId });
}
```

---

#### 9.4 订单步骤2 - 选择项目 (OrderStep2Page / SendPro2)

**路由**: `order-step2`  
**文件**: `/pages/OrderStep2Page.tsx`  
**参数**: `petId: string`

##### 状态管理

```typescript
interface OrderStep2State {
  selectedItems: string[];     // 选中的项目ID数组
  testItems: TestItem[];        // 所有可选项目
  hasRabiesTest: boolean;       // 是否包含狂犬检测
  chipNo: string;               // 芯片号 (狂犬检测必填)
  expressNo: string;            // 快递单号 (狂犬检测必填)
}
```

##### 检测项目展示

```html
<section title="选择检测项目">
  {testItems.map(item => (
    <div 
      class={`test-item-card ${selectedItems.includes(item.id) ? 'selected' : ''}`}
      onClick={() => handleToggleItem(item.id)}
    >
      <div class="item-checkbox">
        {selectedItems.includes(item.id) ? <CheckCircle /> : <Circle />}
      </div>
      
      <div class="item-content">
        <div class="item-header">
          <h4>{item.name}</h4>
          <span class="item-price">¥{item.price}</span>
        </div>
        
        <div class="item-meta">
          <span class="item-category">{item.category}</span>
          {item.isRabies && (
            <span class="badge-rabies">需要芯片号</span>
          )}
        </div>
        
        {item.description && (
          <p class="item-desc">{item.description}</p>
        )}
      </div>
    </div>
  ))}
</section>
```

##### 狂犬检测特殊字段

```html
{hasRabiesTest && (
  <section title="狂犬检测信息" class="rabies-section">
    <div class="alert alert-warning">
      <AlertTriangle icon />
      <div>
        <p>您选择了狂犬病抗体检测</p>
        <p class="small">需要填写宠物芯片号、出生日期和快递单号</p>
      </div>
    </div>
    
    <FormField label="芯片号*" required>
      <input 
        type="text" 
        placeholder="请输入宠物芯片号"
        value={chipNo}
        onChange={(e) => setChipNo(e.target.value)}
      />
      <p class="hint">狂犬检测必须提供芯片号</p>
    </FormField>
    
    <FormField label="出生日期*" required>
      <input 
        type="date"
        value={petBirthday}
        disabled
      />
      <p class="hint">来自宠物档案，如需修改请前往宠物管理</p>
    </FormField>
    
    <FormField label="快递单号*" required>
      <input 
        type="text" 
        placeholder="请输入样本快递单号"
        value={expressNo}
        onChange={(e) => setExpressNo(e.target.value)}
      />
      <p class="hint">请确保快递单号准确无误</p>
    </FormField>
  </section>
)}
```

##### 订单预览

```html
<section title="订单预览" class="order-summary">
  <div class="summary-row">
    <span>已选项目</span>
    <span>{selectedItems.length}项</span>
  </div>
  
  <div class="summary-items">
    {selectedItems.map(itemId => {
      const item = testItems.find(t => t.id === itemId);
      return (
        <div class="summary-item">
          <span>{item.name}</span>
          <span>¥{item.price}</span>
        </div>
      );
    })}
  </div>
  
  <div class="summary-total">
    <span>合计</span>
    <span class="total-amount">¥{calculateTotal()}</span>
  </div>
</section>
```

##### 验证规则

```typescript
function validateStep2(): ValidationResult {
  const errors: string[] = [];
  
  // 至少选择一个项目
  if (selectedItems.length === 0) {
    errors.push('请至少选择一个检测项目');
  }
  
  // 狂犬检测验证
  if (hasRabiesTest) {
    if (!chipNo.trim()) {
      errors.push('狂犬检测需要填写芯片号');
    }
    
    if (!petBirthday) {
      errors.push('狂犬检测需要宠物出生日期，请先完善宠物信息');
    }
    
    if (!expressNo.trim()) {
      errors.push('狂犬检测需要填写快递单号');
    }
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
}
```

##### 提交订单

```typescript
async function handleSubmit() {
  // 1. 验证步骤2
  const validation = validateStep2();
  if (!validation.isValid) {
    alert(validation.errors.join('\n'));
    return;
  }
  
  // 2. 合并步骤1和步骤2的数据
  const step1Data = getOrderDraft().step1Data;
  
  const orderData = {
    // 订单ID
    OrderId: generateUUID(),
    OrderNO: generateOrderNo(),
    
    // 宠物信息
    InfoAnimalId: petId,
    
    // 状态
    OrderStatus: 'pending',
    
    // 检测项目
    TestItemIds: selectedItems,
    TestItemsJson: JSON.stringify(
      selectedItems.map(id => testItems.find(t => t.id === id))
    ),
    TotalAmount: calculateTotal(),
    
    // 预约信息
    AppointmentDate: step1Data.appointmentDate,
    AppointmentTime: step1Data.appointmentTime,
    
    // 联系信息
    ContactName: step1Data.contactName,
    ContactPhone: step1Data.contactPhone,
    
    // 地址
    Province: step1Data.province,
    City: step1Data.city,
    District: step1Data.district,
    Address: step1Data.address,
    
    // 特殊字段
    ChipNO: hasRabiesTest ? chipNo : null,
    ExpressNO: hasRabiesTest ? expressNo : null,
    
    // 备注
    Remark: step1Data.remark || null,
    
    // 时间
    CreateTime: new Date().toISOString(),
    UpdateTime: new Date().toISOString()
  };
  
  // 3. 提交订单
  try {
    setLoading(true);
    await api.createOrder(orderData);
    
    // 4. 清除临时数据
    clearOrderDraft();
    
    // 5. 显示成功提示
    alert('预约成功！\n我们会尽快与您联系确认');
    
    // 6. 跳转到订单列表
    onNavigate('orders');
  } catch (error) {
    alert('提交失败，请重试');
  } finally {
    setLoading(false);
  }
}
```

##### 底部按钮

```html
<div class="bottom-actions">
  <button class="btn-outline" onClick={handleBack}>
    上一步
  </button>
  <button 
    class="btn-primary" 
    onClick={handleSubmit}
    disabled={selectedItems.length === 0 || loading}
  >
    {loading ? '提交中...' : `提交订单 (¥${calculateTotal()})`}
  </button>
</div>
```

---

### 10. 报告管理页 (ReportsPage)

**路由**: `reports`  
**文件**: `/pages/ReportsPage.tsx`

#### 功能模块

##### 10.1 状态筛选

```typescript
const statusFilters = [
  { id: 'all', label: '全部' },
  { id: 'processing', label: '检测中' },
  { id: 'completed', label: '已完成' },
  { id: 'abnormal', label: '异常' }
];
```

##### 10.2 报告卡片

```typescript
interface ReportCardData {
  id: string;              // ReportId
  reportNo: string;        // 报告编号
  petName: string;         // 宠物名称
  petId: string;           // 宠物ID
  testItem: string;        // 检测项目
  testDate: string;        // 检测日期
  reportDate: string;      // 报告日期
  status: string;          // 报告状态
  result: string;          // 结果 (正常/异常)
}
```

**UI布局:**
```html
<div class="report-card">
  <!-- 头部: 报告号 + 状态徽章 -->
  <div class="header">
    <span class="report-no">报告号: {reportNo}</span>
    <span class="status-badge status-{status}">{statusName}</span>
  </div>
  
  <!-- 宠物信息 -->
  <div class="pet-info">
    <PawPrint icon />
    <span>{petName}</span>
  </div>
  
  <!-- 检测项目 -->
  <div class="test-item">
    <FileText icon />
    <span>{testItem}</span>
  </div>
  
  <!-- 检测日期 -->
  <div class="test-date">
    <Calendar icon />
    <span>检测时间: {testDate}</span>
  </div>
  
  <!-- 结果预览 -->
  {status === 'completed' && (
    <div class="result-preview result-{result}">
      {result === 'normal' ? (
        <>
          <CheckCircle icon class="text-green" />
          <span>结果正常</span>
        </>
      ) : (
        <>
          <AlertTriangle icon class="text-orange" />
          <span>存在异常指标</span>
        </>
      )}
    </div>
  )}
  
  <!-- 查看详情按钮 -->
  <button 
    class="btn-view"
    onClick={() => onNavigate('report-view', { reportId: id })}
  >
    查看详情
    <ChevronRight icon />
  </button>
</div>
```

**状态颜色:**
- `processing`: 蓝色
- `completed`: 绿色
- `abnormal`: 橙色

##### 10.3 搜索功能

```typescript
interface ReportsPageState {
  searchQuery: string;
  selectedStatus: string;
  reports: Report[];
}

function filterReports(
  reports: Report[], 
  status: string, 
  query: string
): Report[] {
  return reports.filter(report => {
    const matchStatus = status === 'all' || report.status === status;
    const matchQuery = query === '' || 
                      report.petName.includes(query) ||
                      report.reportNo.includes(query) ||
                      report.testItem.includes(query);
    return matchStatus && matchQuery;
  });
}
```

---

### 11. 报告详情页 (ReportViewPage)

**路由**: `report-view`  
**文件**: `/pages/ReportViewPage.tsx`  
**参数**: `reportId: string`

#### 数据结构

```typescript
interface ReportDetail {
  // 基本信息
  id: string;
  reportNo: string;
  status: string;
  
  // 宠物信息
  petId: string;
  petName: string;
  petType: string;
  petGender: string;
  petAge: string;
  petBirthday: string;
  
  // 检测信息
  testItem: string;
  category: string;
  testDate: string;
  reportDate: string;
  
  // 检测结果
  result: string;               // 总体结果 (normal/abnormal)
  resultData: ResultItem[];     // 详细数据
  conclusion: string;           // 检测结论
  suggestion: string;           // 医生建议
  
  // 医生信息
  doctorName?: string;
  reviewerName?: string;
  
  // 文件
  pdfUrl?: string;
}

interface ResultItem {
  itemName: string;       // 检测指标名称
  value: string;          // 检测值
  unit: string;           // 单位
  referenceRange: string; // 参考范围
  status: string;         // normal/high/low/abnormal
  description?: string;   // 说明
}
```

#### UI模块

##### 11.1 报告头部

```html
<div class="report-header gradient-{result}">
  <div class="header-icon">
    {result === 'normal' ? <CheckCircle /> : <AlertTriangle />}
  </div>
  <div class="header-info">
    <h2>检测报告</h2>
    <p class="report-no">报告号: {reportNo}</p>
    <div class="result-badge result-{result}">
      {result === 'normal' ? '结果正常' : '存在异常'}
    </div>
  </div>
</div>
```

##### 11.2 宠物信息

```html
<section title="宠物信息">
  <div class="pet-info-grid">
    <InfoItem label="宠物名称" value={petName} />
    <InfoItem label="宠物类型" value={petType} />
    <InfoItem label="性别" value={petGender} />
    <InfoItem label="年龄" value={petAge} />
    <InfoItem label="出生日期" value={petBirthday} />
  </div>
  <button onClick={() => onNavigate('pet-detail', { petId })}>
    查看宠物详情
  </button>
</section>
```

##### 11.3 检测信息

```html
<section title="检测信息">
  <InfoRow icon={FileText} label="检测项目" value={testItem} />
  <InfoRow icon={Calendar} label="检测日期" value={testDate} />
  <InfoRow icon={Calendar} label="报告日期" value={reportDate} />
  <InfoRow icon={User} label="检测医生" value={doctorName || '-'} />
  <InfoRow icon={Shield} label="审核医生" value={reviewerName || '-'} />
</section>
```

##### 11.4 检测结果详细数据

```html
<section title="检测结��">
  <div class="result-table">
    <div class="table-header">
      <div>检测指标</div>
      <div>检测值</div>
      <div>参考范围</div>
      <div>状态</div>
    </div>
    
    {resultData.map(item => (
      <div class="table-row row-{item.status}">
        <div class="item-name">
          {item.itemName}
          {item.description && (
            <HelpCircle 
              icon 
              tooltip={item.description}
            />
          )}
        </div>
        
        <div class="item-value">
          {item.value} {item.unit}
        </div>
        
        <div class="item-range">
          {item.referenceRange}
        </div>
        
        <div class="item-status">
          {item.status === 'normal' && (
            <span class="status-normal">
              <CheckCircle icon />
              正常
            </span>
          )}
          {item.status === 'high' && (
            <span class="status-high">
              <ArrowUp icon />
              偏高
            </span>
          )}
          {item.status === 'low' && (
            <span class="status-low">
              <ArrowDown icon />
              偏低
            </span>
          )}
          {item.status === 'abnormal' && (
            <span class="status-abnormal">
              <AlertTriangle icon />
              异常
            </span>
          )}
        </div>
      </div>
    ))}
  </div>
</section>
```

**状态颜色:**
- `normal`: 绿色
- `high`: 橙色
- `low`: 蓝色
- `abnormal`: 红色

##### 11.5 检测结论

```html
<section title="检测结论">
  <div class="conclusion-box">
    <p>{conclusion}</p>
  </div>
</section>
```

##### 11.6 医生建议

```html
<section title="医生建议">
  <div class="suggestion-box">
    <div class="suggestion-icon">
      <Shield icon />
    </div>
    <div class="suggestion-content">
      <p>{suggestion}</p>
    </div>
  </div>
</section>
```

##### 11.7 底部操作

```html
<div class="bottom-actions">
  {pdfUrl && (
    <button class="btn-outline" onClick={handleDownloadPDF}>
      <Download icon />
      下载PDF报告
    </button>
  )}
  
  <button class="btn-outline" onClick={handleShare}>
    <Share2 icon />
    分享报告
  </button>
  
  <button class="btn-primary" onClick={() => onNavigate('order-select-pet')}>
    再次预约
  </button>
</div>
```

---

### 12. 我的页面 (MinePage)

**路由**: `mine`  
**文件**: `/pages/MinePage.tsx`

#### UI模块

##### 12.1 用户信息卡片

```html
<div class="user-card gradient-purple">
  <div class="user-avatar">
    <User icon />
  </div>
  <div class="user-info">
    <h2>{userName || '宠物主人'}</h2>
    <p>{phone || '未绑定手机号'}</p>
  </div>
  <button class="btn-edit" onClick={() => onNavigate('profile-edit')}>
    <Edit2 icon />
  </button>
</div>
```

##### 12.2 统计信息

```html
<div class="stats-grid">
  <div class="stat-item" onClick={() => onNavigate('pets')}>
    <div class="stat-value">{petCount}</div>
    <div class="stat-label">宠物档案</div>
  </div>
  
  <div class="stat-item" onClick={() => onNavigate('orders')}>
    <div class="stat-value">{orderCount}</div>
    <div class="stat-label">我的订单</div>
  </div>
  
  <div class="stat-item" onClick={() => onNavigate('reports')}>
    <div class="stat-value">{reportCount}</div>
    <div class="stat-label">检测报告</div>
  </div>
</div>
```

##### 12.3 功能菜单

```typescript
const menuItems = [
  {
    id: 'icon-download',
    icon: Sparkles,
    title: '图标下载',
    description: '导出微信小程序图标',
    color: 'purple',
    onClick: () => onNavigate('icon-download')
  },
  {
    id: 'address',
    icon: MapPin,
    title: '地址管理',
    description: '管理常用地址',
    color: 'blue',
    onClick: () => onNavigate('address-manage')
  },
  {
    id: 'settings',
    icon: Settings,
    title: '设置',
    description: '账号与系统设置',
    color: 'gray',
    onClick: () => onNavigate('settings')
  },
  {
    id: 'help',
    icon: HelpCircle,
    title: '帮助中心',
    description: '常见问题与联系客服',
    color: 'green',
    onClick: () => onNavigate('help')
  }
];
```

**菜单卡片:**
```html
<div class="menu-list">
  {menuItems.map(item => (
    <div class="menu-item" onClick={item.onClick}>
      <div class={`menu-icon bg-${item.color}-100`}>
        <item.icon class={`text-${item.color}-600`} />
      </div>
      <div class="menu-content">
        <div class="menu-title">{item.title}</div>
        <div class="menu-desc">{item.description}</div>
      </div>
      <ChevronRight class="menu-arrow" />
    </div>
  ))}
</div>
```

##### 12.4 退出登录

```html
<button class="btn-logout" onClick={handleLogout}>
  <LogOut icon />
  退出登录
</button>
```

---

### 13. 图标下载页 (IconDownloadPage)

**路由**: `icon-download`  
**文件**: `/pages/IconDownloadPage.tsx`

#### 功能说明

- 浏览项目中所有35个Lucide React图标
- 搜索和分类筛选
- 提供导出入口

#### UI模块

##### 13.1 统计面板

```html
<div class="stats-panel gradient-purple">
  <div class="stat">
    <div class="stat-value">{totalIcons}</div>
    <div class="stat-label">项目使用图标</div>
  </div>
  <div class="stat">
    <div class="stat-value">{selectedCount}</div>
    <div class="stat-label">已选择</div>
  </div>
  <div class="stat">
    <div class="stat-value">{categoryCount}</div>
    <div class="stat-label">分类</div>
  </div>
</div>
```

##### 13.2 导出入口按钮

```html
<button 
  class="export-button gradient-purple"
  onClick={() => onNavigate('icon-export')}
>
  <Download icon />
  <div>
    <div>导出微信小程序TabBar图标</div>
    <div class="small">81px × 81px · 8个PNG文件 · 可预览</div>
  </div>
  <ChevronRight icon />
</button>
```

##### 13.3 搜索和分类

```typescript
const categories = [
  '全部', '导航', '操作', '功能', '状态', '联系', '文档', '信息', '装饰'
];
```

##### 13.4 图标网格

```html
<div class="icon-grid">
  {filteredIcons.map(icon => (
    <div 
      class={`icon-card ${selectedIcons.has(icon.name) ? 'selected' : ''}`}
      onClick={() => toggleIcon(icon.name)}
    >
      <div class="icon-display">
        <icon.component size={32} />
      </div>
      <div class="icon-name">{icon.name}</div>
      {selectedIcons.has(icon.name) && (
        <div class="selected-badge">
          <CheckCircle icon />
        </div>
      )}
    </div>
  ))}
</div>
```

---

### 14. 图标导出页 (IconExportPage)

**路由**: `icon-export`  
**文件**: `/pages/IconExportPage.tsx`

#### 功能说明

专业的TabBar图标导出工具，提供：
- 实时预览选中/未选中状态
- 单独下载或一键下载
- 符合微信小程序规范

#### UI模块

##### 14.1 规格说明

```html
<div class="spec-card gradient-purple">
  <h3>微信小程序TabBar图标</h3>
  <ul>
    <li>✓ 尺寸：81px × 81px</li>
    <li>✓ 格式：PNG 透明背景</li>
    <li>✓ 描边：2.5px</li>
    <li>✓ 未选中：#8C8C8D（微信标准灰）</li>
    <li>✓ 选中：#615FFF（主题紫色）</li>
  </ul>
</div>
```

##### 14.2 一键下载按钮

```html
<GradientButton
  variant="purple"
  size="lg"
  onClick={downloadAllIcons}
>
  <Download icon />
  一键下载全部 (8个PNG文件)
</GradientButton>
```

##### 14.3 图标预览卡片

```typescript
const tabBarIcons = [
  { name: '首页', fileName: 'home', component: Home },
  { name: '宠物', fileName: 'pet', component: PawPrint },
  { name: '订单', fileName: 'order', component: ShoppingBag },
  { name: '我的', fileName: 'profile', component: User }
];
```

```html
{tabBarIcons.map(icon => (
  <div class="preview-card">
    <!-- 头部信息 -->
    <div class="card-header">
      <div>
        <div class="icon-name">{icon.name}</div>
        <div class="file-names">
          {icon.fileName}.png / {icon.fileName}-active.png
        </div>
      </div>
      <button onClick={() => downloadSingleIcon(icon)}>
        <Download icon />
        下载
      </button>
    </div>
    
    <!-- 预览网格 -->
    <div class="preview-grid">
      <!-- 未选中状态 -->
      <div class="preview-item">
        <div class="preview-display">
          <icon.component size={48} style={{ color: '#8C8C8D' }} />
        </div>
        <div class="preview-label">未选中</div>
        <div class="preview-color">#8C8C8D</div>
        {downloadStatus[`${icon.fileName}-normal`] === 'success' && (
          <CheckCircle class="success-badge" />
        )}
      </div>
      
      <!-- 选中状态 -->
      <div class="preview-item">
        <div class="preview-display active">
          <icon.component size={48} style={{ color: '#615FFF' }} />
        </div>
        <div class="preview-label">选中</div>
        <div class="preview-color">#615FFF</div>
        {downloadStatus[`${icon.fileName}-active`] === 'success' && (
          <CheckCircle class="success-badge" />
        )}
      </div>
    </div>
  </div>
))}
```

##### 14.4 使用说明

```html
<div class="info-box">
  <h4>📌 使用说明</h4>
  <ol>
    <li>点击"一键下载全部"下载所有TabBar图标</li>
    <li>或点击单个图标的"下载"按钮单独下载</li>
    <li>下载的PNG文件可直接用于微信小程序</li>
    <li>将文件放入 images/tabbar/ 目录</li>
    <li>在 app.json 中配置 tabBar</li>
  </ol>
</div>
```

##### 14.5 配置代码示例

```html
<div class="code-block">
  <div class="code-title">app.json 配置示例：</div>
  <pre>{`{
  "tabBar": {
    "color": "#8C8C8D",
    "selectedColor": "#615FFF",
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
      }
    ]
  }
}`}</pre>
</div>
```

---

## 组件库

### 通用组件

#### 1. PageHeader

```typescript
interface PageHeaderProps {
  title: string;              // 页面标题
  subtitle?: string;          // 副标题 (可选)
  onBack?: () => void;        // 返回按钮回调 (可选)
  rightButton?: ReactNode;    // 右侧按钮 (可选)
}
```

**使用示例:**
```tsx
<PageHeader 
  title="宠物详情" 
  onBack={() => onNavigate('pets')}
/>
```

---

#### 2. GradientButton

```typescript
interface GradientButtonProps {
  variant: 'purple' | 'blue' | 'green' | 'orange';
  size: 'sm' | 'md' | 'lg';
  onClick: () => void;
  disabled?: boolean;
  loading?: boolean;
  children: ReactNode;
  className?: string;
}
```

**渐变配置:**
- `purple`: `from-indigo-500 to-purple-600`
- `blue`: `from-blue-500 to-indigo-600`
- `green`: `from-emerald-500 to-teal-600`
- `orange`: `from-orange-500 to-red-600`

---

#### 3. FormField

```typescript
interface FormFieldProps {
  label: string;
  required?: boolean;
  hint?: string;
  error?: string;
  children: ReactNode;
}
```

---

#### 4. StatusBadge

```typescript
interface StatusBadgeProps {
  status: string;
  label: string;
  size?: 'sm' | 'md';
}
```

---

#### 5. BottomNav

```typescript
interface NavItem {
  id: string;
  label: string;
  icon: LucideIcon;
  activeIcon?: LucideIcon;
  badge?: number;
}

const navItems: NavItem[] = [
  { id: 'home', label: '首页', icon: Home },
  { id: 'pets', label: '宠物', icon: PawPrint },
  { id: 'orders', label: '订单', icon: ShoppingBag },
  { id: 'mine', label: '我的', icon: User }
];
```

**样式规范:**
- 高度: 64px + safe-area-inset-bottom
- 背景: 白色
- 阴影: shadow-lg
- 激活色: 紫色渐变
- 未激活色: 灰色

---

## 状态管理

### 全局状态

```typescript
interface AppState {
  // 用户信息
  user: {
    id: string;
    name: string;
    phone: string;
  } | null;
  
  // 宠物列表
  pets: Pet[];
  
  // 订单列表
  orders: Order[];
  
  // 报告列表
  reports: Report[];
  
  // 当前页面
  currentPage: Page;
  
  // 页面参数
  pageParams: any;
  
  // 订单草稿 (临时数据)
  orderDraft: {
    petId: string;
    step1Data: OrderStep1Data;
  } | null;
}
```

### 本地存储

```typescript
// 存储键
const STORAGE_KEYS = {
  USER: 'pet_system_user',
  PETS: 'pet_system_pets',
  ORDERS: 'pet_system_orders',
  REPORTS: 'pet_system_reports',
  ORDER_DRAFT: 'pet_system_order_draft'
};

// 示例函数
function savePets(pets: Pet[]) {
  localStorage.setItem(STORAGE_KEYS.PETS, JSON.stringify(pets));
}

function loadPets(): Pet[] {
  const data = localStorage.getItem(STORAGE_KEYS.PETS);
  return data ? JSON.parse(data) : [];
}
```

---

## 验证规则

### 通用验证

```typescript
// 手机号验证
function validatePhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone);
}

// 日期验证
function validateDate(date: string): boolean {
  const dateObj = new Date(date);
  return !isNaN(dateObj.getTime());
}

// 字符串长度验证
function validateLength(
  str: string, 
  min: number, 
  max: number
): boolean {
  const len = str.trim().length;
  return len >= min && len <= max;
}
```

---

## API接口

### 接口列表

```typescript
// 宠物相关
interface PetAPI {
  // 获取宠物列表
  getPets(): Promise<Pet[]>;
  
  // 获取宠物详情
  getPetById(id: string): Promise<Pet>;
  
  // 创建宠物
  createPet(data: CreatePetDTO): Promise<Pet>;
  
  // 更新宠物
  updatePet(id: string, data: UpdatePetDTO): Promise<Pet>;
  
  // 删除宠物
  deletePet(id: string): Promise<void>;
}

// 订单相关
interface OrderAPI {
  // 获取订单列表
  getOrders(status?: string): Promise<Order[]>;
  
  // 获取订单详情
  getOrderById(id: string): Promise<OrderDetail>;
  
  // 创建订单
  createOrder(data: CreateOrderDTO): Promise<Order>;
  
  // 取消订单
  cancelOrder(id: string): Promise<void>;
}

// 报告相关
interface ReportAPI {
  // 获取报告列表
  getReports(status?: string): Promise<Report[]>;
  
  // 获取报告详情
  getReportById(id: string): Promise<ReportDetail>;
  
  // 下载PDF
  downloadReportPDF(id: string): Promise<Blob>;
}

// 检测项目
interface TestItemAPI {
  // 获取可选项目列表
  getTestItems(): Promise<TestItem[]>;
}
```

### DTO定义

```typescript
// 创建宠物DTO
interface CreatePetDTO {
  AnimalName: string;
  AnimalTypeId: string;
  DictSexId: string;
  Birthday: string;
  ChipNO?: string | null;
  Breed?: string | null;
  Weight?: number | null;
  Color?: string | null;
  IsSterilized?: boolean;
}

// 创建订单DTO
interface CreateOrderDTO {
  InfoAnimalId: string;
  TestItemIds: string[];
  AppointmentDate: string;
  AppointmentTime: string;
  ContactName: string;
  ContactPhone: string;
  Province: string;
  City: string;
  District: string;
  Address: string;
  ChipNO?: string | null;
  ExpressNO?: string | null;
  Remark?: string | null;
}
```

---

## 附录

### A. 颜色规范

```css
/* 主题色 */
--color-primary-start: #6366F1;  /* 蓝紫色 */
--color-primary-end: #8B5CF6;    /* 紫色 */

/* 状态色 */
--color-success: #10B981;        /* 绿色 */
--color-warning: #F59E0B;        /* 橙色 */
--color-error: #EF4444;          /* 红色 */
--color-info: #3B82F6;           /* 蓝色 */

/* 灰度 */
--color-gray-50: #F9FAFB;
--color-gray-100: #F3F4F6;
--color-gray-500: #6B7280;
--color-gray-900: #111827;

/* 微信小程序TabBar */
--color-tabbar-normal: #8C8C8D;
--color-tabbar-active: #615FFF;
```

### B. 圆角规范

```css
--radius-sm: 8px;    /* 小圆角 */
--radius-md: 12px;   /* 中圆角 */
--radius-lg: 16px;   /* 大圆角 */
--radius-xl: 20px;   /* 超大圆角 */
--radius-2xl: 24px;  /* 卡片圆角 */
--radius-3xl: 32px;  /* 最大圆角 */
```

### C. 间距规范

```css
--spacing-xs: 4px;
--spacing-sm: 8px;
--spacing-md: 12px;
--spacing-lg: 16px;
--spacing-xl: 20px;
--spacing-2xl: 24px;
--spacing-3xl: 32px;
```

### D. 图标尺寸

```css
--icon-xs: 16px;
--icon-sm: 20px;
--icon-md: 24px;
--icon-lg: 32px;
--icon-xl: 48px;
```

---

## 版本历史

### v2.0 (2024-12-26)
- ✅ 完成图标导出功能
- ✅ 适配微信小程序TabBar规范
- ✅ 优化首页快捷按钮
- ✅ 完善订单创建流程

### v1.0 (2024-12-25)
- ✅ 初始系统搭建
- ✅ 完成核心页面开发
- ✅ 实现宠物管理功能
- ✅ 实现订单管理功能
- ✅ 实现报告管理功能

---

**文档结束**

如有疑问，请联系技术团队。
