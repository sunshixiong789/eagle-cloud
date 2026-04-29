package com.eagle.feign.interceptor;

import com.eagle.common.pressuretest.PressureTestContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器：透传 Authorization、语言偏好和全链路压测标记 Header。
 *
 * <p>保证下游服务能获取当前用户的 JWT Token 进行鉴权，以及保持 i18n 语言环境一致性。
 * 同时透传压测标记 {@code X-Eagle-Gray}，确保压测流量在整条调用链上始终被识别。
 *
 * <p>B3 链路追踪头由 Spring Cloud OpenFeign + Micrometer Tracing 自动注入，无需手动处理。
 *
 * <p>在异步上下文（定时任务、异步线程）中无 HTTP 请求，此拦截器会跳过 Header 透传——
 * 这些场景通常使用 Client Credentials 认证，无需透传用户 Token。
 *
 * @author 孙士雄
 */
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    @Override
    public void apply(RequestTemplate template) {
        // 全链路压测标记透传（不依赖 HTTP 请求，从 ThreadLocal 读取）
        if (PressureTestContext.isPressureTest()) {
            template.header(PressureTestContext.PRESSURE_TEST_HEADER, "true");
        }

        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            // 异步/定时任务上下文，无 HTTP 请求可透传
            return;
        }

        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (token != null) {
            template.header(AUTHORIZATION_HEADER, token);
            log.debug("Authorization forwarded to {}", template.url());
        }

        String acceptLanguage = request.getHeader(ACCEPT_LANGUAGE_HEADER);
        if (acceptLanguage != null) {
            template.header(ACCEPT_LANGUAGE_HEADER, acceptLanguage);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
