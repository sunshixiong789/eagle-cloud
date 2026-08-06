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
        // 手机号、在线态、黑名单、角色来自 auth-service 与日志表，Mapper 拿不到，
        // 由应用服务查完后用 UserResponse#withEnrichment / #withAccount 回填。
        return new UserResponse(
                user.getId(),
                user.getAccountId(),
                user.getUsername(),
                null,
                user.getEmail(),
                profile != null ? profile.getName() : null,
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getAvatar() : null,
                user.getCreateTime(),
                null,
                false,
                null,
                false,
                null,
                null);
    }
}
