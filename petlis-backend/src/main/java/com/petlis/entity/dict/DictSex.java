package com.petlis.entity.dict;

import com.petlis.entity.base.ActivableEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * 性别字典实体类
 * 对应数据库表: lin_lb_dictsex
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "lin_lb_dictsex")
public class DictSex  {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DictSexId", nullable = false)
    private Integer dictSexId;

    @Column(name = "SexName", nullable = false, length = 50)
    private String sexName;

    @Column(name = "SexName_en", nullable = false, length = 50)
    private String sexNameEn;

    @Column(name = "IsActive")
    private Integer isActive;

    @Column(name = "createtime")
    private Integer createtime;

    @Column(name = "updatetime")
    private Integer updatetime;

    public Integer getId() {
        return dictSexId;
    }

}
