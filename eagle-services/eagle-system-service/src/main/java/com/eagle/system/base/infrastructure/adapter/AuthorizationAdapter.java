package com.eagle.system.base.infrastructure.adapter;

import com.eagle.system.auth.domain.port.AuthorizationInfo;
import com.eagle.system.auth.domain.port.AuthorizationPort;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 授权信息查询适配器(Driven Adapter)
 * <p>
 * 六边形架构端口适配器模式实现:
 * <ul>
 *   <li>实现 auth 域定义的 {@link AuthorizationPort} 接口</li>
 *   <li>为 auth 域提供用户的姓名与角色码(用于 JWT Token 构建)</li>
 *   <li>当前是单体架构,直接查询 system 域数据库</li>
 *   <li>拆分为微服务后,可在 auth 的基础设施层提供远程实现(HTTP/gRPC)</li>
 * </ul>
 * <p>
 * 依赖方向: system → auth::port (符合 DDD 模块依赖规则)
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationAdapter implements AuthorizationPort {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * 根据 accountId 查询授权信息
     * <p>
     * 供 auth 域在用户登录时调用,获取用户的姓名和角色码,
     * 用于构建 JWT Token 中的权限声明(claims)。
     *
     * @param accountId 认证账号 ID
     * @return 授权信息(包含姓名、角色码)
     */
    @Override
    public Optional<AuthorizationInfo> findAuthorizationInfo(Long accountId) {
        return userRepository.findByAccountId(accountId)
                .map(this::toAuthorizationInfo);
    }

    /**
     * 将 User 聚合根转换为 AuthorizationInfo
     * <p>
     * 组装用户的完整授权信息:
     * <ul>
     *   <li>姓名:从 UserProfile 值对象中提取</li>
     *   <li>角色:返回 roleCode 业务标识（不带 ROLE_ 前缀,前缀由 Spring Security 适配层添加）</li>
     * </ul>
     *
     * @param user 用户聚合根
     * @return 授权信息 DTO
     */
    private AuthorizationInfo toAuthorizationInfo(User user) {
        // 提取用户姓名
        String name = user.getProfile() != null ? user.getProfile().getName() : null;

        // 批量查询角色码（业务标识,不带前缀; ROLE_ 前缀由 EagleJwtAuthenticationConverter
        // 和 EagleUserDetailsServiceImpl 在转换为 GrantedAuthority 时统一添加）
        Set<String> roleCodes = Set.of();
        if (!user.getRoleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(user.getRoleIds());
            roleCodes = roles.stream()
                    .map(Role::getRoleCode)
                    .collect(Collectors.toSet());
        }

        return new AuthorizationInfo(name, roleCodes);
    }
}
