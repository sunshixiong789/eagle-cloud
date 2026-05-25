package com.eagle.http.client.interceptor;

import com.eagle.common.pressuretest.PressureTestContext;
import com.eagle.common.http.HttpClientProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.List;

/**
 * RestClient 请求拦截器：透传入站请求 Header 与压测标记。
 *
 * @author eagle
 */
@Slf4j
public class PropagatingHeadersClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final List<String> propagatedHeaders;
    private final boolean pressureTestHeaderEnabled;

    public PropagatingHeadersClientHttpRequestInterceptor() {
        this(new HttpClientProperties());
    }

    public PropagatingHeadersClientHttpRequestInterceptor(HttpClientProperties properties) {
        this.propagatedHeaders = List.copyOf(properties.getPropagatedHeaders());
        this.pressureTestHeaderEnabled = properties.isPressureTestHeaderEnabled();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (pressureTestHeaderEnabled && PressureTestContext.isPressureTest()) {
            request.getHeaders().set(PressureTestContext.PRESSURE_TEST_HEADER, "true");
        }

        HttpServletRequest inbound = getCurrentRequest();
        if (inbound != null) {
            for (String headerName : propagatedHeaders) {
                String value = inbound.getHeader(headerName);
                if (value != null) {
                    request.getHeaders().set(headerName, value);
                    log.debug("HTTP header propagated: {}", headerName);
                }
            }
        }

        return execution.execute(request, body);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
