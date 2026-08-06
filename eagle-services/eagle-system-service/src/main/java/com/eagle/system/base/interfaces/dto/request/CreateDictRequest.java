package com.eagle.system.base.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDictRequest(

        @NotNull(message = "字典类型不能为空")
        String dictType,

        @NotBlank(message = "字典名称不能为空")
        @Size(max = 100)
        String dictName,

        @Size(max = 255)
        String description,

        @Size(max = 500)
        String remarks
) {
}
