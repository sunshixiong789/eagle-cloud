package com.eleganteer.system.system.domain.model.enums;

/**
 * 数据范围
 *
 * @author sunshixiong
 */
public enum DataScope {
    /**
     * 全部数据权限
     */
    ALL,

    /**
     * 自定义数据权限
     */
    CUSTOM,

    /**
     * 本部门数据权限
     */
    DEPT,

    /**
     * 本部门及以下数据权限
     */
    DEPT_AND_CHILD,

    /**
     * 仅本人数据权限
     */
    SELF
}
