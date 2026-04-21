package com.eagle.system.system.application.mapper;

import com.eagle.system.domain.model.Post;
import com.eagle.system.web.dto.response.PostResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {

    PostResponse toResponse(Post post);
}
