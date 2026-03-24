package com.invoice.automation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Set;

/**
 * Cache configuration for performance optimization
 */
@Configuration
@EnableCaching
@EnableScheduling
@Slf4j
public class CacheConfig {

    /**
     * Configure EhCache as the cache manager
     */
    @Bean
    public org.springframework.cache.CacheManager cacheManager() {
        net.sf.ehcache.CacheManager ehCacheManager = net.sf.ehcache.CacheManager.create();
        EhCacheCacheManager cacheManager = new EhCacheCacheManager(ehCacheManager);
        
        log.info("Cache manager initialized with {} caches", 
                ehCacheManager.getCacheNames().size());
        
        return cacheManager;
    }

    /**
     * Cache monitoring bean for performance metrics
     */
    @Bean
    public CacheMonitor cacheMonitor(org.springframework.cache.CacheManager cacheManager) {
        return new CacheMonitor(cacheManager);
    }
}
