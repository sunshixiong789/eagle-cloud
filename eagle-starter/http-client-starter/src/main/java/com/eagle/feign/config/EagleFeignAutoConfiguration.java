package com.eagle.feign.config;

import com.eagle.feign.decoder.FeignErrorDecoder;
import com.eagle.feign.interceptor.FeignAuthInterceptor;
import com.eagle.feign.interceptor.FeignTenantInterceptor;
import com.eagle.feign.interceptor.SeataXidRequestInterceptor;
import com.eagle.feign.properties.FeignProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.PageableSpringQueryMapEncoder;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
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
     * HTTP 错误解码器：解析下游 {@code ErrorResult} JSON，提取真实 message 透传给调用方。
     *
     * <p>注入 Spring 容器的 {@link ObjectMapper}（含 {@link PageJacksonModule} 等定制），
     * 确保与全局 Jackson 配置一致。
     */
    @Bean
    @ConditionalOnMissingBean
    public FeignErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        return new FeignErrorDecoder(objectMapper);
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

    // ==================== Spring Data 分页支持 ====================

    /**
     * Spring Data {@code Page<T>} 反序列化支持。
     *
     * <p>{@code Page} 是接口，Jackson 默认无法实例化。注册此 Module 后，
     * Feign 客户端可以将下游返回的分页 JSON 直接反序列化为 {@code Page<T>}：
     * <pre>{@code
     * @FeignClient(name = "eagle-inventory-server")
     * public interface InventoryFeignClient {
     *     @GetMapping("/items")
     *     Page<ItemResponse> findItems(@SpringQueryMap Pageable pageable);
     * }
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.data.domain.Page")
    public PageJacksonModule pageJacksonModule() {
        return new PageJacksonModule();
    }

    /**
     * Spring Data {@code Sort} 反序列化支持。
     *
     * <p>与 {@link PageJacksonModule} 配套使用，使嵌套在 {@code Page} 中的
     * {@code Sort} 字段可以正确反序列化。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.data.domain.Sort")
    public SortJacksonModule sortJacksonModule() {
        return new SortJacksonModule();
    }

    /**
     * {@code Pageable} 查询参数编码器。
     *
     * <p>使 {@code Pageable} 对象可以通过 {@code @SpringQueryMap} 注解展开为
     * {@code page=0&size=20&sort=name,asc} 格式的查询参数：
     * <pre>{@code
     * Page<ItemResponse> findItems(@SpringQueryMap Pageable pageable);
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean(PageableSpringQueryMapEncoder.class)
    @ConditionalOnClass(name = "org.springframework.data.domain.Pageable")
    public PageableSpringQueryMapEncoder pageableSpringQueryMapEncoder() {
        return new PageableSpringQueryMapEncoder();
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
