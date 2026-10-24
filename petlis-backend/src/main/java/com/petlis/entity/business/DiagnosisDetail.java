package com.petlis.entity.business;

import com.petlis.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 检测明细实体类
 * 对应数据库表: lin_lb_diagnosisdetail
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "lin_lb_diagnosisdetail")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DiagnosisDetail  {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DiagnosisDetailId", nullable = false)
    private Integer diagnosisDetailId;

    /**
     * 所属项目ID
     */
    @Column(name = "ListDiagnosisProjectId", nullable = false)
    private Integer listDiagnosisProjectId;

    /**
     * 编号
     */
    @Column(name = "GenterNO", nullable = false, length = 250)
    private String genterNO;

    /**
     * 测试项ID
     */
    @Column(name = "InfoTestTypeId", nullable = false)
    private Integer infoTestTypeId;

    /**
     * 测试项模型ID
     */
    @Column(name = "InfoTestTypeModelId", nullable = false)
    private Integer infoTestTypeModelId;

    /**
     * 邮件标题
     */
    @Column(name = "MailTitle", nullable = false, length = 250)
    private String mailTitle;

    /**
     * 创建时间
     */
    @Column(name = "CreateDt")
    private Date createDt;

    /**
     * 是否修改模板
     */
    @Column(name = "IsChangeModelTemple", nullable = false)
    private Integer isChangeModelTemple;

    /**
     * 是否修改邮件模板
     */
    @Column(name = "IsChangeMailModelTemple", nullable = false)
    private Integer isChangeMailModelTemple;

    /**
     * 是否已发送
     */
    @Column(name = "IsSendMail", nullable = false)
    private Integer isSendMail;

    /**
     * 实际费用
     */
    @Column(name = "RealFee", nullable = false)
    private Double realFee;

    /**
     * 是否审核通过
     */
    @Column(name = "IsCheckPass", nullable = false)
    private Integer isCheckPass;

    /**
     * PDF地址
     */
    @Column(name = "PDFHttpUrl", nullable = false, length = 250)
    private String pdfHttpUrl;

    /**
     * 测试价格
     */
    @Column(name = "TestPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal testPrice;

    /**
     * 原始价格
     */
    @Column(name = "OrginalPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal orginalPrice;

    /**
     * 原始折扣
     */
    @Column(name = "OrginalDiscount", nullable = false, precision = 18, scale = 2)
    private BigDecimal orginalDiscount;

    /**
     * 发送邮件时间
     */
    @Column(name = "SendMailDt")
    private Date sendMailDt;

    /**
     * 明细备注
     */
    @Column(name = "DetailRemark", nullable = false, length = 250)
    private String detailRemark;

    /**
     * 支付状态
     */
    @Column(name = "PayStatus2", nullable = false)
    private Integer payStatus2;

    /**
     * 诊断状态
     */
    @Column(name = "DiagnosisStatus", nullable = false)
    private Integer diagnosisStatus;

    /**
     * 是否启用
     */
    @Column(name = "IsActive", nullable = false)
    private Integer isActive;

    /**
     * 是否已开发票
     */
    @Column(name = "is_invoiced", nullable = false)
    private Integer isInvoiced;


    public Integer getId() {
        return diagnosisDetailId;
    }

}
