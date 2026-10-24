# Quick Start Guide: Bug Fixes and Enhancements

**Feature**: 004-bugfixes-and-enhancements
**Date**: 2025-12-28
**Audience**: 开发人员

---

## 1. 环境准备

### 1.1 开发环境要求

**后端**:
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- IDE: IntelliJ IDEA (推荐) 或 Eclipse

**前端**:
- 微信开发者工具 (最新版)
- Node.js 16+ (用于npm包管理)

**数据库**:
- MySQL 8.0+
- 客户端工具: Navicat, DBeaver, 或 MySQL Workbench

### 1.2 克隆项目

```bash
# 克隆主项目
cd ~/claudelife/PETLIS

# 确认分支
git branch
# * 004-bugfixes-and-enhancements
```

---

## 2. 数据库设置

### 2.1 创建通知表

**连接MySQL**:
```bash
mysql -u root -p laboklin
```

**执行DDL**:
```sql
-- 创建通知记录表
CREATE TABLE lin_lb_notification (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  order_id INT NOT NULL COMMENT '订单ID (DiagnosisDetailId)',
  hospital_id INT NOT NULL COMMENT '医院ID (InfoHospitalId)',
  old_status INT COMMENT '原状态 (DiagnosisStatus)',
  new_status INT NOT NULL COMMENT '新状态 (DiagnosisStatus)',
  operator_id INT COMMENT '操作人ID (lin_user.id)',
  operation_time DATETIME NOT NULL COMMENT '操作时间',
  wechat_sent_status TINYINT NOT NULL DEFAULT 0 COMMENT '微信发送状态 0-待发送 1-已发送 2-失败',
  wechat_message_id VARCHAR(100) COMMENT '微信消息ID',
  error_message TEXT COMMENT '错误信息',
  createtime INT UNSIGNED COMMENT '创建时间 (Unix时间戳)',
  updatetime INT UNSIGNED COMMENT '更新时间 (Unix时间戳)',
  INDEX idx_order_id (order_id),
  INDEX idx_hospital_id (hospital_id),
  INDEX idx_operation_time (operation_time),
  INDEX idx_hospital_status (hospital_id, wechat_sent_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态变化通知记录表';
```

### 2.2 创建触发器

**触发器**: 订单状态变化时自动创建通知记录

```sql
DELIMITER $$

CREATE TRIGGER trg_diagnosis_status_change
AFTER UPDATE ON lin_lb_diagnosisdetail
FOR EACH ROW
BEGIN
  DECLARE v_hospital_id INT;

  -- 检查状态是否发生变化
  IF OLD.DiagnosisStatus != NEW.DiagnosisStatus THEN

    -- 获取医院ID
    SELECT p.info_hospital_id INTO v_hospital_id
    FROM lin_lb_listdiagnosisproject ldp
    JOIN lin_lb_pet p ON ldp.pet_id = p.id
    WHERE ldp.ListDiagnosisProjectId = NEW.ListDiagnosisProjectId
    LIMIT 1;

    -- 插入通知记录
    INSERT INTO lin_lb_notification (
      order_id,
      hospital_id,
      old_status,
      new_status,
      operation_time,
      createtime,
      updatetime
    ) VALUES (
      NEW.DiagnosisDetailId,
      v_hospital_id,
      OLD.DiagnosisStatus,
      NEW.DiagnosisStatus,
      NOW(),
      UNIX_TIMESTAMP(),
      UNIX_TIMESTAMP()
    );

  END IF;
END$$

DELIMITER ;
```

### 2.3 验证数据库结构

```sql
-- 检查通知表
SHOW CREATE TABLE lin_lb_notification;

-- 检查触发器
SHOW TRIGGERS LIKE 'lin_lb_diagnosisdetail';

-- 验证核心表存在
SELECT COUNT(*) FROM lin_lb_pet;
SELECT COUNT(*) FROM lin_user;
SELECT COUNT(*) FROM lin_lb_diagnosisdetail;
```

---

## 3. 后端开发

### 3.1 启动后端服务

```bash
cd petlis-backend

# 使用Maven编译
mvn clean install

# 运行Spring Boot应用
mvn spring-boot:run
```

