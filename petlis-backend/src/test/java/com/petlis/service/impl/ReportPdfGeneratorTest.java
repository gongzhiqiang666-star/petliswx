package com.petlis.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportPdfGeneratorTest {
    @Test
    void chinesePdfContainsEveryTemplateAndResultItem() throws Exception {
        var generator = new ReportPdfGenerator("");
        byte[] content = generator.create("RPT-中-1", "样本摘要", "临床结论", "B-2", List.of(
                Map.of("template", "血常规模板", "name", "白细胞", "value", "12", "unit", "10^9/L", "referenceRange", "5-10", "abnormalFlag", true),
                Map.of("template", "生化模板", "name", "肌酐", "value", "80", "unit", "μmol/L", "referenceRange", "40-120", "abnormalFlag", false)));
        try (var document = org.apache.pdfbox.Loader.loadPDF(content)) {
            String text = new PDFTextStripper().getText(document);
            for (String expected : List.of("检验报告", "样本摘要", "临床结论", "血常规模板", "白细胞", "异常", "生化模板", "肌酐", "正常")) {
                assertTrue(text.contains(expected), expected);
            }
        }
    }

    @Test
    void multilineSummaryConclusionAndTemplateAreBrokenIntoReadablePdfLines() throws Exception {
        var generator = new ReportPdfGenerator("");
        byte[] content = generator.create("RPT-2", "摘要第一行\n摘要第二行", "结论第一行\n结论第二行", "B-1", List.of(
                Map.of("template", "模板第一行\n模板第二行", "name", "白细胞", "value", "6", "unit", "u", "referenceRange", "1-10", "abnormalFlag", false)));
        try (var document = org.apache.pdfbox.Loader.loadPDF(content)) {
            String text = new PDFTextStripper().getText(document);
            for (String expected : List.of("摘要第一行", "摘要第二行", "结论第一行", "结论第二行", "模板第一行", "模板第二行")) assertTrue(text.contains(expected), expected);
        }
    }
}
