package com.eagle.system.upms.application.mapper;

import com.eagle.system.upms.domain.model.Dict;
import com.eagle.system.upms.web.dto.response.DictResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DictMapper {

    DictResponse toResponse(Dict dict);
}
