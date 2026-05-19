package com.eagle.system.auth.infrastructure.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 OAuth2 access_token 的 claims map 净化为 SAS Jackson 反序列化白名单兼容的形态，
 * 专用于 {@link org.springframework.security.oauth2.server.authorization.OAuth2Authorization
 * OAuth2Authorization} 的 token metadata 持久化。
 * <p>
 * 背景：{@code JdbcOAuth2AuthorizationService} 通过 Jackson 把 metadata 持久化到
 * {@code oauth2_authorization} 表，{@code /userinfo} 等端点回读时由
 * {@code BasicPolymorphicTypeValidator} 校验类型 id。当 {@code Map<String, Object>} 的
 * value 是 {@link Number}（如 id 等）时，Jackson 会写入 {@code @class: java.lang.Long}，
 * 反序列化时被 PTV 拒绝（白名单不含 java.lang 包装类型）。
 * <p>
 * 处理策略：把 {@link Number} 全部转 {@link String}，{@link Iterable} 递归处理元素。
 * 这只影响存进 metadata 的副本和 {@code /userinfo} 响应中对应字段的展现类型，
 * <strong>不影响 JWT 自身 claims 的类型</strong>——下游资源服务器从 JWT 解析仍然拿到 Long。
 */
final class ClaimsMetadataSanitizer {

    private ClaimsMetadataSanitizer() {
    }

    static Map<String, Object> sanitize(Map<String, Object> claims) {
        Map<String, Object> safe = new LinkedHashMap<>(claims.size());
        claims.forEach((k, v) -> safe.put(k, normalize(v)));
        return safe;
    }

    private static Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Iterable<?> iter) {
            List<Object> list = new ArrayList<>();
            iter.forEach(e -> list.add(normalize(e)));
            return list;
        }
        return value;
    }
}
