package com.eagle.gateway.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

/**
 * 网关路由统一配置。
 *
 * <p>所有下游服务的路由由 {@link ServiceRouteLocator} 基于 Nacos {@link DiscoveryClient}
 * 在运行时自动生成，application.yml 不再需要任何
 * {@code spring.cloud.gateway.server.webflux.routes} 静态条目。每个非自身服务最多生成 4 类路由：
 * <ol>
 *   <li><b>业务别名</b>：{@code /{pathPrefix}/{alias}/**} → {@code lb://{serviceId}}（整段透传，
 *       下游 Controller 沿用自身 {@code /api/{alias}/...} 命名）</li>
 *   <li><b>OpenAPI 聚合</b>：{@code /v3/api-docs/{alias}} → {@code lb://{serviceId}/v3/api-docs}
 *       （供 {@link GatewayOpenApiConfig} 注册的 Swagger UI 下拉抓取文档）</li>
 *   <li><b>WebSocket</b>（可选）：{@link RouteProperties.ServiceRoute#wsPaths} 每条 →
 *       {@code lb:ws://{serviceId}}</li>
 *   <li><b>协议固定</b>（可选）：{@link RouteProperties.ServiceRoute#fixedPaths} 每条 →
 *       {@code lb://{serviceId}}，用于 OAuth2 / OIDC 等不能套 alias 前缀的标准路径</li>
 * </ol>
 *
 * @author 孙士雄
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(GatewayRouteConfig.RouteProperties.class)
@ConditionalOnProperty(name = "eagle.gateway.routes.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayRouteConfig {

    private final DiscoveryClient discoveryClient;
    private final RouteProperties properties;

    @Bean
    public RouteDefinitionLocator gatewayServiceRouteLocator() {
        return new ServiceRouteLocator(discoveryClient, properties);
    }

    // ===== Properties ============================================================================

    /**
     * 路由配置。{@link #services} 中按 {@code serviceId} 声明每个下游服务的扩展信息
     * （alias / wsPaths / fixedPaths）。未声明的服务自动按命名规则推导 alias 并生成
     * 业务别名 + OpenAPI 聚合两条基础路由。
     */
    @Data
    @ConfigurationProperties(prefix = "eagle.gateway.routes")
    public static class RouteProperties {

        private static final String NAMESPACE_PREFIX = "eagle-";
        private static final String SERVER_SUFFIX = "-server";
        private static final String SERVICE_SUFFIX = "-service";

        /** 是否启用网关路由自动生成（本地无注册中心场景设为 false） */
        private boolean enabled = true;

        /** 业务别名路由的统一路径前缀，默认 {@code /api}（最终为 {@code /api/{alias}/**}） */
        private String pathPrefix = "/api";

        /** 网关自身服务名，匹配到时跳过路由生成 */
        private String selfServiceId = "eagle-gateway-server";

        /** 按 {@code serviceId} 声明的路由扩展配置 */
        private Map<String, ServiceRoute> services = new LinkedHashMap<>();

        /**
         * 由 serviceId 解析 alias：优先取 {@link #services} 显式配置，否则按命名规则推导
         * （剥离 {@code eagle-} 前缀，再剥离 {@code -server} / {@code -service} 后缀）。
         */
        public String resolveAlias(String serviceId) {
            ServiceRoute cfg = services.get(serviceId);
            if (cfg != null && cfg.getAlias() != null && !cfg.getAlias().isBlank()) {
                return cfg.getAlias();
            }
            String s = serviceId.toLowerCase();
            if (s.startsWith(NAMESPACE_PREFIX)) {
                s = s.substring(NAMESPACE_PREFIX.length());
            }
            if (s.endsWith(SERVER_SUFFIX)) {
                s = s.substring(0, s.length() - SERVER_SUFFIX.length());
            } else if (s.endsWith(SERVICE_SUFFIX)) {
                s = s.substring(0, s.length() - SERVICE_SUFFIX.length());
            }
            return s;
        }

        public ServiceRoute getServiceRoute(String serviceId) {
            return services.get(serviceId);
        }

        /** 单个服务的扩展路由声明 */
        @Data
        public static class ServiceRoute {
            /** 别名覆盖；缺省时由命名规则推导 */
            private String alias;
            /** WebSocket 路径（如 {@code /ws-stomp/**}），每条生成 lb:ws:// 路由 */
            private List<String> wsPaths = new ArrayList<>();
            /**
             * 协议固定路径（OAuth2 的 {@code /oauth2/**}、{@code /login}、{@code /logout}、
             * {@code /.well-known/**} 等），不能套 alias 前缀；每条生成 lb:// 路由。
             */
            private List<String> fixedPaths = new ArrayList<>();
            /**
             * 仅作用于"业务别名路由"的 filter 声明列表，结构化为 {@link FilterSpec}（name + args）。
             *
             * <p>YAML 示例（Retry 多值数组自动展开为 {@code statuses[0]} / {@code statuses[1]} ...，
             * SCG 短 DSL 的 {@code ,} 分隔不支持数组多值，这里用结构化形式直接绑定到 Factory Config）：
             * <pre>{@code
             * filters:
             *   - name: Retry
             *     args:
             *       retries: 3
             *       statuses: [BAD_GATEWAY, GATEWAY_TIMEOUT, SERVICE_UNAVAILABLE]
             *       methods: [GET]
             * }</pre>
             *
             * <p>不应用于 ws / fixed / api-docs：fixed 多为 OAuth2 POST 不可重试，ws 是长连接，
             * api-docs 无重试必要。
             */
            private List<FilterSpec> filters = new ArrayList<>();
        }

        /**
         * 结构化 Filter 声明：name 对应 SCG GatewayFilterFactory 名称，args 直接绑定到 Factory Config。
         * args 的 value 支持标量（String / Number / Boolean）与 List；List 在转 {@link FilterDefinition}
         * 时自动展开为 {@code key[0]} / {@code key[1]} ... 满足 SCG 数组参数绑定语义。
         */
        @Data
        public static class FilterSpec {
            /** Filter 名称（如 {@code Retry} / {@code AddRequestHeader}） */
            private String name;
            /** Filter 参数：List 值会被自动展开为带下标的字符串参数 */
            private Map<String, Object> args = new LinkedHashMap<>();
        }
    }

    // ===== Locator ==============================================================================

    @Slf4j
    @RequiredArgsConstructor
    static class ServiceRouteLocator implements RouteDefinitionLocator {

        private static final String BIZ_PREFIX = "alias-";
        private static final String DOCS_PREFIX = "api-docs-";
        private static final String WS_PREFIX = "ws-";
        private static final String FIXED_PREFIX = "fixed-";
        private static final String API_DOCS_PATH = "/v3/api-docs";

        private final DiscoveryClient discoveryClient;
        private final RouteProperties properties;

        @Override
        public Flux<RouteDefinition> getRouteDefinitions() {
            List<RouteDefinition> defs = new ArrayList<>();
            for (String serviceId : discoveryClient.getServices()) {
                if (serviceId.equalsIgnoreCase(properties.getSelfServiceId())) {
                    continue;
                }
                String alias = properties.resolveAlias(serviceId);
                RouteProperties.ServiceRoute cfg = properties.getServiceRoute(serviceId);

                defs.add(buildBusiness(serviceId, alias, cfg));
                defs.add(buildApiDocs(serviceId, alias));

                if (cfg == null) {
                    continue;
                }
                int wsIdx = 0;
                for (String p : cfg.getWsPaths()) {
                    defs.add(buildWs(serviceId, alias, p, wsIdx++));
                }
                int fxIdx = 0;
                for (String p : cfg.getFixedPaths()) {
                    defs.add(buildFixed(serviceId, alias, p, fxIdx++));
                }
            }
            return Flux.fromIterable(defs);
        }

        private RouteDefinition buildBusiness(String serviceId, String alias, RouteProperties.ServiceRoute cfg) {
            String pattern = properties.getPathPrefix() + "/" + alias + "/**";
            RouteDefinition r = newRoute(BIZ_PREFIX + alias, "lb://" + serviceId, pattern);
            if (cfg != null && !cfg.getFilters().isEmpty()) {
                List<FilterDefinition> filters = new ArrayList<>(cfg.getFilters().size());
                for (RouteProperties.FilterSpec spec : cfg.getFilters()) {
                    filters.add(toFilterDefinition(spec));
                }
                r.setFilters(filters);
                log.info("Alias route: {} → lb://{} (filters={})", pattern, serviceId, filters);
            } else {
                log.info("Alias route: {} → lb://{}", pattern, serviceId);
            }
            return r;
        }

        /**
         * 把结构化 {@link RouteProperties.FilterSpec} 转为 {@link FilterDefinition}：标量直接 toString，
         * List 展开为 {@code key[0]} / {@code key[1]} ... 以匹配 SCG ConfigurationService 的索引绑定。
         */
        private FilterDefinition toFilterDefinition(RouteProperties.FilterSpec spec) {
            FilterDefinition fd = new FilterDefinition();
            fd.setName(spec.getName());
            for (Map.Entry<String, Object> entry : spec.getArgs().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof List<?> list) {
                    for (int i = 0; i < list.size(); i++) {
                        fd.addArg(key + "[" + i + "]", String.valueOf(list.get(i)));
                    }
                } else if (value != null) {
                    fd.addArg(key, String.valueOf(value));
                }
            }
            return fd;
        }

        private RouteDefinition buildApiDocs(String serviceId, String alias) {
            String pattern = API_DOCS_PATH + "/" + alias;
            RouteDefinition r = newRoute(DOCS_PREFIX + alias, "lb://" + serviceId, pattern);
            FilterDefinition setPath = new FilterDefinition();
            setPath.setName("SetPath");
            setPath.addArg("template", API_DOCS_PATH);
            r.setFilters(List.of(setPath));
            log.info("API docs route: {} → lb://{}{}", pattern, serviceId, API_DOCS_PATH);
            return r;
        }

        private RouteDefinition buildWs(String serviceId, String alias, String path, int idx) {
            RouteDefinition r = newRoute(WS_PREFIX + alias + "-" + idx, "lb:ws://" + serviceId, path);
            log.info("WebSocket route: {} → lb:ws://{}", path, serviceId);
            return r;
        }

        private RouteDefinition buildFixed(String serviceId, String alias, String path, int idx) {
            RouteDefinition r = newRoute(FIXED_PREFIX + alias + "-" + idx, "lb://" + serviceId, path);
            log.info("Fixed-path route: {} → lb://{}", path, serviceId);
            return r;
        }

        private RouteDefinition newRoute(String routeId, String uri, String pathPattern) {
            RouteDefinition route = new RouteDefinition();
            route.setId(routeId);
            route.setUri(URI.create(uri));

            PredicateDefinition path = new PredicateDefinition();
            path.setName("Path");
            path.addArg("pattern", pathPattern);
            route.setPredicates(List.of(path));
            route.setFilters(List.<FilterDefinition>of());
            return route;
        }
    }
}
