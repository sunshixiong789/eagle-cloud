package com.eleganteer.system.system.domain.event;

import com.eleganteer.eleganteer.common.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 用户已锁定事件
 * <p>
 * 当用户账户被锁定时发布此事件
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Getter
public class UserLockedEvent extends BaseDomainEvent {

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 用户名
     */
    private final String username;

    /**
     * 锁定原因
     */
    private final String reason;

    public UserLockedEvent(Long userId, String username, String reason) {
        super();
        this.userId = userId;
        this.username = username;
        this.reason = reason;
    }
}
