package com.petlis.service.impl;
import java.math.BigDecimal;
import java.util.*;
/** Fully synthetic old-schema order; no real patient/order fields are copied. */
final class LegacyDetailFixtures {
 static long order(FeatureStore store,String prefix,int actor){
  var columns=store.jdbc().queryForList("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION");
  var names=new ArrayList<String>();var values=new ArrayList<Object>();
  for(var c:columns){String name=c.get("COLUMN_NAME").toString(),type=c.get("DATA_TYPE").toString();if(c.get("EXTRA").toString().contains("auto_increment"))continue;if(!name.matches("[A-Za-z0-9_]+"))throw new IllegalStateException("Unexpected schema identifier");
   if(!Set.of("UserNO","CreateSampleId").contains(name)&&("YES".equals(c.get("IS_NULLABLE"))||c.get("COLUMN_DEFAULT")!=null))continue;
   names.add("`"+name+"`");
   if(name.equals("UserNO"))values.add(prefix);else if(name.equals("CreateSampleId"))values.add(actor);
   else if(Set.of("int","tinyint","smallint","mediumint","bigint","decimal","double","float").contains(type))values.add(BigDecimal.ZERO);
   else if(Set.of("datetime","timestamp").contains(type))values.add("1900-01-01 00:00:00");else if(type.equals("date"))values.add("1900-01-01");
   else if(Set.of("varchar","char","text","mediumtext","longtext").contains(type))values.add(prefix);else throw new IllegalStateException("Unsupported fixture type "+type);
  }
  return store.insert("INSERT INTO lin_lb_listdiagnosisproject("+String.join(",",names)+") VALUES("+String.join(",",Collections.nCopies(values.size(),"?"))+")",values.toArray());
 }
 static int test(FeatureStore store,String prefix){return (int)store.insert("INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive) VALUES(?,?,0,0,0,0,0,'',NOW(),0,1)",prefix,prefix);}
}
