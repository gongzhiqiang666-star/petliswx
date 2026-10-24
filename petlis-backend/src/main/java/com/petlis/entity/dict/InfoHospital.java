package com.petlis.entity.dict;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 医院信息实体类
 * 对应数据库表: lin_lb_infohospital
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "lin_lb_infohospital")
public class InfoHospital {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InfoHospitalId", nullable = false)
    private Integer infoHospitalId;

    @Column(name = "HospitalName", nullable = false, length = 250)
    private String hospitalName;

    @Column(name = "HospitalName_en", nullable = false, length = 250)
    private String hospitalNameEn;

    @Column(name = "Invoice", length = 250)
    private String invoice;

    @Column(name = "CityId")
    private Integer cityId;

    @Column(name = "Place", columnDefinition = "text")
    private String place;

    @Column(name = "Adress", columnDefinition = "text")
    private String adress;

    @Column(name = "Tel", length = 50)
    private String tel;

    @Column(name = "Fax", length = 50)
    private String fax;

    @Column(name = "InfoHospitalContactId")
    private Integer infoHospitalContactId;

    @Column(name = "CreateDt")
    private Date createDt;

    @Column(name = "IsActive")
    private Integer isActive;

    @Column(name = "LoginName", length = 50)
    private String loginName;

    @Column(name = "LoginPwd", length = 50)
    private String loginPwd;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "PayType", nullable = false)
    private Integer payType;
}
