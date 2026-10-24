# User Story 2 - 宠物档案管理

## 概述

实现宠物档案的完整CRUD功能,包括创建、查询、更新、删除和搜索操作。所有数据按医院进行隔离,确保每个医院只能访问和管理自己的宠物档案。

## 实现内容

### 1. 数据库表设计

**表名**: `lin_lb_pet`

**字段说明**:
- `id`: 宠物ID (主键)
- `animal_name`: 宠物中文名 (必填)
- `animal_name_en`: 宠物英文名 (可选)
- `info_animal_id`: 物种ID (关联lin_lb_infoanimal表)
- `info_variety_id`: 品种ID (关联lin_lb_infovariety表)
- `dict_sex_id`: 性别ID (关联lin_lb_dictsex表)
- `birthday`: 生日 (格式: yyyy-MM-dd)
- `chip_no`: 芯片号
- `owner_name`: 畜主中文名 (必填)
- `owner_name_en`: 畜主英文名 (可选)
- `info_hospital_id`: 所属医院ID (必填,用于数据隔离)
- `is_active`: 启用状态 (0=禁用,1=启用)
- `createtime`: 创建时间 (Unix时间戳)
- `updatetime`: 更新时间 (Unix时间戳)

**索引**:
- PRIMARY KEY (`id`)
- KEY `idx_info_hospital_id` (`info_hospital_id`)
- KEY `idx_info_animal_id` (`info_animal_id`)
- KEY `idx_chip_no` (`chip_no`)
- KEY `idx_animal_name` (`animal_name`)
- KEY `idx_is_active` (`is_active`)

**DDL脚本位置**: `/src/main/resources/db/migration/V1.0.0__create_pet_table.sql`

### 2. 实体类

**位置**: `com.petlis.entity.business.Pet`

继承自 `ActivableEntity`,提供基础的启用/禁用功能。

### 3. Repository层

**接口**: `com.petlis.repository.PetRepository`

**提供方法**:
- `findByInfoHospitalIdAndAnimalNameContaining()`: 根据医院ID和名称模糊查询
- `findByInfoHospitalId()`: 根据医院ID查询所有启用的宠物档案
- `findByInfoHospitalId(hospitalId, pageable)`: 根据医院ID分页查询
- `countByInfoHospitalId()`: 统计医院下的宠物档案数量
- `findByChipNo()`: 根据芯片号查询

### 4. Service层

**接口**: `com.petlis.service.PetService`

**实现类**: `com.petlis.service.impl.PetServiceImpl`

**提供功能**:

#### 创建宠物档案
```java
PetResponse create(PetCreateRequest request, Integer hospitalId)
```
- 验证物种、品种、性别是否存在
- 自动设置hospitalId、createTime、updateTime
- 使用@Transactional确保事务一致性

#### 更新宠物档案
```java
PetResponse update(Integer petId, PetUpdateRequest request, Integer hospitalId)
```
- 验证宠物档案是否存在
- 验证权限(只能更新本医院的宠物档案)
- 验证关联数据是否存在

#### 删除宠物档案
```java
void delete(Integer petId, Integer hospitalId)
```
- 逻辑删除,设置is_active=0
- 验证权限

#### 查询宠物档案详情
```java
PetVO queryById(Integer petId, Integer hospitalId)
```
- 验证权限
- 返回包含关联数据的VO对象

#### 分页查询宠物档案
```java
Page<PetVO> query(PetQueryRequest request, Integer hospitalId)
```
- 按hospitalId隔离数据
- 支持分页

#### 搜索宠物档案
```java
List<PetVO> search(String name, Integer hospitalId)
```
- 支持中文名或英文名模糊搜索
- 按hospitalId隔离数据

### 5. Controller层

**位置**: `com.petlis.controller.api.PetController`

**基础路径**: `/api/pets`

**API接口**:

#### 创建宠物档案
```
POST /api/pets
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "animalName": "旺财",
  "animalNameEn": "Lucky",
  "infoAnimalId": 1,
  "infoVarietyId": 10,
  "dictSexId": 1,
  "birthday": "2020-01-15",
  "chipNo": "928000000000001",
  "ownerName": "张三",
  "ownerNameEn": "Zhang San"
}

Response:
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1,
    "animalName": "旺财",
    ...
  }
}
```

#### 更新宠物档案
```
PUT /api/pets/{id}
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "animalName": "旺财",
  "animalNameEn": "Lucky",
  ...
}

Response:
{
  "code": 200,
  "message": "更新成功",
  "data": {...}
}
```

