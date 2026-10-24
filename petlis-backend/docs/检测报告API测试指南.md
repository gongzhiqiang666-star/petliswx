# 检测报告API测试指南

## 概述

本文档提供检测报告查询与展示功能的API测试指南,包括所有端点的使用方法和示例。

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Bearer Token
- **Content-Type**: `application/json`

## API端点

### 1. 查询检测报告列表

#### 端点
```
GET /api/reports
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| petId | Integer | 否 | 宠物ID | 123 |
| status | Integer | 否 | 检测状态(0=检测中,其他=已完成) | 0 |
| startDate | String | 否 | 开始日期(yyyy-MM-dd) | 2024-01-01 |
| endDate | String | 否 | 结束日期(yyyy-MM-dd) | 2024-12-31 |
| pageNum | Integer | 否 | 页码(从0开始) | 0 |
| pageSize | Integer | 否 | 每页大小 | 10 |

#### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "content": [
      {
        "reportId": 1,
        "orderId": 100,
        "genterNO": "55550-20241227001",
        "animalName": "小白",
        "animalType": "金毛",
        "testTypeName": "血常规检查",
        "diagnosisStatus": 1,
        "statusText": "已完成",
        "testPrice": 150.00,
        "pdfHttpUrl": "http://localhost:8080/api/files/report_001.pdf",
        "createTime": "2024-12-27 10:00:00",
        "collectDt": "2024-12-26 00:00:00",
        "reportNO": "R20241227001"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalPages": 5,
    "totalElements": 50,
    "first": true,
    "last": false
  }
}
```

#### cURL示例

```bash
# 基础查询
curl -X GET "http://localhost:8080/api/reports?pageNum=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 按宠物ID查询
curl -X GET "http://localhost:8080/api/reports?petId=123&pageNum=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 按状态查询(检测中)
curl -X GET "http://localhost:8080/api/reports?status=0&pageNum=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 按日期范围查询
curl -X GET "http://localhost:8080/api/reports?startDate=2024-01-01&endDate=2024-12-31&pageNum=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 组合查询
curl -X GET "http://localhost:8080/api/reports?petId=123&status=1&startDate=2024-01-01&endDate=2024-12-31&pageNum=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 2. 获取检测报告详情

#### 端点
```
GET /api/reports/{id}
```

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| id | Integer | 是 | 报告ID(检测明细ID) | 1 |

#### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "reportId": 1,
    "orderId": 100,
    "genterNO": "55550-20241227001",
    "animalName": "小白",
    "animalType": "金毛",
    "dictSexName": "公",
    "age": "3岁",
    "animalWeight": "30kg",
    "chipNO": "156789012345678",
    "infoTestTypeId": 10,
    "testTypeName": "血常规检查",
    "testPrice": 150.00,
    "diagnosisStatus": 1,
    "statusText": "已完成",
    "reportResult": "<html>...</html>",
    "mailReport": "<html>...</html>",
    "pdfHttpUrl": "http://localhost:8080/api/files/report_001.pdf",
    "createTime": "2024-12-27 10:00:00",
    "collectDt": "2024-12-26 00:00:00",
    "receiveSampleDt": "2024-12-26 10:00:00",
    "sendReportDt": "2024-12-27 15:00:00",
    "reportNO": "R20241227001",
    "masterName": "张三",
    "doctorName": "李医生",
    "hospitalId": 5,
    "hospitalName": "宠物医院"
  }
}
```

#### cURL示例

```bash
curl -X GET "http://localhost:8080/api/reports/1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 3. 下载PDF报告

#### 端点
```
GET /api/reports/{id}/download
```

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| id | Integer | 是 | 报告ID(检测明细ID) | 1 |

#### 响应

- **Content-Type**: `application/pdf`
- **Content-Disposition**: `attachment; filename="报告_55550-20241227001.pdf"`

#### cURL示例

```bash
# 直接下载文件
curl -X GET "http://localhost:8080/api/reports/1/download" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -o report.pdf

# 查看响应头
curl -I -X GET "http://localhost:8080/api/reports/1/download" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 4. 获取PDF报告URL

#### 端点
```
GET /api/reports/{id}/pdf-url
```

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| id | Integer | 是 | 报告ID(检测明细ID) | 1 |

#### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "http://localhost:8080/api/files/report_001.pdf"
}
```

#### cURL示例

```bash
curl -X GET "http://localhost:8080/api/reports/1/pdf-url" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 测试场景

