package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final FeatureStore store;
    private final AccessService access;

    public Map<String, Object> dashboard(String from, String to) {
        CurrentActor actor = requireStaff();
        LocalDate start = LocalDate.parse(from), end = LocalDate.parse(to);
        if (!end.isAfter(start)) throw new BusinessException("统计结束日期必须晚于开始日期");
        String orderScope = actor.isAdmin() ? "" : " AND e.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)";
        String cashScope = actor.isAdmin() ? "" : " AND EXISTS(SELECT 1 FROM lin_order_extension pe JOIN lin_user_role ur ON ur.laboratory_id=pe.laboratory_id WHERE pe.order_id=p.order_id AND ur.user_id=? AND ur.role_code='LAB_STAFF' AND ur.is_active=1)";
        String labScope = actor.isAdmin() ? "" : " AND b.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)";
        Map<String, Object> totals = store.one("SELECT (SELECT COUNT(*) FROM lin_lb_listdiagnosisproject o LEFT JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId WHERE o.CreateDt>=? AND o.CreateDt<?" + orderScope + ") AS orderCount," +
                "(SELECT COALESCE(SUM(p.amount),0) FROM lin_wechat_payment p WHERE p.status=1 AND p.paid_at>=? AND p.paid_at<?" + cashScope + ") AS cashRevenue," +
                "(SELECT COALESCE(SUM(b.paid_amount),0) FROM lin_monthly_bill b WHERE b.status='SETTLED' AND b.settled_at>=? AND b.settled_at<?" + labScope + ") AS monthlyRevenue," +
                "(SELECT COALESCE(SUM(b.due_amount-b.paid_amount),0) FROM lin_monthly_bill b WHERE b.status<>'SETTLED'" + labScope + ") AS arrears", summaryArgs(actor, start, end));
        BigDecimal cash = money(totals.get("cashRevenue")), monthly = money(totals.get("monthlyRevenue"));
        String hospitalOrderScope = actor.isAdmin() ? "" : " AND e2.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)";
        String hospitalCashScope = actor.isAdmin() ? "" : " AND pe.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)";
        String hospitalBillScope = actor.isAdmin() ? "" : " AND b.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)";
        Object[] hospitalArgs = actor.isAdmin() ? new Object[]{start, end, start, end, start, end} : new Object[]{start, end, actor.userId(), start, end, actor.userId(), start, end, actor.userId()};
        Object[] projectArgs = actor.isAdmin() ? new Object[]{start, end} : new Object[]{start, end, actor.userId()};
        List<Map<String, Object>> hospitals = store.jdbc().queryForList("SELECT h.HospitalName AS hospitalName," +
                "(SELECT COUNT(*) FROM lin_lb_listdiagnosisproject o2 LEFT JOIN lin_order_extension e2 ON e2.order_id=o2.ListDiagnosisProjectId WHERE o2.InfoHospitalId=h.InfoHospitalId AND o2.CreateDt>=? AND o2.CreateDt<?" + hospitalOrderScope + ") AS orders," +
                "COALESCE((SELECT SUM(p.amount) FROM lin_wechat_payment p JOIN lin_lb_listdiagnosisproject o3 ON o3.ListDiagnosisProjectId=p.order_id LEFT JOIN lin_order_extension pe ON pe.order_id=p.order_id WHERE o3.InfoHospitalId=h.InfoHospitalId AND p.status=1 AND p.paid_at>=? AND p.paid_at<?" + hospitalCashScope + "),0)+" +
                "COALESCE((SELECT SUM(b.paid_amount) FROM lin_monthly_bill b WHERE b.hospital_id=h.InfoHospitalId AND b.status='SETTLED' AND b.settled_at>=? AND b.settled_at<?" + hospitalBillScope + "),0) AS revenue FROM lin_lb_infohospital h ORDER BY revenue DESC LIMIT 10", hospitalArgs);
        List<Map<String, Object>> projects = store.jdbc().queryForList("SELECT t.TestName AS testName,COUNT(*) AS quantity FROM lin_lb_diagnosisdetail d JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=d.InfoTestTypeId JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=d.ListDiagnosisProjectId LEFT JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId WHERE o.CreateDt>=? AND o.CreateDt<? AND d.IsCheckPass=0" + orderScope + " GROUP BY t.InfoTestTypeId,t.TestName ORDER BY quantity DESC LIMIT 10", projectArgs);
        return Map.of("orderCount", ((Number) totals.get("orderCount")).longValue(), "revenue", cash.add(monthly), "cashRevenue", cash,
                "monthlyRevenue", monthly, "arrears", money(totals.get("arrears")), "hospitalRanking", hospitals, "projectRanking", projects);
    }

    private Object[] summaryArgs(CurrentActor actor, LocalDate start, LocalDate end) {
        List<Object> args = new ArrayList<>();
        for (int i = 0; i < 3; i++) { args.add(start); args.add(end); if (!actor.isAdmin()) args.add(actor.userId()); }
        if (!actor.isAdmin()) args.add(actor.userId());
        return args.toArray();
    }

    public String exportOrders(String from, String to) {
        CurrentActor actor = requireStaff();
        LocalDate start = LocalDate.parse(from), end = LocalDate.parse(to);
        String scope = actor.isAdmin() ? "" : " AND EXISTS(SELECT 1 FROM lin_order_extension e JOIN lin_user_role r ON r.laboratory_id=e.laboratory_id WHERE e.order_id=o.ListDiagnosisProjectId AND r.user_id=? AND r.role_code='LAB_STAFF' AND r.is_active=1)";
        Object[] args = actor.isAdmin() ? new Object[]{start, end} : new Object[]{start, end, actor.userId()};
        var rows = store.jdbc().queryForList("SELECT o.ListDiagnosisProjectId AS orderId,o.UserNO AS orderNo,h.HospitalName AS hospitalName,o.TestFee AS amount,o.SampleStatus AS status,o.CreateDt AS createdAt FROM lin_lb_listdiagnosisproject o JOIN lin_lb_infohospital h ON h.InfoHospitalId=o.InfoHospitalId WHERE o.CreateDt>=? AND o.CreateDt<?" + scope + " ORDER BY o.ListDiagnosisProjectId DESC LIMIT 10000", args);
        StringBuilder csv = new StringBuilder("\uFEFF订单ID,订单编号,医院,金额（元）,状态,创建时间\r\n");
        for (var row : rows) csv.append(csv(row.get("orderId"))).append(',').append(csv(row.get("orderNo"))).append(',').append(csv(row.get("hospitalName"))).append(',')
                .append(csv(row.get("amount"))).append(',').append(csv(row.get("status"))).append(',').append(csv(row.get("createdAt"))).append("\r\n");
        return csv.toString();
    }

    private CurrentActor requireStaff() {
        CurrentActor actor = access.current();
        if (!actor.isAdmin() && !actor.hasRole(PlatformRole.LAB_STAFF)) throw new BusinessException(403, "仅管理员或实验人员可查看运营数据");
        return actor;
    }
    private static BigDecimal money(Object value) { return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()); }
    static String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.matches("^[=+@\t\r-].*")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
