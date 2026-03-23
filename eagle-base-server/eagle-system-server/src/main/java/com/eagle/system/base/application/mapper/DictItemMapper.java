package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.entity.DictItemEntity;
import com.eagle.system.base.web.dto.response.DictItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * @author sunshixiong
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictItemMapper {
    DictItemResponse toResponse(DictItemEntity entity);

    List<DictItemResponse> toResponseList(List<DictItemEntity> entities);
}
