package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ReportPdfGenerator {
    private static final List<String> DEFAULT_FONTS = List.of(
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/System/Library/Fonts/Supplemental/NISC18030.ttf",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttf",
            "/usr/share/fonts/truetype/noto/NotoSansCJKsc-Regular.ttf");
    private final String configuredFont;

    public ReportPdfGenerator(@Value("${app.reports.font-path:}") String configuredFont) {
        this.configuredFont = configuredFont;
    }

    public byte[] create(String reportNo, String summary, String conclusion, String batch, List<Map<String, Object>> items) {
        List<String> lines = new ArrayList<>();
        lines.add("检验报告");
        lines.add("报告编号：" + reportNo);
        lines.add("检测批次：" + batch);
        lines.add("样本摘要：" + summary);
        lines.add("临床结论：" + conclusion);
        lines.add("检测结果");
        for (Map<String, Object> item : items) {
            lines.add("项目模板：" + value(item.get("template")));
            lines.add(value(item.get("name")) + "：" + value(item.get("value")) + " " + value(item.get("unit"))
                    + "｜参考范围：" + value(item.get("referenceRange")) + "｜" + (Boolean.TRUE.equals(item.get("abnormalFlag")) ? "异常" : "正常"));
        }
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font;
            try (var input = Files.newInputStream(fontPath())) { font = PDType0Font.load(document, input, true); }
            PDPageContentStream stream = null;
            float y = 0;
            for (String source : lines) {
                for (String line : wrap(source, font)) {
                    if (stream == null || y < 55) {
                        if (stream != null) stream.close();
                        PDPage page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        stream = new PDPageContentStream(document, page);
                        stream.setFont(font, 11);
                        y = 790;
                    }
                    stream.beginText();
                    stream.newLineAtOffset(45, y);
                    stream.showText(line);
                    stream.endText();
                    y -= 20;
                }
            }
            if (stream != null) stream.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException error) {
            throw new BusinessException(500, "中文报告PDF生成失败", error);
        }
    }

    private List<String> wrap(String text, PDType0Font font) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            StringBuilder line = new StringBuilder();
            for (int offset = 0; offset < paragraph.length();) {
                int codePoint = paragraph.codePointAt(offset);
                String next = new String(Character.toChars(codePoint));
                if (font.getStringWidth(line + next) / 1000 * 11 > 505 && !line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(next);
                offset += Character.charCount(codePoint);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private Path fontPath() {
        if (configuredFont != null && !configuredFont.isBlank()) {
            Path path = Path.of(configuredFont);
            if (Files.isRegularFile(path)) return path;
            throw new BusinessException(500, "配置的中文报告字体不可用");
        }
        return DEFAULT_FONTS.stream().map(Path::of).filter(Files::isRegularFile).findFirst()
                .orElseThrow(() -> new BusinessException(500, "生成报告需要中文TrueType字体，请配置 app.reports.font-path"));
    }

    private String value(Object value) { return value == null ? "" : value.toString(); }
}
