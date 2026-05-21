package com.eagle.example.integration.permission;

import com.eagle.datapermission.provider.DataPermissionProvider;
import com.eagle.datapermission.enums.DataScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * 行级数据权限 Starter 验证：自定义数据权限提供者。
 */
@Slf4j
@Component
public class ExampleDataPermissionProvider implements DataPermissionProvider {

    @Override
    public DataScope getCurrentUserDataScope() {
        log.info("[DataPermission] Get current user data scope");
        return DataScope.ALL;
    }

    @Override
    public Long getCurrentUserDeptId() {
        return null;
    }

    @Override
    public Long getCurrentUserId() {
        return 1L;
    }

    @Override
    public Set<Long> getCurrentUserCustomDeptIds() {
        return Collections.emptySet();
    }

    @Override
    public Set<Long> getChildDeptIds(Long deptId) {
        return Collections.emptySet();
    }
}
