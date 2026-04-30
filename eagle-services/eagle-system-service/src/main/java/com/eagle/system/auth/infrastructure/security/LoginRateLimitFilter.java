package com.eagle.system.auth.infrastructure.security;

import com.alibaba.fastjson2.JSON;
import com.eagle.common.dto.ErrorResult;
import com.eagle.system.auth.domain.AuthErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 登录频率限制过滤器
 * <p>
 * 拦截 /login 登录请求，若该 IP 失败次数超过阈值则直接返回 429，
 * 防止暴力破解。与 {@link LoginAttemptService} 配合使用。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/login";

    private final LoginAttemptService loginAttemptService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        if (loginAttemptService.isBlocked(ip)) {
            log.warn("登录频率超限，已拦截 IP：{}", ip);
            writeBlockedResponse(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只对登录请求生效
        return !LOGIN_PATH.equals(request.getServletPath());
    }

    /**
     * 写入 429 响应，消息通过 AuthErrorCode 国际化
     */
    private void writeBlockedResponse(HttpServletRequest request,
                                      HttpServletResponse response) throws IOException {
        String message = AuthErrorCode.LOGIN_BLOCKED.getMessage(request.getLocale());
        ErrorResult error = ErrorResult.of(
                HttpStatus.TOO_MANY_REQUESTS,
                message,
                AuthErrorCode.LOGIN_BLOCKED.getCode(),
                request.getRequestURI()
        );
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.toJSONString(error));
    }

    /**
     * 解析客户端真实 IP，优先取 X-Forwarded-For 头（反向代理场景）
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // 取第一个非空 IP（代理链最左侧为原始客户端 IP）
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
