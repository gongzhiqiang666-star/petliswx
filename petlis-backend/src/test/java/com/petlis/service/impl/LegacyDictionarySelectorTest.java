package com.petlis.service.impl;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LegacyDictionarySelectorTest {
 @Test void wordsAreOrButFieldsRespectAndOr(){
  var p=LegacyDictionarySelector.compile("InfoAnimalId","AnimalName","AnimalName_en",new LegacyDictionarySelector.Query(List.of("cat","dog","0","cat"),List.of("AnimalName","AnimalName_en"),"AND",null,1,10,1));
  assertEquals(" WHERE ((AnimalName LIKE ? AND AnimalName_en LIKE ?) OR (AnimalName LIKE ? AND AnimalName_en LIKE ?)) AND IsActive=?",p.where());
  assertEquals(List.of("%cat%","%cat%","%dog%","%dog%",1),p.args());
  assertEquals(0,p.offset());assertEquals(10,p.limit());
 }
 @Test void preselectedValuesIgnoreSearchStatusAndPagePreserveFirstOrder(){
  var p=LegacyDictionarySelector.compile("DictSexId","SexName","SexName_en",new LegacyDictionarySelector.Query(List.of("notfound"),null,"AND",List.of(3L,1L,3L),5,1,1));
  assertEquals(" WHERE DictSexId IN (?,?)",p.where());assertEquals(List.of(3L,1L),p.args());
  assertEquals(List.of(3L,1L),p.selectedIds());assertEquals(0,p.offset());
 }
 @Test void emptySelectionDoesNotTurnIntoUnfilteredSearch(){
  var p=LegacyDictionarySelector.compile("DictSexId","SexName","SexName_en",new LegacyDictionarySelector.Query(null,null,null,List.of(),1,10,null));
  assertEquals(" WHERE 1=0",p.where());
 }
 @Test void invalidSearchFieldAndPaginationAreRejected(){
  assertThrows(RuntimeException.class,()->LegacyDictionarySelector.compile("DictSexId","SexName","SexName_en",new LegacyDictionarySelector.Query(List.of("x"),List.of("SexName OR 1=1"),null,null,1,10,null)));
  assertThrows(RuntimeException.class,()->LegacyDictionarySelector.compile("DictSexId","SexName","SexName_en",new LegacyDictionarySelector.Query(null,null,null,null,0,10,null)));
 }
 @Test void requestFilterPrecedesSearchAndZeroIsRemoved(){
  var p=LegacyDictionarySelector.compile("DictSexId","SexName","SexName_en",new LegacyDictionarySelector.Query(List.of(" <b>A&B</b> ","0"," "),null,"OR",null,2,5,null));
  assertEquals(List.of("%A&amp;B%"),p.args());assertEquals(5,p.offset());
 }
 @Test void displayUsesOriginalUnicodeAndHistoricalUrlReplacements(){
  assertEquals("猫 http://my.laboklin.cn/a",LegacyDictionarySelector.display("\\u732b http://vetlab.vettina.com/a"));
  assertEquals("A",LegacyDictionarySelector.display("\\u41"));
  assertEquals("\ufffd\ufffd",LegacyDictionarySelector.display("\\ud83d\\udc31"));
  assertEquals("font-family:Microsoft YaHei;",LegacyDictionarySelector.display("font-family:\"Microsoft YaHei\";"));
 }
}
