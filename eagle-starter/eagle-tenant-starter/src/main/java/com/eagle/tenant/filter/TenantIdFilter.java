package com.eagle.tenant.filter;

import com.eagle.tenant.TenantContextHolder;
import com.eagle.tenant.properties.TenantProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 租户 ID 解析过滤器。
 *
 * <p>从 HTTP 请求头中解析租户 ID 并写入 {@link TenantContextHolder}，请求结束后自动清理。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@ConditionalOnWebApplication
@RequiredArgsConstructor
public class TenantIdFilter implements Filter {

    private final TenantProperties properties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantId = httpRequest.getHeader(properties.getHeaderName());
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = properties.getDefaultTenantId();
        }
        TenantContextHolder.setTenantId(tenantId);
        log.debug("Tenant resolved: {}", tenantId);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
