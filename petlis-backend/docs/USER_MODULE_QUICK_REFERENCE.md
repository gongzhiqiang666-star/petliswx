# 用户信息管理模�� - 快速参考指南

## API接口快速参考

### 1. 获取用户信息
```http
GET /api/user/profile
Authorization: Bearer {token}
```

### 2. 更新用户信息
```http
PUT /api/user/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "nickname": "新昵称",
  "mobile": "13800138000",
  "email": "user@example.com",
  "gender": 1,
  "birthday": "1990-01-01",
  "bio": "个人格言"
}
```

### 3. 上传头像
```http
POST /api/user/avatar
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [图片文件]
```

### 4. 访问头像
```http
GET /api/files/avatars/{filename}
```

## cURL命令快速参考

### 获取用户信息
```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 更新昵称
```bash
curl -X PUT http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nickname": "新昵称"}'
```

### 上传头像
```bash
curl -X POST http://localhost:8080/api/user/avatar \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/avatar.jpg"
```

## 配置快速参考

### application.yml关键配置
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 2MB

app:
  file:
    upload-path: /data/petlis/uploads
    access-url: http://localhost:8080/api/files
```

### 环境准备
```bash
# 创建上传目录
mkdir -p /data/petlis/uploads/avatars

# 设置权限（Linux/Mac）
chmod 755 /data/petlis/uploads/avatars
```

## 验证规则快速参考

### 字段验证
- **nickname**: 最大50字符
- **mobile**: 正则 `^1[3-9]\\d{9}$`
- **email**: 最大100字符
- **bio**: 最大100字符
- **avatarUrl**: 最大255字符

### 文件上传验证
- **类型**: image/jpeg, image/jpg, image/png
- **大小**: 最大2MB
- **文件名**: UUID生成

## 常见问题快速参考

### Q1: 如何获取JWT Token?
```bash
# 微信登录获取token
curl -X POST http://localhost:8080/api/auth/wechat/login \
  -H "Content-Type: application/json" \
  -d '{"code": "微信登录code"}'
```

### Q2: 文件上传失败怎么办?
1. 检查文件类型（只支持jpg、jpeg、png）
2. 检查文件大小（最大2MB）
3. 检查上传目录是否存在且有写入权限
4. 检查JWT Token是否有效

### Q3: 如何修改上传路径?
修改`application.yml`中的配置：
```yaml
app:
  file:
    upload-path: /your/custom/path
```

### Q4: 如何修改文件访问URL?
修改`application.yml`中的配置：
```yaml
app:
  file:
    access-url: https://your-domain.com/files
```

## 测试快速参考

### 运行自动化测试
```bash
# 设置Token
export TOKEN='your-jwt-token'

# 运行测试
./test_user_api.sh
```

### 手动测试检查清单
- [ ] 可以获取用户信息
- [ ] 可以更新昵称
- [ ] 可以更新手机号
- [ ] 可以上传头像
- [ ] 可以访问上传的头像
- [ ] 文件类型验证生效
- [ ] 文件大小验证生效
- [ ] 不能修改核心字段

## 文件结构快速参考

```
petlis-backend/
├── src/main/java/com/petlis/
│   ├── dto/
│   │   ├── request/
│   │   │   └── UserUpdateRequest.java      # 用户更新请求DTO
│   │   └── response/
│   │       └── UserInfoResponse.java       # 用户信息响应DTO
│   ├── service/
│   │   ├── UserService.java               # 用户服务接口
│   │   └── impl/
│   │       └── UserServiceImpl.java       # 用户服务实现
│   ├── controller/
│   │   └── api/
│   │       ├── UserController.java        # 用户控制器
│   │       └── FileController.java        # 文件控制器
│   └── config/
│       └── SecurityConfig.java            # 安全配置（已修改）
├── src/main/resources/
│   └── application.yml                    # 应用配置（已修改）
├── USER_MODULE_README.md                  # 使用文档
├── USER_MODULE_TESTING_GUIDE.md           # 测试指南
├── USER_STORY_6_IMPLEMENTATION_SUMMARY.md # 实现总结
├── USER_MODULE_FILES_INDEX.md             # 文件索引
├── USER_MODULE_QUICK_REFERENCE.md         # 快速参考（本文件）
└── test_user_api.sh                       # 测试脚本
```

## 数据库字段快速参考

### User表 (lin_user)
| 字段 | 类型 | 说明 | 可修改 |
|------|------|------|--------|
| id | int | 用户ID | ✗ |
| openid | varchar(100) | 微信openid | ✗ |
| hospital_id | int | 医院ID | ✗ |
| nickname | varchar(50) | 昵称 | ✓ |
| mobile | varchar(11) | 手机号 | ✓ |
| email | varchar(100) | 邮箱 | ✓ |
| avatar | varchar(255) | 头像URL | ✓ |
| gender | tinyint | 性别 | ✓ |
| birthday | varchar | 生日 | ✓ |
| bio | varchar(100) | 格言 | ✓ |

### InfoHospital表 (lin_lb_infohospital)
| 字段 | 类型 | 说明 |
|------|------|------|
| InfoHospitalId | int | 医院ID |
| HospitalName | varchar(250) | 医院名称 |
| Place | text | 医院地址 |
| Tel | varchar(50) | 联系电话 |
| CityId | int | 城市ID |

## 错误码快速参考

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 400 | 参数验证失败 | 检查请求参数格式 |
| 401 | 未认证 | 检查JWT Token |
| 404 | 用户不存在 | 确认用户ID正确 |
| 500 | 业务异常 | 查看具体错误信息 |

## 常用日志查看

### 应用日志
```bash
# 查看用户相关日志
tail -f logs/application.log | grep "用户"

# 查看文件上传日志
tail -f logs/application.log | grep "上传"
```

### Docker日志
```bash
# 查看容器日志
docker logs -f petlis-backend

# 查看最近100行
docker logs --tail 100 petlis-backend
```

## 性能指标快速参考

| 接口 | 预期响应时间 | 并发能力 |
|------|--------------|----------|
| GET /user/profile | < 100ms | 100 QPS |
| PUT /user/profile | < 200ms | 50 QPS |
| POST /user/avatar | < 500ms | 20 QPS |

---

**提示:** 本文档是快速参考指南，详细信息请查看其他文档：
- 使用文档: `USER_MODULE_README.md`
- 测试指南: `USER_MODULE_TESTING_GUIDE.md`
- 文件索引: `USER_MODULE_FILES_INDEX.md`
