package com.invoice.automation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class InvoiceNumberGenerator {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public InvoiceNumberGenerator(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateInvoiceNumber(LocalDate date) {
        String datePrefix = "INV-" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "invoice:seq:" + datePrefix;
        
        // Atomic increment in Redis - thread-safe and high performance
        Long sequence = redisTemplate.opsForValue().increment(key);
        
        // Set expiry at end of day to clean up old keys
        if (sequence == 1) {
            LocalDate tomorrow = date.plusDays(1);
            long secondsUntilTomorrow = java.time.Duration.between(
                java.time.LocalDateTime.now(), 
                tomorrow.atStartOfDay()
            ).getSeconds();
            redisTemplate.expire(key, secondsUntilTomorrow, TimeUnit.SECONDS);
        }
        
        return datePrefix + "-" + String.format("%04d", sequence);
    }

    public String generateInvoiceNumber() {
        return generateInvoiceNumber(LocalDate.now());
    }
}