#### 删除宠物档案
```
DELETE /api/pets/{id}
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

#### 查询宠物档案详情
```
GET /api/pets/{id}
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "animalName": "旺财",
    "infoAnimalName": "犬",
    "infoVarietyName": "金毛",
    "dictSexName": "公",
    ...
  }
}
```

#### 分页查询宠物档案列表
```
GET /api/pets?pageNum=1&pageSize=10
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "content": [...],
    "totalElements": 100,
    "totalPages": 10,
    "pageNumber": 1
  }
}
```

#### 搜索宠物档案
```
GET /api/pets/search?name=旺财
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "message": "搜索成功",
  "data": [...]
}
```

### 6. DTO和VO设计

#### PetCreateRequest
创建宠物档案请求DTO,包含必填字段验证:
- @NotBlank: animalName, ownerName
- @NotNull: infoAnimalId, dictSexId

#### PetUpdateRequest
更新宠物档案请求DTO,所有字段可选

#### PetQueryRequest
查询请求DTO,支持分页参数:
- name: 搜索关键词
- pageNum: 页码(默认1)
- pageSize: 每页数量(默认10)

#### PetResponse
响应DTO,包含宠物所有字段信息

#### PetVO
视图对象,包含关联的字典数据名称:
- 物种名称(中英文)
- 品种名称(中英文)
- 性别名称(中英文)

### 7. MapStruct映射器

**位置**: `com.petlis.mapper.PetMapper`

**提供方法**:
- `toEntity()`: DTO -> Entity
- `updateEntity()`: 更新实体字段
- `toResponse()`: Entity -> Response DTO
- `toVO()`: Entity -> VO

### 8. 数据权限隔离

所有Service方法都接收`hospitalId`参数,确保:
1. 创建时自动关联当前用户的hospitalId
2. 查询时只返回本医院的数据
3. 更新/删除时验证权限,防止跨医院操作

Controller层通过`SecurityUtils.getCurrentHospitalId()`从JWT token中提取当前用户的hospitalId。

### 9. 操作日志

所有Controller方法都添加了`@OperationLog`注解,记录:
- 模块名称: "宠物档案管理"
- 操作类型: "创建"、"更新"、"删除"、"查询"、"搜索"
- 操作描述: 具体的操作说明

### 10. 异常处理

使用统一的异常处理:
- `BusinessException`: 业务逻辑异常(如权限不足、数据验证失败)
- `ResourceNotFoundException`: 资源不存在异常

所有异常都会被`GlobalExceptionHandler`捕获并返回统一的响应格式。

### 11. 字段验证

使用Spring Validation进行参数验证:
- @NotNull: 字段不能为null
- @NotBlank: 字符串不能为空
- @Valid: 级联验证

### 12. 事务管理

使用`@Transactional`确保数据一致性:
- 创建操作
- 更新操作
- 删除操作

## 技术要点

1. **JPA规范**: 使用Spring Data JPA进行数据访问
2. **MapStruct**: 使用编译时生成的映射代码,性能优异
3. **Spring Validation**: 声明式参数验证
4. **JWT认证**: 从token中提取用户信息
5. **数据隔离**: 按hospitalId进行多租户数据隔离
6. **逻辑删除**: 使用is_active字段实现软删除
7. **操作日志**: 使用AOP记录关键操作
8. **异常统一处理**: 使用GlobalExceptionHandler统一处理异常
9. **Swagger文档**: 使用OpenAPI 3.0生成API文档

## 文件清单

### 实体层
- `src/main/java/com/petlis/entity/business/Pet.java`

### Repository层
- `src/main/java/com/petlis/repository/PetRepository.java`

### Service层
- `src/main/java/com/petlis/service/PetService.java`
- `src/main/java/com/petlis/service/impl/PetServiceImpl.java`

### Controller层
- `src/main/java/com/petlis/controller/api/PetController.java`

### DTO层
- `src/main/java/com/petlis/dto/request/PetCreateRequest.java`
- `src/main/java/com/petlis/dto/request/PetUpdateRequest.java`
- `src/main/java/com/petlis/dto/request/PetQueryRequest.java`
- `src/main/java/com/petlis/dto/response/PetResponse.java`

### VO层
- `src/main/java/com/petlis/vo/PetVO.java`

### Mapper层
- `src/main/java/com/petlis/mapper/PetMapper.java`

### 数据库脚本
- `src/main/resources/db/migration/V1.0.0__create_pet_table.sql`

## 使用示例

### 1. 创建宠物档案

```bash
curl -X POST http://localhost:8080/api/pets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
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

### 2. 查询宠物档案列表

```bash
curl -X GET "http://localhost:8080/api/pets?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. 搜索宠物档案

```bash
curl -X GET "http://localhost:8080/api/pets/search?name=旺财" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. 查询宠物档案详情

```bash
curl -X GET http://localhost:8080/api/pets/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5. 更新宠物档案

```bash
curl -X PUT http://localhost:8080/api/pets/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "animalName": "旺财旺财",
    "birthday": "2020-02-15"
  }'
```

### 6. 删除宠物档案

```bash
curl -X DELETE http://localhost:8080/api/pets/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 验收标准

- [x] 完整的CRUD功能
- [x] 数据按医院隔离
- [x] 必填字段验证
- [x] 搜索功能正常(支持中英文名模糊搜索)
- [x] 分页查询正常
- [x] 逻辑删除(软删除)
- [x] 权限验证(只能操作本医院数据)
- [x] 事务一致性
- [x] 操作日志记录
- [x] Swagger API文档

## 后续优化建议

1. **缓存优化**: 对频繁访问的物种、品种、性别字典数据进行缓存
2. **批量操作**: 支持批量导入/导出宠物档案
3. **图片上传**: 支持上传宠物照片
4. **历史记录**: 记录宠物档案的修改历史
5. **数据统计**: 提供宠物数量、品种分布等统计功能
6. **高级搜索**: 支持多条件组合搜索(如按品种、性别、年龄段等)
7. **数据导出**: 支持导出Excel/CSV格式的宠物档案列表

## 相关文档

- [MySQL表结构说明.md](../../../MySQL表结构说明.md)
- [Laboklin送检流程字段详细清单.md](../../../Laboklin送检流程字段详细清单.md)
- [系统设计文档.md](../../PETLIS/SYSTEM_DESIGN_SPEC.md)
