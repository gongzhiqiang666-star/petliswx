package com.petlis.mapper;

import com.petlis.common.enums.OrderStatusEnum;
import com.petlis.dto.response.OrderDetailResponse;
import com.petlis.dto.response.OrderResponse;
import com.petlis.entity.business.DiagnosisDetail;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.vo.OrderVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.petlis.repository.DiagnosisDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单数据转换Mapper
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final @Lazy DiagnosisDetailRepository detailRepository;

    /**
     * PDF报告基础URL（从配置文件读取）
     */
    @Value("${app.pdf.base-url}")
    private String pdfBaseUrl;

    /**
     * 实体转响应DTO
     *
     * @param entity 申请单实体
     * @return 响应DTO
     */
    public OrderResponse toResponse(ListDiagnosisProject entity) {
        if (entity == null) {
            return null;
        }

        OrderResponse response = new OrderResponse();
        response.setOrderId(entity.getListDiagnosisProjectId());
        response.setUserNO(entity.getUserNO());
        response.setCityId(entity.getCityId());
        response.setHospitalId(entity.getInfoHospitalId());
        response.setInfoAnimalId(entity.getInfoAnimalId());
        response.setInfoSampleTypeId(entity.getInfoSampleTypeId());
        response.setAnimalName(entity.getAnimalName());
        response.setAnimalNameEn(entity.getAnimalNameEn());
        response.setAnimalTypeId(entity.getAnimalTypeId());
        response.setAnimalType(entity.getAnimalType());
        response.setAnimalTypeEn(entity.getAnimalTypeEn());
        response.setAnimalWeight(entity.getAnimalWeight());
        response.setChipNO(entity.getChipNO());
        response.setAge(entity.getAge());
        response.setBirthday(entity.getBirthday());
        response.setDictSexId(entity.getDictSexId());
        response.setSampleArea(entity.getSampleArea());
        response.setMasterName(entity.getMasterName());
        response.setMasterNameEn(entity.getMasterNameEn());
        response.setCollectDt(entity.getCollectDt());
        response.setReceiveSampleDt(entity.getReceiveSampleDt());
        response.setTestFee(entity.getTestFee());
        response.setRemark(entity.getRemark());
        response.setSampleStatus(entity.getSampleStatus());
        response.setSampleStatusDesc(getStatusDesc(entity.getSampleStatus()));
        response.setCreateDt(entity.getCreateDt());
        response.setDoctorName(entity.getDoctorName());
        response.setMarketingName(entity.getMarketingName());

        // 计算检测项目数量和总价格
        List<DiagnosisDetail> details = detailRepository.findByListDiagnosisProjectId(entity.getListDiagnosisProjectId());
        if (details != null && !details.isEmpty()) {
            response.setItemCount(details.size());

            // 计算总价格：累加所有检测明细的TestPrice
            BigDecimal totalPrice = details.stream()
                .map(DiagnosisDetail::getTestPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            response.setTotalPrice(totalPrice);

            // 调试日志
            log.info("OrderMapper.toResponse - 订单ID: {}, 检测项目数: {}, 总价: {}",
                entity.getListDiagnosisProjectId(), details.size(), totalPrice);
        } else {
            log.warn("OrderMapper.toResponse - 订单ID: {} 没有检测明细", entity.getListDiagnosisProjectId());
        }

        return response;
    }

    /**
     * 实体列表转响应DTO列表
     *
     * @param entities 申请单实体列表
     * @return 响应DTO列表
     */
    public List<OrderResponse> toResponseList(List<ListDiagnosisProject> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 实体和明细转详情响应DTO
     *
     * @param entity  申请单实体
     * @param details 明细列表
     * @return 详情响应DTO
     */
    public OrderDetailResponse toDetailResponse(ListDiagnosisProject entity, List<DiagnosisDetail> details) {
        if (entity == null) {
            return null;
        }

        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrderId(entity.getListDiagnosisProjectId());
        response.setUserNO(entity.getUserNO());
        response.setCityId(entity.getCityId());
        response.setHospitalId(entity.getInfoHospitalId());
        response.setInfoAnimalId(entity.getInfoAnimalId());
        response.setInfoSampleTypeId(entity.getInfoSampleTypeId());
        response.setAnimalName(entity.getAnimalName());
        response.setAnimalNameEn(entity.getAnimalNameEn());
        response.setAnimalTypeId(entity.getAnimalTypeId());
        response.setAnimalType(entity.getAnimalType());
        response.setAnimalTypeEn(entity.getAnimalTypeEn());
        response.setAnimalWeight(entity.getAnimalWeight());
        response.setChipNO(entity.getChipNO());
        response.setAge(entity.getAge());
        response.setBirthday(entity.getBirthday());
        response.setDictSexId(entity.getDictSexId());
        response.setSampleArea(entity.getSampleArea());
        response.setMasterName(entity.getMasterName());
        response.setMasterNameEn(entity.getMasterNameEn());
        response.setCollectDt(entity.getCollectDt());
        response.setReceiveSampleDt(entity.getReceiveSampleDt());
        response.setSendSampleDt(entity.getSendSampleDt());
        response.setSendReportDt(entity.getSendReportDt());
        response.setTestFee(entity.getTestFee());
        response.setRemark(entity.getRemark());
        response.setSampleStatus(entity.getSampleStatus());
        response.setSampleStatusDesc(getStatusDesc(entity.getSampleStatus()));
        response.setCreateDt(entity.getCreateDt());
        response.setDoctorName(entity.getDoctorName());
        response.setMarketingName(entity.getMarketingName());

        // 转换明细列表
        if (details != null) {
            List<OrderDetailResponse.OrderItemResponse> items = details.stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
            response.setItems(items);
        }

        return response;
    }

    /**
     * 明细实体转明细响应DTO
     *
     * @param detail 明细实体
     * @return 明细响应DTO
     */
    private OrderDetailResponse.OrderItemResponse toItemResponse(DiagnosisDetail detail) {
        if (detail == null) {
            return null;
        }

        OrderDetailResponse.OrderItemResponse item = new OrderDetailResponse.OrderItemResponse();
        item.setItemId(detail.getDiagnosisDetailId());
        item.setGenterNO(detail.getGenterNO());
        item.setInfoTestTypeId(detail.getInfoTestTypeId());
        item.setTestPrice(detail.getTestPrice());
        item.setOrginalPrice(detail.getOrginalPrice());
        item.setOrginalDiscount(detail.getOrginalDiscount());
        item.setRealFee(detail.getRealFee());
        item.setDetailRemark(detail.getDetailRemark());
        item.setPayStatus2(detail.getPayStatus2());
        item.setDiagnosisStatus(detail.getDiagnosisStatus());
        item.setIsActive(detail.getIsActive());
        item.setCreateDt(detail.getCreateDt());

        // 映射PDF相关字段
        item.setPdfHttpUrl(detail.getPdfHttpUrl());
        item.setIsSendMail(detail.getIsSendMail());
        item.setSendMailDt(detail.getSendMailDt());

        // 构建PDF完整URL（使用推荐判断方法：IsSendMail = 1 AND PDFHttpUrl != ''）
        Integer isSendMail = detail.getIsSendMail();
        String pdfHttpUrl = detail.getPdfHttpUrl();

        // 判断PDF已生成：IsSendMail == 1 AND PDFHttpUrl不为空且不是默认路径
        if (isSendMail != null && isSendMail == 1
            && pdfHttpUrl != null
            && !pdfHttpUrl.isEmpty()
            && !pdfHttpUrl.equals("/Content/FilePdf/")) {

            // 从配置文件读取基础URL并拼接
            String fullUrl = pdfBaseUrl + pdfHttpUrl;
            item.setPdfFullUrl(fullUrl);

            log.debug("PDF已生成，构建完整URL: {}", fullUrl);
        } else {
            log.debug("PDF未生成，IsSendMail={}, PDFHttpUrl={}", isSendMail, pdfHttpUrl);
        }

        return item;
    }

    /**
     * 实体转VO
     *
     * @param entity 申请单实体
     * @return VO对象
     */
    public OrderVO toVO(ListDiagnosisProject entity) {
        if (entity == null) {
            return null;
        }

        OrderVO vo = new OrderVO();
        vo.setOrderId(entity.getListDiagnosisProjectId());
        vo.setUserNO(entity.getUserNO());
        vo.setCityId(entity.getCityId());
        vo.setHospitalId(entity.getInfoHospitalId());
        vo.setInfoAnimalId(entity.getInfoAnimalId());
        vo.setInfoSampleTypeId(entity.getInfoSampleTypeId());
        vo.setAnimalName(entity.getAnimalName());
        vo.setAnimalNameEn(entity.getAnimalNameEn());
        vo.setAnimalTypeId(entity.getAnimalTypeId());
        vo.setAnimalType(entity.getAnimalType());
        vo.setAnimalTypeEn(entity.getAnimalTypeEn());
        vo.setAnimalWeight(entity.getAnimalWeight());
        vo.setChipNO(entity.getChipNO());
        vo.setAge(entity.getAge());
        vo.setBirthday(entity.getBirthday());
        vo.setDictSexId(entity.getDictSexId());
        vo.setSampleArea(entity.getSampleArea());
        vo.setMasterName(entity.getMasterName());
        vo.setMasterNameEn(entity.getMasterNameEn());
        vo.setCollectDt(entity.getCollectDt());
        vo.setReceiveSampleDt(entity.getReceiveSampleDt());
        vo.setTestFee(entity.getTestFee());
        vo.setRemark(entity.getRemark());
        vo.setSampleStatus(entity.getSampleStatus());
        vo.setSampleStatusDesc(getStatusDesc(entity.getSampleStatus()));
        vo.setCreateDt(entity.getCreateDt());
        vo.setDoctorName(entity.getDoctorName());
        vo.setMarketingName(entity.getMarketingName());

        // 计算检测项目数量和总价格
        List<DiagnosisDetail> details = detailRepository.findByListDiagnosisProjectId(entity.getListDiagnosisProjectId());
        if (details != null && !details.isEmpty()) {
            vo.setItemCount(details.size());

            // 计算总价格：累加所有检测明细的TestPrice
            BigDecimal totalPrice = details.stream()
                .map(DiagnosisDetail::getTestPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            vo.setTotalPrice(totalPrice);

            // 调试日志
            log.info("OrderMapper.toVO - 订单ID: {}, 检测项目数: {}, 总价: {}",
                entity.getListDiagnosisProjectId(), details.size(), totalPrice);
        } else {
            log.warn("OrderMapper.toVO - 订单ID: {} 没有检测明细", entity.getListDiagnosisProjectId());
        }

        return vo;
    }

    /**
     * 获取状态描述
     *
     * @param status 状态码
     * @return 状态描述
     */
    private String getStatusDesc(Integer status) {
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(status);
        return statusEnum != null ? statusEnum.getDescription() : "未知状态";
    }
}
