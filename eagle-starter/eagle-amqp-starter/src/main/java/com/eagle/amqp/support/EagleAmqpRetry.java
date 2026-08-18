package com.eagle.amqp.support;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;

/**
 * 消费侧重试判定：坏报文和强制回队不走进退避。
 *
 * <p>容器会把 listener 异常包成 {@code ListenerExecutionFailedException}，
 * {@code exceptionExcludes} 按类匹配不上，必须沿 cause 链判断。
 */
public final class EagleAmqpRetry {

    private EagleAmqpRetry() {
    }

    /**
     * @param thrown retry advice 拿到的异常
     * @return {@code false} 表示立刻交给 recoverer / 回队，不再退避
     */
    public static boolean shouldRetry(Throwable thrown) {
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            if (current instanceof AmqpRejectAndDontRequeueException
                    || current instanceof ImmediateRequeueAmqpException) {
                return false;
            }
        }
        return true;
    }
}
