package com.eagle.system.upms.infrastructure.security;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import com.eagle.datapermission.enums.DataScope;
import com.eagle.datapermission.provider.DataPermissionProvider;
import com.eagle.resource.server.util.SecurityUtils;
import com.eagle.system.upms.domain.model.Dept;
import com.eagle.system.upms.domain.model.Role;
import com.eagle.system.upms.domain.repository.DeptRepository;
import com.eagle.system.upms.domain.repository.RoleDeptRepository;
import com.eagle.system.upms.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限提供者实现。
 *
 * <p>基于 system-server 的用户-角色-部门模型提供数据权限信息。
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
    private final RoleDeptRepository roleDeptRepository;
    private final DeptRepository deptRepository;

    @Override
    public DataScope getCurrentUserDataScope() {
        Set<String> roleCodes = extractRoleCodes();
        if (roleCodes.isEmpty()) {
            return DataScope.SELF;
        }

        return roleRepository.findAll().stream()
                .filter(r -> roleCodes.contains(r.getRoleCode()))
                .map(Role::getDataScope)
                .map(com.eagle.system.upms.domain.model.enums.DataScope::name)
                .map(DataScope::valueOf)
                .max(Comparator.comparingInt(SCOPE_PRIORITY::get))
                .orElse(DataScope.SELF);
    }

    @Override
    public Long getCurrentUserDeptId() {
        EagleUser user = SecurityUtils.getCurrentUser();
        return user != null ? user.getDeptId() : null;
    }

    @Override
    public Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    @Override
    public Set<Long> getCurrentUserCustomDeptIds() {
        Set<String> roleCodes = extractRoleCodes();
        Set<Long> roleIds = roleRepository.findAll().stream()
                .filter(r -> roleCodes.contains(r.getRoleCode()))
                .map(Role::getId)
                .collect(Collectors.toSet());

        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return roleDeptRepository.findDeptIdsByRoleIdIn(roleIds);
    }

    @Override
    public Set<Long> getChildDeptIds(Long deptId) {
        if (deptId == null) {
            return Set.of();
        }
        Optional<Dept> deptOpt = deptRepository.findById(deptId);
        if (deptOpt.isEmpty()) {
            return Set.of(deptId);
        }
        String path = deptOpt.get().getDeptPath();
        Set<Long> ids = deptRepository.findIdsByDeptPathStartingWith(path);
        ids.add(deptId);
        return ids;
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
