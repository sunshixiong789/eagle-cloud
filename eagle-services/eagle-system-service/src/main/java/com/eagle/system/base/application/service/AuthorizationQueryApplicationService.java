package com.eagle.system.base.application.service;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.interfaces.dto.response.AuthorizationView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 授权信息查询服务:为 auth-service 远程调用提供用户姓名与角色码。
 * <p>
 * 由 {@code AuthorizationInternalController} 暴露为 HTTP 端点
 * {@code GET /internal/authorization/{accountId}},auth-service 在 OAuth2 登录流程中
 * 通过 RestClient 调用,把姓名 + 角色码注入到 JWT claims。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationQueryApplicationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public Optional<AuthorizationView> findByAccountId(Long accountId) {
        return userRepository.findByAccountId(accountId).map(this::toView);
    }

    private AuthorizationView toView(User user) {
        String name = user.getProfile() != null ? user.getProfile().getName() : null;
        String avatar = user.getProfile() != null ? user.getProfile().getAvatar() : null;
        Set<String> roleCodes = Set.of();
        if (!user.getRoleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(user.getRoleIds());
            roleCodes = roles.stream()
                    .map(Role::getRoleCode)
                    .collect(Collectors.toSet());
        }
        return new AuthorizationView(name, avatar, roleCodes);
    }
}
