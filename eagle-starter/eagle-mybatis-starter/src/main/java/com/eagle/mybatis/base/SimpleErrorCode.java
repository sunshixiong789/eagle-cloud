package com.eagle.mybatis.base;

import com.eagle.common.exception.ErrorCode;
import com.eagle.common.exception.codes.CommonErrorCode;

/**
 * 将任意消息文本包装为 {@link ErrorCode} 的轻量实现。
 *
 * <p>错误码和 i18n key 固定复用 {@link CommonErrorCode#NOT_FOUND}，
 * 仅 {@code defaultMessage} 由调用方传入，用于 {@link com.eagle.mybatis.base.IEagleService#getByIdOrThrow} 等场景，
 * 避免为每个"记录不存在"场景单独定义枚举常量。
 */
class SimpleErrorCode implements ErrorCode {

    private final ErrorCode.Meta meta;

    SimpleErrorCode(String defaultMessage) {
        this.meta = new ErrorCode.Meta(
                CommonErrorCode.NOT_FOUND.getCode(),
                CommonErrorCode.NOT_FOUND.getMessageKey(),
                defaultMessage
        );
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
