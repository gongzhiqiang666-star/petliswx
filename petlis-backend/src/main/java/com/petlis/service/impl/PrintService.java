package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrintService {
    private final FeatureStore store;
    private final AccessService access;
    private final AuditService audits;

    public String order(int orderId) {
        Map<String, Object> order = order(orderId, true);
        var items = store.jdbc().queryForList("SELECT d.DiagnosisDetailId AS detailId,t.TestName AS testName,d.TestPrice AS price FROM lin_lb_diagnosisdetail d JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=d.InfoTestTypeId WHERE d.ListDiagnosisProjectId=? AND d.IsCheckPass=0 ORDER BY d.DiagnosisDetailId", orderId);
        StringBuilder rows = new StringBuilder();
        for (var item : items) rows.append("<tr><td>").append(item.get("detailId")).append("</td><td>").append(escape(item.get("testName"))).append("</td><td>").append(item.get("price")).append("</td></tr>");
        audits.record("PRINT_ORDER", "ORDER", orderId, null, Map.of("format", "HTML"), null);
        return page("送检单", "<h1>送检单 #" + order.get("orderNo") + "</h1><p>医院：" + escape(order.get("hospitalName")) + "</p><p>宠物：" + escape(order.get("animalName")) + "</p><p>金额：" + order.get("amount") + " 元</p><table border=1><tr><th>明细ID</th><th>项目</th><th>价格（元）</th></tr>" + rows + "</table>");
    }

    public String specimenLabel(int orderId) {
        Map<String, Object> order = order(orderId, true);
        String code = order.get("orderNo").toString();
        audits.record("PRINT_SPECIMEN_LABEL", "ORDER", orderId, null, Map.of("format", "HTML", "barcode", "CODE39"), null);
        return page("标本标签", "<h1>标本标签</h1><p>" + escape(order.get("hospitalName")) + " / " + escape(order.get("animalName")) + "</p>" + barcode(code) + "<p>" + escape(code) + "</p>");
    }

    public String report(int detailId) {
        Map<String, Object> report = store.one("SELECT d.DiagnosisDetailId AS detailId,o.ListDiagnosisProjectId AS orderId,o.InfoHospitalId AS hospitalId,e.laboratory_id AS laboratoryId,COALESCE(e.settlement_mode,'cash') AS settlementMode,o.SampleStatus AS sampleStatus,o.IsPayMoney AS paid,i.ReportResult AS result,o.AnimalName AS animalName FROM lin_lb_diagnosisdetail_info i JOIN lin_lb_diagnosisdetail d ON d.DiagnosisDetailId=i.DiagnosisdetailId JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=d.ListDiagnosisProjectId LEFT JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId WHERE d.DiagnosisDetailId=?", detailId);
        authorize(report);
        if (((Number) report.get("sampleStatus")).intValue() != 3) throw new BusinessException(409, "报告尚未完成，禁止打印");
        if ("cash".equals(report.get("settlementMode")) && ((Number) report.get("paid")).intValue() != 1) throw new BusinessException(403, "现结订单未支付或已退款，禁止打印报告");
        audits.record("PRINT_REPORT", "DIAGNOSIS_DETAIL", detailId, null, Map.of("format", "HTML"), null);
        return page("检验报告", "<h1>检验报告</h1><p>明细 #" + detailId + " / 宠物：" + escape(report.get("animalName")) + "</p><article>" + escape(report.get("result")) + "</article>");
    }

    private Map<String, Object> order(int id, boolean authorize) {
        Map<String, Object> order = store.one("SELECT o.ListDiagnosisProjectId AS orderId,o.UserNO AS orderNo,o.InfoHospitalId AS hospitalId,e.laboratory_id AS laboratoryId,h.HospitalName AS hospitalName,o.AnimalName AS animalName,o.TestFee AS amount FROM lin_lb_listdiagnosisproject o JOIN lin_lb_infohospital h ON h.InfoHospitalId=o.InfoHospitalId LEFT JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId WHERE o.ListDiagnosisProjectId=?", id);
        if (authorize) authorize(order);
        return order;
    }
    private void authorize(Map<String, Object> resource) {
        CurrentActor actor = access.current();
        int hospital = ((Number) resource.get("hospitalId")).intValue();
        Integer laboratory = resource.get("laboratoryId") == null ? null : ((Number) resource.get("laboratoryId")).intValue();
        if (actor.isAdmin() || actor.hasRole(PlatformRole.HOSPITAL_ADMIN) && actor.canAccessHospital(hospital)
                || actor.hasRole(PlatformRole.LAB_STAFF) && laboratory != null && actor.canAccessLaboratory(laboratory)) return;
        throw new BusinessException(403, "无权打印该资料");
    }
    private static String page(String title, String body) { return "<!doctype html><html lang=zh-CN><head><meta charset=utf-8><title>" + title + "</title><style>body{font-family:sans-serif;padding:24px}table{border-collapse:collapse}td,th{padding:6px}@media print{button{display:none}}</style></head><body>" + body + "<p><button onclick=print()>打印</button></p></body></html>"; }
    private static String escape(Object value) { return value == null ? "" : value.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }

    private static String barcode(String value) {
        String encoded = "*" + value.toUpperCase() + "*";
        StringBuilder rects = new StringBuilder("<svg xmlns='http://www.w3.org/2000/svg' width='420' height='80' viewBox='0 0 420 80' role='img' aria-label='CODE39 条码'><rect width='100%' height='100%' fill='white'/>");
        int x = 8;
        for (char c : encoded.toCharArray()) {
            String pattern = patterns.get(c);
            if (pattern == null) throw new BusinessException("订单编号包含无法生成条码的字符");
            for (int i = 0; i < 9; i++) {
                int width = pattern.charAt(i) == 'w' ? 3 : 1;
                if (i % 2 == 0) rects.append("<rect x='").append(x).append("' y='5' width='").append(width).append("' height='60' fill='black'/>");
                x += width;
            }
            x++;
        }
        return rects.append("</svg>").toString();
    }
    private static final Map<Character, String> patterns = Map.ofEntries(
            Map.entry('0', "nnnwwnwnn"), Map.entry('1', "wnnwnnnnw"), Map.entry('2', "nnwwnnnnw"), Map.entry('3', "wnwwnnnnn"),
            Map.entry('4', "nnnwwnnnw"), Map.entry('5', "wnnwwnnnn"), Map.entry('6', "nnwwwnnnn"), Map.entry('7', "nnnwnnwnw"),
            Map.entry('8', "wnnwnnwnn"), Map.entry('9', "nnwwnnwnn"), Map.entry('-', "nnwnnwnnw"), Map.entry('*', "nwwnwnnwn"),
            Map.entry('A', "wnnnnwnnw"), Map.entry('B', "nnwnnwnnw"), Map.entry('C', "wnwnnwnnn"), Map.entry('D', "nnnnwwnnw"),
            Map.entry('E', "wnnnwwnnn"), Map.entry('F', "nnwnwwnnn"), Map.entry('G', "nnnnnwwnw"), Map.entry('H', "wnnnnwwnn"),
            Map.entry('I', "nnwnnwwnn"), Map.entry('J', "nnnnwwwnn"), Map.entry('K', "wnnnnnnww"), Map.entry('L', "nnwnnnnww"),
            Map.entry('M', "wnwnnnnwn"), Map.entry('N', "nnnnwnnww"), Map.entry('O', "wnnnwnnwn"), Map.entry('P', "nnwnwnnwn"),
            Map.entry('Q', "nnnnnnwww"), Map.entry('R', "wnnnnnwwn"), Map.entry('S', "nnwnnnwwn"), Map.entry('T', "nnnnwnwwn"),
            Map.entry('U', "wwnnnnnnw"), Map.entry('V', "nwwnnnnnw"), Map.entry('W', "wwwnnnnnn"), Map.entry('X', "nwnnwnnnw"),
            Map.entry('Y', "wwnnwnnnn"), Map.entry('Z', "nwwnwnnnn"));
}
