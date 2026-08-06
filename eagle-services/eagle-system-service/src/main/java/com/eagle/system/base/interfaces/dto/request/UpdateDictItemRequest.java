package com.eagle.system.base.interfaces.dto.request;

/**
 * 更新字典项请求
 * <p>
 * 字段为 null 表示「不修改」。
 */
public record UpdateDictItemRequest(
        String itemValue,
        String name,
        String description,
        Integer sortOrder,
        String remarks
) {
}
