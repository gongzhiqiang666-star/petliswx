package com.petlis.entity.dict;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 检测项目价格实体类
 * 对应数据库表: lin_lb_infotesttype_price
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "lin_lb_infotesttype_price")
public class InfoTestTypePrice {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IID", nullable = false)
    private Long iid;

    @Column(name = "InfoTestTypeid")
    private Long infoTestTypeId;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "IsActive", nullable = false)
    private Integer isActive;

    @Column(name = "createtime")
    private Integer createtime;

    @Column(name = "updatetime")
    private Integer updatetime;
}
