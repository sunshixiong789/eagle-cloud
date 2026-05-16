package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.interfaces.dto.request.CreateUserRequest;
import com.eagle.system.base.interfaces.dto.request.RegisterRequest;
import com.eagle.system.base.interfaces.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * User 映射器
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "nickname", source = "profile.nickname")
    @Mapping(target = "avatar", source = "profile.avatar")
    UserResponse toResponse(User user);

    default UserProfile toProfile(RegisterRequest request) {
        return new UserProfile(request.getAvatar(), request.getNickname(), null, null, null);
    }

    default UserProfile toProfile(CreateUserRequest request) {
        return new UserProfile(request.getAvatar(), request.getNickname(), null, null, null);
    }
}
