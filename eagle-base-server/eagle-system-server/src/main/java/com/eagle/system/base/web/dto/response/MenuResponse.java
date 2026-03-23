package com.eagle.system.base.web.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MenuResponse {

    private Long id;
    private String name;
    private String enName;
    private String permission;
    private Long parentId;
    private String menuPath;
    private Integer level;
    private String icon;
    private String path;
    private String component;
    private Boolean visible;
    private Integer sortOrder;
    private String menuType;
    private Boolean keepAlive;
    private Boolean embedded;
    private Boolean isFrame;
    private String status;
    private LocalDateTime createTime;
    private List<MenuResponse> children;
}
