package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.nio.file.*;import java.nio.file.attribute.PosixFilePermission;
import java.net.URI;import java.security.*;import java.security.spec.*;import java.time.Duration;
/** Runtime only exposes DISABLED or an explicitly configured owned loopback sandbox. */
@Configuration
public class LegacyBankGatewayConfiguration {
 @Bean LegacyBankGatewayClient legacyBankGatewayClient(
   @Value("${app.legacy-bank.mode:DISABLED}") String mode,
   @Value("${app.legacy-bank.endpoint:}") String endpoint,
   @Value("${app.legacy-bank.app-id:}") String app,
   @Value("${app.legacy-bank.merchant:}") String merchant,
   @Value("${app.legacy-bank.private-key:}") String privateKey,
   @Value("${app.legacy-bank.public-key:}") String publicKey,
   @Value("${app.legacy-bank.timeout-ms:15000}") long timeout){
  if("DISABLED".equals(mode))return new LegacyBankGatewayClient();
  if(!"TEST_LOCAL".equals(mode))throw new BusinessException(409,"当前仅允许本机网银测试通道；真实收款需另行启用");
  try{
   if(app.isBlank()||merchant==null||!merchant.matches("[A-Za-z0-9]{1,20}"))throw new IllegalArgumentException();
   var keyFactory=KeyFactory.getInstance("RSA");
   return new LegacyBankGatewayClient(LegacyBankGatewayClient.Mode.TEST_LOCAL,URI.create(endpoint),app,merchant,"https://petlis.invalid/bank-notify",keyFactory.generatePrivate(new PKCS8EncodedKeySpec(key(privateKey))),keyFactory.generatePublic(new X509EncodedKeySpec(key(publicKey))),Duration.ofMillis(timeout));
  }catch(Exception e){throw new BusinessException(409,"本机网银测试配置或合成密钥无效");}
 }
 private static byte[] key(String value)throws Exception{
  var root=Path.of(System.getProperty("user.home"),".local/share/petlis").toRealPath();var path=Path.of(value).toAbsolutePath().normalize();
  if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||!path.toRealPath().startsWith(root)||Files.size(path)>65536||!Files.getOwner(path).getName().equals(System.getProperty("user.name")))throw new IllegalArgumentException();
  for(var permission:Files.getPosixFilePermissions(path))if(permission==PosixFilePermission.GROUP_READ||permission==PosixFilePermission.GROUP_WRITE||permission==PosixFilePermission.OTHERS_READ||permission==PosixFilePermission.OTHERS_WRITE)throw new IllegalArgumentException();
  return Files.readAllBytes(path);
 }
}
