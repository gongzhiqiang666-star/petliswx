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
        if (!end.isBefore(LocalDate.now())) throw new BusinessException("当前或未来账期尚未结束，不能生成月账单");
        var contractRows = store.jdbc().queryForList("SELECT DISTINCT s.contract_id FROM lin_order_pricing_snapshot s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.hospital_id=? AND s.laboratory_id=? AND s.settlement_type='monthly' AND o.SampleStatus IN (2,3) AND o.CreateDt>=? AND o.CreateDt<?", hospitalId, laboratoryId, start, end.plusDays(1));
        if (contractRows.size() != 1) throw new BusinessException(contractRows.isEmpty() ? "该账期没有可出账的订单定价快照" : "账期订单存在多个快照合同，请分合同出账");
        long contractId = ((Number) contractRows.get(0).get("contract_id")).longValue();
        Map<String, Object> contract = store.one("SELECT id,billing_cycle_days FROM lin_contract WHERE id=? FOR UPDATE", contractId);
        var existing = store.jdbc().queryForList("SELECT id,status,paid_amount,supplement_no FROM lin_monthly_bill WHERE contract_id=? AND period_start=? AND period_end=? ORDER BY supplement_no DESC,id DESC LIMIT 1", contractId, start, end);
        Long billId = existing.isEmpty() ? null : ((Number) existing.get(0).get("id")).longValue();
        boolean recalculate = billId != null && Set.of("PENDING_RECONCILIATION", "DISPUTED").contains(existing.get(0).get("status").toString());
        String inclusion = recalculate ? "(NOT EXISTS(SELECT 1 FROM lin_monthly_bill_item i WHERE i.order_id=s.order_id) OR EXISTS(SELECT 1 FROM lin_monthly_bill_item i WHERE i.order_id=s.order_id AND i.bill_id=?))"
                : "NOT EXISTS(SELECT 1 FROM lin_monthly_bill_item i WHERE i.order_id=s.order_id)";
        String sql = "SELECT s.order_id,s.original_amount,s.payable_amount FROM lin_order_pricing_snapshot s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.hospital_id=? AND s.laboratory_id=? AND s.contract_id=? AND s.settlement_type='monthly' AND o.SampleStatus IN (2,3) AND o.CreateDt>=? AND o.CreateDt<? AND " + inclusion + " ORDER BY s.order_id";
        List<Object> args = new ArrayList<>(List.of(hospitalId, laboratoryId, contractId, start, end.plusDays(1)));
        if (recalculate) args.add(billId);
        var orders = store.jdbc().queryForList(sql, args.toArray());
        if (billId != null && orders.isEmpty()) return get(billId);
        BigDecimal total = BigDecimal.ZERO, due = BigDecimal.ZERO;
        for (var order : orders) { total = total.add(money(order.get("original_amount"))); due = due.add(money(order.get("payable_amount"))); }
        BigDecimal discount = total.subtract(due);
        boolean supplement = billId != null && !recalculate;
        if (supplement) {
            int supplementNo = ((Number) existing.get(0).get("supplement_no")).intValue() + 1;
            billId = store.insert("INSERT INTO lin_monthly_bill(bill_no,contract_id,hospital_id,laboratory_id,period_start,period_end,total_amount,discount_amount,due_amount,status,due_date,bill_kind,supplement_no) VALUES(?,?,?,?,?,?,?,?,?,'PENDING_RECONCILIATION',?,'SUPPLEMENT',?)",
                    "B" + UUID.randomUUID().toString().replace("-", ""), contractId, hospitalId, laboratoryId, start, end, total, discount, due, end.plusDays(((Number) contract.get("billing_cycle_days")).intValue()), supplementNo);
        } else if (recalculate) {
            store.jdbc().update("DELETE FROM lin_monthly_bill_item WHERE bill_id=?", billId);
            store.jdbc().update("UPDATE lin_monthly_bill SET total_amount=?,discount_amount=?,due_amount=?,status='PENDING_RECONCILIATION',hospital_reconciled_by=NULL,hospital_reconciled_at=NULL,laboratory_reconciled_by=NULL,laboratory_reconciled_at=NULL,reconciled_at=NULL WHERE id=?", total, discount, due, billId);
        } else {
            billId = store.insert("INSERT INTO lin_monthly_bill(bill_no,contract_id,hospital_id,laboratory_id,period_start,period_end,total_amount,discount_amount,due_amount,status,due_date,bill_kind,supplement_no) VALUES(?,?,?,?,?,?,?,?,?,'PENDING_RECONCILIATION',?,'STANDARD',0)",
                    "B" + UUID.randomUUID().toString().replace("-", ""), contractId, hospitalId, laboratoryId, start, end, total, discount, due, end.plusDays(((Number) contract.get("billing_cycle_days")).intValue()));
        }
        for (var order : orders) insertItems(billId, order);
        audits.record(supplement ? "MONTHLY_BILL_SUPPLEMENT_GENERATED" : recalculate ? "MONTHLY_BILL_RECALCULATED" : "MONTHLY_BILL_GENERATED", "MONTHLY_BILL", billId, existing.isEmpty() ? null : existing.get(0), Map.of("month", month, "amount", due), supplement ? "独立补充账单，原结算保持不变" : recalculate ? "未对账账单锁定重算" : null);
        return get(billId);
    }

    private void insertItems(long billId, Map<String, Object> order) {
        int orderId = ((Number) order.get("order_id")).intValue();
        var details = store.jdbc().queryForList("SELECT d.DiagnosisDetailId AS detail_id,d.InfoTestTypeId AS test_type_id,t.TestName AS description,d.TestPrice AS unit_price FROM lin_lb_diagnosisdetail d JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=d.InfoTestTypeId WHERE d.ListDiagnosisProjectId=? AND d.IsCheckPass=0 ORDER BY d.DiagnosisDetailId", orderId);
        Map<Integer, Map<String, Object>> snapshot = snapshotItems(orderId);
        BigDecimal itemTotal = details.stream().map(row -> snapshotPrice(row, snapshot)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal couponDiscount = itemTotal.subtract(money(order.get("payable_amount")));
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < details.size(); i++) {
            var detail = details.get(i);
            BigDecimal unit = snapshotPrice(detail, snapshot);
            Object priceId = snapshot.isEmpty() ? null : snapshot.get(((Number) detail.get("test_type_id")).intValue()).get("contractItemPriceId");
            BigDecimal itemDiscount = itemTotal.signum() == 0 ? BigDecimal.ZERO.setScale(2) : i == details.size() - 1 ? couponDiscount.subtract(allocated) : couponDiscount.multiply(unit).divide(itemTotal, 2, RoundingMode.HALF_UP);
            allocated = allocated.add(itemDiscount);
            store.insert("INSERT INTO lin_monthly_bill_item(bill_id,order_id,detail_id,contract_item_price_id,description,quantity,unit_price,amount,discount_amount,final_amount) VALUES(?,?,?,?,?,1,?,?,?,?)",
                    billId, orderId, detail.get("detail_id"), priceId, detail.get("description"), unit, unit, itemDiscount, unit.subtract(itemDiscount));
        }
    }

    private Map<Integer, Map<String, Object>> snapshotItems(int orderId) {
        var rows = store.jdbc().queryForList("SELECT items_json FROM lin_order_pricing_snapshot WHERE order_id=?", orderId);
        if (rows.isEmpty()) return Map.of();
        try {
            Map<Integer, Map<String, Object>> result = new java.util.LinkedHashMap<>();
            var items = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rows.get(0).get("items_json").toString());
            for (var item : items) result.put(item.path("testTypeId").asInt(), new com.fasterxml.jackson.databind.ObjectMapper().convertValue(item, Map.class));
            return result;
        } catch (Exception error) { throw new BusinessException("订单定价快照损坏，无法出账"); }
    }
    private BigDecimal snapshotPrice(Map<String, Object> detail, Map<Integer, Map<String, Object>> snapshot) {
        if (snapshot.isEmpty()) return money(detail.get("unit_price"));
        var item = snapshot.get(((Number) detail.get("test_type_id")).intValue());
        if (item == null) throw new BusinessException("订单项目与定价快照不一致");
        return money(item.get("unitPrice"));
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
        LocalDate dueDate = bill.get("due_date") instanceof LocalDate date ? date : LocalDate.parse(bill.get("due_date").toString());
        BigDecimal outstanding = money(bill.get("due_amount")).subtract(money(bill.get("paid_amount")));
        if (!dueDate.isBefore(LocalDate.now()) || outstanding.signum() <= 0) throw new BusinessException(409, "账单尚未逾期或已结清，无需提醒");
        Integer orderId = store.jdbc().queryForObject("SELECT MIN(order_id) FROM lin_monthly_bill_item WHERE bill_id=?", Integer.class, id);
        if (orderId == null) throw new BusinessException(409, "空账单没有可提醒的订单明细");
        store.jdbc().update("UPDATE lin_monthly_bill SET status='OVERDUE',last_overdue_reminded_at=NOW() WHERE id=?", id);
        notifications.emitForOrder(orderId, null,
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
