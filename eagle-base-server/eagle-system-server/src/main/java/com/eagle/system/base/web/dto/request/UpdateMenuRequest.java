package com.eagle.system.base.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMenuRequest {

    @Size(max = 64)
    private String name;

    @Size(max = 64)
    private String enName;

    @Size(max = 128)
    private String permission;

    @Size(max = 128)
    private String icon;

    @Size(max = 500)
    private String path;

    @Size(max = 255)
    private String component;

    private Boolean visible;
    private Integer sortOrder;
}
