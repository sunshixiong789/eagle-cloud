package com.eagle.datapermission.enums;

/**
 * 数据权限范围枚举。
 *
 * @author eagle
 */
public enum DataScope {

    /**
     * 全部数据。
     */
    ALL,

    /**
     * 自定义数据权限（指定部门）。
     */
    CUSTOM,

    /**
     * 本部门数据。
     */
    DEPT,

    /**
     * 本部门及子部门数据。
     */
    DEPT_AND_CHILD,

    /**
     * 仅本人数据。
     */
    SELF
}
