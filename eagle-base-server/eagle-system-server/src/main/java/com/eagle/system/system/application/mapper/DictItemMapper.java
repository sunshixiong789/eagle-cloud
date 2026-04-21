package com.eagle.system.system.application.mapper;

import com.eagle.system.domain.model.entity.DictItemEntity;
import com.eagle.system.web.dto.response.DictItemResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DictItemMapper {
    DictItemResponse toResponse(DictItemEntity entity);
    List<DictItemResponse> toResponseList(List<DictItemEntity> entities);
}
