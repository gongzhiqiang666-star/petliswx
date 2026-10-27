# Task 1：契约基线与 Schema 守护报告

日期：2026-10-27
分支：`feat/merge-confirmed-miniprogram`
范围：仅 Task 1；未复制旧 `backend/` 迁移，未修改既有业务代码、旧表、旧迁移或触发器，未连接 Mac mini 数据库，未 push。

## 交付物

- `docs/FUNCTION-MATRIX.md`：逐项登记 Excel 两个功能工作表的 42 个唯一功能主项及全部子项，包含保留/移植/新增决策、前后端目标位置、验收标准与实现/验收负责 Task。自动比对确认 42/42 功能名无遗漏；物流、实验室、月结/签约、打印、退款和五角色权限均单列。
- `petlis-backend/src/test/java/com/petlis/contract/LegacyApiContractTest.java`：使用既有 MockMvc/mock service 方式固定登录、宠物、订单、报告契约；断言成功 `code=200`、0 基 Spring Page、订单 `items` 形状和旧 order/detail ID。未依赖真实数据库。
- `petlis-backend/src/test/java/com/petlis/schema/PlatformMigrationGuardTest.java`：守护 15 张批准的新表、后续模块字段、旧 ID 的 INT 引用、索引长度、禁用危险 DDL、禁止重复主表、平台 DDL 默认不自动执行和 42 行功能矩阵。
- `petlis-backend/src/main/resources/db/platform/V1__create_platform_workflow_tables.sql`：仅包含 15 个 `CREATE TABLE IF NOT EXISTS lin_*`，涵盖角色、物流、实验任务、结果批次、报告工作流/模板、病史、幂等、签约定价、月结、文件、审计和运营配置。无 `ALTER`/`DROP`/`TRUNCATE`/触发器/旧表修改；未建立跨新旧表外键。
- `integration/schema_guard.py`：通过 `information_schema` 保存/校验旧表列、索引和触发器；允许新增平台表，但旧表删列/改列/改索引/改触发器或旧表新增触发器均失败。只允许显式 `--isolated-copy` 的回环地址和非 3306 独立端口。
- `integration/tests/test_schema_guard.py`：5 项快照比较与连接安全测试。

`application.yml` 保持 `DB_MIGRATIONS_ENABLED:false` 和 `locations: classpath:db/migration`；`db/platform` 未加入任何自动迁移位置，只能由操作者在隔离副本手工执行。

## RED / GREEN 记录

### 基线

- 首次 `./mvnw test` 未运行：该后端没有 `mvnw`，PATH 也无 JDK/Maven。使用 Android Studio 自带 JDK 21 与 `/tmp/petlis-maven-3.9.11` 后完成真实基线，不修改项目业务或构建配置。
- 旧后端：`mvn test`，**103 tests，0 failures，0 errors，5 skipped，BUILD SUCCESS**。
- 小程序：**62 JS 语法、48 JSON 解析通过**。

### 合并守护 RED

- `LegacyApiContractTest`：4/4 通过，确认既有契约已满足，无需为 RED 改旧业务。
- `PlatformMigrationGuardTest`：初次因缺少 DDL/矩阵出现文件读取错误后，先将测试改为明确断言文件存在；再次 RED 为 **4 tests，3 failures，0 errors**（缺 DDL、缺后续字段、缺矩阵），默认不自动执行测试通过。
- `test_schema_guard.py`：初次 RED 为 **5 tests，5 failures**，原因是快照脚本尚不存在。

### 实现后 GREEN

- `LegacyApiContractTest,PlatformMigrationGuardTest`：**8 tests，0 failures，0 errors**。
- `python3 -m unittest discover -s tests -v`：**5 tests，0 failures**。
- MySQL 首轮实际执行发现 `lin_file.storage_path VARCHAR(1024)` 的 utf8mb4 唯一索引超过 3072 字节（ERROR 1071）。先增加索引长度守护并复现 **1 test，1 failure**，再仅将新表该列改为 `VARCHAR(768)`；新增 8 项 Java 守护恢复全绿。

## MySQL 8.4 隔离副本验证

- 引擎：`/opt/homebrew/opt/mysql@8.4/bin/mysqld`，**MySQL 8.4.13**。
- 数据目录：`/tmp/petlis-task1-mysql84-data`；仅绑定 `127.0.0.1:33306`；数据库 `petlis_task1_copy`；验证后已正常 `mysqladmin shutdown`。
- 副本由 `/tmp` 合成旧结构夹具建立，含 6 张 `lin_*` 旧表、25 列、14 个索引项和 1 个旧触发器。未从 Mac mini 读取或写入任何数据。
- 执行前快照：`/tmp/petlis-task1-before.json`。
- 将新 DDL 连续手工执行两次，均成功；`platform_tables=15`，`duplicate_masters=0`（无 `users`/`test_orders`）。
- 执行后快照：`/tmp/petlis-task1-after.json`。
- `schema_guard.py verify`：**6 张旧表的列、索引、触发器均未变化**；独立逐节比较亦完全一致（25/14/1）。

复用命令（必须先准备隔离副本，禁止指向原库）：

```bash
python3 integration/schema_guard.py snapshot \
  --port 33306 --database petlis_task1_copy --isolated-copy \
  --mysql-bin /opt/homebrew/opt/mysql@8.4/bin/mysql \
  --output /tmp/petlis-task1-before.json

# 仅在此处手工执行平台 DDL；执行两次可验证幂等。

python3 integration/schema_guard.py verify \
  --port 33306 --database petlis_task1_copy --isolated-copy \
  --mysql-bin /opt/homebrew/opt/mysql@8.4/bin/mysql \
  --baseline /tmp/petlis-task1-before.json
```

## 最终回归

- 后端全量 `mvn test`：**111 tests，0 failures，0 errors，5 skipped，BUILD SUCCESS**。111 = 旧 103 + 新 8；5 个跳过仍为基线中的 `RechargeLocalDatabaseTest` 本地数据库测试，没有新增跳过。
- schema 守护：**5/5 通过**；`python3 -m py_compile schema_guard.py` 通过。
- 小程序：**62 JS、48 JSON、38 pages、5 tabs 全部通过**。
- Excel 功能名独立比对：**42/42 通过**。
- 未进行微信开发者工具编译、真实微信/支付/短信或真实原库验证；Task 1 不需要且不作通过声明。

## 后续注意事项

- 本次 MySQL 验证使用合成旧结构副本，不是 Mac mini 原库的全量结构转储。后续若获得合规的脱敏全量结构副本，可直接复用同一脚本验证更多旧表；本次没有连接原库获取该转储。
- 新表仅建立目标结构和守护，后续模块必须先写失败测试再实现服务、权限、幂等和跨角色全链路。
