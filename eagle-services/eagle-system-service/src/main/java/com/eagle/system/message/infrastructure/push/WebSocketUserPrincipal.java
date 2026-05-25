package com.eagle.system.message.infrastructure.push;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * STOMP user destination 路由键包装器。
 *
 * <p>Spring STOMP {@code convertAndSendToUser(user, ...)} 通过 {@link #getName()} 匹配在线会话。
 * 业务推送方按 userId 寻址(如 {@code WebSocketRealtimePushListener.sendToUser(userId, ...)}),
 * 故 WebSocket 会话 Principal 的 {@code getName()} 必须返回 userId 字符串。
 * 直接使用 {@code EagleAuthentication} 会让 {@code getName()} 返回 {@code username},
 * 与现有 sendToUser 调用错位、消息投递不到目标用户。
 *
 * <p>同时持有底层 {@link Authentication} (含 {@code EagleUser} principal、角色权限),
 * 供 {@code @MessageMapping} 通过 {@code @AuthenticationPrincipal} / SpEL 获取完整用户信息。
 */
public final class WebSocketUserPrincipal implements Authentication {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Authentication delegate;

    public WebSocketUserPrincipal(String name, Authentication delegate) {
        this.name = name;
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getPrincipal() {
        return delegate.getPrincipal();
    }

    @Override
    public Object getCredentials() {
        return delegate.getCredentials();
    }

    @Override
    public Object getDetails() {
        return delegate.getDetails();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public boolean isAuthenticated() {
        return delegate.isAuthenticated();
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        delegate.setAuthenticated(authenticated);
    }
}
