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
 private final BusinessNotifications notifications;
 private final DraftService drafts;
 private final FeatureStore store;


    private final ListDiagnosisProjectRepository projectRepository;
    private final DiagnosisDetailRepository detailRepository;
    private final DiagnosisDetailInfoRepository detailInfoRepository;
    private final InfoTestTypePriceRepository priceRepository;
    private final InfoTestTypeRepository infoTestTypeRepository;
    private final OrderMapper orderMapper;
    private final InfoHospitalRepository hospitalRepository;
    private final com.petlis.common.security.ReviewPermission reviewPermission;
    private final CouponService couponService;

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

        if(request.getDraftId()!=null)drafts.validateForSubmission(request.getDraftId(),request.getDraftVersion());
        // 从当前登录用户获取hospitalId和cityId（统一从token获取）
        Integer hospitalId = getCurrentHospitalId();
        Integer cityId = getCurrentCityId();

        log.info("当前用户信息, hospitalId: {}, cityId: {}", hospitalId, cityId);

        // 验证cityId不为空（token中的cityId从医院信息获取）
        if (cityId == null) {
            log.error("城市ID为空, hospitalId: {}, 无法创建订单", hospitalId);
            throw new BusinessException("账号信息不完整，城市ID未设置，请联系管理员");
        }

        CouponService.Quote quote = couponService.lockQuote(request.getLaboratoryId(),
                request.getDiagnosisDetails() == null ? null : request.getDiagnosisDetails().stream()
                        .map(OrderCreateRequest.DiagnosisDetailItem::getInfoTestTypeId).toList(), request.getUserCouponId());

        String userNO = "PENDING-" + java.util.UUID.randomUUID().toString().replace("-", "");

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
        project.setAnimalWeight(request.getAnimalWeight()==null?"":request.getAnimalWeight());
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

        // 无论现结还是月结，提交后先审核，不提前收费。
        project.setSampleStatus(OrderStatusEnum.PENDING_REVIEW.getCode());
        project.setCreateDt(new Date());
        project.setDoctorName(request.getDoctorName() != null ? request.getDoctorName() : "");
        project.setMarketingName(request.getMarketingName() != null ? request.getMarketingName() : "");
        project.setCreateSampleId(getCurrentUserId());
        project.setReportNO(userNO); // 生成德国报告编号
        // 病例诊断结果字段(数据库允许为空,但实体类要求非空)
        project.setDiagnosticResults("");

        // 保存申请单
        project = projectRepository.saveAndFlush(project);
        project.setUserNO(String.format("55550-%d", project.getListDiagnosisProjectId()));
        project.setReportNO(generateReportNO(project.getListDiagnosisProjectId()));

        saveItems(project, request, quote, 0);

        project.setTestFee(quote.payableAmount().doubleValue());
        projectRepository.save(project);
        InfoHospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new BusinessException("医院不存在"));
        couponService.recordOrder(project.getListDiagnosisProjectId(), quote, Integer.valueOf(1).equals(hospital.getPayType()));

        log.info("检测申请创建成功, 申请单ID: {}, UserNO: {}", project.getListDiagnosisProjectId(), userNO);
        if(request.getDraftId()!=null)drafts.consume(request.getDraftId(),request.getDraftVersion());
        notifications.order(project.getListDiagnosisProjectId(),getCurrentUserId(),project.getSampleStatus());
        return project.getListDiagnosisProjectId();
    }

    private void saveItems(ListDiagnosisProject project, OrderCreateRequest request, CouponService.Quote quote, long sequenceBase) {
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

                BigDecimal price = quote.prices().get(testTypeId);
                BigDecimal originalDiscount = BigDecimal.valueOf(10); // 默认折扣10

                // 生成GenterNO: 格式"{orderId}-{序号}"
                String genterNO = String.format("%d-%d", project.getListDiagnosisProjectId(), sequenceBase + i + 1);

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

    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void resubmit(Integer orderId, OrderCreateRequest request) {
        ListDiagnosisProject project=projectRepository.findLockedById(orderId)
                .orElseThrow(()->new ResourceNotFoundException("申请单",orderId));
        if(!java.util.Objects.equals(project.getInfoHospitalId(),getCurrentHospitalId())
                || !java.util.Objects.equals(project.getCreateSampleId(),getCurrentUserId()))throw new BusinessException(403,"仅原申请医生可重新提交此送检单");
        if((!Integer.valueOf(1).equals(project.getSampleStatus()) && !Integer.valueOf(-2).equals(project.getSampleStatus()))
                || Integer.valueOf(1).equals(project.getIsPayMoney()))throw new BusinessException(409,"仅未支付的驳回单或草稿可编辑重提");
        if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_wechat_payment WHERE order_id=? AND status IN (0,1))",Boolean.class,orderId)))throw new BusinessException("请先核对并关闭原支付记录");
        if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND (IsSendMail=1 OR DiagnosisStatus>0 OR PayStatus2<>0))",Boolean.class,orderId)))throw new BusinessException("已有检测、报告或结算记录，请联系实验室处理");
        if(request.getDraftId()!=null)drafts.validateForSubmission(request.getDraftId(),request.getDraftVersion());
        couponService.release(orderId);
        CouponService.Quote quote=couponService.lockQuote(request.getLaboratoryId(),request.getDiagnosisDetails()==null?null:request.getDiagnosisDetails().stream().map(OrderCreateRequest.DiagnosisDetailItem::getInfoTestTypeId).toList(),request.getUserCouponId());
        // Existing details remain as void history; a new serial number is never reused.
        if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_invoice_order WHERE order_id=?)",Boolean.class,orderId)))throw new BusinessException("已有开票记录，请联系实验室处理");
        long count=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",Long.class,orderId);
        var oldItems=detailRepository.findActiveDetailsByProjectId(orderId);
        for(var item:oldItems){item.setIsCheckPass(1);detailRepository.save(item);}
        project.setInfoAnimalId(request.getInfoAnimalId());
        project.setInfoSampleTypeId(request.getInfoSampleTypeId());
        project.setAnimalName(request.getAnimalName());
        project.setAnimalNameEn(request.getAnimalNameEn());
        project.setAnimalTypeId(request.getAnimalTypeId());
        project.setAnimalType(request.getAnimalType());
        project.setAnimalTypeEn(request.getAnimalTypeEn());
        project.setAnimalWeight(request.getAnimalWeight()==null?"":request.getAnimalWeight());
        project.setChipNO(request.getChipNO() != null ? request.getChipNO() : "");
        project.setAge(request.getAge());
        project.setBirthday(request.getBirthday());
        project.setDictSexId(request.getDictSexId());
        project.setSampleArea(request.getSampleArea());
        project.setMasterName(request.getMasterName());
        project.setMasterNameEn(request.getMasterNameEn());
        project.setCollectDt(request.getCollectDt());

        project.setCityId(getCurrentCityId());
        project.setReceiveSampleDt(request.getReceiveSampleDt()==null?getDefaultDate():request.getReceiveSampleDt());
        project.setDoctorName(request.getDoctorName()==null?"":request.getDoctorName());
        project.setMarketingName(request.getMarketingName()==null?"":request.getMarketingName());
        project.setRemark(request.getRemark()==null?"":request.getRemark());
        saveItems(project,request,quote,count);
        project.setTestFee(quote.payableAmount().doubleValue());
        project.setSampleStatus(OrderStatusEnum.PENDING_REVIEW.getCode());
        projectRepository.save(project);
        var hospital=hospitalRepository.findById(project.getInfoHospitalId()).orElseThrow(()->new BusinessException("医院不存在"));
        couponService.reviseOrder(orderId,quote,Integer.valueOf(1).equals(hospital.getPayType()));
        if(request.getDraftId()!=null)drafts.consume(request.getDraftId(),request.getDraftVersion());
        notifications.order(orderId,getCurrentUserId(),0);
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
        project.setCityId(getCurrentCityId());
        project.setInfoAnimalId(request.getInfoAnimalId());
        project.setInfoSampleTypeId(request.getInfoSampleTypeId());
        project.setAnimalName(request.getAnimalName());
        project.setAnimalNameEn(request.getAnimalNameEn());
        project.setAnimalTypeId(request.getAnimalTypeId());
        project.setAnimalType(request.getAnimalType());
        project.setAnimalTypeEn(request.getAnimalTypeEn());
        project.setAnimalWeight(request.getAnimalWeight()==null?"":request.getAnimalWeight());
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
        ListDiagnosisProject project = projectRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("申请单", orderId));

        boolean reviewer = reviewPermission.isReviewer();
        if (!reviewer && !java.util.Objects.equals(project.getInfoHospitalId(), getCurrentHospitalId())) {
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

        if((currentStatusEnum==OrderStatusEnum.DRAFT || currentStatusEnum==OrderStatusEnum.REVIEW_FAILED) && targetStatusEnum==OrderStatusEnum.PENDING_REVIEW)throw new BusinessException("请编辑资料并重新核价后提交");
        // 医生仅可提交草稿/驳回单或取消未结算订单；审核和出报告由审核人员完成。
        boolean submit = (currentStatusEnum == OrderStatusEnum.DRAFT || currentStatusEnum == OrderStatusEnum.REVIEW_FAILED)
                && targetStatusEnum == OrderStatusEnum.PENDING_REVIEW;
        boolean cancel = targetStatusEnum == OrderStatusEnum.CANCELLED && !Integer.valueOf(1).equals(project.getIsPayMoney());
        if (!reviewer && !submit && !cancel) {
            throw new BusinessException(403, "不能自行审核、确认支付或发布报告");
        }
        if (currentStatusEnum == OrderStatusEnum.PENDING_PAYMENT && targetStatusEnum == OrderStatusEnum.WAITING_REPORT) {
            throw new BusinessException("请通过已核验的支付回调完成结算");
        }
        if (currentStatusEnum == OrderStatusEnum.PENDING_REVIEW
                && (targetStatusEnum == OrderStatusEnum.PENDING_PAYMENT || targetStatusEnum == OrderStatusEnum.WAITING_REPORT)) {
            InfoHospital hospital = hospitalRepository.findById(project.getInfoHospitalId())
                    .orElseThrow(() -> new BusinessException("医院不存在"));
            boolean monthly = Integer.valueOf(1).equals(hospital.getPayType());
            boolean noPayment = project.getTestFee() != null && project.getTestFee() == 0d;
            if ((monthly || noPayment) != (targetStatusEnum == OrderStatusEnum.WAITING_REPORT)) {
                throw new BusinessException(monthly ? "月结客户审核后挂账" : "现结客户审核后应进入待支付");
            }
        }

        // 更新状态
        if (targetStatusEnum == OrderStatusEnum.CANCELLED || targetStatusEnum == OrderStatusEnum.REVIEW_FAILED) couponService.release(orderId);
        if (currentStatusEnum == OrderStatusEnum.PENDING_REVIEW && targetStatusEnum == OrderStatusEnum.WAITING_REPORT) couponService.consume(orderId);

        if (currentStatusEnum == OrderStatusEnum.PENDING_REVIEW && targetStatusEnum == OrderStatusEnum.WAITING_REPORT
                && project.getTestFee()!=null && project.getTestFee()==0d) project.setIsPayMoney(1);
        if(currentStatusEnum!=OrderStatusEnum.PENDING_REVIEW || targetStatusEnum==OrderStatusEnum.CANCELLED)notifications.order(orderId,project.getCreateSampleId(),targetStatus);
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
        Integer hospitalId = getCurrentHospitalId();

        int page=Math.max(0,request.getPage()==null?0:request.getPage());
        int size=Math.max(1,Math.min(100,request.getSize()==null?10:request.getSize()));
        Integer status=Integer.valueOf(-1).equals(request.getStatus())?null:request.getStatus();
        if(status!=null && OrderStatusEnum.fromCode(status)==null)throw new BusinessException("订单状态无效");
        String keyword=request.getKeyword()==null || request.getKeyword().isBlank()?null:request.getKeyword().trim();
        if(keyword!=null && keyword.length()>100)throw new BusinessException("搜索词过长");
        var pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createDt").and(Sort.by(Sort.Direction.DESC,"listDiagnosisProjectId")));
        return projectRepository.findPagedOrders(hospitalId,status,request.getStartDate(),request.getEndDate(),keyword,pageable).map(orderMapper::toResponse);

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

        var extensions=store.jdbc().queryForList("SELECT e.laboratory_id,l.name,e.user_coupon_id,e.review_reason FROM lin_order_extension e JOIN lin_laboratory l ON l.id=e.laboratory_id WHERE e.order_id=?",orderId);
        if(!extensions.isEmpty()){var e=extensions.get(0);response.setLaboratoryId(((Number)e.get("laboratory_id")).intValue());response.setLaboratoryName((String)e.get("name"));response.setUserCouponId(e.get("user_coupon_id")==null?null:((Number)e.get("user_coupon_id")).longValue());response.setReviewReason((String)e.get("review_reason"));}
        response.setCanResubmit(java.util.Objects.equals(project.getCreateSampleId(),getCurrentUserId()) && (Integer.valueOf(1).equals(project.getSampleStatus()) || Integer.valueOf(-2).equals(project.getSampleStatus())) && !Integer.valueOf(1).equals(project.getIsPayMoney()));
        response.setReviewHistory(store.jdbc().queryForList("SELECT id,title,message,created_at AS createdAt FROM lin_notification_event WHERE user_id=? AND event_type='order' AND JSON_EXTRACT(template_data,'$.orderId')=? ORDER BY id DESC LIMIT 20",getCurrentUserId(),orderId));

        // 计算并设置总费用（汇总所有检测明细的TestPrice）
        BigDecimal totalFee = details.stream()
                .map(DiagnosisDetail::getTestPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTestFee(project.getTestFee() == null ? totalFee.doubleValue() : project.getTestFee());

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
