package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonthlyBillService {
    private final FeatureStore store;
    private final AccessService access;
    private final AuditService audits;
    private final BusinessNotifications notifications;

    @Transactional
    public Map<String, Object> generate(int hospitalId, int laboratoryId, String month) {
        requireLaboratory(laboratoryId);
        YearMonth period = YearMonth.parse(month);
        LocalDate start = period.atDay(1), end = period.atEndOfMonth();
        Map<String, Object> contract = store.one("SELECT id,billing_cycle_days FROM lin_contract WHERE hospital_id=? AND laboratory_id=? AND settlement_type='MONTHLY' AND status='ACTIVE' FOR UPDATE", hospitalId, laboratoryId);
        long contractId = ((Number) contract.get("id")).longValue();
        var existing = store.jdbc().queryForList("SELECT id FROM lin_monthly_bill WHERE contract_id=? AND period_start=? AND period_end=?", contractId, start, end);
        if (!existing.isEmpty()) return get(((Number) existing.get(0).get("id")).longValue());
        var orders = store.jdbc().queryForList("SELECT o.ListDiagnosisProjectId AS order_id,e.original_amount,e.payable_amount FROM lin_order_extension e JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=e.order_id WHERE o.InfoHospitalId=? AND e.laboratory_id=? AND e.settlement_mode='monthly' AND o.SampleStatus IN (2,3) AND o.CreateDt>=? AND o.CreateDt<? AND NOT EXISTS(SELECT 1 FROM lin_monthly_bill_item i WHERE i.order_id=o.ListDiagnosisProjectId) ORDER BY o.ListDiagnosisProjectId", hospitalId, laboratoryId, start, end.plusDays(1));
        BigDecimal total = BigDecimal.ZERO, discount = BigDecimal.ZERO, due = BigDecimal.ZERO;
        for (var order : orders) {
            total = total.add(money(order.get("original_amount")));
            due = due.add(money(order.get("payable_amount")));
        }
        discount = total.subtract(due);
        long billId = store.insert("INSERT INTO lin_monthly_bill(bill_no,contract_id,hospital_id,laboratory_id,period_start,period_end,total_amount,discount_amount,due_amount,status,due_date) VALUES(?,?,?,?,?,?,?,?,?,'PENDING_RECONCILIATION',?)",
                "B" + UUID.randomUUID().toString().replace("-", ""), contractId, hospitalId, laboratoryId, start, end, total, discount, due, end.plusDays(((Number) contract.get("billing_cycle_days")).intValue()));
        for (var order : orders) insertItems(billId, order);
        audits.record("MONTHLY_BILL_GENERATED", "MONTHLY_BILL", billId, null, Map.of("month", month, "dueAmount", due), null);
        return get(billId);
    }

    private void insertItems(long billId, Map<String, Object> order) {
        int orderId = ((Number) order.get("order_id")).intValue();
        var details = store.jdbc().queryForList("SELECT d.DiagnosisDetailId AS detail_id,t.TestName AS description,d.TestPrice AS unit_price,p.id AS contract_item_price_id FROM lin_lb_diagnosisdetail d JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=d.InfoTestTypeId LEFT JOIN lin_contract_item_price p ON p.id=(SELECT cp.id FROM lin_contract_item_price cp JOIN lin_contract c ON c.id=cp.contract_id JOIN lin_order_extension e ON e.laboratory_id=c.laboratory_id JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=e.order_id AND o.InfoHospitalId=c.hospital_id WHERE e.order_id=d.ListDiagnosisProjectId AND cp.info_test_type_id=d.InfoTestTypeId AND cp.effective_from<=DATE(d.CreateDt) AND (cp.effective_to IS NULL OR cp.effective_to>=DATE(d.CreateDt)) ORDER BY cp.id DESC LIMIT 1) WHERE d.ListDiagnosisProjectId=? AND d.IsCheckPass=0 ORDER BY d.DiagnosisDetailId", orderId);
        BigDecimal itemTotal = details.stream().map(row -> money(row.get("unit_price"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal couponDiscount = itemTotal.subtract(money(order.get("payable_amount")));
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < details.size(); i++) {
            var detail = details.get(i);
            BigDecimal unit = money(detail.get("unit_price"));
            BigDecimal itemDiscount = itemTotal.signum() == 0 ? BigDecimal.ZERO.setScale(2) : i == details.size() - 1 ? couponDiscount.subtract(allocated) : couponDiscount.multiply(unit).divide(itemTotal, 2, RoundingMode.HALF_UP);
            allocated = allocated.add(itemDiscount);
            store.insert("INSERT INTO lin_monthly_bill_item(bill_id,order_id,detail_id,contract_item_price_id,description,quantity,unit_price,amount,discount_amount,final_amount) VALUES(?,?,?,?,?,1,?,?,?,?)",
                    billId, orderId, detail.get("detail_id"), detail.get("contract_item_price_id"), detail.get("description"), unit, unit, itemDiscount, unit.subtract(itemDiscount));
        }
    }

    public Map<String, Object> get(long id) {
        var rows = store.jdbc().queryForList("SELECT b.*,GREATEST(DATEDIFF(CURDATE(),b.due_date),0) AS overdueDays,b.due_amount-b.paid_amount AS outstandingAmount FROM lin_monthly_bill b WHERE b.id=?", id);
        if (rows.isEmpty()) throw new BusinessException(404, "账单不存在");
        Map<String, Object> bill = rows.get(0);
        authorizeRead(bill);
        return bill;
    }

    public Map<String, Object> items(long id, int page, int size) {
        get(id);
        return store.page("SELECT COUNT(*) FROM lin_monthly_bill_item WHERE bill_id=?", "SELECT i.*,p.UserNO AS orderNo FROM lin_monthly_bill_item i JOIN lin_lb_listdiagnosisproject p ON p.ListDiagnosisProjectId=i.order_id WHERE i.bill_id=? ORDER BY i.id", page, size, id);
    }

    public Map<String, Object> list(String month, String status, boolean arrears, int page, int size) {
        CurrentActor actor = access.current();
        if (actor.hasRole(PlatformRole.DOCTOR) && !actor.hasRole(PlatformRole.HOSPITAL_ADMIN) && !actor.isAdmin()) throw new BusinessException(403, "仅医院管理员可查看月结账单");
        StringBuilder where = new StringBuilder(" FROM lin_monthly_bill b WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!actor.isAdmin()) {
            if (actor.hasRole(PlatformRole.HOSPITAL_ADMIN)) {
                where.append(" AND b.hospital_id IN (SELECT hospital_id FROM lin_user_role WHERE user_id=? AND role_code='HOSPITAL_ADMIN' AND is_active=1)"); args.add(actor.userId());
            } else if (actor.hasRole(PlatformRole.LAB_STAFF)) {
                where.append(" AND b.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)"); args.add(actor.userId());
            } else throw new BusinessException(403, "无权查看账单");
        }
        if (month != null && !month.isBlank()) { where.append(" AND b.period_start=?"); args.add(YearMonth.parse(month).atDay(1)); }
        if (status != null && !status.isBlank()) { where.append(" AND b.status=?"); args.add(status); }
        if (arrears) where.append(" AND b.status<>'SETTLED' AND b.due_amount>b.paid_amount");
        return store.page("SELECT COUNT(*)" + where, "SELECT b.*,GREATEST(DATEDIFF(CURDATE(),b.due_date),0) AS overdueDays,b.due_amount-b.paid_amount AS outstandingAmount" + where + " ORDER BY b.id DESC", page, size, args.toArray());
    }

    @Transactional
    public Map<String, Object> reconcile(long id, String action, String reason) {
        Map<String, Object> bill = lock(id);
        authorizeReconcile(bill);
        String status = bill.get("status").toString();
        if (!Set.of("PENDING_RECONCILIATION", "DISPUTED", "RECONCILED", "OVERDUE").contains(status)) throw new BusinessException(409, "当前账单不可对账");
        CurrentActor actor = access.current();
        if ("DISPUTE".equals(action)) {
            if (reason == null || reason.isBlank()) throw new BusinessException("请填写账单异议原因");
            store.jdbc().update("UPDATE lin_monthly_bill SET status='DISPUTED',hospital_reconciled_by=NULL,hospital_reconciled_at=NULL,laboratory_reconciled_by=NULL,laboratory_reconciled_at=NULL,reconciled_at=NULL WHERE id=?", id);
        } else if ("CONFIRM".equals(action)) {
            if (actor.hasRole(PlatformRole.HOSPITAL_ADMIN) && !actor.isAdmin()) store.jdbc().update("UPDATE lin_monthly_bill SET hospital_reconciled_by=?,hospital_reconciled_at=NOW() WHERE id=?", actor.userId(), id);
            else store.jdbc().update("UPDATE lin_monthly_bill SET laboratory_reconciled_by=?,laboratory_reconciled_at=NOW() WHERE id=?", actor.userId(), id);
            store.jdbc().update("UPDATE lin_monthly_bill SET status=IF(hospital_reconciled_at IS NOT NULL AND laboratory_reconciled_at IS NOT NULL,'RECONCILED','PENDING_RECONCILIATION'),reconciled_at=IF(hospital_reconciled_at IS NOT NULL AND laboratory_reconciled_at IS NOT NULL,NOW(),NULL) WHERE id=?", id);
        } else throw new BusinessException("对账动作仅支持 CONFIRM 或 DISPUTE");
        store.jdbc().queryForObject("SELECT status FROM lin_monthly_bill WHERE id=?", String.class, id);
        audits.record("MONTHLY_BILL_" + action, "MONTHLY_BILL", id, bill, Map.of("action", action), reason);
        return get(id);
    }

    @Transactional
    public Map<String, Object> settle(long id, String reference) {
        Map<String, Object> bill = lock(id);
        requireLaboratory(((Number) bill.get("laboratory_id")).intValue());
        String status = store.jdbc().queryForObject("SELECT status FROM lin_monthly_bill WHERE id=?", String.class, id);
        if ("SETTLED".equals(status)) {
            if (!java.util.Objects.equals(reference, bill.get("settlement_reference"))) throw new BusinessException(409, "账单已使用其他结算编号结算");
            return get(id);
        }
        if (reference == null || !reference.matches("mock-[A-Za-z0-9_-]{4,100}")) throw new BusinessException("仅允许有效的 mock 结算编号");
        if (!"RECONCILED".equals(status) && !("OVERDUE".equals(status) && bill.get("reconciled_at") != null)) throw new BusinessException(409, "双方对账确认后才能结算");
        store.jdbc().update("UPDATE lin_monthly_bill SET status='SETTLED',paid_amount=due_amount,settled_at=NOW(),settlement_reference=? WHERE id=?", reference, id);
        store.jdbc().update("UPDATE lin_lb_diagnosisdetail d JOIN lin_monthly_bill_item i ON i.detail_id=d.DiagnosisDetailId SET d.PayStatus2=1 WHERE i.bill_id=?", id);
        audits.record("MONTHLY_BILL_MOCK_SETTLED", "MONTHLY_BILL", id, bill, Map.of("reference", reference), null);
        return get(id);
    }

    @Transactional
    public void remindOverdue(long id) {
        Map<String, Object> bill = lock(id);
        requireLaboratory(((Number) bill.get("laboratory_id")).intValue());
        if ("SETTLED".equals(bill.get("status")) || bill.get("last_overdue_reminded_at") != null) return;
        store.jdbc().update("UPDATE lin_monthly_bill SET status=IF(due_date<CURDATE(),'OVERDUE',status),last_overdue_reminded_at=NOW() WHERE id=?", id);
        notifications.emitForOrder(((Number) store.jdbc().queryForObject("SELECT MIN(order_id) FROM lin_monthly_bill_item WHERE bill_id=?", Integer.class, id)).intValue(), null,
                "order", "bill:" + id + ":overdue", "医院月结账单待结清", "账单 #" + id + " 尚未结清，请尽快对账结算。", "pages/mine/mine");
    }

    private Map<String, Object> lock(long id) {
        Map<String, Object> bill = store.one("SELECT b.* FROM lin_monthly_bill b WHERE b.id=? FOR UPDATE", id);
        authorizeRead(bill);
        return bill;
    }
    private void authorizeRead(Map<String, Object> bill) {
        CurrentActor actor = access.current();
        int hospital = ((Number) bill.get("hospital_id")).intValue(), laboratory = ((Number) bill.get("laboratory_id")).intValue();
        if (actor.isAdmin() || actor.hasRole(PlatformRole.HOSPITAL_ADMIN) && actor.canAccessHospital(hospital) || actor.hasRole(PlatformRole.LAB_STAFF) && actor.canAccessLaboratory(laboratory)) return;
        throw new BusinessException(403, "无权访问该账单");
    }
    private void authorizeReconcile(Map<String, Object> bill) {
        CurrentActor actor = access.current();
        if (actor.isAdmin() || actor.hasRole(PlatformRole.HOSPITAL_ADMIN) || actor.hasRole(PlatformRole.LAB_STAFF)) return;
        throw new BusinessException(403, "无权对账");
    }
    private void requireLaboratory(int laboratoryId) {
        CurrentActor actor = access.current();
        if (!actor.isAdmin() && !actor.hasRole(PlatformRole.LAB_STAFF)) throw new BusinessException(403, "仅实验室可执行该操作");
        access.requireLaboratory(laboratoryId);
    }
    private static BigDecimal money(Object value) { return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP); }
}
