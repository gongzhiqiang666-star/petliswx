package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.SecurityUtils;
import com.petlis.dto.response.NotificationResponse;
import com.petlis.entity.business.NotificationRecord;
import com.petlis.mapper.NotificationMapper;
import com.petlis.repository.*;
import com.petlis.service.WechatNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyNotificationAccessTest {
 @Test void detailRejectsAnotherHospitalBeforeMappingOrLoadingNames(){
  var repository=mock(NotificationRepository.class);var mapper=mock(NotificationMapper.class);var hospitals=mock(InfoHospitalRepository.class);var users=mock(UserRepository.class);
  var record=new NotificationRecord();record.setId(11);record.setHospitalId(2);record.setNewStatus(3);
  when(repository.findById(11)).thenReturn(Optional.of(record));when(mapper.toResponse(record)).thenReturn(new NotificationResponse());
  var service=new NotificationServiceImpl(repository,mock(JdbcTemplate.class),mock(ReviewPermission.class),hospitals,users,mapper,mock(WechatNotificationService.class));
  try(var security=mockStatic(SecurityUtils.class)){
   security.when(SecurityUtils::getCurrentHospitalId).thenReturn(1);
   assertThrows(BusinessException.class,()->service.getNotificationDetail(11));
   verifyNoInteractions(mapper,hospitals,users);
  }
 }
 @Test void sameHospitalCanReadCorrectedStatusWording(){
  var repository=mock(NotificationRepository.class);var mapper=mock(NotificationMapper.class);var hospitals=mock(InfoHospitalRepository.class);
  var record=new NotificationRecord();record.setId(11);record.setHospitalId(2);record.setOrderId(9);record.setNewStatus(-3);
  when(repository.findById(11)).thenReturn(Optional.of(record));when(mapper.toResponse(record)).thenReturn(new NotificationResponse());when(hospitals.findById(2)).thenReturn(Optional.empty());
  var service=new NotificationServiceImpl(repository,mock(JdbcTemplate.class),mock(ReviewPermission.class),hospitals,mock(UserRepository.class),mapper,mock(WechatNotificationService.class));
  try(var security=mockStatic(SecurityUtils.class)){
   security.when(SecurityUtils::getCurrentHospitalId).thenReturn(2);
   assertTrue(service.getNotificationDetail(11).getNotificationMessage().contains("待支付"));
  }
 }
}
