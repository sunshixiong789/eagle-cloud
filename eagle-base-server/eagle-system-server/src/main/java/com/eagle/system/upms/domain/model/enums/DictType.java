package com.eagle.system.upms.domain.model.enums;

import lombok.Getter;

/**
 * 字典类型枚举
 *
 * @author sunshixiong
 */
@Getter
public enum DictType {
    /**
     * 用户性别
     */
    USER_GENDER("user_gender", "用户性别"),

    /**
     * 用户状态
     */
    USER_STATUS("user_status", "用户状态"),

    /**
     * 系统配置
     */
    SYSTEM_CONFIG("system_config", "系统配置"),

    /**
     * 业务类型
     */
    BUSINESS_TYPE("business_type", "业务类型"),

    /**
     * 数据范围
     */
    DATA_SCOPE("data_scope", "数据范围"),

    /**
     * 通知类型
     */
    NOTICE_TYPE("notice_type", "通知类型"),

    /**
     * 操作类型
     */
    OPERATION_TYPE("operation_type", "操作类型");

    private final String code;
    private final String description;

    DictType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static DictType fromCode(String code) {
        for (DictType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown dict type code: " + code);
    }
}
