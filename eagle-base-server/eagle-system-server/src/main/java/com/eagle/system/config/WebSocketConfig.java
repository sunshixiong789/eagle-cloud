package com.eagle.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static com.eagle.eagle.common.constant.CommonConstants.MESSAGE_WS_QUEUE;
import static com.eagle.eagle.common.constant.CommonConstants.MESSAGE_WS_SEND;
import static com.eagle.eagle.common.constant.CommonConstants.MESSAGE_WS_STOMP;
import static com.eagle.eagle.common.constant.CommonConstants.MESSAGE_WS_TOPIC;

/**
 * socket 配置
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/29-17:54
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 WebSocket 端点，支持 SockJS 回退
        registry.addEndpoint(MESSAGE_WS_STOMP)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. 客户端订阅前缀（服务端推送用）
        registry.enableSimpleBroker(MESSAGE_WS_TOPIC, MESSAGE_WS_QUEUE);

        // 2. 客户端发送消息前缀（Controller 接收用）
        registry.setApplicationDestinationPrefixes(MESSAGE_WS_SEND);
    }

}
