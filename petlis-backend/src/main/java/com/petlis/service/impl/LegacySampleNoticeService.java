package com.petlis.service.impl;
import com.petlis.common.security.AccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
@Service
public class LegacySampleNoticeService extends LegacyBusinessMailService {
 public LegacySampleNoticeService(FeatureStore store,AccessService access,LegacyOrderDetailService renderer,LegacyMailTransport transport,PlatformTransactionManager manager,@Value("${app.legacy-mail.profile-id:1}") int profile,@Value("${app.legacy-mail.override-to:}") String override,@Value("${app.jwt.secret}") String secret){super(store,access,renderer,transport,manager,profile,override,secret,Kind.SAMPLE_NOTICE,null);}
}
