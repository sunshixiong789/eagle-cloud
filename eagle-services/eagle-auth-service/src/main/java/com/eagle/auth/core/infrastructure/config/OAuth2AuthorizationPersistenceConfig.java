package com.eagle.auth.core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * OAuth2 authorization and consent persistence.
 */
@Configuration
public class OAuth2AuthorizationPersistenceConfig {

    /**
     * OAuth2 授权持久化 — JDBC 实现。
     *
     * <p>替代 {@code InMemoryOAuth2AuthorizationService}，授权码、访问令牌、
     * 刷新令牌等数据持久化到数据库，服务重启不丢失活跃授权，多实例部署共享授权状态。
     *
     * <p>SAS 7.0.5 默认使用 Jackson 3 的 {@code JsonMapperOAuth2AuthorizationRowMapper}
     * 与 {@code JsonMapperOAuth2AuthorizationParametersMapper}。默认 PTV(
     * {@code BasicPolymorphicTypeValidator}) 只放行 Spring Security 自带的类型与
     * {@code java.time.*}，<strong>不</strong>包含 {@code java.lang.Long} 等基本包装类型。
     *
     * <p>JWT customizer 把 {@code user.getId()} (Long) 写入 access token claims,
     * SAS 在 {@code oauth2_authorization.access_token_metadata.token.claims} 里
     * 用 default typing 持久化时会写入 {@code @class: java.lang.Long},回读时 PTV 拒绝,
     * 触发 "Could not resolve type id java.lang.Long as a subtype of `java.lang.Object`"。
     *
     * <p>修复:基于 SAS 默认 PTV 追加包装类型白名单后构建自定义 {@link JsonMapper},
     * 同时覆盖 RowMapper 和 ParametersMapper(两侧都用同一份 mapper,序列化/反序列化对称)。
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {
        JsonMapper jsonMapper = buildAuthorizationJsonMapper();
        JdbcOAuth2AuthorizationService service =
                new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
        service.setAuthorizationRowMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(
                        registeredClientRepository, jsonMapper));
        service.setAuthorizationParametersMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper(jsonMapper));
        return service;
    }

    /**
     * 构建 OAuth2Authorization 持久化用 JsonMapper:在 SAS 默认安全模块基础上,
     * 把 JWT claims 中可能出现的基本包装类型加入 PTV 白名单。
     */
    private static JsonMapper buildAuthorizationJsonMapper() {
        // SAS 默认 PTV(SecurityJacksonModules 内置)不放行的实际持久化类型,按 access_token_metadata
        // 实测 JSON 补齐。注意 `iss` claim 被 JwtClaimsSet.Builder.issuer(String) 转成 java.net.URL
        // 存进 claims;`aud`/`scope` 是 Collections 工厂方法返回的内部类(SingletonList/UnmodifiableSet)。
        // 这些都用 default typing 写成 ["java.xxx.YYY", value],回读时 PTV 拒绝即抛
        // "Could not resolve type id java.xxx.YYY as a subtype",/userinfo 表现为 invalid_token。
        BasicPolymorphicTypeValidator.Builder ptvBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Long.class)
                .allowIfSubType(java.net.URL.class)
                // 覆盖 Collections$SingletonList / $UnmodifiableSet / $UnmodifiableMap / $EmptyList ...
                .allowIfSubType("java.util.Collections$");
        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(
                        OAuth2AuthorizationPersistenceConfig.class.getClassLoader(), ptvBuilder))
                .build();
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
    }
}
