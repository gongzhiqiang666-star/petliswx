-- 用户已批准并在 2026-10-24 执行；当前本机副本也已恢复。请勿重复执行。
-- 待用户批准，禁止自动执行。适用 laboklin，MySQL 8+。
-- 仅新增 Petlis 专用业务表；不修改、删除任何既有表/字段/数据。
-- 已存在 lin_user.money / score 与流水表继续复用；医生密码单独存 hash，不改旧密码字段。
-- 不创建触发器，不修改已有 Flyway 历史。表间关系由事务和应用校验维护，避免影响旧系统。
-- 执行前备份；必须确认以下表名均不存在，若重名应停止核对而非覆盖。

CREATE TABLE lin_doctor_profile (
 user_id INT UNSIGNED NOT NULL PRIMARY KEY,
 password_hash VARCHAR(255) NULL,
 title VARCHAR(100) NOT NULL DEFAULT '',
 qualification_files JSON NULL,
 requested_hospital_id INT UNSIGNED NULL,
 review_status VARCHAR(20) NOT NULL DEFAULT 'pending',
 review_reason VARCHAR(500) NOT NULL DEFAULT '',
 reviewed_by INT UNSIGNED NULL,
 reviewed_at DATETIME NULL,
 locked_until DATETIME NULL,
 credential_version INT UNSIGNED NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_profile_change (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id INT UNSIGNED NOT NULL,
 requested_data JSON NOT NULL,
 previous_data JSON NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'pending',
 review_reason VARCHAR(500) NOT NULL DEFAULT '',
 reviewed_by INT UNSIGNED NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 reviewed_at DATETIME NULL,
 INDEX idx_profile_user (user_id, created_at),
 INDEX idx_profile_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_laboratory (
 id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(50) NOT NULL UNIQUE,
 name VARCHAR(200) NOT NULL,
 description VARCHAR(1000) NOT NULL DEFAULT '',
 active TINYINT NOT NULL DEFAULT 1,
 sort_order INT NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_lab_test (
 laboratory_id INT UNSIGNED NOT NULL,
 test_type_id INT UNSIGNED NOT NULL,
 price DECIMAL(12,2) NULL,
 active TINYINT NOT NULL DEFAULT 1,
 PRIMARY KEY (laboratory_id, test_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_order_extension (
 order_id INT UNSIGNED NOT NULL PRIMARY KEY,
 user_id INT UNSIGNED NOT NULL,
 laboratory_id INT UNSIGNED NOT NULL,
 user_coupon_id BIGINT UNSIGNED NULL,
 original_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
 discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
 payable_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
 review_status VARCHAR(20) NOT NULL DEFAULT 'pending',
 review_reason VARCHAR(500) NOT NULL DEFAULT '',
 reviewed_by INT UNSIGNED NULL,
 reviewed_at DATETIME NULL,
 settlement_mode VARCHAR(20) NOT NULL DEFAULT 'cash',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_order_user (user_id, created_at),
 INDEX idx_order_review (review_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_order_draft (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id INT UNSIGNED NOT NULL,
 laboratory_id INT UNSIGNED NULL,
 payload JSON NOT NULL,
 version INT UNSIGNED NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_draft_user (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_coupon (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 amount DECIMAL(12,2) NOT NULL,
 minimum_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
 laboratory_id INT UNSIGNED NULL,
 starts_at DATETIME NOT NULL,
 expires_at DATETIME NOT NULL,
 active TINYINT NOT NULL DEFAULT 1,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_user_coupon (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id INT UNSIGNED NOT NULL,
 coupon_id BIGINT UNSIGNED NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'available',
 order_id INT UNSIGNED NULL,
 issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 used_at DATETIME NULL,
 INDEX idx_coupon_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_gift (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(200) NOT NULL,
 description TEXT NULL,
 image_url VARCHAR(1000) NOT NULL DEFAULT '',
 points_cost INT UNSIGNED NOT NULL,
 stock INT UNSIGNED NOT NULL DEFAULT 0,
 active TINYINT NOT NULL DEFAULT 1,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_redemption (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id INT UNSIGNED NOT NULL,
 gift_id BIGINT UNSIGNED NOT NULL,
 gift_name VARCHAR(200) NOT NULL,
 quantity INT UNSIGNED NOT NULL,
 points_spent INT UNSIGNED NOT NULL,
 delivery_address JSON NOT NULL,
 request_key VARCHAR(64) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'pending',
 tracking_number VARCHAR(100) NOT NULL DEFAULT '',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_redemption_request (user_id, request_key),
 INDEX idx_redemption_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_recharge_order (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id INT UNSIGNED NOT NULL,
 payment_no VARCHAR(64) NOT NULL UNIQUE,
 amount DECIMAL(12,2) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'pending',
 transaction_id VARCHAR(100) NULL UNIQUE,
 request_key VARCHAR(64) NOT NULL,
 prepay_id VARCHAR(128) NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 paid_at DATETIME NULL,
 UNIQUE KEY uk_recharge_request (user_id, request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_invoice_profile (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id INT UNSIGNED NOT NULL,
 title VARCHAR(200) NOT NULL,
 tax_number VARCHAR(50) NOT NULL DEFAULT '',
 email VARCHAR(200) NOT NULL,
 address VARCHAR(500) NOT NULL DEFAULT '',
 phone VARCHAR(30) NOT NULL DEFAULT '',
 bank_name VARCHAR(200) NOT NULL DEFAULT '',
 bank_account VARCHAR(100) NOT NULL DEFAULT '',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_invoice_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_invoice_application (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id INT UNSIGNED NOT NULL,
 profile_snapshot JSON NOT NULL,
 amount DECIMAL(12,2) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'pending',
 request_key VARCHAR(64) NOT NULL,
 invoice_url VARCHAR(1000) NOT NULL DEFAULT '',
 reject_reason VARCHAR(500) NOT NULL DEFAULT '',
 issued_at DATETIME NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_invoice_request (user_id, request_key),
 INDEX idx_invoice_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_invoice_order (
 application_id BIGINT UNSIGNED NOT NULL,
 order_id INT UNSIGNED NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_article (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 category VARCHAR(100) NOT NULL,
 type VARCHAR(20) NOT NULL DEFAULT 'article',
 title VARCHAR(250) NOT NULL,
 summary VARCHAR(1000) NOT NULL DEFAULT '',
 content MEDIUMTEXT NOT NULL,
 cover_url VARCHAR(1000) NOT NULL DEFAULT '',
 live_at DATETIME NULL,
 live_url VARCHAR(1000) NOT NULL DEFAULT '',
 popup_enabled TINYINT NOT NULL DEFAULT 0,
 status VARCHAR(20) NOT NULL DEFAULT 'draft',
 published_at DATETIME NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_article_published (status, category, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_article_action (
 user_id INT UNSIGNED NOT NULL,
 article_id BIGINT UNSIGNED NOT NULL,
 liked TINYINT NOT NULL DEFAULT 0,
 bookmarked TINYINT NOT NULL DEFAULT 0,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (user_id, article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_user_preferences (
 user_id INT UNSIGNED NOT NULL PRIMARY KEY,
 order_notifications TINYINT NOT NULL DEFAULT 1,
 report_notifications TINYINT NOT NULL DEFAULT 1,
 invoice_notifications TINYINT NOT NULL DEFAULT 1,
 news_notifications TINYINT NOT NULL DEFAULT 0,
 preferred_laboratory_id INT UNSIGNED NULL,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lin_notification_receipt (
 user_id INT UNSIGNED NOT NULL,
 notification_id INT NOT NULL,
 read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (user_id, notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

