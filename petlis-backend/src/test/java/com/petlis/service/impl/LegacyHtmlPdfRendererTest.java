package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyHtmlPdfRendererTest {
 @Test void missingConfigurationAndEmptyInputAreExplicitFailures(){var renderer=new LegacyHtmlPdfRenderer("node","","",System.getProperty("java.io.tmpdir"));assertEquals(400,assertThrows(BusinessException.class,()->renderer.render("")).getCode());assertEquals(409,assertThrows(BusinessException.class,()->renderer.render("<p>content</p>")).getCode());}
 @Test @EnabledIfEnvironmentVariable(named="PETLIS_HTML_PDF_TEST",matches="true") void actualProcessProducesReadablePdfAndCleansTemporaryFiles()throws Exception{
  Path script=Path.of("../scripts/editor-parity/legacy-html-pdf.js").toAbsolutePath().normalize();if(!Files.isRegularFile(script))script=Path.of("scripts/editor-parity/legacy-html-pdf.js").toAbsolutePath().normalize();Path directory=Path.of(System.getProperty("user.home"),".local/share/petlis/reports/qa-html-pdf/java");Files.createDirectories(directory);
  var renderer=new LegacyHtmlPdfRenderer("node",script.toString(),"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",directory.toString());
  byte[] pdf=renderer.render("<h1>中文报告 Java API</h1><table border='1'><tr><td>结果</td><td>12.50</td></tr></table>_ueditor_page_break_tag_<p>END</p>");
  try(var document=Loader.loadPDF(pdf)){String text=java.text.Normalizer.normalize(new PDFTextStripper().getText(document),java.text.Normalizer.Form.NFKC).replaceAll("\\s+","");assertTrue(text.contains("中文报告"),"Chinese text: "+text);assertTrue(text.contains("12.50"),"Numeric result: "+text);assertTrue(text.contains("END"),"End marker: "+text);assertTrue(document.getNumberOfPages()>=2,"Explicit page break");}
  assertEquals(422,assertThrows(BusinessException.class,()->renderer.render("<img src='http://127.0.0.1:9/missing.png'>")).getCode());try(var files=Files.list(directory)){assertEquals(0,files.count());}
 }
}
