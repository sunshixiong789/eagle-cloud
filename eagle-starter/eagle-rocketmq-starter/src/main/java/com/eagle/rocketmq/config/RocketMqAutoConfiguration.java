package com.eagle.rocketmq.config;

import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import com.eagle.rocketmq.publisher.RocketMqDomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * RocketMQ 自动配置。
 *
 * <p>提供领域事件发布能力，基于 RocketMQ 5.x 轻量客户端。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.apache.rocketmq.client.apis.ClientServiceProvider")
@EnableConfigurationProperties(RocketMqProperties.class)
@ConditionalOnProperty(name = "eagle.rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class RocketMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DomainEventPublisher domainEventPublisher(RocketMqProperties properties) {
        log.info("RocketMQ domain event publisher initialized, endpoints: {}", properties.getEndpoints());
        return new RocketMqDomainEventPublisher(properties);
    }
}
