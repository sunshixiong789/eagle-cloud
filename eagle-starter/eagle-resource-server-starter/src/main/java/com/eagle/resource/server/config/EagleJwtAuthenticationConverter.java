package com.eagle.resource.server.config;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证转换器，将 JWT Token 转换为 Spring Security 的 Authentication 对象
 * 从 JWT Claims 中提取用户信息和权限，构建 EagleUser 对象
 *
 * @author 孙士雄
 */
public class EagleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        // 1. 提取权限
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        // 2. 从 JWT Claims 中提取用户信息
        Long userId = jwt.getClaim(SecurityConstants.DETAILS_USER_ID);
        String username = jwt.getClaim(SecurityConstants.DETAILS_USERNAME);
        String name = jwt.getClaim(SecurityConstants.DETAILS_USER_NAME);
        Long deptId = jwt.getClaim(SecurityConstants.DETAILS_DEP_ID);
        String deptName = jwt.getClaim(SecurityConstants.DETAILS_DEP_NAME);
        String phone = jwt.getClaim(SecurityConstants.DETAILS_PHONE);

        // 3. 构建 EagleUser 对象（资源服务器不需要密码）
        EagleUser user = new EagleUser(
                userId,
                username,
                "",
                name,
                deptId,
                deptName,
                phone,
                authorities
        );

        // 4. 返回 JwtAuthenticationToken，Principal 为 EagleUser
        return new JwtAuthenticationToken(jwt, authorities, username);
    }

    /**
     * 从 JWT 中提取权限信息
     *
     * @param jwt JWT Token
     * @return 权限集合
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaim(SecurityConstants.DETAILS_ROLES);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_START + role))
                .collect(Collectors.toList());
    }
}