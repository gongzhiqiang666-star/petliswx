package com.petlis.entity.business;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 检测报告内容实体类
 * 对应数据库表: lin_lb_diagnosisdetail_info
 * 存储检测报告的详细内容(邮件报告和检测结果)
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "lin_lb_diagnosisdetail_info")
public class DiagnosisDetailInfo {

    private static final long serialVersionUID = 1L;

    /**
     * 关联检测明细ID (主键,外键,关联lin_lb_diagnosisdetail.DiagnosisDetailId)
     */
    @Id
    @Column(name = "diagnosisdetail_id", nullable = false)
    private Integer diagnosisdetailId;

    /**
     * 邮件报告内容 (长文本,存储完整的邮件报告模板)
     */
    @Column(name = "MailReport", columnDefinition = "longtext")
    private String mailReport;

    /**
     * 报告结果 (长文本,存储检测报告结果)
     */
    @Column(name = "ReportResult", columnDefinition = "longtext")
    private String reportResult;
}
