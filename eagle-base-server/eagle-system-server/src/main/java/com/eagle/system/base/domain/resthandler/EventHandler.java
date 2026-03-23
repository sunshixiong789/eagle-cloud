package com.eagle.system.base.domain.resthandler;

import com.eagle.system.base.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * spring data rest事件监听
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/11/18-19:35
 */
@Component
@RepositoryEventHandler
@RequiredArgsConstructor
public class EventHandler {
    private final PasswordEncoder passwordEncoder;

    /**
     * 新增用户密码加密
     *
     * @param user SysUserPO
     */
    @HandleBeforeCreate
    public void beforeCreate(User user) {
        // 新增用户密码加密

    }

    /**
     * 更新用户密码加密
     *
     * @param user userId
     */
    @HandleBeforeSave
    public void beforeSave(User user) {
        // 更新时不允许修改用户名
        // （需先从 DB 查询原对象对比，此处简化）
    }


}
