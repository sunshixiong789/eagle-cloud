package com.eagle.system.base.infrastructure.event;

import com.eagle.system.auth.domain.event.AccountDeletedEvent;
import com.eagle.system.auth.domain.event.AccountRegisteredEvent;
import com.eagle.system.base.domain.event.UserCreatedEvent;
import com.eagle.system.base.domain.event.UserLockedEvent;
import com.eagle.system.base.domain.event.UserPasswordChangedEvent;
import com.eagle.system.base.domain.event.UserUpdatedEvent;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 用户领域事件处理器
 * <p>
 * 职责：
 * <ul>
 *   <li>监听并处理用户相关的领域事件</li>
 *   <li>监听 auth 域的跨域事件（AccountRegisteredEvent、AccountDeletedEvent）</li>
 *   <li>实现跨聚合的业务逻辑（如发送通知、记录日志）</li>
 *   <li>保持领域模型的纯粹性（领域模型不依赖基础设施）</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler {

    private final CacheManager cacheManager;
    private final UserRepository userRepository;

    /**
     * 处理用户创建事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("用户创建事件: username={}, phone={}, email={}",
                event.getUsername(), event.getPhone(), event.getEmail());
    }

    /**
     * 处理密码修改事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordChanged(UserPasswordChangedEvent event) {
        log.info("密码修改事件: userId={}, username={}",
                event.getUserId(), event.getUsername());
        evictUserCache(event.getUsername());
    }

    /**
     * 处理用户锁定事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLocked(UserLockedEvent event) {
        log.info("用户锁定事件: userId={}, username={}, reason={}",
                event.getUserId(), event.getUsername(), event.getReason());
        evictUserCache(event.getUsername());
    }

    /**
     * 处理用户信息变更事件 -- 驱动缓存失效
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("用户信息变更事件: userId={}, username={}",
                event.getUserId(), event.getUsername());
        evictUserCache(event.getUsername());
    }

    /**
     * 处理账号注册事件(来自 auth 域,通过 auth::event Named Interface)
     * <p>
     * 跨域协作机制:
     * <ul>
     *   <li>auth 域创建 Account 后发布 AccountRegisteredEvent</li>
     *   <li>system 域监听事件并自动创建对应的 User</li>
     *   <li>使用 REQUIRES_NEW 事务传播,确保 User 创建失败不影响 Account 创建</li>
     *   <li>profileHints(部门、角色、邮箱)由管理员创建时携带,自主注册时为空</li>
     * </ul>
     *
     * @param event 账号注册事件
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAccountRegistered(AccountRegisteredEvent event) {
        // 幂等性检查:防止重复处理同一事件
        if (userRepository.existsByAccountId(event.accountId())) {
            log.debug("User already exists for accountId: {}", event.accountId());
            return;
        }
        // 构建 UserProfile (如果有昵称或头像)
        UserProfile profile = null;
        if (event.nickname() != null || event.avatar() != null) {
            profile = new UserProfile(
                    event.avatar(), event.nickname(), null, null, null);
        }
        // 创建 User
        User user = User.createForAccount(
                event.accountId(), event.username(), event.phone(), profile);
        // 应用 profile hints (管理员创建时携带的部门、角色、邮箱)
        if (event.email() != null) {
            user.updateContact(event.email());
        }
        if (event.deptId() != null) {
            user.assignDept(event.deptId());
        }
        if (event.roleIds() != null && !event.roleIds().isEmpty()) {
            user.assignRoles(event.roleIds());
        }
        userRepository.save(user);
        log.info("User created from AccountRegisteredEvent, accountId: {}, username: {}",
                event.accountId(), event.username());
    }

    /**
     * 处理账号删除事件(来自 auth 域,通过 auth::event Named Interface)
     * <p>
     * 跨域级联删除:
     * <ul>
     *   <li>auth 域删除 Account 后发布 AccountDeletedEvent</li>
     *   <li>system 域监听事件并级联删除对应的 User</li>
     *   <li>使用 REQUIRES_NEW 事务传播,确保删除操作独立提交</li>
     *   <li>删除后清除用户缓存,防止脏数据</li>
     * </ul>
     *
     * @param event 账号删除事件
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAccountDeleted(AccountDeletedEvent event) {
        userRepository.findByAccountId(event.accountId()).ifPresent(user -> {
            userRepository.delete(user);
            // 清除缓存,防止脏数据
            evictUserCache(user.getUsername());
            log.info("User deleted from AccountDeletedEvent, accountId: {}",
                    event.accountId());
        });
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
