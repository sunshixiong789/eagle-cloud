package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.Post;
import com.eagle.system.base.web.dto.response.PostResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {

    PostResponse toResponse(Post post);
}
