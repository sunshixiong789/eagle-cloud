package com.eagle.example.integration.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Resilience4j Starter 验证服务。
 */
@Slf4j
@Service
public class ResilienceVerificationService {

    @CircuitBreaker(name = "eagle-default", fallbackMethod = "fallback")
    @Retry(name = "eagle-default")
    public String callWithResilience(boolean shouldFail) {
        if (shouldFail) {
            throw new RuntimeException("Simulated failure");
        }
        return "success";
    }

    public String fallback(boolean shouldFail, Throwable t) {
        log.warn("[Resilience4j] Fallback triggered: {}", t.getMessage());
        return "fallback";
    }
}
