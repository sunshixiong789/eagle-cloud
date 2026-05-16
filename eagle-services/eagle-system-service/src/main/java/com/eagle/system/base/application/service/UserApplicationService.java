package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.UserMapper;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.enums.RoleStatus;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.domain.repository.UserSpecification;
import com.eagle.system.base.domain.repository.UserSummary;
import com.eagle.system.base.domain.service.RoleValidationService;
import com.eagle.system.base.interfaces.dto.request.UpdateUserRequest;
import com.eagle.system.base.interfaces.dto.request.UserQueryRequest;
import com.eagle.system.base.interfaces.dto.response.AssignedRoleResponse;
import com.eagle.system.base.interfaces.dto.response.UserResponse;
import com.eagle.system.base.domain.model.enums.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 用户应用服务
 * <p>
 * 职责：
 * <ul>
 *   <li>管理 system 域 User 聚合根的组织档案信息</li>
 *   <li>User 的创建和删除由 auth 域通过事件驱动（AccountRegisteredEvent / AccountDeletedEvent）</li>
 *   <li>认证凭据操作（密码、锁定）由 auth 域的 AccountController 直接处理</li>
 * </ul>
 * <p>
 * 部门/岗位管理已下线，但 User.deptId 仍作为外部 ID 引用保留。
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleValidationService roleValidationService;
    private final RoleRepository roleRepository;

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
                .and(UserSpecification.deptIdEquals(request.getDepartmentId()))
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

    /**
     * 分配角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long id, Set<Long> roleIds) {
        User user = findUserById(id);
        roleValidationService.validateRoles(roleIds);
        user.assignRoles(roleIds);
        userRepository.save(user);
    }

    /**
     * 获取用户已分配角色列表
     *
     * @param userId 用户 ID
     * @return 已分配角色列表
     */
    @Transactional(readOnly = true)
    public List<AssignedRoleResponse> getUserRoles(Long userId) {
        User user = findUserById(userId);
        Set<Long> roleIds = user.getRoleIds();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.findAllById(roleIds).stream()
                .map(role -> AssignedRoleResponse.builder()
                        .id(role.getId())
                        .roleName(role.getRoleName())
                        .roleCode(role.getRoleCode())
                        // API 契约：RoleStatus.NORMAL → "ENABLE", DISABLED → "DISABLE"
                        .status(role.getStatus() == RoleStatus.NORMAL ? "ENABLE" :
                                role.getStatus() == RoleStatus.DISABLED ? "DISABLE" : null)
                        .build())
                .toList();
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::toNotFoundException);
    }
}
