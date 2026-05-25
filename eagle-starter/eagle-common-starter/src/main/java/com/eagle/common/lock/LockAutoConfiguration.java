package com.eagle.common.lock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 分布式锁公共自动配置。
 *
 * <p>仅启用 {@link LockProperties}，{@link DistributedLock} 实例由各 starter（redis / rocketmq）
 * 按 {@code eagle.lock.type} 选择性注入。
 *
 * @author eagle
 */
@AutoConfiguration
@EnableConfigurationProperties(LockProperties.class)
public class LockAutoConfiguration {
}
