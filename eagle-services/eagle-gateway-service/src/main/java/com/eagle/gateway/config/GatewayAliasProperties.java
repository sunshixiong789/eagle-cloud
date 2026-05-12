package com.eagle.gateway.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关服务别名配置。
 *
 * <p>把"路径前缀 → 下游服务名"的映射集中收敛在 yml 中，配合
 * {@link AliasRouteDefinitionLocator} 基于 {@code DiscoveryClient}
 * 在运行时为每个注册到 Nacos 的服务自动生成路由 {@code /api/{alias}/**} →
 * {@code lb://{serviceId}}。
 *
 * <p>未显式配置 mapping 的服务按默认规则推导别名：
 * <ul>
 *   <li>{@code eagle-system-server} → {@code system}</li>
 *   <li>{@code eagle-order-service} → {@code order}</li>
 *   <li>其他原样保留</li>
 * </ul>
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.gateway.alias")
public class GatewayAliasProperties {

    private static final String NAMESPACE_PREFIX = "eagle-";
    private static final String SERVER_SUFFIX = "-server";
    private static final String SERVICE_SUFFIX = "-service";

    /** 是否启用别名自动路由（依赖 DiscoveryClient，本地无注册中心时应关闭） */
    private boolean enabled = true;

    /** 别名前的统一路径前缀，例如 {@code /api}，最终路由为 {@code /api/{alias}/**} */
    private String pathPrefix = "/api";

    /** 网关自身服务名，匹配的实例不会被生成路由 */
    private String selfServiceId = "eagle-gateway-server";

    /** 显式别名映射：alias → serviceId（覆盖默认推导规则） */
    private Map<String, String> mappings = new HashMap<>();

    /**
     * 由 serviceId 解析其对外暴露的 alias：优先反查显式 mappings，否则用默认规则。
     */
    public String resolveAlias(String serviceId) {
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(serviceId)) {
                return entry.getKey();
            }
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
}
