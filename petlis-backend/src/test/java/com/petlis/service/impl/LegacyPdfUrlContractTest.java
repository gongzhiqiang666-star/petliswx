package com.petlis.service.impl;

import com.petlis.entity.business.DiagnosisDetail;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.mapper.ReportMapper;
import com.petlis.repository.*;
import com.petlis.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegacyPdfUrlContractTest {
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

    @Test
    void pdfUrlEndpointReturnsAbsoluteAuthenticatedViewInsteadOfRelativeStoragePath() throws Exception {
        var detail = new DiagnosisDetail(); detail.setDiagnosisDetailId(11); detail.setListDiagnosisProjectId(22); detail.setPdfHttpUrl("https://laboklin.cn/Content/FilePdf/report.pdf");
        var project = new ListDiagnosisProject(); project.setListDiagnosisProjectId(22); project.setInfoHospitalId(1); project.setIsPayMoney(1);
        var hospital = new InfoHospital(); hospital.setInfoHospitalId(1); hospital.setPayType(0);
        when(diagnosisDetailRepository.findById(11)).thenReturn(Optional.of(detail));
        when(listDiagnosisProjectRepository.findById(22)).thenReturn(Optional.of(project));
        when(infoHospitalRepository.findById(1)).thenReturn(Optional.of(hospital));
        ReflectionTestUtils.setField(service, "serverDomain", "https://laboklin.cn");
        ReflectionTestUtils.setField(service, "contextPath", "/api");
        try (var security = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            security.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId).thenReturn(1);
            assertEquals("https://laboklin.cn/api/pdf/view/11", service.getReportPdfUrl(11).toString());
        }
    }
}
