package com.petlis.repository;

import com.petlis.entity.business.DiagnosisDetailInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 检测报告内容Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface DiagnosisDetailInfoRepository extends JpaRepository<DiagnosisDetailInfo, Integer>, JpaSpecificationExecutor<DiagnosisDetailInfo> {

    /**
     * 根据检测明细ID查询报告内容
     *
     * @param diagnosisdetailId 检测明细ID
     * @return 报告内容
     */
    Optional<DiagnosisDetailInfo> findById(Integer diagnosisdetailId);

    /**
     * 检查是否存在报告内容
     *
     * @param diagnosisdetailId 检测明细ID
     * @return 是否存在
     */
    boolean existsById(Integer diagnosisdetailId);

    /**
     * 根据检测明细ID删除报告内容
     *
     * @param diagnosisdetailId 检测明细ID
     */
    void deleteById(Integer diagnosisdetailId);
}
