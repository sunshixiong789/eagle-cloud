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
        DictResponse response = new DictResponse();
        response.setId(dict.getId());
        response.setDictType(dict.getDictType() != null ? dict.getDictType().name() : null);
        response.setDictName(dict.getDictName());
        response.setDescription(dict.getDescription());
        response.setSystemFlag(dict.getSystemFlag());
        response.setStatus(dict.getStatus() != null ? dict.getStatus().name() : null);
        response.setRemarks(dict.getRemarks());
        response.setCreateTime(dict.getCreateTime());
        return response;
    }
}
