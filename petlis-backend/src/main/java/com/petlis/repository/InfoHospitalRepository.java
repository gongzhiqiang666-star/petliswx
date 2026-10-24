package com.petlis.repository;

import com.petlis.entity.dict.InfoHospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 医院信息Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface InfoHospitalRepository extends JpaRepository<InfoHospital, Integer>, JpaSpecificationExecutor<InfoHospital> {

    /**
     * 根据城市ID查询医院列表
     *
     * @param cityId 城市ID
     * @return 医院列表
     */
    @Query("SELECT ih FROM InfoHospital ih WHERE ih.cityId = :cityId AND ih.isActive = 1")
    List<InfoHospital> findByCityId(@Param("cityId") Integer cityId);

    /**
     * 根据医院名称查询
     *
     * @param hospitalName 医院名称
     * @return 医院信息
     */
    @Query("SELECT ih FROM InfoHospital ih WHERE ih.hospitalName = :hospitalName AND ih.isActive = 1")
    Optional<InfoHospital> findByHospitalName(@Param("hospitalName") String hospitalName);

    /**
     * 根据医院名称模糊查询
     *
     * @param keyword 关键词
     * @return 医院列表
     */
    @Query("SELECT ih FROM InfoHospital ih WHERE ih.hospitalName LIKE %:keyword% AND ih.isActive = 1")
    List<InfoHospital> findByHospitalNameContaining(@Param("keyword") String keyword);

    /**
     * 根据登录名查询
     *
     * @param loginName 登录名
     * @return 医院信息
     */
    @Query("SELECT ih FROM InfoHospital ih WHERE ih.loginName = :loginName AND ih.isActive = 1")
    Optional<InfoHospital> findByLoginName(@Param("loginName") String loginName);

    /**
     * 查询所有启用的医院
     *
     * @return 医院列表
     */
    @Query("SELECT ih FROM InfoHospital ih WHERE ih.isActive = 1 ORDER BY ih.hospitalName")
    List<InfoHospital> findAllActive();
}
