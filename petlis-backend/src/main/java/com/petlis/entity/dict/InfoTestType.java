package com.petlis.entity.dict;

import com.petlis.entity.base.ActivableEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.Date;

@Data
@Entity
@Table(name = "lin_lb_infotesttype")
public class InfoTestType  {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InfoTestTypeId", nullable = false)
    private Integer infoTestTypeId;

    @Column(name = "TestName", nullable = false, length = 250)
    private String testName;

    @Column(name = "TestNO", nullable = false, length = 250)
    private String testNO;

    @Column(name = "Price", nullable = false)
    private Double price;

    @Column(name = "CtoG", nullable = false)
    private Double ctoG;

    @Column(name = "GtoC", nullable = false)
    private Double gtoC;

    @Column(name = "DiscountReal", nullable = false)
    private Double discountReal;

    @Column(name = "DiscountNext", nullable = false)
    private Double discountNext;

    @Column(name = "TextModel", columnDefinition = "longtext", nullable = false)
    private String textModel;

    @Column(name = "CreateDt", nullable = false)
    private Date createDt;

    @Column(name = "type")
    private Integer type;

    @Column(name = "IsActive")
    private Integer isActive;

    @Column(name = "createtime")
    private Integer createtime;

    @Column(name = "updatetime")
    private Integer updatetime;

    public Integer getId() {
        return infoTestTypeId;
    }

}
