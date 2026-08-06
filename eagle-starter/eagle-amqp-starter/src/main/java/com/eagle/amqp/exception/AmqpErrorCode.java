package com.eagle.amqp.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * 消息中间件错误码。
 *
 * <p>沿用原 {@code RocketMqErrorCode} 的 16000–16999 号段与具体码值 ——
 * 错误码是对外 API 契约，换中间件不应改变已发布的码。
 *
 * @author eagle
 */
public enum AmqpErrorCode implements ErrorCode {

    /**
     * 生产者初始化失败
     */
    PRODUCER_INIT_FAILED(16001, "error.amqp.producer_init_failed", "消息生产者初始化失败"),

    /**
     * 消费者初始化失败
     */
    CONSUMER_INIT_FAILED(16002, "error.amqp.consumer_init_failed", "消息消费者初始化失败"),

    /**
     * 消息发布失败
     */
    PUBLISH_FAILED(16003, "error.amqp.publish_failed", "消息发布失败");

    private final ErrorCode.Meta meta;

    AmqpErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
