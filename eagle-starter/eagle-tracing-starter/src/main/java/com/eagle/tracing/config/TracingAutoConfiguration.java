package com.eagle.tracing.config;

import com.eagle.tracing.properties.TracingProperties;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * 分布式链路追踪自动配置。
 *
 * <p>集成 Micrometer Tracing + Brave + Zipkin，提供：
 * <ul>
 *   <li>Zipkin OkHttpSender（按需启用，Spring 管理生命周期）</li>
 *   <li>AsyncZipkinSpanHandler（将 Span 异步上报至 Zipkin）</li>
 *   <li>DefaultTracingObservationHandler（回退注册，Spring Boot Actuator 存在时由其提供）</li>
 * </ul>
 *
 * <p>通过 {@code eagle.tracing.enabled=false} 可整体禁用。
 *
 * @author eagle
 */
@Slf4j
@AutoConfiguration(
        afterName = {
                "org.springframework.boot.actuate.autoconfigure.tracing.brave.BraveAutoConfiguration",
                "org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration"
        }
)
@ConditionalOnClass(Tracer.class)
@EnableConfigurationProperties(TracingProperties.class)
public class TracingAutoConfiguration {

    /**
     * 回退注册 DefaultTracingObservationHandler。
     * 当 spring-boot-starter-actuator 存在时，Spring Boot 的
     * MicrometerTracingAutoConfiguration 已注册此 Bean，此处不重复创建。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(Tracer.class)
    public DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
        return new DefaultTracingObservationHandler(tracer);
    }

    /**
     * Zipkin 上报配置，仅在配置了 {@code eagle.tracing.zipkin.endpoint} 时激活。
     * OkHttpSender 和 AsyncZipkinSpanHandler 均注册为 Spring Bean，确保 Context 关闭时资源正确释放。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "eagle.tracing.zipkin.endpoint")
    static class ZipkinConfiguration {

        /**
         * Zipkin HTTP 发送器，Context 关闭时由 Spring 调用 {@code close()} 释放连接池。
         */
        @Bean
        @ConditionalOnMissingBean(BytesMessageSender.class)
        public OkHttpSender okHttpSender(TracingProperties properties) {
            String endpoint = properties.getZipkin().getEndpoint();
            log.info("[Eagle Tracing] Zipkin enabled, endpoint: {}", endpoint);
            return OkHttpSender.create(endpoint);
        }

        /**
         * Zipkin Span Handler，将 Trace 数据异步写入 Zipkin。
         * 依赖上方 {@link OkHttpSender} Bean，Context 关闭时自动 flush 并释放。
         */
        @Bean
        @ConditionalOnMissingBean
        public AsyncZipkinSpanHandler zipkinSpanHandler(BytesMessageSender sender) {
            return AsyncZipkinSpanHandler.newBuilder(sender).build();
        }
    }
}
