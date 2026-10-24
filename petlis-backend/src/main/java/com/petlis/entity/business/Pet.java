package com.petlis.entity.business;

import com.petlis.entity.base.ActivableEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宠物档案实体类
 * 对应数据库表: lin_lb_pet
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "lin_lb_pet")
public class Pet extends ActivableEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 宠物ID (主键)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /**
     * 宠物中文名
     */
    @Column(name = "animal_name", nullable = false, length = 100)
    private String animalName;

    /**
     * 宠物英文名
     */
    @Column(name = "animal_name_en", length = 100)
    private String animalNameEn;

    /**
     * 物种ID (关联lin_lb_infoanimal表)
     */
    @Column(name = "info_animal_id")
    private Integer infoAnimalId;

    /**
     * 品种ID (关联lin_lb_infovariety表)
     */
    @Column(name = "info_variety_id")
    private Integer infoVarietyId;

    /**
     * 性别ID (关联lin_lb_dictsex表)
     */
    @Column(name = "dict_sex_id")
    private Integer dictSexId;

    /**
     * 生日(格式: yyyy-MM-dd)
     */
    @Column(name = "birthday", length = 20)
    private String birthday;

    /**
     * 芯片号
     */
    @Column(name = "chip_no", length = 50)
    private String chipNo;

    /**
     * 畜主中文名
     */
    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    /**
     * 畜主英文名
     */
    @Column(name = "owner_name_en", length = 100)
    private String ownerNameEn;

    /**
     * 所属医院ID (关联lin_lb_infohospital表)
     */
    @Column(name = "info_hospital_id", nullable = false)
    private Integer infoHospitalId;

    @Override
    public Integer getId() {
        return id;
    }

}
