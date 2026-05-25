package com.eagle.common.dto;

import lombok.Data;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * 错误返回对象。
 *
 * <p>{@code requestId} 用于前端 / 运维全链路定位:
 * <ul>
 *   <li>Servlet 环境:由 {@code RequestIdMdcFilter} 自动从 {@code X-Request-Id} 请求头读取并写入 MDC,
 *       工厂方法内自动注入,调用方零改动</li>
 *   <li>WebFlux 环境(网关):MDC 不可靠,需显式 {@code setRequestId(String)} 写入</li>
 * </ul>
 *
 * @author eagle（sunshix@seeyon.com）
 * 2025/12/8-11:16
 */
@Data
public class ErrorResult {

    /**
     * MDC 中 requestId 的 key,与 RequestIdMdcFilter / 日志格式保持一致
     */
    public static final String MDC_REQUEST_ID = "requestId";

    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;
    private Integer errorCode;
    private String requestId;

    public static ErrorResult of(HttpStatus status, String message, String path) {
        ErrorResult apiError = new ErrorResult();
        apiError.timestamp = Instant.now();
        apiError.status = status.value();
        apiError.error = status.getReasonPhrase();
        apiError.message = message;
        apiError.path = path;
        // Servlet 环境:RequestIdMdcFilter 已写入 MDC;WebFlux 环境无 MDC,调用方需后续覆盖
        apiError.requestId = MDC.get(MDC_REQUEST_ID);
        return apiError;
    }

    public static ErrorResult of(HttpStatus status, String message,
                                 Integer errorCode, String path) {
        ErrorResult apiError = of(status, message, path);
        apiError.errorCode = errorCode;
        return apiError;
    }

}
