-- V1__create_notification_table.sql
-- 创建订单状态变化通知记录表
-- Feature: 004-bugfixes-and-enhancements
-- Date: 2025-12-28

CREATE TABLE IF NOT EXISTS lin_lb_notification (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  order_id INT NOT NULL COMMENT '订单ID (DiagnosisDetailId)',
  hospital_id INT NOT NULL COMMENT '医院ID (InfoHospitalId)',
  old_status INT COMMENT '原状态 (DiagnosisStatus)',
  new_status INT NOT NULL COMMENT '新状态 (DiagnosisStatus)',
  operator_id INT COMMENT '操作人ID (lin_user.id)',
  operation_time DATETIME NOT NULL COMMENT '操作时间',
  wechat_sent_status TINYINT NOT NULL DEFAULT 0 COMMENT '微信发送状态 0-待发送 1-已发送 2-失败',
  wechat_message_id VARCHAR(100) COMMENT '微信消息ID',
  error_message TEXT COMMENT '错误信息',
  createtime INT UNSIGNED COMMENT '创建时间 (Unix时间戳)',
  updatetime INT UNSIGNED COMMENT '更新时间 (Unix时间戳)',
  INDEX idx_order_id (order_id),
  INDEX idx_hospital_id (hospital_id),
  INDEX idx_operation_time (operation_time),
  INDEX idx_hospital_status (hospital_id, wechat_sent_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态变化通知记录表';
