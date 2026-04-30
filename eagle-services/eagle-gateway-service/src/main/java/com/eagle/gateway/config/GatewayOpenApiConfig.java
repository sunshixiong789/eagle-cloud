package com.eagle.gateway.config;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gateway OpenAPI 聚合配置。
 *
 * <p>监听 {@link ApplicationReadyEvent} 代替 {@code @PostConstruct}，
 * 确保 Nacos 服务注册完成后再发起服务发现，避免启动时竞争条件导致服务列表为空。
 *
 * @author 孙士雄
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eagle.gateway.openapi.discovery-enabled", havingValue = "true", matchIfMissing = true)
public class GatewayOpenApiConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final String GATEWAY_NAME = "eagle-gateway-server";
    private static final String API_DOCS_PATH = "/v3/api-docs";
    private final NacosDiscoveryClient discoveryClient;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    /**
     * 应用完全就绪后从 Nacos 发现服务，注册到 Swagger UI 聚合 URLs。
     *
     * <p>使用 {@link ApplicationReadyEvent} 而非 {@code @PostConstruct}，
     * 保证 Nacos 客户端已完成服务订阅，不会因启动时序问题返回空列表。
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<String> services = discoveryClient.getServices();
        if (services.isEmpty()) {
            log.warn("No services found in Nacos, OpenAPI aggregation will be empty");
            return;
        }

        Set<SwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();
        for (String serviceId : services) {
            if (GATEWAY_NAME.equals(serviceId)) {
                continue;
            }
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances.isEmpty()) {
                continue;
            }

            // name 作为 Swagger UI 下拉框的唯一 key，displayName 作为展示标签
            String name = serviceId;
            String displayName = formatServiceName(serviceId);
            String url = API_DOCS_PATH + "/" + serviceId;
            urls.add(new SwaggerUiConfigProperties.SwaggerUrl(name, url, displayName));
            log.info("OpenAPI aggregated service: {} -> {}", displayName, url);
        }

        if (!urls.isEmpty()) {
            swaggerUiConfigProperties.setUrls(urls);
            log.info("OpenAPI aggregation configured with {} services", urls.size());
        }
    }

    /**
     * 将服务 ID 转为可读展示名，如 "eagle-system-server" → "System Server"。
     */
    private String formatServiceName(String serviceId) {
        String stripped = serviceId.replace("eagle-", "").replace("-server", "");
        String[] parts = stripped.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
