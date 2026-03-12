package com.eleganteer.system.common.dto;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * 错误返回对象
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/8-11:16
 */
@Data
public class ErrorResult {
    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;
    private Integer errorCode;

    public static ErrorResult of(HttpStatus status, String message, String path) {
        ErrorResult apiError = new ErrorResult();
        apiError.timestamp = Instant.now();
        apiError.status = status.value();
        apiError.error = status.getReasonPhrase();
        apiError.message = message;
        apiError.path = path;
        return apiError;
    }

    public static ErrorResult of(HttpStatus status, String message,
                                 Integer errorCode, String path) {
        ErrorResult apiError = of(status, message, path);
        apiError.errorCode = errorCode;
        return apiError;
    }

}
