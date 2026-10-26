package com.petlis.repository;

import com.petlis.entity.business.ListDiagnosisProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 检测申请Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface ListDiagnosisProjectRepository extends JpaRepository<ListDiagnosisProject, Integer>, JpaSpecificationExecutor<ListDiagnosisProject> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ListDiagnosisProject p where p.listDiagnosisProjectId = :id")
    java.util.Optional<ListDiagnosisProject> findLockedById(@Param("id") Integer id);


    /**
     * 根据医院ID查询申请列表
     *
     * @param infoHospitalId 医院ID
     * @return 申请列表
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.infoHospitalId = :infoHospitalId ORDER BY ldp.createDt DESC")
    List<ListDiagnosisProject> findByInfoHospitalId(@Param("infoHospitalId") Integer infoHospitalId);

    /**
     * 根据样本状态查询申请列表
     *
     * @param sampleStatus 样本状态
     * @return 申请列表
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.sampleStatus = :sampleStatus ORDER BY ldp.createDt DESC")
    List<ListDiagnosisProject> findBySampleStatus(@Param("sampleStatus") Integer sampleStatus);

    /**
     * 根据报告编号查询
     *
     * @param reportNO 报告编号
     * @return 申请信息
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.reportNO = :reportNO")
    ListDiagnosisProject findByReportNO(@Param("reportNO") String reportNO);

    /**
     * 根据用户编号查询
     *
     * @param userNO 用户编号
     * @return 申请列表
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.userNO = :userNO ORDER BY ldp.createDt DESC")
    List<ListDiagnosisProject> findByUserNO(@Param("userNO") String userNO);

    /**
     * 根据收到样本日期范围查询
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 申请列表
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.receiveSampleDt BETWEEN :startDate AND :endDate ORDER BY ldp.receiveSampleDt DESC")
    List<ListDiagnosisProject> findByReceiveSampleDtBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 根据宠物姓名查询
     *
     * @param animalName 宠物姓名
     * @return 申请列表
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.animalName = :animalName ORDER BY ldp.createDt DESC")
    List<ListDiagnosisProject> findByAnimalName(@Param("animalName") String animalName);

    /**
     * 根据芯片号查询
     *
     * @param chipNO 芯片号
     * @return 申请列表
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.chipNO = :chipNO ORDER BY ldp.createDt DESC")
    List<ListDiagnosisProject> findByChipNO(@Param("chipNO") String chipNO);

    /**
     * 查询最新的申请记录
     *
     * @param limit 限制数量
     * @return 申请列表
     */
    @Query(value = "SELECT * FROM lin_lb_listdiagnosisproject ORDER BY CreateDt DESC LIMIT :limit", nativeQuery = true)
    List<ListDiagnosisProject> findRecentProjects(@Param("limit") int limit);

    /**
     * 统计指定状态的申请数量
     *
     * @param sampleStatus 样本状态
     * @return 数量
     */
    @Query("SELECT COUNT(ldp) FROM ListDiagnosisProject ldp WHERE ldp.sampleStatus = :sampleStatus")
    Long countBySampleStatus(@Param("sampleStatus") Integer sampleStatus);

    /**
     * 根据医院ID和状态查询申请列表
     *
     * @param infoHospitalId 医院ID
     * @param sampleStatus   样本状态
     * @return 申请列表
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.infoHospitalId = :infoHospitalId AND ldp.sampleStatus = :sampleStatus ORDER BY ldp.createDt DESC")
    List<ListDiagnosisProject> findByInfoHospitalIdAndSampleStatus(@Param("infoHospitalId") Integer infoHospitalId, @Param("sampleStatus") Integer sampleStatus);

    /**
     * 根据医院ID和创建时间范围查询申请列表
     *
     * @param infoHospitalId 医院ID
     * @param startDate      开始时间
     * @param endDate        结束时间
     * @return 申请列表
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.infoHospitalId = :infoHospitalId AND ldp.createDt BETWEEN :startDate AND :endDate ORDER BY ldp.createDt DESC")
    List<ListDiagnosisProject> findByInfoHospitalIdAndCreateDtBetween(@Param("infoHospitalId") Integer infoHospitalId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 查询最大ID
     * 用于生成UserNO
     *
     * @return 最大ID
     */
    @Query(value = "SELECT MAX(ListDiagnosisProjectId) FROM lin_lb_listdiagnosisproject", nativeQuery = true)
    Integer findMaxId();

    /**
     * 根据医院ID、状态和时间范围查询申请列表(支持分页)
     *
     * @param infoHospitalId 医院ID
     * @param sampleStatus   样本状态(可选)
     * @param startDate      开始时间(可选)
     * @param endDate        结束时间(可选)
     * @return 申请列表
     */
    @Query("SELECT ldp FROM ListDiagnosisProject ldp WHERE ldp.infoHospitalId = :infoHospitalId " +
            "AND (:sampleStatus IS NULL OR ldp.sampleStatus = :sampleStatus) " +
            "AND (:startDate IS NULL OR ldp.createDt >= :startDate) " +
            "AND (:endDate IS NULL OR ldp.createDt <= :endDate) " +
            "ORDER BY ldp.createDt DESC")
    List<ListDiagnosisProject> findByInfoHospitalIdWithFilters(
            @Param("infoHospitalId") Integer infoHospitalId,
            @Param("sampleStatus") Integer sampleStatus,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);
    @Query("SELECT p FROM ListDiagnosisProject p WHERE p.infoHospitalId=:hospitalId " +
        "AND (:status IS NULL OR p.sampleStatus=:status) AND (:startDate IS NULL OR p.createDt>=:startDate) " +
        "AND (:endDate IS NULL OR p.createDt<=:endDate) AND (:keyword IS NULL OR LOWER(p.animalName) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
        "OR LOWER(p.masterName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(p.userNO) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(p.chipNO) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    org.springframework.data.domain.Page<ListDiagnosisProject> findPagedOrders(@Param("hospitalId") Integer hospitalId,@Param("status") Integer status,
        @Param("startDate") Date startDate,@Param("endDate") Date endDate,@Param("keyword") String keyword,org.springframework.data.domain.Pageable pageable);

}
