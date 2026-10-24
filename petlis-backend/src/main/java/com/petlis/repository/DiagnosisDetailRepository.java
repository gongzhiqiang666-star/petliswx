package com.petlis.repository;

import com.petlis.entity.business.DiagnosisDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 检测明细Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface DiagnosisDetailRepository extends JpaRepository<DiagnosisDetail, Integer>, JpaSpecificationExecutor<DiagnosisDetail> {

    /**
     * 根据申请单ID查询明细列表
     *
     * @param listDiagnosisProjectId 申请单ID
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.listDiagnosisProjectId = :listDiagnosisProjectId ORDER BY dd.createDt DESC")
    List<DiagnosisDetail> findByListDiagnosisProjectId(@Param("listDiagnosisProjectId") Integer listDiagnosisProjectId);

    /**
     * 根据检测项目ID查询明细列表
     *
     * @param infoTestTypeId 检测项目ID
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.infoTestTypeId = :infoTestTypeId ORDER BY dd.createDt DESC")
    List<DiagnosisDetail> findByInfoTestTypeId(@Param("infoTestTypeId") Integer infoTestTypeId);

    /**
     * 根据是否作废查询
     *
     * @param isCheckPass 是否作废 (0=有效, 1=作废)
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.isCheckPass = :isCheckPass ORDER BY dd.createDt DESC")
    List<DiagnosisDetail> findByIsCheckPass(@Param("isCheckPass") Integer isCheckPass);

    /**
     * 根据支付状态查询
     *
     * @param payStatus2 支付状态 (0=未结, 1=现金, 2=转账, 3=网银已支付, 4=忽略)
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.payStatus2 = :payStatus2 ORDER BY dd.createDt DESC")
    List<DiagnosisDetail> findByPayStatus2(@Param("payStatus2") Integer payStatus2);

    /**
     * 根据是否激活查询
     *
     * @param isActive 是否激活 (0=禁用, 1=启用)
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.isActive = :isActive ORDER BY dd.createDt DESC")
    List<DiagnosisDetail> findByIsActive(@Param("isActive") Integer isActive);

    /**
     * 根据是否已发送邮件查询
     *
     * @param isSendMail 是否已发送邮件
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.isSendMail = :isSendMail ORDER BY dd.createDt DESC")
    List<DiagnosisDetail> findByIsSendMail(@Param("isSendMail") Integer isSendMail);

    /**
     * 查询有效的明细列表(未作废且已激活)
     *
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.isCheckPass = 0 AND dd.isActive = 1 ORDER BY dd.createDt DESC")
    List<DiagnosisDetail> findActiveDetails();

    /**
     * 根据申��单ID查询有效明细
     *
     * @param listDiagnosisProjectId 申请单ID
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.listDiagnosisProjectId = :listDiagnosisProjectId AND dd.isCheckPass = 0 AND dd.isActive = 1")
    List<DiagnosisDetail> findActiveDetailsByProjectId(@Param("listDiagnosisProjectId") Integer listDiagnosisProjectId);

    /**
     * 统计指定申请单的有效明细数量
     *
     * @param listDiagnosisProjectId 申请单ID
     * @return 数量
     */
    @Query("SELECT COUNT(dd) FROM DiagnosisDetail dd WHERE dd.listDiagnosisProjectId = :listDiagnosisProjectId AND dd.isCheckPass = 0 AND dd.isActive = 1")
    Long countActiveDetailsByProjectId(@Param("listDiagnosisProjectId") Integer listDiagnosisProjectId);

    /**
     * 根据申请单ID查询明细的最大序号
     * 用于生成GenterNO
     *
     * @param listDiagnosisProjectId 申请单ID
     * @return 最大序号
     */
    @Query("SELECT COUNT(dd) FROM DiagnosisDetail dd WHERE dd.listDiagnosisProjectId = :listDiagnosisProjectId")
    Long countDetailsByProjectId(@Param("listDiagnosisProjectId") Integer listDiagnosisProjectId);

    /**
     * 计算指定申请单的检测总价格
     *
     * @param listDiagnosisProjectId 申请单ID
     * @return 总价格
     */
    @Query("SELECT COALESCE(SUM(dd.testPrice), 0) FROM DiagnosisDetail dd WHERE dd.listDiagnosisProjectId = :listDiagnosisProjectId AND dd.isCheckPass = 0 AND dd.isActive = 1")
    java.math.BigDecimal sumTestPriceByProjectId(@Param("listDiagnosisProjectId") Integer listDiagnosisProjectId);

    /**
     * 根据申请单ID和是否作废查询明细列表
     *
     * @param listDiagnosisProjectId 申请单ID
     * @param isCheckPass 是否作废 (0=有效, 1=作废)
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.listDiagnosisProjectId = :listDiagnosisProjectId AND dd.isCheckPass = :isCheckPass ORDER BY dd.createDt DESC")
    List<DiagnosisDetail> findByListDiagnosisProjectIdAndIsCheckPass(
            @Param("listDiagnosisProjectId") Integer listDiagnosisProjectId,
            @Param("isCheckPass") Integer isCheckPass
    );

    /**
     * 根据申请单ID列表和检测状态查询明细列表
     *
     * @param listDiagnosisProjectIds 申请单ID列表
     * @param diagnosisStatus 检测状态 (0=待检测)
     * @return 明细列表
     */
    @Query("SELECT dd FROM DiagnosisDetail dd WHERE dd.listDiagnosisProjectId IN :listDiagnosisProjectIds AND dd.diagnosisStatus = :diagnosisStatus AND dd.isCheckPass = 0 ORDER BY dd.createDt DESC")
    List<DiagnosisDetail> findByListDiagnosisProjectIdInAndDiagnosisStatus(
            @Param("listDiagnosisProjectIds") List<Integer> listDiagnosisProjectIds,
            @Param("diagnosisStatus") Integer diagnosisStatus
    );
}
