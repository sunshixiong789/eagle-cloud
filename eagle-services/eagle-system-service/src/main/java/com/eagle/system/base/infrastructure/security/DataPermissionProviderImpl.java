package com.eagle.system.base.infrastructure.security;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import com.eagle.datapermission.enums.DataScope;
import com.eagle.datapermission.provider.DataPermissionProvider;
import com.eagle.resource.server.util.SecurityUtils;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限提供者实现。
 *
 * <p>基于 system-server 的用户-角色模型提供数据权限信息。
 * <p>部门管理已下线，CUSTOM/DEPT/DEPT_AND_CHILD 范围在缺少部门数据时退化为空集合，
 * 由 datapermission 切面回退到 SELF 范围。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPermissionProviderImpl implements DataPermissionProvider {

    private static final Map<DataScope, Integer> SCOPE_PRIORITY = new EnumMap<>(DataScope.class);

    static {
        SCOPE_PRIORITY.put(DataScope.ALL, 5);
        SCOPE_PRIORITY.put(DataScope.CUSTOM, 4);
        SCOPE_PRIORITY.put(DataScope.DEPT_AND_CHILD, 3);
        SCOPE_PRIORITY.put(DataScope.DEPT, 2);
        SCOPE_PRIORITY.put(DataScope.SELF, 1);
    }

    private final RoleRepository roleRepository;

    @Override
    public DataScope getCurrentUserDataScope() {
        Set<String> roleCodes = extractRoleCodes();
        if (roleCodes.isEmpty()) {
            return DataScope.SELF;
        }

        return roleRepository.findAll().stream()
                .filter(r -> roleCodes.contains(r.getRoleCode()))
                .map(Role::getDataScope)
                .map(com.eagle.system.base.domain.model.enums.DataScope::name)
                .map(DataScope::valueOf)
                .max(Comparator.comparingInt(SCOPE_PRIORITY::get))
                .orElse(DataScope.SELF);
    }

    @Override
    public Long getCurrentUserDeptId() {
        // 部门管理已下线，EagleUser 不再携带 deptId
        return null;
    }

    @Override
    public Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    @Override
    public Set<Long> getCurrentUserCustomDeptIds() {
        // 部门管理已下线，CUSTOM 范围无数据来源
        return Set.of();
    }

    @Override
    public Set<Long> getChildDeptIds(Long deptId) {
        // 部门管理已下线，无法查询子部门，返回自身 ID 兜底
        if (deptId == null) {
            return Set.of();
        }
        return Set.of(deptId);
    }

    private Set<String> extractRoleCodes() {
        EagleUser user = SecurityUtils.getCurrentUser();
        if (user == null || user.getAuthorities() == null) {
            return Set.of();
        }
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith(SecurityConstants.ROLE_START))
                .map(auth -> auth.substring(SecurityConstants.ROLE_START.length()))
                .collect(Collectors.toSet());
    }
}
