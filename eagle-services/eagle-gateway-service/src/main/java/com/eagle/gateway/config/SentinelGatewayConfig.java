package com.eagle.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Sentinel 网关限流配置。
 *
 * <p>Sentinel Gateway Filter 由 {@code SentinelSCGAutoConfiguration} 自动注册，
 * 本配置仅自定义限流后的响应处理器。
 *
 * @author 孙士雄
 */
@Slf4j
@Configuration
public class SentinelGatewayConfig {

    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelGatewayConfig(ServerCodecConfigurer serverCodecConfigurer) {
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * 自定义限流响应处理器。
     */
    @PostConstruct
    public void init() {
        BlockRequestHandler handler = (exchange, throwable) -> {
            log.warn("Gateway request blocked by Sentinel, path: {}",
                    exchange.getRequest().getURI().getPath());

            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            error.put("error", "Too Many Requests");
            error.put("message", "请求过于频繁，请稍后重试");

            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(error));
        };
        GatewayCallbackManager.setBlockHandler(handler);
    }
}
