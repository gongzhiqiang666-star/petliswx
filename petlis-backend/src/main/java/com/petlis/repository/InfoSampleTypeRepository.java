package com.petlis.repository;

import com.petlis.entity.dict.InfoSampleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 样本类型Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface InfoSampleTypeRepository extends JpaRepository<InfoSampleType, Integer>, JpaSpecificationExecutor<InfoSampleType> {

    /**
     * 查询所有启用的样本类型
     *
     * @return 样本类型列表
     */
    @Query("SELECT ist FROM InfoSampleType ist WHERE ist.isActive = 1 ORDER BY ist.infoSampleTypeId")
    List<InfoSampleType> findAllActive();

    /**
     * 根据样本名称查询
     *
     * @param sampleName 样本名称
     * @return 样本类型
     */
    @Query("SELECT ist FROM InfoSampleType ist WHERE ist.sampleName = :sampleName AND ist.isActive = 1")
    InfoSampleType findBySampleName(String sampleName);
}
