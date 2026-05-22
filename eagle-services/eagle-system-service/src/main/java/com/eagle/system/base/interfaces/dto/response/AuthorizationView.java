package com.eagle.system.base.interfaces.dto.response;

import java.util.Set;

/**
 * 授权信息视图(由 base 暴露给 auth-service /internal/authorization/{accountId} 使用)。
 * <p>
 * 仅用于跨服务契约,字段与 auth-service 端 {@code AuthorizationInfo} 对齐:
 * <ul>
 *   <li>{@code name} - 用户真实姓名,写入 JWT claim</li>
 *   <li>{@code roleCodes} - 角色业务标识集合(不带 ROLE_ 前缀,前缀由 auth-service
 *       的 Spring Security 适配层添加)</li>
 * </ul>
 */
public record AuthorizationView(String name, Set<String> roleCodes) {
}
