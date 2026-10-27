package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import com.petlis.dto.response.ReportResponse;
import com.petlis.entity.business.DiagnosisDetail;
import com.petlis.entity.business.DiagnosisDetailInfo;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.entity.dict.InfoSampleType;
import com.petlis.mapper.ReportMapper;
import com.petlis.repository.DiagnosisDetailInfoRepository;
import com.petlis.repository.DiagnosisDetailRepository;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.InfoSampleTypeRepository;
import com.petlis.repository.InfoTestTypeRepository;
import com.petlis.repository.ListDiagnosisProjectRepository;
import com.petlis.service.FileService;
import com.petlis.vo.ReportVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @Mock DiagnosisDetailRepository diagnosisDetailRepository;
    @Mock DiagnosisDetailInfoRepository diagnosisDetailInfoRepository;
    @Mock ListDiagnosisProjectRepository listDiagnosisProjectRepository;
    @Mock InfoTestTypeRepository infoTestTypeRepository;
    @Mock InfoHospitalRepository infoHospitalRepository;
    @Mock InfoSampleTypeRepository infoSampleTypeRepository;
    @Mock JdbcTemplate jdbcTemplate;
    @Mock ReportMapper reportMapper;
    @Mock FileService fileService;
    @Mock com.petlis.service.PdfService pdfService;
    @InjectMocks ReportServiceImpl service;

    DiagnosisDetail detail;
    ListDiagnosisProject project;
    InfoHospital hospital;

    @BeforeEach
    void setUp() {
        detail = new DiagnosisDetail();
        detail.setDiagnosisDetailId(11);
        detail.setListDiagnosisProjectId(22);
        detail.setPdfHttpUrl("/Content/FilePdf/report.pdf");
        project = new ListDiagnosisProject();
        project.setListDiagnosisProjectId(22);
        project.setInfoHospitalId(1);
        project.setIsPayMoney(0);
        hospital = new InfoHospital();
        hospital.setInfoHospitalId(1);
        hospital.setPayType(0);
    }

    void stubQuery() {
        ReportResponse response = new ReportResponse();
        when(diagnosisDetailRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(detail)));
        when(reportMapper.toResponse(detail)).thenReturn(response);
        when(listDiagnosisProjectRepository.findById(22)).thenReturn(Optional.of(project));
        when(infoHospitalRepository.findById(1)).thenReturn(Optional.of(hospital));
    }

    ReportVO stubDetail() {
        ReportVO vo = new ReportVO();
        vo.setReportId(11);
        vo.setOrderId(22);
        vo.setPdfHttpUrl("/Content/FilePdf/report.pdf");
        when(diagnosisDetailRepository.findById(11)).thenReturn(Optional.of(detail));
        when(listDiagnosisProjectRepository.findById(22)).thenReturn(Optional.of(project));
        when(infoHospitalRepository.findById(1)).thenReturn(Optional.of(hospital));
        when(reportMapper.toVO(detail)).thenReturn(vo);
        DiagnosisDetailInfo info = new DiagnosisDetailInfo();
        info.setReportResult("secret result");
        info.setMailReport("secret mail");
        lenient().when(diagnosisDetailInfoRepository.findById(11)).thenReturn(Optional.of(info));
        return vo;
    }

    ReportResponse query() {
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            return service.queryReports(new com.petlis.dto.request.ReportQueryRequest()).getContent().get(0);
        }
    }

    @Test
    void cashUnpaidListIsPaymentRestricted() {
        stubQuery();
        assertTrue(query().getPaymentRestricted());
    }

    @Test
    void cashPaidListIsNotPaymentRestricted() {
        project.setIsPayMoney(1);
        stubQuery();
        assertFalse(query().getPaymentRestricted());
    }

    @Test
    void monthlyUnpaidListIsNotPaymentRestricted() {
        hospital.setPayType(1);
        stubQuery();
        assertFalse(query().getPaymentRestricted());
    }

    @Test
    void cashUnpaidDetailReturnsMetadataButMasksContentAndPdf() {
        project.setInfoSampleTypeId(33);
        ReportVO vo = stubDetail();
        InfoSampleType sampleType = new InfoSampleType();
        sampleType.setSampleName("全血");
        when(infoSampleTypeRepository.findById(33)).thenReturn(Optional.of(sampleType));
        when(jdbcTemplate.queryForList(
                org.mockito.ArgumentMatchers.contains("lin_order_extension"),
                org.mockito.ArgumentMatchers.eq(22)))
                .thenReturn(List.of(Map.of("laboratoryName", "中心实验室", "settlementMode", "cash")));
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            ReportVO result = service.getReportDetail(11);
            assertSame(vo, result);
            assertTrue(result.getPaymentRestricted());
            assertFalse(result.getPaid());
            assertEquals("cash", result.getSettlementMode());
            assertEquals(22, result.getOrderId());
            assertEquals("全血", result.getSampleTypeName());
            assertEquals("中心实验室", result.getLaboratoryName());
            assertNull(result.getReportResult());
            assertNull(result.getMailReport());
            assertNull(result.getPdfHttpUrl());
        }
    }

    @Test
    void monthlyUnpaidDetailIsNotRestrictedAndHasPaymentMetadata() {
        hospital.setPayType(1);
        stubDetail();
        when(jdbcTemplate.queryForList(
                org.mockito.ArgumentMatchers.contains("lin_order_extension"),
                org.mockito.ArgumentMatchers.eq(22)))
                .thenReturn(List.of(Map.of("settlementMode", "monthly")));
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            ReportVO result = service.getReportDetail(11);
            assertFalse(result.getPaymentRestricted());
            assertFalse(result.getPaid());
            assertEquals("monthly", result.getSettlementMode());
        }
    }

    @Test
    void detailWithoutOrderExtensionLeavesLaboratoryNameEmpty() {
        hospital.setHospitalName("送检医院");
        stubDetail();
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            ReportVO result = service.getReportDetail(11);
            assertEquals("送检医院", result.getHospitalName());
            assertNull(result.getLaboratoryName());
        }
    }

    @Test
    void cashPaidDetailExposesContentAndPdf() {
        project.setIsPayMoney(1);
        stubDetail();
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            ReportVO result = service.getReportDetail(11);
            assertFalse(result.getPaymentRestricted());
            assertEquals("secret result", result.getReportResult());
            assertEquals("secret mail", result.getMailReport());
            assertEquals("/Content/FilePdf/report.pdf", result.getPdfHttpUrl());
        }
    }

    @Test
    void crossHospitalDetailIsRejectedBeforeContentIsLoaded() {
        when(diagnosisDetailRepository.findById(11)).thenReturn(Optional.of(detail));
        when(listDiagnosisProjectRepository.findById(22)).thenReturn(Optional.of(project));
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(2);
            assertThrows(BusinessException.class, () -> service.getReportDetail(11));
            verify(diagnosisDetailInfoRepository, never()).findById(any());
        }
    }

    @Test
    void cashUnpaidDownloadAndUrlAreRejected() {
        when(diagnosisDetailRepository.findById(11)).thenReturn(Optional.of(detail));
        when(listDiagnosisProjectRepository.findById(22)).thenReturn(Optional.of(project));
        when(infoHospitalRepository.findById(1)).thenReturn(Optional.of(hospital));
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            BusinessException download = assertThrows(BusinessException.class, () -> service.downloadReportPdf(11));
            BusinessException url = assertThrows(BusinessException.class, () -> service.getReportPdfUrl(11));
            assertTrue(download.getMessage().contains("订单未支付"));
            assertTrue(url.getMessage().contains("订单未支付"));
            verifyNoInteractions(fileService);
        }
    }

    @Test
    void cashSnapshotRestrictsUnpaidReportEvenWhenHospitalIsNowMonthly() {
        hospital.setPayType(1);
        stubDetail();
        when(jdbcTemplate.queryForList(contains("SELECT settlement_mode"), eq(22))).thenReturn(List.of(Map.of("settlement_mode", "cash")));
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            ReportVO result = service.getReportDetail(11);
            assertTrue(result.getPaymentRestricted());
            assertNull(result.getReportResult());
        }
    }

    @Test
    void monthlySnapshotAllowsUnpaidReportEvenWhenHospitalIsNowCash() {
        stubDetail();
        when(jdbcTemplate.queryForList(contains("SELECT settlement_mode"), eq(22))).thenReturn(List.of(Map.of("settlement_mode", "monthly")));
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            ReportVO result = service.getReportDetail(11);
            assertFalse(result.getPaymentRestricted());
            assertEquals("secret result", result.getReportResult());
        }
    }

    @Test
    void crossHospitalDownloadIsRejected() {
        when(diagnosisDetailRepository.findById(11)).thenReturn(Optional.of(detail));
        when(listDiagnosisProjectRepository.findById(22)).thenReturn(Optional.of(project));
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(2);
            assertThrows(BusinessException.class, () -> service.downloadReportPdf(11));
            verifyNoInteractions(fileService);
        }
    }

    @Test
    void cashPaidDownloadIsAllowed() throws Exception {
        project.setIsPayMoney(1);
        when(diagnosisDetailRepository.findById(11)).thenReturn(Optional.of(detail));
        when(listDiagnosisProjectRepository.findById(22)).thenReturn(Optional.of(project));
        when(infoHospitalRepository.findById(1)).thenReturn(Optional.of(hospital));
        when(pdfService.getPdfWithAuth(eq(11), isNull())).thenReturn("pdf".getBytes());
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            assertArrayEquals("pdf".getBytes(), service.downloadReportPdf(11).getBody());
        }
    }

    @Test
    void absoluteLegacyDownloadUsesAuthenticatedLocalStorageNotRemoteUrl() throws Exception {
        project.setIsPayMoney(1);
        detail.setPdfHttpUrl("https://laboklin.cn/Content/FilePdf/report.pdf");
        when(diagnosisDetailRepository.findById(11)).thenReturn(Optional.of(detail));
        when(listDiagnosisProjectRepository.findById(22)).thenReturn(Optional.of(project));
        when(infoHospitalRepository.findById(1)).thenReturn(Optional.of(hospital));
        when(pdfService.getPdfWithAuth(eq(11), isNull())).thenReturn("local-pdf".getBytes());
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
            assertArrayEquals("local-pdf".getBytes(), service.downloadReportPdf(11).getBody());
            verifyNoInteractions(fileService);
        }
    }
}
