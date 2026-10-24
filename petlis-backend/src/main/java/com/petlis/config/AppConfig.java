package com.petlis.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

/**
 * 应用配置
 *
 * @author PETLIS Team
 */
@Configuration
@EnableCaching
@EnableJpaAuditing
public class AppConfig {

    /**
     * 缓存管理器
     *
     * 使用Caffeine作为缓存实现
     */
    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("dictionary",
                newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(java.time.Duration.ofHours(1))
                        .build());
        cacheManager.registerCustomCache("hospitals",
                newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(java.time.Duration.ofMinutes(30))
                        .build());
        return cacheManager;
    }
}
