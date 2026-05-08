package com.eagle.system.base.infrastructure.config;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.enums.DataScope;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
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
 * 角色数据初始化器
 * <p>
 * 在应用启动完成后预置系统角色（admin、user），并为管理员用户分配 admin 角色。
 * 使用 {@link ApplicationReadyEvent} 确保在所有 ApplicationRunner 完成后执行。
 * <p>
 * admin User 由 {@code handleAccountRegistered} 异步创建，与本初始化器存在竞态。
 * 为此 {@code assignAdminRoleWithRetry} 每次重试均开启独立事务，以读取到对方已提交的数据，
 * 最多重试 10 次（间隔 500ms），超时则跳过（非首次启动时 User 已存在，不影响正常运行）。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class RoleDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(RoleDataInitializer.class);

    private static final String ADMIN_ROLE_CODE = "admin";
    private static final String USER_ROLE_CODE = "user";
    private static final String ADMIN_USERNAME = "admin";
    private static final int MAX_RETRY = 10;
    private static final long RETRY_INTERVAL_MS = 500L;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * 初始化系统角色并分配管理员角色
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        Long adminRoleId = seedSystemRoles();
        assignAdminRoleWithRetry(adminRoleId);
    }

    private Long seedSystemRoles() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Role adminRole = seedRole(ADMIN_ROLE_CODE, "系统管理员",
                    "拥有所有系统管理权限", 1, DataScope.ALL);
            seedRole(USER_ROLE_CODE, "普通用户",
                    "拥有基础查看权限", 2, DataScope.SELF);
            return adminRole.getId();
        });
    }

    /**
     * 带重试的管理员角色分配。
     * <p>
     * admin User 由异步事件处理器创建，与本方法存在竞态，每次重试开独立事务以读取最新提交数据。
     */
    private void assignAdminRoleWithRetry(Long adminRoleId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            Boolean assigned = tx.execute(status ->
                    userRepository.findByUsername(ADMIN_USERNAME).map(user -> {
                        // 保留 UserEventHandler 已分配的默认 user 角色, 仅在缺失 admin 角色时追加
                        if (!user.getRoleIds().contains(adminRoleId)) {
                            Set<Long> mergedRoleIds = new HashSet<>(user.getRoleIds());
                            mergedRoleIds.add(adminRoleId);
                            user.assignRoles(mergedRoleIds);
                            userRepository.save(user);
                            log.info("已为管理员用户分配 admin 角色, username: {}", ADMIN_USERNAME);
                        }
                        return true;
                    }).orElse(false)
            );
            if (Boolean.TRUE.equals(assigned)) return;

            if (attempt < MAX_RETRY) {
                log.debug("等待管理员用户创建中 ({}/{})...", attempt, MAX_RETRY);
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("角色分配等待被中断");
                    return;
                }
            }
        }
        log.warn("已重试 {} 次仍未找到管理员用户，跳过角色分配（非首次启动则属正常）, username: {}",
                MAX_RETRY, ADMIN_USERNAME);
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
}
