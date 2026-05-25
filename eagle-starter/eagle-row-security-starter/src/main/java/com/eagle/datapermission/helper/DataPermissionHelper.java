package com.eagle.datapermission.helper;

import com.eagle.datapermission.context.DataPermissionContext;
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
 * <p>权限范围判定优先级：{@link DataPermissionContext}（ThreadLocal 覆盖）
 * &gt; {@link DataPermissionProvider#getCurrentUserDataScope()}（Security Context）。
 *
 * @author eagle
 */
@Slf4j
public class DataPermissionHelper {

    private DataPermissionHelper() {
    }

    /**
     * 构建数据权限 Specification（供切面外直接调用）。
     *
     * <p>权限范围由 {@link DataPermissionContext} 或 {@link DataPermissionProvider} 自动解析。
     *
     * @param provider     数据权限提供者
     * @param deptField    实体中部门 ID 字段名
     * @param userField    实体中用户 ID 字段名
     * @param existingSpec 已有的查询条件（可空）
     * @param <T>          实体类型
     * @return 带数据权限过滤的 Specification
     */
    public static <T> Specification<T> specification(
            DataPermissionProvider provider,
            String deptField,
            String userField,
            Specification<T> existingSpec) {
        // ThreadLocal 优先，否则读 SecurityContext
        DataScope scope = DataPermissionContext.getScope();
        if (scope == null) {
            scope = provider.getCurrentUserDataScope();
        }
        if (scope == null) {
            log.warn("DataPermissionProvider returned null scope, falling back to SELF as fail-safe");
            scope = DataScope.SELF;
        }
        return specification(provider, scope, deptField, userField, existingSpec);
    }

    /**
     * 构建数据权限 Specification（供切面调用，scope 已在切面层解析）。
     *
     * <p>切面已在进入方法前解析好 scope，此重载避免重复调用 {@code getCurrentUserDataScope()}。
     *
     * @param provider     数据权限提供者
     * @param scope        已解析的权限范围
     * @param deptField    实体中部门 ID 字段名
     * @param userField    实体中用户 ID 字段名
     * @param existingSpec 已有的查询条件（可空）
     * @param <T>          实体类型
     * @return 带数据权限过滤的 Specification
     */
    public static <T> Specification<T> specification(
            DataPermissionProvider provider,
            DataScope scope,
            String deptField,
            String userField,
            Specification<T> existingSpec) {
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

    // -------------------------------------------------------------------------
    // 内部实现
    // -------------------------------------------------------------------------

    private static <T> Specification<T> buildPermissionSpec(
            DataPermissionProvider provider,
            DataScope scope,
            String deptField,
            String userField) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            switch (scope) {
                case ALL -> {
                    return cb.conjunction();
                }
                case SELF -> {
                    Long userId = provider.getCurrentUserId();
                    if (userId == null) {
                        log.warn("SELF scope: currentUserId is null, no filter applied");
                        return cb.conjunction();
                    }
                    predicates.add(cb.equal(root.get(userField), userId));
                }
                case DEPT -> {
                    Long deptId = provider.getCurrentUserDeptId();
                    if (deptId == null) {
                        log.warn("DEPT scope: currentUserDeptId is null, falling back to SELF");
                        Long userId = provider.getCurrentUserId();
                        if (userId != null) {
                            predicates.add(cb.equal(root.get(userField), userId));
                        }
                    } else {
                        predicates.add(cb.equal(root.get(deptField), deptId));
                    }
                }
                case DEPT_AND_CHILD -> {
                    Long deptId = provider.getCurrentUserDeptId();
                    if (deptId == null) {
                        log.warn("DEPT_AND_CHILD scope: currentUserDeptId is null, falling back to SELF");
                        Long userId = provider.getCurrentUserId();
                        if (userId != null) {
                            predicates.add(cb.equal(root.get(userField), userId));
                        }
                    } else {
                        Set<Long> deptIds = provider.getChildDeptIds(deptId);
                        if (deptIds == null || deptIds.isEmpty()) {
                            // 无子部门时退化为本部门
                            predicates.add(cb.equal(root.get(deptField), deptId));
                        } else {
                            predicates.add(root.get(deptField).in(deptIds));
                        }
                    }
                }
                case CUSTOM -> {
                    Set<Long> customDeptIds = provider.getCurrentUserCustomDeptIds();
                    if (customDeptIds != null && !customDeptIds.isEmpty()) {
                        predicates.add(root.get(deptField).in(customDeptIds));
                    } else {
                        // 无自定义部门权限时回退到仅本人数据
                        Long userId = provider.getCurrentUserId();
                        log.warn("CUSTOM scope: customDeptIds is empty, falling back to SELF. userId: {}", userId);
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