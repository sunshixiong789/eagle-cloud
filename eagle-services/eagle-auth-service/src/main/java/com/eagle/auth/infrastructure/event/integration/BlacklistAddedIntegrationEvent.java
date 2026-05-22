package com.eagle.auth.infrastructure.event.integration;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 黑名单新增集成事件（跨服务）。tag {@code blacklist.added}。
 *
 * <p>{@code type} 序列化为 BlacklistType 枚举名字符串，跨服务不共享枚举类。
 *
 * @author sunshixiong
 */
@Getter
public class BlacklistAddedIntegrationEvent extends BaseEvent {

    private final Long id;
    private final String type;
    private final String value;
    private final LocalDateTime expiresAt;

    public BlacklistAddedIntegrationEvent(Long id, String type, String value, LocalDateTime expiresAt) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.expiresAt = expiresAt;
    }
}
