package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.interfaces.dto.response.UserResponse;
import org.springframework.stereotype.Component;

/**
 * User 映射器（纯 Java 实现）。
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        UserProfile profile = user.getProfile();
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(profile != null ? profile.getName() : null)
                .nickname(profile != null ? profile.getNickname() : null)
                .avatar(profile != null ? profile.getAvatar() : null)
                .build();
    }
}
