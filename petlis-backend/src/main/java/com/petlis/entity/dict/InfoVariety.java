package com.petlis.entity.dict;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 动物品种实��类
 * 对应数据库表: lin_lb_infovariety
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "lin_lb_infovariety")
public class InfoVariety {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IID", nullable = false)
    private Long iid;

    @Column(name = "Variety_CN", nullable = false, length = 50)
    private String varietyCn;

    @Column(name = "Variety_EN", nullable = false, length = 50)
    private String varietyEn;

    @Column(name = "createtime")
    private Integer createtime;

    @Column(name = "updatetime")
    private Integer updatetime;
}
