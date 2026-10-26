package com.petlis.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final QualificationStorage qualifications;

    public Map<String, Object> summary() {
        int userId = SecurityUtils.getCurrentUserId();
        var rows = jdbc.queryForList("""
                SELECT u.id AS userId, u.nickname, u.avatar, u.mobile, p.qualification_files AS qualificationFiles, u.hospital_id AS hospitalId,
                       u.money AS balance, u.score AS points, p.title, p.review_status AS reviewStatus,
                       h.HospitalName AS hospitalName
                FROM lin_user u LEFT JOIN lin_doctor_profile p ON p.user_id=u.id
                LEFT JOIN lin_lb_infohospital h ON h.InfoHospitalId=u.hospital_id WHERE u.id=?
                """, userId);
        if (rows.isEmpty()) throw new BusinessException(404, "用户不存在");
        return rows.get(0);
    }
    public Map<String, Object> transactions(boolean points, int page, int size, String category) {
        page = Math.max(page, 0); size = Math.max(1, Math.min(size, 100));
        int userId = SecurityUtils.getCurrentUserId();
        String table = points ? "lin_user_score_log" : "lin_user_money_log";
        String amount = points ? "score" : "money";
        String filter = "income".equals(category) ? " AND " + amount + ">0" : "expense".equals(category) ? " AND " + amount + "<0" : "";
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE user_id=?" + filter, Long.class, userId);
        var items = jdbc.queryForList("SELECT id," + amount + " AS amount,`before` AS balanceBefore,`after` AS balanceAfter,memo,createtime AS createdAt FROM "
                + table + " WHERE user_id=?" + filter + " ORDER BY id DESC LIMIT ? OFFSET ?", userId, size, (long)page * size);
        return Map.of("items", items, "total", total == null ? 0 : total, "page", page, "size", size);
    }
    public Map<String, Object> preferences() {
        var rows = jdbc.queryForList("SELECT order_notifications AS orderNotifications,report_notifications AS reportNotifications,invoice_notifications AS invoiceNotifications,news_notifications AS newsNotifications,preferred_laboratory_id AS preferredLaboratoryId FROM lin_user_preferences WHERE user_id=?", SecurityUtils.getCurrentUserId());
        if (!rows.isEmpty()) return rows.get(0);
        return Map.of("orderNotifications", true, "reportNotifications", true, "invoiceNotifications", true, "newsNotifications", false);
    }
    public record Preferences(boolean orderNotifications, boolean reportNotifications, boolean invoiceNotifications,
                              boolean newsNotifications, Integer preferredLaboratoryId) { }
    @Transactional
    public void savePreferences(Preferences r) {
        if (r.preferredLaboratoryId() != null && !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lin_laboratory WHERE id=? AND active=1)", Boolean.class, r.preferredLaboratoryId()))) {
            throw new BusinessException("实验室不可用");
        }
        jdbc.update("""
                INSERT INTO lin_user_preferences(user_id,order_notifications,report_notifications,invoice_notifications,news_notifications,preferred_laboratory_id)
                VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE order_notifications=VALUES(order_notifications),report_notifications=VALUES(report_notifications),
                invoice_notifications=VALUES(invoice_notifications),news_notifications=VALUES(news_notifications),preferred_laboratory_id=VALUES(preferred_laboratory_id)
                """, SecurityUtils.getCurrentUserId(), r.orderNotifications(), r.reportNotifications(), r.invoiceNotifications(), r.newsNotifications(), r.preferredLaboratoryId());
    }
    public record ProfileChange(String nickname, String title, String avatar, Integer hospitalId, List<String> qualificationFiles) { }
    @Transactional
    public void requestProfileChange(ProfileChange r) {
        if (r.nickname() == null || r.nickname().isBlank() || r.nickname().length()>50 || r.hospitalId()==null || r.title()==null || r.title().length()>100) {
            throw new BusinessException("请完整填写姓名、职称和医院");
        }
        if (r.avatar()!=null && r.avatar().length()>255) throw new BusinessException("头像地址过长");
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lin_lb_infohospital WHERE InfoHospitalId=? AND IsActive=1)", Boolean.class, r.hospitalId()))) throw new BusinessException("医院不可用");
        qualifications.validate(r.qualificationFiles());
        int userId = SecurityUtils.getCurrentUserId();
        jdbc.queryForList("SELECT id FROM lin_user WHERE id=? FOR UPDATE", userId);
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lin_profile_change WHERE user_id=? AND status='pending')", Boolean.class, userId))) throw new BusinessException("已有待审核资料，请等待审核");
        jdbc.update("INSERT INTO lin_profile_change(user_id,requested_data,previous_data) VALUES(?,?,?)",userId,encode(new ProfileChange(r.nickname(),r.title(),r.avatar()==null?java.util.Objects.toString(summary().get("avatar"),""):r.avatar(),r.hospitalId(),r.qualificationFiles())),encode(summary()));
    }
    public List<Map<String,Object>> profileHistory() {
        return jdbc.queryForList("SELECT id,status,review_reason AS reviewReason,created_at AS createdAt,reviewed_at AS reviewedAt FROM lin_profile_change WHERE user_id=? ORDER BY id DESC LIMIT 100",SecurityUtils.getCurrentUserId());
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new BusinessException("资料格式无效"); }
    }
    public String exportTransactions(boolean points, String category) {
        int userId = SecurityUtils.getCurrentUserId();
        String table = points ? "lin_user_score_log" : "lin_user_money_log";
        String amount = points ? "score" : "money";
        String filter = "income".equals(category) ? " AND "+amount+">0" : "expense".equals(category) ? " AND "+amount+"<0" : "";
        var rows=jdbc.queryForList("SELECT "+amount+" AS amount,`after` AS balance,memo,createtime FROM "+table+" WHERE user_id=?"+filter+" ORDER BY id DESC LIMIT 10000",userId);
        StringBuilder out=new StringBuilder("\uFEFF变动,余额,说明,时间戳\r\n");
        for(var row:rows) out.append(csv(row.get("amount"))).append(',').append(csv(row.get("balance"))).append(',').append(csv(row.get("memo"))).append(',').append(csv(row.get("createtime"))).append("\r\n");
        return out.toString();
    }
    static String csv(Object value) {
        String s=value==null?"":String.valueOf(value);
        if(s.matches("^[=+@\t\r].*") || s.startsWith("-")) s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
}
