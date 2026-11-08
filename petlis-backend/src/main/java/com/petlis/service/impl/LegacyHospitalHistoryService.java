package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/** User::Pros and the index model's Detail relation. Read-only hospital business history. */
@Service
@RequiredArgsConstructor
public class LegacyHospitalHistoryService {
    private final FeatureStore store;
    private final AccessService access;
    public record Query(Integer hospitalId, String userNo, Integer status,
                        String from, String to, Integer page, Integer size) {}

    private boolean hospitalRole() {
        var actor = access.current();
        return actor.isAdmin() || actor.hasRole(PlatformRole.DOCTOR)
                || actor.hasRole(PlatformRole.HOSPITAL_ADMIN);
    }
    static final String PARENT_FIELDS = "p.ListDiagnosisProjectId AS orderId,p.UserNO AS userNo,DATE_FORMAT(p.CreateDt,'%Y-%m-%d %H:%i:%s') AS submittedAt,p.DoctorName AS doctorName,p.MasterName AS ownerName,p.AnimalName AS animalName,p.SampleStatus AS sampleStatus,p.Remark AS remark,p.IsPayMoney AS paidFlag,(EXISTS(SELECT 1 FROM lin_order_extension e WHERE e.order_id=p.ListDiagnosisProjectId) OR EXISTS(SELECT 1 FROM lin_shipment s WHERE s.order_id=p.ListDiagnosisProjectId) OR EXISTS(SELECT 1 FROM lin_report_workflow w WHERE w.order_id=p.ListDiagnosisProjectId)) AS hasNewWorkflow";
    @Transactional(readOnly = true)
    public Map<String, Boolean> capabilities() { return policy(false); }
    Map<String, Boolean> policy(boolean lock) {
        if (!hospitalRole()) return Map.of("legacyHospitalReportDownload",false,"legacyHospitalHistory", false, "legacyHospitalHistoryCancel", false,"legacyHospitalDrafts",false,"legacyHospitalDraftWrites",false,"legacyHospitalDraftItems",false,"legacyHospitalDraftCreates",false,"legacyHospitalDraftSubmit",false);
        var rows = store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-hospital-history' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'" + (lock ? " FOR UPDATE" : ""));
        if (rows.size() != 1) return Map.of("legacyHospitalReportDownload",false,"legacyHospitalHistory", false, "legacyHospitalHistoryCancel", false,"legacyHospitalDrafts",false,"legacyHospitalDraftWrites",false,"legacyHospitalDraftItems",false,"legacyHospitalDraftCreates",false,"legacyHospitalDraftSubmit",false);
        try {
            var json = new ObjectMapper().readTree(rows.get(0).get("config_value").toString());
            boolean enabled = flag(json, "enabled");
            return Map.of("legacyHospitalReportDownload",enabled&&flag(json,"reportDownloadEnabled"),"legacyHospitalHistory", enabled, "legacyHospitalHistoryCancel", enabled && flag(json,"writeEnabled") && flag(json,"cancelEnabled"),"legacyHospitalDrafts",enabled && flag(json,"draftEnabled"),"legacyHospitalDraftWrites",enabled && flag(json,"draftEnabled") && flag(json,"writeEnabled") && flag(json,"draftWriteEnabled"),"legacyHospitalDraftItems",enabled && flag(json,"draftEnabled") && flag(json,"writeEnabled") && flag(json,"draftWriteEnabled") && flag(json,"draftItemsEnabled"),"legacyHospitalDraftCreates",enabled && flag(json,"draftEnabled") && flag(json,"writeEnabled") && flag(json,"draftWriteEnabled") && flag(json,"draftCreateEnabled"),"legacyHospitalDraftSubmit",enabled && flag(json,"draftEnabled") && flag(json,"writeEnabled") && flag(json,"draftWriteEnabled") && flag(json,"draftSubmitEnabled") && Set.of("ID","LEGACY").contains(json.path("previewVarietyMode").asText()));
        } catch (Exception error) { throw new BusinessException(409, "医院历史查询参数无效"); }
    }
    @Transactional(readOnly=true)
    public Map<String,Object> hospitals(String query,int page,int size){
        if(!hospitalRole())throw new BusinessException(403,"需要医院业务权限");
        if(!policy(false).get("legacyHospitalHistory"))throw new BusinessException(409,"医院历史查询尚未开启");
        String q=query==null?"":query.trim();
        if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE||q.length()>100)throw new BusinessException(400,"医院查询参数无效");
        var actor=access.current();var ids=new TreeSet<>(actor.hospitalIds());ids.removeIf(id->id<=0);var args=new ArrayList<Object>();
        String scope=actor.dataScope().global()?"1=1":ids.isEmpty()?"1=0":"InfoHospitalId IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")";
        if(!actor.dataScope().global())args.addAll(ids);args.add("%"+q+"%");String where=" WHERE "+scope+" AND HospitalName LIKE ?";
        return store.page("SELECT COUNT(*) FROM lin_lb_infohospital"+where,"SELECT InfoHospitalId AS id,HospitalName AS name FROM lin_lb_infohospital"+where+" ORDER BY InfoHospitalId",page,size,args.toArray());
    }
    private static boolean flag(com.fasterxml.jackson.databind.JsonNode json,String key) { return json.path(key).isBoolean() && json.path(key).asBoolean(); }
    Map<String,Object> cancellationSnapshot(long id,int hospitalId,boolean lock) {
        return store.one("SELECT " + PARENT_FIELDS + " FROM lin_lb_listdiagnosisproject p WHERE p.ListDiagnosisProjectId=? AND p.InfoHospitalId=?" + (lock ? " FOR UPDATE" : ""),id,hospitalId);
    }
    String cancellationToken(Map<String,Object> row) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    static boolean hasNewWorkflow(Map<String,Object> row) { Object value=row.get("hasNewWorkflow"); return Boolean.TRUE.equals(value) || value instanceof Number n && n.intValue()!=0; }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Query query) {
        if (!hospitalRole()) throw new BusinessException(403, "需要医院业务权限");
        if (!capabilities().get("legacyHospitalHistory")) throw new BusinessException(409, "医院历史查询尚未开启");
        if (query == null || query.hospitalId() == null || query.hospitalId() <= 0
                || !access.current().canAccessHospital(query.hospitalId()))
            throw new BusinessException(403, "无权查看该医院送检历史");
        var hospital = store.one("SELECT HospitalName FROM lin_lb_infohospital WHERE InfoHospitalId=?", query.hospitalId());
        int page = query.page() == null ? 0 : query.page();
        int size = query.size() == null ? 10 : query.size();
        if (page < 0 || size < 1 || size > 100 || (long)page * size > Integer.MAX_VALUE)
            throw new BusinessException(400, "分页参数无效");
        if (query.status() != null && !Set.of(-1, 0, 1, 2, 3, 4).contains(query.status()))
            throw new BusinessException(400, "历史状态筛选无效");
        var args = new ArrayList<Object>(); args.add(query.hospitalId());
        var where = new StringBuilder(" WHERE p.InfoHospitalId=? AND p.SampleStatus<>-2");
        String no = query.userNo() == null ? "" : query.userNo().trim();
        if (no.length() > 100) throw new BusinessException(400, "样本编号搜索过长");
        // PHP empty('0') is true; retain the original LIKE wildcard semantics.
        if (!no.isEmpty() && !no.equals("0")) { where.append(" AND p.UserNO LIKE ?"); args.add("%" + no + "%"); }
        if (query.status() != null && query.status() >= 0) { where.append(" AND p.SampleStatus=?"); args.add(query.status()); }
        if (query.from() != null && !query.from().isBlank() || query.to() != null && !query.to().isBlank()) {
            try {
                var from = LocalDateTime.parse(query.from()); var to = LocalDateTime.parse(query.to());
                if (from.isAfter(to)) throw new IllegalArgumentException();
                where.append(" AND p.CreateDt BETWEEN ? AND ?"); args.add(from); args.add(to);
            } catch (DateTimeParseException | NullPointerException error) { throw new BusinessException(400, "请提供有效的起止日期时间"); }
            catch (IllegalArgumentException error) { throw new BusinessException(400, "结束时间不能早于开始时间"); }
        }
        long total = store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_listdiagnosisproject p" + where, Long.class, args.toArray());
        var paged = new ArrayList<>(args); paged.add(size); paged.add((long)page * size);
        var rows = store.jdbc().queryForList("SELECT " + PARENT_FIELDS + " FROM lin_lb_listdiagnosisproject p" + where + " ORDER BY p.ListDiagnosisProjectId DESC LIMIT ? OFFSET ?", paged.toArray());
        // One bounded batch preserves the source INNER JOIN, including exempt/inactive test items.
        var byOrder = new HashMap<Long, List<Map<String, Object>>>();
        if (!rows.isEmpty()) {
            var ids = rows.stream().map(row -> row.get("orderId")).toList();
            String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
            var details = store.jdbc().queryForList("SELECT d.ListDiagnosisProjectId AS orderId,d.DiagnosisDetailId AS detailId,t.TestName AS testName,t.TestNO AS testNo,d.IsSendMail AS mailFlag FROM lin_lb_diagnosisdetail d INNER JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=d.InfoTestTypeId WHERE d.ListDiagnosisProjectId IN (" + placeholders + ") ORDER BY d.DiagnosisDetailId", ids.toArray());
            for (var detail : details) byOrder.computeIfAbsent(((Number)detail.get("orderId")).longValue(), id -> new ArrayList<>()).add(detail);
        }
        var caps=capabilities();boolean cancelEnabled = caps.get("legacyHospitalHistoryCancel"),reportDownload=caps.get("legacyHospitalReportDownload");
        for (var row : rows) {
            int status = ((Number)row.get("sampleStatus")).intValue();
            row.put("cancelToken", cancellationToken(row));
            row.put("canCancel", cancelEnabled && status == 0 && !hasNewWorkflow(row));
            row.put("paid", row.get("paidFlag") instanceof Number n && n.intValue() == 1);
            row.remove("paidFlag"); row.remove("hasNewWorkflow");
            row.put("statusLabel", switch (status) { case 0 -> "待审核"; case 1 -> "审核未通过"; case 2 -> "审核通过"; case 3 -> "已出报告"; case 4 -> "已取消"; case -3 -> "待支付"; default -> "原状态 " + status; });
            row.put("rejectionReason", status == 1 ? Objects.toString(row.get("remark"), "") : ""); row.remove("remark");
            var items = byOrder.getOrDefault(((Number)row.get("orderId")).longValue(), List.of());
            for (var item : items) {
                int sent = item.get("mailFlag") instanceof Number n ? n.intValue() : 0;
                item.put("canViewLegacyReport",status>=2&&sent==1);item.put("canDownloadLegacyReport",reportDownload&&status>=2&&sent==1);
                item.put("reportState", status == 2 || status == 3 ? sent == 0 ? "等待报告" : "已出报告" : "");
                item.remove("mailFlag"); item.remove("orderId");
            }
            row.put("items", items);
        }
        return Map.of("items", rows, "total", total, "page", page, "size", size,
                "hospitalId", query.hospitalId(), "hospitalName", Objects.toString(hospital.get("HospitalName"), ""));
    }
}
