package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ArticleValidationTest {
    @Test void malformedDatesAndExternalLinksAreRejectedBeforeWritingOrBroadcasting(){
        for(var input:new ArticleService.Article[]{
            new ArticleService.Article("资讯","live","标题","摘要","正文","","2026-02-30T12:00","https://example.test/live",false),
            new ArticleService.Article("资讯","live","标题","摘要","正文","","2026-11-09T12:00","https://",false),
            new ArticleService.Article("资讯","article","标题","摘要","正文","javascript:alert(1)",null,null,false),
            new ArticleService.Article("资讯","article","标题","x".repeat(1001),"正文","",null,null,false)
        }){
            var notifications=mock(BusinessNotifications.class);var store=mock(FeatureStore.class);
            var service=new ArticleService(notifications,store,mock(ReviewPermission.class));
            assertEquals(400,assertThrows(BusinessException.class,()->service.publish(input)).getCode());
            verifyNoInteractions(store,notifications);
        }
    }
}
