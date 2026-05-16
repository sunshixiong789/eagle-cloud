package com.eagle.system.base.interfaces.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典响应
 *
 * @author sunshixiong
 */
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

    /**
     * 字典项列表（仅按类型查询时返回）
     */
    private List<DictItemResponse> items;
}