**或** 在IDE中运行 `PetlisApplication.java`

### 3.2 验证后端API

**测试认证接口**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"code":"wx_code"}'
```

**测试订单列表**:
```bash
curl -X GET "http://localhost:8080/api/orders/list?pageNum=1&pageSize=5" \
  -H "Authorization: Bearer <your_jwt_token>"
```

### 3.3 新增实体类

**创建 `NotificationRecord.java`**:

```bash
# 创建文件
touch petlis-backend/src/main/java/com/petlis/entity/business/NotificationRecord.java
```

**参考**: `data-model.md` 第1.7节定义字段

---

## 4. 前端开发

### 4.1 打开微信开发者工具

1. 打开微信开发者工具
2. 导入项目: `~/claudelife/PETLIS/miniprogram`
3. 选择AppID (测试号或正式号)
4. 点击"编译"

### 4.2 配置API地址

**编辑** `miniprogram/utils/api.js`:

```javascript
const BASE_URL = 'http://localhost:8080/api'

// 或使用开发环境
// const BASE_URL = 'https://dev-api.petlis.com/api'
```

### 4.3 修改订单列表页

**文件**: `miniprogram/pages/orders/orders.js`

**关键修改**:
1. ✅ 修改pageSize从100改为5
2. ✅ 实现onReachBottom加载更多
3. ✅ 验证数据正确显示

**代码片段**:
```javascript
data: {
  orders: [],
  pageNum: 1,
  pageSize: 5,  // ✅ 修改为5
  hasMore: true,
  loading: false
},

onReachBottom() {
  if (this.data.hasMore && !this.data.loading) {
    this.loadMore()
  }
}
```

### 4.4 修改宠物选择页

**文件**: `miniprogram/pages/order-select-pet/order-select-pet.js`

**关键修改**:
1. ✅ onShow中检查newPetId参数
2. ✅ 默认选中最新宠物
3. ✅ 宠物列表按createtime倒序

**代码片段**:
```javascript
onShow() {
  const pages = getCurrentPages()
  const currPage = pages[pages.length - 1]

  if (currPage.data && currPage.data.newPetId) {
    this.setData({ selectedPetId: currPage.data.newPetId })
  }

  this.loadPets()
}
```

---

## 5. 开发优先级

根据功能规格说明书中的优先级：

### P1 - 严重Bug (必须修复)

1. ✅ **订单列表分页加载** (`pages/orders/orders.js`)
   - 默认显示5条
   - 下拉加载更多
   - 显示"加载中"和"没有更多"

2. ✅ **订单详情数据显示** (`pages/order-detail/order-detail.js`)
   - 验证所有字段正确显示
   - 处理空数据情况

3. ✅ **宠物编辑预填充** (`pages/order-add-pet/order-add-pet.js`)
   - 编辑模式加载宠物详情
   - 预填充所有表单字段

### P2 - 重要功能

4. ✅ **宠物报告显示** (`pages/pet-detail/pet-detail.js`)
   - 显示申请时间
   - 显示报告状态
   - 按时间倒序

5. ✅ **新增宠物自动选择** (`pages/order-select-pet/order-select-pet.js`)
   - 新增后自动选中
   - 刷新后默认选最新

6. ✅ **我的页面医院信息** (`pages/mine/mine.js`)
   - 显示用户所属医院
   - 显示医院地址和电话

### P3 - 未来功能

7. ⏸️ **支付模块** (仅预留接口)
8. ⏸️ **微信通知** (数据库已创建，功能暂不实现)

---

## 6. 测试指南

### 6.1 功能测试

**订单列表测试**:
```javascript
// 1. 打开订单列表页
// 2. 验证默认显示5条订单 ✅
// 3. 滚动到底部，验证加载更多 ✅
// 4. 验证数据正确显示（宠物名称、状态等） ✅
```

**宠物编辑测试**:
```javascript
// 1. 打开宠物详情页
// 2. 点击"编辑"按钮
// 3. 验证表单预填充正确 ✅
// 4. 修改并保存 ✅
```

**新增宠物测试**:
```javascript
// 1. 在预约检测流程点击"添加宠物"
// 2. 创建新宠物
// 3. 验证返回后自动选中新宠物 ✅
// 4. 刷新页面，验证默认选最新 ✅
```

### 6.2 性能测试

**API响应时间**:
```bash
# 订单列表API
ab -n 100 -c 10 http://localhost:8080/api/orders/list?pageNum=1&pageSize=5

