package com.eagle.websocket.config;

import com.eagle.websocket.interceptor.WebSocketAuthHandshakeInterceptor;
import com.eagle.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP WebSocket 消息代理配置。
 *
 * <p>注册 STOMP 端点、配置消息代理前缀和心跳间隔。
 * 由 {@link WebSocketAutoConfiguration} 导入，属性来自 {@link WebSocketProperties}。
 *
 * @author 孙士雄
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class EagleWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;
    private final WebSocketAuthHandshakeInterceptor handshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理，处理广播和点对点消息
        registry.enableSimpleBroker(properties.getTopicPrefix(), properties.getUserPrefix());
        // 配置客户端发送消息的目标前缀
        registry.setApplicationDestinationPrefixes(properties.getAppPrefix());
        // 配置用户专属消息前缀（配合 convertAndSendToUser）
        registry.setUserDestinationPrefix(properties.getUserPrefix());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(properties.getEndpoint())
                .setAllowedOriginPatterns(properties.getAllowedOrigins().toArray(String[]::new))
                .addInterceptors(handshakeInterceptor)
                // SockJS 降级支持（兼容不支持 WebSocket 的浏览器）
                .withSockJS();
    }
}
