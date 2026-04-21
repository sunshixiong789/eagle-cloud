package com.eagle.system.system.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建字典项请求
 * <p>
 * dictId 和 dictType 由路径变量和聚合根提供，无需在请求体中传递
 *
 * @author sunshixiong
 */
@Data
public class CreateDictItemRequest {

    @NotBlank(message = "字典项值不能为空")
    private String itemValue;

    @NotBlank(message = "字典项标签不能为空")
    private String name;

    /** 父级字典项 ID，0 或 null 表示顶级 */
    private Long parentId;

    private String description;

    private Integer sortOrder;

    private String remarks;
}
