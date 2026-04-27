package com.eagle.tracing.config;

import com.eagle.tracing.properties.TracingProperties;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * 分布式链路追踪自动配置。
 *
 * <p>集成 Micrometer Tracing + Brave + Zipkin，提供：
 * <ul>
 *   <li>Zipkin Span Handler（条件上报）</li>
 *   <li>Observation Registry 增强</li>
 * </ul>
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Tracer.class)
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnProperty(name = "eagle.tracing.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TracingAutoConfiguration {

    /**
     * 配置 Zipkin Span Handler，将 trace 数据上报到 Zipkin。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "eagle.tracing.zipkin.endpoint")
    public AsyncZipkinSpanHandler zipkinSpanHandler(TracingProperties properties) {
        BytesMessageSender sender = OkHttpSender.create(properties.getZipkin().getEndpoint());
        log.info("Zipkin tracing enabled, endpoint: {}", properties.getZipkin().getEndpoint());
        return AsyncZipkinSpanHandler.newBuilder(sender).build();
    }

    /**
     * 为 ObservationRegistry 注册 tracing handler，使所有被观测的方法自动生成 span。
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
        return new DefaultTracingObservationHandler(tracer);
    }
}
