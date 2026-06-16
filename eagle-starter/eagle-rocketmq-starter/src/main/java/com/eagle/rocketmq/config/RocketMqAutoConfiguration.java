package com.eagle.rocketmq.config;

import com.eagle.common.lock.DistributedLock;
import com.eagle.common.lock.LockProperties;
import com.eagle.rocketmq.admin.RocketMqTopicAdmin;
import com.eagle.rocketmq.lock.LockTokenInitializer;
import com.eagle.rocketmq.lock.RocketMqDistributedLock;
import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import com.eagle.rocketmq.publisher.RocketMqDomainEventPublisher;
import com.eagle.rocketmq.transaction.AbstractRocketMqTransactionChecker;
import com.eagle.rocketmq.transaction.RocketMqTransactionalEventPublisher;
import com.eagle.rocketmq.transaction.TransactionalEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * RocketMQ 自动配置。
 *
 * <p>提供领域事件发布能力，基于 RocketMQ 5.x 轻量客户端（gRPC）。
 * 自动注册：
 * <ul>
 *   <li>{@link DomainEventPublisher} — 同步、异步、延迟、顺序消息</li>
 *   <li>{@link TransactionalEventPublisher} — 事务消息（Outbox Pattern），需要容器内有 {@link AbstractRocketMqTransactionChecker} Bean</li>
 * </ul>
 *
 * @author eagle
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.apache.rocketmq.client.apis.ClientServiceProvider")
@EnableConfigurationProperties(RocketMqProperties.class)
public class RocketMqAutoConfiguration {

    /**
     * Topic admin 客户端:启动期幂等建 topic,避免依赖 Producer 首发触发 autoCreateTopicEnable。
     *
     * <p>仅在 {@code eagle.rocketmq.topic-admin.enabled=true}(默认 true)时启用。
     * 生产严格运维场景可关闭,改为运维预建 topic。
     */
    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "eagle.rocketmq.topic-admin", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RocketMqTopicAdmin rocketMqTopicAdmin(RocketMqProperties properties) {
        log.info("Initializing RocketMQ topic admin, namesrv: {}", properties.getNamesrvAddr());
        return new RocketMqTopicAdmin(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainEventPublisher domainEventPublisher(RocketMqProperties properties,
                                                     ObjectProvider<RocketMqTopicAdmin> topicAdminProvider) {
        log.info("Initializing RocketMQ domain event publisher, endpoints: {}", properties.getEndpoints());
        return new RocketMqDomainEventPublisher(properties, topicAdminProvider.getIfAvailable());
    }

    /**
     * 事务消息发布器：容器中存在 {@link AbstractRocketMqTransactionChecker} 时自动注册。
     *
     * <p>使用事务消息时，业务服务需继承 {@link AbstractRocketMqTransactionChecker} 并声明为 Bean，
     * 用于 Broker 回查本地事务状态。
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionalEventPublisher transactionalEventPublisher(
            RocketMqProperties properties,
            ObjectProvider<AbstractRocketMqTransactionChecker> checkerProvider) {
        List<AbstractRocketMqTransactionChecker> checkers = checkerProvider.stream().toList();
        log.info("Initializing RocketMQ transactional event publisher, checkers: {}", checkers.size());
        return new RocketMqTransactionalEventPublisher(properties, checkers);
    }

    /**
     * 基于 RocketMQ SimpleConsumer 的分布式锁实现。
     *
     * <p>仅在 {@code eagle.lock.type=mq} 时注册，与 redis-starter 提供的
     * {@code RedisDistributedLock} 互斥。
     */
    @Bean
    @ConditionalOnMissingBean(DistributedLock.class)
    @ConditionalOnProperty(name = "eagle.lock.type", havingValue = "mq")
    public DistributedLock rocketMqDistributedLock(RocketMqProperties mqProps, LockProperties lockProps) {
        log.info("Initializing RocketMQ distributed lock, granularity: {}", lockProps.getGranularity());
        return new RocketMqDistributedLock(mqProps, lockProps);
    }

    /**
     * MQ 锁 token 初始化器：仅在 {@code eagle.lock.type=mq} 且
     * {@code eagle.lock.auto-init-token=true} 时注册。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "eagle.lock.type", havingValue = "mq")
    public LockTokenInitializer lockTokenInitializer(RocketMqProperties mqProps, LockProperties lockProps) {
        return new LockTokenInitializer(mqProps, lockProps);
    }
}
