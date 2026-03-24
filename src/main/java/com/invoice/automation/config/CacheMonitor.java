package com.invoice.automation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache monitoring and metrics collection
 */
@Component
@Slf4j
public class CacheMonitor {

    private final CacheManager cacheManager;
    private final Map<String, AtomicLong> cacheHitCounts = new HashMap<>();
    private final Map<String, AtomicLong> cacheMissCounts = new HashMap<>();

    @Autowired
    public CacheMonitor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        initializeCounters();
    }

    private void initializeCounters() {
        cacheManager.getCacheNames().forEach(name -> {
            cacheHitCounts.put(name, new AtomicLong(0));
            cacheMissCounts.put(name, new AtomicLong(0));
        });
        log.info("Cache monitoring initialized for {} caches", cacheManager.getCacheNames().size());
    }

    /**
     * Log cache statistics every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logCacheStatistics() {
        log.info("=== Cache Statistics Report ===");
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                long hits = cacheHitCounts.get(cacheName).get();
                long misses = cacheMissCounts.get(cacheName).get();
                double hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) * 100 : 0;
                
                log.info("Cache: {} | Hits: {} | Misses: {} | Hit Rate: {:.2f}%", 
                        cacheName, hits, misses, hitRate);
            }
        });
        
        log.info("=== End Cache Statistics ===");
    }

    /**
     * Get cache performance metrics
     */
    public Map<String, Object> getCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Map<String, Object> cacheMetrics = new HashMap<>();
            Cache cache = cacheManager.getCache(cacheName);
            
            if (cache != null) {
                long hits = cacheHitCounts.get(cacheName).get();
                long misses = cacheMissCounts.get(cacheName).get();
                double hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) * 100 : 0;
                
                cacheMetrics.put("hits", hits);
                cacheMetrics.put("misses", misses);
                cacheMetrics.put("hitRate", hitRate);
                cacheMetrics.put("totalRequests", hits + misses);
            }
            
            metrics.put(cacheName, cacheMetrics);
        });
        
        return metrics;
    }

    /**
     * Record cache hit
     */
    public void recordCacheHit(String cacheName) {
        cacheHitCounts.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss(String cacheName) {
        cacheMissCounts.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Reset all counters
     */
    public void resetCounters() {
        cacheHitCounts.values().forEach(counter -> counter.set(0));
        cacheMissCounts.values().forEach(counter -> counter.set(0));
        log.info("Cache counters reset");
    }
}
