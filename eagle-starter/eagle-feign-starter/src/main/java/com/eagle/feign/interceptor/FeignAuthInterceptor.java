package com.eagle.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器：从当前 HTTP 请求中透传 Authorization Header 和链路追踪信息。
 *
 * <p>确保下游服务能拿到当前用户的 JWT Token 进行鉴权，并维持分布式链路追踪上下文。
 *
 * @author 孙士雄
 */
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final Tracer tracer;

    public FeignAuthInterceptor() {
        this(null);
    }

    public FeignAuthInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void apply(RequestTemplate template) {
        String token = extractTokenFromCurrentRequest();
        if (token != null) {
            template.header(AUTHORIZATION_HEADER, token);
            log.debug("Feign request intercepted, Authorization header forwarded to {}", template.url());
        }

        propagateTraceContext(template);
    }

    /**
     * 透传链路追踪上下文（B3 Propagation）。
     */
    private void propagateTraceContext(RequestTemplate template) {
        if (tracer == null || tracer.currentSpan() == null) {
            return;
        }
        Span span = tracer.currentSpan();
        template.header("X-B3-TraceId", span.context().traceId());
        template.header("X-B3-SpanId", span.context().spanId());
        if (span.context().parentId() != null) {
            template.header("X-B3-ParentSpanId", span.context().parentId());
        }
        template.header("X-B3-Sampled", span.context().sampled() ? "1" : "0");
        log.debug("Trace context propagated: traceId={}, spanId={}",
                span.context().traceId(), span.context().spanId());
    }

    /**
     * 从当前请求上下文中提取 Authorization Header。
     *
     * @return Bearer token 或 null
     */
    private String extractTokenFromCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(AUTHORIZATION_HEADER);
    }
}
