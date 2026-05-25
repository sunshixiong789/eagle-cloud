package com.eagle.gateway.config;

import com.alibaba.nacos.client.naming.event.InstancesChangeEvent;
import com.alibaba.nacos.common.notify.Event;
import com.alibaba.nacos.common.notify.NotifyCenter;
import com.alibaba.nacos.common.notify.listener.Subscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.InitializingBean;
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
 * <p>下游服务在 Nacos 实例 metadata 中声明 {@code spring-doc=<alias>} 即可被聚合，无需在网关侧维护服务清单。
 * 聚合 URL 形如 {@code /v3/api-docs/{alias}}，由 {@link OpenApiRouteLocator} 转发到
 * {@code lb://<serviceId>/v3/api-docs}。
 *
 * <p>双触发刷新策略：
 * <ul>
 *   <li>{@link ApplicationReadyEvent} —— 启动后扫描一次，保证冷启动可见</li>
 *   <li>Nacos {@link InstancesChangeEvent} —— 实例上下线时实时刷新（参考 merchant-service 的
 *       {@code SwaggerDocRegister}）</li>
 * </ul>
 *
 * @author eagle
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eagle.gateway.openapi.discovery-enabled", havingValue = "true", matchIfMissing = true)
public class GatewayOpenApiConfig implements InitializingBean, ApplicationListener<ApplicationReadyEvent> {

    /**
     * Nacos 实例 metadata 中声明 OpenAPI alias 的 key
     */
    public static final String METADATA_KEY = "spring-doc";

    /**
     * Nacos 实例 metadata 中可选的展示名 key（缺省按 alias 首字母大写）
     */
    public static final String METADATA_DISPLAY_NAME_KEY = "spring-doc-name";

    /**
     * 网关侧聚合 URL 模板：{@code /v3/api-docs/{alias}}
     */
    public static final String API_DOCS_PATH = "/v3/api-docs";

    private final DiscoveryClient discoveryClient;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    /**
     * 注册 Nacos InstancesChangeEvent 订阅者，实现实例上下线后的 Swagger URL 实时刷新。
     */
    @Override
    public void afterPropertiesSet() {
        NotifyCenter.registerSubscriber(new InstancesChangeSubscriber());
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

    /**
     * Nacos 实例变化订阅者，触发 Swagger URL 刷新。
     *
     * <p>{@link NotifyCenter} 全局广播 InstancesChangeEvent，订阅 type 即可收到所有 serviceId 的事件，
     * 无需自行注册具体 service listener。
     */
    private final class InstancesChangeSubscriber extends Subscriber<InstancesChangeEvent> {

        @Override
        public void onEvent(InstancesChangeEvent event) {
            refresh();
        }

        @Override
        public Class<? extends Event> subscribeType() {
            return InstancesChangeEvent.class;
        }
    }
}
