package com.petlis.service.impl;

import com.petlis.common.enums.OrderStatusEnum;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.exception.ResourceNotFoundException;
import com.petlis.dto.request.OrderCreateRequest;
import com.petlis.dto.request.OrderQueryRequest;
import com.petlis.dto.request.OrderUpdateRequest;
import com.petlis.dto.response.OrderDetailResponse;
import com.petlis.dto.response.OrderResponse;
import com.petlis.entity.business.DiagnosisDetail;
import com.petlis.entity.business.DiagnosisDetailInfo;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.entity.dict.InfoTestTypePrice;
import com.petlis.entity.dict.InfoTestType;
import com.petlis.mapper.OrderMapper;
import com.petlis.repository.DiagnosisDetailRepository;
import com.petlis.repository.InfoTestTypeRepository;
import com.petlis.repository.DiagnosisDetailInfoRepository;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.InfoTestTypePriceRepository;
import com.petlis.repository.ListDiagnosisProjectRepository;
import com.petlis.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 检测申请Service实现类
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ListDiagnosisProjectRepository projectRepository;
    private final DiagnosisDetailRepository detailRepository;
    private final DiagnosisDetailInfoRepository detailInfoRepository;
    private final InfoTestTypePriceRepository priceRepository;
    private final InfoTestTypeRepository infoTestTypeRepository;
    private final OrderMapper orderMapper;
    private final InfoHospitalRepository hospitalRepository;

    /**
     * 创建检测申请
     *
     * @param request 创建请求
     * @return 申请单ID
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public Integer createOrder(OrderCreateRequest request) {
        log.info("创建检测申请, 宠物姓名: {}", request.getAnimalName());

        // 从当前登录用户获取hospitalId和cityId（统一从token获取）
        Integer hospitalId = getCurrentHospitalId();
        Integer cityId = getCurrentCityId();

        log.info("当前用户信息, hospitalId: {}, cityId: {}", hospitalId, cityId);

        // 验证cityId不为空（token中的cityId从医院信息获取）
        if (cityId == null) {
            log.error("城市ID为空, hospitalId: {}, 无法创建订单", hospitalId);
            throw new BusinessException("账号信息不完整，城市ID未设置，请联系管理员");
        }

        // 生成UserNO: 查询最大ID+1, 格式"55550-{id}"
        Integer maxId = projectRepository.findMaxId();
        Integer newId = (maxId == null ? 0 : maxId) + 1;
        String userNO = String.format("55550-%d", newId);

        // 创建申请单实体
        ListDiagnosisProject project = new ListDiagnosisProject();
        project.setCreateDataTypeIId(System.currentTimeMillis());
        project.setUserNO(userNO);
        project.setCityId(cityId);  // 使用token中的cityId
        project.setInfoHospitalId(hospitalId);  // 使用token中的hospitalId
        project.setInfoAnimalId(request.getInfoAnimalId());
        project.setInfoSampleTypeId(request.getInfoSampleTypeId());
        project.setAnimalName(request.getAnimalName());
        project.setAnimalNameEn(request.getAnimalNameEn());
        project.setAnimalTypeId(request.getAnimalTypeId());
        project.setAnimalType(request.getAnimalType());
        project.setAnimalTypeEn(request.getAnimalTypeEn());
        project.setAnimalWeight(request.getAnimalWeight());
        project.setChipNO(request.getChipNO() != null ? request.getChipNO() : "");
        project.setAge(request.getAge());
        project.setBirthday(request.getBirthday());
        project.setDictSexId(request.getDictSexId());
        project.setSampleArea(request.getSampleArea());
        project.setMasterName(request.getMasterName());
        project.setMasterNameEn(request.getMasterNameEn());
        project.setCollectDt(request.getCollectDt());
        // 日期字段默认值: "1900-01-01" (用户不会在创建时设置这些字段)
        project.setReceiveSampleDt(request.getReceiveSampleDt() != null ?
            request.getReceiveSampleDt() : getDefaultDate());
        project.setSendSampleDt(getDefaultDate());
        project.setSendReportDt(getDefaultDate());
        project.setTestFee(0.0);
        project.setDeliverySendGerFee(0.0);
        project.setDeliverySendCNFee(0.0);
        project.setInfoCheckReceivedId(0);
        project.setSaveMedium(0);
        project.setIsPayMoney(0);
        project.setRemark(request.getRemark() != null ? request.getRemark() : "");
        project.setIsSendNoticeMail(0);

        // 根据医院支付类型设置订单初始状态
        // PayType == 1: 月结客户 → 待审核(0)
        // PayType != 1: 次结客户 → 待支付(-3)
        Integer initialStatus;
        try {
            InfoHospital hospital = hospitalRepository.findById(hospitalId)
                    .orElse(null);
            if (hospital != null && hospital.getPayType() != null && hospital.getPayType() == 1) {
                // 月结客户，直接进入待审核状态
                initialStatus = OrderStatusEnum.PENDING_REVIEW.getCode();
                log.info("月结客户订单, hospitalId: {}, 初始状态: 待审核", hospitalId);
            } else {
                // 次结客户，进入待支付状态
                initialStatus = OrderStatusEnum.PENDING_PAYMENT.getCode();
                log.info("次结客户订单, hospitalId: {}, 初始状态: 待支付", hospitalId);
            }
        } catch (Exception e) {
            log.error("查询医院信息失败, hospitalId: {}, 默认使用待支付状态", hospitalId, e);
            initialStatus = OrderStatusEnum.PENDING_PAYMENT.getCode();
        }

        project.setSampleStatus(initialStatus);
        project.setCreateDt(new Date());
        project.setDoctorName(request.getDoctorName() != null ? request.getDoctorName() : "");
        project.setMarketingName(request.getMarketingName() != null ? request.getMarketingName() : "");
        project.setCreateSampleId(getCurrentUserId());
        project.setReportNO(generateReportNO(newId)); // 生成德国报告编号
        // 病例诊断结果字段(数据库允许为空,但实体类要求非空)
        project.setDiagnosticResults("");

        // 保存申请单
        project = projectRepository.save(project);

        // 保存检测明细
        if (request.getDiagnosisDetails() != null && !request.getDiagnosisDetails().isEmpty()) {
            log.info("开始保存检测明细, 数量: {}", request.getDiagnosisDetails().size());

            for (int i = 0; i < request.getDiagnosisDetails().size(); i++) {
                OrderCreateRequest.DiagnosisDetailItem item = request.getDiagnosisDetails().get(i);
                Integer testTypeId = item.getInfoTestTypeId();

                // 狂犬检测验证（检测项ID=1225）
                if (testTypeId.equals(1225)) {
                    if (project.getChipNO() == null || project.getChipNO().trim().isEmpty()) {
                        throw new BusinessException("狂犬检测必须填写芯片号");
                    }
                    if (project.getBirthday() == null) {
                        throw new BusinessException("狂犬检测必须填写宠物生日");
                    }
                    if (request.getRemark() == null || request.getRemark().trim().isEmpty()) {
                        throw new BusinessException("狂犬检测必须在备注中填写快递单号");
                    }
                }

                // 从InfoTestTypePrice获取价格
                List<InfoTestTypePrice> prices = priceRepository.findByInfoTestTypeid(testTypeId.longValue());
                if (prices == null || prices.isEmpty()) {
                    throw new BusinessException("未找到检测项目价格信息, 检测项ID: " + testTypeId);
                }

                // 使用启用的价格
                InfoTestTypePrice priceInfo = prices.stream()
                        .filter(p -> p.getIsActive() != null && p.getIsActive().equals(1))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("检测项目价格未启用, 检测项ID: " + testTypeId));

                BigDecimal price = BigDecimal.valueOf(priceInfo.getPrice());
                BigDecimal originalDiscount = BigDecimal.valueOf(10); // 默认折扣10

                // 生成GenterNO: 格式"{orderId}-{序号}"
                String genterNO = String.format("%d-%d", project.getListDiagnosisProjectId(), i + 1);

                // 创建明细实体
                DiagnosisDetail detail = new DiagnosisDetail();
                detail.setListDiagnosisProjectId(project.getListDiagnosisProjectId());
                detail.setGenterNO(genterNO);
                detail.setInfoTestTypeId(testTypeId);
                detail.setInfoTestTypeModelId(0);
                detail.setMailTitle("");
                detail.setCreateDt(new Date());
                detail.setIsChangeModelTemple(0);
                detail.setIsChangeMailModelTemple(0);
                detail.setIsSendMail(0);
                detail.setRealFee(0.0);
                detail.setIsCheckPass(0); // 有效
                detail.setPdfHttpUrl("");
                detail.setTestPrice(price); // 从价格表获取
                detail.setOrginalPrice(price); // 从价格表获取
                detail.setOrginalDiscount(originalDiscount); // 默认10
                detail.setDetailRemark(testTypeId.equals(1225) ? request.getRemark() : ""); // 狂犬检测需要填写备注
                detail.setPayStatus2(0); // 未结
                detail.setDiagnosisStatus(0); // 待检测
                detail.setIsActive(1); // 启用
                detail.setIsInvoiced(0); // 未开票
                detail.setSendMailDt(getDefaultDate()); // 默认值 1900-01-01 00:00:00

                // 保存明细
                detailRepository.save(detail);

                log.info("检测明细保存成功, 明细ID: {}, GenterNO: {}, 价格: {}", detail.getDiagnosisDetailId(), genterNO, price);
            }

            log.info("所有检测明细保存完成, 申请单ID: {}", project.getListDiagnosisProjectId());
        }

        log.info("检测申请创建成功, 申请单ID: {}, UserNO: {}", project.getListDiagnosisProjectId(), userNO);
        return project.getListDiagnosisProjectId();
    }

    /**
     * 添加检测项目到申请单
     *
     * @param orderId    申请单ID
     * @param testTypeId 检测项目ID
     * @param cityId     城市ID
     * @return 明细ID
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public Integer addOrderItem(Integer orderId, Integer testTypeId, Integer cityId) {
        log.info("添加检测项目, 申请单ID: {}, 检测项目ID: {}", orderId, testTypeId);

        // 验证申请单存在
        ListDiagnosisProject project = projectRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("申请单", orderId));

        // 验证权限
        Integer currentHospitalId = getCurrentHospitalId();
        if (!project.getInfoHospitalId().equals(currentHospitalId)) {
            throw new BusinessException(403, "无权操作此申请单");
        }

        // 验证状态: 只有草稿状态可以添加检测项目
        if (!project.getSampleStatus().equals(OrderStatusEnum.DRAFT.getCode())) {
            throw new BusinessException("只有草稿状态的申请单可以添加检测项目");
        }

        // 从InfoTestTypePrice获取价格
        List<InfoTestTypePrice> prices = priceRepository.findByInfoTestTypeid(testTypeId.longValue());
        if (prices == null || prices.isEmpty()) {
            throw new BusinessException("未找到检测项目价格信息");
        }

        // 使用启用的价格
        InfoTestTypePrice priceInfo = prices.stream()
                .filter(p -> p.getIsActive() != null && p.getIsActive().equals(1))
                .findFirst()
                .orElseThrow(() -> new BusinessException("检测项目价格未启用"));

        BigDecimal price = BigDecimal.valueOf(priceInfo.getPrice());
        BigDecimal originalPrice = price;
        BigDecimal originalDiscount = BigDecimal.ZERO;

        // 生成GenterNO: 格式"{orderId}-{序号}"
        Long count = detailRepository.countDetailsByProjectId(orderId);
        String genterNO = String.format("%d-%d", orderId, count + 1);

        // 验证狂犬检测规则 (检测项ID=1225时必须填写ChipNO、Birthday、DetailRemark)
        if (testTypeId.equals(1225)) {
            if (project.getChipNO() == null || project.getChipNO().trim().isEmpty()) {
                throw new BusinessException("狂犬检测必须填写芯片号");
            }
            if (project.getBirthday() == null) {
                throw new BusinessException("狂犬检测必须填写宠物生日");
            }
        }

        // 创建明细实体
        DiagnosisDetail detail = new DiagnosisDetail();
        detail.setListDiagnosisProjectId(orderId);
        detail.setGenterNO(genterNO);
        detail.setInfoTestTypeId(testTypeId);
        detail.setInfoTestTypeModelId(0);
        detail.setMailTitle("");
        detail.setCreateDt(new Date());
        detail.setIsChangeModelTemple(0);
        detail.setIsChangeMailModelTemple(0);
        detail.setIsSendMail(0);
        detail.setRealFee(0.0);
        detail.setIsCheckPass(0); // 有效
        detail.setPdfHttpUrl("");
        detail.setTestPrice(price);
        detail.setOrginalPrice(originalPrice);
        detail.setOrginalDiscount(originalDiscount);
        detail.setDetailRemark(""); // 必填,默认空字符串
        detail.setPayStatus2(0); // 未结
        detail.setDiagnosisStatus(0); // 待检测
        detail.setIsActive(1); // 启用
        detail.setIsInvoiced(0); // 未开票

        // 保存明细
        detail = detailRepository.save(detail);

        // 创建对应的Info记录
        DiagnosisDetailInfo detailInfo = new DiagnosisDetailInfo();
        detailInfo.setDiagnosisdetailId(detail.getDiagnosisDetailId());
        detailInfo.setMailReport("");
        detailInfo.setReportResult("");
        detailInfoRepository.save(detailInfo);

        log.info("检测项目添加成功, 明细ID: {}, GenterNO: {}", detail.getDiagnosisDetailId(), genterNO);
        return detail.getDiagnosisDetailId();
    }

    /**
     * 删除检测项目
     *
     * @param orderId 申请单ID
     * @param itemId  明细ID
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public void removeOrderItem(Integer orderId, Integer itemId) {
        log.info("删除检测项目, 申请单ID: {}, 明细ID: {}", orderId, itemId);

        // 验证申请单存在
        ListDiagnosisProject project = projectRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("申请单", orderId));

        // 验证权限
        Integer currentHospitalId = getCurrentHospitalId();
        if (!project.getInfoHospitalId().equals(currentHospitalId)) {
            throw new BusinessException(403, "无权操作此申请单");
        }

        // 验证状态
        if (!project.getSampleStatus().equals(OrderStatusEnum.DRAFT.getCode())) {
            throw new BusinessException("只有草稿状态的申请单可以删除检测项目");
        }

        // 查证明细
        DiagnosisDetail detail = detailRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("检测项目明细", itemId));

        // 验证明细属于该申请单
        if (!detail.getListDiagnosisProjectId().equals(orderId)) {
            throw new BusinessException("检测项目明细不属于该申请单");
        }

        // 删除对应的Info记录
        detailInfoRepository.deleteById(detail.getDiagnosisDetailId());

        // 删除明细(软删除,设置为作废)
        detail.setIsCheckPass(1); // 作废
        detailRepository.save(detail);

        log.info("检测项目删除成功, 明细ID: {}", itemId);
    }

    /**
     * 更新检测申请信息
     *
     * @param orderId 申请单ID
     * @param request 更新请求
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public void updateOrder(Integer orderId, OrderUpdateRequest request) {
        log.info("更新检测申请, 申请单ID: {}", orderId);

        // 验证申请单存在
        ListDiagnosisProject project = projectRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("申请单", orderId));

        // 验证权限
        Integer currentHospitalId = getCurrentHospitalId();
        if (!project.getInfoHospitalId().equals(currentHospitalId)) {
            throw new BusinessException(403, "无权操作此申请单");
        }

        // 验证状态: 只有草稿状态可以修改
        if (!project.getSampleStatus().equals(OrderStatusEnum.DRAFT.getCode())) {
            throw new BusinessException("只有草稿状态的申请单可以修改");
        }

        // 更新字段
        project.setCityId(request.getCityId());
        project.setInfoAnimalId(request.getInfoAnimalId());
        project.setInfoSampleTypeId(request.getInfoSampleTypeId());
        project.setAnimalName(request.getAnimalName());
        project.setAnimalNameEn(request.getAnimalNameEn());
        project.setAnimalTypeId(request.getAnimalTypeId());
        project.setAnimalType(request.getAnimalType());
        project.setAnimalTypeEn(request.getAnimalTypeEn());
        project.setAnimalWeight(request.getAnimalWeight());
        project.setChipNO(request.getChipNO() != null ? request.getChipNO() : "");
        project.setAge(request.getAge());
        project.setBirthday(request.getBirthday());
        project.setDictSexId(request.getDictSexId());
        project.setSampleArea(request.getSampleArea());
        project.setMasterName(request.getMasterName());
        project.setMasterNameEn(request.getMasterNameEn());
        project.setCollectDt(request.getCollectDt());
        project.setReceiveSampleDt(request.getReceiveSampleDt());
        project.setDoctorName(request.getDoctorName() != null ? request.getDoctorName() : "");
        project.setMarketingName(request.getMarketingName() != null ? request.getMarketingName() : "");
        project.setRemark(request.getRemark());

        // 保存更新
        projectRepository.save(project);

        log.info("检测申请更新成功, 申请单ID: {}", orderId);
    }

    /**
     * 更新申请状态
     *
     * @param orderId      申请单ID
     * @param targetStatus 目标状态
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public void updateOrderStatus(Integer orderId, Integer targetStatus) {
        log.info("更新申请状态, 申请单ID: {}, 目标状态: {}", orderId, targetStatus);

        // 验证申请单存在
        ListDiagnosisProject project = projectRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("申请单", orderId));

        // 验证权限
        Integer currentHospitalId = getCurrentHospitalId();
        if (!project.getInfoHospitalId().equals(currentHospitalId)) {
            throw new BusinessException(403, "无权操作此申请单");
        }

        // 获取当前状态和目标状态枚举
        OrderStatusEnum currentStatusEnum = OrderStatusEnum.fromCode(project.getSampleStatus());
        OrderStatusEnum targetStatusEnum = OrderStatusEnum.fromCode(targetStatus);

        if (currentStatusEnum == null || targetStatusEnum == null) {
            throw new BusinessException("无效的状态码");
        }

        // 验证状态流转是否合法
        if (!OrderStatusEnum.isValidTransition(currentStatusEnum, targetStatusEnum)) {
            throw new BusinessException(
                    String.format("不允许从%s状态变更为%s状态",
                            currentStatusEnum.getDescription(),
                            targetStatusEnum.getDescription())
            );
        }

        // 更新状态
        project.setSampleStatus(targetStatus);
        projectRepository.save(project);

        log.info("申请状态更新成功, 申请单ID: {}, 状态: {} -> {}",
                orderId, currentStatusEnum.getDescription(), targetStatusEnum.getDescription());
    }

    /**
     * 查询检测申请列表(分页)
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @Override
    public Page<OrderResponse> queryOrders(OrderQueryRequest request) {
        log.info("查询检测申请列表, 参数: {}", request);

        // 使用请求参数中的hospitalId,如果未提供则使用当前用户的医院ID
        Integer hospitalId = request.getHospitalId() != null
                ? request.getHospitalId()
                : getCurrentHospitalId();

        // 构建分页参数
        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 10,
                Sort.by(Sort.Direction.DESC, "createDt")
        );

        // 查询数据
        List<ListDiagnosisProject> projects = projectRepository.findByInfoHospitalIdWithFilters(
                hospitalId,
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate()
        );

        // 手动分页(由于使用自定义查询,不支持直接返回Page对象)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), projects.size());
        List<ListDiagnosisProject> pagedProjects = projects.subList(start, end);

        // 转换为DTO
        List<OrderResponse> responses = orderMapper.toResponseList(pagedProjects);

        // 构建Page对象(简化版)
        return new org.springframework.data.domain.PageImpl<>(responses, pageable, projects.size());
    }

    /**
     * 获取申请详情(包含明细)
     *
     * @param orderId 申请单ID
     * @return 申请详情
     */
    @Override
    public OrderDetailResponse getOrderDetail(Integer orderId) {
        log.info("获取申请详情, 申请单ID: {}", orderId);

        // 查询申请单
        ListDiagnosisProject project = projectRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("申请单", orderId));

        // 验证权限
        Integer currentHospitalId = getCurrentHospitalId();
        if (!project.getInfoHospitalId().equals(currentHospitalId)) {
            throw new BusinessException(403, "无权查看此申请单");
        }

        // 查询明细列表
        List<DiagnosisDetail> details = detailRepository.findActiveDetailsByProjectId(orderId);

        // 转换为DTO
        OrderDetailResponse response = orderMapper.toDetailResponse(project, details);

        // 加载检测项目名称
        if (response.getItems() != null) {
            for (OrderDetailResponse.OrderItemResponse item : response.getItems()) {
                if (item.getInfoTestTypeId() != null) {
                    InfoTestType testType = infoTestTypeRepository.findById(item.getInfoTestTypeId()).orElse(null);
                    if (testType != null) {
                        item.setTestTypeName(testType.getTestName());
                    }
                }
            }
        }

        // 计算并设置总费用（汇总所有检测明细的TestPrice）
        BigDecimal totalFee = details.stream()
                .map(DiagnosisDetail::getTestPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTestFee(totalFee.doubleValue());

        log.info("获取申请详情成功, 申请单ID: {}, 明细数量: {}, 总费用: {}", orderId, details.size(), totalFee);
        return response;
    }

    /**
     * 计算申请总费用
     *
     * @param orderId 申请单ID
     * @return 总费用
     */
    @Override
    public BigDecimal calculateTotalFee(Integer orderId) {
        log.info("计算申请总费用, 申请单ID: {}", orderId);

        // 验证申请单存在
        ListDiagnosisProject project = projectRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("申请单", orderId));

        // 验证权限
        Integer currentHospitalId = getCurrentHospitalId();
        if (!project.getInfoHospitalId().equals(currentHospitalId)) {
            throw new BusinessException(403, "无权查看此申请单");
        }

        // 汇总所有有效明细的TestPrice
        BigDecimal totalFee = detailRepository.sumTestPriceByProjectId(orderId);

        log.info("申请总费用计算完成, 申请单ID: {}, 总费用: {}", orderId, totalFee);
        return totalFee;
    }

    /**
     * 获取当前用户的医院ID
     * 从JWT上下文中获取
     *
     * @return 医院ID
     */
    private Integer getCurrentHospitalId() {
        try {
            return com.petlis.common.security.SecurityUtils.getCurrentHospitalId();
        } catch (Exception e) {
            log.error("获取当前用户hospitalId失败", e);
            throw new com.petlis.common.exception.BusinessException("无法获取当前用户医院信息");
        }
    }

    /**
     * 获取当前用户的城市ID
     * 从JWT上下文中获取（统一从token获取）
     *
     * @return 城市ID
     */
    private Integer getCurrentCityId() {
        try {
            Integer cityId = com.petlis.common.security.SecurityUtils.getCurrentCityId();
            if (cityId == null) {
                log.error("从token获取的cityId为null");
                throw new com.petlis.common.exception.BusinessException("账号城市信息未设置，请联系管理员维护医院信息");
            }
            return cityId;
        } catch (Exception e) {
            log.error("获取当前用户cityId失败", e);
            throw new com.petlis.common.exception.BusinessException("无法获取当前用户城市信息");
        }
    }

    /**
     * 获取当前用户ID
     * 从JWT上下文中获取
     *
     * @return 用户ID
     */
    private Integer getCurrentUserId() {
        try {
            return com.petlis.common.security.SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            throw new com.petlis.common.exception.BusinessException("无法获取当前用户信息");
        }
    }

    /**
     * 生成德国报告编号
     * 格式：RAB{year}{orderId}
     * 例如：RAB20251
     *
     * @param orderId 订单ID
     * @return 报告编号
     */
    private String generateReportNO(Integer orderId) {
        String year = String.valueOf(java.time.Year.now().getValue());
        return String.format("RAB%s%d", year, orderId);
    }

    /**
     * 获取默认日期 "1900-01-01"
     * 用于未设置的日期字段
     *
     * @return 默认日期
     */
    private Date getDefaultDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse("1900-01-01");
        } catch (ParseException e) {
            log.error("解析默认日期失败", e);
            return new Date(0); // 返回 epoch 时间作为后备
        }
    }
}
