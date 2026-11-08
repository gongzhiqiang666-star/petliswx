package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Narrow, audited pre-review correction; never changes identity, pricing, items or downstream records. */
@Service
@RequiredArgsConstructor
public class LegacyClinicalService {
    private final FeatureStore store;
    private final AccessService access;
    private final BusinessNotifications notifications;
    private static final Map<String,String> ORIGINAL = new LinkedHashMap<>();
    static {
        ORIGINAL.put("animalName","AnimalName"); ORIGINAL.put("animalNameEn","AnimalName_en");
        ORIGINAL.put("animalWeight","AnimalWeight"); ORIGINAL.put("chipNO","ChipNO");
        ORIGINAL.put("age","Age"); ORIGINAL.put("sampleArea","SampleArea");
        ORIGINAL.put("masterName","MasterName"); ORIGINAL.put("masterNameEn","MasterName_en");
        ORIGINAL.put("doctorName","DoctorName"); ORIGINAL.put("remark","Remark");
    }
    private static final Map<String,String> CLINICAL = Map.of("medicalHistory","medical_history","clinicalSymptoms","clinical_symptoms","diagnosis","diagnosis","medication","medication");
    public record Correction(String version, Map<String,String> fields, String reason, boolean resubmit) { }

    @Transactional(readOnly=true)
    public Map<String,Object> read(int orderId) {
        var row=order(orderId,false);
        var actor=access.current();
        if(!actor.isAdmin()) {
            if(actor.hasRole(PlatformRole.LAB_STAFF)) access.requireLaboratory(((Number)row.get("laboratory_id")).intValue());
            else access.requireHospital(((Number)row.get("InfoHospitalId")).intValue());
        }
        return snapshot(orderId,row,false);
    }

