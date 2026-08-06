package com.eagle.gateway.config;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.event.EventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 网关 OpenAPI 聚合配置。
 *
 * <p>下游服务在注册中心实例 metadata 中声明 {@code spring-doc=<alias>} 即可被聚合，无需在网关侧维护服务清单。
 * 聚合 URL 形如 {@code /v3/api-docs/{alias}}，由 {@link OpenApiRouteLocator} 转发到
 * {@code lb://<serviceId>/v3/api-docs}。
 *
 * <p>双触发刷新策略：
 * <ul>
 *   <li>{@link ApplicationReadyEvent} —— 启动后扫描一次，保证冷启动可见</li>
 *   <li>{@link HeartbeatEvent} —— 注册中心实例上下线时实时刷新（注册中心无关，
 *       Consul / Eureka / Zookeeper 的 DiscoveryClient 均发布此事件）</li>
 * </ul>
 *
 * @author eagle
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
// 同时要求 swagger-ui 开启：prod 关闭 swagger 时 springdoc 不注册 SwaggerUiConfigProperties，
// 本聚合配置（依赖该 bean 填充 UI 下拉项）此时无事可做，必须一并不加载，否则构造器注入失败启动崩溃。
// 多个 name 为 AND 语义：两者均为 true 或缺省（matchIfMissing）才装配。
@ConditionalOnProperty(
        name = {"eagle.gateway.openapi.discovery-enabled", "springdoc.swagger-ui.enabled"},
        havingValue = "true", matchIfMissing = true)
public class GatewayOpenApiConfig implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * 实例 metadata 中声明 OpenAPI alias 的 key
     */
    public static final String METADATA_KEY = "spring-doc";

    /**
     * 实例 metadata 中可选的展示名 key（缺省按 alias 首字母大写）
     */
    public static final String METADATA_DISPLAY_NAME_KEY = "spring-doc-name";

    /**
     * 网关侧聚合 URL 模板：{@code /v3/api-docs/{alias}}
     */
    public static final String API_DOCS_PATH = "/v3/api-docs";

    private final DiscoveryClient discoveryClient;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    /**
     * 注册中心状态变化时刷新 Swagger URL。
     *
     * <p>{@link HeartbeatEvent} 是 Spring Cloud 的注册中心无关事件 —— Consul / Eureka /
     * Zookeeper 的 DiscoveryClient 在探测到注册表变更时都会发布它，
     * 取代了原先绑死 Nacos 的 {@code NotifyCenter} + {@code InstancesChangeEvent} 订阅。
     *
     * @param event 心跳事件，仅作触发信号，不读取其内容
     */
    @EventListener(HeartbeatEvent.class)
    public void onHeartbeat(HeartbeatEvent event) {
        refresh();
    }

    /**
     * 应用就绪后做一次初始扫描，保证冷启动时即可见已注册的服务。
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        refresh();
    }

    /**
     * 全量扫描所有服务实例，按 metadata.spring-doc 聚合成 Swagger UI 下拉项。
     */
    private void refresh() {
        Set<SwaggerUrl> urls = new LinkedHashSet<>();
        for (String serviceId : discoveryClient.getServices()) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            for (ServiceInstance instance : instances) {
                Map<String, String> metadata = instance.getMetadata();
                if (metadata == null) {
                    continue;
                }
                String alias = metadata.get(METADATA_KEY);
                if (alias == null || alias.isBlank()) {
                    continue;
                }
                String displayName = metadata.getOrDefault(METADATA_DISPLAY_NAME_KEY, defaultDisplayName(alias));
                urls.add(new SwaggerUrl(alias, API_DOCS_PATH + "/" + alias, displayName));
                break;   // 同 serviceId 下多实例只取首个，避免重复
            }
        }

        swaggerUiConfigProperties.setUrls(urls);
        if (urls.isEmpty()) {
            log.warn("No downstream services advertise 'spring-doc' metadata; Swagger UI aggregation is empty");
        } else {
            log.info("OpenAPI aggregation refreshed with {} service(s): {}", urls.size(),
                    urls.stream().map(SwaggerUrl::getName).toList());
        }
    }

    private String defaultDisplayName(String alias) {
        if (alias.isEmpty()) {
            return alias;
        }
        return Character.toUpperCase(alias.charAt(0)) + alias.substring(1);
    }

}
