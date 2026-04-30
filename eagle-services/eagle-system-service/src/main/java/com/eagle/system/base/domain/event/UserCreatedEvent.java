package com.eagle.system.base.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户已创建事件
 * <p>
 * 当新用户注册成功时发布此事件
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserCreatedEvent {

    /**
     * 用户ID
     */
    private  Long userId;

    /**
     * 用户名
     */
    private  String username;

    /**
     * 手机号
     */
    private  String phone;

    /**
     * 邮箱
     */
    private  String email;

}
