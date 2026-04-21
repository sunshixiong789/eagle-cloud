package com.eagle.system.upms.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDeptRequest {

    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 100)
    private String name;

    private Long leaderId;

    @Size(max = 20)
    private String phone;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;
}
