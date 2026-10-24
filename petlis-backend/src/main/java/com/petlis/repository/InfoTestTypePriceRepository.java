package com.petlis.repository;

import com.petlis.entity.dict.InfoTestTypePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 检测项目价格Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface InfoTestTypePriceRepository extends JpaRepository<InfoTestTypePrice, Long>, JpaSpecificationExecutor<InfoTestTypePrice> {

    /**
     * 根据检测项目ID查询价格列表
     *
     * @param infoTestTypeId 检测项目ID
     * @return 价格列表
     */
    @Query("SELECT ittp FROM InfoTestTypePrice ittp WHERE ittp.infoTestTypeId = :infoTestTypeId AND ittp.isActive = 1")
    List<InfoTestTypePrice> findByInfoTestTypeid(@Param("infoTestTypeId") Long infoTestTypeId);

    /**
     * 根据检测项目ID查询当前有效价格
     *
     * @param infoTestTypeId 检测项目ID
     * @return 价格信息
     */
    @Query("SELECT ittp FROM InfoTestTypePrice ittp WHERE ittp.infoTestTypeId = :infoTestTypeId AND ittp.isActive = 1 ORDER BY ittp.iid DESC LIMIT 1")
    Optional<InfoTestTypePrice> findActivePriceByTestTypeId(@Param("infoTestTypeId") Long infoTestTypeId);

    /**
     * 查询所有启用的价格信息
     *
     * @return 价格列表
     */
    @Query("SELECT ittp FROM InfoTestTypePrice ittp WHERE ittp.isActive = 1")
    List<InfoTestTypePrice> findAllActive();
}
