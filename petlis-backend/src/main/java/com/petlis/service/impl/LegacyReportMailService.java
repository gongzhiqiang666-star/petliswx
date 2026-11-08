package com.petlis.service.impl;
import com.petlis.common.security.AccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
@Service
public class LegacyReportMailService extends LegacyBusinessMailService {
 public LegacyReportMailService(FeatureStore store,AccessService access,LegacyOrderDetailService renderer,LegacyMailTransport transport,PlatformTransactionManager manager,@Value("${app.legacy-mail.profile-id:1}") int profile,@Value("${app.legacy-mail.override-to:}") String override,@Value("${app.jwt.secret}") String secret,LegacyReportFiles files){super(store,access,renderer,transport,manager,profile,override,secret,Kind.REPORT_MAIL,files);}
}
