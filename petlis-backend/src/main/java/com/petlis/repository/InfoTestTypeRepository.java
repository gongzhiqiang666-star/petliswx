package com.petlis.repository;

import com.petlis.entity.dict.InfoTestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 检测项目类型Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface InfoTestTypeRepository extends JpaRepository<InfoTestType, Integer>, JpaSpecificationExecutor<InfoTestType> {

    /**
     * 查询所有启用的检测项目
     *
     * @return 检测项目列表
     */
    @Query("SELECT itt FROM InfoTestType itt WHERE itt.isActive = 1 ORDER BY itt.testName")
    List<InfoTestType> findAllActive();

    /**
     * 根据类型查询检测项目
     *
     * @param type 类型 (0=other, 1=细胞��&组织病理, 2=过敏原, 3=微生物, 4=综合)
     * @return 检测项目列表
     */
    @Query("SELECT itt FROM InfoTestType itt WHERE itt.type = :type AND itt.isActive = 1 ORDER BY itt.testName")
    List<InfoTestType> findByType(@Param("type") Integer type);

    /**
     * 根据检测编号查询
     *
     * @param testNO 检测编号
     * @return 检测项目
     */
    @Query("SELECT itt FROM InfoTestType itt WHERE itt.testNO = :testNO AND itt.isActive = 1")
    InfoTestType findByTestNO(@Param("testNO") String testNO);

    /**
     * 根据检测名称模糊查询
     *
     * @param testName 关键词
     * @return 检测项目列表
     */
    @Query("SELECT itt FROM InfoTestType itt WHERE itt.testName LIKE %:testName% AND itt.isActive = 1")
    List<InfoTestType> findByTestNameContaining(@Param("testName") String testName);

    /**
     * 根据多个ID查询检测项目
     *
     * @param ids 检测项目ID列表
     * @return 检测项目列表
     */
    @Query("SELECT itt FROM InfoTestType itt WHERE itt.infoTestTypeId IN :ids AND itt.isActive = 1")
    List<InfoTestType> findByIdIn(@Param("ids") List<Integer> ids);

    /**
     * 分页查询检测项目（支持关键词搜索）
     *
     * @param keyword 搜索关键词（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT itt FROM InfoTestType itt WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR itt.testName LIKE %:keyword%)" +
           " AND itt.isActive = 1 " +
           "ORDER BY itt.infoTestTypeId")
    Page<InfoTestType> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 分页查询所有启用的检测项目
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT itt FROM InfoTestType itt WHERE itt.isActive = 1 ORDER BY itt.infoTestTypeId")
    Page<InfoTestType> findAllActive(Pageable pageable);
}
