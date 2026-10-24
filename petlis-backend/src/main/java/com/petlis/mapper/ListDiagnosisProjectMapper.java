package com.petlis.mapper;

import com.petlis.dto.response.OrderDetailResponse;

/**
 * 订单Mapper
 * 用于查询订单详情
 *
 * 注意：此文件暂时注释掉MyBatis注解，因为项目使用Spring Data JPA
 *
 * @author PETLIS
 * @since 1.0.0
 */
// @Mapper  // 暂时注释，使用Spring Data JPA
public interface ListDiagnosisProjectMapper {

    /**
     * 查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情（Map格式）
     */
    // @Select("SELECT " +
    //         "ldp.ListDiagnosisProjectId as orderId, " +
    //         "ldp.UserNO as userNO, " +
    //         "ldp.AnimalName as animalName, " +
    //         "ldp.AnimalName_en as animalNameEn, " +
    //         "ldp.SampleStatus as sampleStatus, " +
    //         "ldp.InfoHospitalId as infoHospitalId, " +
    //         "ldp.MasterName as masterName " +
    //         "FROM lin_lb_listdiagnosisproject ldp " +
    //         "WHERE ldp.ListDiagnosisProjectId = #{orderId}")
    OrderDetailResponse findOrderDetailById(Integer orderId);
}
