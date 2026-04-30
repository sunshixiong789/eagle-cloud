package com.eagle.system.base.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户信息变更事件
 * <p>
 * 当用户的资料、联系方式、角色、部门、岗位等信息发生变更时发布。
 * 消费方（如缓存处理器）通过此事件驱动缓存失效，无需在应用服务中手动 @CacheEvict。
 *
 * @author sunshixiong
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdatedEvent {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名（缓存 key）
     */
    private String username;
}
