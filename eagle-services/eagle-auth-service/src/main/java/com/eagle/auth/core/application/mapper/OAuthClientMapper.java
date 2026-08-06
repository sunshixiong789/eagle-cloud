package com.eagle.auth.core.application.mapper;

import com.eagle.auth.core.domain.model.OAuthClient;
import com.eagle.auth.core.interfaces.dto.response.OAuthClientResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2 客户端对象映射器（纯 Java 实现）。
 *
 * @author sunshixiong
 */
@Component
public class OAuthClientMapper {

    public OAuthClientResponse toResponse(OAuthClient entity) {
        if (entity == null) {
            return null;
        }
        return new OAuthClientResponse(
                entity.getId(),
                entity.getClientId(),
                entity.getClientName(),
                csvToSet(entity.getClientAuthenticationMethods()),
                csvToSet(entity.getAuthorizationGrantTypes()),
                csvToSet(entity.getRedirectUris()),
                csvToSet(entity.getScopes()),
                entity.getRequireProofKey(),
                entity.getRequireAuthorizationConsent(),
                entity.getAccessTokenTtlSeconds(),
                entity.getRefreshTokenTtlSeconds(),
                entity.getClientIdIssuedAt(),
                entity.getEnabled(),
                entity.getCreateTime(),
                entity.getUpdateTime());
    }

    private Set<String> csvToSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
