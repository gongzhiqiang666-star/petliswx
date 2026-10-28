package com.petlis.service.impl;

import com.petlis.common.enums.OrderStatusEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.io.IOException;

/** Imports future legacy transitions without changing the legacy trigger or its rows. */
@Component @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name="app.notifications.legacy-bridge-enabled",havingValue="true")
public class LegacyOrderEventBridge {
 private final FeatureStore store;
 private final BusinessNotifications notifications;
 private final Environment env;
 private long baseline;
 @PostConstruct public void initialize() throws IOException {
  Path file=Path.of(env.getProperty("app.notifications.legacy-baseline-file",System.getProperty("user.home")+"/.local/share/petlis/legacy-notification-baseline.txt"));
  if(Files.exists(file)){baseline=Long.parseLong(Files.readString(file).trim());if(baseline<0)throw new IOException("Invalid notification baseline");return;}
  Long latest=store.jdbc().queryForObject("SELECT COALESCE(MAX(id),0) FROM lin_lb_notification",Long.class);
  baseline=latest==null?0:latest;
  Files.createDirectories(file.toAbsolutePath().getParent());
  // A fixed baseline, not a moving cursor: late-committing transactions cannot be skipped.
  try{Files.writeString(file,Long.toString(baseline),StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException e){baseline=Long.parseLong(Files.readString(file).trim());}
 }
 @Scheduled(fixedDelayString="${app.notifications.legacy-interval-ms:5000}")
 public void scheduled(){try{importBatch();}catch(Exception e){log.warn("Legacy notification import failed; next scan will retry ({})",e.getClass().getSimpleName());}}
 public synchronized int importBatch(){
  var rows=store.jdbc().queryForList("""
   SELECT n.id,n.order_id,n.new_status,u.id AS user_id
   FROM lin_lb_notification n
   JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=n.order_id AND o.InfoHospitalId=n.hospital_id
   JOIN lin_user u ON u.hospital_id=n.hospital_id AND u.status='normal'
   LEFT JOIN lin_doctor_profile p ON p.user_id=u.id
   LEFT JOIN lin_notification_event e ON e.user_id=u.id AND e.event_type=IF(n.new_status=3,'report','order') AND e.business_key=CONCAT('legacy:',n.id)
   WHERE n.id>? AND e.id IS NULL AND n.new_status IN (-3,-2,0,1,2,3,4)
   AND (p.review_status IS NULL OR p.review_status='approved')
   AND (o.CreateSampleId=u.id OR NOT EXISTS (SELECT 1 FROM lin_user creator WHERE creator.id=o.CreateSampleId AND creator.hospital_id=n.hospital_id))
   ORDER BY n.id,u.id LIMIT 50
   """,baseline);
  int imported=0;
  for(var row:rows){
   int status=((Number)row.get("new_status")).intValue(),order=((Number)row.get("order_id")).intValue();
   if(status==3 && Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_notification_event WHERE event_type='report' AND business_key=?)",Boolean.class,order+":published")))continue;
   var state=OrderStatusEnum.fromCode(status);
   notifications.emit(((Number)row.get("user_id")).intValue(),status==3?"report":"order","legacy:"+row.get("id"),"送检单"+state.getDescription(),"送检单 #"+order+" 当前状态："+state.getDescription(),"pages/order-detail/order-detail?id="+order);
   imported++;
  }
  return imported;
 }
}
