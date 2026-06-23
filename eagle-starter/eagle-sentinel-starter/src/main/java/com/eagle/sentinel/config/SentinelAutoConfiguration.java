package com.eagle.sentinel.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import com.eagle.sentinel.aspect.RateLimitAspect;
import com.eagle.sentinel.handler.EagleSentinelBlockExceptionHandler;
import com.eagle.sentinel.parser.EagleSentinelRequestOriginParser;
import com.eagle.sentinel.properties.SentinelProperties;
import com.eagle.sentinel.rule.SentinelRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * Eagle Sentinel 自动配置。
 *
 * <p>在类路径存在 {@code com.alibaba.csp.sentinel.SphU} 时自动激活，
 * 注册以下组件：
 * <ul>
 *   <li>{@link EagleSentinelBlockExceptionHandler} — 统一流控异常响应处理器（返回 JSON）</li>
 *   <li>{@link EagleSentinelRequestOriginParser} — 从 {@code X-Application-Name} 头解析来源（可选）</li>
 *   <li>{@link RateLimitAspect} — {@code @RateLimit} 注解的 AOP 切面，声明式限流支持</li>
 *   <li>{@link SentinelRuleManager} — 程序化规则管理器，简化动态规则配置 API</li>
 * </ul>
 *
 * @author eagle
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "com.alibaba.csp.sentinel.SphU")
@EnableConfigurationProperties(SentinelProperties.class)
public class SentinelAutoConfiguration {

    /**
     * 统一 Sentinel 流控异常处理器。
     *
     * <p>将 {@link com.alibaba.csp.sentinel.slots.block.BlockException} 各子类型
     * 映射为标准 JSON 错误响应，使用方可通过声明自定义 {@link BlockExceptionHandler} Bean 覆盖。
     *
     * @param objectMapper Boot 托管的 ObjectMapper，用于序列化错误响应
     * @return Eagle Sentinel 流控异常处理器实例
     */
    @Bean
    @ConditionalOnMissingBean(BlockExceptionHandler.class)
    public BlockExceptionHandler blockExceptionHandler(ObjectMapper objectMapper) {
        log.info("[Eagle Sentinel] BlockExceptionHandler registered: EagleSentinelBlockExceptionHandler");
        return new EagleSentinelBlockExceptionHandler(objectMapper);
    }

    /**
     * 请求来源解析器（可选）。
     *
     * <p>从 {@code X-Application-Name} 请求头提取调用方应用名，
     * 用于 Sentinel 授权规则来源匹配。
     * 可通过 {@code eagle.sentinel.origin-parser-enabled=false} 禁用，
     * 或声明自定义 {@link RequestOriginParser} Bean 覆盖。
     *
     * @return Eagle Sentinel 请求来源解析器实例
     */
    @Bean
    @ConditionalOnMissingBean(RequestOriginParser.class)
    @ConditionalOnProperty(
            name = "eagle.sentinel.origin-parser-enabled",
            havingValue = "true",
            matchIfMissing = true)
    public RequestOriginParser requestOriginParser() {
        log.info("[Eagle Sentinel] RequestOriginParser registered: EagleSentinelRequestOriginParser");
        return new EagleSentinelRequestOriginParser();
    }

    /**
     * {@code @RateLimit} 注解的 AOP 切面。
     *
     * <p>拦截标注了 {@link com.eagle.sentinel.annotation.RateLimit} 的方法或类，
     * 首次调用时动态注册 Sentinel 流控规则，后续通过 {@code SphU.entry} 进行资源保护。
     * 可通过声明自定义 {@link RateLimitAspect} Bean 覆盖默认实现。
     *
     * @return RateLimit 切面实例
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    public RateLimitAspect rateLimitAspect() {
        log.info("[Eagle Sentinel] RateLimitAspect registered");
        return new RateLimitAspect();
    }

    /**
     * Sentinel 程序化规则管理器。
     *
     * <p>提供流控规则、熔断规则、热点参数规则的增删 API，
     * 简化在业务代码中动态配置 Sentinel 规则的操作。
     * 可通过声明自定义 {@link SentinelRuleManager} Bean 覆盖。
     *
     * @return SentinelRuleManager 实例
     */
    @Bean
    @ConditionalOnMissingBean(SentinelRuleManager.class)
    public SentinelRuleManager sentinelRuleManager() {
        log.info("[Eagle Sentinel] SentinelRuleManager registered");
        return new SentinelRuleManager();
    }
}
