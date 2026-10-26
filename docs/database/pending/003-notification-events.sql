-- 用户已批准并在 2026-10-24 执行；当前本机副本也已恢复。请勿重复执行。
-- 待批准，禁止自动执行。仅新增以下两表，不修改既有通知表、触发器或历史数据。
-- 旧 lin_lb_notification 专用于订单状态，不能承载资讯、发票等全业务通知的持久化重试。
CREATE TABLE lin_notification_event (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id INT UNSIGNED NOT NULL,
 event_type VARCHAR(40) NOT NULL,
 business_key VARCHAR(100) NOT NULL,
 title VARCHAR(200) NOT NULL,
 message VARCHAR(1000) NOT NULL,
 page_path VARCHAR(300) NOT NULL DEFAULT '',
 template_data JSON NULL,
 push_status VARCHAR(20) NOT NULL DEFAULT 'pending',
 retry_count INT UNSIGNED NOT NULL DEFAULT 0,
 next_attempt_at DATETIME NULL,
 read_at DATETIME NULL,
 last_error VARCHAR(1000) NOT NULL DEFAULT '',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_event_business (user_id,event_type,business_key),
 INDEX idx_event_user (user_id,created_at),
 INDEX idx_event_retry (push_status,next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE lin_notification_delivery (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_id BIGINT UNSIGNED NOT NULL,
 status VARCHAR(20) NOT NULL,
 error_code VARCHAR(50) NOT NULL DEFAULT '',
 error_message VARCHAR(1000) NOT NULL DEFAULT '',
 message_id VARCHAR(100) NOT NULL DEFAULT '',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_delivery_event (event_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
