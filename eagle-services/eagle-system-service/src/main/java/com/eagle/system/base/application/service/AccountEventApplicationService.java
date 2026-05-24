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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 处理来自 auth-service 的账号集成事件(RocketMQ 消费侧业务逻辑)。
 * <p>
 * 拆服务前为 base/infrastructure/event/UserEventHandler 里的两个
 * {@code @TransactionalEventListener},拆分后改由 RocketMQ Consumer 触发。
 * <p>
 * <strong>幂等策略</strong>:
 * <ul>
 *   <li>register: 先查 {@code existsByAccountId} 跳过显式重复;并发场景下 DB 层 {@code account_id}
 *       唯一索引(见 {@code User} 实体 {@code @Index(unique = true)})兜底,
 *       并发 INSERT 会抛 {@link DataIntegrityViolationException} 被本服务转换为静默跳过(已是预期的重复)</li>
 *   <li>delete: {@code findByAccountId} 找不到即跳过(级联删除已生效)</li>
 * </ul>
 * <p>
 * <strong>不引入 Redis SETNX 幂等</strong>: SETNX 与 JPA 事务跨事务域,占位先于事务回滚会导致
 * 后续重投递被假成功跳过 → 永不进 DLQ → 静默丢失。DB unique 约束是唯一可靠的幂等防线。
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
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // existsByAccountId 与 save 之间的并发窗口被 unique 约束兜住 — 已是预期重复,静默跳过
            log.debug("concurrent insert detected for accountId={}, treated as duplicate",
                    event.getAccountId());
            return;
        }
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
