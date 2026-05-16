package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.Dict;
import com.eagle.system.base.interfaces.dto.response.DictResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DictMapper {

    DictResponse toResponse(Dict dict);
}
