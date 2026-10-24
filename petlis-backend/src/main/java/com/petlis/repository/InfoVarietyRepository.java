package com.petlis.repository;

import com.petlis.entity.dict.InfoVariety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 品种信息Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface InfoVarietyRepository extends JpaRepository<InfoVariety, Long>, JpaSpecificationExecutor<InfoVariety> {

    /**
     * 根据中文名称查询品种
     *
     * @param varietyCN 品种中文名
     * @return 品种信息
     */
    @Query("SELECT iv FROM InfoVariety iv WHERE iv.varietyCn = :varietyCN")
    InfoVariety findByVarietyCN(@Param("varietyCN") String varietyCN);

    /**
     * 根据英文名称查询品种
     *
     * @param varietyEN 品种英文名
     * @return 品种信息
     */
    @Query("SELECT iv FROM InfoVariety iv WHERE iv.varietyEn = :varietyEN")
    InfoVariety findByVarietyEN(@Param("varietyEN") String varietyEN);

    /**
     * 根据中文名称模糊查询
     *
     * @param keyword 关键词
     * @return 品种列表
     */
    @Query("SELECT iv FROM InfoVariety iv WHERE iv.varietyCn LIKE %:keyword%")
    List<InfoVariety> findByVarietyCNContaining(@Param("keyword") String keyword);

    /**
     * 根据英文名称模糊查询
     *
     * @param keyword 关键词
     * @return 品种列表
     */
    @Query("SELECT iv FROM InfoVariety iv WHERE iv.varietyEn LIKE %:keyword%")
    List<InfoVariety> findByVarietyENContaining(@Param("keyword") String keyword);

    /**
     * 查询所有品种(按中文名排序)
     *
     * @return 品种列表
     */
    @Query("SELECT iv FROM InfoVariety iv ORDER BY iv.varietyCn")
    List<InfoVariety> findAllOrderByVarietyCN();
}
