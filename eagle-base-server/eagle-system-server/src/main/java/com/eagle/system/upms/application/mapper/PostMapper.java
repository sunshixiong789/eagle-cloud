package com.eagle.system.upms.application.mapper;

import com.eagle.system.upms.domain.model.Post;
import com.eagle.system.upms.web.dto.response.PostResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {

    PostResponse toResponse(Post post);
}
