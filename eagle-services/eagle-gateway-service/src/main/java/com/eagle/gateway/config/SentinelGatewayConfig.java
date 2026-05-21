package com.eagle.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sentinel 网关限流配置。
 *
 * <p>Sentinel Gateway Filter 由 {@code SentinelSCGAutoConfiguration} 自动注册，
 * 本配置仅自定义限流后的响应处理器。
 *
 * @author 孙士雄
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewaySentinelProperties.class)
@RequiredArgsConstructor
public class SentinelGatewayConfig {

    private final GatewaySentinelProperties properties;

    /**
     * 自定义限流响应处理器。
     */
    @PostConstruct
    public void init() {
        loadDefaultRules();

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

    private void loadDefaultRules() {
        if (!properties.isEnabled() || properties.getRules().isEmpty()) {
            log.info("Gateway Sentinel default rules disabled");
            return;
        }

        Set<ApiDefinition> apiDefinitions = new HashSet<>();
        List<GatewayFlowRule> rules = properties.getRules().stream()
                .peek(rule -> registerApiDefinition(rule, apiDefinitions))
                .map(this::toGatewayFlowRule)
                .collect(Collectors.toList());

        if (!apiDefinitions.isEmpty()) {
            GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);
        }
        GatewayRuleManager.loadRules(new HashSet<>(rules));
        log.info("Gateway Sentinel default rules loaded, apiDefinitions={}, rules={}",
                apiDefinitions.size(), rules.size());
    }

    private void registerApiDefinition(GatewaySentinelProperties.Rule rule, Set<ApiDefinition> apiDefinitions) {
        if (rule.getResourceMode() != GatewaySentinelProperties.ResourceMode.API
                || rule.getPathPrefixes().isEmpty()) {
            return;
        }

        Set<ApiPredicateItem> predicateItems = rule.getPathPrefixes().stream()
                .map(pattern -> new ApiPathPredicateItem()
                        .setPattern(pattern)
                        .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX))
                .collect(Collectors.toSet());
        apiDefinitions.add(new ApiDefinition(rule.getResource()).setPredicateItems(predicateItems));
    }

    private GatewayFlowRule toGatewayFlowRule(GatewaySentinelProperties.Rule rule) {
        GatewayFlowRule gatewayRule = new GatewayFlowRule(rule.getResource())
                .setResourceMode(toSentinelResourceMode(rule.getResourceMode()))
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(rule.getQps())
                .setIntervalSec(rule.getIntervalSec())
                .setBurst(rule.getBurst())
                .setMaxQueueingTimeoutMs(rule.getMaxQueueingTimeoutMs());

        if (rule.isRateLimiter()) {
            gatewayRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
        }
        return gatewayRule;
    }

    private int toSentinelResourceMode(GatewaySentinelProperties.ResourceMode resourceMode) {
        if (resourceMode == GatewaySentinelProperties.ResourceMode.ROUTE) {
            return SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID;
        }
        return SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME;
    }
}
