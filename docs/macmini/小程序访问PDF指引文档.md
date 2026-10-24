# 微信小程序访问Laboklin检测报告PDF指引文档

## 📋 概述

本文档详细说明如何从现有Laboklin系统中获取PDF报告的下载地址，以及如何判断PDF是否已生成，供微信小程序开发使用。

---

## 🗄️ 数据库相关表和字段

### 主要数据表：`lb_diagnosisdetail`

#### 关键字段说明

| 字段名 | 类型 | 说明 | 用途 |
|--------|------|------|------|
| `DiagnosisDetailId` | int | 诊断详情ID（主键） | 唯一标识一个检测项 |
| `ListDiagnosisProjectId` | int | 项目ID | 用于构建PDF路径 |
| `PDFHttpUrl` | varchar | PDF访问URL | **核心字段**，存储PDF的相对路径 |
| `IsSendMail` | tinyint | 是否已发送邮件 | **1=已发送报告（PDF已生成）** |
| `SendMailDt` | datetime | 发送邮件时间 | 报告发送时间 |
| `PayStatus2` | int | 支付状态 | 0=未支付，1=现金，2=转账，3=网银已支付 |
| `IsCheckPass` | tinyint | 是否免除审核 | 0=未免除，1=免除 |
| `ReportResult` | text | 报告结果（HTML） | PDF的HTML内容源 |
| `InfoTestTypeId` | int | 检测类型ID | 关联检测类型 |
| `UserNO` | varchar | 用户编号 | 报告中的用户编号 |
| `ReportNO` | varchar | 报告编号 | 报告唯一编号 |

---

## 📁 PDF文件存储路径详解

### 1. PDF文件系统存储路径

```
服务器绝对路径：
/public/Content/FilePdf/{ListDiagnosisProjectId}_{DiagnosisDetailId}/{文件名}.pdf

示例：
/public/Content/FilePdf/12345_6789/2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf
```

**路径组成说明：**
- `/public/Content/FilePdf/` - 固定基础路径
- `{ListDiagnosisProjectId}_{DiagnosisDetailId}` - 项目ID_详情ID 组成的子目录名
- `{文件名}.pdf` - 按规则生成的PDF文件名

### 2. PDF文件名生成规则

根据 `application/common.php` 第839-854行的代码，PDF文件名格式为：

```php
文件名格式：
{发送日期},{动物名},{主人名},{用户号},{报告号}{诊断结果}.pdf

字段说明：
- 发送日期：SendMailDt字段的日期部分，格式：Y.m.d（如：2025.06.06）
- 动物名：动物名称（如：小五）
- 主人名：宠物主人姓名（如：廖）
- 用户号：UserNO（如：55550-24541）
- 报告号：ReportNO（如：2505-Q-34439）
- 诊断结果：DiagnosticResults（如有）
```

**文件名处理规则：**
- 移除所有双引号和单引号
- 移除所有空格
- `&` 字符替换为 `和`
- `#` 字符移除
- 自动添加 `.pdf` 扩展名

**完整示例：**
```
2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf
```

### 3. 数据库中存储的PDF路径（PDFHttpUrl字段）

```php
数据库存储的相对路径：
/Content/FilePdf/{ListDiagnosisProjectId}_{DiagnosisDetailId}/{文件名}.pdf

示例（存储在lb_diagnosisdetail.PDFHttpUrl字段）：
/Content/FilePdf/12345_6789/2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf
```

### 4. PDF的HTTP访问URL

```php
完整访问URL格式：
https://{域名}/Content/FilePdf/{ListDiagnosisProjectId}_{DiagnosisDetailId}/{文件名}.pdf

示例：
https://laboklin.cn/Content/FilePdf/12345_6789/2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf
```

---

## ✅ 如何判断PDF已生成

### 方法1：检查 `IsSendMail` 字段（推荐）

```sql
-- 查询已生成PDF的报告
SELECT * FROM lb_diagnosisdetail
WHERE IsSendMail = 1;
```

**判断逻辑：**
- `IsSendMail = 1` → PDF已生成并发送
- `IsSendMail = 0` → PDF未生成或报告未完成

