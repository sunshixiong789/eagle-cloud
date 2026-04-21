package com.eagle.system.system.application.mapper;

import com.eagle.system.domain.model.Dict;
import com.eagle.system.web.dto.response.DictResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DictMapper {

    DictResponse toResponse(Dict dict);
}
