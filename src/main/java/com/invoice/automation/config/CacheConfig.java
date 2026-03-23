package com.invoice.automation.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager jCacheManager() {
        return Caching.getCachingProvider().getCacheManager();
    }

    @Bean
    public JCacheCacheManager cacheManager(CacheManager jCacheManager) {
        return new JCacheCacheManager(jCacheManager);
    }
}
