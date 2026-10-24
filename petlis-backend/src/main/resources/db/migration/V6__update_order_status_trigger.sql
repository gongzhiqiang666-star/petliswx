-- V6__update_order_status_trigger.sql
-- 更新订单状态变化触发器，添加详细信息
-- Feature: 004-bugfixes-and-enhancements
-- Date: 2026-01-03

DELIMITER $$

-- 删除旧触发器
DROP TRIGGER IF EXISTS trg_order_status_change$$

-- 创建新触发器：包含宠物和主人信息
CREATE TRIGGER trg_order_status_change
AFTER UPDATE ON lin_lb_listdiagnosisproject
FOR EACH ROW
BEGIN
  DECLARE status_text VARCHAR(100);
  DECLARE message_content TEXT;

  -- 检查 SampleStatus 是否发生变化且不是初始插入
  IF OLD.SampleStatus != NEW.SampleStatus AND OLD.SampleStatus IS NOT NULL THEN

    -- 生成状态描述
    SET status_text = CASE NEW.SampleStatus
      WHEN -3 THEN '已取消'
      WHEN -2 THEN '草稿'
      WHEN 0 THEN '待审核'
      WHEN 1 THEN '已审核'
      WHEN 2 THEN '检测中'
      WHEN 3 THEN '检测完成'
      ELSE '状态更新'
    END;

    -- 生成通知消息内容
    SET message_content = CONCAT(
      '您的宠物「', NEW.AnimalName, '」（主人：', NEW.MasterName, '）',
      '的检测订单状态已更新为：', status_text
    );

    -- 插入通知记录（包含详细信息）
    INSERT INTO lin_lb_notification (
      order_id,
      hospital_id,
      old_status,
      new_status,
      animal_name,
      master_name,
      notification_message,
      operator_id,
      operation_time,
      createtime,
      updatetime
    ) VALUES (
      NEW.ListDiagnosisProjectId,
      NEW.InfoHospitalId,
      OLD.SampleStatus,
      NEW.SampleStatus,
      NEW.AnimalName,
      NEW.MasterName,
      message_content,
      NULL,
      NOW(),
      UNIX_TIMESTAMP(),
      UNIX_TIMESTAMP()
    );

  END IF;
END$$

DELIMITER ;
