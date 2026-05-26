package com.eagle.auth.core.domain.port;

import java.util.Set;

/**
 * 授权信息 DTO
 * <p>
 * auth 域通过 {@link AuthorizationPort} 获取此对象，
 * 仅包含构建 JWT claims 所需的展示信息和角色码。
 *
 * @author sunshixiong
 */
public record AuthorizationInfo(
        // 真实姓名（JWT claim）
        String name,

        // 头像 URL（JWT claim，供 /userinfo 渲染）
        String avatar,

        // 角色码集合（如 "ROLE_admin"），用于 GrantedAuthority
        Set<String> roleCodes
) {

    /**
     * 空授权信息（新注册用户尚未分配角色时使用）
     */
    public static AuthorizationInfo empty() {
        return new AuthorizationInfo(null, null, Set.of());
    }
}
