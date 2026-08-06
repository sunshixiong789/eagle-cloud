package com.eagle.system.base.interfaces.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典项响应
 * <p>
 * {@code children} 为树形结构的子节点：构建时必须先递归生成子节点，再构造父节点
 * （record 不可变，无法先建空节点再回填）。
 */
public record DictItemResponse(
        Long id,
        Long dictId,
        String itemValue,
        String name,
        String dictType,
        Long parentId,
        String description,
        Integer sortOrder,
        String status,
        String remarks,
        LocalDateTime createTime,
        List<DictItemResponse> children
) {
}
