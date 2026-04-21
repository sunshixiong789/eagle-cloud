package com.eagle.system.system.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户已锁定事件
 * <p>
 * 当用户账户被锁定时发布此事件
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserLockedEvent {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 锁定原因
     */
    private String reason;
}
