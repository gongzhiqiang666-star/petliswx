-- V3__add_notification_details.sql
-- 为通知表添加详细信息字段
-- Feature: 004-bugfixes-and-enhancements
-- Date: 2026-01-03

-- 添加宠物和主人信息字段
ALTER TABLE lin_lb_notification
ADD COLUMN animal_name VARCHAR(250) COMMENT '宠物名称' AFTER new_status,
ADD COLUMN master_name VARCHAR(250) COMMENT '主人名称' AFTER animal_name,
ADD COLUMN notification_message TEXT COMMENT '通知消息内容' AFTER master_name;
