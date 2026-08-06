package com.eagle.gateway.config;

import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 网关限流的 key 解析策略（Spring Cloud Gateway 内置 {@code RequestRateLimiter} 用）。
 *
 * <p>取代原先的 {@code SentinelGatewayConfig} —— Sentinel 的规则是「路径前缀 + 单机 QPS」，
 * 这里改为「路由 + Redis 令牌桶」，阈值配置见 {@code application.yml} 各 route 的
 * {@code RequestRateLimiter} filter。
 *
 * <p><b>与原实现的关键差异</b>：Redis 后端的令牌桶是<b>集群级</b>计数，
 * 而 Sentinel 的流控规则是每个网关实例各算各的。原先多副本部署时集群总阈值 = 单机阈值 × 副本数，
 * 弹性伸缩会让实际阈值漂移；换成 Redis 后阈值就是集群真实阈值，副本数变化不影响。
 *
 * @author eagle
 */
@Configuration(proxyBeanMethods = false)
public class GatewayRateLimitConfig {

    /**
     * 默认限流维度：客户端 IP。
     *
     * <p>用 {@link XForwardedRemoteAddressResolver#maxTrustedIndex(int)} 只信任最后一跳代理写入的地址，
     * 避免客户端伪造 {@code X-Forwarded-For} 绕过限流。若网关前面串了 N 层可信代理，
     * 把 1 改成 N。
     *
     * @return 以客户端 IP 为 key 的解析器
     */
    @Bean
    @Primary
    public KeyResolver clientIpKeyResolver() {
        XForwardedRemoteAddressResolver resolver =
                XForwardedRemoteAddressResolver.maxTrustedIndex(1);
        return exchange -> Mono.justOrEmpty(resolver.resolve(exchange))
                .map(address -> address.getAddress().getHostAddress())
                .defaultIfEmpty("unknown");
    }
}
