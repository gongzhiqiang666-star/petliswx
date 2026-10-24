# API契约文档

本目录包含PETLIS后端API服务的所有接口契约定义。

## 接口列表

### 1. 认证相关接口 (auth-api.md)
- POST /api/auth/wechat/login - 微信小程序登录
- POST /api/auth/refresh - 刷新Token
- POST /api/auth/logout - 登出

### 2. 宠物管理接口 (pet-api.md)
- POST /api/pets - 创建宠物档案
- GET /api/pets - 查询宠物列表(支持分页和搜索)
- GET /api/pets/{id} - 获取宠物详情
- PUT /api/pets/{id} - 更新宠物档案
- DELETE /api/pets/{id} - 删除宠物(逻辑删除)

### 3. 检测申请接口 (order-api.md)
- POST /api/orders - 创建检测申请
- GET /api/orders - 查询申请列表(支持状态筛选)
- GET /api/orders/{id} - 获取申请详情
- PUT /api/orders/{id} - 更新检测申请
- PUT /api/orders/{id}/status - 更新申请状态
- POST /api/orders/{id}/items - 添加检测项目
- DELETE /api/orders/{id}/items/{itemId} - 删除检测项目

### 4. 报告查询接口 (report-api.md)
- GET /api/reports - 查询报告列表
- GET /api/reports/{id} - 获取报告详情
- GET /api/reports/{id}/download - 下载PDF报告

### 5. 数据字典接口 (dictionary-api.md)
- GET /api/dictionary/animals - 获取动物物种列表
- GET /api/dictionary/varieties - 获取品种列表(支持按物种筛选)
- GET /api/dictionary/sexes - 获取性别列表
- GET /api/dictionary/sample-types - 获取样本类型列表
- GET /api/dictionary/test-types - 获取检测项目列表
- GET /api/dictionary/test-types/{id}/price - 获取检测项目价格
- GET /api/dictionary/cities - 获取城市列表
- GET /api/dictionary/hospitals - 获取医院列表

### 6. 用户管理接口 (user-api.md)
- GET /api/user/profile - 获取当前用户信息
- PUT /api/user/profile - 更新用户信息
- POST /api/user/avatar - 上传头像

## 统一响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

### 失败响应
```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": null
}
```

## 统一错误码

- 200: 成功
- 400: 请求参数错误
- 401: 未认证
- 403: 无权限
- 404: 资源不存在
- 500: 服务器内部错误

## 认证方式

所有业务接口(除/auth/*外)需要在HTTP Header中携带Token:

```
Authorization: Bearer {jwt_token}
```

详细的接口定义请参考各个API文档文件。
