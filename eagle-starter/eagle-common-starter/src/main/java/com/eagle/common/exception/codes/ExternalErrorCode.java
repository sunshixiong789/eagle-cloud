package com.eagle.common.exception.codes;

import com.eagle.common.exception.ErrorCode;

/**
 * 外部服务错误码（15001–15003）
 */
public enum ExternalErrorCode implements ErrorCode {

    EXTERNAL_SERVICE_ERROR(15001, "error.external.service_error", "外部服务调用失败"),
    EXTERNAL_SERVICE_TIMEOUT(15002, "error.external.service_timeout", "外部服务调用超时"),
    /**
     * 携带下游错误原因的透传码（供 HTTP 客户端错误处理器使用）。
     * 消息模板为 {@code {0}}，调用时须传入从下游 ErrorResult 提取的 message。
     */
    EXTERNAL_SERVICE_DETAIL(15003, "error.external.service_detail", "外部服务调用失败");

    private final ErrorCode.Meta meta;

    ExternalErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
