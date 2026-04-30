package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.entity.DictItemEntity;
import com.eagle.system.base.web.dto.response.DictItemResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DictItemMapper {
    DictItemResponse toResponse(DictItemEntity entity);

    List<DictItemResponse> toResponseList(List<DictItemEntity> entities);
}
