package com.eagle.idgenerator.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式 ID 生成器配置属性。
 *
 * <p>示例配置（application.yml）：
 * <pre>
 * eagle:
 *   id-generator:
 *     enabled: true
 *     worker-id: 1        # 工作机器 ID，范围 0-31，集群部署时每个实例须不同
 *     datacenter-id: 1    # 数据中心 ID，范围 0-31
 *     sequence: 0
 * </pre>
 *
 * @author sunshixiong
 */
@Data
@ConfigurationProperties(prefix = "eagle.id-generator")
public class IdGeneratorProperties {

    /**
     * 是否启用 ID 生成器，默认启用。
     */
    private boolean enabled = true;

    /**
     * 工作机器 ID，范围 0-31。
     * <p>集群部署时，每个服务实例须配置不同的 workerId，以保证全局唯一性。
     */
    private long workerId = 1;

    /**
     * 数据中心 ID，范围 0-31。
     * <p>多数据中心部署时，每个数据中心配置不同值。
     */
    private long datacenterId = 1;

    /**
     * 序列号起始值，通常保持默认值 0。
     */
    private long sequence = 0L;

    /**
     * 是否同时注册 {@link com.eagle.idgenerator.generator.OrderNoGenerator} 和
     * {@link com.eagle.idgenerator.util.IdGeneratorFacade}，默认启用。
     * <p>设置为 {@code false} 可仅使用基础的 {@link com.eagle.idgenerator.generator.IdGenerator}
     * Bean，不引入订单号生成器门面。
     */
    private boolean enableFacade = true;
}
