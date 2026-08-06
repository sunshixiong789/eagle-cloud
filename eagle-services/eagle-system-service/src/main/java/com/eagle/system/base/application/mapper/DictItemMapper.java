package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.entity.DictItemEntity;
import com.eagle.system.base.interfaces.dto.response.DictItemResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典项映射器（纯 Java 实现）。
 */
@Component
public class DictItemMapper {

    public DictItemResponse toResponse(DictItemEntity entity) {
        return toResponse(entity, null);
    }

    /**
     * 带子节点的映射：树形结构自底向上构建，调用方先递归生成 children 再传入。
     */
    public DictItemResponse toResponse(DictItemEntity entity, List<DictItemResponse> children) {
        if (entity == null) {
            return null;
        }
        return new DictItemResponse(
                entity.getId(),
                entity.getDictId(),
                entity.getItemValue(),
                entity.getName(),
                entity.getDictType() != null ? entity.getDictType().name() : null,
                entity.getParentId(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getRemarks(),
                entity.getCreateTime(),
                children);
    }
}