# 目标: p95 < 200ms
```

**前端加载时间**:
```javascript
console.time('loadOrders')
await this.loadOrders()
console.timeEnd('loadOrders')
// 目标: < 1秒
```

### 6.3 数据验证测试

**订单数据完整性**:
```javascript
// 验证所有必填字段非空
result.list.forEach(order => {
  assert(order.diagnosisDetailId, '订单ID缺失')
  assert(order.pet?.animalName, '宠物名称缺失')
  assert(order.diagnosisStatus !== undefined, '状态缺失')
})
```

---

## 7. 调试技巧

### 7.1 后端调试

**启用SQL日志** (application.yml):
```yaml
logging:
  level:
    com.petlis: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**断点调试**:
- 在Service层设置断点
- 查看Entity字段映射
- 验证SQL查询结果

### 7.2 前端调试

**微信开发者工具**:
- Console查看日志
- Network查看API请求
- AppData查看页面数据
- Performance查看性能

**调试代码**:
```javascript
console.log('[Orders] 加载订单', { pageNum, pageSize })
console.error('[Orders] 加载失败', err)
console.table(this.data.orders)
```

### 7.3 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 订单列表为空 | API返回数据格式错误 | 检查result.list或result.content |
| 宠物信息不显示 | 字段名不匹配 | 检查后端返回的字段名 |
| 分页不工作 | hasMore标志错误 | 检查list.length >= pageSize |
| 医院信息显示错误 | hospital_id为0 | 检查用户表的hospital_id |

---

## 8. 提交代码

### 8.1 代码审查清单

**后端**:
- ✅ 遵循RESTful规范
- ✅ 统一返回格式 {code, message, data}
- ✅ 字段映射使用@Column注解
- ✅ 添加异常处理

**前端**:
- ✅ 数据绑定正确
- ✅ 空数据处理
- ✅ 加载状态提示
- ✅ 错误提示友好

### 8.2 Git提交

```bash
# 查看状态
git status

# 添加修改的文件
git add miniprogram/pages/orders/orders.js
git add petlis-backend/src/main/java/com/petlis/entity/business/NotificationRecord.java

# 提交
git commit -m "fix: 实现订单列表分页加载和宠物编辑预填充

- 修改订单列表默认显示5条，支持下拉加载更多
- 实现宠物编辑页面预填充功能
- 新增NotificationRecord实体类
- 创建通知记录表和触发器

Refs: #004-bugfixes-and-enhancements"

# 推送到远程
git push origin 004-bugfixes-and-enhancements
```

---

## 9. 部署检查清单

### 9.1 数据库

- ✅ 通知表已创建
- ✅ 触发器已创建并测试
- ✅ 索引已创建

### 9.2 后端

- ✅ NotificationRecord实体类已创建
- ✅ NotificationService已实现
- ✅ NotificationController已添加
- ✅ API测试通过

### 9.3 前端

- ✅ 订单列表分页加载实现
- ✅ 订单详情数据显示正确
- ✅ 宠物编辑预填充实现
- ✅ 宠物报告显示调整
- ✅ 新增宠物自动选择实现
- ✅ 我的页面医院信息显示

### 9.4 测试

- ✅ 所有P1功能测试通过
- ✅ 所有P2功能测试通过
- ✅ API性能测试通过
- ✅ 前端加载时间测试通过

---

## 10. 下一步

完成Quick Start后，请：

1. ✅ 阅读 `research.md` 了解技术架构
2. ✅ 阅读 `data-model.md` 了解数据模型
3. ✅ 阅读 `contracts/*.md` 了解API接口
4. ✅ 按照优先级开发功能 (P1 → P2 → P3)
5. ✅ 运行测试并修复Bug
6. ✅ 提交代码并创建Pull Request

---

**文档版本**: 1.0.0
**最后更新**: 2025-12-28
**作者**: PETLIS Team
