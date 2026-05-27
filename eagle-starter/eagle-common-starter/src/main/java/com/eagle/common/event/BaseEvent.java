package com.eagle.common.event;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 领域事件基类
 * <p>
 * 所有领域事件都应继承此类
 * 领域事件特性：
 * 1. 记录了领域中发生的重要业务事实
 * 2. 事件名应该使用过去时态
 * <p>
 * eventId / occurredOn 不再使用 final 修饰：JSON 反序列化路径(fastjson2)
 * 走无参构造 + setter,必须能覆盖父类构造期生成的占位值,否则消费端拿到的
 * eventId 跟 publisher 端不一致,跨链路幂等失效。
 *
 * @author eagle
 * @since 1.0.0
 */
@Getter
@Setter
public class BaseEvent {

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件发生时间
     */
    private LocalDateTime occurredOn;

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
