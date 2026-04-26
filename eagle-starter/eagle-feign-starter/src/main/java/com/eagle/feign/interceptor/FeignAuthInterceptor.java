package com.eagle.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器：从当前 HTTP 请求中透传 Authorization Header。
 *
 * <p>确保下游服务能拿到当前用户的 JWT Token 进行鉴权。
 *
 * @author 孙士雄
 */
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        String token = extractTokenFromCurrentRequest();
        if (token != null) {
            template.header(AUTHORIZATION_HEADER, token);
            log.debug("Feign request intercepted, Authorization header forwarded to {}", template.url());
        }
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
