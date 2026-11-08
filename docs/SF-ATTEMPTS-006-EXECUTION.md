# 顺丰多次寄件结构006执行报告（2026-11-08）

用户明确批准“仅在本机调整这两张新增表”。目标固定127.0.0.1:13306/laboklin，未连接Mac mini、生产或其他项目。执行前停止经进程路径核验的Petlis Java后台，使寄件入口不可写，MySQL事件始终OFF；完成后恢复后台。PHP预览未停止。

## 执行后的业务状态更新

2026-11-08后续官方沙箱验证已持久化第2次寄件：旧运单70只读保留，当前运单176亦经官方确认取消；外部号PETLIS_SANDBOX_23956_A2。因此下文“尚未产生第二次记录”为DDL刚执行时的历史状态，当前物理恢复旧唯一索引的前置条件已经不满足。只允许关闭再寄参数并保留兼容结构/历史回退，不得删历史恢复旧约束。详见SF-RESHIP-BACKEND-VALIDATION.md。

当前已完成第三次官方沙箱寄件（当前运单232）并经内部合成物流节点、PHP收样/检测/复核/发布至报告33833。第二、第三次记录均保留，详见同目录顺丰验证报告；本更新不增加DDL授权。

## 实际结构调整

- lin_shipment（项目新增工作流表）：attempt_no INT UNSIGNED NOT NULL DEFAULT 1；is_current TINYINT NULL DEFAULT 1；以(order_id,attempt_no)唯一键uk_shipment_order_attempt、(order_id,is_current)唯一键uk_shipment_order_current替代原uk_shipment_order；CHECK限定is_current仅1或NULL，tracking_no唯一约束保留。
- lin_sf_request（项目新增顺丰请求表）：attempt_no INT UNSIGNED NOT NULL DEFAULT 1；以(order_id,attempt_no,operation)唯一键uk_sf_order_attempt_operation替代原uk_sf_order_operation；request_key唯一约束保留。
- 仅执行批准SQL的两条ALTER，无第三条DDL、无原PHP表/视图/触发器修改，无业务历史删除/覆盖。表总数90、视图7、触发器1，未增删对象。

## 备份与核验

全库mysqldump（含数据、触发器、例程、事件定义）成功退出，大小2941276480字节，SHA256 `e8c8c7f67b8394c2dae2d7b35c4a551636a27cd9c955c4e5b020264504cf08dd`。备份和快照保存在本机私有目录 `/Users/apple/.local/share/petlis/backups/sf-attempts-006-20261108-061214`，目录0700、文件0600，不提交Git。SQL源文件执行时SHA256 `9dee15d23b55893d8152fbcc834da9dcb50dba4cc3f226140eb76b8a0c34e1e5`；执行后仅更新源文件注释为已执行，DDL未改。

schema_guard读取前后全部列/选项/索引/外键/CHECK/触发器，排除明确批准的两表后无差异；另核对其余平台表及全部视图定义、例程未变。原lin_shipment 70条、lin_sf_request 12条逐行所有原字段的SHA256与记录数一致；新增值均为attempt_no=1、is_current=1。约束测试后再次核验原行摘要未变。

实库回滚测试两项：允许多个is_current=NULL历史行；同订单/次数不可重复，仅一个当前运单，非法标识0由CHECK3819拒绝；每次CREATE/CANCEL独立唯一，后续次数可并存。仅事务内合成复制夹具，全部回滚，无外部顺丰调用。MySQL9对CHECK错误被驱动归为UncategorizedSQLException，测试改为核对实际SQLException3819，未放宽约束。

最终后端464项0失败/0错误/0跳过。真实兼容读取：旧订单23956仍返回运单70及取消SUCCEEDED；后台能力接口200；PHP本机入口302至本机登录页200。未因观测302擅自重启PHP。重放脚本实测在DDL前拒绝已执行状态，不创建新备份或修改结构。

执行工具：scripts/local/apply-sf-attempts-006.py，只接受批准的两条语句；默认仅预检，实际执行需专用参数且确认8082不再监听、无其他活动Petlis库操作。逐条DDL后保存检查点，任一步失败停止，不能整包重放。该工具不是启动迁移，不自动运行。

## 回退限制与未完成业务

尚未产生持久化第二次寄件记录；再寄入口/参数/多次读取与官方沙箱验收尚未完成，不能宣称再寄可用。现有业务继续读第一次记录。

新业务产生多次记录后，回退必须关闭再寄能力并保留结构/历史，用兼容版本接续；禁止删历史以重建旧唯一索引。仅在多次记录计数为0且满足SQL注释所列前置条件时才可评估物理逆向SQL，本次未执行回退，也未做破坏性全库恢复演练。备份校验不冒充恢复演练。
