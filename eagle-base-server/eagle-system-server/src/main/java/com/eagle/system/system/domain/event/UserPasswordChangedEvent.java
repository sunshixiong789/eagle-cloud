package com.eagle.system.system.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户密码已修改事件
 * <p>
 * 当用户修改密码成功时发布此事件
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserPasswordChangedEvent {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;
}
