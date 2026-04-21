package com.eagle.system.system.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequest {

    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 64)
    private String postCode;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 64)
    private String postName;

    @NotNull(message = "岗位排序不能为空")
    private Integer postSort;

    @Size(max = 500)
    private String remark;
}
