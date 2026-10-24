package com.petlis.repository;

import com.petlis.entity.business.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 宠物档案Repository接口
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Integer>, JpaSpecificationExecutor<Pet> {

    /**
     * 根据医院ID和宠物名称模糊查询
     *
     * @param hospitalId 医院ID
     * @param name 宠物名称(中文或英文)
     * @return 宠物档案列表
     */
    @Query("SELECT p FROM Pet p WHERE p.infoHospitalId = :hospitalId AND p.isActive = 1 " +
           "AND (p.animalName LIKE %:name% OR p.animalNameEn LIKE %:name%)")
    List<Pet> findByInfoHospitalIdAndAnimalNameContaining(@Param("hospitalId") Integer hospitalId,
                                                          @Param("name") String name);

    /**
     * 根据医院ID查询所有启用的宠物档案
     *
     * @param hospitalId 医院ID
     * @return 宠物档案列表
     */
    @Query("SELECT p FROM Pet p WHERE p.infoHospitalId = :hospitalId AND p.isActive = 1")
    List<Pet> findByInfoHospitalId(@Param("hospitalId") Integer hospitalId);

    /**
     * 根据医院ID分页查询宠物档案
     *
     * @param hospitalId 医院ID
     * @param pageable 分页参数
     * @return 宠物档案分页结果
     */
    @Query("SELECT p FROM Pet p WHERE p.infoHospitalId = :hospitalId AND p.isActive = 1")
    Page<Pet> findByInfoHospitalId(@Param("hospitalId") Integer hospitalId, Pageable pageable);

    /**
     * 统计医院下的宠物档案数量
     *
     * @param hospitalId 医院ID
     * @return 宠物档案数量
     */
    @Query("SELECT COUNT(p) FROM Pet p WHERE p.infoHospitalId = :hospitalId AND p.isActive = 1")
    Long countByInfoHospitalId(@Param("hospitalId") Integer hospitalId);

    /**
     * 根据芯片号查询宠物档案
     *
     * @param chipNo 芯片号
     * @return 宠物档案
     */
    @Query("SELECT p FROM Pet p WHERE p.chipNo = :chipNo AND p.isActive = 1")
    Pet findByChipNo(@Param("chipNo") String chipNo);
}
