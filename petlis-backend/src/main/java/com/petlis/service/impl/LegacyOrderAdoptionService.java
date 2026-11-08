package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/** Explicitly adopts an unreviewed legacy order; never reprices or copies the primary order. */
@Service
@RequiredArgsConstructor
public class LegacyOrderAdoptionService {
    private final FeatureStore store;
    private final AccessService access;

    @Transactional
    public void submitPrepared(int orderId, int doctorId, int laboratoryId, BigDecimal confirmedAmount) {
        access.requireAdmin();
        if (orderId <= 0 || doctorId <= 0 || laboratoryId <= 0) throw new BusinessException("请选择订单、医生和实验室");
        var order = store.one("SELECT SampleStatus,CreateSampleId,InfoHospitalId,TestFee,IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? FOR UPDATE", orderId);
        var existing = store.jdbc().queryForList("SELECT user_id,laboratory_id FROM lin_order_extension WHERE order_id=?", orderId);
        if (!existing.isEmpty()) {
            if (((Number)existing.get(0).get("user_id")).intValue() != doctorId || ((Number)existing.get(0).get("laboratory_id")).intValue() != laboratoryId)
                throw new BusinessException(409,"已接入单据不能变更医生或实验室");
            return;
        }
        if (((Number)order.get("SampleStatus")).intValue() != 0) throw new BusinessException(409,"仅待审核准备单可绑定医生送审");
        if (order.get("CreateSampleId") != null && ((Number)order.get("CreateSampleId")).intValue() != 0 && ((Number)order.get("CreateSampleId")).intValue() != doctorId)
            throw new BusinessException(409,"已有医生归属不能重新绑定");
        store.one("SELECT id FROM lin_user WHERE id=? AND hospital_id=? AND status='normal'", doctorId, order.get("InfoHospitalId"));
        if (confirmedAmount == null || confirmedAmount.signum() < 0 || confirmedAmount.scale() > 2) throw new BusinessException("请核对并确认原明细合计");
        var detailTotal = store.one("SELECT SUM(TestPrice) AS amount FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND IsActive=1 AND IsCheckPass=0 FOR UPDATE", orderId);
        if (detailTotal.get("amount") == null || confirmedAmount.compareTo(new BigDecimal(detailTotal.get("amount").toString())) != 0)
            throw new BusinessException(409,"明细金额已变化，请刷新并重新确认");
        BigDecimal original = new BigDecimal(order.get("TestFee").toString());
        boolean unowned = order.get("CreateSampleId") == null || ((Number)order.get("CreateSampleId")).intValue() == 0;
        if (original.compareTo(confirmedAmount) != 0 && !(unowned && original.signum() == 0)) throw new BusinessException(409,"原单金额存在差异，不能自动覆盖");
        if (Integer.valueOf(1).equals(order.get("IsPayMoney"))) throw new BusinessException(409,"已支付单据不能作为准备单送审");
        store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET CreateSampleId=?,TestFee=? WHERE ListDiagnosisProjectId=?", doctorId, confirmedAmount, orderId);
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,before_json,after_json) VALUES(?,'LEGACY_PREPARED_SUBMITTED','ORDER',?,?,?)", access.current().userId(), String.valueOf(orderId), store.encode(java.util.Map.of("storedAmount", original)), store.encode(java.util.Map.of("doctorId",doctorId,"confirmedAmount",confirmedAmount)));

