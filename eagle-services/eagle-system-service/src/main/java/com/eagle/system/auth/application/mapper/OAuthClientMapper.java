package com.eagle.system.auth.application.mapper;

import com.eagle.system.auth.domain.model.OAuthClient;
import com.eagle.system.auth.interfaces.dto.response.OAuthClientResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2 客户端对象映射器
 *
 * @author sunshixiong
 */
@Mapper(componentModel = "spring")
public interface OAuthClientMapper {

    @Mapping(source = "clientAuthenticationMethods", target = "clientAuthenticationMethods", qualifiedByName = "csvToSet")
    @Mapping(source = "authorizationGrantTypes", target = "authorizationGrantTypes", qualifiedByName = "csvToSet")
    @Mapping(source = "redirectUris", target = "redirectUris", qualifiedByName = "csvToSet")
    @Mapping(source = "scopes", target = "scopes", qualifiedByName = "csvToSet")
    OAuthClientResponse toResponse(OAuthClient entity);

    /**
     * 逗号分隔字符串转 Set
     */
    @Named("csvToSet")
    default Set<String> csvToSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
