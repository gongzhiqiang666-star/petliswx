package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminOrderService {
    private final FeatureStore store;
    private final AccessService access;

    public Map<String, Object> list(Integer status, int page, int size) {
        CurrentActor actor = access.current();
        if (!actor.isAdmin() && !actor.hasRole(PlatformRole.LAB_STAFF) && !actor.hasRole(PlatformRole.HOSPITAL_ADMIN)) throw new BusinessException(403, "无权查看订单管理");
        StringBuilder where = new StringBuilder(" FROM lin_lb_listdiagnosisproject o JOIN lin_lb_infohospital h ON h.InfoHospitalId=o.InfoHospitalId LEFT JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (status != null) { where.append(" AND o.SampleStatus=?"); args.add(status); }
        if (!actor.isAdmin()) {
            if (actor.hasRole(PlatformRole.LAB_STAFF)) where.append(" AND e.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)");
            else where.append(" AND o.InfoHospitalId IN (SELECT hospital_id FROM lin_user_role WHERE user_id=? AND role_code='HOSPITAL_ADMIN' AND is_active=1)");
            args.add(actor.userId());
        }
        return store.page("SELECT COUNT(*)" + where, "SELECT o.ListDiagnosisProjectId AS orderId,o.UserNO AS orderNo,o.InfoHospitalId AS hospitalId,h.HospitalName AS hospitalName,e.laboratory_id AS laboratoryId,o.AnimalName AS animalName,o.TestFee AS amount,o.SampleStatus AS status,o.IsPayMoney AS paid,e.settlement_mode AS settlementMode,o.CreateDt AS createdAt" + where + " ORDER BY o.ListDiagnosisProjectId DESC", page, size, args.toArray());
    }
}
