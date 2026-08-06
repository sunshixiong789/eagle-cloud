package com.eagle.system.base.interfaces.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典响应
 *
 * @param items 字典项列表（仅按类型查询时返回）
 * @author sunshixiong
 */
public record DictResponse(
        Long id,
        String dictType,
        String dictName,
        String description,
        Boolean systemFlag,
        String status,
        String remarks,
        LocalDateTime createTime,
        List<DictItemResponse> items
) {

    /** 返回一个仅替换 items 的副本 —— 用于「先查字典、再挂载字典项」的两步组装。 */
    public DictResponse withItems(List<DictItemResponse> newItems) {
        return new DictResponse(id, dictType, dictName, description, systemFlag,
                status, remarks, createTime, newItems);
    }
}
