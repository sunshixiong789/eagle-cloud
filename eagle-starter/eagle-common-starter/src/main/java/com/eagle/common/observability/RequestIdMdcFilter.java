package com.eagle.common.observability;

import com.eagle.common.dto.ErrorResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Request ID 解析 + MDC 注入过滤器（Servlet 环境）。
 *
 * <p>请求阶段：优先取上游传入的 {@code X-Request-Id} 头,无则生成 UUID;写入 MDC 供日志格式 / 异常处理器
 * （{@link ErrorResult#of}）自动消费,并将值回写响应头,前端可直接读取用于问题定位。
 *
 * <p>finally 阶段：从 MDC 移除,避免线程池下线程复用时上下文污染。
 *
 * <p>注册顺序 HIGHEST_PRECEDENCE：必须先于业务过滤器执行,确保后续过滤器 / Servlet 内部都能从 MDC
 * 取到 requestId(详见 EagleCommonAutoConfiguration.WebMvcConfiguration)。
 *
 * @author 孙士雄
 */
@Slf4j
public class RequestIdMdcFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            // 兜底:直连业务服务时网关不在路径上,自行生成保证日志与异常响应可关联
            requestId = UUID.randomUUID().toString();
        }
        try {
            MDC.put(ErrorResult.MDC_REQUEST_ID, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(ErrorResult.MDC_REQUEST_ID);
        }
    }
}
