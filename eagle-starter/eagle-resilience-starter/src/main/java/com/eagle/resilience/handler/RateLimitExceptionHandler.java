package com.eagle.resilience.handler;

import com.eagle.common.dto.ErrorResult;
import com.eagle.common.exception.codes.CommonErrorCode;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

/**
 * Resilience4J 限流异常处理器，统一转成 HTTP 429。
 *
 * <p>处理两类异常：
 * <ul>
 *   <li>{@link RequestNotPermitted} — QPS 令牌耗尽（{@code @RateLimit(qps = ...)}）</li>
 *   <li>{@link BulkheadFullException} — 并发数超限（{@code @RateLimit(threads = ...)}）</li>
 * </ul>
 *
 * <p>响应体沿用平台统一的 {@link ErrorResult}，{@code errorCode} 取
 * {@link CommonErrorCode#TOO_MANY_REQUESTS}，与原 Sentinel
 * {@code EagleSentinelBlockExceptionHandler} 返回的 429 语义保持一致。
 *
 * <p>声明为最高优先级：{@code GlobalExceptionHandler} 未指定 order（默认最低优先级）
 * 且含 {@code @ExceptionHandler(Exception.class)} 兜底，若不提高本 advice 的优先级，
 * 限流异常会被兜底处理器吃成 500。
 *
 * @author eagle
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@RequiredArgsConstructor
public class RateLimitExceptionHandler {

    private final MessageSource messageSource;

    /**
     * 处理 QPS 限流触发 — HTTP 429。
     *
     * @param e       Resilience4J 限流异常（{@code getMessage()} 含实例名）
     * @param request 当前请求
     * @param locale  请求 locale，用于消息国际化
     * @return 统一错误响应体
     */
    @ExceptionHandler(RequestNotPermitted.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ResponseBody
    public ErrorResult handleRequestNotPermitted(RequestNotPermitted e,
                                                 HttpServletRequest request, Locale locale) {
        log.warn("[RateLimit] QPS 限流触发: {}", e.getMessage());
        return tooManyRequests(request, locale);
    }

    /**
     * 处理并发数超限 — HTTP 429。
     *
     * @param e       Resilience4J 隔离舱满载异常
     * @param request 当前请求
     * @param locale  请求 locale，用于消息国际化
     * @return 统一错误响应体
     */
    @ExceptionHandler(BulkheadFullException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ResponseBody
    public ErrorResult handleBulkheadFull(BulkheadFullException e,
                                          HttpServletRequest request, Locale locale) {
        log.warn("[RateLimit] 并发限流触发: {}", e.getMessage());
        return tooManyRequests(request, locale);
    }

    /**
     * 构建 429 响应体，消息按 locale 国际化，缺失时回落到错误码自带的默认文案。
     *
     * @param request 当前请求
     * @param locale  请求 locale
     * @return 统一错误响应体
     */
    private ErrorResult tooManyRequests(HttpServletRequest request, Locale locale) {
        CommonErrorCode errorCode = CommonErrorCode.TOO_MANY_REQUESTS;
        String msg = messageSource.getMessage(
                errorCode.getMessageKey(), null, errorCode.getDefaultMessage(), locale);
        return ErrorResult.of(HttpStatus.TOO_MANY_REQUESTS, msg,
                errorCode.getCode(), request.getRequestURI());
    }
}
