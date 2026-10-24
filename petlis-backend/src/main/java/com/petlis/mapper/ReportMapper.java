package com.petlis.mapper;

import com.petlis.dto.response.ReportResponse;
import com.petlis.entity.business.DiagnosisDetail;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.vo.ReportVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

/**
 * 检测报告MapStruct映射器
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ReportMapper {

    /**
     * 将检测明细转换为报告响应DTO
     *
     * @param detail 检测明细
     * @return 报告响应
     */
    @Mapping(target = "reportId", source = "diagnosisDetailId")
    @Mapping(target = "orderId", source = "listDiagnosisProjectId")
    @Mapping(target = "statusText", source = "diagnosisStatus", qualifiedByName = "statusToText")
    @Mapping(target = "createTime", source = "createDt")
    ReportResponse toResponse(DiagnosisDetail detail);

    /**
     * 将检测明细转换为报告视图对象
     *
     * @param detail 检测明细
     * @return 报告视图对象
     */
    @Mapping(target = "reportId", source = "diagnosisDetailId")
    @Mapping(target = "orderId", source = "listDiagnosisProjectId")
    @Mapping(target = "statusText", source = "diagnosisStatus", qualifiedByName = "statusToText")
    @Mapping(target = "createTime", source = "createDt")
    ReportVO toVO(DiagnosisDetail detail);

    /**
     * 将申请单信息合并到报告视图对象
     *
     * @param vo 视图对象
     * @param project 申请单
     */
    @Mapping(target = "animalName", source = "animalName")
    @Mapping(target = "animalType", source = "animalType")
    @Mapping(target = "age", source = "age")
    @Mapping(target = "animalWeight", source = "animalWeight")
    @Mapping(target = "chipNO", source = "chipNO")
    @Mapping(target = "masterName", source = "masterName")
    @Mapping(target = "doctorName", source = "doctorName")
    @Mapping(target = "collectDt", source = "collectDt")
    @Mapping(target = "receiveSampleDt", source = "receiveSampleDt")
    @Mapping(target = "sendReportDt", source = "sendReportDt")
    @Mapping(target = "reportNO", source = "reportNO")
    @Mapping(target = "hospitalId", source = "infoHospitalId")
    void updateProjectInfo(@MappingTarget ReportVO vo, ListDiagnosisProject project);

    /**
     * 将状态码转换为状态文本
     *
     * @param status 状态码
     * @return 状态文本
     */
    @Named("statusToText")
    static String statusToText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 0 ? "检测中" : "已完成";
    }
}
