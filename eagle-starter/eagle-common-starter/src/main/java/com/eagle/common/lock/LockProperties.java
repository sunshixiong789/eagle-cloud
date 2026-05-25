package com.eagle.common.lock;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 分布式锁配置（{@code eagle.lock.*}）。
 *
 * <p>统一控制 {@link DistributedLock} 的实现选型、锁粒度与 MQ 模式下的拓扑参数。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.lock")
public class LockProperties {

    /**
     * 锁实现类型。
     * <ul>
     *   <li>{@code redis}（默认）— 基于 Redisson，性能最优</li>
     *   <li>{@code mq}            — 基于 RocketMQ SimpleConsumer，跨机房或资源受限场景</li>
     * </ul>
     */
    private Type type = Type.REDIS;

    /**
     * 锁粒度（仅 {@code mq} 类型生效）。
     * <ul>
     *   <li>{@code per-key}      — 每个 lockKey 一个 topic，真·按 key 隔离</li>
     *   <li>{@code shared-topic} — 所有 lockKey 共用一个 topic，退化为全局单锁</li>
     * </ul>
     */
    private Granularity granularity = Granularity.PER_KEY;

    /**
     * {@link Granularity#PER_KEY} 模式下，topic 命名前缀。
     */
    private String topicPrefix = "eagle-lock-";

    /**
     * {@link Granularity#SHARED_TOPIC} 模式下，共用 topic 名称。
     */
    private String sharedTopic = "eagle-lock-shared";

    /**
     * MQ 锁的不可见时长（秒）。
     *
     * <p>等价于锁的最大持有时间：进程崩溃且未主动释放时，{@code invisibleDuration} 到期后
     * token 消息重新对其他消费者可见，锁自然恢复。建议设置为业务执行最长时间的 2-3 倍。
     */
    private int invisibleDurationSeconds = 30;

    /**
     * MQ 锁消费者组名。
     */
    private String consumerGroup = "eagle-lock-consumer";

    /**
     * MQ 锁需要管理的 lockKey 列表。
     *
     * <p>启动时由 {@code LockTokenInitializer} 为每个 key 在对应 topic 中发布一条 token 消息，
     * 之后 {@code tryLock(key)} 通过独占消费该 token 实现互斥。
     *
     * <p><b>注意</b>：未声明的 lockKey 在 MQ 模式下无法获取锁。Redis 模式忽略此配置。
     */
    private List<String> keys = new ArrayList<>();

    /**
     * 是否在应用启动时自动发布 token 消息。
     *
     * <p>RocketMQ 不提供消息去重，多节点同时启动会导致每个 lockKey 堆积多条 token，
     * 表现为「锁退化为信号量」。生产环境建议设为 {@code false}，由运维通过一次性脚本初始化；
     * 测试/单节点环境可开启。
     */
    private boolean autoInitToken = false;

    /**
     * 单次轮询拉取 token 消息的超时（秒），用于在 {@code waitTime} 范围内做循环 receive。
     */
    private int pollIntervalSeconds = 1;

    public enum Type {
        REDIS, MQ
    }

    public enum Granularity {
        PER_KEY, SHARED_TOPIC
    }
}
