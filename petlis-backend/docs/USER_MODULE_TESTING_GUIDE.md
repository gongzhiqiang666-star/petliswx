# 用户信息管理模块测试指南

## 实现完成总结

已成功实现User Story 6 - 用户信息管理的所有功能，包括：

### 已完成的功能模块

#### 1. DTO层 (T117-T118) ✓

**UserUpdateRequest.java**
- 路径: `/src/main/java/com/petlis/dto/request/UserUpdateRequest.java`
- 功能: 用户信息更新请求DTO
- 字段:
  - nickname (昵称, 最大50字符)
  - avatarUrl (头像URL, 最大255字符)
  - mobile (手机号, 正则验证)
  - email (邮箱, 最大100字符)
  - gender (性别: 0=未知, 1=男, 2=女)
  - birthday (生日)
  - bio (格言, 最大100字符)
- 验证: 使用Jakarta Validation注解

**UserInfoResponse.java**
- 路径: `/src/main/java/com/petlis/dto/response/UserInfoResponse.java`
- 功能: 用户信息响应DTO
- 包含内容:
  - 用户所有基本信息
  - 关联的医院信息（嵌套对象HospitalInfo）
  - 医院信息包含: hospitalId, hospitalName, place, tel, cityId

#### 2. Service层 (T119-T120) ✓

**UserService.java**
- 路径: `/src/main/java/com/petlis/service/UserService.java`
- 功能: 用户服务接口
- 方法定义:
  - `getUserProfile()` - 获取当前用户信息
  - `updateUserProfile(UserUpdateRequest request)` - 更新用户信息
  - `uploadAvatar(MultipartFile file)` - 上传头像

**UserServiceImpl.java**
- 路径: `/src/main/java/com/petlis/service/impl/UserServiceImpl.java`
- 功能: 用户服务实现类
- 核心实现:
  - 从SecurityContext获取当前用户ID
  - 查询User表获取用户信息
  - 关联查询InfoHospital表获取医院信息
  - 转换为UserInfoResponse返回
  - 更新时验证不允许修改openid、hospitalId
  - 手机号唯一性验证
  - 自动更新updatetime时间戳
  - 文件上传验证（类型、大小）
  - 生成唯一文件名（UUID）

#### 3. Controller层 (T121) ✓

**UserController.java**
- 路径: `/src/main/java/com/petlis/controller/api/UserController.java`
- 功能: 用户管理控制器
- API接口:
  - `GET /api/user/profile` - 获取当前用户信息
  - `PUT /api/user/profile` - 更新用户信息
  - `POST /api/user/avatar` - 上传头像
- 特性:
  - 所有接口需要JWT认证
  - 统一返回Result格式
  - Swagger API文档注解
  - 操作日志注解

#### 4. 文件上传功能 (T122-T124) ✓

**FileController.java**
- 路径: `/src/main/java/com/petlis/controller/api/FileController.java`
- 功能: 文件访问控制器
- API接口:
  - `GET /api/files/avatars/{filename}` - 访问上传的头像文件
- 安全特性:
  - 路径遍历攻击防护
  - 文件存在性检查
  - 正确的Content-Type设置

**配置更新**
- `application.yml`:
  - 文件上传大小限制: 2MB
  - 文件上传路径: `/data/petlis/uploads`
  - 文件访问URL前缀: `http://localhost:8080/api/files`

- `SecurityConfig.java`:
  - 添加`/files/**`到公开访问路径
  - 允许无认证访问上传的文件

## 测试用例

### 测试准备

1. 启动应用
2. 使用微信登录获取JWT token
3. 使用token访问以下接口

### 测试用例1: 获取用户信息

**请求:**
```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期结果:**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "id": 1,
    "nickname": "微信用户",
    "mobile": null,
    "email": null,
    "avatarUrl": null,
    "gender": 0,
    "birthday": null,
    "bio": null,
    "openid": "oxxxxxxxxxxxxxx",
    "hospitalId": 1,
    "hospital": {
      "hospitalId": 1,
      "hospitalName": "Laboklin",
      "place": "北京市朝阳区",
      "tel": "010-12345678",
      "cityId": 1
    }
  }
}
```

**验证点:**
- [ ] 返回用户完整信息
- [ ] 包含关联的医院信息
- [ ] 时间戳格式正确

### 测试用例2: 更新用户昵称

**请求:**
```bash
curl -X PUT http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "张三"
  }'
```

**预期结果:**
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 1,
    "nickname": "张三",
    ...
  }
}
```

**验证点:**
- [ ] 昵称更新成功
- [ ] updatetime自动更新

### 测试用例3: 更新手机号

**请求:**
```bash
curl -X PUT http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "mobile": "13800138000"
  }'
```

**预期结果:**
成功更新

**验证点:**
- [ ] 手机号格式正确才能更新
- [ ] 格式错误返回错误信息
- [ ] 手机号被其他用户占用时返回错误

### 测试用例4: 更新头像URL

**请求:**
```bash
curl -X PUT http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "avatarUrl": "http://example.com/avatar.jpg"
  }'
