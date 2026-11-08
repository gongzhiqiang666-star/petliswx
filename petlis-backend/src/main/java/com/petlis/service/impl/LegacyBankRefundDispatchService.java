package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Service;import org.springframework.transaction.support.TransactionSynchronizationManager;import java.util.*;
@Service
public class LegacyBankRefundDispatchService {
 private final LegacyBankRefundService refunds;private final LegacyBankGatewayClient gateway;private final String merchant;
 public LegacyBankRefundDispatchService(LegacyBankRefundService refunds,LegacyBankGatewayClient gateway,@Value("${app.legacy-bank.merchant:}")String merchant){this.refunds=refunds;this.gateway=gateway;this.merchant=merchant;}
 public Map<String,Boolean> capabilities(){boolean create=refunds.enabled(false)&&merchant!=null&&merchant.matches("[A-Za-z0-9]{1,20}");return Map.of("bankRefundCreate",create,"bankPaymentRefund",create&&gateway.available(),"bankPaymentRefundQuery",refunds.enabled(true)&&gateway.available());}
 public Map<String,Object> create(LegacyBankRefundService.Command command){if(!capabilities().get("bankRefundCreate"))throw new BusinessException(409,"退款申请尚未开启或商户配置缺失");return refunds.create(command,merchant);}
 public void execute(String key,boolean query){if(TransactionSynchronizationManager.isActualTransactionActive())throw new BusinessException(409,"退款网关调用不能在外层事务中执行");if(!gateway.available()||!refunds.enabled(query))throw new BusinessException(409,"退款发送或查询尚未开启");var claim=refunds.claim(key,gateway.merchant(),query);com.fasterxml.jackson.databind.JsonNode result;try{result=gateway.exchange(query?LegacyBankProtocol.Method.REFUND_QUERY:LegacyBankProtocol.Method.REFUND,claim.parameters());}catch(RuntimeException e){refunds.unknown(key,claim.token());return;}try{refunds.finish(key,claim.token(),result);}catch(RuntimeException e){refunds.unknown(key,claim.token());throw new BusinessException(409,"银行退款结果已返回但本地保存未完成，请查询原退款核对");}}
}