**参考代码位置：** `application/admin/model/LbDiagnosisdetail.php` 第136行
```php
if ($this->IsSendMail == 1) {
    // PDF已生成
}
```

### 方法2：检查 `PDFHttpUrl` 字段是否为空

```sql
-- 查询PDFHttpUrl不为空的记录
SELECT * FROM lb_diagnosisdetail
WHERE PDFHttpUrl IS NOT NULL
  AND PDFHttpUrl != ''
  AND PDFHttpUrl != '/Content/FilePdf/';
```

**判断逻辑：**
- `PDFHttpUrl` 不为空且不等于 `/Content/FilePdf/` → PDF已生成
- `PDFHttpUrl` 为空或只有路径前缀 → PDF未生成

**参考代码位置：** `application/index/controller/Project/View.php` 第73-75行和94-96行
```php
if(!empty($enty->PDFHttpUrl)){
    return $this->redirect($enty->PDFHttpUrl);
}
```

### 方法3：组合判断（最严格）

```sql
-- 同时检查IsSendMail和PDFHttpUrl
SELECT * FROM lb_diagnosisdetail
WHERE IsSendMail = 1
  AND PDFHttpUrl IS NOT NULL
  AND PDFHttpUrl != ''
  AND PDFHttpUrl != '/Content/FilePdf/';
```

---

## 📊 从数据库获取PDF地址的SQL示例

### 示例1：根据诊断详情ID获取PDF地址

```sql
SELECT
    DiagnosisDetailId,
    ListDiagnosisProjectId,
    PDFHttpUrl,
    IsSendMail,
    SendMailDt,
    UserNO,
    ReportNO
FROM lb_diagnosisdetail
WHERE DiagnosisDetailId = 6789;
```

### 示例2：根据用户编号获取所有已完成的报告

```sql
SELECT
    d.DiagnosisDetailId,
    d.ListDiagnosisProjectId,
    d.PDFHttpUrl,
    d.IsSendMail,
    d.SendMailDt,
    d.UserNO,
    d.ReportNO,
    p.AnimalName,
    p.MasterName,
    h.HospitalName
FROM lb_diagnosisdetail d
LEFT JOIN lb_listdiagnosisproject p ON d.ListDiagnosisProjectId = p.ListDiagnosisProjectId
LEFT JOIN lb_infohospital h ON p.InfoHospitalId = h.InfoHospitalId
WHERE d.UserNO = '55550-24541'
  AND d.IsSendMail = 1
  AND d.PDFHttpUrl IS NOT NULL
  AND d.PDFHttpUrl != ''
ORDER BY d.SendMailDt DESC;
```

### 示例3：获取最近30天已完成的报告列表

```sql
SELECT
    d.DiagnosisDetailId,
    d.ListDiagnosisProjectId,
    d.PDFHttpUrl,
    d.SendMailDt,
    d.UserNO,
    d.ReportNO,
    p.AnimalName,
    p.MasterName,
    tt.TestTypeName
FROM lb_diagnosisdetail d
LEFT JOIN lb_listdiagnosisproject p ON d.ListDiagnosisProjectId = p.ListDiagnosisProjectId
LEFT JOIN lb_infotesttype tt ON d.InfoTestTypeId = tt.InfoTestTypeId
WHERE d.IsSendMail = 1
  AND d.SendMailDt >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY d.SendMailDt DESC
LIMIT 50;
```

---

## 🔧 PDF生成流程说明

### PDF生成时机

根据代码分析，PDF在以下时机生成：

1. **触发点：** `LbDiagnosisdetail` 模型更新时（`beforeUpdate` 事件）
   - **代码位置：** `application/admin/model/LbDiagnosisdetail.php` 第88-108行

2. **生成条件：**
   - `IsSendMail` 字段被设置为 `1`
   - `SendMailDt` 发送邮件时间有效（大于2000年）

3. **生成方式：**
   - 使用队列异步生成（`wkhtmltopdf`）
   - HTML源来自 `ReportResult` 字段
   - 生成后路径自动保存到 `PDFHttpUrl` 字段

### PDF生成代码流程

