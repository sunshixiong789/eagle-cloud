package com.eagle.seata.properties;

import com.eagle.seata.config.SeataEnvironmentPostProcessor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Seata 分布式事务配置属性。
 *
 * <p>示例配置（application.yml）：
 * <pre>
 * eagle:
 *   seata:
 *     enabled: true
 *     application-id: eagle-order-server
 *     tx-service-group: eagle_tx_group
 * </pre>
 *
 * <p>{@link SeataEnvironmentPostProcessor} 会自动将本类属性同步到
 * Seata 原生配置前缀（{@code seata.*}），无需重复配置。
 *
 * @author eagle
 * @see com.eagle.seata.config.SeataEnvironmentPostProcessor
 */
@Data
@ConfigurationProperties(prefix = "eagle.seata")
public class SeataProperties {

    /**
     * 应用名称，通常与 {@code spring.application.name} 保持一致。
     * 用于在 Seata Server 中标识当前服务。
     */
    private String applicationId;

    /**
     * 事务服务分组名称，需与 Seata Server 端配置的 cluster 映射保持一致。
     */
    private String txServiceGroup = "eagle_tx_group";
}
