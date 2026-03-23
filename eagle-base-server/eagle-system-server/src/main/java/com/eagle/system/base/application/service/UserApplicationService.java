package com.eagle.system.base.application.service;

import com.eagle.common.exception.ResourceConflictException;
import com.eagle.common.exception.ResourceNotFoundException;
import com.eagle.system.base.application.mapper.UserMapper;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.domain.repository.UserSpecification;
import com.eagle.system.base.web.dto.request.CreateUserRequest;
import com.eagle.system.base.web.dto.request.RegisterRequest;
import com.eagle.system.base.web.dto.request.UpdateUserRequest;
import com.eagle.system.base.web.dto.request.UserQueryRequest;
import com.eagle.system.base.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 用户应用服务
 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResourceConflictException("用户名已存在");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        UserProfile profile = userMapper.toProfile(request);

        User user = User.create(request.getUsername(), encodedPassword,
                request.getPhone(), request.getEmail(), profile);

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResourceConflictException("用户名已存在");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        UserProfile profile = userMapper.toProfile(request);

        User user = User.create(request.getUsername(), encodedPassword,
                request.getPhone(), request.getEmail(), profile);

        if (request.getDepartmentId() != null) {
            user.assignDept(request.getDepartmentId());
        }
        if (request.getRoleIds() != null && request.getRoleIds().length > 0) {
            user.assignRoles(Set.of(request.getRoleIds()));
        }

        User saved = userRepository.save(user);

        return userMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserById(id);

        if (request.getNickname() != null || request.getAvatar() != null || request.getName() != null) {
            UserProfile newProfile = user.getProfile() != null
                    ? user.getProfile().update(request.getName(), request.getNickname(), request.getAvatar())
                    : new UserProfile(request.getAvatar(), request.getNickname(), request.getName(), null, null);
            user.updateProfile(newProfile);
        }

        if (request.getPhone() != null || request.getEmail() != null) {
            user.updateContact(request.getPhone(), request.getEmail());
        }

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, String newPassword) {
        User user = findUserById(id);

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedPassword);
        userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void lockUser(Long id, String reason) {
        User user = findUserById(id);
        user.lock(reason);
        userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(Long id) {
        User user = findUserById(id);
        user.unlock();
        userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findUserById(id);
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> queryUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> queryUsers(UserQueryRequest request, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecification.usernameLike(request.getUsername()))
                .and(UserSpecification.deptIdEquals(request.getDepartmentId()))
                .and(UserSpecification.lockFlagEquals(request.getLocked()))
                .and(UserSpecification.phoneLike(request.getPhone()))
                .and(UserSpecification.emailLike(request.getEmail()));
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
}
