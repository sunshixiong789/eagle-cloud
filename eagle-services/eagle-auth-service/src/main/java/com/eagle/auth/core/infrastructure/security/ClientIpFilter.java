package com.eagle.auth.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 在请求线程上设置 {@link ClientIpHolder}，供后续 AuthenticationProvider /
 * UserDetailsService 读取真实客户端 IP（已经过 {@link RequestIpResolver} 可信代理校验）。
 *
 * <p>请求结束清理 ThreadLocal 防止线程池污染。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class ClientIpFilter extends OncePerRequestFilter {

    private final RequestIpResolver requestIpResolver;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            ClientIpHolder.set(requestIpResolver.resolve(request), request.getHeader("User-Agent"));
            filterChain.doFilter(request, response);
        } finally {
            ClientIpHolder.clear();
        }
    }
}
