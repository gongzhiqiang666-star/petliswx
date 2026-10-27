package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.PlatformRole;
import com.petlis.config.WeChatPayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final FeatureStore store;
    private final AccessService access;
    private final AuditService audits;
    private final WeChatPayConfig config;

    @Transactional
    public Map<String, Object> refund(int orderId, String reason, String requestKey) {
        if (reason == null || reason.isBlank() || reason.length() > 500 || requestKey == null || !requestKey.matches("[A-Za-z0-9_-]{8,100}")) throw new BusinessException("退款原因或请求编号无效");
        Map<String, Object> ownership = store.one("SELECT o.ListDiagnosisProjectId AS order_id,o.InfoHospitalId AS hospital_id,e.laboratory_id,1 AS ownership FROM lin_lb_listdiagnosisproject o LEFT JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId WHERE o.ListDiagnosisProjectId=?", orderId);
        authorize(ownership);
        var existing = store.jdbc().queryForList("SELECT id,resource_id,action,after_json FROM lin_audit_record WHERE resource_type='MOCK_REFUND' AND resource_id=? ORDER BY id DESC", String.valueOf(orderId));
        if (!existing.isEmpty()) return Map.of("orderId", orderId, "status", "REFUNDED", "mock", true, "auditId", ((Number) existing.get(0).get("id")).longValue());
        if (!Boolean.TRUE.equals(config.getMockEnabled())) throw new BusinessException("真实支付退款已禁用，仅允许 mock 退款");
        Map<String, Object> order = store.one("SELECT o.ListDiagnosisProjectId AS order_id,o.InfoHospitalId AS hospital_id,o.SampleStatus,e.laboratory_id,e.settlement_mode,p.payment_no,p.amount,p.status FROM lin_lb_listdiagnosisproject o JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId JOIN lin_wechat_payment p ON p.order_id=o.ListDiagnosisProjectId AND p.status=1 WHERE o.ListDiagnosisProjectId=? FOR UPDATE", orderId);
        existing = store.jdbc().queryForList("SELECT id,resource_id,action,after_json FROM lin_audit_record WHERE resource_type='MOCK_REFUND' AND resource_id=? ORDER BY id DESC", String.valueOf(orderId));
        if (!existing.isEmpty()) return Map.of("orderId", orderId, "status", "REFUNDED", "mock", true, "auditId", ((Number) existing.get(0).get("id")).longValue());
        authorize(order);
        if (!"cash".equals(order.get("settlement_mode")) || !Set.of(2, 3).contains(((Number) order.get("SampleStatus")).intValue())) throw new BusinessException("仅已支付的现结待报告或已完成订单可退款");
        if (new BigDecimal(order.get("amount").toString()).signum() <= 0) throw new BusinessException("退款金额必须大于零");
        if (Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_invoice_order io JOIN lin_invoice_application a ON a.id=io.application_id WHERE io.order_id=? AND a.status IN ('pending','issued'))", Boolean.class, orderId))) {
            throw new BusinessException("订单已申请或开具发票，请先按财务流程处理发票，禁止直接退款");
        }
        store.jdbc().update("UPDATE lin_wechat_payment SET status=3,trade_state='REFUNDED',update_at=NOW() WHERE payment_no=? AND status=1", order.get("payment_no"));
        store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET IsPayMoney=0 WHERE ListDiagnosisProjectId=? AND IsPayMoney=1", orderId);
        audits.record("MOCK_REFUND_COMPLETED", "MOCK_REFUND", orderId, order,
                Map.of("orderId", orderId, "amount", order.get("amount"), "requestKey", requestKey, "mock", true), reason);
        return Map.of("orderId", orderId, "status", "REFUNDED", "amount", order.get("amount"), "mock", true);
    }

    private void authorize(Map<String, Object> order) {
        CurrentActor actor = access.current();
        int hospital = ((Number) order.get("hospital_id")).intValue(), laboratory = ((Number) order.get("laboratory_id")).intValue();
        if (actor.isAdmin() || actor.hasRole(PlatformRole.HOSPITAL_ADMIN) && actor.canAccessHospital(hospital)
                || actor.hasRole(PlatformRole.LAB_STAFF) && actor.canAccessLaboratory(laboratory)) return;
        throw new BusinessException(403, "无权退款此订单");
    }
}
