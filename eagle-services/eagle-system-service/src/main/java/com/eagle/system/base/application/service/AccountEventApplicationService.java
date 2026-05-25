package com.eagle.system.base.application.service;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.infrastructure.config.AdminProperties;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
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
 * <p>
 * <strong>角色分配策略</strong>: 创建 User 时按 username 是否等于 {@code eagle.admin.username}
 * 决定分配 user / user+admin 角色,与 auth-service 端 {@code AdminInitializer} 通过相同
 * 环境变量 {@code EAGLE_ADMIN_USERNAME} 联动。该判断下沉到事件消费现场,无需 system-service
 * 启动期同步等待跨服务事件投递。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEventApplicationService {

    /**
     * 系统角色码,与 RoleDataInitializer 中预置的系统角色 roleCode 保持一致。
     */
    private static final String DEFAULT_USER_ROLE_CODE = "user";
    private static final String ADMIN_ROLE_CODE = "admin";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CacheManager cacheManager;
    private final AdminProperties adminProperties;

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
        assignInitialRoles(user, event.getUsername());
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

    /**
     * 为新创建的 User 分配初始角色:
     * <ul>
     *   <li>普通用户 → 仅 user 角色</li>
     *   <li>管理员(username 匹配 {@code eagle.admin.username}) → user + admin 双角色,
     *       与 auth-service 的 {@code AdminInitializer} 通过相同环境变量
     *       {@code EAGLE_ADMIN_USERNAME} 联动。该判断把"哪个用户是 admin"的决策点
     *       下沉到事件消费现场,彻底消除启动期跨服务时序依赖。</li>
     * </ul>
     * <p>缺失角色定义时仅 {@code log.warn} 跳过,不抛异常——首次启动时角色 seed 与本消费可能并发,
     * 由 {@code RoleDataInitializer} 在 ApplicationReadyEvent 完成 seed 后,
     * 后续注册事件即可正常匹配。
     */
    private void assignInitialRoles(User user, String username) {
        Set<Long> roleIds = new LinkedHashSet<>();
        roleRepository.findByRoleCode(DEFAULT_USER_ROLE_CODE).map(Role::getId).ifPresentOrElse(
                roleIds::add,
                () -> log.warn("默认普通用户角色 [{}] 不存在, 跳过为新用户分配默认角色, username: {}",
                        DEFAULT_USER_ROLE_CODE, username));
        if (adminProperties.getUsername().equals(username)) {
            roleRepository.findByRoleCode(ADMIN_ROLE_CODE).map(Role::getId).ifPresentOrElse(
                    roleIds::add,
                    () -> log.warn("管理员角色 [{}] 不存在, 跳过为管理员用户分配 admin 角色, username: {}",
                            ADMIN_ROLE_CODE, username));
        }
        if (!roleIds.isEmpty()) {
            user.assignRoles(roleIds);
        }
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
