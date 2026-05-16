package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.entity.DictItemEntity;
import com.eagle.system.base.interfaces.dto.response.DictItemResponse;
import org.springframework.stereotype.Component;

/**
 * 字典项映射器（纯 Java 实现）。
 */
@Component
public class DictItemMapper {

    public DictItemResponse toResponse(DictItemEntity entity) {
        if (entity == null) {
            return null;
        }
        DictItemResponse response = new DictItemResponse();
        response.setId(entity.getId());
        response.setDictId(entity.getDictId());
        response.setItemValue(entity.getItemValue());
        response.setName(entity.getName());
        response.setDictType(entity.getDictType() != null ? entity.getDictType().name() : null);
        response.setParentId(entity.getParentId());
        response.setDescription(entity.getDescription());
        response.setSortOrder(entity.getSortOrder());
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        response.setRemarks(entity.getRemarks());
        response.setCreateTime(entity.getCreateTime());
        return response;
    }
}
