package com.eagle.system.system.domain.event;

import com.eagle.eagle.common.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 用户密码已修改事件
 * <p>
 * 当用户修改密码成功时发布此事件
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Getter
public class UserPasswordChangedEvent extends BaseDomainEvent {

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 用户名
     */
    private final String username;

    public UserPasswordChangedEvent(Long userId, String username) {
        super();
        this.userId = userId;
        this.username = username;
    }
}
