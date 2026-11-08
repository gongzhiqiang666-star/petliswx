-- 2026-11-08 已获用户批准并仅在本机执行一次，禁止重放。目标127.0.0.1:13306/laboklin。
-- 执行、备份与验证详见 docs/SF-ATTEMPTS-006-EXECUTION.md；以下DDL保持批准内容。
-- 仅调整本项目新增的 lin_shipment、lin_sf_request；不改原 PHP 表/视图/触发器。
-- 必要性：现有两表的按订单唯一约束只允许一次寄件；删除或覆盖旧行会丢失历史。
-- 执行前：停止本项目寄件写入口，保存全量备份和 schema_guard 快照；
-- SHOW CREATE TABLE 核对现有约束名称，确认本文件从未执行。
-- MySQL DDL非跨语句事务；任一步失败停止，不重放整个文件、不启动新功能。

ALTER TABLE lin_shipment
 ADD COLUMN attempt_no INT UNSIGNED NOT NULL DEFAULT 1,
 ADD COLUMN is_current TINYINT NULL DEFAULT 1,
 DROP INDEX uk_shipment_order,
 ADD UNIQUE KEY uk_shipment_order_attempt (order_id,attempt_no),
 ADD UNIQUE KEY uk_shipment_order_current (order_id,is_current),
 ADD CONSTRAINT chk_shipment_current_flag CHECK (is_current IS NULL OR is_current=1);

ALTER TABLE lin_sf_request
 ADD COLUMN attempt_no INT UNSIGNED NOT NULL DEFAULT 1,
 DROP INDEX uk_sf_order_operation,
 ADD UNIQUE KEY uk_sf_order_attempt_operation (order_id,attempt_no,operation);

-- 历史行自动为 attempt_no=1、is_current=1；保留原ID/数据/时间/版本/运单号唯一约束。
-- 再寄启用后：旧运单仅置is_current=NULL，新运单插入独立ID并设is_current=1；
-- 取消状态、面单、路由、审计均保留旧ID归属，不复制主业务订单或再次收费。

-- 回退前置检查（只读）：以下两查询必须均返回0才允许物理恢复原唯一索引。
-- SELECT COUNT(*) FROM (SELECT order_id FROM lin_shipment GROUP BY order_id HAVING COUNT(*)>1) q;
-- SELECT COUNT(*) FROM (SELECT order_id,operation FROM lin_sf_request GROUP BY order_id,operation HAVING COUNT(*)>1) q;
-- 若已产生多次寄件数据：关闭再寄参数，保留新结构与全部历史，使用兼容补丁版本回退；
-- 禁止为恢复唯一索引删历史、批量覆盖或强行导回旧备份。
-- 无多次记录且备份验证通过时的逆向SQL（仅注释，不自动执行）：
-- ALTER TABLE lin_sf_request DROP INDEX uk_sf_order_attempt_operation,
--  ADD UNIQUE KEY uk_sf_order_operation(order_id,operation), DROP COLUMN attempt_no;
-- ALTER TABLE lin_shipment DROP CHECK chk_shipment_current_flag,
--  DROP INDEX uk_shipment_order_attempt, DROP INDEX uk_shipment_order_current,
--  ADD UNIQUE KEY uk_shipment_order(order_id), DROP COLUMN is_current, DROP COLUMN attempt_no;
