package com.eagle.system.base.infrastructure.persistence;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.enums.RoleStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * 角色动态查询条件
 *
 * @author sunshixiong
 */
public class RoleSpecification {

    private RoleSpecification() {}

    /**
     * 角色名称模糊匹配
     *
     * @param roleName 角色名称
     * @return Specification
     */
    public static Specification<Role> roleNameLike(String roleName) {
        return (root, query, cb) ->
            StringUtils.hasText(roleName)
                ? cb.like(root.get("roleName"), "%" + roleName + "%")
                : cb.conjunction();
    }

    /**
     * 角色标识精确匹配
     *
     * @param roleCode 角色标识
     * @return Specification
     */
    public static Specification<Role> roleCodeEquals(String roleCode) {
        return (root, query, cb) ->
            StringUtils.hasText(roleCode)
                ? cb.equal(root.get("roleCode"), roleCode)
                : cb.conjunction();
    }

    /**
     * 状态精确匹配
     *
     * @param status 角色状态
     * @return Specification
     */
    public static Specification<Role> statusEquals(RoleStatus status) {
        return (root, query, cb) ->
            status != null
                ? cb.equal(root.get("status"), status)
                : cb.conjunction();
    }
}
