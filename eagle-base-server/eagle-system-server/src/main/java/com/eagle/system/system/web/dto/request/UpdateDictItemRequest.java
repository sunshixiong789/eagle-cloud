package com.eagle.system.system.web.dto.request;

import lombok.Data;

@Data
public class UpdateDictItemRequest {
    private String itemValue;
    private String name;
    private String description;
    private Integer sortOrder;
    private String remarks;
}
