package com.eagle.system.base.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDictRequest {

    @NotNull(message = "字典类型不能为空")
    private String dictType;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100)
    private String dictName;

    @Size(max = 255)
    private String description;

    @Size(max = 500)
    private String remarks;
}
