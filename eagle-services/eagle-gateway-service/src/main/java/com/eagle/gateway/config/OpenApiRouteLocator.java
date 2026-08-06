package com.eagle.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 网关 OpenAPI 转发路由：把 {@code /v3/api-docs/{alias}} 转发到 {@code lb://<serviceId>/v3/api-docs}。
 *
 * <p>{@link GatewayOpenApiConfig} 负责把聚合 URL 写入 Swagger UI；本类负责让这些 URL 真的能拉到下游 JSON。
 * 路由项基于注册中心实例 metadata 中的 {@code spring-doc=<alias>} 动态生成，配置由下游服务声明，
 * 网关侧无需维护服务清单。
 *
 * <p>SCG {@link RouteDefinitionLocator} 在每次匹配请求时被读取，因此实例上下线后无需手动刷新路由表。
 *
 * @author eagle
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eagle.gateway.openapi.discovery-enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiRouteLocator implements RouteDefinitionLocator {

    private static final String ROUTE_ID_PREFIX = "openapi-";

    private final DiscoveryClient discoveryClient;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        Set<String> seen = new HashSet<>();
        List<RouteDefinition> defs = new ArrayList<>();
        for (String serviceId : discoveryClient.getServices()) {
            for (ServiceInstance instance : discoveryClient.getInstances(serviceId)) {
                Map<String, String> metadata = instance.getMetadata();
                if (metadata == null) {
                    continue;
                }
                String alias = metadata.get(GatewayOpenApiConfig.METADATA_KEY);
                if (alias == null || alias.isBlank() || !seen.add(alias)) {
                    continue;
                }
                defs.add(buildRoute(serviceId, alias));
                break;
            }
        }
        return Flux.fromIterable(defs);
    }

    /**
     * 构造 {@code /v3/api-docs/{alias}} → {@code lb://<serviceId>/v3/api-docs} 路由。
     *
     * <p>使用 {@code SetPath} 过滤器把请求路径重写为下游真实的 {@code /v3/api-docs}，
     * 避免下游 controller 需要识别 alias 段。
     */
    private RouteDefinition buildRoute(String serviceId, String alias) {
        String pattern = GatewayOpenApiConfig.API_DOCS_PATH + "/" + alias;

        RouteDefinition route = new RouteDefinition();
        route.setId(ROUTE_ID_PREFIX + alias);
        route.setUri(URI.create("lb://" + serviceId));

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        predicate.addArg("pattern", pattern);
        route.setPredicates(List.of(predicate));

        FilterDefinition setPath = new FilterDefinition();
        setPath.setName("SetPath");
        setPath.addArg("template", GatewayOpenApiConfig.API_DOCS_PATH);
        route.setFilters(List.of(setPath));

        log.debug("OpenAPI route: {} -> lb://{}{}", pattern, serviceId, GatewayOpenApiConfig.API_DOCS_PATH);
        return route;
    }
}
