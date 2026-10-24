# PETLIS 数据库脚本说明

## 目录结构

```
db/
├── schema.sql                 # 数据库表结构(完整DDL)
├── demo-data.sql             # 示例数据(开发测试用)
└── migration/                # 数据库迁移脚本
    └── V1.0.1__add_indexes.sql  # 性能优化索引脚本
```

## 使用说明

### 1. 创建数据库

```sql
CREATE DATABASE laboklin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 导入表结构

```bash
mysql -u root -p laboklin < schema.sql
```

### 3. 导入示例数据(可选)

```bash
mysql -u root -p laboklin < demo-data.sql
```

### 4. 执行迁移脚本

```bash
mysql -u root -p laboklin < migration/V1.0.1__add_indexes.sql
```

## 脚本说明

### schema.sql

包含所有数据库表的DDL语句,按依赖顺序创建:

1. 用户表 (lin_lb_user)
2. 宠物档案表 (lin_lb_pet)
3. 字典表(城市、医院、物种、品种、性别、样本类型、检测类型等)
4. 检测申请主表 (lin_lb_listdiagnosisproject)
5. 检测明细表 (lin_lb_diagnosis_detail)
6. 检测报告内容表 (lin_lb_diagnosis_detail_info)

### demo-data.sql

包含用于开发和测试的示例数据:

- 测试用户数据
- 示例宠物档案
- 示例检测申请
- 字典基础数据

**警告**: 请勿在生产环境执行此脚本!

### migration/V1.0.1__add_indexes.sql

性能优化索引脚本,为常用查询字段添加索引以提升查询性能。

## 注意事项

1. **字符集**: 数据库使用 `utf8mb4` 字符集,支持emoji等特殊字符
2. **排序规则**: 使用 `utf8mb4_unicode_ci` 排序规则
3. **时间戳**: 所有时间字段使用Unix时间戳(秒级)
4. **外键**: 本项目未使用外键约束,通过应用层维护数据一致性
5. **索引**: 执行索引脚本前请评估数据量,大表创建索引可能需要较长时间

## 数据库表清单

| 表名 | 说明 | 主要用途 |
|------|------|----------|
| lin_lb_user | 用户表 | 存储医院用户信息 |
| lin_lb_pet | 宠物档案表 | 存储宠物基本信息 |
| lin_lb_city | 城市表 | 字典表:城市信息 |
| lin_lb_infohospital | 医院表 | 字典表:医院信息 |
| lin_lb_infoanimal | 物种表 | 字典表:动物物种 |
| lin_lb_infovariety | 品种表 | 字典表:动物品种 |
| lin_lb_dictsex | 性别表 | 字典表:性别 |
| lin_lb_infosampletype | 样本类型表 | 字典表:样本类型 |
| lin_lb_infotesttype | 检测类型表 | 字典表:检测项目类型 |
| lin_lb_infotesttypeprice | 检测类型价格表 | 检测项目价格 |
| lin_lb_listdiagnosisproject | 检测申请主表 | 存储检测申请单 |
| lin_lb_diagnosis_detail | 检测明细表 | 存储检测项目明细 |
| lin_lb_diagnosis_detail_info | 检测报告内容表 | 存储检测结果内容 |

## 维护建议

1. 定期备份数据库
2. 监控慢查询日志
3. 定期分析表碎片并优化
4. 根据实际查询情况调整索引
5. 定期清理历史数据或归档

---

**更新日期**: 2025-12-27
**版本**: 1.0.0
