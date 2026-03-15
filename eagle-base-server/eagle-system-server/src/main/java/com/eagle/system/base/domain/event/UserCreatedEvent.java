package com.eagle.system.base.domain.event;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * 用户已创建事件
 * <p>
 * 当新用户注册成功时发布此事件
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Getter
public class UserCreatedEvent extends BaseEvent {

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 用户名
     */
    private final String username;

    /**
     * 手机号
     */
    private final String phone;

    /**
     * 邮箱
     */
    private final String email;

    public UserCreatedEvent(Long userId, String username, String phone, String email) {
        super();
        this.userId = userId;
        this.username = username;
        this.phone = phone;
        this.email = email;
    }
}
