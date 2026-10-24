-- V2__create_order_status_trigger.sql
-- 创建订单状态变化触发器（修复版）
-- Feature: 004-bugfixes-and-enhancements
-- Date: 2025-12-28
-- Update: 2026-01-03 - 修复为监听主订单表

DELIMITER $$

-- 删除旧的触发器
DROP TRIGGER IF EXISTS trg_order_status_change$$

-- 创建新触发器：监听主订单表的 SampleStatus 字段变化
CREATE TRIGGER trg_order_status_change
AFTER UPDATE ON lin_lb_listdiagnosisproject
FOR EACH ROW
BEGIN
  -- 检查 SampleStatus 是否发生变化且不是初始插入
  IF OLD.SampleStatus != NEW.SampleStatus AND OLD.SampleStatus IS NOT NULL THEN

    -- 插入通知记录
    INSERT INTO lin_lb_notification (
      order_id,
      hospital_id,
      old_status,
      new_status,
      operator_id,
      operation_time,
      createtime,
      updatetime
    ) VALUES (
      NEW.ListDiagnosisProjectId,
      NEW.InfoHospitalId,
      OLD.SampleStatus,
      NEW.SampleStatus,
      NULL,
      NOW(),
      UNIX_TIMESTAMP(),
      UNIX_TIMESTAMP()
    );

  END IF;
END$$

DELIMITER ;
