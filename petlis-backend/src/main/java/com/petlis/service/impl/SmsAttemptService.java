package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Failed guesses commit independently; successful verification does not consume the code yet. */
@Service @RequiredArgsConstructor
public class SmsAttemptService {
 private final FeatureStore store;
 @Transactional(propagation=Propagation.REQUIRES_NEW,noRollbackFor=BusinessException.class)
 public long verify(String mobile,String scene,String code){
  var rows=store.jdbc().queryForList("SELECT id,code,times FROM lin_sms WHERE mobile=? AND event=? AND createtime>UNIX_TIMESTAMP()-300 ORDER BY id DESC LIMIT 1 FOR UPDATE",mobile,"petlis_"+scene);
  if(rows.isEmpty() || code==null || !code.matches("[0-9]{6}"))throw new BusinessException("验证码无效或已过期");
  var record=rows.get(0);if(((Number)record.get("times")).intValue()>=5)throw new BusinessException("验证码尝试次数过多，请重新获取");
  if(!matches(record.get("code"),code)){store.jdbc().update("UPDATE lin_sms SET times=times+1 WHERE id=?",record.get("id"));throw new BusinessException("验证码不正确");}
  return ((Number)record.get("id")).longValue();
 }
 static boolean matches(Object expected,String actual){return actual!=null && MessageDigest.isEqual(String.valueOf(expected).getBytes(StandardCharsets.US_ASCII),actual.getBytes(StandardCharsets.US_ASCII));}
}
