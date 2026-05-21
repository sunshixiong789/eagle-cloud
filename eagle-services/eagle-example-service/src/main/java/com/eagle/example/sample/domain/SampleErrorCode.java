package com.eagle.example.sample.domain;

import com.eagle.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sample 模块错误码。
 */
@Getter
@RequiredArgsConstructor
public enum SampleErrorCode implements ErrorCode {

    PRODUCT_NOT_FOUND(200001, "error.sample.product_not_found", "商品不存在"),
    PRODUCT_NAME_EXISTS(200002, "error.sample.product_name_exists", "商品名称已存在"),
    PRODUCT_PRICE_INVALID(200003, "error.sample.product_price_invalid", "商品价格无效"),
    PRODUCT_STOCK_INSUFFICIENT(200004, "error.sample.product_stock_insufficient", "商品库存不足");

    private final ErrorCode.Meta meta;

    SampleErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
