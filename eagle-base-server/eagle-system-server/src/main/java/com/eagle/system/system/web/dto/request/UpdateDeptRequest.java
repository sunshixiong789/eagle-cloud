package com.eagle.system.system.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDeptRequest {

    @Size(max = 100)
    private String name;

    private Long leaderId;

    @Size(max = 20)
    private String phone;

    private Integer sortOrder;
}
