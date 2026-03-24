package com.invoice.automation.controller;

import com.invoice.automation.config.CacheMonitor;
import com.invoice.automation.service.impl.InvoiceHeaderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache management and monitoring controller
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheManagementController {

    private final CacheMonitor cacheMonitor;
    private final InvoiceHeaderServiceImpl invoiceHeaderService;

    /**
     * Get cache performance metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getCacheMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("cacheMetrics", cacheMonitor.getCacheMetrics());
        return ResponseEntity.ok(response);
    }

    /**
     * Clear all application caches
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        invoiceHeaderService.clearAllCaches();
        cacheMonitor.resetCounters();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All caches cleared successfully");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Warm up caches with frequently accessed data
     */
    @PostMapping("/warm")
    public ResponseEntity<Map<String, String>> warmCaches() {
        invoiceHeaderService.warmCache();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache warming initiated");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh specific cache
     */
    @PostMapping("/refresh/{cacheName}")
    public ResponseEntity<Map<String, String>> refreshCache(@PathVariable String cacheName) {
        Map<String, String> response = new HashMap<>();
        
        switch (cacheName.toLowerCase()) {
            case "customerstats":
                invoiceHeaderService.refreshCustomerStatsCache();
                response.put("message", "Customer statistics cache refreshed");
                break;
            case "querycache":
                invoiceHeaderService.refreshQueryCache();
                response.put("message", "Query cache refreshed");
                break;
            default:
                response.put("message", "Unknown cache: " + cacheName);
                response.put("availableCaches", "customerstats, querycache");
                return ResponseEntity.badRequest().body(response);
        }
        
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }

    /**
     * Get cache health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> cacheMetrics = cacheMonitor.getCacheMetrics();
        
        // Calculate overall cache health
        long totalHits = 0;
        long totalRequests = 0;
        
        for (Map.Entry<String, Object> entry : cacheMetrics.entrySet()) {
            Map<String, Object> metrics = (Map<String, Object>) entry.getValue();
            totalHits += (Long) metrics.get("hits");
            totalRequests += (Long) metrics.get("totalRequests");
        }
        
        double overallHitRate = totalRequests > 0 ? (double) totalHits / totalRequests * 100 : 0;
        
        response.put("overallHitRate", overallHitRate);
        response.put("totalRequests", totalRequests);
        response.put("totalHits", totalHits);
        response.put("health", overallHitRate > 70 ? "GOOD" : overallHitRate > 50 ? "FAIR" : "POOR");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}
