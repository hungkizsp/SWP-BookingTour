package com.tourbooking.booking.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    // Allow up to 3 requests per minute
    private static final int MAX_REQUESTS = 3;
    private static final long REFILL_DURATION = 60000;

    public boolean tryConsume(String key) {
        long now = System.currentTimeMillis();
        
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(MAX_REQUESTS, now));
        
        synchronized (bucket) {
            // Refill tokens if duration has passed
            if (now - bucket.lastRefillTime > REFILL_DURATION) {
                bucket.tokens = MAX_REQUESTS;
                bucket.lastRefillTime = now;
            }
            
            if (bucket.tokens > 0) {
                bucket.tokens--;
                return true;
            }
            
            return false;
        }
    }

    private static class TokenBucket {
        int tokens;
        long lastRefillTime;

        TokenBucket(int tokens, long lastRefillTime) {
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }
    }
}
