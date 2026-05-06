package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.UserMapper;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.domain.repository.UserSpecification;
import com.eagle.system.base.domain.repository.UserSummary;
import com.eagle.system.base.web.dto.request.UpdateUserRequest;
import com.eagle.system.base.web.dto.request.UserQueryRequest;
import com.eagle.system.base.web.dto.response.UserResponse;
import com.eagle.system.base.domain.model.enums.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用服务
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * 更新用户档案信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserById(id);

        if (request.getNickname() != null || request.getAvatar() != null
                || request.getName() != null) {
            UserProfile newProfile = user.getProfile() != null
                    ? user.getProfile().update(
                    request.getName(), request.getNickname(), request.getAvatar())
                    : new UserProfile(
                    request.getAvatar(), request.getNickname(),
                    request.getName(), null, null);
            user.updateProfile(newProfile);
        }

        if (request.getEmail() != null) {
            user.updateContact(request.getEmail());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * 根据 ID 查询用户详情
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findUserById(id));
    }

    /**
     * 分页查询用户列表
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> queryUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    /**
     * 条件分页查询用户列表
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> queryUsers(UserQueryRequest request, Pageable pageable) {
        Specification<User> spec = Specification
                .where(UserSpecification.usernameLike(request.getUsername()))
                .and(UserSpecification.emailLike(request.getEmail()));
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    /**
     * 分页查询用户摘要（CQRS 投影）
     */
    @Transactional(readOnly = true)
    public Page<UserSummary> queryUserSummaries(Pageable pageable) {
        return userRepository.findUserSummaries(pageable);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::toNotFoundException);
    }
}
