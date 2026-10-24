package com.petlis.entity.dict;

import com.petlis.entity.base.ActivableEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Data
@Entity
@Table(name = "lin_lb_infosampletype")
public class InfoSampleType  {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InfoSampleTypeId", nullable = false)
    private Integer infoSampleTypeId;

    @Column(name = "SampleName", nullable = false, length = 250)
    private String sampleName;

    @Column(name = "SampleName_en", nullable = false, length = 250)
    private String sampleNameEn;

    @Column(name = "IsActive")
    private Integer isActive;

    @Column(name = "createtime")
    private Integer createtime;

    @Column(name = "updatetime")
    private Integer updatetime;

    public Integer getId() {
        return infoSampleTypeId;
    }

}
