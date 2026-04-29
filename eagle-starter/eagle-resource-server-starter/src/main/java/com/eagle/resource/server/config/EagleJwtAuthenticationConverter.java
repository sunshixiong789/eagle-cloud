package com.eagle.resource.server.config;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证转换器：将 JWT Token 转换为以 {@link EagleUser} 为 Principal 的 {@link EagleAuthentication}。
 *
 * <p>从 JWT Claims 中提取用户信息，构建 {@link EagleUser} 后存入 {@link EagleAuthentication}，
 * 使得 SpEL 权限表达式可直接访问用户字段：
 * <pre>{@code
 * @PreAuthorize("#userId == authentication.principal.id")
 * @PreAuthorize("authentication.principal.deptId == #deptId")
 * }</pre>
 *
 * @author 孙士雄
 */
public class EagleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        Long userId = jwt.getClaim(SecurityConstants.DETAILS_USER_ID);
        String username = jwt.getClaim(SecurityConstants.DETAILS_USERNAME);
        String name = jwt.getClaim(SecurityConstants.DETAILS_USER_NAME);
        Long deptId = jwt.getClaim(SecurityConstants.DETAILS_DEPT_ID);
        String deptName = jwt.getClaim(SecurityConstants.DETAILS_DEPT_NAME);
        String phone = jwt.getClaim(SecurityConstants.DETAILS_PHONE);

        EagleUser user = new EagleUser(userId, username, "", name, deptId, deptName, phone, authorities);
        return new EagleAuthentication(jwt, user, authorities);
    }

    /**
     * 从 JWT Claims 中提取角色列表，添加 {@code ROLE_} 前缀后转为 {@link GrantedAuthority}。
     */
    static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaim(SecurityConstants.DETAILS_ROLES);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_START + role))
                .toList();
    }
}
