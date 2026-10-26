-- 用户决定暂不修改，记录 TODO；禁止自动执行，需再次批准。
-- 用户于 2026-10-24 明确暂不修改，已记录 TODO；不得自动执行，后续需再次批准。
-- 拟修复范围仅为本机开发副本的通知触发器状态文案。
-- 不改表、字段或历史数据；不在 Mac mini 原库执行。
-- 原触发器将 -3 显示为已取消、1 显示为已审核，与实际业务冲突。
DELIMITER $$
DROP TRIGGER IF EXISTS trg_order_status_change$$
CREATE DEFINER=`laboklin`@`%` TRIGGER trg_order_status_change
AFTER UPDATE ON lin_lb_listdiagnosisproject FOR EACH ROW
BEGIN
 DECLARE status_text VARCHAR(100);
 DECLARE message_content TEXT;
 IF OLD.SampleStatus != NEW.SampleStatus AND OLD.SampleStatus IS NOT NULL THEN
  SET status_text = CASE NEW.SampleStatus
   WHEN -3 THEN '待支付' WHEN -2 THEN '草稿' WHEN 0 THEN '待审核'
   WHEN 1 THEN '审核未通过' WHEN 2 THEN '等待报告' WHEN 3 THEN '已完成'
   WHEN 4 THEN '已取消' ELSE '状态更新' END;
  SET message_content = CONCAT('您的宠物「',NEW.AnimalName,'」（主人：',NEW.MasterName,'）的检测订单状态已更新为：',status_text);
  INSERT INTO lin_lb_notification(order_id,hospital_id,old_status,new_status,animal_name,master_name,notification_message,operator_id,operation_time,createtime,updatetime)
   VALUES(NEW.ListDiagnosisProjectId,NEW.InfoHospitalId,OLD.SampleStatus,NEW.SampleStatus,NEW.AnimalName,NEW.MasterName,message_content,NULL,NOW(),UNIX_TIMESTAMP(),UNIX_TIMESTAMP());
 END IF;
END$$
DELIMITER ;
