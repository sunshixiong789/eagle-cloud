package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.UserMapper;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.enums.LogStatus;
import com.eagle.system.base.domain.model.enums.LogType;
import com.eagle.system.base.domain.model.enums.RoleStatus;
import com.eagle.system.base.domain.model.enums.UserErrorCode;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.domain.repository.UserSpecification;
import com.eagle.system.base.domain.repository.UserSummary;
import com.eagle.system.base.domain.service.RoleValidationService;
import com.eagle.system.base.interfaces.dto.request.UpdateUserRequest;
import com.eagle.system.base.interfaces.dto.request.UserQueryRequest;
import com.eagle.system.base.interfaces.dto.response.AssignedRoleResponse;
import com.eagle.system.base.infrastructure.remote.AuthClientFacade;
import com.eagle.system.base.infrastructure.remote.dto.AccountBlacklistSnapshot;
import com.eagle.system.base.interfaces.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleValidationService roleValidationService;
    private final RoleRepository roleRepository;
    private final LogRepository logRepository;
    private final AuthClientFacade authClientFacade;

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
        return userRepository.findAll(pageable).map(this::toListResponse);
    }

    /**
     * 条件分页查询用户列表
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> queryUsers(UserQueryRequest request, Pageable pageable) {
        Specification<User> spec = Specification
                .where(UserSpecification.usernameLike(request.getUsername()))
                .and(UserSpecification.emailLike(request.getEmail()));
        return userRepository.findAll(spec, pageable).map(this::toListResponse);
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
        return getAssignedRoles(user);
    }

    private UserResponse toListResponse(User user) {
        UserResponse response = userMapper.toResponse(user);
        response.setRoles(getAssignedRoles(user));
        response.setLastLoginAt(logRepository
                .findLatestCreateTimeByUsernameAndLogTypeAndStatus(
                        user.getUsername(), LogType.LOGIN, LogStatus.SUCCESS)
                .orElse(null));

        boolean online = isOnline(user.getAccountId());
        response.setOnline(online);
        response.setLoginStatus(online ? "ONLINE" : "OFFLINE");
        enrichBlacklistStatus(user, response);
        return response;
    }

    /**
     * 查询账号在线状态。降级 / 熔断 / 异常处理全部下沉到 {@link AuthClientFacade},
     * 本方法只关心业务语义。
     */
    private boolean isOnline(Long accountId) {
        if (accountId == null) {
            return false;
        }
        return !authClientFacade.listJtisByAccount(accountId).isEmpty();
    }

    private void enrichBlacklistStatus(User user, UserResponse response) {
        response.setBlacklisted(false);
        response.setBlacklistId(null);
        if (user.getAccountId() == null) {
            return;
        }
        ResponseEntity<AccountBlacklistSnapshot> resp =
                authClientFacade.findBlacklistByAccountId(user.getAccountId());
        AccountBlacklistSnapshot info = resp.getBody();
        if (resp.getStatusCode().is2xxSuccessful() && info != null) {
            response.setBlacklisted(true);
            response.setBlacklistId(info.id());
        }
    }

    private List<AssignedRoleResponse> getAssignedRoles(User user) {
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
