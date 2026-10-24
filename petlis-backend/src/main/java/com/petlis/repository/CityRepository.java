package com.petlis.repository;

import com.petlis.entity.dict.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 城市Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface CityRepository extends JpaRepository<City, Integer>, JpaSpecificationExecutor<City> {

    /**
     * 查询所有启用的城市
     *
     * @return 城市列表
     */
    @Query("SELECT c FROM City c WHERE c.isActive = 1 ORDER BY c.cityName")
    List<City> findAllActive();

    /**
     * 根据城市名称查询
     *
     * @param cityName 城市名称
     * @return 城市信息
     */
    @Query("SELECT c FROM City c WHERE c.cityName = :cityName AND c.isActive = 1")
    City findByCityName(String cityName);

    /**
     * 根据城市名称模糊查询
     *
     * @param keyword 关键词
     * @return 城市列表
     */
    @Query("SELECT c FROM City c WHERE c.cityName LIKE %:keyword% AND c.isActive = 1")
    List<City> findByCityNameContaining(String keyword);

    /**
     * 根据所属城市ID查询子城市
     *
     * @param belongCityCityId 所属城市ID
     * @return 城市列表
     */
    @Query("SELECT c FROM City c WHERE c.belongCityCityId = :belongCityCityId AND c.isActive = 1")
    List<City> findByBelongCityCityId(Integer belongCityCityId);
}
