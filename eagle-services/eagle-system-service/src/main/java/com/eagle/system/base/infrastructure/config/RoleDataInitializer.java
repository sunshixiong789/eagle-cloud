package com.eagle.system.base.infrastructure.config;

import com.eagle.common.exception.NotFoundException;
import com.eagle.system.base.application.service.AccountEventApplicationService;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.enums.DataScope;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.application.event.AccountRegisteredMessage;
import com.eagle.system.base.infrastructure.remote.AuthAccountClient;
import com.eagle.system.base.infrastructure.remote.dto.AccountSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.Set;

/**
 * 系统启动期数据初始化器。
 * <p>
 * 职责:
 * <ol>
 *   <li>{@link #seedSystemRoles()} 幂等预置系统角色 {@code admin} / {@code user}。</li>
 *   <li>{@link #ensureAdminUser()} <strong>不依赖 MQ</strong> 主动拉取 admin Account 兜底创建/修复 admin User。</li>
 * </ol>
 * <p>
 * <strong>为什么需要启动期 ensure</strong>:
 * auth-service {@code AdminInitializer} 创建 admin Account 时只 emit AccountRegisteredEvent
 * <em>一次</em>(对应本服务消费的 {@link AccountRegisteredMessage})。如果当时 MQ 链路任何一段不通
 * (producer 发不出 / broker 不存 / consumer 未启动 / handle 异常),system-service
 * 永远收不到事件 → admin User 永远不会被创建 → admin 登录后 hasRole('admin') 全 401。
 * <p>
 * 故启动期同步用 HTTP 兜底:从 auth-service 拉 admin Account 信息,本地创建对应 User 并赋
 * admin + user 角色。MQ 仍是常规同步通道,本兜底仅在 admin User 缺失时介入,业务正常时一次轻量 HTTP。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class RoleDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(RoleDataInitializer.class);

    private static final String SUPER_ADMIN_ROLE_CODE = "super_admin";
    private static final String ADMIN_ROLE_CODE = "admin";
    private static final String USER_ROLE_CODE = "user";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AdminProperties adminProperties;
    private final AuthAccountClient authAccountClient;
    private final AccountEventApplicationService accountEventService;
    private final PlatformTransactionManager transactionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        seedSystemRoles();
        ensureAdminUser();
    }

    /** 幂等预置系统角色;单独事务,失败不影响下游 ensureAdminUser。 */
    private void seedSystemRoles() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            seedRole(SUPER_ADMIN_ROLE_CODE, "超级管理员",
                    "系统最高权限,可执行平台级运维操作", 0, DataScope.ALL);
            seedRole(ADMIN_ROLE_CODE, "系统管理员",
                    "拥有所有系统管理权限", 1, DataScope.ALL);
            seedRole(USER_ROLE_CODE, "普通用户",
                    "拥有基础查看权限", 2, DataScope.SELF);
            return null;
        });
    }

    private Role seedRole(String roleCode, String roleName, String roleDesc,
                          Integer sortOrder, DataScope dataScope) {
        return roleRepository.findByRoleCode(roleCode).orElseGet(() -> {
            Role role = Role.createSystemRole(roleName, roleCode, roleDesc, sortOrder, dataScope);
            Role saved = roleRepository.save(role);
            log.info("系统角色初始化成功, roleCode: {}, roleName: {}", roleCode, roleName);
            return saved;
        });
    }

    /**
     * 兜底确保 admin User 存在且持有 admin 角色。HTTP 失败 / Account 未注册时静默跳过,
     * MQ 事件后续到达时仍能补救创建——本方法仅做"启动期主动拉取",不取代 MQ 链路。
     */
    private void ensureAdminUser() {
        String username = adminProperties.getUsername();
        AccountSnapshot snapshot;
        try {
            snapshot = authAccountClient.findByUsername(username);
        } catch (NotFoundException ex) {
            log.info("admin Account 尚未在 auth-service 创建, 跳过 User 兜底, username: {}", username);
            return;
        } catch (RuntimeException ex) {
            log.warn("启动期 HTTP 拉取 admin Account 失败, 将依赖 MQ 事件最终一致, username: {}, reason: {}",
                    username, ex.toString());
            return;
        }
        if (userRepository.existsByAccountId(snapshot.accountId())) {
            ensureAdminSystemRolesAssigned(snapshot.accountId());
            return;
        }
        AccountRegisteredMessage message = new AccountRegisteredMessage();
        message.setAccountId(snapshot.accountId());
        message.setUsername(snapshot.username());
        message.setPhone(snapshot.phone());
        accountEventService.onAccountRegistered(message);
        log.info("启动期兜底创建 admin User 完成, username: {}, accountId: {}",
                username, snapshot.accountId());
    }

    /**
     * admin User 已存在但缺 admin / super_admin 系统角色时补救
     * (用户被旧版本逻辑创建过、或后续新增 super_admin 系统角色后老用户未自动持有)。
     */
    private void ensureAdminSystemRolesAssigned(Long accountId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            Set<Long> missing = new HashSet<>();
            roleRepository.findByRoleCode(SUPER_ADMIN_ROLE_CODE).map(Role::getId).ifPresent(missing::add);
            roleRepository.findByRoleCode(ADMIN_ROLE_CODE).map(Role::getId).ifPresent(missing::add);
            if (missing.isEmpty()) {
                return null;
            }
            User user = userRepository.findByAccountId(accountId).orElse(null);
            if (user == null) {
                return null;
            }
            Set<Long> merged = new HashSet<>(user.getRoleIds());
            if (!merged.addAll(missing)) {
                return null;
            }
            user.assignRoles(merged);
            userRepository.save(user);
            log.info("补救为现有 admin User 分配 admin / super_admin 系统角色, accountId: {}", accountId);
            return null;
        });
    }
}
