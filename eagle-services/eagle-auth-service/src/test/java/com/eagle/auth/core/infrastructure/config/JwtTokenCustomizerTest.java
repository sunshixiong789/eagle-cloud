package com.eagle.auth.core.infrastructure.config;

import com.eagle.auth.core.infrastructure.security.JwtKeyProperties;
import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OAuth2TokenConfig#jwtTokenCustomizer} 单测。
 *
 * <p>重点覆盖 {@code client_credentials}：该 grant 没有用户 principal，
 * {@code principal.getName()} 是 client_id，若照用户流程去 {@code UserDetailsService} 查用户会直接签发失败。
 *
 * @author sunshixiong
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("jwtTokenCustomizer")
class JwtTokenCustomizerTest {

    private static final String OPS_CLIENT_ID = "shengxinOps";

    @Mock
    private UserDetailsService userDetailsService;

    private OAuth2TokenCustomizer<JwtEncodingContext> customizer;

    @BeforeEach
    void setUp() {
        customizer = new OAuth2TokenConfig(new JwtKeyProperties()).jwtTokenCustomizer(userDetailsService);
    }

    private RegisteredClient opsClient() {
        return RegisteredClient.withId("1")
                .clientId(OPS_CLIENT_ID)
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("shopping-gold.grant")
                .scope("shopping-gold.revoke")
                .build();
    }

    private JwtEncodingContext clientCredentialsContext(RegisteredClient client) {
        return JwtEncodingContext.with(
                        JwsHeader.with(SignatureAlgorithm.RS256), JwtClaimsSet.builder())
                .registeredClient(client)
                // client_credentials 下 SAS 传入的 principal 是客户端认证令牌，getName() = client_id
                .principal(new UsernamePasswordAuthenticationToken(OPS_CLIENT_ID, null, List.of()))
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizedScopes(client.getScopes())
                .build();
    }

    @Test
    @DisplayName("client_credentials 不应去 UserDetailsService 查用户(client_id 不是用户名)")
    void shouldNotLoadUserForClientCredentials() {
        RegisteredClient client = opsClient();

        assertThatCode(() -> customizer.customize(clientCredentialsContext(client)))
                .doesNotThrowAnyException();

        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("client_credentials 应把 client_id 写入 preferred_username(下游重建 EagleUser 需要非空用户名)")
    void shouldWriteClientIdAsPreferredUsername() {
        JwtEncodingContext context = clientCredentialsContext(opsClient());

        customizer.customize(context);

        String username = context.getClaims().build().getClaim(SecurityConstants.DETAILS_USERNAME);
        assertThat(username).isEqualTo(OPS_CLIENT_ID);
    }

    @Test
    @DisplayName("client_credentials 应把 scope 写入 roles claim(资源服务器只从 roles 构造 authority)")
    void shouldWriteScopesAsRoles() {
        JwtEncodingContext context = clientCredentialsContext(opsClient());

        customizer.customize(context);

        List<String> roles = context.getClaims().build().getClaim(SecurityConstants.DETAILS_ROLES);
        assertThat(roles).containsExactlyInAnyOrder("shopping-gold.grant", "shopping-gold.revoke");
    }

    @Test
    @DisplayName("用户流程仍走 UserDetailsService 写入完整用户 claims")
    void shouldStillLoadUserForUserGrant() {
        EagleUser user = new EagleUser(7L, "phone_a1b2c3d4", "", "张三", "17708080863", "avatar.png",
                List.of(new SimpleGrantedAuthority("ROLE_user")));
        when(userDetailsService.loadUserByUsername("phone_a1b2c3d4")).thenReturn(user);
        JwtEncodingContext context = JwtEncodingContext.with(
                        JwsHeader.with(SignatureAlgorithm.RS256), JwtClaimsSet.builder())
                .registeredClient(RegisteredClient.withId("2")
                        .clientId("eagleApp")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("http://localhost/cb")
                        .scope("openid")
                        .build())
                .principal(new UsernamePasswordAuthenticationToken("phone_a1b2c3d4", null, List.of()))
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizedScopes(Set.of("openid"))
                .build();

        customizer.customize(context);

        JwtClaimsSet claims = context.getClaims().build();
        Long userId = claims.getClaim(SecurityConstants.DETAILS_USER_ID);
        String username = claims.getClaim(SecurityConstants.DETAILS_USERNAME);
        List<String> roles = claims.getClaim(SecurityConstants.DETAILS_ROLES);
        assertThat(userId).isEqualTo(7L);
        assertThat(username).isEqualTo("phone_a1b2c3d4");
        assertThat(roles).containsExactly("user");
    }
}
