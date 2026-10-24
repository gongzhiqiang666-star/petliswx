-- V5__add_order_subscribe_field.sql
-- 为订单表添加订阅同意字段
-- Feature: 004-bugfixes-and-enhancements
-- Date: 2026-01-03

ALTER TABLE lin_lb_listdiagnosisproject
ADD COLUMN notification_subscribe TINYINT DEFAULT 0 COMMENT '是否订阅消息通知 0-未订阅 1-已订阅'
AFTER SampleStatus;
