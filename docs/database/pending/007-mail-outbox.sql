-- 待用户批准，禁止执行。目标仅为 Petlis 本机 127.0.0.1:13306/laboklin。
-- 原 PHP 表/字段/索引/触发器不变；不包含启用参数、作业或真实发送。
-- 原 lin_notification_event / delivery 是微信订阅消息队列，不具备邮件快照、附件及回调恢复状态。
-- 只执行一次；如任一表已存在，停止核对，不使用 IF NOT EXISTS 隐藏差异。
CREATE TABLE lin_mail_message (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 business_type VARCHAR(40) NOT NULL,
 business_id VARCHAR(160) NOT NULL,
 requested_by INT UNSIGNED NOT NULL,
 request_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 request_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 smtp_profile_id INT NOT NULL,
 sender_address VARCHAR(250) NOT NULL,
 sender_name VARCHAR(250) NOT NULL,
 recipients_json JSON NOT NULL,
 bcc_json JSON NOT NULL,
 subject VARCHAR(2000) NOT NULL,
 body_html MEDIUMTEXT NOT NULL,
 attachments_json JSON NOT NULL,
 source_version CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 callback_json JSON NOT NULL,
 status VARCHAR(32) NOT NULL DEFAULT 'READY',
 callback_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
 attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
 next_attempt_at DATETIME NULL,
 accepted_at DATETIME NULL,
 last_error_code VARCHAR(50) NOT NULL DEFAULT '',
 last_error_message VARCHAR(1000) NOT NULL DEFAULT '',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_mail_request (requested_by,request_key),
 INDEX idx_mail_business (business_type,business_id),
 INDEX idx_mail_work (status,next_attempt_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_mail_attempt (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 message_id BIGINT UNSIGNED NOT NULL,
 attempt_no INT UNSIGNED NOT NULL,
 claim_token CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 status VARCHAR(32) NOT NULL,
 started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 finished_at DATETIME NULL,
 smtp_message_id VARCHAR(250) NOT NULL DEFAULT '',
 error_code VARCHAR(50) NOT NULL DEFAULT '',
 error_message VARCHAR(1000) NOT NULL DEFAULT '',
 resolved_by INT UNSIGNED NULL,
 resolved_at DATETIME NULL,
 resolution_note VARCHAR(1000) NOT NULL DEFAULT '',
 UNIQUE KEY uk_mail_attempt (message_id,attempt_no),
 UNIQUE KEY uk_mail_claim (claim_token),
 INDEX idx_mail_attempt_status (status,started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
