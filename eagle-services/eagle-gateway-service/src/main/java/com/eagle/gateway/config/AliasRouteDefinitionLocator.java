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
 * <p>为 Nacos 中每个非自身服务生成两条路由：
 * <ol>
 *   <li><b>业务别名路由</b> {@code /api/{alias}/**} → {@code lb://{serviceId}}（不 StripPrefix，
 *       下游 Controller 沿用自身 {@code /api/{alias}/**} 命名整段透传）。
 *       例如 {@code GET /api/system/users/1} 命中 alias=system 后整段透传给
 *       {@code eagle-system-server}，由其 {@code @RequestMapping("/api/system/users")} 处理。</li>
 *   <li><b>OpenAPI 聚合路由</b> {@code /v3/api-docs/{alias}} → {@code lb://{serviceId}/v3/api-docs}
 *       （用 {@code SetPath} 把 alias 段还原为下游 SpringDoc 默认路径），供
 *       {@link GatewayOpenApiConfig} 注册的 Swagger UI 聚合下拉 URL 抓取文档。</li>
 * </ol>
 *
 * <p>路由刷新由 Spring Cloud Gateway 内置的 {@code RouteRefreshListener} 监听
 * Nacos {@code HeartbeatEvent} 触发，新服务上线/下线会自动重新生成路由表。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class AliasRouteDefinitionLocator implements RouteDefinitionLocator {

    private static final String BIZ_ROUTE_ID_PREFIX = "alias-";
    private static final String DOCS_ROUTE_ID_PREFIX = "api-docs-";
    private static final String API_DOCS_PATH = "/v3/api-docs";

    private final DiscoveryClient discoveryClient;
    private final GatewayAliasProperties properties;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        List<RouteDefinition> definitions = new ArrayList<>();
        for (String serviceId : discoveryClient.getServices()) {
            if (serviceId.equalsIgnoreCase(properties.getSelfServiceId())) {
                continue;
            }
            String alias = properties.resolveAlias(serviceId);
            definitions.add(buildBusinessRoute(serviceId, alias));
            definitions.add(buildApiDocsRoute(serviceId, alias));
        }
        return Flux.fromIterable(definitions);
    }

    private RouteDefinition buildBusinessRoute(String serviceId, String alias) {
        String pattern = properties.getPathPrefix() + "/" + alias + "/**";
        RouteDefinition route = newRoute(BIZ_ROUTE_ID_PREFIX + alias, serviceId, pattern);
        route.setFilters(List.<FilterDefinition>of());
        log.info("Alias route generated: {} → lb://{}", pattern, serviceId);
        return route;
    }

    private RouteDefinition buildApiDocsRoute(String serviceId, String alias) {
        String pattern = API_DOCS_PATH + "/" + alias;
        RouteDefinition route = newRoute(DOCS_ROUTE_ID_PREFIX + alias, serviceId, pattern);

        FilterDefinition setPath = new FilterDefinition();
        setPath.setName("SetPath");
        setPath.addArg("template", API_DOCS_PATH);
        route.setFilters(List.of(setPath));

        log.info("API docs route generated: {} → lb://{}{}", pattern, serviceId, API_DOCS_PATH);
        return route;
    }

    private RouteDefinition newRoute(String routeId, String serviceId, String pathPattern) {
        RouteDefinition route = new RouteDefinition();
        route.setId(routeId);
        route.setUri(URI.create("lb://" + serviceId));

        PredicateDefinition path = new PredicateDefinition();
        path.setName("Path");
        path.addArg("pattern", pathPattern);
        route.setPredicates(List.of(path));
        return route;
    }
}
