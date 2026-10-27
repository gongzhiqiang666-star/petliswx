package com.petlis.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportWorkflowService {
    private final FeatureStore store;
    private final AccessService access;
    private final LabWorkflowService laboratory;
    private final BusinessNotifications notifications;
    private final ReportPdfGenerator pdf;
    private final PlatformFileService files;
    private final ObjectMapper json = new ObjectMapper();
    @org.springframework.beans.factory.annotation.Value("${server.domain:}")
    private String serverDomain;

    @Transactional
    public List<Long> generate(long taskId, String summary, String conclusion) {
        if (blank(summary) || blank(conclusion) || summary.length() > 4000 || conclusion.length() > 4000) throw new BusinessException("报告摘要和结论不能为空");
        labActor();
        Map<String, Object> task = laboratory.taskForReport(taskId);
        if (!List.of("RESULT_ENTERED", "REJECTED", "REPORT_REVIEW").contains(String.valueOf(task.get("status")))) throw invalidState();
        List<Map<String, Object>> resultRows = laboratory.activeResults(taskId);
        if (resultRows.size() != 1) throw new BusinessException(422, "当前批次结果不完整");
        int orderId = number(task.get("order_id")).intValue();
        int laboratoryId = number(task.get("laboratory_id")).intValue();
        List<Map<String, Object>> details = store.jdbc().queryForList("SELECT DiagnosisDetailId AS detail_id,InfoTestTypeId AS info_test_type_id FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND IsCheckPass=0 AND IsActive=1 ORDER BY DiagnosisDetailId", orderId);
        Long resultCount = store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND IsCheckPass=0 AND IsActive=1", Long.class, orderId);
        if (details.isEmpty() || resultCount == null || resultCount != details.size()) throw new BusinessException(422, "订单检测项目不完整");
        List<Map<String, Object>> results = parseResults(resultRows.get(0).get("result_items"));
        if (results.size() != details.size()) throw new BusinessException(422, "所有项目结果完整后才能生成报告");
        if (Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_report_workflow WHERE order_id=? AND status IN ('PENDING_REVIEW','APPROVED','PUBLISHED'))", Boolean.class, orderId)))
            throw new BusinessException(409, "该订单已存在有效报告工作流，不能重复生成");
        Map<Integer, List<Map<String, Object>>> itemsByDetail = new java.util.HashMap<>();
        Map<Integer, Map<String, Object>> selectedTemplates = new java.util.HashMap<>();
        for (Map<String, Object> detail : details) {
            List<Map<String, Object>> templates = store.jdbc().queryForList("SELECT id,version,content FROM lin_report_template WHERE laboratory_id=? AND info_test_type_id=? AND status='ACTIVE' ORDER BY version DESC LIMIT 1", laboratoryId, number(detail.get("info_test_type_id")).intValue());
            Map<String, Object> selected = templates.isEmpty() ? Map.of("content", "默认检测模板") : templates.get(0);
            int detailId = number(detail.get("detail_id")).intValue();
            selectedTemplates.put(detailId, selected);
            String template = String.valueOf(selected.get("content"));
            List<Map<String, Object>> ownItems = new ArrayList<>();
            for (Map<String, Object> result : results) {
                if (number(result.get("orderItemId")).intValue() == detailId) {
                    var item = new java.util.LinkedHashMap<String, Object>(result);
                    item.put("template", template);
                    ownItems.add(item);
                }
            }
            itemsByDetail.put(detailId, ownItems);
        }
        if (itemsByDetail.values().stream().mapToInt(List::size).sum() != details.size()) throw new BusinessException(422, "结果项目与订单明细不匹配");
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> detail : details) {
            int detailId = number(detail.get("detail_id")).intValue();
            Map<String, Object> template = selectedTemplates.get(detailId);
            List<Map<String, Object>> ownItems = itemsByDetail.get(detailId);
            long workflowId = store.insert("INSERT INTO lin_report_workflow(order_id,detail_id,test_result_id,template_id,template_version,hospital_id,laboratory_id,status,submitted_by,submitted_at) VALUES(?,?,?,?,?,?,?,'PENDING_REVIEW',?,NOW())",
                    orderId, detailId, number(resultRows.get(0).get("id")).longValue(),
                    template.get("id"), template.get("version"), number(task.get("hospital_id")).intValue(), laboratoryId, access.current().userId());
            byte[] content = pdf.create("RPT-" + workflowId, summary.trim(), conclusion.trim(), String.valueOf(resultRows.get(0).get("batch_no")), ownItems);
            PlatformFileService.Stored stored = files.saveReport(number(task.get("hospital_id")).intValue(), laboratoryId, workflowId, content);
            store.jdbc().update("UPDATE lin_report_workflow SET pdf_file_id=?,version=version+1 WHERE id=?", stored.id(), workflowId);
            store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,after_json) VALUES(?,'GENERATED','REPORT_WORKFLOW',?,?)",
                    access.current().userId(), String.valueOf(workflowId), store.encode(Map.of("summary", summary, "conclusion", conclusion, "items", ownItems)));
            ids.add(workflowId);
        }
        store.jdbc().update("UPDATE lin_lab_task SET status='REPORT_REVIEW',version=version+1 WHERE id=?", taskId);
        return ids;
    }

    @Transactional
    public void review(long workflowId, String decision, String reason) {
        labActor();
        Map<String, Object> report = manageable(workflowId);
        if (!"PENDING_REVIEW".equals(report.get("status"))) throw invalidState();
        String target;
        if ("APPROVE".equals(decision)) target = "APPROVED";
        else if ("REJECT".equals(decision) && !blank(reason)) target = "REJECTED";
        else throw new BusinessException("审核决定无效，驳回必须填写原因");
        store.jdbc().update("UPDATE lin_report_workflow SET status=?,reviewed_by=?,reviewed_at=NOW(),review_note=?,version=version+1 WHERE id=?", target, access.current().userId(), reason, workflowId);
        if (report.get("submitted_by") != null) notifications.emit(number(report.get("submitted_by")).intValue(), "report", "report-workflow:" + workflowId + ":" + target,
                "报告审核" + ("APPROVED".equals(target) ? "通过" : "驳回"), blank(reason) ? "审核完成" : reason, "pages/order-detail/order-detail?id=" + report.get("order_id"));
    }

    @Transactional
    public Map<String, Object> publish(long workflowId) {
        Map<String, Object> order = store.one("SELECT w.order_id,w.test_result_id,o.SampleStatus FROM lin_report_workflow w JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=w.order_id WHERE w.id=? FOR UPDATE OF o", workflowId);
        int orderId = number(order.get("order_id")).intValue();
        long resultId = number(order.get("test_result_id")).longValue();
        Map<String, Object> task = store.one("SELECT t.id,t.status FROM lin_lab_task t JOIN lin_test_result r ON r.lab_task_id=t.id WHERE r.id=? FOR UPDATE OF t", resultId);
        labActor();
        Map<String, Object> report = manageable(workflowId);
        String status = String.valueOf(report.get("status"));
        if (!List.of("APPROVED", "PUBLISHED").contains(status)) throw invalidState();
        int detailId = number(report.get("detail_id")).intValue();
        if ("APPROVED".equals(status)) {
            if (report.get("result_status") != null && !"ENTERED".equals(report.get("result_status"))) throw new BusinessException(409, "检测结果已作废，不能发布报告");
            if (!isMonthly(report) && number(report.get("is_pay_money")).intValue() != 1) throw new BusinessException(422, "现结订单未支付，不能发布报告");
            String fileName = report.get("storage_path") == null ? "report-" + report.get("pdf_file_id") + ".pdf" : report.get("storage_path").toString();
            String pdfUrl = serverDomain + "/Content/FilePdf/" + fileName;
            String result = legacyContent(workflowId, report);
            store.jdbc().update("INSERT INTO lin_lb_diagnosisdetail_info(diagnosisdetail_id,ReportResult,MailReport) VALUES(?,?,?) ON DUPLICATE KEY UPDATE ReportResult=VALUES(ReportResult),MailReport=VALUES(MailReport)", detailId, result, result);
            store.jdbc().update("UPDATE lin_lb_diagnosisdetail SET DiagnosisStatus=2,IsSendMail=1,PDFHttpUrl=?,SendMailDt=NOW() WHERE DiagnosisDetailId=?", pdfUrl, detailId);
            store.jdbc().update("UPDATE lin_report_workflow SET status='PUBLISHED',payment_checked=1,published_by=?,published_at=NOW(),legacy_synced_at=NOW(),version=version+1 WHERE id=?", access.current().userId(), workflowId);
        }
        Map<String, Object> completion = store.one("SELECT COUNT(DISTINCT d.DiagnosisDetailId) AS total,COUNT(DISTINCT CASE WHEN w.status='PUBLISHED' THEN d.DiagnosisDetailId END) AS published FROM lin_lb_diagnosisdetail d LEFT JOIN lin_report_workflow w ON w.detail_id=d.DiagnosisDetailId WHERE d.ListDiagnosisProjectId=? AND d.IsCheckPass=0 AND d.IsActive=1 FOR UPDATE", orderId);
        if (number(completion.get("total")).intValue() == number(completion.get("published")).intValue()) {
            boolean changed = false;
            if (number(order.get("SampleStatus")).intValue() != 3) {
                store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=3,SendReportDt=NOW() WHERE ListDiagnosisProjectId=?", orderId);
                changed = true;
            }
            if (!"COMPLETED".equals(task.get("status"))) {
                store.jdbc().update("UPDATE lin_lab_task SET status='COMPLETED',completed_at=NOW(),version=version+1 WHERE id=(SELECT lab_task_id FROM lin_test_result WHERE id=?)", resultId);
                changed = true;
            }
            if (changed) notifications.emitForOrder(orderId, integer(report.get("doctor_user_id")), "report", orderId + ":published",
                    "检测报告已发布", "送检单 #" + orderId + " 的全部检测报告已发布", "pages/report-detail/report-detail?id=" + detailId);
        }
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(long workflowId) {
        Map<String, Object> report = store.one(reportSelect() + " WHERE w.id=?", workflowId);
        CurrentActor actor = access.current();
        if (!actor.isAdmin()) {
            if (actor.hasRole(PlatformRole.LAB_STAFF)) access.requireLaboratory(number(report.get("laboratory_id")).intValue());
            else {
                access.requireHospital(number(report.get("hospital_id")).intValue());
                if (!"PUBLISHED".equals(report.get("status"))) throw new BusinessException(404, "报告尚未发布");
                requirePayment(report);
            }
        }
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String status, int page, int size) {
        CurrentActor actor = access.current();
        String from = " FROM lin_report_workflow w JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=w.order_id JOIN lin_lb_infohospital h ON h.InfoHospitalId=w.hospital_id LEFT JOIN lin_order_extension e ON e.order_id=w.order_id";
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!actor.isAdmin()) {
            if (actor.hasRole(PlatformRole.LAB_STAFF)) {
                where.append(" AND w.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)"); args.add(actor.userId());
            } else {
                where.append(" AND w.status='PUBLISHED' AND (COALESCE(e.settlement_mode,CASE WHEN h.PayType=1 THEN 'monthly' ELSE 'cash' END)='monthly' OR o.IsPayMoney=1) AND w.hospital_id IN (SELECT hospital_id FROM lin_user_role WHERE user_id=? AND role_code IN ('DOCTOR','HOSPITAL_ADMIN') AND is_active=1)"); args.add(actor.userId());
            }
        }
        if (!blank(status)) { where.append(" AND w.status=?"); args.add(status); }
        String select = "SELECT w.id,w.order_id AS orderId,w.detail_id AS detailId,w.test_result_id AS testResultId,w.hospital_id AS hospitalId,w.laboratory_id AS laboratoryId,w.status,w.pdf_file_id AS pdfFileId,w.submitted_at AS submittedAt,w.reviewed_at AS reviewedAt,w.published_at AS publishedAt" + from;
        return store.page("SELECT COUNT(*)" + from + where, select + where + " ORDER BY w.id DESC", page, size, args.toArray());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(long workflowId) {
        Map<String, Object> report = get(workflowId);
        if (!"PUBLISHED".equals(report.get("status")) && !access.current().isAdmin() && !access.current().hasRole(PlatformRole.LAB_STAFF)) throw new BusinessException(404, "报告尚未发布");
        return files.downloadReport(number(report.get("pdf_file_id")).longValue(), workflowId);
    }

    @Transactional
    public long saveTemplate(int infoTestTypeId, int laboratoryId, String name, String code, String content) {
        labActor();
        if (!access.current().isAdmin()) access.requireLaboratory(laboratoryId);
        if (blank(name) || blank(code) || blank(content)) throw new BusinessException("模板名称、编码和内容不能为空");
        Long version = store.jdbc().queryForObject("SELECT COALESCE(MAX(version),0)+1 FROM lin_report_template WHERE laboratory_id=? AND template_code=?", Long.class, laboratoryId, code);
        store.jdbc().update("UPDATE lin_report_template SET status='ARCHIVED' WHERE laboratory_id=? AND info_test_type_id=? AND template_code=? AND status='ACTIVE'", laboratoryId, infoTestTypeId, code);
        return store.insert("INSERT INTO lin_report_template(laboratory_id,info_test_type_id,template_name,template_code,content,version,status,created_by) VALUES(?,?,?,?,?,?,'ACTIVE',?)",
                laboratoryId, infoTestTypeId, name.trim(), code.trim(), content, version, access.current().userId());
    }

    private Map<String, Object> manageable(long id) {
        Map<String, Object> report = store.one(reportSelect() + " WHERE w.id=? FOR UPDATE", id);
        if (!access.current().isAdmin()) access.requireLaboratory(number(report.get("laboratory_id")).intValue());
        return report;
    }

    private String reportSelect() {
        return "SELECT w.*,r.batch_no,r.result_status,f.storage_path,o.CreateSampleId AS doctor_user_id,o.IsPayMoney AS is_pay_money,h.PayType AS pay_type,e.settlement_mode FROM lin_report_workflow w "
                + "JOIN lin_test_result r ON r.id=w.test_result_id JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=w.order_id "
                + "JOIN lin_lb_infohospital h ON h.InfoHospitalId=w.hospital_id LEFT JOIN lin_file f ON f.id=w.pdf_file_id LEFT JOIN lin_order_extension e ON e.order_id=w.order_id";
    }

    private String legacyContent(long workflowId, Map<String, Object> report) {
        List<Map<String, Object>> snapshots = store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE resource_type='REPORT_WORKFLOW' AND resource_id=? AND action='GENERATED' ORDER BY id DESC LIMIT 1", String.valueOf(workflowId));
        if (!snapshots.isEmpty()) {
            try {
                Map<String, Object> snapshot = json.readValue(String.valueOf(snapshots.get(0).get("after_json")), new TypeReference<>() { });
                StringBuilder text = new StringBuilder("检验报告\n样本摘要：").append(snapshot.get("summary"))
                        .append("\n临床结论：").append(snapshot.get("conclusion")).append("\n检测结果");
                List<Map<String, Object>> items = json.convertValue(snapshot.get("items"), new TypeReference<>() { });
                for (Map<String, Object> item : items) {
                    text.append("\n项目模板：").append(item.get("template"))
                            .append("\n").append(item.get("name")).append("：").append(item.get("value")).append(" ").append(item.get("unit"))
                            .append("｜参考范围：").append(item.get("referenceRange"))
                            .append("｜").append(Boolean.TRUE.equals(item.get("abnormalFlag")) ? "异常" : "正常");
                }
                return text.toString();
            } catch (Exception error) {
                throw new BusinessException("报告快照内容无效");
            }
        }
        return "检验报告\n订单 #" + report.get("order_id") + "\n检测批次：" + report.get("batch_no") + "\n报告已审核发布";
    }

    private List<Map<String, Object>> parseResults(Object value) {
        try { return json.readValue(String.valueOf(value), new TypeReference<>() { }); }
        catch (Exception error) { throw new BusinessException("检测结果内容无效"); }
    }

    private void requirePayment(Map<String, Object> report) {
        if (!isMonthly(report) && number(report.get("is_pay_money")).intValue() != 1) throw new BusinessException(403, "现结订单未支付，报告不可见");
    }
    private boolean isMonthly(Map<String, Object> report) {
        Object snapshot = report.get("settlement_mode");
        return snapshot == null ? number(report.get("pay_type")).intValue() == 1 : "monthly".equalsIgnoreCase(snapshot.toString());
    }
    private void labActor() { if (!access.current().isAdmin() && !access.current().hasRole(PlatformRole.LAB_STAFF)) throw new BusinessException(403, "仅实验人员可操作报告工作流"); }
    private BusinessException invalidState() { return new BusinessException(409, "报告状态不允许该操作"); }
    private Number number(Object value) { return (Number) value; }
    private Integer integer(Object value) { return value == null ? null : number(value).intValue(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
