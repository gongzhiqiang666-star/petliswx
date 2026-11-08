package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import org.apache.commons.csv.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import java.io.*;
import java.math.BigDecimal;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/** First-sheet raw values, never evaluates spreadsheet formulas or resolves external links. */
final class LegacyPaymentLogImportFile {
 static final int MAX_ROWS=1000, MAX_BYTES=2*1024*1024, MAX_COLUMNS=64;
 record Row(int number,List<String> cells){}
 record Table(List<String> headers,List<Row> rows,String encoding){}
 static Table read(String filename,byte[] bytes,String encoding){
  if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw bad("请选择不超过2MB的导入文件");
  String name=filename==null?"":filename.toLowerCase(Locale.ROOT);
  try{
   if(name.endsWith(".csv"))return csv(bytes,encoding);
   if(!name.endsWith(".xls")&&!name.endsWith(".xlsx"))throw bad("仅支持CSV、XLS、XLSX文件");
   if(name.endsWith(".xlsx")||bytes.length>1&&bytes[0]=='P'&&bytes[1]=='K')zipLimit(bytes);
   try(Workbook workbook=WorkbookFactory.create(new ByteArrayInputStream(bytes))){
    if(workbook.getNumberOfSheets()==0)throw bad("文件没有工作表");var sheet=workbook.getSheetAt(0);
    if(sheet.getLastRowNum()>MAX_ROWS)throw bad("单批最多1000行，请拆分文件");
    List<String> headers=cells(sheet.getRow(0),-1);if(headers.isEmpty())throw bad("文件首行必须是表头");
    var rows=new ArrayList<Row>();for(int i=1;i<=sheet.getLastRowNum();i++){var values=cells(sheet.getRow(i),headers.size());if(values.stream().anyMatch(v->!v.isEmpty()))rows.add(new Row(i+1,values));}
    return new Table(headers,List.copyOf(rows),"EXCEL_RAW");
   }
  }catch(BusinessException e){throw e;}catch(Exception e){throw bad("导入文件无法读取，请检查文件格式、编码或加密设置");}
 }
 private static Table csv(byte[] bytes,String encoding)throws IOException{
  String chosen=encoding==null?"AUTO":encoding.toUpperCase(Locale.ROOT),text=null;
  List<String> candidates=chosen.equals("AUTO")?List.of("UTF-8","GBK","ISO-8859-1"):List.of(chosen);
  for(String candidate:candidates){if(!List.of("UTF-8","GBK","BIG5","ISO-8859-1").contains(candidate))throw bad("不支持该CSV编码");try{text=Charset.forName(candidate).newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();chosen=candidate;break;}catch(CharacterCodingException ignored){}}
  if(text==null)throw bad("CSV编码不匹配，请选择原文件编码");if(text.startsWith("\ufeff"))text=text.substring(1);
  try(var parser=CSVFormat.DEFAULT.parse(new StringReader(text))){List<String> headers=null;var rows=new ArrayList<Row>();int number=0;
   for(var record:parser){number++;if(record.size()>MAX_COLUMNS)throw bad("导入列数超过64列");var values=new ArrayList<String>();record.forEach(values::add);
    if(headers==null){headers=List.copyOf(values);continue;}if(number-1>MAX_ROWS)throw bad("单批最多1000行，请拆分文件");if(values.stream().anyMatch(v->!v.isEmpty()))rows.add(new Row(number,List.copyOf(values)));
   }if(headers==null||headers.isEmpty())throw bad("文件首行必须是表头");return new Table(headers,List.copyOf(rows),chosen);
  }
 }
 private static List<String> cells(org.apache.poi.ss.usermodel.Row row,int count){
  if(row==null)return count<0?List.of():Collections.nCopies(count,"");int length=count<0?Math.max(0,row.getLastCellNum()):count;
  if(row.getLastCellNum()>MAX_COLUMNS||length>MAX_COLUMNS)throw bad("导入列数超过64列");var values=new ArrayList<String>();
  for(int c=0;c<length;c++){var cell=row.getCell(c);if(cell==null){values.add("");continue;}String value=switch(cell.getCellType()){
   case STRING->cell.getStringCellValue();case FORMULA->"="+cell.getCellFormula();case BOOLEAN->cell.getBooleanCellValue()?"1":"";
   case NUMERIC->{String raw=cell instanceof XSSFCell x?x.getRawValue():NumberToTextConverter.toText(cell.getNumericCellValue());yield new BigDecimal(raw).toPlainString();}
   case ERROR->org.apache.poi.ss.usermodel.FormulaError.forInt(cell.getErrorCellValue()).getString();default->"";};values.add(value);
  }return List.copyOf(values);
 }
 private static void zipLimit(byte[] bytes)throws IOException{
  long total=0;int entries=0;try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))){var buffer=new byte[8192];while(zip.getNextEntry()!=null){if(++entries>2000)throw bad("工作簿资源过多，请拆分文件");int n;while((n=zip.read(buffer))!=-1){total+=n;if(total>16*1024*1024)throw bad("工作簿解压后过大，请拆分文件");}}}
 }
 private static BusinessException bad(String message){return new BusinessException(400,message);}
}
