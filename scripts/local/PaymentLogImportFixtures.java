import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
/** Synthetic files only; invoked by the isolated live test, never production data. */
class PaymentLogImportFixtures {
 public static void main(String[] args)throws Exception{
  Path root=Path.of(System.getProperty("user.home"),".local/share/petlis/qa").toRealPath(),dir=Path.of(args[0]).toRealPath();String marker=args[1];
  if(!dir.startsWith(root)||Files.isSymbolicLink(Path.of(args[0]))||!marker.matches("it_paylog_live_[a-f0-9]{10}"))throw new IllegalArgumentException("Only owned QA fixtures are allowed");
  String[] headers={"BuyMail","BuyerId","TradeNoAli","TradeNoUser","TotalFee","TradeStatus","Subject","TradeBody","Exterface","CreateDt"};
  String csv="\ufeff"+String.join(",",headers)+"\r\n,,"+marker+","+marker+",-12.3456,CUSTOM,\"中文,第一行\n第二行\",,,2020-02-29 13:14:15\r\n,,"+marker+","+marker+",9999999999999999.99,CUSTOM,大金额,,,2020-02-29 13:14:15\r\n";
  Files.writeString(dir.resolve("new.csv"),csv,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
  for(boolean xlsx:new boolean[]{false,true})try(Workbook wb=xlsx?new XSSFWorkbook():new HSSFWorkbook()){
   var sheet=wb.createSheet("first");var head=sheet.createRow(0);for(int i=0;i<headers.length;i++)head.createCell(i).setCellValue(headers[i]);
   var row=sheet.createRow(1);String[] values={"","",marker,marker,xlsx?"4.005":"3.0001","CUSTOM",xlsx?"XLSX正文":"=1+1","","","2020-02-29 13:14:15"};
   for(int i=0;i<values.length;i++){var cell=row.createCell(i);if(!xlsx&&i==6)cell.setCellFormula("1+1");else cell.setCellValue(values[i]);}
   wb.createSheet("ignored").createRow(0).createCell(0).setCellValue("not imported");
   try(var out=Files.newOutputStream(dir.resolve(xlsx?"new.xlsx":"new.xls"),StandardOpenOption.CREATE_NEW)){wb.write(out);}
  }
 }
}
