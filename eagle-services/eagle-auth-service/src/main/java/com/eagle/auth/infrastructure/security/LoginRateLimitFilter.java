package com.eagle.auth.infrastructure.security;

import com.eagle.common.dto.ErrorResult;
import com.eagle.common.exception.AppException;
import com.eagle.common.exception.ErrorCode;
import com.eagle.auth.domain.AuthErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 表单登录入口（POST /login）的频率限制 + 黑名单前置过滤器。
 *
 * <p>合并了原 LoginRateLimitFilter（IP 失败次数限流）+ IP / username 黑名单检查，
 * 避免在 UserDetailsService 中重复查 Redis（自定义 grant 路径会回调 UserDetailsService 二次加载）。
 *
 * <p>顺序约定：必须排在 {@link ClientIpFilter} 之后执行，保证 IP 已经过可信代理校验。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/login";
    private static final String POST_METHOD = "POST";

    private final LoginAttemptService loginAttemptService;
    private final LoginRateLimitProperties properties;
    private final RequestIpResolver requestIpResolver;
    private final BlacklistChecker blacklistChecker;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String ip = resolveIp(request);
        if (loginAttemptService.isBlocked(ip)) {
            log.warn("登录频率超限，已拦截 IP：{}", ip);
            writeError(request, response, HttpStatus.TOO_MANY_REQUESTS, AuthErrorCode.LOGIN_BLOCKED);
            return;
        }
        try {
            blacklistChecker.checkLogin(request.getParameter("username"), null, ip, null);
        } catch (AppException ex) {
            log.warn("login blocked by blacklist, code={}", ex.getErrorCode().getCode());
            writeError(request, response, HttpStatus.FORBIDDEN, ex.getErrorCode());
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        return !(LOGIN_PATH.equals(request.getServletPath())
                && POST_METHOD.equalsIgnoreCase(request.getMethod()));
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = ClientIpHolder.get();
        return ip != null ? ip : requestIpResolver.resolve(request);
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, ErrorCode errorCode) throws IOException {
        ErrorResult err = ErrorResult.of(
                status,
                errorCode.getMessage(request.getLocale()),
                errorCode.getCode(),
                request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(err));
    }
}
