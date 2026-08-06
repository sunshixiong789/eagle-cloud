package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.Dict;
import com.eagle.system.base.interfaces.dto.response.DictResponse;
import org.springframework.stereotype.Component;

/**
 * 字典映射器（纯 Java 实现）。
 */
@Component
public class DictMapper {

    public DictResponse toResponse(Dict dict) {
        if (dict == null) {
            return null;
        }
        return new DictResponse(
                dict.getId(),
                dict.getDictType() != null ? dict.getDictType().name() : null,
                dict.getDictName(),
                dict.getDescription(),
                dict.getSystemFlag(),
                dict.getStatus() != null ? dict.getStatus().name() : null,
                dict.getRemarks(),
                dict.getCreateTime(),
                null);
    }
}
