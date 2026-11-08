package com.petlis.controller.api;
import com.petlis.common.exception.BusinessException;
import com.petlis.service.impl.LegacyBankGatewayClient;
import com.petlis.service.impl.LegacyBankPaymentExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
/** Only this exact POST path is public; signature and durable business identity are mandatory. */
@RestController @RequiredArgsConstructor
public class LegacyBankNotificationController {
 private final LegacyBankGatewayClient gateway;
 private final LegacyBankPaymentExecutionService execution;
 private static final int LIMIT=1024*1024;
 @PostMapping(value="/payment/legacy-bank/notify",produces=MediaType.TEXT_PLAIN_VALUE)
 public ResponseEntity<String> notification(HttpServletRequest request){
  if(request.getContentLengthLong()>LIMIT)return ResponseEntity.status(413).body("fail");
  try{
   byte[] raw=request.getInputStream().readNBytes(LIMIT+1);if(raw.length>LIMIT)return ResponseEntity.status(413).body("fail");
   var verified=gateway.verifyNotification(raw);
   return execution.notification(verified,gateway.merchant())?ResponseEntity.ok("success"):ResponseEntity.badRequest().body("fail");
  }catch(BusinessException e){return ResponseEntity.status(e.getCode()==409?503:400).body("fail");}
  catch(IOException e){return ResponseEntity.badRequest().body("fail");}
 }
}
