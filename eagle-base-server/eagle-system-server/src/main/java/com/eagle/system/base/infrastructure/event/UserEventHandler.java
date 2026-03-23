package com.eagle.system.base.infrastructure.event;

import com.eagle.system.base.domain.event.UserCreatedEvent;
import com.eagle.system.base.domain.event.UserLockedEvent;
import com.eagle.system.base.domain.event.UserPasswordChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler {

    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("用户创建事件: username={}, phone={}, email={}",
                event.getUsername(), event.getPhone(), event.getEmail());
    }

    @EventListener
    public void handlePasswordChanged(UserPasswordChangedEvent event) {
        log.info("密码修改事件: userId={}, username={}",
                event.getUserId(), event.getUsername());
    }

    @EventListener
    public void handleUserLocked(UserLockedEvent event) {
        log.info("用户锁定事件: userId={}, username={}, reason={}",
                event.getUserId(), event.getUsername(), event.getReason());
    }
}
