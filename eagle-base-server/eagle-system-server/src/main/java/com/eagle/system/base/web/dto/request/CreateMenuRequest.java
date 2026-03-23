package com.eagle.system.base.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author sunshixiong
 */
@Data
public class CreateMenuRequest {

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64)
    private String name;

    @Size(max = 64)
    private String enName;

    @Size(max = 128)
    private String permission;

    private Long parentId = 0L;

    @Size(max = 128)
    private String icon;

    @Size(max = 500)
    private String path;

    @Size(max = 255)
    private String component;

    private Boolean visible;
    private Integer sortOrder;

    @NotNull(message = "菜单类型不能为空")
    private String menuType;
}
