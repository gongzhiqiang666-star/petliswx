-- 用户已于 2026-11-07 批准，仅在本机开发副本执行完成；禁止重复执行。
-- 仅新增顺丰集成表，不修改任何原有或既有业务表。
-- 执行前须核对当前 schema 未存在同名表，并保存结构快照。
CREATE TABLE lin_sf_request (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 order_id INT NOT NULL,
 operation VARCHAR(24) NOT NULL,
 request_key VARCHAR(64) NOT NULL,
 external_order_no VARCHAR(64) NOT NULL,
 payload_hash CHAR(64) NOT NULL,
 state VARCHAR(24) NOT NULL,
 tracking_no VARCHAR(64) NULL,
 result_json JSON NULL,
 platform_code VARCHAR(32) NULL,
 business_code VARCHAR(32) NULL,
 created_by INT NOT NULL,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_sf_order_operation (order_id,operation),
 UNIQUE KEY uk_sf_request_key (request_key),
 KEY idx_sf_pending (state,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- state: RESERVED/SENDING/SUCCEEDED/REJECTED/UNKNOWN。
-- SENDING/UNKNOWN 只能查单对账，不自动重新创建。
-- result_json 仅保存脱敏、白名单业务字段；不存凭据、签名、完整联系人或地址。

CREATE TABLE lin_sf_route_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 shipment_id BIGINT NOT NULL,
 event_key CHAR(64) NOT NULL,
 carrier_code VARCHAR(32) NOT NULL,
 event_time DATETIME(6) NOT NULL,
 received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 mapped_status VARCHAR(32) NULL,
 description VARCHAR(1000) NULL,
 applied_at DATETIME(6) NULL,
 UNIQUE KEY uk_sf_route_event (event_key),
 KEY idx_sf_route_shipment (shipment_id,event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 回调验签通过后才入事件；按运单+路由码+业务事件时间等稳定字段生成event_key。
-- 不把快递妥投映射成实验室核样通过。
-- 回滚：先停用顺丰接入并备份新增表；有业务记录时保留归档，不直接DROP。