```php
// 1. 在更新诊断详情时触发
// application/admin/model/LbDiagnosisdetail.php:106
$model->PDFHttpUrl = GenterPdf($model, "", "PDFHttpUrl");

// 2. 生成PDF路径和文件名
// application/common.php:833
function GenterPdf($model, $pdftitle, $keyword = "", $sendmail = "")
{
    $httpFloderPath = "/Content/FilePdf/" .
                      $model->ListDiagnosisProjectId . "_" .
                      $model->DiagnosisDetailId . "/";

    // 生成文件名...
    $pdftitle = "2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf";

    return [
        "PDFHttpUrl" => $httpFloderPath . $pdftitle,
        "pdfTitle" => $pdftitle,
    ];
}

// 3. 调用wkhtmltopdf生成PDF
// application/common.php:800
\think\Queue::push("app\admin\job\wkhtmltopdf", $cmd);
```

---

## 📱 微信小程序开发指引

### 方案A：通过API接口获取PDF列表（推荐）

#### 需要后端提供的API接口

**接口1：获取用户的报告列表**
```
GET /api/reports/list
请求参数：
  - token: 用户登录token
  - page: 页码（默认1）
  - limit: 每页数量（默认10）

返回数据示例：
{
  "code": 1,
  "msg": "成功",
  "data": {
    "total": 100,
    "list": [
      {
        "DiagnosisDetailId": 6789,
        "ListDiagnosisProjectId": 12345,
        "PDFHttpUrl": "/Content/FilePdf/12345_6789/2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf",
        "pdf_full_url": "https://laboklin.cn/Content/FilePdf/12345_6789/2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf",
        "IsSendMail": 1,
        "SendMailDt": "2025-06-06 14:30:00",
        "UserNO": "55550-24541",
        "ReportNO": "2505-Q-34439",
        "AnimalName": "小五",
        "MasterName": "廖",
        "TestTypeName": "血常规检查"
      }
    ]
  }
}
```

**SQL查询示例（后端实现）：**
```sql
SELECT
    d.DiagnosisDetailId,
    d.ListDiagnosisProjectId,
    d.PDFHttpUrl,
    d.IsSendMail,
    d.SendMailDt,
    d.UserNO,
    d.ReportNO,
    p.AnimalName,
    p.MasterName,
    tt.TestTypeName
FROM lb_diagnosisdetail d
LEFT JOIN lb_listdiagnosisproject p ON d.ListDiagnosisProjectId = p.ListDiagnosisProjectId
LEFT JOIN lb_infotesttype tt ON d.InfoTestTypeId = tt.InfoTestTypeId
WHERE d.UserNO = ?  -- 用户编号
  AND d.IsSendMail = 1
  AND d.PDFHttpUrl IS NOT NULL
  AND d.PDFHttpUrl != ''
ORDER BY d.SendMailDt DESC
LIMIT ? OFFSET ?;
```

**接口2：获取单个报告详情**
```
GET /api/reports/detail
请求参数：
  - token: 用户登录token
  - id: DiagnosisDetailId（诊断详情ID）

返回数据示例：
{
  "code": 1,
  "msg": "成功",
  "data": {
    "DiagnosisDetailId": 6789,
    "PDFHttpUrl": "/Content/FilePdf/12345_6789/2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf",
    "pdf_full_url": "https://laboklin.cn/Content/FilePdf/12345_6789/2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf",
    "IsSendMail": 1,
    "AnimalName": "小五",
    "MasterName": "廖",
    "SendMailDt": "2025-06-06 14:30:00",
    "ReportResult": "<html>...</html>"  // 报告HTML内容
  }
}
```

### 方案B：小程序直接访问PDF文件

#### 微信小程序配置要求

1. **在微信公众平台配置业务域名**
   - 登录：微信公众平台
   - 路径：开发 → 开发管理 → 开发设置 → 业务域名
   - 添加域名：`laboklin.cn` 或 `www.laboklin.cn`
   - 下载验证文件并上传到服务器网站根目录

2. **创建web-view页面显示PDF**

```javascript
// pages/report-detail/report-detail.js
Page({
  data: {
    pdfUrl: ''
  },

  onLoad(options) {
    const { pdf_url } = options;
    if (pdf_url) {
      this.setData({
        pdfUrl: decodeURIComponent(pdf_url)
      });
    }
  }
});
```

