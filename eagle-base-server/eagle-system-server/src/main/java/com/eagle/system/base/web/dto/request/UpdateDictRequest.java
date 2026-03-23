package com.eagle.system.base.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDictRequest {

    @Size(max = 100)
    private String dictName;

    @Size(max = 255)
    private String description;

    @Size(max = 500)
    private String remarks;
}
