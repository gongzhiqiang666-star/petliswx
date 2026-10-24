package com.petlis.entity.business;

import com.petlis.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 检测项目实体类
 * 对应数据库表: lin_lb_listdiagnosisproject
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "lin_lb_listdiagnosisproject")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ListDiagnosisProject  {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ListDiagnosisProjectId", nullable = false)
    private Integer listDiagnosisProjectId;

    /**
     * 创建数据类型ID
     */
    @Column(name = "CreateDataTypeIId", nullable = false)
    private Long createDataTypeIId;

    /**
     * 用户编号
     */
    @Column(name = "UserNO", nullable = false, length = 250)
    private String userNO;

    /**
     * 城市ID
     */
    @Column(name = "CityId", nullable = false)
    private Integer cityId;

    /**
     * 医院ID
     */
    @Column(name = "InfoHospitalId", nullable = false)
    private Integer infoHospitalId;

    /**
     * 物种ID
     */
    @Column(name = "InfoAnimalId", nullable = false)
    private Integer infoAnimalId;

    /**
     * 采样方式ID
     */
    @Column(name = "InfoSampleTypeId", nullable = false)
    private Integer infoSampleTypeId;

    /**
     * 宠物姓名
     */
    @Column(name = "AnimalName", nullable = false, length = 250)
    private String animalName;

    /**
     * 宠物姓名(英文)
     */
    @Column(name = "AnimalName_en", nullable = false, length = 250)
    private String animalNameEn;

    /**
     * 品种ID
     */
    @Column(name = "AnimalTypeId", nullable = false)
    private Integer animalTypeId;

    /**
     * 品种
     */
    @Column(name = "AnimalType", nullable = false, length = 250)
    private String animalType;

    /**
     * 品种(英文)
     */
    @Column(name = "AnimalType_en", nullable = false, length = 250)
    private String animalTypeEn;

    /**
     * 体重
     */
    @Column(name = "AnimalWeight", nullable = false, length = 250)
    private String animalWeight;

    /**
     * 芯片号
     */
    @Column(name = "ChipNO", nullable = false, length = 250)
    private String chipNO;

    /**
     * 年龄
     */
    @Column(name = "Age", nullable = false, length = 250)
    private String age;

    /**
     * 生日
     */
    @Column(name = "Birthday")
    private Date birthday;

    /**
     * 性别ID
     */
    @Column(name = "DictSexId", nullable = false)
    private Integer dictSexId;

    /**
     * 采样部位
     */
    @Column(name = "SampleArea", nullable = false, length = 250)
    private String sampleArea;

    /**
     * 畜主姓名
     */
    @Column(name = "MasterName", nullable = false, length = 250)
    private String masterName;

    /**
     * 畜主姓名(英文)
     */
    @Column(name = "MasterName_en", nullable = false, length = 250)
    private String masterNameEn;

    /**
     * 诊断结果
     */
    @Column(name = "DiagnosticResults", nullable = false, length = 250)
    private String diagnosticResults;

    /**
     * 采样日期
     */
    @Column(name = "CollectDt")
    private Date collectDt;

    /**
     * 收到样本日期
     */
    @Column(name = "ReceiveSampleDt")
    private Date receiveSampleDt;

    /**
     * 寄出样本日期
     */
    @Column(name = "SendSampleDt")
    private Date sendSampleDt;

    /**
     * 报告编号
     */
    @Column(name = "ReportNO", nullable = false, length = 250)
    private String reportNO;

    /**
     * 报告发出日期
     */
    @Column(name = "SendReportDt")
    private Date sendReportDt;

    /**
     * 测试费用
     */
    @Column(name = "TestFee", nullable = false)
    private Double testFee;

    /**
     * 中国至德国快递费
     */
    @Column(name = "DeliverySendGerFee", nullable = false)
    private Double deliverySendGerFee;

    /**
     * 德国至中国快递费
     */
    @Column(name = "DeliverySendCNFee", nullable = false)
    private Double deliverySendCNFee;

    /**
     * 确认回收样本ID
     */
    @Column(name = "InfoCheckReceivedId", nullable = false)
    private Integer infoCheckReceivedId;

    /**
     * 是否存入媒介
     */
    @Column(name = "SaveMedium", nullable = false)
    private Integer saveMedium;

    /**
     * 是否付款
     */
    @Column(name = "IsPayMoney", nullable = false)
    private Integer isPayMoney;

    /**
     * 备注
     */
    @Column(name = "Remark", nullable = false, length = 250)
    private String remark;

    /**
     * 是否发送通知邮件
     */
    @Column(name = "IsSendNoticeMail", nullable = false)
    private Integer isSendNoticeMail;

    /**
     * 样本状态
     */
    @Column(name = "SampleStatus", nullable = false)
    private Integer sampleStatus;

    /**
     * 医生姓名
     */
    @Column(name = "DoctorName", nullable = false, length = 250)
    private String doctorName;

    /**
     * 市场活动名称
     */
    @Column(name = "MarketingName", nullable = false, length = 250)
    private String marketingName;

    /**
     * 创建样本ID
     */
    @Column(name = "CreateSampleId", nullable = false)
    private Integer createSampleId;

    /**
     * 创建时间
     */
    @Column(name = "CreateDt", nullable = false)
    private Date createDt;


    public Integer getId() {
        return listDiagnosisProjectId;
    }

}
