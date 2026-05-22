package com.eagle.example.integration.httpclient;

import com.eagle.http.client.support.EagleRestServiceClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * HTTP Client Starter 验证服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpClientVerificationService {

    private final EagleRestServiceClientFactory clientFactory;

    public EagleRestServiceClientFactory getFactory() {
        log.info("[HttpClient] Factory type: {}", clientFactory.getClass().getName());
        return clientFactory;
    }
}
