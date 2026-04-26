package com.eagle.datapermission.helper;

import com.eagle.datapermission.enums.DataScope;
import com.eagle.datapermission.provider.DataPermissionProvider;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 数据权限辅助工具类。
 *
 * <p>根据当前用户的数据权限范围，生成 JPA {@link Specification} 查询条件。
 *
 * @author 孙士雄
 */
@Slf4j
public class DataPermissionHelper {

    private DataPermissionHelper() {
    }

    /**
     * 构建数据权限 Specification。
     *
     * @param provider    数据权限提供者
     * @param deptField   实体中部门 ID 字段名
     * @param userField   实体中用户 ID 字段名
     * @param existingSpec 已有的查询条件（可空）
     * @param <T>         实体类型
     * @return 带数据权限过滤的 Specification
     */
    public static <T> Specification<T> specification(
            DataPermissionProvider provider,
            String deptField,
            String userField,
            Specification<T> existingSpec) {

        DataScope scope = provider.getCurrentUserDataScope();
        if (scope == null) {
            scope = DataScope.SELF;
        }

        Specification<T> permissionSpec = buildPermissionSpec(provider, scope, deptField, userField);
        if (existingSpec == null) {
            return permissionSpec;
        }
        return existingSpec.and(permissionSpec);
    }

    /**
     * 构建数据权限 Specification（无已有条件）。
     */
    public static <T> Specification<T> specification(
            DataPermissionProvider provider,
            String deptField,
            String userField) {
        return specification(provider, deptField, userField, null);
    }

    private static <T> Specification<T> buildPermissionSpec(
            DataPermissionProvider provider,
            DataScope scope,
            String deptField,
            String userField) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            switch (scope) {
                case ALL -> {
                    // 不添加任何限制
                    return cb.conjunction();
                }
                case SELF -> {
                    Long userId = provider.getCurrentUserId();
                    if (userId != null) {
                        predicates.add(cb.equal(root.get(userField), userId));
                    }
                }
                case DEPT -> {
                    Long deptId = provider.getCurrentUserDeptId();
                    if (deptId != null) {
                        predicates.add(cb.equal(root.get(deptField), deptId));
                    }
                }
                case DEPT_AND_CHILD -> {
                    Long deptId = provider.getCurrentUserDeptId();
                    if (deptId != null) {
                        Set<Long> deptIds = provider.getChildDeptIds(deptId);
                        predicates.add(root.get(deptField).in(deptIds));
                    }
                }
                case CUSTOM -> {
                    Set<Long> deptIds = provider.getCurrentUserCustomDeptIds();
                    if (deptIds != null && !deptIds.isEmpty()) {
                        predicates.add(root.get(deptField).in(deptIds));
                    } else {
                        // 无自定义权限时 fallback 到仅本人
                        Long userId = provider.getCurrentUserId();
                        if (userId != null) {
                            predicates.add(cb.equal(root.get(userField), userId));
                        }
                    }
                }
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
