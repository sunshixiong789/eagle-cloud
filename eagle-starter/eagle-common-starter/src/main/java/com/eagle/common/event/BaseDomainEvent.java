package com.eagle.common.event;

import java.time.LocalDateTime;

/**
 * 领域事件基类
 *
 * @author sunshixiong
 */
public abstract class BaseDomainEvent {

    private final LocalDateTime occurredOn;

    protected BaseDomainEvent() {
        this.occurredOn = LocalDateTime.now();
    }

    public LocalDateTime occurredOn() {
        return occurredOn;
    }
}
