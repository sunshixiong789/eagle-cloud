package com.eagle.feign.config;

import com.eagle.feign.decoder.FeignErrorDecoder;
import com.eagle.feign.interceptor.FeignAuthInterceptor;
import feign.Logger;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Eagle Feign 自动配置类。
 *
 * @author 孙士雄
 */
@AutoConfiguration(after = FeignAutoConfiguration.class)
@ConditionalOnClass(feign.Feign.class)
public class EagleFeignAutoConfiguration {

    /**
     * Feign 请求拦截器：透传 JWT Token 和链路追踪信息。
     *
     * @param tracerProvider Tracer provider（可选，tracing starter 未引入时为 null）
     * @return FeignAuthInterceptor
     */
    @Bean
    public FeignAuthInterceptor feignAuthInterceptor(ObjectProvider<Tracer> tracerProvider) {
        Tracer tracer = tracerProvider.getIfAvailable();
        return new FeignAuthInterceptor(tracer);
    }

    /**
     * Feign 错误解码器：将下游异常转换为 AppException。
     *
     * @return FeignErrorDecoder
     */
    @Bean
    public FeignErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }

    /**
     * Feign 日志级别配置为 BASIC。
     *
     * @return Logger.Level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
