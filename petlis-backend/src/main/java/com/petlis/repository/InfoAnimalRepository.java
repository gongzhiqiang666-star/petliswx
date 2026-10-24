package com.petlis.repository;

import com.petlis.entity.dict.InfoAnimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 动物物种Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface InfoAnimalRepository extends JpaRepository<InfoAnimal, Integer>, JpaSpecificationExecutor<InfoAnimal> {

    /**
     * 查询所有启用的动物物种
     *
     * @return 启用的物种列表
     */
    @Query("SELECT ia FROM InfoAnimal ia WHERE ia.isActive = 1 ORDER BY ia.infoAnimalId")
    List<InfoAnimal> findAllActive();

    /**
     * 根据物种名称查询
     *
     * @param animalName 物种名称
     * @return 物种信息
     */
    @Query("SELECT ia FROM InfoAnimal ia WHERE ia.animalName = :animalName AND ia.isActive = 1")
    InfoAnimal findByAnimalName(@Param("animalName") String animalName);

    /**
     * 根据物种名称(英文)查询
     *
     * @param animalNameEn 物种英文名称
     * @return 物种信息
     */
    @Query("SELECT ia FROM InfoAnimal ia WHERE ia.animalNameEn = :animalNameEn AND ia.isActive = 1")
    InfoAnimal findByAnimalNameEn(@Param("animalNameEn") String animalNameEn);
}
