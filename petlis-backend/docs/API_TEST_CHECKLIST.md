# 宠物档��管理API测试清单

## 测试前准备

### 1. 数据库准备
```sql
-- 1. 创建宠物档案表
source src/main/resources/db/migration/V1.0.0__create_pet_table.sql

-- 2. 确认字典数据存在
SELECT * FROM lin_lb_infoanimal WHERE IsActive = 1;  -- 物种数据
SELECT * FROM lin_lb_infovariety LIMIT 10;          -- 品种数据
SELECT * FROM lin_lb_dictsex WHERE IsActive = 1;    -- 性别数据
SELECT * FROM lin_lb_infohospital LIMIT 1;          -- 医院数据
```

### 2. 获取JWT Token
```bash
# 1. 微信登录获取token
curl -X POST http://localhost:8080/auth/wechat/login \
  -H "Content-Type: application/json" \
  -d '{
    "code": "YOUR_WECHAT_CODE"
  }'

# 2. 从响应中获取token
# 保存token到环境变量
export JWT_TOKEN="your_jwt_token_here"
```

## API测试用例

### 测试用例1: 创建宠物档案 - 成功

**请求**:
```bash
curl -X POST http://localhost:8080/api/pets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "animalName": "旺财",
    "animalNameEn": "Lucky",
    "infoAnimalId": 1,
    "infoVarietyId": 10,
    "dictSexId": 1,
    "birthday": "2020-01-15",
    "chipNo": "928000000000001",
    "ownerName": "张三",
    "ownerNameEn": "Zhang San"
  }'
```

**预期结果**:
- HTTP状态码: 200
- 响应包含新创建的宠物ID
- 数据库中可以查到该记录

### 测试用例2: 创建宠物档案 - 缺少必填字段

**请求**:
```bash
curl -X POST http://localhost:8080/api/pets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "animalNameEn": "Lucky",
    "infoAnimalId": 1
  }'
```

**预期结果**:
- HTTP状态码: 400 或 500
- 错误信息提示缺少必填字段(animalName, ownerName, dictSexId)

### 测试用例3: 创建宠物档案 - 物种不存在

**请求**:
```bash
curl -X POST http://localhost:8080/api/pets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "animalName": "测试宠物",
    "infoAnimalId": 99999,
    "dictSexId": 1,
    "ownerName": "测试用户"
  }'
```

**预期结果**:
- HTTP状态码: 500
- 错误信息: "指定的物种不存在或已禁用"

### 测试用例4: 分页查询宠物档案

**请求**:
```bash
curl -X GET "http://localhost:8080/api/pets?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**预期结果**:
- HTTP状态码: 200
- 返回分页数据,包含content、totalElements、totalPages等字段
- 只返回当前医院的宠物数据

### 测试用例5: 搜索宠物档案 - 中文名称

**请求**:
```bash
curl -X GET "http://localhost:8080/api/pets/search?name=旺财" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**预期结果**:
- HTTP状态码: 200
- 返回名称包含"旺财"的宠物列表
- 只返回当前医院的宠物数据

### 测试用例6: 搜索宠物档案 - 英文名称

**请求**:
```bash
curl -X GET "http://localhost:8080/api/pets/search?name=Lucky" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**预期结果**:
- HTTP状态码: 200
- 返回英文名包含"Lucky"的宠物列表

### 测试用例7: 查询宠物档案详情

**请求**:
```bash
curl -X GET http://localhost:8080/api/pets/1 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**预期结果**:
- HTTP状态码: 200
- 返回宠物详细信息,包含关联的字典数据名称(infoAnimalName, infoVarietyName等)

### 测试用例8: 查询宠物档案详情 - 不存在

**请求**:
```bash
curl -X GET http://localhost:8080/api/pets/99999 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**预期结果**:
- HTTP状态码: 404
- 错误信息: "宠物档案 not found with id: 99999"

### 测试用例9: 更新宠物档案 - 成功

**请求**:
```bash
curl -X PUT http://localhost:8080/api/pets/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "animalName": "旺财旺财",
    "birthday": "2020-02-15"
  }'
```

**预期结果**:
- HTTP状态码: 200
- 返回更新后的宠物信息
- 数据库中记录已更新

### 测试用例10: 更新宠物档案 - 无权限(其他医院的宠物)

**请求**:
```bash
# 先用医院A的账号创建宠物,获取ID为PET_ID_A
# 然后用医院B的token尝试更新

curl -X PUT http://localhost:8080/api/pets/PET_ID_A \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer HOSPITAL_B_JWT_TOKEN" \
  -d '{
    "animalName": "尝试修改"
  }'
```

**预期结果**:
- HTTP状态码: 500
- 错误信息: "无权限更新此宠物档案"

### 测试用例11: 删除宠物档案 - 成功

**请求**:
```bash
curl -X DELETE http://localhost:8080/api/pets/1 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**预期结果**:
- HTTP状态码: 200
- 数据库中is_active字段设置为0(逻辑删除)
- 再次查询该宠物应返回404或空数据

