package com.eagle.common.event;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 领域事件基类
 * <p>
 * 所有领域事件都应继承此类
 * 领域事件特性：
 * 1. 不可变
 * 2. 记录了领域中发生的重要业务事实
 * 3. 事件名应该使用过去时态
 *
 * @author eagle
 * @since 1.0.0
 */
@Getter
public class BaseEvent {

    /**
     * 事件ID
     */
    private final String eventId;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;

    protected BaseEvent() {
        this.eventId = UuidCreator.getTimeOrderedEpoch().toString();
        this.occurredOn = LocalDateTime.now();
    }

    /**
     * 获取事件类型
     *
     * @return 事件类型名称
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
