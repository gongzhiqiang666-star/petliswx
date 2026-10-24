package com.petlis.entity.dict;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 动物物种实体类
 * 对应数据库表: lin_lb_infoanimal
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "lin_lb_infoanimal")
public class InfoAnimal {
    private static final long serialVersionUID = 1L;

    /**
     * 物种ID (主键)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InfoAnimalId", nullable = false)
    private Integer infoAnimalId;

    /**
     * 物种名称(中文)
     */
    @Column(name = "AnimalName", nullable = false, length = 250)
    private String animalName;

    /**
     * 物种名称(英文)
     */
    @Column(name = "AnimalName_en", nullable = false, length = 250)
    private String animalNameEn;

    /**
     * 是否启用
     */
    @Column(name = "IsActive", nullable = false)
    private Integer isActive;

    /**
     * 创建时间
     */
    @Column(name = "createtime")
    private Integer createtime;

    /**
     * 更新时间
     */
    @Column(name = "updatetime")
    private Integer updatetime;

    public Integer getId() {
        return infoAnimalId;
    }
}
