-- V4__create_notification_log_table.sql
-- 创建通知发送日志表
-- Feature: 004-bugfixes-and-enhancements
-- Date: 2026-01-03

CREATE TABLE IF NOT EXISTS lin_lb_notification_log (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  notification_id INT NOT NULL COMMENT '通知记录ID (lin_lb_notification.id)',
  order_id INT NOT NULL COMMENT '订单ID',
  hospital_id INT NOT NULL COMMENT '医院ID',
  user_id INT COMMENT '用户ID',
  openid VARCHAR(100) COMMENT '微信OpenID',
  template_id VARCHAR(100) COMMENT '消息模板ID',
  message_id VARCHAR(100) COMMENT '微信消息ID',
  send_status TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态 0-待发送 1-成功 2-失败',
  error_code VARCHAR(50) COMMENT '错误码',
  error_message TEXT COMMENT '错误信息',
  send_time DATETIME COMMENT '发送时间',
  retry_count INT DEFAULT 0 COMMENT '重试次数',
  createtime INT UNSIGNED COMMENT '创建时间 (Unix时间戳)',
  updatetime INT UNSIGNED COMMENT '更新时间 (Unix时间戳)',
  INDEX idx_notification_id (notification_id),
  INDEX idx_order_id (order_id),
  INDEX idx_openid (openid),
  INDEX idx_send_status (send_status),
  INDEX idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知发送日志表';
