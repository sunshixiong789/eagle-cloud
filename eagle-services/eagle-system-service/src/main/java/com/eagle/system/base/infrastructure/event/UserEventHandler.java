package com.eagle.system.base.infrastructure.event;

import com.eagle.common.util.LogMask;
import com.eagle.system.base.domain.event.UserCreatedEvent;
import com.eagle.system.base.domain.event.UserPasswordChangedEvent;
import com.eagle.system.base.domain.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 用户本地领域事件处理器
 * <p>
 * 职责:监听本域产生的 User 相关事件,异步记录日志、失效缓存。
 * <p>
 * 来自 auth-service 的跨服务集成事件(AccountRegistered / AccountDeleted)
 * 改由 {@code AccountRegisteredConsumer} / {@code AccountDeletedConsumer}
 * 通过 RocketMQ 消费,业务逻辑下沉至 {@code AccountEventApplicationService}。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler {

    private final CacheManager cacheManager;

    /**
     * 处理用户创建事件
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("用户创建事件: username={}, phone={}, email={}",
                event.getUsername(), LogMask.phone(event.getPhone()), LogMask.email(event.getEmail()));
    }

    /**
     * 处理密码修改事件
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordChanged(UserPasswordChangedEvent event) {
        log.info("密码修改事件: userId={}, username={}",
                event.getUserId(), event.getUsername());
        evictUserCache(event.getUsername());
    }

    /**
     * 处理用户信息变更事件 -- 驱动缓存失效
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("用户信息变更事件: userId={}, username={}",
                event.getUserId(), event.getUsername());
        evictUserCache(event.getUsername());
    }

    /**
     * 精准失效指定用户名的缓存条目
     */
    private void evictUserCache(String username) {
        var cache = cacheManager.getCache("USER_NAME");
        if (cache != null) {
            cache.evict(username);
            log.debug("已清除用户缓存: {}", username);
        }
    }
}
