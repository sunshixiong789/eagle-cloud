package com.eagle.system.base.interfaces.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 更新字典请求
 * <p>
 * 字段为 null 表示「不修改」。
 */
public record UpdateDictRequest(

        @Size(max = 100)
        String dictName,

        @Size(max = 255)
        String description,

        @Size(max = 500)
        String remarks
) {
}
