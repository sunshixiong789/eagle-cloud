package com.eagle.system.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * 认证事件监听器
 * <p>
 * 监听 Spring Security 的登录成功/失败事件,实现登录防护机制:
 * <ul>
 *   <li>登录失败时:递增 IP 失败计数,超过阈值后触发封锁</li>
 *   <li>登录成功时:清除 IP 失败计数,重置限制</li>
 *   <li>与 LoginAttemptService 和 LoginRateLimitFilter 配合使用</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;

    /**
     * 登录失败时递增失败计数
     * <p>
     * 监听 Spring Security 的 AbstractAuthenticationFailureEvent,
     * 当任何认证提供者(AuthenticationProvider)认证失败时触发。
     *
     * @param event 认证失败事件
     */
    @EventListener
    public void onAuthFailure(AbstractAuthenticationFailureEvent event) {
        String ip = extractIp(event.getAuthentication().getDetails());
        if (ip != null) {
            loginAttemptService.registerFailure(ip);
        }
    }

    /**
     * 登录成功时重置失败计数
     * <p>
     * 监听 Spring Security 的 AuthenticationSuccessEvent,
     * 认证成功时清除该 IP 的失败记录,防止误封。
     *
     * @param event 认证成功事件
     */
    @EventListener
    public void onAuthSuccess(AuthenticationSuccessEvent event) {
        String ip = extractIp(event.getAuthentication().getDetails());
        if (ip != null) {
            loginAttemptService.registerSuccess(ip);
        }
    }

    private String extractIp(Object details) {
        if (details instanceof WebAuthenticationDetails webDetails) {
            return webDetails.getRemoteAddress();
        }
        return null;
    }
}
