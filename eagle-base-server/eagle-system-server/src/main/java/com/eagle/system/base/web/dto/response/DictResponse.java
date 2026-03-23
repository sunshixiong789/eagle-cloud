package com.eagle.system.base.web.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictResponse {

    private Long id;
    private String dictType;
    private String dictName;
    private String description;
    private Boolean systemFlag;
    private String status;
    private String remarks;
    private LocalDateTime createTime;
}