### 场景1: 查询所有报告
```bash
curl -X GET "http://localhost:8080/api/reports?pageNum=0&pageSize=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 场景2: 查询检测中的报告
```bash
curl -X GET "http://localhost:8080/api/reports?status=0&pageNum=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 场景3: 查询已完成的报告
```bash
curl -X GET "http://localhost:8080/api/reports?status=1&pageNum=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 场景4: 查询特定宠物的报告
```bash
curl -X GET "http://localhost:8080/api/reports?petId=123&pageNum=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 场景5: 查询指定日期范围的报告
```bash
curl -X GET "http://localhost:8080/api/reports?startDate=2024-01-01&endDate=2024-12-31&pageNum=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 场景6: 查看报告详情
```bash
curl -X GET "http://localhost:8080/api/reports/1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 场景7: 下载PDF报告
```bash
curl -X GET "http://localhost:8080/api/reports/1/download" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -o report.pdf
```

---

## 使用Postman测试

### 导入集合

创建以下Postman集合:

```json
{
  "info": {
    "name": "PETLIS检测报告API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "查询报告列表",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/reports?pageNum=0&pageSize=10",
          "host": ["{{baseUrl}}"],
          "path": ["reports"],
          "query": [
            {
              "key": "pageNum",
              "value": "0"
            },
            {
              "key": "pageSize",
              "value": "10"
            }
          ]
        }
      }
    },
    {
      "name": "获取报告详情",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/reports/1",
          "host": ["{{baseUrl}}"],
          "path": ["reports", "1"]
        }
      }
    },
    {
      "name": "下载PDF报告",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/reports/1/download",
          "host": ["{{baseUrl}}"],
          "path": ["reports", "1", "download"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api"
    },
    {
      "key": "token",
      "value": "YOUR_JWT_TOKEN"
    }
  ]
}
```

---

## 常见错误处理

### 1. 未授权
```json
{
  "code": 401,
  "message": "未授权访问"
}
```
**解决方案**: 确保请求头包含有效的JWT Token

### 2. 报告不存在
```json
{
  "code": 500,
  "message": "报告不存在"
}
```
**解决方案**: 检查报告ID是否正确

### 3. PDF报告不存在
```json
{
  "code": 500,
  "message": "PDF报告不存在"
}
```
**解决方案**: 检查检测明细的pdfHttpUrl字段是否有值

### 4. 参数验证失败
```json
{
  "code": 400,
  "message": "参数验证失败"
}
```
**解决方案**: 检���请求参数格式是否正确

---

## 性能测试

### 查询性能测试
```bash
# 使用Apache Bench进行压力测试
ab -n 1000 -c 10 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8080/api/reports?pageNum=0&pageSize=10"
```

### 下载性能测试
```bash
# 测试大文件下载
time curl -X GET "http://localhost:8080/api/reports/1/download" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -o large_report.pdf
```

---

## 数据准备SQL

### 测试数据准备
```sql
-- 插入检测申请
INSERT INTO lin_lb_listdiagnosisproject (
  ListDiagnosisProjectId, InfoHospitalId, AnimalName, AnimalType,
  SampleStatus, CreateDt
) VALUES
(100, 5, '小白', '金毛', 3, NOW());

-- 插入检测明细(已完成)
INSERT INTO lin_lb_diagnosisdetail (
  DiagnosisDetailId, ListDiagnosisProjectId, GenterNO,
  InfoTestTypeId, DiagnosisStatus, IsCheckPass,
  PDFHttpUrl, TestPrice, CreateDt
) VALUES
(1, 100, '55550-20241227001', 10, 1, 0,
 'http://localhost:8080/api/files/report_001.pdf', 150.00, NOW());

-- 插入检测明细(检测中)
INSERT INTO lin_lb_diagnosisdetail (
  DiagnosisDetailId, ListDiagnosisProjectId, GenterNO,
  InfoTestTypeId, DiagnosisStatus, IsCheckPass,
  TestPrice, CreateDt
) VALUES
(2, 100, '55550-20241227002', 11, 0, 0, 200.00, NOW());

-- 插入报告内容
INSERT INTO lin_lb_diagnosisdetail_info (
  diagnosisdetail_id, MailReport, ReportResult
) VALUES
(1, '<html>邮件报告内容</html>', '<html>检测结果内容</html>');
```

---

## Swagger UI测试

访问Swagger UI进行可视化测试:
```
http://localhost:8080/api/swagger-ui.html
```

在Swagger UI中:
1. 点击右上角"Authorize"按钮
2. 输入JWT Token (格式: `Bearer YOUR_TOKEN`)
3. 找到"检测报告管理"标签
4. 展开需要测试的API
5. 点击"Try it out"
6. 填写参数
7. 点击"Execute"执行请求

---

## 注意事项

1. **JWT Token**: 所有请求都需要在Header中携带有效的JWT Token
2. **分页参数**: pageNum从0开始,不是1
3. **日期格式**: 使用yyyy-MM-dd格式
4. **状态值**: 0表示检测中,其他值表示已完成
5. **文件下载**: 建议使用异步下载,避免阻塞主线程
6. **医院权限**: 当前实现未添加医院权限过滤,需要根据业务需求添加

---

**文档版本**: 1.0.0
**更新时间**: 2024-12-27
**作者**: PETLIS Team
