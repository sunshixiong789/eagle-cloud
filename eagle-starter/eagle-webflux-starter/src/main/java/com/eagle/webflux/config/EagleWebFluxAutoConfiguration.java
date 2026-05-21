package com.eagle.webflux.config;

import com.eagle.webflux.filter.RequestIdWebFilter;
import com.eagle.webflux.handler.ReactiveGlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.WebFilter;

/**
 * Eagle WebFlux auto-configuration.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(DispatcherHandler.class)
public class EagleWebFluxAutoConfiguration {

    /**
     * Provides request id propagation for reactive HTTP requests.
     */
    @Bean
    @ConditionalOnMissingBean(RequestIdWebFilter.class)
    public WebFilter requestIdWebFilter() {
        return new RequestIdWebFilter();
    }

    /**
     * Provides unified JSON error responses for WebFlux applications.
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveGlobalExceptionHandler.class)
    public ReactiveGlobalExceptionHandler reactiveGlobalExceptionHandler(
            ObjectMapper objectMapper,
            MessageSource messageSource) {
        return new ReactiveGlobalExceptionHandler(objectMapper, messageSource);
    }
}
