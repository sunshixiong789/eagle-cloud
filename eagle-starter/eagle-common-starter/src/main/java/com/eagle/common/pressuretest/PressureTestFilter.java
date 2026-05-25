package com.eagle.common.pressuretest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 全链路压测流量识别过滤器。
 *
 * <p>读取请求头 {@code X-Eagle-Gray: true}，写入 {@link PressureTestContext}，
 * 供业务代码在同一线程内识别压测流量并切换影子资源。
 *
 * <p>请求结束时在 {@code finally} 块中强制清除 ThreadLocal，
 * 防止线程池复用导致的上下文污染。
 *
 * <p>注册方式（在 AutoConfiguration 中）：
 * <pre>{@code
 * @Bean
 * public FilterRegistrationBean<PressureTestFilter> pressureTestFilter() {
 *     FilterRegistrationBean<PressureTestFilter> bean = new FilterRegistrationBean<>();
 *     bean.setFilter(new PressureTestFilter());
 *     bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
 *     return bean;
 * }
 * }</pre>
 *
 * @author eagle
 */
@Slf4j
public class PressureTestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String grayHeader = request.getHeader(PressureTestContext.PRESSURE_TEST_HEADER);
            if ("true".equalsIgnoreCase(grayHeader)) {
                PressureTestContext.mark();
                log.debug("[PressureTest] Pressure test request detected, uri: {}", request.getRequestURI());
            }
            filterChain.doFilter(request, response);
        } finally {
            // 防止 ThreadLocal 泄漏，线程归还线程池前必须清除
            PressureTestContext.clear();
        }
    }
}
