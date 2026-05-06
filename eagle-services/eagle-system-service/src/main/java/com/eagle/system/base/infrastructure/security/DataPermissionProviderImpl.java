package com.eagle.system.base.infrastructure.security;

import com.eagle.datapermission.enums.DataScope;
import com.eagle.datapermission.provider.DataPermissionProvider;
import com.eagle.resource.server.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 数据权限提供者实现（简化版：始终返回 ALL，无角色/部门依赖）
 *
 * @author 孙士雄
 */
@Component
@RequiredArgsConstructor
public class DataPermissionProviderImpl implements DataPermissionProvider {

    @Override
    public DataScope getCurrentUserDataScope() {
        return DataScope.ALL;
    }

    @Override
    public Long getCurrentUserDeptId() {
        return null;
    }

    @Override
    public Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    @Override
    public Set<Long> getCurrentUserCustomDeptIds() {
        return Set.of();
    }

    @Override
    public Set<Long> getChildDeptIds(Long deptId) {
        return Set.of();
    }
}
