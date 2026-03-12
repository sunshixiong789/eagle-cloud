package com.eleganteer.system.config.handler;

import com.eleganteer.eleganteer.common.dto.ErrorResult;
import com.eleganteer.eleganteer.common.exception.BusinessException;
import com.eleganteer.eleganteer.common.exception.ResourceConflictException;
import com.eleganteer.eleganteer.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;


/**
 * 全局异常统一处理
 * <p>
 * 将业务异常映射到合适的 HTTP 状态码
 *
 * @author sunshixiong
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理资源不存在异常 - 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorResult handleResourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("资源不存在：{}", e.getMessage());
        return ErrorResult.of(HttpStatus.NOT_FOUND, e.getMessage(), request.getRequestURI());
    }

    /**
     * 处理资源冲突异常 - 409
     * <p>
     * 常见场景：
     * - 用户名已存在
     * - 手机号已注册
     * - 邮箱已被使用
     * - 用户已被锁定/解锁
     */
    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorResult handleResourceConflict(ResourceConflictException e, HttpServletRequest request) {
        log.warn("资源冲突：{}", e.getMessage());
        return ErrorResult.of(HttpStatus.CONFLICT, e.getMessage(), request.getRequestURI());
    }

    /**
     * 处理参数校验异常 - 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorResult handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
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
    public ErrorResult handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数：{}", e.getMessage());
        return ErrorResult.of(HttpStatus.BAD_REQUEST, e.getMessage(), request.getRequestURI());
    }

    /**
     * 处理非法状态异常 - 409
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorResult handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        log.warn("状态冲突：{}", e.getMessage());
        return ErrorResult.of(HttpStatus.CONFLICT, e.getMessage(), request.getRequestURI());
    }

    /**
     * 处理数据库唯一性约束异常 - 409
     * <p>
     * 当数据库唯一索引冲突时（如用户名、手机号、邮箱重复），
     * 捕获 DataIntegrityViolationException 并返回友好的错误信息
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorResult handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        String message = "数据冲突";
        String errorMsg = e.getMessage();

        if (errorMsg != null) {
            if (errorMsg.contains("username") || errorMsg.contains("idx_username")) {
                message = "用户名已存在";
            } else if (errorMsg.contains("phone") || errorMsg.contains("idx_phone")) {
                message = "手机号已被注册";
            } else if (errorMsg.contains("email") || errorMsg.contains("idx_email")) {
                message = "邮箱已被注册";
            }
        }

        log.warn("数据库约束冲突：{}", message, e);
        return ErrorResult.of(HttpStatus.CONFLICT, message, request.getRequestURI());
    }

    /**
     * 处理业务异常 - 500
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorResult handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("业务异常：{}", e.getMessage(), e);
        return ErrorResult.of(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCode(), request.getRequestURI());
    }

    /**
     * 处理其他未捕获异常 - 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorResult handleException(Exception e, HttpServletRequest request) {
        log.error("服务器异常：{}", e.getMessage(), e);
        return ErrorResult.of(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误", request.getRequestURI());
    }
}