```xml
<!-- pages/report-detail/report-detail.wxml -->
<web-view src="{{pdfUrl}}"></web-view>
```

```json
{
  "navigationBarTitleText": "检测报告",
  "usingComponents": {}
}
```

**使用方式：**
```javascript
// 从列表页跳转到PDF详情页
wx.navigateTo({
  url: `/pages/report-detail/report-detail?pdf_url=${encodeURIComponent(pdf_full_url)}`
});
```

### 方案C：下载PDF后打开

```javascript
// 需要在微信公众平台配置downloadFile合法域名
wx.downloadFile({
  url: 'https://laboklin.cn/Content/FilePdf/12345_6789/2025.06.06,小五,廖,55550-24541,2505-Q-34439.pdf',
  success: function(res) {
    wx.openDocument({
      filePath: res.tempFilePath,
      fileType: 'pdf',
      success: function(res) {
        console.log('打开PDF成功');
      },
      fail: function(err) {
        console.error('打开PDF失败', err);
        wx.showToast({
          title: '打开失败',
          icon: 'none'
        });
      }
    });
  },
  fail: function(err) {
    console.error('下载失败', err);
    wx.showToast({
      title: '下载失败',
      icon: 'none'
    });
  }
});
```

**需要配置的域名：**
- 业务域名：`laboklin.cn`
- downloadFile合法域名：`https://laboklin.cn`

---

## ⚠️ 重要注意事项

### 1. 权限验证

- 所有API接口必须验证用户身份（token验证）
- 确保用户只能访问自己的报告（通过UserNO或用户ID关联）
- 建议添加访问日志记录

### 2. PDF访问控制

当前系统PDF文件直接存储在public目录下，任何人都可以直接访问。

**建议增强安全：**
- 添加token参数验证
- 使用临时访问链接（带时效性）
- 或通过PHP接口读取文件并输出（添加权限验证）

### 3. 文件路径处理

- PDFHttpUrl字段存储的是相对路径（以 `/Content/` 开头）
- 需要拼接域名前缀：`https://laboklin.cn` + `PDFHttpUrl`
- 示例：`https://laboklin.cn/Content/FilePdf/12345_6789/xxx.pdf`

### 4. 判断PDF已生成的推荐方法

**优先级排序：**
1. **首选：** `IsSendMail = 1 AND PDFHttpUrl != ''`
2. **次选：** `PDFHttpUrl != '' AND PDFHttpUrl != '/Content/FilePdf/'`
3. **辅助判断：** `SendMailDt > '2000-01-01'`

### 5. PDF生成延迟

由于PDF是通过队列异步生成的，可能存在以下情况：
- `IsSendMail` 已设置为1，但PDF还未完全生成
- 建议在获取PDF地址后，先验证文件是否存在

**验证方法（小程序端）：**
```javascript
wx.request({
  url: pdfUrl,
  method: 'HEAD',
  success: function(res) {
    if (res.statusCode === 200) {
      // PDF文件存在
    } else {
      // PDF文件不存在
    }
  }
});
```

---

## 📞 联系与支持

如有技术问题，请联系开发团队。

---

## 📝 附录：相关代码文件位置

### 核心代码文件

| 文件路径 | 说明 |
|---------|------|
| `application/common.php` | PDF生成函数（700-880行） |
| `application/admin/model/LbDiagnosisdetail.php` | 诊断详情模型（包含PDF生成触发） |
| `application/admin/controller/project/LbListdiagnosisproject.php` | 项目列表控制器 |
| `application/index/controller/Project/View.php` | 前端报告查看控制器 |
| `application/admin/job/wkhtmltopdf.php` | PDF生成队列任务 |

### 数据库表

| 表名 | 说明 |
|------|------|
| `lb_diagnosisdetail` | 诊断详情主表（存储PDF信息） |
| `lb_diagnosisdetailinfo` | 诊断详情信息表 |
| `lb_listdiagnosisproject` | 诊断项目列表 |
| `lb_infotesttype` | 检测类型字典表 |
| `lb_infohospital` | 医院信息表 |
| `lb_infoanimal` | 动物信息表 |

---

**文档版本：** v1.0
**最后更新：** 2025-01-02
**维护者：** Laboklin开发团队
