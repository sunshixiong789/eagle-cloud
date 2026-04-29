package com.eagle.feign.config;

import com.eagle.feign.decoder.FeignErrorDecoder;
import com.eagle.feign.interceptor.FeignAuthInterceptor;
import com.eagle.feign.interceptor.FeignTenantInterceptor;
import com.eagle.feign.interceptor.SeataXidRequestInterceptor;
import com.eagle.feign.properties.FeignProperties;
import feign.Logger;
import feign.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Eagle Feign 自动配置。
 *
 * <p>注册以下全局组件：
 * <ul>
 *   <li>{@link FeignAuthInterceptor} — 透传 Authorization / Accept-Language</li>
 *   <li>{@link FeignErrorDecoder} — HTTP 错误 → 类型化 AppException</li>
 *   <li>{@link Logger.Level} — 可通过 {@code eagle.feign.log-level} 配置</li>
 *   <li>{@link Request.Options} — 可通过 {@code eagle.feign.connect-timeout} / {@code read-timeout} 配置</li>
 *   <li>{@link SeataXidRequestInterceptor} — Seata XID 透传（仅 Seata 存在时）</li>
 *   <li>{@link FeignTenantInterceptor} — 租户 ID 透传（仅 eagle-tenant-starter 存在时）</li>
 * </ul>
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration(after = FeignAutoConfiguration.class)
@ConditionalOnClass(feign.Feign.class)
@EnableConfigurationProperties(FeignProperties.class)
public class EagleFeignAutoConfiguration {

    /**
     * Authorization / Accept-Language 透传拦截器。
     */
    @Bean
    @ConditionalOnMissingBean
    public FeignAuthInterceptor feignAuthInterceptor() {
        return new FeignAuthInterceptor();
    }

    /**
     * HTTP 错误解码器：将下游 HTTP 错误响应转换为 AppException 体系异常。
     */
    @Bean
    @ConditionalOnMissingBean
    public FeignErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }

    /**
     * 全局 Feign 日志级别，可通过 {@code eagle.feign.log-level} 覆盖。
     */
    @Bean
    @ConditionalOnMissingBean(Logger.Level.class)
    public Logger.Level feignLoggerLevel(FeignProperties properties) {
        log.info("Feign log level: {}", properties.getLogLevel());
        return properties.getLogLevel();
    }

    /**
     * 全局连接/读取超时配置，可通过 {@code eagle.feign.connect-timeout} 和
     * {@code eagle.feign.read-timeout} 覆盖。
     */
    @Bean
    @ConditionalOnMissingBean(Request.Options.class)
    public Request.Options feignRequestOptions(FeignProperties properties) {
        log.info("Feign timeouts: connectTimeout={}ms, readTimeout={}ms",
                properties.getConnectTimeout(), properties.getReadTimeout());
        return new Request.Options(
                properties.getConnectTimeout(), TimeUnit.MILLISECONDS,
                properties.getReadTimeout(), TimeUnit.MILLISECONDS,
                true);
    }

    /**
     * Seata XID 透传（仅在 seata-spring-boot-starter 存在于类路径时注册）。
     *
     * <p>使用嵌套 {@code @Configuration} + {@code @ConditionalOnClass(name = "...")} 保证
     * 在 Seata 不存在时不触发 {@link SeataXidRequestInterceptor} 的类加载。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.seata.core.context.RootContext")
    static class SeataConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SeataXidRequestInterceptor seataXidRequestInterceptor() {
            log.info("Seata XID propagation enabled");
            return new SeataXidRequestInterceptor();
        }
    }

    /**
     * 租户 ID 透传（仅在 eagle-tenant-starter 存在于类路径时注册）。
     *
     * <p>使用嵌套 {@code @Configuration} + {@code @ConditionalOnClass(name = "...")} 保证
     * 在 eagle-tenant-starter 不存在时不触发 {@link FeignTenantInterceptor} 的类加载。
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.eagle.tenant.TenantContextHolder")
    static class TenantConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public FeignTenantInterceptor feignTenantInterceptor() {
            log.info("Feign tenant ID propagation enabled");
            return new FeignTenantInterceptor();
        }
    }
}
