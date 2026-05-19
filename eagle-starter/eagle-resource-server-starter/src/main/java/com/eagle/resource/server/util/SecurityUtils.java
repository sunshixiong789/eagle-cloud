package com.eagle.resource.server.util;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import com.eagle.resource.server.config.EagleAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 安全工具类，用于获取当前登录用户信息。
 *
 * <p>优先从 {@link EagleAuthentication}（由 {@link com.eagle.resource.server.config.EagleJwtAuthenticationConverter} 设置）
 * 中直接获取 {@link EagleUser}，无需重新解析 JWT Claims。
 * 兼容性回退：若 Authentication 类型为 {@link JwtAuthenticationToken}，则从 JWT Claims 中提取。
 *
 * <p>所有方法在未认证时返回 {@code null}，调用方需自行判空。
 *
 * @author 孙士雄
 */
public class SecurityUtils {

    private SecurityUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取当前认证对象。
     *
     * @return {@link Authentication}，未认证时返回 {@code null}
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return {@link EagleUser}，未认证时返回 {@code null}
     */
    public static EagleUser getCurrentUser() {
        Authentication auth = getAuthentication();
        if (auth instanceof EagleAuthentication eagleAuth) {
            // 直接返回已解析的 EagleUser，无需重新解析 JWT
            return eagleAuth.getPrincipal();
        }
        // 兼容性回退：从 JWT 中提取
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return buildEagleUserFromJwt(jwtAuth.getToken());
        }
        return null;
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 用户 ID，未认证时返回 {@code null}
     */
    public static Long getCurrentUserId() {
        EagleUser user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前用户名（登录名）。
     *
     * @return 用户名，未认证时返回 {@code null}
     */
    public static String getCurrentUsername() {
        EagleUser user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 判断当前用户是否拥有指定角色。
     *
     * @param role 角色名称（不需要 {@code ROLE_} 前缀）
     * @return 是否拥有角色
     */
    public static boolean hasRole(String role) {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return false;
        }
        String roleWithPrefix = SecurityConstants.ROLE_START + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(roleWithPrefix::equals);
    }

    /**
     * 判断当前用户是否拥有任意一个指定角色。
     *
     * @param roles 角色名称（不需要 {@code ROLE_} 前缀）
     * @return 是否拥有任意角色
     */
    public static boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles).anyMatch(SecurityUtils::hasRole);
    }

    /**
     * 从 JWT 构建 {@link EagleUser}（兼容性回退方法，正常流程应走 {@link EagleAuthentication}）。
     */
    private static EagleUser buildEagleUserFromJwt(Jwt jwt) {
        Long userId = jwt.getClaim(SecurityConstants.DETAILS_USER_ID);
        String username = jwt.getClaim(SecurityConstants.DETAILS_USERNAME);
        String name = jwt.getClaim(SecurityConstants.DETAILS_USER_NAME);
        String phone = jwt.getClaim(SecurityConstants.DETAILS_PHONE);

        Collection<GrantedAuthority> authorities = extractAuthoritiesFromJwt(jwt);
        return new EagleUser(userId, username, "[PROTECTED]", name, phone, authorities);
    }

    private static Collection<GrantedAuthority> extractAuthoritiesFromJwt(Jwt jwt) {
        List<String> roles = jwt.getClaim(SecurityConstants.DETAILS_ROLES);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_START + role))
                .toList();
    }
}
