package com.eagle.auth.application.mapper;

import com.eagle.auth.domain.model.OAuthClient;
import com.eagle.auth.interfaces.dto.response.OAuthClientResponse;
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
        OAuthClientResponse response = new OAuthClientResponse();
        response.setId(entity.getId());
        response.setClientId(entity.getClientId());
        response.setClientName(entity.getClientName());
        response.setClientAuthenticationMethods(csvToSet(entity.getClientAuthenticationMethods()));
        response.setAuthorizationGrantTypes(csvToSet(entity.getAuthorizationGrantTypes()));
        response.setRedirectUris(csvToSet(entity.getRedirectUris()));
        response.setScopes(csvToSet(entity.getScopes()));
        response.setRequireProofKey(entity.getRequireProofKey());
        response.setRequireAuthorizationConsent(entity.getRequireAuthorizationConsent());
        response.setAccessTokenTtlSeconds(entity.getAccessTokenTtlSeconds());
        response.setRefreshTokenTtlSeconds(entity.getRefreshTokenTtlSeconds());
        response.setClientIdIssuedAt(entity.getClientIdIssuedAt());
        response.setEnabled(entity.getEnabled());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
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
