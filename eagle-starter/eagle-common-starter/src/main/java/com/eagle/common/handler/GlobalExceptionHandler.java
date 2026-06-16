package com.eagle.common.handler;

import com.eagle.common.dto.ErrorResult;
import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.common.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.stream.Collectors;


/**
 * 全局异常统一处理
 * <p>
 * 消息解析策略：所有 exception.getMessage() 先尝试作为 i18n key 解析，
 * 若 key 不存在则直接返回原文（向后兼容）。
 *
 * @author sunshixiong
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // ==================== 新类型化异常处理器（携带数字 errorCode）====================

    /**
     * 处理 NotFoundException — HTTP 404，携带数字 errorCode
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorResult handleNotFoundException(NotFoundException e,
                                               HttpServletRequest request, Locale locale) {
        String msg = e.getLocalizedMessage(messageSource, locale);
        log.warn("资源不存在 [{}]: {}", e.getErrorCode().getCode(), msg, e);
        return ErrorResult.of(HttpStatus.NOT_FOUND, msg, e.getErrorCode().getCode(), request.getRequestURI());
    }

    /**
     * 处理 ConflictException — HTTP 409，携带数字 errorCode
     */
    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorResult handleConflictException(ConflictException e,
                                               HttpServletRequest request, Locale locale) {
        String msg = e.getLocalizedMessage(messageSource, locale);
        log.warn("资源冲突 [{}]: {}", e.getErrorCode().getCode(), msg, e);
        return ErrorResult.of(HttpStatus.CONFLICT, msg, e.getErrorCode().getCode(), request.getRequestURI());
    }

    /**
     * 处理 DomainException — HTTP 400，携带数字 errorCode
     */
    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorResult handleDomainException(DomainException e,
                                             HttpServletRequest request, Locale locale) {
        String msg = e.getLocalizedMessage(messageSource, locale);
        log.warn("领域异常 [{}]: {}", e.getErrorCode().getCode(), msg, e);
        return ErrorResult.of(HttpStatus.BAD_REQUEST, msg, e.getErrorCode().getCode(), request.getRequestURI());
    }

    /**
     * 处理 ServiceException — HTTP 500，携带数字 errorCode
     */
    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorResult handleServiceException(ServiceException e,
                                              HttpServletRequest request, Locale locale) {
        String msg = e.getLocalizedMessage(messageSource, locale);
        log.error("服务异常 [{}]: {}", e.getErrorCode().getCode(), msg, e);
        return ErrorResult.of(HttpStatus.INTERNAL_SERVER_ERROR, msg, e.getErrorCode().getCode(), request.getRequestURI());
    }

    /**
     * 处理参数校验异常 - 400
     * <p>
     * 字段错误消息支持 i18n：在 @NotBlank 等注解的 message 属性中使用 i18n key 即可。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorResult handleValidationException(MethodArgumentNotValidException e,
                                                 HttpServletRequest request, Locale locale) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + resolveMessage(error.getDefaultMessage(), locale))
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", message);
        return ErrorResult.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * 处理非法参数异常 - 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorResult handleIllegalArgument(IllegalArgumentException e,
                                             HttpServletRequest request, Locale locale) {
        String message = resolveMessage(e.getMessage(), locale);
        log.warn("非法参数：{}", message, e);
        return ErrorResult.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * 处理非法状态异常 - 400
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorResult handleIllegalState(IllegalStateException e,
                                          HttpServletRequest request, Locale locale) {
        String message = resolveMessage(e.getMessage(), locale);
        log.warn("非法状态：{}", message, e);
        return ErrorResult.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * 处理无权限异常 - 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorResult handleAccessDenied(AccessDeniedException e,
                                          HttpServletRequest request, Locale locale) {
        String message = resolveMessage("error.common.forbidden", locale);
        log.warn("无权限访问：{} {}", request.getMethod(), request.getRequestURI());
        return ErrorResult.of(HttpStatus.FORBIDDEN, message, request.getRequestURI());
    }

    /**
     * 处理其他未捕获异常 - 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorResult handleException(Exception e, HttpServletRequest request, Locale locale) {
        log.error("服务器异常：{}", e.getMessage(), e);
        String message = resolveMessage("error.server.internal_error", locale);
        return ErrorResult.of(HttpStatus.INTERNAL_SERVER_ERROR, message, request.getRequestURI());
    }

    /**
     * 解析消息：先尝试作为 i18n key，找不到则返回原文（降级）
     *
     * @param messageOrKey i18n key 或原始消息文本
     * @param locale       当前语言环境
     * @return 解析后的消息
     */
    private String resolveMessage(String messageOrKey, Locale locale) {
        if (messageOrKey == null) {
            return resolveMessage("error.server.internal_error", locale);
        }
        return messageSource.getMessage(messageOrKey, null, messageOrKey, locale);
    }
}
