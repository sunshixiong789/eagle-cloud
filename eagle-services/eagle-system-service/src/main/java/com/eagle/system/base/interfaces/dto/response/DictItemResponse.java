package com.eagle.system.base.interfaces.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DictItemResponse {
    private Long id;
    private Long dictId;
    private String itemValue;
    private String name;
    private String dictType;
    private Long parentId;
    private String description;
    private Integer sortOrder;
    private String status;
    private String remarks;
    private LocalDateTime createTime;
    private List<DictItemResponse> children;
}
