package com.eagle.seata.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Seata 环境属性后处理器。
 *
 * <p>将 {@code eagle.seata.*} 配置自动同步到 Seata 原生配置前缀（{@code seata.*}），
 * 避免用户在同一 {@code application.yml} 中重复配置两套属性。
 *
 * <p>同步规则：
 * <ul>
 *   <li>{@code eagle.seata.application-id} → {@code seata.application-id}</li>
 *   <li>{@code eagle.seata.tx-service-group} → {@code seata.tx-service-group}</li>
 *   <li>{@code seata.enabled} 默认置为 {@code true}（starter 引入即启用）</li>
 * </ul>
 *
 * <p>同步属性优先级低于用户直接配置的 {@code seata.*}（使用 {@code addLast} 插入），
 * 用户若需覆盖可直接在配置文件中写 {@code seata.*} 属性。
 *
 * <p>注册方式：{@code META-INF/spring.factories}
 * {@code org.springframework.context.ApplicationListener=com.eagle.seata.config.SeataEnvironmentPostProcessor}
 *
 * @author eagle
 */
public class SeataEnvironmentPostProcessor implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    /**
     * 合并属性源的名称，用于标识本处理器注入的属性。
     */
    private static final String PROPERTY_SOURCE_NAME = "eagleSeataProperties";

    /**
     * 将 {@code eagle.seata.*} 属性同步到 {@code seata.*} 前缀。
     *
     * @param event 环境准备完成事件
     */
    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();

        String appId = environment.getProperty("eagle.seata.application-id");
        String txGroup = environment.getProperty("eagle.seata.tx-service-group", "eagle_tx_group");

        MutablePropertySources sources = environment.getPropertySources();

        // 防止重复注册（热重载场景）
        if (sources.contains(PROPERTY_SOURCE_NAME)) {
            sources.remove(PROPERTY_SOURCE_NAME);
        }

        Map<String, Object> props = new HashMap<>();
        if (StringUtils.hasText(appId)) {
            props.put("seata.application-id", appId);
        }
        props.put("seata.tx-service-group", txGroup);
        // starter 引入即启用，使用方需关闭可显式覆盖 seata.enabled=false
        props.put("seata.enabled", "true");

        // addLast 保证优先级低于用户直接配置的 seata.* 属性
        sources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }
}