        adopt(orderId, laboratoryId);
    }

    @Transactional
    public void adopt(int orderId, int laboratoryId) {
        access.requireAdmin();
        if (orderId <= 0 || laboratoryId <= 0) throw new BusinessException("订单或实验室编号无效");
        var order = store.one("SELECT o.UserNO,o.InfoHospitalId,o.SampleStatus,o.CreateSampleId,o.TestFee,o.IsPayMoney,h.PayType FROM lin_lb_listdiagnosisproject o JOIN lin_lb_infohospital h ON h.InfoHospitalId=o.InfoHospitalId WHERE o.ListDiagnosisProjectId=? FOR UPDATE", orderId);
        var existing = store.jdbc().queryForList("SELECT laboratory_id FROM lin_order_extension WHERE order_id=?", orderId);
        if (!existing.isEmpty()) {
            if (((Number)existing.get(0).get("laboratory_id")).intValue() != laboratoryId) throw new BusinessException(409,"订单已接入其他实验室");
            return;
        }
        if (((Number)order.get("SampleStatus")).intValue() != 0 || order.get("CreateSampleId") == null || Integer.valueOf(1).equals(order.get("IsPayMoney")))
            throw new BusinessException(409,"仅可接入具有医生归属、未支付的待审核旧单");
        String barcode = order.get("UserNO") == null ? "" : order.get("UserNO").toString().trim();
        if (!barcode.matches("[A-Za-z0-9][A-Za-z0-9_./ +%-]{1,62}[A-Za-z0-9]")) throw new BusinessException("请先补全可扫描的原送检条码");
        var duplicate = store.jdbc().queryForList("SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE UserNO=? AND ListDiagnosisProjectId<>? FOR UPDATE", barcode, orderId);
        if (!duplicate.isEmpty()) throw new BusinessException(409,"条码已被其他送检单使用，请先核实原单");
        store.one("SELECT id FROM lin_laboratory WHERE id=? AND active=1", laboratoryId);
        store.one("SELECT id FROM lin_user WHERE id=? AND hospital_id=? AND status='normal'", order.get("CreateSampleId"), order.get("InfoHospitalId"));
        boolean downstream = Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_shipment WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_report_workflow WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND IsActive=1 AND (IsSendMail=1 OR DiagnosisStatus=2))", Boolean.class, orderId, orderId, orderId));
        if (downstream) throw new BusinessException(409,"旧单已有物流或报告记录，不能作为新待审单接入");
        var details = store.jdbc().queryForList("SELECT d.InfoTestTypeId,d.TestPrice FROM lin_lb_diagnosisdetail d WHERE d.ListDiagnosisProjectId=? AND d.IsActive=1 AND d.IsCheckPass=0 FOR UPDATE", orderId);
        if (details.isEmpty()) throw new BusinessException("旧单没有有效检测项目");
        BigDecimal amount = new BigDecimal(order.get("TestFee").toString());
        BigDecimal total = BigDecimal.ZERO;
        for (var detail : details) {
            if (detail.get("TestPrice") == null) throw new BusinessException("旧单项目金额缺失，请核实原单");
            BigDecimal price = new BigDecimal(detail.get("TestPrice").toString());
            if (price.signum() < 0) throw new BusinessException("旧单项目金额无效");
            total = total.add(price);
            store.one("SELECT lt.test_type_id FROM lin_lab_test lt JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=lt.test_type_id WHERE lt.laboratory_id=? AND lt.test_type_id=? AND lt.active=1 AND t.IsActive=1", laboratoryId, detail.get("InfoTestTypeId"));
        }
        if (amount.signum() < 0 || amount.compareTo(total) != 0) throw new BusinessException("旧单总额与明细不一致，接入不会自动改价");
        String settlement = ((Number)order.get("PayType")).intValue() == 1 ? "monthly" : "cash";
        store.jdbc().update("INSERT INTO lin_order_extension(order_id,user_id,laboratory_id,original_amount,discount_amount,payable_amount,settlement_mode) VALUES(?,?,?,?,0,?,?)", orderId, order.get("CreateSampleId"), laboratoryId, amount, amount, settlement);
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,after_json) VALUES(?,'LEGACY_ORDER_ADOPTED','ORDER',?,?)", access.current().userId(), String.valueOf(orderId), store.encode(java.util.Map.of("laboratoryId", laboratoryId,"amount",amount,"settlementMode",settlement)));
    }
}
