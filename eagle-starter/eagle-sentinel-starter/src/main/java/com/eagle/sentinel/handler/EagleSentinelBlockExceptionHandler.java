package com.eagle.sentinel.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Eagle Sentinel 统一流控异常处理器。
 *
 * <p>根据不同的 {@code BlockException} 子类型，返回对应的 HTTP 状态码和 JSON 错误响应：
 * <ul>
 *   <li>{@link FlowException} → 429，请求过于频繁</li>
 *   <li>{@link DegradeException} → 503，服务降级</li>
 *   <li>{@link ParamFlowException} → 429，热点参数限流</li>
 *   <li>{@link AuthorityException} → 403，访问被拒绝</li>
 *   <li>{@link SystemBlockException} → 503，系统负载过高</li>
 * </ul>
 *
 * @author 孙士雄
 */
@Slf4j
public class EagleSentinelBlockExceptionHandler implements BlockExceptionHandler {

    /**
     * JSON 内容类型。
     */
    private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    /**
     * Jackson ObjectMapper，线程安全，可安全共享。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 处理 Sentinel 流控异常，向客户端返回标准 JSON 错误响应。
     *
     * @param request   HTTP 请求
     * @param response  HTTP 响应
     * @param exception Sentinel 流控异常
     * @throws Exception 序列化或 IO 异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       String resourceName, com.alibaba.csp.sentinel.slots.block.BlockException exception) throws Exception {

        int status;
        String message;

        if (exception instanceof FlowException) {
            status = 429;
            message = "请求过于频繁，请稍后再试";
        } else if (exception instanceof DegradeException) {
            status = 503;
            message = "服务降级，请稍后再试";
        } else if (exception instanceof ParamFlowException) {
            status = 429;
            message = "热点参数限流";
        } else if (exception instanceof AuthorityException) {
            status = 403;
            message = "访问被拒绝";
        } else if (exception instanceof SystemBlockException) {
            status = 503;
            message = "系统负载过高";
        } else {
            status = 429;
            message = "请求被限流";
        }

        log.warn("[Sentinel] BlockException triggered: rule={}, resource={}, status={}",
                exception.getClass().getSimpleName(), resourceName, status);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status);
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());

        response.setStatus(status);
        response.setContentType(CONTENT_TYPE_JSON);
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }
}
