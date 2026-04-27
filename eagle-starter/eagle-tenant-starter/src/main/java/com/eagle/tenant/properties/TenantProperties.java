package com.eagle.tenant.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多租户配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.tenant")
public class TenantProperties {

    /**
     * 是否启用多租户。
     */
    private boolean enabled = false;

    /**
     * 隔离模式：column（共享库分字段）/ database（独立数据库）。
     */
    private TenantMode mode = TenantMode.COLUMN;

    /**
     * 从请求头中解析租户 ID 的 Header 名称。
     */
    private String headerName = "X-Tenant-Id";

    /**
     * 默认租户 ID（未传 Header 时的降级值）。
     */
    private String defaultTenantId = "0";

    /**
     * 租户隔离模式枚举。
     */
    public enum TenantMode {
        COLUMN,
        DATABASE
    }
}
