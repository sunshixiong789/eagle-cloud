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
 *     type: snowflake          # 默认 IdGenerator 实现：snowflake / uuid / tsid
 *     worker-id: 1             # 雪花算法 workerId，范围 0-31
 *     datacenter-id: 1         # 雪花算法 dataCenterId，范围 0-31
 *     tsid:
 *       node-id: 1             # TSID 节点 ID，范围 [0, 1023]
 *       node-bits: 10          # TSID 节点位数（8/10/12 → 256/1024/4096 节点）
 *     nano-id:
 *       default-size: 21       # NanoId 默认长度
 * </pre>
 *
 * @author sunshixiong
 */
@Data
@ConfigurationProperties(prefix = "eagle.id-generator")
public class IdGeneratorProperties {

    /**
     * TSID 配置
     */
    private final Tsid tsid = new Tsid();
    /**
     * NanoId 配置
     */
    private final NanoId nanoId = new NanoId();
    /**
     * 默认 {@code IdGenerator} Bean 选用的实现。
     * <ul>
     *   <li>{@link Type#SNOWFLAKE}（默认）— Hutool Snowflake，long 主键</li>
     *   <li>{@link Type#UUID} — UUID v7（time-ordered Unix Epoch）</li>
     *   <li>{@link Type#TSID} — TSID（Time-Sorted Unique Identifier）</li>
     * </ul>
     */
    private Type type = Type.SNOWFLAKE;
    /**
     * 雪花算法工作机器 ID，范围 0-31，集群部署时各实例须不同。
     */
    private long workerId = 1;
    /**
     * 雪花算法数据中心 ID，范围 0-31，多数据中心部署时各中心不同。
     */
    private long datacenterId = 1;
    /**
     * 序列号起始值（Hutool Snowflake 不使用此项，保留以兼容旧配置）
     */
    private long sequence = 0L;
    /**
     * 是否同时注册 {@code OrderNoGenerator} 和 {@code IdGeneratorFacade}，默认启用。
     */
    private boolean enableFacade = true;

    /**
     * 默认 IdGenerator 实现类型
     */
    public enum Type {
        SNOWFLAKE,
        UUID,
        TSID
    }

    @Data
    public static class Tsid {
        /**
         * 节点 ID，范围依 nodeBits 决定（默认 1024 节点 → [0, 1023]）
         */
        private int nodeId = 1;
        /**
         * 节点位数：8=256 / 10=1024 / 12=4096 节点
         */
        private int nodeBits = 10;
    }

    @Data
    public static class NanoId {
        /**
         * NanoId 默认长度（21 字符 ≈ UUID v4 碰撞概率）
         */
        private int defaultSize = 21;
    }
}
