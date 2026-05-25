package com.eagle.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 网关 Sentinel 默认规则配置项（{@code eagle.gateway.sentinel.*}）。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.gateway.sentinel")
public class GatewaySentinelProperties {

    /**
     * 是否加载网关默认限流规则。
     */
    private boolean enabled = true;

    /**
     * 默认网关流控规则。生产可通过 Nacos / 环境配置覆盖。
     */
    private List<Rule> rules = List.of(
            Rule.api("global-api", List.of("/"), 1800, 300),
            Rule.api("auth-sensitive-api", List.of("/oauth2/", "/.well-known/", "/userinfo", "/logout", "/connect/",
                    "/login"), 600, 100),
            Rule.api("sms-api", List.of("/sms/", "/accounts/register", "/accounts/password/reset"), 80, 20)
    );

    /**
     * 单条 Sentinel Gateway 规则。
     */
    @Data
    public static class Rule {

        /**
         * 资源名：route id 或自定义 API 分组名。
         */
        private String resource;

        /**
         * 资源模式：route 表示 Gateway route id；api 表示自定义 API 分组。
         */
        private ResourceMode resourceMode = ResourceMode.API;

        /**
         * API 分组路径前缀，仅 resourceMode=api 时生效。
         */
        private List<String> pathPrefixes = List.of();

        /**
         * QPS 阈值。
         */
        private double qps;

        /**
         * 统计窗口秒数。
         */
        private long intervalSec = 1;

        /**
         * 允许短时突发请求数。
         */
        private int burst;

        /**
         * 是否启用匀速排队。默认直接快速失败，保护下游。
         */
        private boolean rateLimiter;

        /**
         * 匀速排队最大等待时间。
         */
        private int maxQueueingTimeoutMs = 500;

        private static Rule api(String resource, List<String> pathPrefixes, double qps, int burst) {
            Rule rule = new Rule();
            rule.setResource(resource);
            rule.setResourceMode(ResourceMode.API);
            rule.setPathPrefixes(pathPrefixes);
            rule.setQps(qps);
            rule.setBurst(burst);
            return rule;
        }
    }

    public enum ResourceMode {
        ROUTE,
        API
    }
}
