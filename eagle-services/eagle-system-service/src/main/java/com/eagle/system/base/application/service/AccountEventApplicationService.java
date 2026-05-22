package com.eagle.system.base.application.service;

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

    @Transactional(rollbackFor = Exception.class)
    public void onAccountRegistered(AccountRegisteredMessage event) {
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
