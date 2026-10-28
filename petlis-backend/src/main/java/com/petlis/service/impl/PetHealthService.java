package com.petlis.service.impl;

import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PetHealthService {
    private final FeatureStore store;
    private final AccessService access;

    @Transactional(readOnly = true)
    public Map<String, Object> health(int petId, int page, int size) {
        Map<String, Object> pet = store.one("SELECT id,info_hospital_id AS hospital_id,animal_name AS petName FROM lin_lb_pet WHERE id=? AND IsActive=1", petId);
        int hospitalId = ((Number) pet.get("hospital_id")).intValue();
        access.requireHospital(hospitalId);
        var history = store.jdbc().queryForList("SELECT c.order_id AS orderId,c.medical_history AS medicalHistory,c.clinical_symptoms AS clinicalSymptoms,c.diagnosis,c.medication,o.CreateDt AS createDt FROM lin_order_clinical c JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=c.order_id WHERE c.pet_id=? AND c.hospital_id=? ORDER BY c.order_id DESC", petId, hospitalId);
        var reports = store.page(
                "SELECT COUNT(*) FROM lin_lb_diagnosisdetail d JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=d.ListDiagnosisProjectId JOIN lin_order_clinical c ON c.order_id=o.ListDiagnosisProjectId WHERE c.pet_id=? AND c.hospital_id=?",
                "SELECT d.DiagnosisDetailId AS reportId,o.ListDiagnosisProjectId AS orderId,o.UserNO AS orderNo,t.TestName AS testTypeName,d.DiagnosisStatus AS diagnosisStatus,d.SendReportDt AS sendReportDt,o.CreateDt AS createDt FROM lin_lb_diagnosisdetail d JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=d.ListDiagnosisProjectId JOIN lin_order_clinical c ON c.order_id=o.ListDiagnosisProjectId JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=d.InfoTestTypeId WHERE c.pet_id=? AND c.hospital_id=? ORDER BY d.DiagnosisDetailId DESC",
                page, size, petId, hospitalId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("petId", petId);
        result.put("petName", pet.get("petName"));
        result.put("medicalHistory", history);
        result.put("reports", reports);
        return result;
    }
}
