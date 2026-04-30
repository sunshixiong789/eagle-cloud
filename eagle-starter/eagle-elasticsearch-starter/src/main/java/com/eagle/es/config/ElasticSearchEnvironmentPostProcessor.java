package com.eagle.es.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Elasticsearch 环境属性桥接处理器。
 *
 * <p>将 {@code eagle.elasticsearch.*} 自定义属性桥接到
 * Spring Data Elasticsearch 原生属性（{@code spring.elasticsearch.*}），
 * 使得用户只需配置 {@code eagle.elasticsearch.*} 即可完成 ES 客户端的初始化。
 *
 * <p>仅在对应的 {@code eagle.elasticsearch.*} 属性存在且
 * {@code spring.elasticsearch.*} 未显式配置时生效，避免覆盖用户的原生配置。
 *
 * @author eagle
 */
public class ElasticSearchEnvironmentPostProcessor implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    /**
     * 桥接属性源名称
     */
    private static final String PROPERTY_SOURCE_NAME = "eagleElasticsearchBridge";

    /**
     * Eagle ES 属性前缀
     */
    private static final String EAGLE_PREFIX = "eagle.elasticsearch.";

    /**
     * Spring ES 属性前缀
     */
    private static final String SPRING_PREFIX = "spring.elasticsearch.";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        Map<String, Object> bridgedProperties = new LinkedHashMap<>();

        // 桥接 uris
        bridgeListProperty(environment, bridgedProperties,
                EAGLE_PREFIX + "uris", SPRING_PREFIX + "uris");

        // 桥接 username
        bridgeProperty(environment, bridgedProperties,
                EAGLE_PREFIX + "username", SPRING_PREFIX + "username");

        // 桥接 password
        bridgeProperty(environment, bridgedProperties,
                EAGLE_PREFIX + "password", SPRING_PREFIX + "password");

        // 桥接 connect-timeout（毫秒 → Duration 字符串）
        bridgeTimeoutProperty(environment, bridgedProperties,
                EAGLE_PREFIX + "connect-timeout", SPRING_PREFIX + "connection-timeout");

        // 桥接 socket-timeout（毫秒 → Duration 字符串）
        bridgeTimeoutProperty(environment, bridgedProperties,
                EAGLE_PREFIX + "socket-timeout", SPRING_PREFIX + "socket-timeout");

        if (!bridgedProperties.isEmpty()) {
            // 使用最低优先级，避免覆盖用户显式配置的 spring.elasticsearch.* 属性
            environment.getPropertySources().addLast(
                    new MapPropertySource(PROPERTY_SOURCE_NAME, bridgedProperties));
        }
    }

    /**
     * 桥接单值字符串属性。
     *
     * @param environment       Spring 环境
     * @param bridgedProperties 待注册的属性映射
     * @param eagleKey          Eagle 配置键
     * @param springKey         Spring 原生配置键
     */
    private void bridgeProperty(ConfigurableEnvironment environment,
                                Map<String, Object> bridgedProperties,
                                String eagleKey,
                                String springKey) {
        String value = environment.getProperty(eagleKey);
        if (value != null && !environment.containsProperty(springKey)) {
            bridgedProperties.put(springKey, value);
        }
    }

    /**
     * 桥接列表类型属性（逗号分隔字符串）。
     *
     * @param environment       Spring 环境
     * @param bridgedProperties 待注册的属性映射
     * @param eagleKey          Eagle 配置键
     * @param springKey         Spring 原生配置键
     */
    private void bridgeListProperty(ConfigurableEnvironment environment,
                                    Map<String, Object> bridgedProperties,
                                    String eagleKey,
                                    String springKey) {
        String value = environment.getProperty(eagleKey);
        if (value != null && !environment.containsProperty(springKey)) {
            bridgedProperties.put(springKey, value);
        }
    }

    /**
     * 桥接超时属性，将毫秒整数值转换为 Spring Boot Duration 字符串（如 {@code 5000ms}）。
     *
     * @param environment       Spring 环境
     * @param bridgedProperties 待注册的属性映射
     * @param eagleKey          Eagle 配置键（毫秒整数）
     * @param springKey         Spring 原生配置键（Duration）
     */
    private void bridgeTimeoutProperty(ConfigurableEnvironment environment,
                                       Map<String, Object> bridgedProperties,
                                       String eagleKey,
                                       String springKey) {
        String value = environment.getProperty(eagleKey);
        if (value != null && !environment.containsProperty(springKey)) {
            // 转换为 Spring Boot Duration 格式（毫秒）
            bridgedProperties.put(springKey, value + "ms");
        }
    }
}
