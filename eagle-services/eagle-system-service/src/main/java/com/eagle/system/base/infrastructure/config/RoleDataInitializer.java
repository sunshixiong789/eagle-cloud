package com.eagle.system.base.infrastructure.config;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.enums.DataScope;
import com.eagle.system.base.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统角色种子初始化器。
 * <p>
 * 在应用启动完成后预置系统角色 {@code admin}、{@code user}(幂等,已存在则跳过)。
 * <p>
 * <strong>不再负责 admin User 角色分配</strong>: 该职责已下沉到
 * {@link com.eagle.system.base.application.service.AccountEventApplicationService#onAccountRegistered}
 * ——消费 {@code AccountRegisteredMessage} 时按 {@code eagle.admin.username} 判定身份,
 * 直接分配 admin / user 角色。这样彻底消除"system-service 启动 → 同步等待 auth-service
 * 跨服务事件投递"的时序耦合。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class RoleDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(RoleDataInitializer.class);

    private static final String ADMIN_ROLE_CODE = "admin";
    private static final String USER_ROLE_CODE = "user";

    private final RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(rollbackFor = Exception.class)
    public void seedSystemRoles() {
        seedRole(ADMIN_ROLE_CODE, "系统管理员",
                "拥有所有系统管理权限", 1, DataScope.ALL);
        seedRole(USER_ROLE_CODE, "普通用户",
                "拥有基础查看权限", 2, DataScope.SELF);
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
