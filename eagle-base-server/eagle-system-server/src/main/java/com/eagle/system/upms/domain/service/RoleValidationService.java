package com.eagle.system.upms.domain.service;

import java.util.Set;

/**
 * 角色分配领域服务
 * <p>
 * 负责跨聚合的角色有效性校验业务规则：
 * <ul>
 *   <li>角色必须存在</li>
 *   <li>角色必须处于启用状态</li>
 * </ul>
 * 此类校验跨越 User 聚合和 Role 聚合，属于领域服务范畴。
 *
 * @author sunshixiong
 */
public interface RoleValidationService {

    /**
     * 校验角色 ID 集合有效性
     * <p>
     * 若任意角色不存在或已禁用，则抛出 {@link com.eagle.common.exception.DomainException}。
     *
     * @param roleIds 待校验的角色 ID 集合
     */
    void validateRoles(Set<Long> roleIds);
}
