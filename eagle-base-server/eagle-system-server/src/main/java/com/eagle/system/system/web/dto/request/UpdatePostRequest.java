package com.eagle.system.system.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePostRequest {

    @Size(max = 64)
    private String postName;

    private Integer postSort;

    @Size(max = 500)
    private String remark;
}