### 测试用例12: 删除宠物档案 - 不存在

**请求**:
```bash
curl -X DELETE http://localhost:8080/api/pets/99999 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**预期结果**:
- HTTP状态码: 404
- 错误信息: "宠物档案 not found with id: 99999"

### 测试用例13: 数据隔离验证

**步骤**:
1. 使用医院A的token创建宠物档案A
2. 使用医院B的token查询宠物列表

**预期结果**:
- 医院B的查询结果中不包含宠物档案A

### 测试用例14: 未登录访问

**请求**:
```bash
curl -X GET http://localhost:8080/api/pets
```

**预期结果**:
- HTTP状态码: 401 或 403
- 错误信息: 未授权

### 测试用例15: Token过期访问

**请求**:
```bash
curl -X GET http://localhost:8080/api/pets \
  -H "Authorization: Bearer expired_token"
```

**预期结果**:
- HTTP状态码: 401
- 错误信息: Token无效或已过期

## 测试数据清理

```sql
-- 清理测试数据
DELETE FROM lin_lb_pet WHERE id > 0;
-- 或逻辑删除
UPDATE lin_lb_pet SET is_active = 0 WHERE animal_name LIKE '测试%';
```

## 性能测试

### 并发创建测试
```bash
# 使用Apache Bench进行并发测试
ab -n 1000 -c 10 -T 'application/json' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -p pet_create.json \
  http://localhost:8080/api/pets
```

### 大数据量查询测试
```bash
# 先插入10000条测试数据,然后测试分页查询性能
curl -X GET "http://localhost:8080/api/pets?pageNum=100&pageSize=100" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -w "\nTime: %{time_total}s\n"
```

## 自动化测试脚本示例

```bash
#!/bin/bash

# 设置基础URL和Token
BASE_URL="http://localhost:8080"
JWT_TOKEN="your_jwt_token_here"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
  local test_name=$1
  local method=$2
  local url=$3
  local data=$4

  echo "Testing: $test_name"

  if [ -z "$data" ]; then
    response=$(curl -s -X $method "$BASE_URL$url" \
      -H "Authorization: Bearer $JWT_TOKEN" \
      -w "\nHTTP_STATUS:%{http_code}")
  else
    response=$(curl -s -X $method "$BASE_URL$url" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $JWT_TOKEN" \
      -d "$data" \
      -w "\nHTTP_STATUS:%{http_code}")
  fi

  http_code=$(echo "$response" | grep "HTTP_STATUS" | cut -d: -f2)
  body=$(echo "$response" | sed '/HTTP_STATUS/d')

  if [ "$http_code" -eq 200 ]; then
    echo -e "${GREEN}✓ PASSED${NC} - HTTP $http_code"
  else
    echo -e "${RED}✗ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
  fi
  echo "----------------------------------------"
}

# 运行测试
test_api "创建宠物档案" "POST" "/api/pets" '{
  "animalName": "测试宠物",
  "infoAnimalId": 1,
  "dictSexId": 1,
  "ownerName": "测试用户"
}'

test_api "查询宠物列表" "GET" "/api/pets?pageNum=1&pageSize=10"

test_api "搜索宠物" "GET" "/api/pets/search?name=测试"

echo "All tests completed!"
```

## Swagger UI测试

访问Swagger UI进行交互式测试:
```
http://localhost:8080/swagger-ui.html
```

步骤:
1. 点击"认证管理" -> "/auth/wechat/login"
2. 执行微信登录,复制返回的token
3. 点击页面右上角的"Authorize"按钮
4. 输入"Bearer {token}"并点击Authorize
5. 然后就可以测试所有需要认证的API了

## 注意事项

1. **JWT Token**: 所有API都需要有效的JWT token(除了登录接口)
2. **数据隔离**: 确保测试不同医院之间的数据隔离
3. **逻辑删除**: 删除操作是软删除,数据仍在数据库中,只是is_active=0
4. **时间格式**: 生日字段使用"yyyy-MM-dd"格式
5. **必填字段**: 创建时animalName、ownerName、infoAnimalId、dictSexId是必填的
6. **关联数据**: infoAnimalId、infoVarietyId、dictSexId必须在数据库中存在

## 测试报告模板

| 测试用例 | 测试结果 | HTTP状态码 | 响应时间 | 备注 |
|---------|---------|-----------|---------|------|
| 创建宠物档案 - 成功 | ✓ | 200 | 150ms | - |
| 创建宠物档案 - 缺少必填字段 | ✓ | 400 | 50ms | - |
| 分页查询 | ✓ | 200 | 100ms | - |
| 搜索宠物 | ✓ | 200 | 80ms | - |
| 数据隔离验证 | ✓ | 500 | 60ms | 验证通过 |
| ... | ... | ... | ... | ... |
