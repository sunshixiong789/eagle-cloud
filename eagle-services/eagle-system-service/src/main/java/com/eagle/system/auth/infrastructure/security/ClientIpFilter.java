package com.eagle.system.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 在请求线程上设置 {@link ClientIpHolder}，供后续 AuthenticationProvider /
 * UserDetailsService 读取真实客户端 IP。
 *
 * <p>请求结束清理 ThreadLocal 防止线程池污染。
 *
 * @author sunshixiong
 */
@Component
public class ClientIpFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            ClientIpHolder.set(resolveClientIp(request));
            filterChain.doFilter(request, response);
        } finally {
            ClientIpHolder.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
