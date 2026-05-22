package com.eagle.auth.infrastructure.event.integration;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * 黑名单删除集成事件（跨服务）。tag {@code blacklist.removed}。
 *
 * @author sunshixiong
 */
@Getter
public class BlacklistRemovedIntegrationEvent extends BaseEvent {

    private final Long id;
    private final String type;
    private final String value;

    public BlacklistRemovedIntegrationEvent(Long id, String type, String value) {
        this.id = id;
        this.type = type;
        this.value = value;
    }
}
