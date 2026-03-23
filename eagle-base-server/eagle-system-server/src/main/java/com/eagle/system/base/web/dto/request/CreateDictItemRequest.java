package com.eagle.system.base.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDictItemRequest {
    @NotNull
    private Long dictId;
    @NotBlank
    private String itemValue;
    @NotBlank
    private String name;
    @NotNull
    private String dictType;
    private Long parentId;
    private String description;
    private Integer sortOrder;
    private String remarks;
}