```

**预期结果:**
头像URL更新成功

### 测试用例5: 上传头像 - 成功

**请求:**
```bash
curl -X POST http://localhost:8080/api/user/avatar \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/avatar.jpg"
```

**预期结果:**
```json
{
  "code": 200,
  "message": "上传成功",
  "data": "http://localhost:8080/api/files/avatars/uuid-filename.jpg"
}
```

**验证点:**
- [ ] 文件上传成功
- [ ] 返回正确的URL
- [ ] 数据库avatar字段已更新
- [ ] 可以通过URL访问图片

### 测试用例6: 上传头像 - 文件类型错误

**请求:**
```bash
curl -X POST http://localhost:8080/api/user/avatar \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/document.pdf"
```

**预期结果:**
```json
{
  "code": 500,
  "message": "上传失败: 只支持jpg、jpeg、png格式的图片"
}
```

**验证点:**
- [ ] 非图片文件被拒绝
- [ ] 返回明确的错误信息

### 测试用例7: 上传头像 - 文件过大

**请求:**
```bash
curl -X POST http://localhost:8080/api/user/avatar \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/large-image.jpg"
```

**预期结果:**
```json
{
  "code": 500,
  "message": "上传失败: 图片大小不能超过2MB"
}
```

**验证点:**
- [ ] 超过2MB的文件被拒绝
- [ ] 返回明确的错误信息

### 测试用例8: 访问上传的头像

**请求:**
```bash
curl -X GET http://localhost:8080/api/files/avatars/uuid-filename.jpg
```

**预期结果:**
返回图片文件内容，Content-Type: image/jpeg

**验证点:**
- [ ] 可以访问上传的图片
- [ ] Content-Type正确
- [ ] 不需要认证即可访问

### 测试用例9: 尝试修改核心字段（应该失败）

**请求:**
```bash
curl -X PUT http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "openid": "new_openid"
  }'
```

**预期结果:**
openid字段被忽略，不会更新

**验证点:**
- [ ] 核心字段不能通过接口修改
- [ ] 其他字段可以正常更新

### 测试用例10: 字段验证

**请求:**
```bash
curl -X PUT http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "mobile": "12345",
    "nickname": "这是一个超过五十个字符限制的非常非常非常非常非常非常非常非常非常非常非常长的昵称"
  }'
```

**预期结果:**
```json
{
  "code": 400,
  "message": "参数验证失败"
}
```

**验证点:**
- [ ] 手机号格式验证生效
- [ ] 字段长度限制生效

## 性能测试

### 并发测试

使用JMeter或类似工具进行并发测试：

1. **获取用户信息**
   - 并发用户: 100
   - 循环次数: 10
   - 预期响应时间: < 100ms
   - 预期成功率: 100%

2. **更新用户信息**
   - 并发用户: 50
   - 循环次数: 5
   - 预期响应时间: < 200ms
   - 预期成功率: 100%

3. **上传头像**
   - 并发用户: 20
   - 循环次数: 3
   - 预期响应时间: < 500ms
   - 预期成功率: 100%

## 文件上传目录结构

```
/data/petlis/uploads/
└── avatars/
    ├── uuid1-filename1.jpg
    ├── uuid2-filename2.png
    └── uuid3-filename3.jpeg
```

**注意:**
- 首次运行前需要创建目录: `mkdir -p /data/petlis/uploads/avatars`
- 确保应用有写入权限

## 部署检查清单

### 开发环境

- [ ] application.yml配置正确
- [ ] 上传目录存在并有写入权限
- [ ] 数据库连接正常
- [ ] JWT配置正确
- [ ] 文件大小限制配置正确

### 生产环境

- [ ] 修改文件访问URL为实际域名
- [ ] 配置CDN或对象存储服务
- [ ] 调整文件上传大小限制
- [ ] 配置文件存储路径（建议使用独立的存储服务器）
- [ ] 添加文件清理定时任务（删除未使用的文件）
- [ ] 配置备份策略

## 已知限制

1. **文件存储**
   - 当前使用本地文件系统
   - 不支持分布式部署
   - 建议生产环境使用OSS或COS

2. **图片处理**
   - 没有图片压缩功能
   - 没有缩略图生成
   - 没有图片格式转换

3. **安全性**
   - 没有图片内容审核
   - 没有病毒扫描
   - 建议添加图片审核服务

## 后续优化建议

1. **集成对象存储**
   - 阿里云OSS
   - 腾讯云COS
   - AWS S3

2. **图片处理**
   - 添加图片压缩
   - 生成多尺寸缩略图
   - 支持图片裁剪

3. **性能优化**
   - 添加Redis缓存
   - 使用CDN加速
   - 异步处理文件上传

4. **功能增强**
   - 支持多图片上传
   - 支持图片水印
   - 支持人脸识别

## 总结

用户信息管理模块已完全实现，包括：
- ✓ DTO定义（UserUpdateRequest, UserInfoResponse）
- ✓ Service层（UserService接口及实现）
- ✓ Controller层（UserController, FileController）
- ✓ 文件上传功能（类型验证、大小验证、安全措施）
- ✓ 配置更新（application.yml, SecurityConfig）
- ✓ 完整的API文档和测试指南

所有功能已按照User Story 6的要求实现，可以进行测试验收。
