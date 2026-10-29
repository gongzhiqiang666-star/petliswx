package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabWorkflowService {
    private final FeatureStore store;
    private final AccessService access;
    private final BusinessNotifications notifications;

    public record ResultsCommand(List<ResultCommand> results) { }
    public record ResultCommand(Integer orderItemId, String name, String value, String unit,
                                String referenceRange, Boolean abnormalFlag) { }

    @Transactional
    public Map<String, Object> assign(int orderId, int assigneeId, String equipmentCode) {
        CurrentActor actor = labActor();
        Map<String, Object> shipment = store.one("SELECT s.*,o.CreateSampleId AS doctor_user_id FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.order_id=? AND s.status='RECEIVED' FOR UPDATE", orderId);
        int labId = number(shipment.get("laboratory_id")).intValue();
        access.requireLaboratory(labId);
        if (blank(equipmentCode) || equipmentCode.length() > 64 || !assigneeExists(assigneeId, labId)) throw new BusinessException("检测人员必须属于本实验室，设备不能为空");
        int updated = store.jdbc().update("UPDATE lin_lab_task SET assigned_to=?,equipment_code=?,status='TESTING',started_at=NOW(),version=version+1 WHERE order_id=? AND laboratory_id=? AND status='RECEIVED'", assigneeId, equipmentCode.trim(), orderId, labId);
        if (updated != 1) throw invalidState();
        Map<String, Object> task = store.one("SELECT t.*,o.CreateSampleId AS doctor_user_id FROM lin_lab_task t JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=t.order_id WHERE t.order_id=? AND t.task_type='INITIAL'", orderId);
        notifications.emit(assigneeId, "order", "lab-task:" + task.get("id") + ":ASSIGNED", "新检测任务", "送检单 #" + orderId + " 已分配检测任务", "pages/order-detail/order-detail?id=" + orderId);
        return view(task);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(long taskId) { return view(accessible(taskId, false)); }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String status, int page, int size) {
        CurrentActor actor = access.current();
        if (!actor.isAdmin() && !actor.hasRole(PlatformRole.LAB_STAFF)) throw new BusinessException(403, "无权查看实验任务");
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" FROM lin_lab_task t WHERE 1=1");
        if (!actor.isAdmin()) { where.append(" AND t.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)"); args.add(actor.userId()); }
        if (!blank(status)) { where.append(" AND t.status=?"); args.add(status); }
        String select = "SELECT t.id,t.order_id AS orderId,t.detail_id AS detailId,t.laboratory_id AS labId,t.assigned_to AS assignedTo,t.equipment_code AS equipmentName,t.status,t.task_type AS taskType,t.parent_task_id AS parentTaskId,t.received_at AS receivedAt,t.started_at AS assignedAt,t.completed_at AS completedAt";
        return store.page("SELECT COUNT(*)" + where, select + where + " ORDER BY t.id DESC", page, size, args.toArray());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> assignees(int laboratoryId) {
        CurrentActor actor = access.current();
        if (!actor.isAdmin()) access.requireLaboratory(laboratoryId);
        var items = store.jdbc().queryForList("SELECT u.id,u.nickname AS name,u.username FROM lin_user u JOIN lin_user_role r ON r.user_id=u.id WHERE r.laboratory_id=? AND r.role_code='LAB_STAFF' AND r.is_active=1 AND u.status='normal' ORDER BY u.id", laboratoryId);
        return Map.of("items", items, "total", items.size(), "page", 0, "size", Math.max(1, items.size()));
    }

    @Transactional
    public long enterResults(long taskId, ResultsCommand command) {
        Map<String, Object> task = accessible(taskId, true);
        if (!access.current().userId().equals(integer(task.get("assigned_to")))) throw new BusinessException(403, "仅被分配的检测人员可录入结果");
        if (!"TESTING".equals(task.get("status"))) throw invalidState();
        List<Integer> detailIds = store.jdbc().queryForList("SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND IsCheckPass=0 AND IsActive=1 ORDER BY DiagnosisDetailId", Integer.class, number(task.get("order_id")).intValue());
        validateResults(command, detailIds);
        String batch = batch(task);
        if (Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_test_result WHERE lab_task_id=? AND batch_no=?)", Boolean.class, taskId, batch))) throw new BusinessException(409, "该批次结果已录入，历史不可覆盖");
        var normalized = new ArrayList<Map<String, Object>>();
        for (ResultCommand input : command.results()) {
            Boolean abnormal = input.abnormalFlag() == null ? deriveAbnormal(input.value(), input.referenceRange()) : input.abnormalFlag();
            if (abnormal == null) throw new BusinessException("定性结果或无法识别的参考范围必须明确异常标记");
            normalized.add(Map.of("orderItemId", input.orderItemId(), "name", input.name().trim(), "value", input.value().trim(),
                    "unit", input.unit().trim(), "referenceRange", input.referenceRange().trim(), "abnormalFlag", abnormal));
        }
        CurrentActor actor = access.current();
        long id = store.insert("INSERT INTO lin_test_result(lab_task_id,order_id,detail_id,laboratory_id,batch_no,is_retest,result_status,result_items,entered_by) VALUES(?,?,?,?,?,?,'ENTERED',?,?)",
                taskId, number(task.get("order_id")).intValue(), detailIds.get(0), number(task.get("laboratory_id")).intValue(), batch,
                "RETEST".equals(task.get("task_type")) ? 1 : 0, store.encode(normalized), actor.userId());
        store.jdbc().update("UPDATE lin_lab_task SET status='RESULT_ENTERED',version=version+1 WHERE id=?", taskId);
        return id;
    }

    @Transactional
    public void returnSample(long taskId, String reason) {
        if (blank(reason) || reason.length() > 1000) throw new BusinessException("退样原因不能为空");
        Map<String, Object> task = accessible(taskId, true);
        if (!Set.of("RECEIVED", "TESTING", "RESULT_ENTERED", "REPORT_REVIEW", "REJECTED").contains(String.valueOf(task.get("status")))) throw invalidState();
        store.jdbc().update("UPDATE lin_test_result SET result_status='VOIDED',reviewed_by=?,reviewed_at=NOW(),version=version+1 WHERE lab_task_id=? AND result_status<>'VOIDED'", access.current().userId(), taskId);
        store.jdbc().update("UPDATE lin_report_workflow SET status='CANCELLED',version=version+1 WHERE test_result_id IN (SELECT id FROM lin_test_result WHERE lab_task_id=?) AND status<>'PUBLISHED'", taskId);
        store.jdbc().update("UPDATE lin_lab_task SET status='RETURNED',return_reason=?,returned_at=NOW(),version=version+1 WHERE id=?", reason.trim(), taskId);
        notify(task, "RETURNED", "样本已退样", reason.trim());
    }

    @Transactional
    public long retest(long taskId, String reason) {
        if (blank(reason) || reason.length() > 1000) throw new BusinessException("复检原因不能为空");
        Map<String, Object> task = accessible(taskId, true);
        if ("RETESTED".equals(task.get("status"))) {
            Map<String, Object> child = store.one("SELECT id,return_reason FROM lin_lab_task WHERE parent_task_id=? AND task_type='RETEST' ORDER BY id DESC LIMIT 1", taskId);
            if (!reason.trim().equals(child.get("return_reason"))) throw new BusinessException(409, "相同复检请求已处理，变更原因请使用新请求");
            return number(child.get("id")).longValue();
        }
        if (!"RETURNED".equals(task.get("status"))) throw invalidState();
        CurrentActor actor = access.current();
        store.jdbc().update("UPDATE lin_test_result SET result_status='VOIDED',reviewed_by=?,reviewed_at=NOW(),version=version+1 WHERE lab_task_id=? AND result_status<>'VOIDED'", actor.userId(), taskId);
        long id = store.insert("INSERT INTO lin_lab_task(shipment_id,order_id,detail_id,laboratory_id,parent_task_id,task_type,status,assigned_to,equipment_code,received_by,received_at,started_at,return_reason) SELECT shipment_id,order_id,detail_id,laboratory_id,?,'RETEST','TESTING',assigned_to,equipment_code,received_by,received_at,NOW(),? FROM lin_lab_task WHERE id=?", taskId, reason.trim(), taskId);
        store.jdbc().update("UPDATE lin_lab_task SET status='RETESTED',version=version+1 WHERE id=?", taskId);
        notify(task, "RETEST:" + id, "已发起复检", reason.trim());
        return id;
    }

    @Transactional
    public Map<String, Object> taskForReport(long taskId) { return accessible(taskId, true); }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> activeResults(long taskId) {
        Map<String, Object> task = accessible(taskId, false);
        return store.jdbc().queryForList("SELECT * FROM lin_test_result WHERE lab_task_id=? AND batch_no=? AND result_status='ENTERED'", taskId, batch(task));
    }

    private Map<String, Object> accessible(long taskId, boolean locked) {
        Map<String, Object> task = store.one("SELECT t.*,o.CreateSampleId AS doctor_user_id,o.InfoHospitalId AS hospital_id,o.IsPayMoney AS is_pay_money,h.PayType AS pay_type FROM lin_lab_task t JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=t.order_id JOIN lin_lb_infohospital h ON h.InfoHospitalId=o.InfoHospitalId WHERE t.id=?" + (locked ? " FOR UPDATE" : ""), taskId);
        CurrentActor actor = labActor();
        if (!actor.isAdmin()) access.requireLaboratory(number(task.get("laboratory_id")).intValue());
        return task;
    }

    private CurrentActor labActor() {
        CurrentActor actor = access.current();
        if (!actor.isAdmin() && !actor.hasRole(PlatformRole.LAB_STAFF)) throw new BusinessException(403, "仅实验人员可执行该操作");
        return actor;
    }

    private boolean assigneeExists(int userId, int laboratoryId) {
        return Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_user_role r JOIN lin_user u ON u.id=r.user_id WHERE r.user_id=? AND r.laboratory_id=? AND r.role_code='LAB_STAFF' AND r.is_active=1 AND u.status='normal')", Boolean.class, userId, laboratoryId));
    }

    private void validateResults(ResultsCommand command, List<Integer> detailIds) {
        if (command == null || command.results() == null || detailIds.isEmpty() || command.results().size() != detailIds.size()) throw new BusinessException("必须完整录入所有检测项目结果");
        Set<Integer> actual = new HashSet<>();
        for (ResultCommand input : command.results()) {
            if (input == null || input.orderItemId() == null || !detailIds.contains(input.orderItemId()) || !actual.add(input.orderItemId())
                    || blank(input.name()) || blank(input.value()) || blank(input.unit()) || blank(input.referenceRange())
                    || input.name().length() > 200 || input.value().length() > 500 || input.unit().length() > 64 || input.referenceRange().length() > 200)
                throw new BusinessException("结果项目、名称、值、单位和参考范围必须完整且不可重复");
        }
    }

    private Boolean deriveAbnormal(String value, String range) {
        try {
            BigDecimal number = new BigDecimal(value.trim());
            String normalized = range.trim();
            String[] bounds = normalized.split("-");
            if (bounds.length == 2) return number.compareTo(new BigDecimal(bounds[0].trim())) < 0 || number.compareTo(new BigDecimal(bounds[1].trim())) > 0;
            if (normalized.startsWith("<")) return number.compareTo(new BigDecimal(normalized.substring(1).trim())) >= 0;
            if (normalized.startsWith(">")) return number.compareTo(new BigDecimal(normalized.substring(1).trim())) <= 0;
        } catch (NumberFormatException ignored) { }
        return null;
    }

    private Map<String, Object> view(Map<String, Object> task) {
        var view = new java.util.LinkedHashMap<String, Object>();
        task.forEach((key, value) -> view.put(key, value));
        view.put("labId", task.get("laboratory_id"));
        view.put("orderId", task.get("order_id"));
        view.put("equipmentName", task.get("equipment_code"));
        view.put("testBatch", batch(task));
        view.put("results", store.jdbc().queryForList("SELECT id,batch_no AS batchNo,result_status AS resultStatus,result_items AS resultItems,entered_at AS enteredAt FROM lin_test_result WHERE lab_task_id=? ORDER BY id", task.get("id")));
        view.put("orderItems", store.jdbc().queryForList("SELECT DiagnosisDetailId AS id,InfoTestTypeId AS infoTestTypeId,GenterNO AS itemNameSnapshot FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND IsCheckPass=0 AND IsActive=1 ORDER BY DiagnosisDetailId", task.get("order_id")));
        return view;
    }

    private String batch(Map<String, Object> task) {
        Object value = task.get("batch_no");
        return value == null ? "TASK-" + task.get("id") : value.toString();
    }

    private void notify(Map<String, Object> task, String action, String title, String message) {
        int orderId = number(task.get("order_id")).intValue();
        notifications.emitForOrder(orderId, integer(task.get("doctor_user_id")), "order", "lab-task:" + task.get("id") + ":" + action,
                title, message, "pages/order-detail/order-detail?id=" + orderId);
    }

    private BusinessException invalidState() { return new BusinessException(409, "实验任务状态不允许该操作"); }
    private Number number(Object value) { return (Number) value; }
    private Integer integer(Object value) { return value == null ? null : number(value).intValue(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
