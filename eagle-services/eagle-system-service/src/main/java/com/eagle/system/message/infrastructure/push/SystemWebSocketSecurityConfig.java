package com.eagle.system.message.infrastructure.push;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 将 {@link JwtWebSocketChannelInterceptor} 注册到客户端入站消息通道。
 *
 * <p>{@code eagle-websocket-starter} 提供的 {@code EagleWebSocketConfig} 已 {@code @EnableWebSocketMessageBroker}
 * 并完成端点 / broker 装配,本服务无需再次 enable。Spring 的
 * {@code DelegatingWebSocketMessageBrokerConfiguration} 会收集容器内所有
 * {@link WebSocketMessageBrokerConfigurer} bean,因此这里只补一个 inbound channel 拦截器即可。
 */
@Configuration
@RequiredArgsConstructor
public class SystemWebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtWebSocketChannelInterceptor jwtWebSocketChannelInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtWebSocketChannelInterceptor);
    }
}
