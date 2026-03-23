package com.eagle.system.base.web.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeptResponse {

    private Long id;
    private Long parentId;
    private String name;
    private String deptPath;
    private Integer level;
    private Long leaderId;
    private String phone;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createTime;
}
