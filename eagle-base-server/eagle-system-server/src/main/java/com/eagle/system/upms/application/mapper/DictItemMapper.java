package com.eagle.system.upms.application.mapper;

import com.eagle.system.upms.domain.model.entity.DictItemEntity;
import com.eagle.system.upms.web.dto.response.DictItemResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DictItemMapper {
    DictItemResponse toResponse(DictItemEntity entity);
    List<DictItemResponse> toResponseList(List<DictItemEntity> entities);
}
