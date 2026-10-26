# 本机数据库开发

Mac mini 的 `laboklin` 已导出至本机，使用独立的 MySQL 9.3.0 实例。原库保留；两份数据不会自动同步。

| 项目 | 配置 |
| --- | --- |
| 地址 | `127.0.0.1` |
| 端口 | `13306` |
| 数据库 | `laboklin` |
| 开发用户 | `petlis_dev` |
| 数据与日志 | `~/.local/share/petlis/mysql/` |
| 开发连接配置与密码 | `~/.local/share/petlis/mysql/app.cnf`（仅当前用户可读） |
| 后端环境变量 | `~/.local/share/petlis/mysql/backend.env`（仅当前用户可读） |

在项目根目录执行：

```bash
./scripts/local/mysql.sh start
./scripts/local/mysql.sh status
./scripts/local/mysql.sh client
./scripts/local/mysql.sh stop
```

后端使用本地库：

```bash
source "$HOME/.local/share/petlis/mysql/backend.env"
cd petlis-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Java 17 与 Maven 已安装在 Petlis 专用目录。数据库复制不等于小程序功能已经开发完成。

数据库只监听本机回环地址，不依赖局域网。实例有独立的数据目录、socket 和端口，不使用 Homebrew 全局 MySQL 服务，也不改变 Docker、xsf 或其他项目的配置。

开发账号可进行业务数据读写，无建表、删表和改表权限。原有触发器、视图、存储过程保留；事件调度关闭，Flyway 自动迁移关闭。后续结构变更继续先审批，新增表遵循 `lin_业务名`。

数据库备份、客户数据、账号密码均保存在 Git 仓库外，不能提交到 GitHub。代码通过 GitHub 同步，数据库按需显式导入，不自动双向同步。

## 复制校验

2026-10-24 从 Mac mini 最新快照恢复：72 张数据表、7 个视图、1 个触发器，合计 392,373 行；逐表行数已与来源核对。备份 SHA-256：`d61dcab3df6862f59f33be7aef1cf67d4a4457cf4559f3a230ef39483b798421`。

原库的 `vw_hospmonthreport`、`vw_hospmonthreport3_sendmaildt` 在 `ONLY_FULL_GROUP_BY` 模式下均报 MySQL 1055；本机保持相同行为，没有修改视图或全局 SQL 模式。其余 5 个视图可由开发账号查询。该旧月报问题应单独审核修复。

详细校验结果保存在 `~/.local/share/petlis/mysql/verification.json`，不包含客户记录。

## 后端运行与回归

Java 17 与 Maven 已准备在 `~/.local/share/petlis/tools/`，不依赖全局 Java 设置。启动：

```bash
./scripts/local/backend.sh
```

接口地址：`http://127.0.0.1:8082/api`。开发审核员用户名 `petlis_reviewer`；初始密码在 `~/.local/share/petlis/reviewer-credentials.txt`（权限 600），仅供本机开发，不关联真实医院。

全套离线测试（不连接数据库）：

```bash
JAVA_HOME="$HOME/.local/share/petlis/tools/Home" \
 "$HOME/.local/share/petlis/tools/libexec/bin/mvn" -o \
 -f petlis-backend/pom.xml \
 -Dmaven.repo.local="$HOME/.local/share/petlis/tools/m2" test
```

在本机开发库与后端已启动时，可运行 `scripts/local/test-auth.py` 和 `scripts/local/test-business.py`。它们仅向 `127.0.0.1:8082` 请求，使用随机测试医生及礼品等数据，并在退出时清理。短信使用数据库隔离测试记录；支付只验证开发测试流程，不能作为真实收款验收。

报告使用已取回的 PHP 资料库 PDF 目录，路径在私有 `backend.env` 配置。原文件只读；下载通过当前账号鉴权，不把登录令牌放在 URL 中。

MySQL 使用独立 LaunchAgent `com.petlis.mysql-local` 管理，保证终端退出后仍可用，并在本用户登录时启动；`mysql.sh stop` 可停止。

小程序源文件检查：运行 `python3 scripts/local/check-miniprogram.py`，使用已安装微信开发者工具中的 WXML/WXSS 编译器，并检查全部 JavaScript 语法。其他安装位置可通过 `PETLIS_WECHAT_COMPILER_DIR` 指向包含 `wcc`、`wcsc` 的目录。业务页面逻辑回归运行 `node scripts/local/test-client-flows.js`。这些命令不会上传或发布小程序，也不替代模拟器、真机验收。

开发环境启用旧订单通知桥接，每 5 秒读取原通知表中的新状态事件，写入已批准的新业务消息表。首次启动以当前最大通知 ID 为基线，保存在 `~/.local/share/petlis/legacy-notification-baseline.txt`，不向用户补发基线之前的历史通知；重启沿用此文件。它是固定基线而非递增游标，已导入事件通过唯一业务键 `legacy:通知ID` 去重，可恢复扫描失败。不要删除基线文件；迁移开发服务时需一并复制。通过 `app.notifications.legacy-baseline-file` 可指定独立路径。原触发器、原通知记录及原发送标记不被修改；真实微信发送仍由独立推送配置控制。
