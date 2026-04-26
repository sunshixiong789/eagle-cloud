package com.eagle.resource.server.util;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 安全工具类，用于获取当前登录用户信息
 *
 * @author 孙士雄
 */
public class SecurityUtils {

    private SecurityUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取当前认证对象
     *
     * @return Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录用户信息
     *
     * @return EagleUser
     */
    public static EagleUser getCurrentUser() {
        Authentication authentication = getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return buildEagleUserFromJwt(jwt);
        }
        return null;
    }

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID
     */
    public static Long getCurrentUserId() {
        EagleUser user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    public static String getCurrentUsername() {
        EagleUser user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前用户部门 ID
     *
     * @return 部门 ID
     */
    public static Long getCurrentDeptId() {
        EagleUser user = getCurrentUser();
        return user != null ? user.getDeptId() : null;
    }

    /**
     * 判断当前用户是否拥有指定角色
     *
     * @param role 角色名称（不需要 ROLE_ 前缀）
     * @return 是否拥有角色
     */
    public static boolean hasRole(String role) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }
        String roleWithPrefix = SecurityConstants.ROLE_START + role;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(roleWithPrefix));
    }

    /**
     * 判断当前用户是否拥有任意一个指定角色
     *
     * @param roles 角色名称数组
     * @return 是否拥有任意角色
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 JWT 构建 EagleUser 对象
     *
     * @param jwt JWT Token
     * @return EagleUser
     */
    private static EagleUser buildEagleUserFromJwt(Jwt jwt) {
        Long userId = jwt.getClaim(SecurityConstants.DETAILS_USER_ID);
        String username = jwt.getClaim(SecurityConstants.DETAILS_USERNAME);
        String name = jwt.getClaim(SecurityConstants.DETAILS_USER_NAME);
        Long deptId = jwt.getClaim(SecurityConstants.DETAILS_DEP_ID);
        String deptName = jwt.getClaim(SecurityConstants.DETAILS_DEP_NAME);
        String phone = jwt.getClaim(SecurityConstants.DETAILS_PHONE);

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        return new EagleUser(
                userId,
                username,
                "[PROTECTED]",
                name,
                deptId,
                deptName,
                phone,
                authorities
        );
    }

    /**
     * 从 JWT 中提取权限信息
     *
     * @param jwt JWT Token
     * @return 权限集合
     */
    private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaim(SecurityConstants.DETAILS_ROLES);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_START + role))
                .collect(Collectors.toList());
    }
}
