package com.eagle.gateway.config;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gateway OpenAPI 聚合配置。
 *
 * <p>从 Nacos 服务发现动态获取服务列表，注册到 SpringDoc Swagger UI 聚合展示。
 *
 * @author 孙士雄
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eagle.gateway.openapi.discovery-enabled", havingValue = "true", matchIfMissing = true)
public class GatewayOpenApiConfig {

    private final NacosDiscoveryClient discoveryClient;
    private final org.springdoc.core.properties.SwaggerUiConfigProperties swaggerUiConfigProperties;

    private static final String GATEWAY_NAME = "eagle-gateway-server";
    private static final String API_DOCS_PATH = "/v3/api-docs";

    /**
     * 从 Nacos 发现服务并注册到 Swagger UI URLs。
     */
    @PostConstruct
    public void init() {
        List<String> services = discoveryClient.getServices();
        if (services.isEmpty()) {
            log.warn("No services found in Nacos, OpenAPI aggregation will be empty");
            return;
        }

        Set<org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();
        for (String serviceId : services) {
            if (GATEWAY_NAME.equals(serviceId)) {
                continue;
            }

            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances.isEmpty()) {
                continue;
            }

            String displayName = formatServiceName(serviceId);
            String url = API_DOCS_PATH + "/" + serviceId;
            urls.add(new org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl(displayName, url, serviceId));
            log.info("OpenAPI aggregated service: {} -> {}", displayName, url);
        }

        if (!urls.isEmpty()) {
            swaggerUiConfigProperties.setUrls(urls);
            log.info("OpenAPI aggregation configured with {} services", urls.size());
        }
    }

    /**
     * 格式化服务名用于展示。
     */
    private String formatServiceName(String serviceId) {
        return serviceId.replace("eagle-", "")
                .replace("-", " ")
                .toUpperCase();
    }
}
