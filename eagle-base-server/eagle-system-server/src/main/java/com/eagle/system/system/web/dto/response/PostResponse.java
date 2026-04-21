package com.eagle.system.system.web.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResponse {

    private Long id;
    private String postCode;
    private String postName;
    private Integer postSort;
    private String remark;
    private LocalDateTime createTime;
}
