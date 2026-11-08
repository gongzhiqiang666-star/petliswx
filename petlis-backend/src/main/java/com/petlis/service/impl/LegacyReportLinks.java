package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
/** Generates strings only. No network requests and no authentication based solely on this legacy cipher. */
@Service
public class LegacyReportLinks {
 private final String base,phrase;
 public LegacyReportLinks(@Value("${APP_LEGACY_PUBLIC_BASE_URL:}") String base,@Value("${APP_LEGACY_LINK_KEY_PHRASE:}") String phrase){this.base=base;this.phrase=phrase;}
 public String detail(long id){
  if(id<0||id>4294967295L)throw new BusinessException(400,"原明细编号无效");
  if(base==null||base.isBlank()||phrase==null||phrase.isBlank())throw new BusinessException(409,"原报告链接尚未配置，请先配置本机链接地址和私有兼容密钥");
  try{var uri=URI.create(base);if(!uri.isAbsolute()||!("http".equalsIgnoreCase(uri.getScheme())||"https".equalsIgnoreCase(uri.getScheme()))||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException();}catch(IllegalArgumentException e){throw new BusinessException(409,"原报告链接地址无效");}
  // Preserve original concatenation; configuration owns the trailing slash.
  return base+"Project/View/Detail.aspx?CID="+LegacyLinkCodec.encrypt(String.valueOf(id),phrase);
 }
}
