package com.eagle.system.message.announcement.domain.model;

/**
 * 公告受众类型。
 *
 * @author sunshixiong
 */
public enum TargetType {

    /** 全体用户可见。 */
    ALL,

    /** 按角色过滤；{@code TargetFilter.roles} 命中任一即可见。 */
    ROLE,

    /** 按用户标签过滤；{@code TargetFilter.tags} 命中任一即可见。 */
    TAG
}
