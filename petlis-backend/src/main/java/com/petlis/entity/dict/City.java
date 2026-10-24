package com.petlis.entity.dict;

import com.petlis.entity.base.ActivableEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Data
@Entity
@Table(name = "lin_lb_city")
public class City  {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CityId", nullable = false)
    private Integer cityId;

    @Column(name = "CityName", nullable = false, length = 50)
    private String cityName;

    @Column(name = "AreaCode")
    private Integer areaCode;

    @Column(name = "Grade", length = 50)
    private String grade;

    @Column(name = "ZipCode")
    private Integer zipCode;

    @Column(name = "BelongCity_cityId")
    private Integer belongCityCityId;

    @Column(name = "IsActive")
    private Integer isActive;

    public Integer getId() {
        return cityId;
    }

}
