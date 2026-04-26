package com.eagle.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sentinel 网关限流配置。
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

    /**
     * Sentinel Gateway Filter（必须注册为 GlobalFilter）。
     *
     * @return GlobalFilter
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }
}
