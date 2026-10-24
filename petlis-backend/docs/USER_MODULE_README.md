# 用户信息管理模块使用文档

## 概述

用户信息管理模块提供了用户信息的查询、更新和头像上传功能。所有接口都需要JWT认证。

## 已实现的功能

### 1. DTO定义

#### UserUpdateRequest.java
用户信息更新请求DTO，包含以下字段：
- nickname (昵称)
- avatarUrl (头像URL)
- mobile (手机号)
- email (邮箱)
- gender (性别: 0=未知, 1=男, 2=女)
- birthday (生日)
- bio (格言)

#### UserInfoResponse.java
用户信息响应DTO，包含：
- 用户基本信��（ID、昵称、手机号、邮箱等）
- 关联的医院信息（医院ID、医院名称、地址、电话等）

### 2. Service层

#### UserService接口
定义了以下方法：
- `getUserProfile()` - 获取当前用户信息
- `updateUserProfile(UserUpdateRequest request)` - 更新用户信息
- `uploadAvatar(MultipartFile file)` - 上传头像

#### UserServiceImpl��现类
实现了以下核心逻辑：
- 从SecurityContext获取当前用户ID
- 查询User表获取用户信息
- 关联查询InfoHospital表获取医院信息
- 验证不允许修改openid、hospitalId等核心字段
- 文件上传验证（类型、大小）
- 自动更新updatetime时间戳

### 3. Controller层

#### UserController
提供了以下API接口：
- `GET /api/user/profile` - 获取当前用户信息
- `PUT /api/user/profile` - 更新用户信息
- `POST /api/user/avatar` - 上传头像

#### FileController
提供了文件访问接口：
- `GET /api/files/avatars/{filename}` - 访问上传的头像文件

## 配置说明

### application.yml配置

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 2MB        # 单个文件最大2MB
      max-request-size: 2MB     # 请求最大2MB
      enabled: true

app:
  file:
    upload-path: /data/petlis/uploads    # 文件上传路径
    max-size: 2097152                     # 2MB
    access-url: http://localhost:8080/api/files  # 文件访问URL前缀
```

### SecurityConfig配置

已添加以下公开访问路径：
- `/files/**` - 访问上传的文件

## API接口说明

### 1. 获取用户信息

**接口:** `GET /api/user/profile`

**请求头:**
```
Authorization: Bearer {token}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "id": 1,
    "nickname": "张三",
    "mobile": "13800138000",
    "email": "user@example.com",
    "avatarUrl": "http://localhost:8080/api/files/avatars/xxx.jpg",
    "gender": 1,
    "birthday": "1990-01-01",
    "bio": "热爱生活",
    "openid": "oxxxxxxxxxxxxxx",
    "hospitalId": 1,
    "hospital": {
      "hospitalId": 1,
      "hospitalName": "宠物医院",
      "place": "北京市朝阳区",
      "tel": "010-12345678",
      "cityId": 1
    }
  }
}
```

### 2. 更新用户信息

**接口:** `PUT /api/user/profile`

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体:**
```json
{
  "nickname": "李四",
  "mobile": "13900139000",
  "email": "lisi@example.com",
  "gender": 1,
  "birthday": "1990-01-01",
  "bio": "新的格言"
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 1,
    "nickname": "李四",
    "mobile": "13900139000",
    "email": "lisi@example.com",
    ...
  }
}
```

### 3. 上传头像

**接口:** `POST /api/user/avatar`

**请求头:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**请求参数:**
- file: 头像图片文件（支持jpg、jpeg、png格式，最大2MB）

**响应示例:**
```json
{
  "code": 200,
  "message": "上传成功",
  "data": "http://localhost:8080/api/files/avatars/uuid-filename.jpg"
}
```

## 验证规则

### 字段验证

1. **昵称**
   - 最大长度: 50个字符

2. **头像URL**
   - 最大长度: 255个字符

3. **手机号**
   - 格式: 必须符合中国大陆手机号格式（1开头，第二位3-9，共11位）
   - 唯一性: 不能与其他用户重复

4. **邮箱**
   - 最大长度: 100个字符

5. **格言**
   - 最大长度: 100个字符

### 文件上传验证

1. **文件类型**
   - 支持的格式: image/jpeg, image/jpg, image/png

2. **文件大小**
   - 最大大小: 2MB

3. **文件名**
   - 使用UUID生成唯一文件名
   - 保留原始文件扩展名

## 安全措施

1. **核心字段保护**
   - 不允许修改openid、hospitalId等核心字段
   - 只能更新自己的信息（从JWT token获取用户ID）

2. **文件上传安全**
   - 验证文件类型和大小
   - 生成唯一文件名防止文件名冲突
   - 防止路径遍历攻击

3. **访问控制**
   - 所有用户接口都需要JWT认证
   - 文件访问接口公开，但路径经过验证

## 使用示例

### 使用curl测试接口

```bash
# 1. 获取用户信息
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"

# 2. 更新用户信息
curl -X PUT http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "新昵称",
    "mobile": "13800138000"
  }'

# 3. 上传头像
curl -X POST http://localhost:8080/api/user/avatar \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/avatar.jpg"
```

## 注意事项

1. **文件存储**
   - 默认上传路径: `/data/petlis/uploads`
   - 生产环境需要确保该目录存在且有写入权限
   - 建议使用CDN或对象存储服务（如阿里云OSS）

2. **URL配置**
   - 开发环境: `http://localhost:8080/api/files`
   - 生产环境: 需要修改为实际的域名或CDN地址

3. **数据库**
   - avatar字段存储完整的URL
   - 便于后续迁移到CDN

4. **事务管理**
   - 使用`@Transactional`确保数据一致性
   - 更新用户信息时自动更新updatetime字段

## 测试清单

- [ ] 可以成功获取用户信息（包含医院信息）
- [ ] 可以更新用户昵称
- [ ] 可以更新用户手机号（验证唯一性）
- [ ] 可以上传头像图片
- [ ] 文件类型验证生效（非图片文件被拒绝）
- [ ] 文件大小验证生效（超过2MB被拒绝）
- [ ] 不允许修改openid字段
- [ ] 不允许修改hospitalId字段
- [ ] 上传的头像可以正常访问
- [ ] 日志记录完整

## 后续优化建议

1. **文件存储优化**
   - 集成阿里云OSS或腾讯云COS
   - 实现图片缩略图功能
   - 添加图片压缩功能

2. **功能增强**
   - 支持批量上传
   - 支持图片裁剪
   - 添加人脸识别功能

3. **性能优化**
   - 添加缓存（Redis）
   - 异步处理文件上传
   - 使用CDN加速文件访问
