package com.eagle.system.base.application.service;

import com.eagle.rocketmq.idempotency.IdempotencyChecker;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 处理来自 auth-service 的账号集成事件(RocketMQ 消费侧业务逻辑)。
 * <p>
 * 拆服务前为 base/infrastructure/event/UserEventHandler 里的两个
 * {@code @TransactionalEventListener},拆分后改由 RocketMQ Consumer 触发。
 * <p>
 * <strong>幂等策略</strong>:首层 {@link IdempotencyChecker} 用 {@code event.eventId} 做 Redis SETNX,
 * RocketMQ 至少一次投递的重投递在 24h 内只会处理一次;二层 {@code existsByAccountId} / 找不到即跳过
 * 是业务兜底,应对幂等键过期 + 真实重复(如运维手工补单)的极端场景。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEventApplicationService {

    /**
     * 默认普通用户角色码,新注册用户自动分配该角色。
     * 与 RoleDataInitializer 中预置的系统角色 roleCode 保持一致。
     */
    private static final String DEFAULT_USER_ROLE_CODE = "user";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CacheManager cacheManager;
    private final IdempotencyChecker idempotencyChecker;

    @Transactional(rollbackFor = Exception.class)
    public void onAccountRegistered(AccountRegisteredMessage event) {
        if (idempotencyChecker.isDuplicate(event.getEventId())) {
            log.debug("duplicate AccountRegistered event, eventId={}, accountId={}, skip",
                    event.getEventId(), event.getAccountId());
            return;
        }
        if (userRepository.existsByAccountId(event.getAccountId())) {
            log.debug("User already exists for accountId: {}", event.getAccountId());
            return;
        }
        UserProfile profile = null;
        if (event.getNickname() != null || event.getAvatar() != null) {
            profile = new UserProfile(event.getAvatar(), event.getNickname(), null, null, null);
        }
        User user = User.createForAccount(
                event.getAccountId(), event.getUsername(), event.getPhone(), profile);
        if (event.getEmail() != null) {
            user.updateContact(event.getEmail());
        }
        roleRepository.findByRoleCode(DEFAULT_USER_ROLE_CODE).ifPresentOrElse(
                role -> user.assignRoles(Set.of(role.getId())),
                () -> log.warn("默认普通用户角色 [{}] 不存在, 跳过为新用户分配默认角色, username: {}",
                        DEFAULT_USER_ROLE_CODE, event.getUsername())
        );
        userRepository.save(user);
        log.info("User created from AccountRegisteredMessage, accountId: {}, username: {}",
                event.getAccountId(), event.getUsername());
    }

    @Transactional(rollbackFor = Exception.class)
    public void onAccountDeleted(AccountDeletedMessage event) {
        if (idempotencyChecker.isDuplicate(event.getEventId())) {
            log.debug("duplicate AccountDeleted event, eventId={}, accountId={}, skip",
                    event.getEventId(), event.getAccountId());
            return;
        }
        userRepository.findByAccountId(event.getAccountId()).ifPresent(user -> {
            userRepository.delete(user);
            var cache = cacheManager.getCache("USER_NAME");
            if (cache != null) {
                cache.evict(user.getUsername());
            }
            log.info("User deleted from AccountDeletedMessage, accountId: {}",
                    event.getAccountId());
        });
    }
}
