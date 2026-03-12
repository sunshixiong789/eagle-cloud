package com.eagle.system.config;

import com.eagle.common.constant.CommonConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


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
        registry.addEndpoint(CommonConstants.MESSAGE_WS_STOMP)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. 客户端订阅前缀（服务端推送用）
        registry.enableSimpleBroker(CommonConstants.MESSAGE_WS_TOPIC, CommonConstants.MESSAGE_WS_QUEUE);

        // 2. 客户端发送消息前缀（Controller 接收用）
        registry.setApplicationDestinationPrefixes(CommonConstants.MESSAGE_WS_SEND);
    }

}
