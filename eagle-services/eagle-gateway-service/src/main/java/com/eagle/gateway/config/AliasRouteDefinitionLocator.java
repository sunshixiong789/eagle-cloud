package com.eagle.gateway.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import reactor.core.publisher.Flux;

/**
 * 基于 {@link DiscoveryClient} 的别名路由生成器。
 *
 * <p>遍历 Nacos 中注册的所有服务（排除网关自身），为每个服务生成
 * {@code Path=/{pathPrefix}/{alias}/**} → {@code lb://{serviceId}} 路由。
 *
 * <p>不做 StripPrefix —— 下游 Controller 沿用自身 {@code /api/{module}/**}
 * 命名（与 {@link GatewayAliasProperties#getPathPrefix()} 拼接后整体匹配 alias 段）。
 * 例如 {@code GET /api/system/users/1} 命中 alias=system 的路由后整段透传给
 * {@code eagle-system-server}，由其 Controller 的 {@code @RequestMapping("/api/system/users")}
 * 处理。
 *
 * <p>路由刷新由 Spring Cloud Gateway 内置的 {@code RouteRefreshListener} 监听
 * Nacos {@code HeartbeatEvent} 触发，新服务上线/下线会自动重新生成路由表。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class AliasRouteDefinitionLocator implements RouteDefinitionLocator {

    private static final String ROUTE_ID_PREFIX = "alias-";

    private final DiscoveryClient discoveryClient;
    private final GatewayAliasProperties properties;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        List<RouteDefinition> definitions = new ArrayList<>();
        for (String serviceId : discoveryClient.getServices()) {
            if (serviceId.equalsIgnoreCase(properties.getSelfServiceId())) {
                continue;
            }
            definitions.add(buildRoute(serviceId));
        }
        return Flux.fromIterable(definitions);
    }

    private RouteDefinition buildRoute(String serviceId) {
        String alias = properties.resolveAlias(serviceId);
        String pattern = properties.getPathPrefix() + "/" + alias + "/**";

        RouteDefinition route = new RouteDefinition();
        route.setId(ROUTE_ID_PREFIX + alias);
        route.setUri(URI.create("lb://" + serviceId));

        PredicateDefinition path = new PredicateDefinition();
        path.setName("Path");
        path.addArg("pattern", pattern);
        route.setPredicates(List.of(path));

        route.setFilters(List.<FilterDefinition>of());

        log.info("Alias route generated: {} → lb://{}", pattern, serviceId);
        return route;
    }
}
