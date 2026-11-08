package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
/** Deliberately outside a transaction: state methods commit before any gateway exchange. */
@Service @RequiredArgsConstructor
public class LegacyBankPaymentDispatchService {
 private final LegacyBankPaymentExecutionService execution;
 private final LegacyBankGatewayClient gateway;
 public Map<String,Boolean> capabilities(){return Map.of("bankPaymentSend",execution.allowed("sendEnabled")&&gateway.available(),"bankPaymentProviderQuery",execution.allowed("queryEnabled")&&gateway.available());}
 public void execute(String key,boolean query){
  if(TransactionSynchronizationManager.isActualTransactionActive())throw new BusinessException(409,"银行调用不能在外层事务中执行");
  if(!gateway.available()||!execution.allowed(query?"queryEnabled":"sendEnabled"))throw new BusinessException(409,"原网银发送或查询尚未开启");
  var claimed=execution.claim(key,gateway.merchant(),query);if(claimed==null)return;
  com.fasterxml.jackson.databind.JsonNode verified;
  try{verified=gateway.exchange(query?LegacyBankProtocol.Method.PAY_QUERY:LegacyBankProtocol.Method.PREPAY,claimed.parameters());}
  catch(RuntimeException e){execution.unknown(key,claimed.token());return;}
  try{execution.finish(key,claimed.token(),query,verified);}
  catch(RuntimeException e){execution.unknown(key,claimed.token());throw new BusinessException(409,"银行结果已返回但本地处理未完成，请查询原交易核对");}
 }
}
