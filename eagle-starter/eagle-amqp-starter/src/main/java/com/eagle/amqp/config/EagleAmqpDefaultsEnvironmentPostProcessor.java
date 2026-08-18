package com.eagle.amqp.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * 给 {@code spring.rabbitmq.listener.simple.*} 补一组 starter 级默认值。
 *
 * <p><b>为什么需要这个类</b>：本 starter 的核心承诺是「消费失败自动退避重试，耗尽后进 DLQ」，
 * 而 Spring Boot 的 {@code spring.rabbitmq.listener.simple.retry.enabled} 默认是
 * <b>{@code false}</b> —— 直接把容器交给 Boot 的 factory configurer 装配，重试会静默消失，
 * 消费失败的消息一次就进 DLQ。这里把它默认打开，并补上原 {@code eagle.amqp.consumer.*}
 * 那套等价的退避参数。
 *
 * <p><b>为什么不写死在 factory 上</b>：写死等于又一次架空框架的配置面 ——
 * 使用方照官方文档配 {@code spring.rabbitmq.listener.simple.retry.max-retries} 会不生效。
 * 用 {@link EnvironmentPostProcessor} 追加到 {@code PropertySource} 链的<b>最末尾</b>，
 * 任何来源（yml / 环境变量 / 命令行 / Consul KV）的显式配置都优先于它。
 *
 * <p>注册方式是 {@code META-INF/spring.factories} —— {@code EnvironmentPostProcessor}
 * 并未迁到 {@code AutoConfiguration.imports}，那个文件只收 {@code EnableAutoConfiguration} 一项。
 *
 * @author eagle
 */
public class EagleAmqpDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "eagleAmqpDefaults";

    /**
     * 默认值取自迁移前 {@code eagle.amqp.consumer.*} 的取值，保证行为不变：
     * max-attempts 4（含首次）即 max-retries 3。
     */
    private static final Map<String, Object> DEFAULTS = Map.ofEntries(
            Map.entry("spring.rabbitmq.listener.simple.retry.enabled", "true"),
            Map.entry("spring.rabbitmq.listener.simple.retry.max-retries", "3"),
            Map.entry("spring.rabbitmq.listener.simple.retry.initial-interval", "1s"),
            Map.entry("spring.rabbitmq.listener.simple.retry.multiplier", "2.0"),
            Map.entry("spring.rabbitmq.listener.simple.retry.max-interval", "30s"),
            Map.entry("spring.rabbitmq.listener.simple.prefetch", "32"),
            Map.entry("spring.rabbitmq.template.retry.enabled", "true"),
            Map.entry("spring.rabbitmq.template.retry.max-retries", "2"),
            Map.entry("spring.rabbitmq.template.retry.initial-interval", "200ms"));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // addLast：优先级最低，使用方的任何显式配置都能覆盖
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
    }
}
