package com.petlis.service.impl;

import com.petlis.dto.request.ReportQueryRequest;
import com.petlis.dto.response.ReportResponse;
import com.petlis.entity.business.DiagnosisDetail;
import com.petlis.entity.business.DiagnosisDetailInfo;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.entity.dict.InfoSampleType;
import com.petlis.entity.dict.InfoTestType;
import com.petlis.common.exception.BusinessException;
import com.petlis.mapper.ReportMapper;
import com.petlis.repository.DiagnosisDetailInfoRepository;
import com.petlis.repository.DiagnosisDetailRepository;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.InfoSampleTypeRepository;
import com.petlis.repository.InfoTestTypeRepository;
import com.petlis.repository.ListDiagnosisProjectRepository;
import com.petlis.service.FileService;
import com.petlis.service.ReportService;
import com.petlis.vo.ReportVO;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 检测报告Service实现类
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final DiagnosisDetailRepository diagnosisDetailRepository;
    private final DiagnosisDetailInfoRepository diagnosisDetailInfoRepository;
    private final ListDiagnosisProjectRepository listDiagnosisProjectRepository;
    private final InfoTestTypeRepository infoTestTypeRepository;
    private final InfoHospitalRepository infoHospitalRepository;
    private final InfoSampleTypeRepository infoSampleTypeRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ReportMapper reportMapper;
    private final FileService fileService;
    private final com.petlis.service.PdfService pdfService;

    @org.springframework.beans.factory.annotation.Value("${server.domain:}")
    private String serverDomain;
    @org.springframework.beans.factory.annotation.Value("${server.servlet.context-path:/api}")
    private String contextPath;

    /**
     * 查询检测报告列表(分页)
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> queryReports(ReportQueryRequest request) {
        log.info("查询检测报告列表, 参数: {}", request);

        // 构建查询条件
        Specification<DiagnosisDetail> spec = buildQuerySpecification(request);

        // 构建分页和排序
        Sort sort = Sort.by(Sort.Direction.DESC, "createDt");
        Pageable pageable = PageRequest.of(
                request.getPageNum() != null ? request.getPageNum() : 0,
                request.getPageSize() != null ? request.getPageSize() : 10,
                sort
        );

        // 执行查询
        Page<DiagnosisDetail> detailPage = diagnosisDetailRepository.findAll(spec, pageable);

        // 转换为DTO
        return detailPage.map(detail -> {
            ReportResponse response = reportMapper.toResponse(detail);

            // 加载关联信息
            loadRelatedInfo(response, detail);

            return response;
        });
    }

    /**
     * 获取报告详情
     *
     * @param reportId 报告ID(检测明细ID)
     * @return 报告详情
     */
    @Override
    @Transactional(readOnly = true)
    public ReportVO getReportDetail(Integer reportId) {
        log.info("获取报告详情, reportId: {}", reportId);

        DiagnosisDetail detail = diagnosisDetailRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("报告不存在"));
        ListDiagnosisProject project = requireOwnedProject(detail);
        InfoHospital hospital = requireHospital(project);
        boolean paymentRestricted = isPaymentRestricted(project, hospital);

        ReportVO vo = reportMapper.toVO(detail);
        reportMapper.updateProjectInfo(vo, project);
        vo.setHospitalName(hospital.getHospitalName());
        vo.setPaymentRestricted(paymentRestricted);
        vo.setPaid(project.getIsPayMoney() != null && project.getIsPayMoney() == 1);
        if (project.getInfoSampleTypeId() != null) {
            InfoSampleType sampleType = infoSampleTypeRepository.findById(project.getInfoSampleTypeId()).orElse(null);
            if (sampleType != null) {
                vo.setSampleTypeName(sampleType.getSampleName());
            }
        }
        List<Map<String, Object>> orderMetadata = jdbcTemplate.queryForList(
                "SELECT l.name AS laboratoryName, oe.settlement_mode AS settlementMode FROM lin_order_extension oe LEFT JOIN lin_laboratory l ON l.id = oe.laboratory_id WHERE oe.order_id = ?",
                project.getListDiagnosisProjectId()
        );
        if (!orderMetadata.isEmpty()) {
            Map<String, Object> metadata = orderMetadata.get(0);
            vo.setLaboratoryName((String) metadata.get("laboratoryName"));
            Object settlementMode = metadata.get("settlementMode");
            if (settlementMode != null) {
                vo.setSettlementMode(settlementMode.toString());
            }
        }
        if (paymentRestricted) {
            vo.setReportResult(null);
            vo.setMailReport(null);
            vo.setPdfHttpUrl(null);
        } else {
            loadReportContent(vo, reportId);
        }

        // 加载检测项目名称
        if (detail.getInfoTestTypeId() != null) {
            InfoTestType testType = infoTestTypeRepository.findById(detail.getInfoTestTypeId()).orElse(null);
            if (testType != null) {
                vo.setTestTypeName(testType.getTestName());
            }
        }

        return vo;
    }

    /**
     * 下载PDF报告
     *
     * @param reportId 报告ID(检测明细ID)
     * @return PDF文件响应
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadReportPdf(Integer reportId) {
        log.info("下载PDF报告, reportId: {}", reportId);

        // 查询检测明细获取PDF URL
        DiagnosisDetail detail = diagnosisDetailRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("报告不存在"));
        assertPdfAccessAllowed(detail);

        String pdfUrl = detail.getPdfHttpUrl();
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            throw new BusinessException("PDF报告不存在");
        }

        byte[] fileContent;
        try {
            fileContent = pdfService.getPdfWithAuth(reportId, null);
        } catch (java.io.IOException error) {
            throw new BusinessException(500, "PDF文件读取失败", error);
        }

        // 构建响应
        String filename = "报告_" + detail.getGenterNO() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(fileContent.length)
                .body(fileContent);
    }

    /**
     * 获取PDF报告URL
     *
     * @param reportId 报告ID(检测明细ID)
     * @return PDF文件URL
     */
    @Override
    @Transactional(readOnly = true)
    public URL getReportPdfUrl(Integer reportId) {
        log.info("获取PDF报告URL, reportId: {}", reportId);

        // 查询检测明细获取PDF URL
        DiagnosisDetail detail = diagnosisDetailRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("报告不存在"));
        assertPdfAccessAllowed(detail);

        String pdfUrl = detail.getPdfHttpUrl();
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            throw new BusinessException("PDF报告不存在");
        }

        try {
            return new URL(serverDomain + contextPath + "/pdf/view/" + reportId);
        } catch (Exception e) {
            log.error("解析PDF URL失败", e);
            throw new BusinessException("PDF报告URL无效");
        }
    }

    /**
     * 构建查询条件
     *
     * @param request 查询请求
     * @return 查询规范
     */
    private Specification<DiagnosisDetail> buildQuerySpecification(ReportQueryRequest request) {
        // 获取当前用户的医院ID（必须与当前操作员所属机构一致）
        Integer currentHospitalId = getCurrentHospitalId();
        log.info("报告查询筛选条件, 当前用户医院ID: {}", currentHospitalId);

        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // 只查询有效记录 (isCheckPass=0)
            predicates.add(cb.equal(root.get("isCheckPass"), 0));

            // 只查询已完成的报告 (DiagnosisStatus != 0，0表示检测中)
            // 注意：这里使用 NOT NULL 且 != 0，因为已完成可能有多种状态
            predicates.add(cb.notEqual(root.get("diagnosisStatus"), 0));

            // 【关键】使用子查询筛选当前用户所属机构的报告
            // 通过ListDiagnosisProjectId关联，筛选InfoHospitalId等于当前用户医院ID的记录
            jakarta.persistence.criteria.Subquery<Integer> subquery = query.subquery(Integer.class);
            jakarta.persistence.criteria.Root<ListDiagnosisProject> projectRoot = subquery.from(ListDiagnosisProject.class);
            subquery.select(projectRoot.get("listDiagnosisProjectId"));
            subquery.where(cb.equal(projectRoot.get("infoHospitalId"), currentHospitalId));

            // 主查询中，只查询在子查询结果中的记录
            predicates.add(cb.in(root.get("listDiagnosisProjectId")).value(subquery));

            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String keyword=request.getKeyword().trim();
                if(keyword.length()>100)throw new BusinessException("搜索关键词最多100个字符");
                // Escape LIKE wildcards so user input remains a literal substring.
                String pattern="%"+keyword.toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
                var matchingPets=query.subquery(Integer.class);
                var petProject=matchingPets.from(ListDiagnosisProject.class);
                matchingPets.select(petProject.get("listDiagnosisProjectId"));
                matchingPets.where(cb.equal(petProject.get("infoHospitalId"),currentHospitalId),cb.like(cb.lower(petProject.get("animalName")),pattern,'!'));
                var matchingTests=query.subquery(Integer.class);
                var test=matchingTests.from(InfoTestType.class);
                matchingTests.select(test.get("infoTestTypeId"));
                matchingTests.where(cb.like(cb.lower(test.get("testName")),pattern,'!'));
                predicates.add(cb.or(root.get("listDiagnosisProjectId").in(matchingPets),root.get("infoTestTypeId").in(matchingTests)));
            }

            // 按宠物ID筛选
            if (request.getPetId() != null) {
                // 需要通过关联的ListDiagnosisProject查询
                predicates.add(cb.equal(root.get("listDiagnosisProjectId"), request.getPetId()));
            }

            // 按状态筛选（用户主动筛选状态时）
            if (request.getStatus() != null) {
                if (request.getStatus() == 0) {
                    // 检测中（通常不会用到，因为默认已过滤）
                    predicates.add(cb.equal(root.get("diagnosisStatus"), 0));
                } else {
                    // 已完成
                    predicates.add(cb.notEqual(root.get("diagnosisStatus"), 0));
                }
            }

            // 按日期范围筛选
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                try {
                    Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(request.getStartDate());
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createDt"), startDate));
                } catch (Exception e) {
                    log.warn("解析开始日期失败: {}", request.getStartDate());
                }
            }

            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                try {
                    Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(request.getEndDate());
                    // 将结束日期设置为当天23:59:59
                    endDate = new Date(endDate.getTime() + 24 * 60 * 60 * 1000 - 1);
                    predicates.add(cb.lessThanOrEqualTo(root.get("createDt"), endDate));
                } catch (Exception e) {
                    log.warn("解析结束日期失败: {}", request.getEndDate());
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
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

    private ListDiagnosisProject requireOwnedProject(DiagnosisDetail detail) {
        ListDiagnosisProject project = listDiagnosisProjectRepository.findById(detail.getListDiagnosisProjectId())
                .orElseThrow(() -> new BusinessException("报告关联订单不存在"));
        if (!project.getInfoHospitalId().equals(getCurrentHospitalId())) {
            throw new BusinessException("无权访问此报告");
        }
        return project;
    }

    private InfoHospital requireHospital(ListDiagnosisProject project) {
        return infoHospitalRepository.findById(project.getInfoHospitalId())
                .orElseThrow(() -> new BusinessException("订单所属医院不存在"));
    }

    private boolean isPaymentRestricted(ListDiagnosisProject project, InfoHospital hospital) {
        var rows = jdbcTemplate.queryForList("SELECT settlement_mode FROM lin_order_extension WHERE order_id=?", project.getListDiagnosisProjectId());
        String settlementMode = rows.isEmpty() || rows.get(0).get("settlement_mode") == null ? null : rows.get(0).get("settlement_mode").toString();
        boolean monthly = settlementMode == null ? Integer.valueOf(1).equals(hospital.getPayType()) : "monthly".equalsIgnoreCase(settlementMode);
        return !monthly && !Integer.valueOf(1).equals(project.getIsPayMoney());
    }

    private void assertPdfAccessAllowed(DiagnosisDetail detail) {
        ListDiagnosisProject project = requireOwnedProject(detail);
        if (isPaymentRestricted(project, requireHospital(project))) {
            throw new BusinessException("订单未支付，报告暂未开放，请完成支付后查看");
        }
    }

    /**
     * 加载关联信息到Response
     *
     * @param response 响应对象
     * @param detail   检测明细
     */
    private void loadRelatedInfo(ReportResponse response, DiagnosisDetail detail) {
        // 加载申请单信息获取宠物信息
        ListDiagnosisProject project = listDiagnosisProjectRepository.findById(detail.getListDiagnosisProjectId())
                .orElse(null);
        if (project != null) {
            response.setPaymentRestricted(isPaymentRestricted(project, requireHospital(project)));
            if (Boolean.TRUE.equals(response.getPaymentRestricted())) {
                response.setPdfHttpUrl(null);
                response.setPdfFullUrl(null);
                response.setSecurePdfUrl(null);
            }
            response.setAnimalName(project.getAnimalName());
            response.setAnimalType(project.getAnimalType());
            response.setCollectDt(project.getCollectDt());
            response.setReportNO(project.getReportNO());

            // 添加更多报告相关信息
            response.setReceiveSampleDt(project.getReceiveSampleDt());
            response.setSendSampleDt(project.getSendSampleDt());
            response.setDoctorName(project.getDoctorName());

            // 报告相关日期
            response.setCreateDt(detail.getCreateDt());
            response.setSendMailDt(detail.getSendMailDt());

            log.debug("加载报告关联信息, reportId: {}, collectDt: {}, sendMailDt: {}, doctor: {}",
                detail.getDiagnosisDetailId(), project.getCollectDt(), detail.getSendMailDt(), project.getDoctorName());
        }

        // 加载检测项目名称
        if (detail.getInfoTestTypeId() != null) {
            InfoTestType testType = infoTestTypeRepository.findById(detail.getInfoTestTypeId()).orElse(null);
            if (testType != null) {
                response.setTestTypeName(testType.getTestName());
            }
        }
    }

    /**
     * 加载报告内容
     *
     * @param vo      视图对象
     * @param reportId 报告ID
     */
    private void loadReportContent(ReportVO vo, Integer reportId) {
        DiagnosisDetailInfo info = diagnosisDetailInfoRepository.findById(reportId).orElse(null);
        if (info != null) {
            vo.setReportResult(info.getReportResult());
            vo.setMailReport(info.getMailReport());
        }
    }
}
