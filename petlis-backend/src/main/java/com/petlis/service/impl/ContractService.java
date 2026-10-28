package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContractService {
    private final FeatureStore store;
    private final AccessService access;
    private final AuditService audits;

    public record ItemPrice(long id, BigDecimal listPrice, BigDecimal finalPrice, BigDecimal discountRate) { }
    public record Resolution(long contractId, boolean contracted, String settlementType, Map<Integer, ItemPrice> items) { }
    public record Command(String contractNo, Integer hospitalId, Integer laboratoryId, String settlementType,
                          Integer billingCycleDays, BigDecimal creditAmount, LocalDate effectiveFrom, LocalDate effectiveTo) { }
    public record PriceCommand(Integer infoTestTypeId, BigDecimal listPrice, BigDecimal discountRate,
                               BigDecimal agreedPrice, LocalDate effectiveFrom, LocalDate effectiveTo, String status) { }

    public Map<String, Object> list(int page, int size) {
        access.requireAdmin();
        return store.page("SELECT COUNT(*) FROM lin_contract", "SELECT * FROM lin_contract ORDER BY id DESC", page, size);
    }

    public Map<String, Object> get(long id) {
        access.requireAdmin();
        Map<String, Object> result = new LinkedHashMap<>(store.one("SELECT * FROM lin_contract WHERE id=?", id));
        result.put("items", store.jdbc().queryForList("SELECT * FROM lin_contract_item_price WHERE contract_id=? ORDER BY info_test_type_id,id", id));
        return result;
    }

    @Transactional
    public long create(Command command) {
        access.requireAdmin();
        validate(command);
        validateNoOverlap(0, command);
        long id = store.insert("INSERT INTO lin_contract(contract_no,hospital_id,laboratory_id,settlement_type,billing_cycle_days,credit_amount,effective_from,effective_to,status,created_by) VALUES(?,?,?,?,?,?,?,?,'ACTIVE',?)",
                command.contractNo().trim(), command.hospitalId(), command.laboratoryId(), command.settlementType(), command.billingCycleDays(),
                command.creditAmount(), command.effectiveFrom(), command.effectiveTo(), access.current().userId());
        audits.record("CONTRACT_CREATED", "CONTRACT", id, null, command, null);
        return id;
    }

    @Transactional
    public void update(long id, int version, Command command) {
        access.requireAdmin();
        validate(command);
        Map<String, Object> before = store.one("SELECT * FROM lin_contract WHERE id=? FOR UPDATE", id);
        if (((Number) before.get("version")).intValue() != version) throw new BusinessException(409, "签约配置已变更，请刷新后重试");
        if ("ACTIVE".equals(before.get("status"))) validateNoOverlap(id, command);
        if (command.creditAmount().compareTo(new BigDecimal(before.get("credit_amount").toString())) < 0) {
            BigDecimal used = usedCredit(id, -1);
            if (command.creditAmount().compareTo(used) < 0) throw new BusinessException("信用额度不得低于当前未结占用");
        }
        int changed = store.jdbc().update("UPDATE lin_contract SET contract_no=?,hospital_id=?,laboratory_id=?,settlement_type=?,billing_cycle_days=?,credit_amount=?,effective_from=?,effective_to=?,version=version+1,updated_by=? WHERE id=? AND version=?",
                command.contractNo().trim(), command.hospitalId(), command.laboratoryId(), command.settlementType(), command.billingCycleDays(), command.creditAmount(),
                command.effectiveFrom(), command.effectiveTo(), access.current().userId(), id, version);
        if (changed != 1) throw new BusinessException(409, "签约配置已变更，请刷新后重试");
        audits.record("CONTRACT_UPDATED", "CONTRACT", id, before, command, null);
    }

    @Transactional
    public void setStatus(long id, boolean active, String reason) {
        access.requireAdmin();
        Map<String, Object> before = store.one("SELECT id,status FROM lin_contract WHERE id=? FOR UPDATE", id);
        store.jdbc().update("UPDATE lin_contract SET status='" + (active ? "ACTIVE" : "INACTIVE") + "',version=version+1,updated_by=? WHERE id=?", access.current().userId(), id);
        audits.record(active ? "CONTRACT_ACTIVATED" : "CONTRACT_DEACTIVATED", "CONTRACT", id, before, Map.of("active", active), reason);
    }

    private void validateNoOverlap(long excludedId, Command command) {
        LocalDate to = command.effectiveTo() == null ? LocalDate.of(9999, 12, 31) : command.effectiveTo();
        LocalDate from = command.effectiveFrom();
        if (Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_contract c WHERE c.hospital_id=? AND c.laboratory_id=? AND c.status='ACTIVE' AND c.effective_from<=COALESCE(?,?) AND (c.effective_to IS NULL OR c.effective_to>=COALESCE(?,?)) AND c.id<>?)", Boolean.class,
                command.hospitalId(), command.laboratoryId(), command.effectiveTo(), to, command.effectiveFrom(), from, excludedId))) {
            throw new BusinessException(409, "同医院和实验室已存在效期重叠的有效签约");
        }
    }

    @Transactional
    public long savePrice(long contractId, PriceCommand command) {
        access.requireAdmin();
        store.one("SELECT id,laboratory_id FROM lin_contract WHERE id=?", contractId);
        if (command.infoTestTypeId() == null || command.listPrice() == null || command.listPrice().signum() < 0 || command.listPrice().scale() > 2
                || command.agreedPrice() != null && (command.agreedPrice().signum() < 0 || command.agreedPrice().scale() > 2)
                || command.discountRate() != null && (command.discountRate().signum() <= 0 || command.discountRate().compareTo(BigDecimal.TEN) > 0)
                || command.effectiveFrom() == null || command.effectiveTo() != null && !command.effectiveTo().isAfter(command.effectiveFrom())
                || !Set.of("ACTIVE", "INACTIVE").contains(command.status())) throw new BusinessException("项目协议价无效");
        Map<String, Object> contract = store.one("SELECT laboratory_id FROM lin_contract WHERE id=?", contractId);
        store.one("SELECT lt.test_type_id FROM lin_lab_test lt JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=lt.test_type_id WHERE lt.laboratory_id=? AND lt.test_type_id=? AND lt.active=1 AND t.IsActive=1", contract.get("laboratory_id"), command.infoTestTypeId());
        long id = store.insert("INSERT INTO lin_contract_item_price(contract_id,laboratory_id,info_test_type_id,list_price,discount_rate,agreed_price,effective_from,effective_to,status,created_by) VALUES(?,?,?,?,?,?,?,?,?,?)",
                contractId, contract.get("laboratory_id"), command.infoTestTypeId(), command.listPrice(), command.discountRate(), command.agreedPrice(),
                command.effectiveFrom(), command.effectiveTo(), command.status(), access.current().userId());
        audits.record("CONTRACT_PRICE_CREATED", "CONTRACT_ITEM_PRICE", id, null, command, null);
        return id;
    }

    public Resolution priceForOrder(int hospitalId, int laboratoryId, List<Integer> testTypeIds) {
        var contracts = store.jdbc().queryForList("SELECT c.id,c.settlement_type FROM lin_contract c WHERE c.hospital_id=? AND c.laboratory_id=? AND c.status='ACTIVE' AND c.effective_from<=CURDATE() AND (c.effective_to IS NULL OR c.effective_to>=CURDATE()) ORDER BY c.id DESC LIMIT 2", hospitalId, laboratoryId);
        if (contracts.isEmpty()) {
            if (Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_contract c WHERE c.hospital_id=? AND c.laboratory_id=? AND c.status='INACTIVE')", Boolean.class, hospitalId, laboratoryId))) {
                throw new BusinessException(409, "医院签约已停用，不能创建新订单");
            }
            return new Resolution(0, false, "CASH", Map.of());
        }
        if (contracts.size() != 1) throw new BusinessException("医院存在重叠的有效签约，请联系管理员");
        long contractId = ((Number) contracts.get(0).get("id")).longValue();
        Map<Integer, ItemPrice> result = new LinkedHashMap<>();
        for (int testTypeId : testTypeIds) {
            store.one("SELECT lt.test_type_id FROM lin_lab_test lt JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=lt.test_type_id WHERE lt.laboratory_id=? AND lt.test_type_id=? AND lt.active=1 AND t.IsActive=1", laboratoryId, testTypeId);
            var rows = store.jdbc().queryForList("SELECT id,list_price,discount_rate,agreed_price FROM lin_contract_item_price WHERE contract_id=? AND laboratory_id=? AND info_test_type_id=? AND status='ACTIVE' AND effective_from<=CURDATE() AND (effective_to IS NULL OR effective_to>=CURDATE()) ORDER BY id DESC LIMIT 2", contractId, laboratoryId, testTypeId);
            if (rows.size() != 1) throw new BusinessException(rows.isEmpty() ? "签约缺少检测项目协议价" : "检测项目存在重叠协议价");
            Map<String, Object> row = rows.get(0);
            BigDecimal list = decimal(row.get("list_price"));
            BigDecimal rate = row.get("discount_rate") == null ? null : new BigDecimal(row.get("discount_rate").toString());
            BigDecimal price = row.get("agreed_price") != null ? decimal(row.get("agreed_price"))
                    : rate == null ? list : list.multiply(rate).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            result.put(testTypeId, new ItemPrice(((Number) row.get("id")).longValue(), list, price.setScale(2, RoundingMode.HALF_UP), rate));
        }
        return new Resolution(contractId, true, contracts.get(0).get("settlement_type").toString(), result);
    }

    @Transactional
    public void reserveCredit(int orderId, BigDecimal additionalAmount) {
        Map<String, Object> contract = store.one("SELECT c.id,c.credit_amount FROM lin_order_pricing_snapshot s JOIN lin_contract c ON c.id=s.contract_id WHERE s.order_id=? AND s.settlement_type='MONTHLY' FOR UPDATE", orderId);
        long contractId = ((Number) contract.get("id")).longValue();
        BigDecimal used = usedCredit(contractId, orderId);
        if (used.add(additionalAmount).compareTo(decimal(contract.get("credit_amount"))) > 0) throw new BusinessException("医院月结信用额度不足");
    }

    private BigDecimal usedCredit(long contractId, int excludedOrderId) {
        BigDecimal value = store.jdbc().queryForObject("SELECT COALESCE(SUM(s.payable_amount),0) FROM lin_order_pricing_snapshot s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.contract_id=? AND s.settlement_type='monthly' AND s.order_id<>? AND o.SampleStatus IN (2,3) AND NOT EXISTS(SELECT 1 FROM lin_monthly_bill_item i JOIN lin_monthly_bill b ON b.id=i.bill_id WHERE i.order_id=s.order_id AND b.status='SETTLED')", BigDecimal.class, contractId, excludedOrderId);
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validate(Command c) {
        if (c == null || c.contractNo() == null || c.contractNo().isBlank() || c.contractNo().length() > 64 || c.hospitalId() == null || c.laboratoryId() == null
                || !Set.of("CASH", "MONTHLY").contains(c.settlementType()) || c.billingCycleDays() == null || c.billingCycleDays() < 1 || c.billingCycleDays() > 365
                || c.creditAmount() == null || c.creditAmount().signum() < 0 || c.creditAmount().scale() > 2 || c.effectiveFrom() == null
                || c.effectiveTo() != null && !c.effectiveTo().isAfter(c.effectiveFrom())) throw new BusinessException("签约配置无效");
    }

    private static BigDecimal decimal(Object value) { return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP); }
}
