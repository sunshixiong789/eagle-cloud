package com.eagle.example.integration.httpclient;

import com.eagle.http.client.support.EagleHttpServiceClientFactory;
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

    private final EagleHttpServiceClientFactory clientFactory;

    public EagleHttpServiceClientFactory getFactory() {
        log.info("[HttpClient] Factory type: {}", clientFactory.getClass().getName());
        return clientFactory;
    }
}
