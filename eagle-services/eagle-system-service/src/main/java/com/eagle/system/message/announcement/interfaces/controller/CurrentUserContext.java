package com.eagle.system.message.announcement.interfaces.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 控制器层取当前用户上下文的小工具——仅本子模块内使用。
 *
 * @author sunshixiong
 */
final class CurrentUserContext {

    private static final String ROLE_PREFIX = "ROLE_";

    private CurrentUserContext() {}

    /** 当前用户的角色集合（去掉 {@code ROLE_} 前缀）；未登录返回空集。 */
    static Set<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 当前用户的标签集合。
     *
     * <p>项目尚未引入用户标签体系，本期返回空集；预留扩展点
     * （未来可接入用户画像服务，标签写入 JWT 或独立查询）。
     */
    static Set<String> currentTags() {
        return Set.of();
    }
}
