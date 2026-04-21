package com.eagle.system.system.web.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<DeptResponse> children;
}
