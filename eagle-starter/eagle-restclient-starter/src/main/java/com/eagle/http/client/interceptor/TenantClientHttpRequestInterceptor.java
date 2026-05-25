package com.eagle.http.client.interceptor;

import com.eagle.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestClient 请求拦截器：透传当前租户 ID。
 *
 * @author eagle
 */
@Slf4j
public class TenantClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            request.getHeaders().set(TENANT_HEADER, tenantId);
            log.debug("Tenant ID propagated to downstream: {}", tenantId);
        }
        return execution.execute(request, body);
    }
}
