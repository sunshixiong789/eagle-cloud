package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.infrastructure.event.AuthLoginLogPublisher;
import com.eagle.auth.core.infrastructure.event.LoginLogIntegrationEvent;
import com.eagle.common.dto.EagleUser;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
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
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;
    private final DomainEventPublisher publisher;

    /**
     * 用户名/密码错误时递增失败计数（限缩到 BadCredentials，仅针对真实的登录尝试）。
     * <p>
     * 历史上监听 {@code AbstractAuthenticationFailureEvent}（所有认证失败基类），
     * 会把 JWT 解码失败、UserDetailsService 异常、Provider 不匹配等
     * 与"登录尝试"无关的事件全部计入 IP 失败计数，导致用户未登录也被 429 限流。
     *
     * @param event 用户名/密码错误事件
     */
    @EventListener
    public void onAuthFailure(AuthenticationFailureBadCredentialsEvent event) {
        String ip = ClientIpHolder.get();
        if (ip != null) {
            loginAttemptService.registerFailure(ip);
        }
        publishLoginLog(event.getAuthentication(), ip, false, event.getException().getMessage());
    }

    /**
     * 用户登录成功时重置失败计数并记录登录日志（限缩到 UsernamePasswordAuthenticationToken）。
     * <p>
     * {@code AuthenticationSuccessEvent} 对服务内<b>所有</b>认证成功都会发布：JWT bearer
     * （每个携带 access token 的 API 请求）、OAuth2 客户端认证、授权码兑换、refresh_token
     * 续期等。这些技术性认证不是"用户登录"，历史上全部记录导致一次登录产生大量登录日志。
     * <p>
     * 约定：所有代表"用户完成一次登录"的入口统一以 {@link UsernamePasswordAuthenticationToken}
     * 发布成功事件 —— 表单登录（DaoAuthenticationProvider）、Web 短信登录
     * （{@code LoginController#smsLogin} 手工发布）、custom grant App 登录
     * （{@code AbstractCustomGrantAuthenticationProvider#generateTokens} 手工发布）。
     *
     * @param event 认证成功事件
     */
    @EventListener
    public void onAuthSuccess(AuthenticationSuccessEvent event) {
        if (!(event.getAuthentication() instanceof UsernamePasswordAuthenticationToken authentication)) {
            return;
        }
        String ip = ClientIpHolder.get();
        if (ip != null) {
            loginAttemptService.registerSuccess(ip);
        }
        publishLoginLog(authentication, ip, true, null);
    }

    private void publishLoginLog(Authentication authentication, String ip,
                                 boolean success, String failReason) {
        try {
            LoginLogIntegrationEvent event = new LoginLogIntegrationEvent(
                    resolveAccountId(authentication),
                    authentication != null ? authentication.getName() : null,
                    ip,
                    ClientIpHolder.getUserAgent(),
                    success,
                    failReason);
            publisher.publish(AuthLoginLogPublisher.TOPIC, AuthLoginLogPublisher.TAG, event);
        } catch (RuntimeException ex) {
            log.warn("publish login log failed, username={}",
                    authentication != null ? authentication.getName() : null, ex);
        }
    }

    private Long resolveAccountId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof EagleUser user) {
            return user.getId();
        }
        return null;
    }
}
