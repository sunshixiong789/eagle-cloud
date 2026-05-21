package com.eagle.system.message.domain.model;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * UserMessage 已落库的领域事件。
 *
 * <p>由 {@link UserMessage#create} 注册到聚合根，事务提交后由
 * {@code WebSocketRealtimePushListener} 监听并执行 WebSocket 推送。
 *
 * <p>本事件**不发布到 RocketMQ**——仅在本进程内通过 Spring 应用事件机制传递，
 * 因此不视为跨服务集成事件，schema 可自由演进。
 *
 * @author sunshixiong
 */
@Getter
public class UserMessageCreatedEvent extends BaseEvent {

    private final Long messageId;
    private final Long userId;
    private final MessageCategory category;
    private final String title;
    private final String content;

    public UserMessageCreatedEvent(Long messageId, Long userId, MessageCategory category,
                                   String title, String content) {
        this.messageId = messageId;
        this.userId = userId;
        this.category = category;
        this.title = title;
        this.content = content;
    }
}
