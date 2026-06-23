package com.eagle.websocket.config;

import com.eagle.websocket.interceptor.WebSocketAuthHandshakeInterceptor;
import com.eagle.websocket.interceptor.WebSocketChannelInterceptor;
import com.eagle.websocket.listener.WebSocketEventListener;
import com.eagle.websocket.metrics.WebSocketMetrics;
import com.eagle.websocket.offline.OfflineMessageStore;
import com.eagle.websocket.offline.RedisOfflineMessageStore;
import com.eagle.websocket.properties.WebSocketProperties;
import com.eagle.websocket.session.WebSocketSessionManager;
import com.eagle.websocket.sse.SseEmitterManager;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import tools.jackson.databind.ObjectMapper;

/**
 * WebSocket + SSE 实时推送自动配置。
 *
 * <p>注册以下组件：
 * <ul>
 *   <li>{@link EagleWebSocketConfig} — STOMP WebSocket 消息代理配置（端点、前缀、心跳）</li>
 *   <li>{@link WebSocketSessionManager} — 点对点推送和广播工具</li>
 *   <li>{@link SseEmitterManager} — SSE 推送管理器（适合单向推送场景）</li>
 *   <li>{@link WebSocketAuthHandshakeInterceptor} — 握手拦截器，提取 JWT Token</li>
 *   <li>{@link WebSocketChannelInterceptor} — STOMP 通道拦截器（日志、鉴权扩展点）</li>
 *   <li>{@link RedisOfflineMessageStore} — 离线消息存储（需 Redisson 依赖）</li>
 *   <li>{@link WebSocketMetrics} — 连接和消息指标（需 Micrometer 依赖）</li>
 * </ul>
 *
 * <p>通过 {@code eagle.websocket.enabled=false} 可整体禁用。
 *
 * @author eagle
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(WebSocketMessageBrokerConfigurer.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(WebSocketProperties.class)
@Import(EagleWebSocketConfig.class)
public class WebSocketAutoConfiguration {

    /**
     * WebSocket 握手拦截器，提取 Token 存入会话属性。
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor() {
        return new WebSocketAuthHandshakeInterceptor();
    }

    /**
     * WebSocket 消息推送管理器（点对点 + 广播）。
     *
     * <p>若上下文中存在 {@link WebSocketMetrics} Bean，则注入以自动上报消息发送指标。
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketSessionManager webSocketSessionManager(
            SimpMessagingTemplate template,
            ObjectProvider<WebSocketMetrics> metricsProvider) {
        return new WebSocketSessionManager(template, metricsProvider.getIfAvailable());
    }

    /**
     * SSE 推送管理器（单向实时推送）。
     */
    @Bean
    @ConditionalOnMissingBean
    public SseEmitterManager sseEmitterManager(ObjectMapper objectMapper) {
        return new SseEmitterManager(objectMapper);
    }

    /**
     * STOMP 消息通道拦截器，提供日志记录和可扩展的鉴权钩子。
     *
     * <p>业务方可定义 {@link WebSocketChannelInterceptor} 子类 Bean 覆盖此默认实现，
     * 以添加 JWT 验证、订阅权限校验等自定义逻辑。
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketChannelInterceptor webSocketChannelInterceptor() {
        return new WebSocketChannelInterceptor();
    }

    /**
     * 离线消息存储配置（依赖 Redisson）。
     *
     * <p>仅在类路径存在 {@link RedissonClient} 时激活，
     * 业务方可自定义 {@link OfflineMessageStore} 实现（如数据库存储）覆盖此默认实现。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    static class OfflineMessageConfiguration {

        /**
         * 基于 Redis List 的离线消息存储。
         *
         * @param client Redisson 客户端
         * @return 离线消息存储实例
         */
        @Bean
        @ConditionalOnMissingBean(OfflineMessageStore.class)
        public OfflineMessageStore offlineMessageStore(RedissonClient client) {
            log.info("[Eagle WebSocket] RedisOfflineMessageStore enabled");
            return new RedisOfflineMessageStore(client);
        }
    }

    /**
     * WebSocket 指标配置（依赖 Micrometer）。
     *
     * <p>仅在类路径存在 {@link MeterRegistry} 时激活，向 Micrometer 注册
     * {@code eagle.websocket.connections.active} 和 {@code eagle.websocket.messages.sent} 指标。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    static class MetricsConfiguration {

        /**
         * WebSocket 指标收集器。
         *
         * @param registry Micrometer 指标注册器
         * @return 指标收集器实例
         */
        @Bean
        @ConditionalOnMissingBean
        public WebSocketMetrics webSocketMetrics(MeterRegistry registry) {
            return new WebSocketMetrics(registry);
        }
    }

    /**
     * WebSocket 会话生命周期事件监听器配置。
     *
     * <p>监听 STOMP 连接 / 断开事件，记录日志并更新指标（指标可选）。
     */
    @Configuration(proxyBeanMethods = false)
    static class EventListenerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public WebSocketEventListener webSocketEventListener(
                ObjectProvider<WebSocketMetrics> metricsProvider) {
            return new WebSocketEventListener(metricsProvider.getIfAvailable());
        }
    }
}
