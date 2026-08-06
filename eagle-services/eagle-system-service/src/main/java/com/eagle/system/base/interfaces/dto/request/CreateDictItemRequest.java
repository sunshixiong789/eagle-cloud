package com.eagle.system.base.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建字典项请求
 * <p>
 * dictId 和 dictType 由路径变量和聚合根提供，无需在请求体中传递
 *
 * @param itemValue   字典项值
 * @param name        字典项标签
 * @param parentId    父级字典项 ID，0 或 null 表示顶级
 * @param description 描述
 * @param sortOrder   排序值
 * @param remarks     备注
 * @author sunshixiong
 */
public record CreateDictItemRequest(

        @NotBlank(message = "字典项值不能为空")
        String itemValue,

        @NotBlank(message = "字典项标签不能为空")
        String name,

        Long parentId,

        String description,

        Integer sortOrder,

        String remarks
) {
}
