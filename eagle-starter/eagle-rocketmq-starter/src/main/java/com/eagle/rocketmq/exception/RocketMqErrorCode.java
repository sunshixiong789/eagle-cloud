package com.eagle.rocketmq.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * RocketMQ 错误码（16001–16004）。
 *
 * @author eagle
 */
public enum RocketMqErrorCode implements ErrorCode {

    PRODUCER_INIT_FAILED(16001, "error.rocketmq.producer_init_failed", "消息生产者初始化失败"),
    CONSUMER_INIT_FAILED(16002, "error.rocketmq.consumer_init_failed", "消息消费者初始化失败"),
    PUBLISH_FAILED(16003, "error.rocketmq.publish_failed", "消息发布失败"),
    PUBLISH_TIMEOUT(16004, "error.rocketmq.publish_timeout", "消息发布超时");

    private final ErrorCode.Meta meta;

    RocketMqErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