    @Transactional
    public void correct(int orderId, Correction command) {
        access.requireAdmin();
        if(command==null || command.fields()==null || command.reason()==null || command.reason().isBlank() || command.reason().length()>400) throw new BusinessException(400,"请填写修正原因，最多400字");
        var row=order(orderId,true);
        int status=((Number)row.get("SampleStatus")).intValue();
        if(!editable(orderId,row)) throw new BusinessException(409,"仅无下游记录的待审核或未通过送检单可修正资料");
        var before=snapshot(orderId,row,true);
        if(command.version()==null || !command.version().equals(before.get("version"))) throw new BusinessException(409,"资料已被其他操作更新，请刷新后核对");
        if(command.resubmit() && status!=1) throw new BusinessException(409,"仅审核未通过的单据可重新送审");
        @SuppressWarnings("unchecked") var oldFields=(Map<String,String>)before.get("fields");
        var fields=new LinkedHashMap<String,String>();
        var allowed=new HashSet<>(ORIGINAL.keySet());allowed.addAll(CLINICAL.keySet());
        if(!command.fields().keySet().equals(allowed)) throw new BusinessException(400,"修正字段不完整或包含不允许修改的字段");
        for(var key:allowed){
            String value=command.fields().get(key);
            if(value==null || value.length()>(CLINICAL.containsKey(key)?2000:250)) throw new BusinessException(400,"资料字段为空或超过允许长度");
            fields.put(key,value.trim());
        }
        if(fields.get("animalName").isBlank() || fields.get("masterName").isBlank()) throw new BusinessException(400,"宠物姓名和畜主姓名必填");
        String age=fields.get("age");
        if(!age.equals(oldFields.get("age")) && !age.matches("(?=.+)(?:[0-9]{1,3}Y)?(?:[0-9]{1,2}M)?")) throw new BusinessException(400,"年龄请使用3Y7M、5M或3Y格式");
        boolean rabies=Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND InfoTestTypeId=1225 AND IsActive=1 AND IsCheckPass=0)",Boolean.class,orderId));
        if(rabies && (fields.get("chipNO").isBlank() || fields.get("remark").isBlank())) throw new BusinessException(400,"狂犬检测必须保留芯片号和送检备注");
        var args=new ArrayList<Object>();var sets=new ArrayList<String>();
        ORIGINAL.forEach((key,column)->{sets.add(column+"=?");args.add(fields.get(key));});args.add(orderId);
        store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET "+String.join(",",sets)+" WHERE ListDiagnosisProjectId=?",args.toArray());
        if(rabies) store.jdbc().update("UPDATE lin_lb_diagnosisdetail SET DetailRemark=? WHERE ListDiagnosisProjectId=? AND InfoTestTypeId=1225 AND IsActive=1 AND IsCheckPass=0",fields.get("remark"),orderId);
        store.jdbc().update("INSERT INTO lin_order_clinical(order_id,hospital_id,medical_history,clinical_symptoms,diagnosis,medication,created_by) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE medical_history=VALUES(medical_history),clinical_symptoms=VALUES(clinical_symptoms),diagnosis=VALUES(diagnosis),medication=VALUES(medication),updated_by=VALUES(created_by)",orderId,row.get("InfoHospitalId"),fields.get("medicalHistory"),fields.get("clinicalSymptoms"),fields.get("diagnosis"),fields.get("medication"),access.current().userId());
        store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,before_json,after_json,reason) VALUES(?,'LEGACY_CLINICAL_UPDATED','ORDER',?,?,?,?)",access.current().userId(),String.valueOf(orderId),store.encode(oldFields),store.encode(fields),command.reason().trim());
        if(command.resubmit()) {
            store.jdbc().update("UPDATE lin_order_extension SET review_status='pending',review_reason='',reviewed_by=NULL,reviewed_at=NULL WHERE order_id=?",orderId);
            store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=0 WHERE ListDiagnosisProjectId=?",orderId);
            notifications.order(orderId,((Number)row.get("CreateSampleId")).intValue(),0);
        }
    }

    private Map<String,Object> order(int id, boolean lock) {
        return store.one("SELECT o.*,e.laboratory_id,e.review_status,e.updated_at AS extension_updated_at FROM lin_lb_listdiagnosisproject o JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId WHERE o.ListDiagnosisProjectId=?"+(lock?" FOR UPDATE":""),id);
    }
    private boolean editable(int id,Map<String,Object> row) {
        int status=((Number)row.get("SampleStatus")).intValue();
        if(!List.of(0,1).contains(status) || ((Number)row.get("IsPayMoney")).intValue()==1 || !List.of("pending","rejected").contains(String.valueOf(row.get("review_status")))) return false;
        return !Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_shipment WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_lab_task WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_report_workflow WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND (IsSendMail=1 OR DiagnosisStatus>0 OR PayStatus2<>0))",Boolean.class,id,id,id,id));
    }
    private Map<String,Object> snapshot(int id,Map<String,Object> row,boolean lock) {
        var clinical=store.jdbc().queryForList("SELECT medical_history,clinical_symptoms,diagnosis,medication,updated_at FROM lin_order_clinical WHERE order_id=?"+(lock?" FOR UPDATE":""),id);
        var fields=new TreeMap<String,String>();
        ORIGINAL.forEach((key,column)->fields.put(key,Objects.toString(row.get(column),"")));
        CLINICAL.forEach((key,column)->fields.put(key,clinical.isEmpty()?"":Objects.toString(clinical.get(0).get(column),"")));
        var revisions=store.jdbc().queryForList("SELECT id FROM lin_audit_record WHERE resource_type='ORDER' AND resource_id=? AND action='LEGACY_CLINICAL_UPDATED' ORDER BY id DESC LIMIT 1"+(lock?" FOR UPDATE":""),String.valueOf(id));
        var versionSource=new ArrayList<Object>(List.of(fields,String.valueOf(row.get("SampleStatus")),String.valueOf(row.get("review_status")),String.valueOf(row.get("extension_updated_at"))));
        versionSource.add(clinical.isEmpty()?"":String.valueOf(clinical.get(0).get("updated_at")));versionSource.add(revisions.isEmpty()?0:revisions.get(0).get("id"));
        try {
            String version=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(versionSource).getBytes(StandardCharsets.UTF_8)));
            return Map.of("fields",fields,"version",version,"editable",editable(id,row),"status",row.get("SampleStatus"));
        }catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}
    }
}
