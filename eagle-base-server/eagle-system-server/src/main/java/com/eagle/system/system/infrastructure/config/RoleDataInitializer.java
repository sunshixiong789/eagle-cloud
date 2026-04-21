package com.eagle.system.system.infrastructure.config;

import com.eagle.system.domain.model.Role;
import com.eagle.system.domain.model.enums.DataScope;
import com.eagle.system.domain.repository.RoleRepository;
import com.eagle.system.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 角色数据初始化器
 * <p>
 * 在应用启动完成后预置系统角色（admin、user），并为管理员用户分配 admin 角色。
 * 使用 {@link ApplicationReadyEvent} 确保在所有 ApplicationRunner 完成后执行，
 * 此时管理员账号的异步事件已有足够时间完成 User 创建。
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

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    /**
     * 初始化系统角色并分配管理员角色
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(rollbackFor = Exception.class)
    public void initialize() {
        Role adminRole = seedRole(ADMIN_ROLE_CODE, "系统管理员",
                "拥有所有系统管理权限", 1, DataScope.ALL);
        seedRole(USER_ROLE_CODE, "普通用户",
                "拥有基础查看权限", 2, DataScope.SELF);

        assignAdminRole(adminRole.getId());
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

    private void assignAdminRole(Long adminRoleId) {
        userRepository.findByUsername(ADMIN_USERNAME).ifPresentOrElse(
                user -> {
                    if (user.getRoleIds().isEmpty()) {
                        user.assignRoles(Set.of(adminRoleId));
                        userRepository.save(user);
                        log.info("已为管理员用户分配 admin 角色, username: {}", ADMIN_USERNAME);
                    }
                },
                () -> log.warn("管理员用户尚未创建，跳过角色分配, username: {}", ADMIN_USERNAME)
        );
    }
}
