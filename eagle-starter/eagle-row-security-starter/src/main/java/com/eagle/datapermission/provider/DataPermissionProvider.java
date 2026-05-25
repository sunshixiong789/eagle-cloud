package com.eagle.datapermission.provider;

import com.eagle.datapermission.enums.DataScope;

import java.util.Set;

/**
 * 数据权限提供者接口。
 *
 * <p>各业务服务需实现此接口，提供当前登录用户的数据权限信息。
 *
 * @author eagle
 */
public interface DataPermissionProvider {

    /**
     * 获取当前用户的数据权限范围。
     *
     * @return 数据权限范围（取用户所有角色中最大的范围）
     */
    DataScope getCurrentUserDataScope();

    /**
     * 获取当前用户的部门 ID。
     *
     * @return 部门 ID，无部门返回 null
     */
    Long getCurrentUserDeptId();

    /**
     * 获取当前用户 ID。
     *
     * @return 用户 ID
     */
    Long getCurrentUserId();

    /**
     * 获取当前用户自定义权限的部门 ID 集合（CUSTOM 范围用）。
     *
     * @return 部门 ID 集合
     */
    Set<Long> getCurrentUserCustomDeptIds();

    /**
     * 获取指定部门的所有子部门 ID（包含自身）。
     *
     * @param deptId 部门 ID
     * @return 部门 ID 集合（包含自身）
     */
    Set<Long> getChildDeptIds(Long deptId);
}
