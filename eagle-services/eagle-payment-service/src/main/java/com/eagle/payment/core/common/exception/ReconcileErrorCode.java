package com.eagle.payment.core.common.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * Reconcile 域错误码 (70060-70079)。
 *
 * @author sunshixiong
 */
public enum ReconcileErrorCode implements ErrorCode {

    FETCH_FAILED(70060, "error.reconcile.fetch_failed", "渠道清算单拉取失败"),
    PARSE_FAILED(70061, "error.reconcile.parse_failed", "渠道清算单解析失败");

    private final ErrorCode.Meta meta;

    ReconcileErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
